package com.gooodhandletruck.zippick.data.remote

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.gooodhandletruck.zippick.BuildConfig
import com.gooodhandletruck.zippick.data.AppJson
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

/**
 * 서버가 실패로 답했다. [code]는 서버 에러 코드(C001·B001·F001 …)이고, 응답이 JSON이 아니면 null이다.
 * 통신 자체가 안 된 경우(서버 꺼짐·시간 초과)는 OkHttp의 IOException이 그대로 나간다.
 */
class ServerException(val status: Int, val code: String?, message: String?) :
    IOException("서버 오류 $status ${code.orEmpty()}: ${message.orEmpty()}")

/**
 * 백엔드와 주고받는 곳. 모든 응답은 { success, code, message, data } 봉투로 오고, 성공이면 data만 돌려준다.
 *
 * 기기 등록: 처음 요청할 때 POST /api/v1/devices로 익명 ID를 받아 이 기기에 기억하고,
 * 이후 모든 요청에 X-Anonymous-Id 헤더로 보낸다. 서버가 헤더를 모른다고 하면(D001·D002, 서버 DB를 비웠을 때)
 * 한 번 새로 등록하고 다시 보낸다. 화면과 Repository는 이 과정을 몰라도 된다.
 *
 * Retrofit 변환기를 새로 넣지 않으려고 OkHttp로 직접 부르고, 데이터 클래스는 kotlinx.serialization(AppJson)으로 읽는다.
 */
class ServerClient(context: Context, baseUrl: String = BuildConfig.API_BASE_URL) {

    private val baseUrl = baseUrl.toHttpUrl()
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val registerLock = Mutex()

    @Volatile
    private var anonymousId: String? = prefs.getString(KEY_ANONYMOUS_ID, null)

    private val http = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_S, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_S, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            }
        )
        .build()

    suspend fun get(path: String, query: Map<String, Any?> = emptyMap()): JsonElement? =
        call(METHOD_GET, path, query, body = null)

    suspend fun post(path: String, body: JsonElement): JsonElement? = call(METHOD_POST, path, emptyMap(), body)

    suspend fun put(path: String, body: JsonElement): JsonElement? = call(METHOD_PUT, path, emptyMap(), body)

    suspend fun patch(path: String): JsonElement? = call(METHOD_PATCH, path, emptyMap(), JsonObject(emptyMap()))

    suspend fun delete(path: String): JsonElement? = call(METHOD_DELETE, path, emptyMap(), body = null)

    private suspend fun call(method: String, path: String, query: Map<String, Any?>, body: JsonElement?): JsonElement? {
        val id = ensureRegistered()
        return try {
            send(method, path, query, body, id)
        } catch (e: ServerException) {
            if (e.code != CODE_MISSING_ID && e.code != CODE_UNKNOWN_DEVICE) throw e
            Log.w(TAG, "서버가 이 기기를 몰라서 다시 등록해요 (${e.code})")
            send(method, path, query, body, register(staleId = id))
        }
    }

    /** 기억한 익명 ID가 없으면 등록한다. 여러 요청이 동시에 와도 한 번만 등록한다 */
    private suspend fun ensureRegistered(): String = anonymousId ?: register(staleId = null)

    /** 새 익명 ID를 받는다. 기다리는 동안 다른 요청이 이미 새로 받았으면([staleId]와 다르면) 그것을 쓴다 */
    private suspend fun register(staleId: String?): String = registerLock.withLock {
        anonymousId?.takeIf { it != staleId }?.let { return@withLock it }
        val request = buildJsonObject { put("appVersion", BuildConfig.VERSION_NAME) }
        val data = send(METHOD_POST, DEVICES_PATH, emptyMap(), request, anonymousId = null)
        val id = data.decode<DeviceDto>().anonymousId
        anonymousId = id
        prefs.edit { putString(KEY_ANONYMOUS_ID, id) }
        id
    }

    private suspend fun send(
        method: String,
        path: String,
        query: Map<String, Any?>,
        body: JsonElement?,
        anonymousId: String?
    ): JsonElement? {
        val url = baseUrl.newBuilder()
            .addPathSegments(path.trimStart('/'))
            .apply { query.forEach { (name, value) -> if (value != null) addQueryParameter(name, value.toString()) } }
            .build()
        val request = Request.Builder()
            .url(url)
            .method(method, body?.let { AppJson.encodeToString(JsonElement.serializer(), it).toRequestBody(JSON) })
            .apply { if (anonymousId != null) header(HEADER_ANONYMOUS_ID, anonymousId) }
            .build()
        return http.newCall(request).await().use { response -> unwrap(response) }
    }

    /** 봉투를 벗긴다. 실패면 [ServerException]을 던지고, 성공이면 data(없으면 null)를 돌려준다 */
    private fun unwrap(response: Response): JsonElement? {
        val text = response.body?.string().orEmpty()
        val envelope = try {
            AppJson.parseToJsonElement(text) as? JsonObject
        } catch (e: SerializationException) {
            null
        }
        val success = envelope?.get("success")?.jsonPrimitive?.booleanOrNull == true
        if (!response.isSuccessful || !success) {
            throw ServerException(
                status = response.code,
                code = envelope?.stringOrNull("code"),
                message = envelope?.stringOrNull("message") ?: text.take(ERROR_TEXT_MAX)
            )
        }
        return envelope?.get("data")?.takeUnless { it is JsonNull }
    }

    private fun JsonObject.stringOrNull(key: String): String? =
        (get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response)
                }
            }
        )
    }

    private companion object {
        const val TAG = "ServerClient"
        const val PREFS_NAME = "server"
        const val KEY_ANONYMOUS_ID = "anonymous_id"
        const val HEADER_ANONYMOUS_ID = "X-Anonymous-Id"
        const val DEVICES_PATH = "api/v1/devices"
        const val CODE_MISSING_ID = "D001"
        const val CODE_UNKNOWN_DEVICE = "D002"
        const val METHOD_GET = "GET"
        const val METHOD_POST = "POST"
        const val METHOD_PUT = "PUT"
        const val METHOD_PATCH = "PATCH"
        const val METHOD_DELETE = "DELETE"
        const val CONNECT_TIMEOUT_S = 5L
        const val READ_TIMEOUT_S = 10L
        const val ERROR_TEXT_MAX = 200
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}

/** 봉투에서 꺼낸 data를 데이터 클래스로 읽는다. data가 없으면 서버가 약속을 어긴 것이라 오류로 본다 */
inline fun <reified T> JsonElement?.decode(): T =
    AppJson.decodeFromJsonElement(this ?: throw ServerException(status = 200, code = null, message = "data가 비어 있어요"))

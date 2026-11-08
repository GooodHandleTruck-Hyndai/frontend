package com.gooodhandletruck.zippick.data.remote

import android.content.Context
import com.gooodhandletruck.zippick.BuildConfig
import com.gooodhandletruck.zippick.data.AppJson
import com.gooodhandletruck.zippick.data.remote.dto.DeviceRegisterRequest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * 서버와 통신하는 창구 하나(axios 인스턴스와 비슷하다). 공통 헤더, 기기 등록, 실패 응답 바꾸기를 여기서 맡는다.
 * 앱에서는 [create]로 만들어 AppContainer가 하나만 들고 있는다.
 *
 * - 헤더가 필요 없는 API(주변 건물, 건물 상세 등)는 [call]로 부른다. 기기 ID가 있으면 헤더는 붙는다.
 * - X-Anonymous-Id가 꼭 있어야 하는 API(저장, 내 기기)는 [callAsDevice]로 부른다. 처음이면 기기부터 등록한다.
 * - 실패 응답(4xx·5xx)은 [ApiException]으로 던지고, 통신이 안 되면 IOException이 난다.
 *
 * ```
 * val nearby = apiClient.call { nearbyBuildings(lat, lon, radius = 1000) }.requireData()
 * apiClient.callAsDevice { deleteFavorite(buildingId) }
 * ```
 */
class ApiClient(
    private val api: ZippickApi,
    private val deviceIds: DeviceIdStorage
) {
    // 동시에 여러 요청이 기기 등록을 시작하지 않게 한다
    private val registerLock = Mutex()

    /** [request]를 보내고 성공 응답을 돌려준다. 실패 응답은 [ApiException]으로 던진다 */
    suspend fun <T> call(request: suspend ZippickApi.() -> ApiResponse<T>): ApiResponse<T> = try {
        api.request()
    } catch (e: HttpException) {
        throw e.toApiException()
    }

    /**
     * X-Anonymous-Id가 꼭 있어야 하는 API를 보낸다. 아직 등록하지 않았으면 먼저 등록한다.
     * 서버가 ID를 모르면(D002: 서버 DB를 비웠거나 서버 주소를 바꿨을 때) 다시 등록하고 한 번만 더 보낸다.
     */
    suspend fun <T> callAsDevice(request: suspend ZippickApi.() -> ApiResponse<T>): ApiResponse<T> {
        val usedId = ensureDevice()
        return try {
            call(request)
        } catch (e: ApiException) {
            if (e.code != ApiErrorCode.DEVICE_NOT_FOUND) throw e
            renewDevice(staleId = usedId)
            call(request)
        }
    }

    /** 이 기기의 익명 ID. 없으면 서버에 등록해 받아 기억한다. 여러 곳에서 동시에 불러도 등록은 한 번만 한다 */
    suspend fun ensureDevice(): String = registerLock.withLock { deviceIds.anonymousId ?: register() }

    /** 서버가 [staleId]를 모른다고 했다. 그사이 다른 요청이 새로 등록했으면 그 ID를 쓰고, 아니면 다시 등록한다 */
    private suspend fun renewDevice(staleId: String): String = registerLock.withLock {
        val current = deviceIds.anonymousId
        if (current != null && current != staleId) {
            current
        } else {
            deviceIds.anonymousId = null // 등록 요청은 헤더 없이 보낸다
            register()
        }
    }

    // registerLock 안에서만 부른다
    private suspend fun register(): String {
        val device = call { registerDevice(DeviceRegisterRequest(appVersion = BuildConfig.VERSION_NAME)) }.requireData()
        deviceIds.anonymousId = device.anonymousId
        return device.anonymousId
    }

    companion object {
        /** 앱에서 쓰는 창구. 서버 주소는 BuildConfig.API_BASE_URL(app/build.gradle.kts)이다 */
        fun create(context: Context): ApiClient {
            val deviceIds = DeviceIdStore(context)
            return ApiClient(createZippickApi(BuildConfig.API_BASE_URL, createOkHttpClient(deviceIds)), deviceIds)
        }
    }
}

/** 공통 헤더와 로그를 붙인 OkHttp. 단위 테스트는 여기에 가짜 서버 interceptor를 더해 쓴다 */
internal fun createOkHttpClient(deviceIds: DeviceIdStorage): OkHttpClient = OkHttpClient.Builder()
    .addInterceptor(CommonHeadersInterceptor(deviceIds))
    // 디버그 빌드만 요청·응답 본문을 logcat(okhttp.OkHttpClient 태그)에 남긴다
    .addInterceptor(
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
    )
    // 주변 건물은 서버가 카카오에 좌표를 나눠 물어서 반경 3km에서 3초쯤 걸린다. 넉넉히 기다린다
    .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
    .build()

/** 서버 응답을 목업과 같은 JSON 설정(AppJson)으로 읽는 Retrofit */
internal fun createZippickApi(baseUrl: String, client: OkHttpClient): ZippickApi = Retrofit.Builder()
    .baseUrl(baseUrl)
    .client(client)
    .addConverterFactory(AppJson.asConverterFactory("application/json".toMediaType()))
    .build()
    .create(ZippickApi::class.java)

/**
 * 모든 요청에 붙이는 공통 헤더.
 * - ngrok-skip-browser-warning: 개발 서버(ngrok)가 응답 대신 안내 페이지를 주지 않게 한다. 정식 서버는 이 헤더를 무시한다
 * - X-Anonymous-Id: 기기를 등록한 뒤 모든 요청에 붙인다. 등록은 ID가 없을 때만 하니 등록 요청은 헤더 없이 나간다
 */
internal class CommonHeadersInterceptor(private val deviceIds: DeviceIdStorage) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().header(HEADER_NGROK_SKIP_WARNING, "1")
        deviceIds.anonymousId?.let { request.header(HEADER_ANONYMOUS_ID, it) }
        return chain.proceed(request.build())
    }
}

internal const val HEADER_ANONYMOUS_ID = "X-Anonymous-Id"
internal const val HEADER_NGROK_SKIP_WARNING = "ngrok-skip-browser-warning"
private const val READ_TIMEOUT_SECONDS = 20L

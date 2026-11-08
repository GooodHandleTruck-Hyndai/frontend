package com.gooodhandletruck.zippick.data.remote

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer

/**
 * 네트워크 없이 서버처럼 답하는 마지막 interceptor. "GET /api/v1/favorites"처럼 메서드와 경로마다 답을 정해 두고,
 * 받은 요청을 보낸 순서대로 남긴다. 공통 헤더 interceptor 뒤에 붙어서 실제 Retrofit·OkHttp·JSON 변환을 그대로 거친다.
 */
class StubServer : Interceptor {

    private val routes = mutableMapOf<String, (Request) -> StubResponse>()
    private val received = mutableListOf<Request>()

    /** 받은 요청, 보낸 순서대로 */
    val requests: List<Request> get() = synchronized(received) { received.toList() }

    fun on(method: String, path: String, respond: (Request) -> StubResponse) {
        routes["$method $path"] = respond
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        synchronized(received) { received += request }
        val stub = routes["${request.method} ${request.url.encodedPath}"]?.invoke(request) ?: NO_ROUTE
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(stub.status)
            .message("stub")
            .apply { stub.headers.forEach { (name, value) -> header(name, value) } }
            .body(stub.body.toResponseBody(stub.contentType.toMediaType()))
            .build()
    }

    private companion object {
        val NO_ROUTE = StubResponse(404, """{"success":false,"code":"C002","message":"요청한 리소스를 찾을 수 없어요"}""")
    }
}

data class StubResponse(
    val status: Int,
    val body: String,
    val contentType: String = "application/json",
    val headers: Map<String, String> = emptyMap()
)

/** 테스트용 기기 ID 저장소(메모리) */
class MemoryDeviceIds(@Volatile override var anonymousId: String? = null) : DeviceIdStorage

/** 가짜 서버를 붙인 진짜 [ApiClient] */
fun stubApiClient(server: StubServer, deviceIds: DeviceIdStorage): ApiClient {
    val client = createOkHttpClient(deviceIds).newBuilder().addInterceptor(server).build()
    return ApiClient(createZippickApi("https://zippick.test/", client), deviceIds)
}

/** 요청 본문(JSON) */
fun Request.bodyText(): String = Buffer().also { body?.writeTo(it) }.readUtf8()

/** [block]이 [E]를 던지는지 본다. 코루틴 안에서 부르면 [block] 안에서 suspend 함수를 불러도 된다 */
inline fun <reified E : Throwable> expectThrows(block: () -> Unit): E {
    try {
        block()
    } catch (e: Throwable) {
        if (e is E) return e
        throw AssertionError("${E::class.java.simpleName} 대신 ${e::class.java.simpleName}이 났어요", e)
    }
    throw AssertionError("${E::class.java.simpleName}이 나지 않았어요")
}

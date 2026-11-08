package com.gooodhandletruck.zippick.data.remote

import com.gooodhandletruck.zippick.BuildConfig
import com.gooodhandletruck.zippick.data.remote.dto.FavoriteCreateRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 공통 헤더, 기기 등록, 다시 등록하기, 실패 응답 바꾸기를 가짜 서버로 본다. */
class ApiClientTest {

    private val server = StubServer()
    private val deviceIds = MemoryDeviceIds()
    private val client = stubApiClient(server, deviceIds)

    @Test
    fun callWithoutDeviceDoesNotRegister() = runBlocking<Unit> {
        server.on("GET", "/api/v1/buildings/nearby") { StubResponse(200, ApiSamples.NEARBY) }

        client.call { nearbyBuildings(lat = 37.5404, lon = 126.9455, radius = 1000) }.requireData()

        val request = server.requests.single()
        assertEquals("lat=37.5404&lon=126.9455&radius=1000", request.url.encodedQuery)
        assertEquals("1", request.header(HEADER_NGROK_SKIP_WARNING))
        assertNull(request.header(HEADER_ANONYMOUS_ID))
    }

    @Test
    fun callSendsStoredDeviceId() = runBlocking<Unit> {
        deviceIds.anonymousId = "saved-id"
        server.on("GET", "/api/v1/buildings/21") { StubResponse(200, ApiSamples.DETAIL) }

        client.call { buildingDetail(21) }

        // 건물 상세는 헤더가 있어야 saved가 맞게 온다
        assertEquals("saved-id", server.requests.single().header(HEADER_ANONYMOUS_ID))
    }

    @Test
    fun callAsDeviceRegistersFirstThenSendsId() = runBlocking<Unit> {
        server.on("POST", "/api/v1/devices") { StubResponse(201, ApiSamples.DEVICE) }
        server.on("GET", "/api/v1/favorites") { StubResponse(200, ApiSamples.FAVORITES) }

        client.callAsDevice { favorites() }

        val (register, favorites) = server.requests
        assertEquals("POST /api/v1/devices", "${register.method} ${register.url.encodedPath}")
        assertNull(register.header(HEADER_ANONYMOUS_ID))
        assertEquals("""{"appVersion":"${BuildConfig.VERSION_NAME}"}""", register.bodyText())
        assertEquals(ApiSamples.DEVICE_ID, favorites.header(HEADER_ANONYMOUS_ID))
        assertEquals(ApiSamples.DEVICE_ID, deviceIds.anonymousId)
    }

    @Test
    fun registersOnceWhenCalledTogether() = runBlocking<Unit> {
        server.on("POST", "/api/v1/devices") { StubResponse(201, ApiSamples.DEVICE) }
        server.on("GET", "/api/v1/favorites") { StubResponse(200, ApiSamples.FAVORITES) }

        List(3) { async { client.callAsDevice { favorites() } } }.awaitAll()

        assertEquals(1, server.requests.count { it.url.encodedPath == "/api/v1/devices" })
        assertEquals(3, server.requests.count { it.header(HEADER_ANONYMOUS_ID) == ApiSamples.DEVICE_ID })
    }

    @Test
    fun registersAgainWhenServerForgotDevice() = runBlocking<Unit> {
        deviceIds.anonymousId = "old-id"
        server.on("POST", "/api/v1/devices") { StubResponse(201, ApiSamples.DEVICE) }
        server.on("GET", "/api/v1/favorites") { request ->
            if (request.header(HEADER_ANONYMOUS_ID) == "old-id") {
                StubResponse(404, ApiSamples.DEVICE_NOT_FOUND)
            } else {
                StubResponse(200, ApiSamples.FAVORITES)
            }
        }

        val favorites = client.callAsDevice { favorites() }.requireData()

        assertEquals(1, favorites.size)
        assertEquals(
            listOf("old-id", null, ApiSamples.DEVICE_ID),
            server.requests.map { it.header(HEADER_ANONYMOUS_ID) }
        )
        assertEquals(ApiSamples.DEVICE_ID, deviceIds.anonymousId)
    }

    @Test
    fun retriesOnlyOnceWhenDeviceStaysUnknown() = runBlocking<Unit> {
        deviceIds.anonymousId = "old-id"
        server.on("POST", "/api/v1/devices") { StubResponse(201, ApiSamples.DEVICE) }
        server.on("GET", "/api/v1/favorites") { StubResponse(404, ApiSamples.DEVICE_NOT_FOUND) }

        val error = expectThrows<ApiException> { client.callAsDevice { favorites() } }

        assertEquals(ApiErrorCode.DEVICE_NOT_FOUND, error.code)
        assertEquals(3, server.requests.size) // 목록, 다시 등록, 목록 한 번 더
    }

    @Test
    fun failureBodyBecomesApiException() = runBlocking<Unit> {
        server.on("GET", "/api/v1/buildings/999999") { StubResponse(404, ApiSamples.BUILDING_NOT_FOUND) }

        val error = expectThrows<ApiException> { client.call { buildingDetail(999999) } }

        assertEquals(404, error.httpStatus)
        assertEquals(ApiErrorCode.BUILDING_NOT_FOUND, error.code)
        assertEquals("건물을 찾을 수 없어요", error.message)
    }

    @Test
    fun ngrokOfflinePageHasNoCode() = runBlocking<Unit> {
        server.on("GET", "/api/v1/buildings/21") {
            StubResponse(
                404,
                ApiSamples.NGROK_OFFLINE_HTML,
                contentType = "text/html",
                headers = mapOf("ngrok-error-code" to "ERR_NGROK_3200")
            )
        }

        val error = expectThrows<ApiException> { client.call { buildingDetail(21) } }

        // 같은 404여도 없는 건물(B001)과 헷갈리지 않게 code가 비어 있다
        assertNull(error.code)
        assertTrue(error.message, error.message!!.contains("ERR_NGROK_3200"))
    }

    @Test
    fun alreadySavedIsConflict() = runBlocking<Unit> {
        deviceIds.anonymousId = ApiSamples.DEVICE_ID
        server.on("POST", "/api/v1/favorites") { StubResponse(409, ApiSamples.FAVORITE_ALREADY_EXISTS) }

        val error = expectThrows<ApiException> { client.callAsDevice { saveFavorite(FavoriteCreateRequest(21)) } }

        assertEquals(ApiErrorCode.FAVORITE_ALREADY_EXISTS, error.code)
        assertEquals("""{"buildingId":21}""", server.requests.single().bodyText())
    }

    @Test
    fun deleteFavoriteSucceedsWithoutData() = runBlocking<Unit> {
        deviceIds.anonymousId = ApiSamples.DEVICE_ID
        server.on("DELETE", "/api/v1/favorites/21") { StubResponse(200, ApiSamples.OK_NO_DATA) }

        val response = client.callAsDevice { deleteFavorite(21) }

        assertTrue(response.success)
        assertNull(response.data)
    }

    @Test
    fun requireDataFailsWithoutData() {
        val response = ApiResponse<String>(success = true, code = "OK", message = "요청 성공")

        expectThrows<IllegalStateException> { response.requireData() }
    }
}

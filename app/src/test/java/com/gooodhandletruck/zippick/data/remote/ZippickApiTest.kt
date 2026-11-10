package com.gooodhandletruck.zippick.data.remote

import com.gooodhandletruck.zippick.data.AppJson
import com.gooodhandletruck.zippick.data.remote.dto.FavoriteCreateRequest
import com.gooodhandletruck.zippick.data.remote.dto.InterestConditionRequest
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 엔드포인트마다 경로·쿼리가 백엔드와 같은지, 응답 예시가 dto/ 데이터 클래스로 읽히는지 본다. */
class ZippickApiTest {

    private val server = StubServer()
    private val deviceIds = MemoryDeviceIds(anonymousId = ApiSamples.DEVICE_ID)
    private val client = stubApiClient(server, deviceIds)

    @Test
    fun myDevice() = runBlocking<Unit> {
        server.on("GET", "/api/v1/devices/me") { StubResponse(200, ApiSamples.DEVICE) }

        val device = client.callAsDevice { myDevice() }.requireData()

        assertEquals(ApiSamples.DEVICE_ID, device.anonymousId)
        assertEquals(OffsetDateTime.parse("2026-11-08T16:40:12.123456+09:00"), device.createdAt)
    }

    @Test
    fun nearbyBuildings() = runBlocking<Unit> {
        server.on("GET", "/api/v1/buildings/nearby") { StubResponse(200, ApiSamples.NEARBY) }

        val nearby = client.call { nearbyBuildings(lat = 37.5404, lon = 126.9455) }.requireData()

        // radius를 비우면 쿼리에서 빠지고, 서버가 1000m로 본다
        assertEquals("lat=37.5404&lon=126.9455", server.requests.single().url.encodedQuery)
        assertEquals(LocalDate.of(2026, 11, 7), nearby.dataBaseDate)
        val (first, second) = nearby.buildings
        assertEquals(21L, first.buildingId)
        assertEquals("서울 마포구 도화동 555", first.address)
        assertEquals(180, first.distanceM)
        assertEquals(59700, first.latestDeal!!.price)
        assertEquals(LocalDate.of(2026, 9, 30), first.latestDeal!!.dealDate)
        assertEquals(36.49, first.latestDeal!!.areaM2, 0.0)
        assertNull(second.latestDeal)
    }

    @Test
    fun nearbyBuildingsEmpty() = runBlocking<Unit> {
        server.on("GET", "/api/v1/buildings/nearby") { StubResponse(200, ApiSamples.NEARBY_EMPTY) }

        val nearby = client.call { nearbyBuildings(lat = 37.5404, lon = 126.9455, radius = 100) }.requireData()

        assertNull(nearby.dataBaseDate)
        assertTrue(nearby.buildings.isEmpty())
    }

    @Test
    fun buildingDetail() = runBlocking<Unit> {
        server.on("GET", "/api/v1/buildings/21") { StubResponse(200, ApiSamples.DETAIL) }

        val detail = client.call { buildingDetail(21) }.requireData()

        assertEquals("한화오벨리스크", detail.name)
        assertNull(detail.roadAddress)
        assertEquals(2004, detail.builtYear)
        assertEquals(37.54004, detail.lat!!, 0.0)
        assertFalse(detail.saved)
        assertEquals(LocalDate.of(2026, 11, 7), detail.dataBaseDate)
        assertEquals(listOf("101", null), detail.recentTransactions.map { it.dong })
        assertEquals(listOf(59700, 83700), detail.recentTransactions.map { it.price })
        assertTrue(detail.hasMoreTransactions)
    }

    @Test
    fun transactions() = runBlocking<Unit> {
        server.on("GET", "/api/v1/buildings/21/transactions") { StubResponse(200, ApiSamples.TRANSACTIONS) }

        val page = client.call { transactions(21, page = 1) }.requireData()

        assertEquals("page=1&size=20", server.requests.single().url.encodedQuery)
        assertEquals(1, page.page)
        assertFalse(page.hasNext)
        assertEquals(18.1, page.transactions.single().areaPyeong, 0.0)
    }

    @Test
    fun priceTrend() = runBlocking<Unit> {
        server.on("GET", "/api/v1/buildings/21/price-trend") { StubResponse(200, ApiSamples.PRICE_TREND) }

        val months = client.call { priceTrend(21) }.requireData().months

        assertEquals(listOf("2025-12", "2026-01"), months.map { it.yearMonth })
        assertEquals(listOf(83700, null), months.map { it.avgPrice })
    }

    @Test
    fun favorites() = runBlocking<Unit> {
        server.on("GET", "/api/v1/favorites") { StubResponse(200, ApiSamples.FAVORITES) }
        server.on("POST", "/api/v1/favorites") { StubResponse(201, ApiSamples.FAVORITE) }

        val saved = client.callAsDevice { saveFavorite(FavoriteCreateRequest(21)) }.requireData()
        val list = client.callAsDevice { favorites() }.requireData()

        // 서버가 좌표를 아직 못 찾은 건물은 lat·lon이 비어 있다
        assertNull(saved.lat)
        assertEquals(OffsetDateTime.parse("2026-11-08T16:41:03.5+09:00"), saved.savedAt)
        assertEquals(listOf(21L), list.map { it.buildingId })
        assertEquals(OffsetDateTime.parse("2026-11-08T16:41:03+09:00"), list.single().savedAt)
    }

    @Test
    fun interestConditionNotSetYet() = runBlocking<Unit> {
        server.on("GET", "/api/v1/interest-conditions") { StubResponse(200, ApiSamples.OK_NO_DATA) }

        // 조건을 정한 적이 없으면 data가 없다
        assertNull(client.callAsDevice { interestCondition() }.data)
    }

    @Test
    fun saveInterestCondition() = runBlocking<Unit> {
        server.on("PUT", "/api/v1/interest-conditions") { StubResponse(200, ApiSamples.INTEREST_CONDITION) }
        val request = InterestConditionRequest(
            regionCodes = listOf("11440"),
            priceMin = 80000,
            priceMax = null,
            types = listOf("APT"),
            areaMinM2 = null,
            areaMaxM2 = 85,
            alertEnabled = true
        )

        val saved = client.callAsDevice { saveInterestCondition(request) }.requireData()

        // 전체 값을 바꾸는 PUT이라 null인 칸도 빼지 않고 null로 보낸다
        val body = AppJson.parseToJsonElement(server.requests.single().bodyText()).jsonObject
        assertEquals(7, body.size)
        assertEquals(JsonNull, body["priceMax"])
        assertEquals(JsonNull, body["areaMinM2"])
        assertEquals(listOf("11440"), saved.regionCodes)
        assertEquals(150000, saved.priceMax)
        assertTrue(saved.alertEnabled)
    }

    @Test
    fun alertsSince() = runBlocking<Unit> {
        server.on("GET", "/api/v1/alerts") { StubResponse(200, ApiSamples.ALERTS) }

        val response = client.callAsDevice { alerts(OffsetDateTime.parse("2026-11-03T09:00:00+09:00")) }.requireData()

        val url = server.requests.single().url
        assertEquals("2026-11-03T09:00+09:00", url.queryParameter("since"))
        assertTrue(url.encodedQuery!!, url.encodedQuery!!.contains("%2B09")) // +는 %2B로 간다
        val alert = response.alerts.single()
        assertEquals("한화오벨리스크", alert.buildingName)
        assertEquals(59700, alert.deal.price)
        assertEquals(LocalDate.of(2026, 10, 12), alert.deal.dealDate)
        assertFalse(alert.read)
        assertEquals(OffsetDateTime.parse("2026-11-09T04:30:01.123456+09:00"), alert.createdAt)
    }

    @Test
    fun alertsWithoutSinceAndRead() = runBlocking<Unit> {
        server.on("GET", "/api/v1/alerts") { StubResponse(200, ApiSamples.ALERTS) }
        server.on("PATCH", "/api/v1/alerts/7/read") { StubResponse(200, ApiSamples.ALERT_READ) }
        server.on("PATCH", "/api/v1/alerts/8/read") { StubResponse(404, ApiSamples.ALERT_NOT_FOUND) }

        client.callAsDevice { alerts() }
        val read = client.callAsDevice { markAlertRead(7) }.requireData()
        val error = expectThrows<ApiException> { client.callAsDevice { markAlertRead(8) } }

        // since를 비우면 쿼리에서 빠지고, 서버가 최근 7일로 본다
        assertNull(server.requests.first().url.encodedQuery)
        assertTrue(read.read)
        assertEquals(OffsetDateTime.parse("2026-11-09T10:12:00+09:00"), read.readAt)
        assertEquals(ApiErrorCode.ALERT_NOT_FOUND, error.code)
    }
}

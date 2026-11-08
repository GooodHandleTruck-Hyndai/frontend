package com.gooodhandletruck.zippick.data.remote

import com.gooodhandletruck.zippick.data.remote.dto.FavoriteCreateRequest
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlinx.coroutines.runBlocking
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
}

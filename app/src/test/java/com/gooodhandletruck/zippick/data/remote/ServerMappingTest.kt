package com.gooodhandletruck.zippick.data.remote

import com.gooodhandletruck.zippick.data.AlertConditions
import com.gooodhandletruck.zippick.data.AppJson
import com.gooodhandletruck.zippick.data.AreaRange
import com.gooodhandletruck.zippick.data.ComplexType
import com.gooodhandletruck.zippick.data.PriceRange
import com.gooodhandletruck.zippick.data.model.hasCoordinate
import java.time.LocalDate
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 서버 응답 예시(Swagger)를 앱 데이터 클래스로 바꾸는 규칙 */
class ServerMappingTest {

    @Test
    fun nearbyKeepsLatestDealAsLastTrade() {
        val json = """
            {"success":true,"code":"OK","message":"요청 성공","data":{"dataBaseDate":"2026-11-07","buildings":[
              {"buildingId":21,"name":"한화오벨리스크","type":"APT","address":"서울 마포구 도화동 555",
               "lat":37.54004,"lon":126.94547,"distanceM":180,"imageUrl":null,
               "latestDeal":{"price":59700,"dealDate":"2026-09-30","areaM2":36.49,"areaPyeong":11.0}},
              {"buildingId":9,"name":"거래 없는 단지","type":"APT","address":"서울 마포구 도화동 1",
               "lat":37.5,"lon":126.9,"distanceM":900,"imageUrl":null,"latestDeal":null}
            ]}}
        """.trimIndent()
        val data = AppJson.parseToJsonElement(json).jsonObject["data"]
        val response = data.decode<NearbyBuildingsDto>()

        val first = response.buildings[0].toSummary(response.dataBaseDate, saved = true)
        assertEquals("21", first.complexId)
        assertEquals(true, first.saved)
        assertNull(first.price?.avgPriceManwon)
        assertEquals(59700, first.price?.lastTrade?.priceManwon)
        assertEquals(LocalDate.parse("2026-11-07"), first.price?.asOf)
        // 서버가 층을 주지 않으니 0층이 아니라 '모름'이다
        assertNull(first.price?.lastTrade?.floor)
        assertNull(response.buildings[1].toSummary(response.dataBaseDate, saved = false).price)
    }

    @Test
    fun detailTradesDoNotCountRecentTradesTwice() {
        val recent = listOf(trade("2026-10-01", 84.97, 100_000))
        val more = listOf(trade("2026-10-01", 84.97, 100_000), trade("2026-09-01", 84.97, 110_000))

        // 더보기를 불렀으면 최근 3건은 그 첫 쪽과 같은 거래라 버린다
        assertEquals(more, detailTrades(recent, more))
        // 더보기를 안 불렀으면 최근 3건만 쓴다
        assertEquals(recent, detailTrades(recent, emptyList()))
    }

    @Test
    fun detailWithoutCoordinateBlocksRoute() {
        val dto = BuildingDetailDto(buildingId = 9, name = "좌표 없는 단지", type = "APT", lat = null, lon = null)
        val today = LocalDate.parse("2026-11-10")

        val unknown = dto.toDetail(emptyList(), saved = false, fallback = null, today = today)
        assertFalse(unknown.hasCoordinate)

        val fallback = NearbyBuildingDto(buildingId = 9, name = "좌표 없는 단지", type = "APT", lat = 37.54, lon = 126.94)
            .toSummary(asOf = null, saved = false)
        val known = dto.toDetail(emptyList(), saved = false, fallback = fallback, today = today)
        assertTrue(known.hasCoordinate)
        assertEquals(37.54, known.lat, 0.0)
    }

    @Test
    fun tradeWithoutFloorKeepsFloorUnknown() {
        val dto = BuildingDetailDto(buildingId = 21, name = "한화오벨리스크", type = "APT", lat = 37.5, lon = 126.9)
        val noFloor = TransactionDto(
            dealDate = LocalDate.parse("2026-10-01"),
            areaM2 = 84.97,
            floor = null,
            price = 100_000
        )

        val detail = dto.toDetail(
            listOf(noFloor),
            saved = false,
            fallback = null,
            today = LocalDate.parse("2026-11-10")
        )

        assertNull(detail.areas.single().recentTrades.single().floor)
    }

    @Test
    fun detailBuildsAreaAveragesAndRecentTrades() {
        val dto = BuildingDetailDto(
            buildingId = 21,
            name = "한화오벨리스크",
            type = "APT",
            jibunAddress = "서울 마포구 도화동 555",
            lat = null,
            lon = null,
            dataBaseDate = LocalDate.parse("2026-11-07")
        )
        val trades = listOf(
            trade("2026-10-01", 84.97, 100_000),
            trade("2026-09-01", 84.97, 110_000),
            // 90일 밖
            trade("2026-01-01", 84.97, 90_000),
            // 기간 안 거래가 없는 평형
            trade("2025-12-01", 59.98, 70_000)
        )
        val detail = dto.toDetail(trades, saved = false, fallback = null, today = LocalDate.parse("2026-11-10"))

        assertEquals(listOf(59.98, 84.97), detail.areas.map { it.areaM2 })
        val big = detail.areas.last()
        assertEquals(105_000, big.avgPriceManwon)
        assertEquals(2, big.tradeCount)
        assertEquals(3, big.recentTrades.size)
        assertNull(detail.areas.first().avgPriceManwon)
        assertEquals(84.97, detail.defaultAreaM2)
        assertEquals("서울 마포구 도화동 555", detail.address)
    }

    @Test
    fun alertConditionsSendOuterBounds() {
        val conditions = AlertConditions(
            types = setOf(ComplexType.APT, ComplexType.ONE_ROOM),
            prices = setOf(PriceRange.FROM_5_TO_10_EOK, PriceRange.FROM_10_TO_15_EOK),
            areas = setOf(AreaRange.FROM_60_TO_85, AreaRange.OVER_85)
        )
        val body = conditions.toServer(regionCode = "11440", alertEnabled = true)

        assertEquals(listOf("11440"), body.regionCodes)
        assertEquals(50_000, body.priceMin)
        assertEquals(150_000, body.priceMax)
        assertEquals(60, body.areaMinM2)
        assertNull(body.areaMaxM2) // 85㎡ 초과는 위쪽 끝이 없다
        assertEquals(listOf("APT"), body.types)
    }

    @Test
    fun readsAlerts() {
        val json = """
            {"alerts":[{"alertId":7,"buildingId":101,"buildingName":"상암월드컵파크7단지",
              "deal":{"price":125000,"dealDate":"2026-10-12","areaM2":84.97,"areaPyeong":25.7},
              "read":false,"createdAt":"2026-11-03T06:00:00+09:00"}]}
        """.trimIndent()
        val alert = AppJson.parseToJsonElement(json).decode<AlertListDto>().alerts.single().toAlert()

        assertEquals("101", alert.complexId)
        assertEquals(125_000, alert.priceManwon)
        assertEquals(false, alert.read)
    }

    private fun trade(date: String, area: Double, price: Int) =
        TransactionDto(dealDate = LocalDate.parse(date), areaM2 = area, floor = 10, price = price)
}

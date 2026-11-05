package com.gooodhandletruck.sidong.data

import com.gooodhandletruck.sidong.data.model.AreaPrice
import com.gooodhandletruck.sidong.data.model.ComplexDetail
import com.gooodhandletruck.sidong.data.model.NearbyResponse
import com.gooodhandletruck.sidong.data.model.defaultArea
import java.io.File
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 단지 상세 목업이 데이터 클래스와 맞는지, 카드(주변 단지 목업)와 같은 가격을 보여 주는지 본다. */
class FakeDetailTest {

    // 단위 테스트는 app 모듈 폴더에서 돈다
    private val nearby: NearbyResponse =
        AppJson.decodeFromString(File("src/main/assets/mock/complexes_nearby.json").readText())
    private val details: Map<String, ComplexDetail> =
        AppJson.decodeFromString<List<ComplexDetail>>(File("src/main/assets/mock/complex_details.json").readText())
            .associateBy { it.complexId }

    @Test
    fun everyNearbyComplexHasDetail() = assertEquals(nearby.items.map { it.complexId }.toSet(), details.keys)

    @Test
    fun defaultAreaMatchesCard() {
        nearby.items.forEach { card ->
            val price = card.price ?: return@forEach
            val area = details.getValue(card.complexId).defaultArea!!
            assertEquals(card.complexId, price.areaM2, area.areaM2, 0.0)
            assertEquals(card.complexId, price.avgPriceManwon, area.avgPriceManwon)
            assertEquals(card.complexId, price.tradeCount, area.tradeCount)
            assertEquals(card.complexId, price.lastTrade, area.recentTrades.first())
        }
    }

    @Test
    fun areasAreSortedAndTradesAreLatestFirst() {
        details.values.forEach { detail ->
            val sizes = detail.areas.map { it.areaM2 }
            assertEquals(detail.complexId, sizes.sorted(), sizes)
            detail.areas.forEach { area ->
                val dates = area.recentTrades.map { it.contractDate }
                assertTrue(detail.complexId, area.recentTrades.size in 1..3)
                assertEquals(detail.complexId, dates.sortedDescending(), dates)
            }
        }
    }

    @Test
    fun complexWithoutTradesHasNoArea() {
        // 바람꽃 1단지: 거래 기록이 아예 없다
        val detail = details.getValue("c-1017")
        assertTrue(detail.areas.isEmpty())
        assertNull(detail.defaultArea)
    }

    @Test
    fun hanbitMatchesApiExample() {
        // 한빛마을 3단지는 API 제안 3번 응답 예시와 같다
        val area = details.getValue("c-1001").defaultArea!!
        assertEquals(91200, area.officialPriceManwon)
        assertEquals(LocalDate.of(2026, 10, 24), area.recentTrades.first().contractDate)
    }

    @Test
    fun missingDefaultFallsBackToMostTraded() {
        val detail = details.getValue("c-1001").copy(defaultAreaM2 = 10.0)
        assertEquals(84.97, detail.defaultArea!!.areaM2, 0.0)
    }

    @Test
    fun noDefaultAndNoAreasIsNull() {
        val detail = details.getValue("c-1001").copy(defaultAreaM2 = null, areas = emptyList<AreaPrice>())
        assertNull(detail.defaultArea)
    }
}

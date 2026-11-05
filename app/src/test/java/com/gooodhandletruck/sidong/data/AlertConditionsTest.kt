package com.gooodhandletruck.sidong.data

import com.gooodhandletruck.sidong.data.model.NearbyResponse
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 알림 조건 칩의 경계와, 단지가 조건에 맞는지 본다. */
class AlertConditionsTest {

    // 단위 테스트는 app 모듈 폴더에서 돈다
    private val byId = AppJson.decodeFromString<NearbyResponse>(
        File("src/main/assets/mock/complexes_nearby.json").readText()
    ).items.associateBy { it.complexId }

    @Test
    fun priceBoundsIncludeUpperEnd() {
        assertTrue(PriceRange.UP_TO_5_EOK.contains(50_000))
        assertFalse(PriceRange.FROM_5_TO_10_EOK.contains(50_000))
        assertTrue(PriceRange.FROM_5_TO_10_EOK.contains(100_000))
        assertTrue(PriceRange.OVER_15_EOK.contains(150_001))
    }

    @Test
    fun areaBoundsIncludeUpperEnd() {
        assertTrue(AreaRange.UP_TO_60.contains(59.96))
        assertTrue(AreaRange.FROM_60_TO_85.contains(84.97))
        assertTrue(AreaRange.OVER_85.contains(114.8))
    }

    @Test
    fun defaultChoosesEverythingAvailable() {
        val conditions = AlertConditions()
        assertEquals(setOf(ComplexType.APT), conditions.types)
        assertTrue(conditions.isComplete)
        assertTrue(conditions.matches(byId.getValue("c-1001")))
    }

    @Test
    fun priceRangeFilters() {
        val tenToFifteen = AlertConditions(prices = setOf(PriceRange.FROM_10_TO_15_EOK))
        assertTrue(tenToFifteen.matches(byId.getValue("c-1001"))) // 12억 3,000만
        assertFalse(tenToFifteen.matches(byId.getValue("c-1003"))) // 9억 8,500만
    }

    @Test
    fun noRecentAverageUsesLastTrade() {
        // 한솔 5단지는 최근 3개월 평균이 없고 마지막 거래가 6억 4,500만이다
        assertTrue(AlertConditions(prices = setOf(PriceRange.FROM_5_TO_10_EOK)).matches(byId.getValue("c-1002")))
    }

    @Test
    fun complexWithoutTradesMatchesOnlyWithoutFilters() {
        val noTrades = byId.getValue("c-1017")
        assertTrue(AlertConditions().matches(noTrades))
        assertFalse(AlertConditions(areas = setOf(AreaRange.FROM_60_TO_85)).matches(noTrades))
    }

    @Test
    fun emptySectionIsIncomplete() = assertFalse(AlertConditions(prices = emptySet()).isComplete)
}

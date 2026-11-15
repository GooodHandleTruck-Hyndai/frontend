package com.gooodhandletruck.zippick.ui

import com.gooodhandletruck.zippick.data.AppJson
import com.gooodhandletruck.zippick.data.AreaRange
import com.gooodhandletruck.zippick.data.ComplexType
import com.gooodhandletruck.zippick.data.PriceRange
import com.gooodhandletruck.zippick.data.model.NearbyResponse
import com.gooodhandletruck.zippick.ui.nearby.SELECTABLE_TYPES
import com.gooodhandletruck.zippick.ui.nearby.matchesFilter
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** S-03 '종류'·'필터'가 받은 단지를 어떻게 거르는지 본다. */
class NearbyFilterTest {

    // 단위 테스트는 app 모듈 폴더에서 돈다
    private val byId = AppJson.decodeFromString<NearbyResponse>(
        File("src/main/assets/mock/complexes_nearby.json").readText()
    ).items.associateBy { it.complexId }

    @Test
    fun nothingChosenKeepsEverything() {
        assertTrue(byId.values.all { it.matchesFilter(SELECTABLE_TYPES, emptySet(), emptySet()) })
    }

    @Test
    fun typeFilters() {
        val apartment = byId.getValue("c-1001")
        assertTrue(apartment.matchesFilter(setOf(ComplexType.APT), emptySet(), emptySet()))
        assertFalse(apartment.matchesFilter(setOf(ComplexType.OFFICETEL), emptySet(), emptySet()))
    }

    @Test
    fun priceRangeFilters() {
        val tenToFifteen = setOf(PriceRange.FROM_10_TO_15_EOK)
        assertTrue(byId.getValue("c-1001").matchesFilter(SELECTABLE_TYPES, tenToFifteen, emptySet())) // 12억 3,000만
        assertFalse(byId.getValue("c-1003").matchesFilter(SELECTABLE_TYPES, tenToFifteen, emptySet())) // 9억 8,500만
    }

    @Test
    fun noRecentAverageUsesLastTrade() {
        // 한솔 5단지는 최근 3개월 평균이 없고 마지막 거래가 6억 4,500만이다
        val fiveToTen = setOf(PriceRange.FROM_5_TO_10_EOK)
        assertTrue(byId.getValue("c-1002").matchesFilter(SELECTABLE_TYPES, fiveToTen, emptySet()))
    }

    @Test
    fun complexWithoutTradesIsDroppedOnlyWhenFiltering() {
        val noTrades = byId.getValue("c-1017")
        assertTrue(noTrades.matchesFilter(SELECTABLE_TYPES, emptySet(), emptySet()))
        assertFalse(noTrades.matchesFilter(SELECTABLE_TYPES, emptySet(), setOf(AreaRange.FROM_60_TO_85)))
    }
}

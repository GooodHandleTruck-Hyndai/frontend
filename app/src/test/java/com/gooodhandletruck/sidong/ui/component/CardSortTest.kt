package com.gooodhandletruck.sidong.ui.component

import com.gooodhandletruck.sidong.data.AppJson
import com.gooodhandletruck.sidong.data.model.NearbyResponse
import com.gooodhandletruck.sidong.ui.format.RelativeDirection
import com.gooodhandletruck.sidong.ui.format.RelativePlace
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/** 카드 정렬: 가격은 평균(없으면 마지막 거래), 거래 기록이 없는 단지와 거리를 모르는 카드는 늘 맨 뒤. */
class CardSortTest {

    // 단위 테스트는 app 모듈 폴더에서 돈다
    private val byId = AppJson.decodeFromString<NearbyResponse>(
        File("src/main/assets/mock/complexes_nearby.json").readText()
    ).items.associateBy { it.complexId }

    private fun card(id: String, meters: Double?) = ComplexCardModel(
        complex = byId.getValue(id),
        place = meters?.let { RelativePlace(RelativeDirection.FRONT, it) }
    )

    // 12억 3,000만 · 6억 4,500만(마지막 거래) · 9억 8,500만 · 거래 기록 없음
    private val cards =
        listOf(card("c-1001", 400.0), card("c-1002", 670.0), card("c-1003", 250.0), card("c-1017", null))

    @Test
    fun priceLowFirstAndNoPriceLast() =
        assertEquals(listOf("c-1002", "c-1003", "c-1001", "c-1017"), cards.sortedByPriceAscending().ids())

    @Test
    fun priceHighFirstAndNoPriceLast() =
        assertEquals(listOf("c-1001", "c-1003", "c-1002", "c-1017"), cards.sortedByPriceDescending().ids())

    @Test
    fun nearestFirstAndUnknownDistanceLast() =
        assertEquals(listOf("c-1003", "c-1001", "c-1002", "c-1017"), cards.sortedByDistance().ids())

    private fun List<ComplexCardModel>.ids() = map { it.complex.complexId }
}

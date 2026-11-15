package com.gooodhandletruck.zippick.ui.nearby

import com.gooodhandletruck.zippick.data.AreaRange
import com.gooodhandletruck.zippick.data.ComplexType
import com.gooodhandletruck.zippick.data.PriceRange
import com.gooodhandletruck.zippick.data.model.ComplexSummary
import com.gooodhandletruck.zippick.ui.component.ComplexCardModel

/** S-03 '종류'·'필터'로 받은 목록을 거른다. 반경만 서버에 보내고, 나머지는 받은 목록 안에서 거른다 */
internal fun List<ComplexCardModel>.filteredBy(state: NearbyUiState): List<ComplexCardModel> =
    filter { it.complex.matchesFilter(state.types, state.prices, state.areas) }

/**
 * 단지가 '종류'·가격대·면적에 맞는지. 가격·면적은 카드의 대표 평형으로 보고, 최근 3개월 평균이 없으면 마지막 거래 가격으로 본다.
 * 가격대나 면적을 하나도 고르지 않았으면 그 항목으로는 거르지 않는다. 골랐는데 가격이나 면적을 모르는 단지는 뺀다.
 * 앱이 모르는 유형은 '종류'로 거르지 않는다.
 */
internal fun ComplexSummary.matchesFilter(
    types: Set<ComplexType>,
    prices: Set<PriceRange>,
    areas: Set<AreaRange>
): Boolean {
    val complexType = ComplexType.of(type)
    if (complexType != null && complexType !in types) return false
    if (prices.isNotEmpty()) {
        val priceManwon = price?.let { it.avgPriceManwon ?: it.lastTrade?.priceManwon } ?: return false
        if (prices.none { it.contains(priceManwon) }) return false
    }
    if (areas.isNotEmpty()) {
        val areaM2 = price?.areaM2 ?: return false
        if (areas.none { it.contains(areaM2) }) return false
    }
    return true
}

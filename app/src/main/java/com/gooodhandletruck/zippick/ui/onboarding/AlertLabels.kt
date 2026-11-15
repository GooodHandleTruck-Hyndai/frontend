package com.gooodhandletruck.zippick.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gooodhandletruck.zippick.R
import com.gooodhandletruck.zippick.data.AlertConditions
import com.gooodhandletruck.zippick.data.AreaRange
import com.gooodhandletruck.zippick.data.ComplexType
import com.gooodhandletruck.zippick.data.PriceRange
import com.gooodhandletruck.zippick.ui.format.formatPrice

/*
 * 알림 조건 칩과 요약 문구. S-02 칩과 S-07 '알림 조건' 요약이 같이 쓴다.
 */

/** 유형 이름("아파트"·"오피스텔"·"원룸") */
@Composable
fun typeLabel(type: ComplexType): String = stringResource(
    // 시안(플로우_14)은 오피스텔·원룸에 "준비 중"을 붙이지 않는다. 고를 수 없는 칩은 누를 수 없게만 둔다
    when (type) {
        ComplexType.APT -> R.string.type_apartment
        ComplexType.OFFICETEL -> R.string.type_officetel
        ComplexType.ONE_ROOM -> R.string.type_one_room
    }
)

/** "5억 이하" · "5억~10억" · "15억 이상". 가격 숫자는 formatPrice로만 만든다 */
@Composable
fun priceRangeLabel(range: PriceRange): String {
    val min = range.minManwon
    val max = range.maxManwon
    return when {
        min == null && max != null -> stringResource(R.string.range_up_to, formatPrice(max))
        min != null && max == null -> stringResource(R.string.range_over, formatPrice(min))
        else -> stringResource(R.string.range_between, formatPrice(min ?: 0), formatPrice(max ?: 0))
    }
}

/** "60㎡ 이하" · "60~85㎡" · "85㎡ 이상" */
@Composable
fun areaRangeLabel(range: AreaRange): String {
    val min = range.minM2
    val max = range.maxM2
    return when {
        min == null && max != null -> stringResource(R.string.area_up_to, max)
        min != null && max == null -> stringResource(R.string.area_over, min)
        else -> stringResource(R.string.area_between, min ?: 0, max ?: 0)
    }
}

/**
 * S-07 '알림 조건' 요약: "아파트 · 5억~10억, 10억~15억 · 60~85㎡".
 * 가격대나 면적을 모두 골랐으면 "모든 가격대"·"모든 면적"이라고 쓴다.
 */
@Composable
fun conditionsSummary(conditions: AlertConditions): String {
    val types = ComplexType.entries.filter { it in conditions.types }.map { typeLabel(it) }
    val prices = if (conditions.prices.size == PriceRange.entries.size) {
        listOf(stringResource(R.string.settings_all_prices))
    } else {
        PriceRange.entries.filter { it in conditions.prices }.map { priceRangeLabel(it) }
    }
    val areas = if (conditions.areas.size == AreaRange.entries.size) {
        listOf(stringResource(R.string.settings_all_areas))
    } else {
        AreaRange.entries.filter { it in conditions.areas }.map { areaRangeLabel(it) }
    }
    return listOf(types, prices, areas).joinToString(" · ") { it.joinToString(", ") }
}

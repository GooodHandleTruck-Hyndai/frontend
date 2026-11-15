package com.gooodhandletruck.zippick.ui.nearby

import com.gooodhandletruck.zippick.data.AreaRange
import com.gooodhandletruck.zippick.data.CarLocation
import com.gooodhandletruck.zippick.data.ComplexType
import com.gooodhandletruck.zippick.data.PriceRange
import com.gooodhandletruck.zippick.ui.component.ComplexCardModel

/** S-03 범위 칩. 기본은 1km다. */
enum class NearbyRadius(val meters: Int) {
    M500(500),
    KM1(1000),
    KM3(3000);

    /** 한 단계 넓은 범위. 3km면 없다 */
    val wider: NearbyRadius?
        get() = entries.getOrNull(ordinal + 1)
}

/** S-03 정렬(Figma 시안 S-03 정렬 펼침). 기본은 가까운 순이다 */
enum class NearbySort { NEAREST, PRICE_LOW, RECENT_TRADE }

/** S-03 '종류'에서 고를 수 있는 유형(원룸은 아직 고를 수 없다). 처음에는 모두 고른 상태다 */
val SELECTABLE_TYPES: Set<ComplexType> = ComplexType.entries.filter { it.available }.toSet()

/**
 * S-03 화면 상태.
 * @property types '종류'에서 고른 유형. 여러 개를 고를 수 있고, 하나는 꼭 남는다
 * @property prices '필터'에서 고른 가격대. 비어 있으면 가격으로 거르지 않는다
 * @property areas '필터'에서 고른 면적. 비어 있으면 면적으로 거르지 않는다
 * @property listExpanded 지도 위 목록 패널을 펼쳤는지. '단지 N곳' 칩으로 접고 편다
 */
data class NearbyUiState(
    val radius: NearbyRadius = NearbyRadius.KM1,
    val sort: NearbySort = NearbySort.NEAREST,
    val types: Set<ComplexType> = SELECTABLE_TYPES,
    val prices: Set<PriceRange> = emptySet(),
    val areas: Set<AreaRange> = emptySet(),
    val listExpanded: Boolean = true,
    val content: NearbyContent = NearbyContent.Idle
) {
    /** '필터 N'의 N. 고른 가격대·면적 칩 수에 반경 1을 더한다(시안 "필터 3" = 가격대 1 + 면적 1 + 반경 1) */
    val filterCount: Int
        get() = prices.size + areas.size + 1

    /** 정렬이나 거르는 조건이 바뀌면 목록을 맨 위로 올리려고 쓰는 열쇠 */
    val listKey: String
        get() = "$sort|$types|$prices|$areas"
}

sealed interface NearbyContent {

    /** 아직 부르지 않았다. 위치 권한이 없으면 여기에 머문다 */
    data object Idle : NearbyContent

    /** 위치를 기다리거나 단지를 불러오는 중. 움직이지 않는 회색 자리 카드 3개를 보여 준다 */
    data object Loading : NearbyContent

    /** [cards]는 가까운 순 최대 10개, [totalCount]는 반경 안 전체 수다. [center]는 목록을 계산한 차 위치(약도 가운데)다 */
    data class Loaded(
        val totalCount: Int,
        val cards: List<ComplexCardModel>,
        val center: CarLocation
    ) : NearbyContent

    /** 지금 위치를 받지 못했다. [다시 시도]는 위치를 다시 묻는다 */
    data object LocationFailed : NearbyContent

    /** 단지 가격을 불러오지 못했다. [다시 시도]는 목록을 다시 부른다 */
    data object Failed : NearbyContent
}

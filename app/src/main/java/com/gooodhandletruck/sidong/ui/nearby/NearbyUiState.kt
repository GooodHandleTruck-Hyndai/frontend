package com.gooodhandletruck.sidong.ui.nearby

import com.gooodhandletruck.sidong.ui.component.ComplexCardModel

/** S-03 범위 칩. 기본은 1km다. */
enum class NearbyRadius(val meters: Int) {
    M500(500),
    KM1(1000),
    KM3(3000);

    /** 한 단계 넓은 범위. 3km면 없다 */
    val wider: NearbyRadius?
        get() = entries.getOrNull(ordinal + 1)
}

/** S-03 화면 상태 */
data class NearbyUiState(
    val radius: NearbyRadius = NearbyRadius.KM1,
    val content: NearbyContent = NearbyContent.Idle
)

sealed interface NearbyContent {

    /** 아직 부르지 않았다. 위치 권한이 없으면 여기에 머문다 */
    data object Idle : NearbyContent

    /** 위치를 기다리거나 단지를 불러오는 중. 움직이지 않는 회색 자리 카드 3개를 보여 준다 */
    data object Loading : NearbyContent

    /** [cards]는 가까운 순 최대 10개, [totalCount]는 반경 안 전체 수다 */
    data class Loaded(val totalCount: Int, val cards: List<ComplexCardModel>) : NearbyContent

    /** 지금 위치를 받지 못했다. [다시 시도]는 위치를 다시 묻는다 */
    data object LocationFailed : NearbyContent

    /** 단지 가격을 불러오지 못했다. [다시 시도]는 목록을 다시 부른다 */
    data object Failed : NearbyContent
}

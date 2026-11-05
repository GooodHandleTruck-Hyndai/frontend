package com.gooodhandletruck.sidong.ui.detail

import com.gooodhandletruck.sidong.data.model.AreaPrice
import com.gooodhandletruck.sidong.data.model.ComplexDetail
import com.gooodhandletruck.sidong.ui.format.RelativePlace

/** S-04 화면 상태 */
data class ComplexDetailUiState(
    /** 보여 주는 단지. 다른 단지를 열면 바로 바뀌고 [content]는 불러오는 중이 된다 */
    val complexId: String? = null,
    val content: DetailContent = DetailContent.Loading,
    /** 고른 평형 칩의 전용면적. 처음에는 대표 평형이다 */
    val selectedAreaM2: Double? = null,
    /** [저장] 버튼. 누르면 서버 답을 기다리지 않고 바로 바뀐다 */
    val saved: Boolean = false,
    /** 차에서 본 방향·거리. 위치를 아직 못 받았으면 null이고 칩을 숨긴다 */
    val place: RelativePlace? = null
) {
    /** 고른 평형의 가격. 거래 기록이 아예 없는 단지면 null이다 */
    val selectedArea: AreaPrice?
        get() = (content as? DetailContent.Loaded)?.detail?.areas?.firstOrNull { it.areaM2 == selectedAreaM2 }
}

sealed interface DetailContent {

    /** 불러오는 중. 움직이지 않는 회색 자리를 보여 준다 */
    data object Loading : DetailContent

    data class Loaded(val detail: ComplexDetail) : DetailContent

    /** 서버에 없는 단지다(COMPLEX_NOT_FOUND) */
    data object NotFound : DetailContent

    /** 불러오지 못했다. [다시 시도]는 다시 부른다 */
    data object Failed : DetailContent
}

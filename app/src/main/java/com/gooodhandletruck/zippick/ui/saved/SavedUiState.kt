package com.gooodhandletruck.zippick.ui.saved

import com.gooodhandletruck.zippick.data.ComplexType
import com.gooodhandletruck.zippick.ui.component.ComplexCardModel

/**
 * S-05 화면 상태(Figma 시안 저장_01).
 * @property type 위 유형 칩(아파트·오피스텔·원룸)에서 고른 것. 하나만 고르고, 처음에는 아파트다
 */
data class SavedUiState(
    val type: ComplexType = ComplexType.APT,
    val content: SavedContent = SavedContent.Loading
)

sealed interface SavedContent {

    /** 처음 불러오는 중. 움직이지 않는 회색 자리 카드 3개를 보여 준다 */
    data object Loading : SavedContent

    /** [cards]는 최근에 저장한 것부터다. 비어 있으면 빈 목록 상자를 보여 준다 */
    data class Loaded(val cards: List<ComplexCardModel>) : SavedContent

    /** 불러오지 못했다. [다시 시도]는 다시 부른다 */
    data object Failed : SavedContent
}

package com.gooodhandletruck.sidong.ui.saved

import com.gooodhandletruck.sidong.ui.component.ComplexCardModel

/** S-05 정렬(화면 설계서 S-05 '정렬·필터'). 기본은 저장한 순(최근에 저장한 것부터)이다 */
enum class SavedSort { SAVED, NEAREST, PRICE }

/** S-05 화면 상태 */
data class SavedUiState(
    val sort: SavedSort = SavedSort.SAVED,
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

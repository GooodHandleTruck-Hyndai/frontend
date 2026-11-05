package com.gooodhandletruck.sidong.ui.history

import com.gooodhandletruck.sidong.ui.component.ComplexCardModel

/** S-06 화면 상태 */
data class HistoryUiState(val content: HistoryContent = HistoryContent.Loading)

/** 오늘 들은 단지 한 줄: 들은 시각("14:32")과 단지 카드 */
data class HeardRow(val time: String, val card: ComplexCardModel)

sealed interface HistoryContent {

    /** 처음 불러오는 중. 움직이지 않는 회색 자리 카드 3개를 보여 준다 */
    data object Loading : HistoryContent

    /** [rows]는 최근에 들은 것부터다. 비어 있으면 빈 목록 상자를 보여 준다 */
    data class Loaded(val rows: List<HeardRow>) : HistoryContent

    /** 불러오지 못했다. [다시 시도]는 다시 부른다 */
    data object Failed : HistoryContent
}

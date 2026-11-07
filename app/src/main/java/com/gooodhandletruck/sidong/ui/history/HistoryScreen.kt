package com.gooodhandletruck.sidong.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gooodhandletruck.sidong.R
import com.gooodhandletruck.sidong.ui.WindowLayout
import com.gooodhandletruck.sidong.ui.component.CardTime
import com.gooodhandletruck.sidong.ui.component.ComplexCard
import com.gooodhandletruck.sidong.ui.component.LoadingCards
import com.gooodhandletruck.sidong.ui.component.StatusBox
import com.gooodhandletruck.sidong.ui.theme.AppColors
import com.gooodhandletruck.sidong.ui.theme.AppDimens
import com.gooodhandletruck.sidong.ui.theme.AppType

/**
 * S-06을 ViewModel에 잇는다. 들어올 때와 1:2 창에서 상세를 닫을 때 목록을 새로 받아 오고,
 * 위치 권한이 있으면 보이는 동안 차 위치를 따라 방향·거리 칩을 고친다.
 *
 * @param alertsOn 주행 중 자동 알림이 켜져 있는지. 빈 목록 문구가 달라진다
 */
@Composable
fun HistoryRoute(
    window: WindowLayout,
    locationGranted: Boolean,
    alertsOn: Boolean,
    selectedComplexId: String?,
    onOpenComplex: (String) -> Unit,
    onSetAlert: () -> Unit
) {
    val viewModel: HistoryViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(selectedComplexId == null) { viewModel.refresh() }
    LaunchedEffect(locationGranted, lifecycleOwner) {
        if (locationGranted) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.followLocation() }
        }
    }
    HistoryScreen(
        window = window,
        state = state,
        alertsOn = alertsOn,
        selectedComplexId = selectedComplexId,
        onOpenComplex = onOpenComplex,
        onSetAlert = onSetAlert,
        onRetry = viewModel::retry
    )
}

/**
 * S-06 오늘 들은 단지. 오늘 0시부터 주행 중에 소리로 들은 단지를 최근 것부터 보여 주고,
 * 카드 왼쪽 칸에 들은 시각("14:32")을 써서 하루 기록처럼 읽히게 한다(시안 D-05).
 * 저장한 단지는 카드에 "저장함"을 붙인다. 카드를 누르면 S-04가 열린다.
 * 빈 목록은 알림이 켜져 있으면 안내 한 줄만, 꺼져 있으면 [알림 조건 정하기](S-02)를 함께 보여 준다.
 */
@Composable
fun HistoryScreen(
    window: WindowLayout,
    state: HistoryUiState,
    alertsOn: Boolean,
    selectedComplexId: String?,
    onOpenComplex: (String) -> Unit,
    onSetAlert: () -> Unit,
    onRetry: () -> Unit
) {
    val content = state.content
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = AppDimens.ListPaneWidth + window.contentPadding * 2)
            .padding(
                start = window.contentPadding,
                end = window.contentPadding,
                top = AppDimens.ContentPaddingTop
            ),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceM)
    ) {
        Text(
            text = if (content is HistoryContent.Loaded && content.rows.isNotEmpty()) {
                stringResource(R.string.history_header_count, content.rows.size)
            } else {
                stringResource(R.string.screen_history)
            },
            style = AppType.DisplayM,
            color = AppColors.TextPrimary
        )
        when (content) {
            HistoryContent.Loading -> LoadingCards(modifier = Modifier.weight(1f))
            HistoryContent.Failed -> StatusBox(
                message = stringResource(R.string.status_load_failed),
                actionLabel = stringResource(R.string.action_retry),
                onAction = onRetry
            )
            is HistoryContent.Loaded -> if (content.rows.isEmpty()) {
                if (alertsOn) {
                    StatusBox(message = stringResource(R.string.history_empty_alerts_on))
                } else {
                    StatusBox(
                        message = stringResource(R.string.history_empty_alerts_off),
                        actionLabel = stringResource(R.string.action_set_alert),
                        onAction = onSetAlert
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceM),
                    contentPadding = PaddingValues(bottom = AppDimens.ContentPaddingVertical)
                ) {
                    items(content.rows, key = { it.card.complex.complexId }) { row ->
                        ComplexCard(
                            model = row.card,
                            onClick = { onOpenComplex(row.card.complex.complexId) },
                            selected = row.card.complex.complexId == selectedComplexId,
                            showSavedMark = true,
                            leading = { CardTime(time = row.time) }
                        )
                    }
                }
            }
        }
    }
}

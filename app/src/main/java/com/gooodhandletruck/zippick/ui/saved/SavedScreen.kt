package com.gooodhandletruck.zippick.ui.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import com.gooodhandletruck.zippick.R
import com.gooodhandletruck.zippick.data.ComplexType
import com.gooodhandletruck.zippick.ui.WindowLayout
import com.gooodhandletruck.zippick.ui.component.ComplexCard
import com.gooodhandletruck.zippick.ui.component.LoadingCards
import com.gooodhandletruck.zippick.ui.component.StatusBox
import com.gooodhandletruck.zippick.ui.component.ToggleChip
import com.gooodhandletruck.zippick.ui.component.rememberSortedListState
import com.gooodhandletruck.zippick.ui.onboarding.typeLabel
import com.gooodhandletruck.zippick.ui.theme.AppColors
import com.gooodhandletruck.zippick.ui.theme.AppDimens
import com.gooodhandletruck.zippick.ui.theme.AppType

/**
 * S-05를 ViewModel에 잇는다. 들어올 때와 1:2 창에서 상세를 닫을 때 목록을 새로 받아 오고,
 * 위치 권한이 있으면 보이는 동안 차 위치를 따라 방향·거리 줄을 고친다.
 */
@Composable
fun SavedRoute(
    window: WindowLayout,
    locationGranted: Boolean,
    selectedComplexId: String?,
    onOpenComplex: (String) -> Unit,
    onGoNearby: () -> Unit
) {
    val viewModel: SavedViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(selectedComplexId == null) { viewModel.refresh() }
    LaunchedEffect(locationGranted, lifecycleOwner) {
        if (locationGranted) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.followLocation() }
        }
    }
    SavedScreen(
        window = window,
        state = state,
        selectedComplexId = selectedComplexId,
        onOpenComplex = onOpenComplex,
        onGoNearby = onGoNearby,
        onSelectType = viewModel::selectType,
        onRetry = viewModel::retry
    )
}

/**
 * S-05 저장한 매물(Figma 시안 저장_01). 제목 "저장한 매물", 유형 칩(아파트·오피스텔·원룸), 저장한 순 단지 목록이다.
 * 카드는 바탕 없는 목록 줄이고, 오른쪽 칸에 상세를 연 카드만 옅은 강조색 바탕이다. 카드를 누르면 S-04가 열린다.
 * 1:2 창에서는 왼쪽에 목록을 두고 오른쪽 칸에 상세를 연다(AppRoot). 새 거래 알림은 머리글 종에서 연다.
 */
@Composable
fun SavedScreen(
    window: WindowLayout,
    state: SavedUiState,
    selectedComplexId: String?,
    onOpenComplex: (String) -> Unit,
    onGoNearby: () -> Unit,
    onSelectType: (ComplexType) -> Unit,
    onRetry: () -> Unit
) {
    val content = state.content
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = AppDimens.ListPaneWidth + window.contentPadding * 2)
            .padding(start = AppDimens.SpaceL, end = AppDimens.SpaceS, top = AppDimens.SpaceM),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceS)
    ) {
        Text(
            text = stringResource(R.string.screen_saved),
            style = AppType.Title2,
            color = AppColors.TextPrimary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceXs)) {
            ComplexType.entries.forEach { type ->
                ToggleChip(
                    text = typeLabel(type),
                    checked = type == state.type,
                    onCheckedChange = { onSelectType(type) },
                    enabled = type.available
                )
            }
        }
        when (content) {
            SavedContent.Loading -> LoadingCards(modifier = Modifier.weight(1f))
            SavedContent.Failed -> StatusBox(
                message = stringResource(R.string.status_load_failed),
                actionLabel = stringResource(R.string.action_retry),
                onAction = onRetry
            )
            is SavedContent.Loaded -> {
                val cards = content.cards.filter { ComplexType.of(it.complex.type) == state.type }
                when {
                    content.cards.isEmpty() -> StatusBox(
                        message = stringResource(R.string.saved_empty),
                        detail = stringResource(R.string.saved_empty_detail),
                        actionLabel = stringResource(R.string.action_go_nearby),
                        onAction = onGoNearby
                    )
                    cards.isEmpty() -> StatusBox(
                        message = stringResource(R.string.saved_type_empty, typeLabel(state.type))
                    )
                    else -> LazyColumn(
                        modifier = Modifier.weight(1f),
                        state = rememberSortedListState(state.type.name),
                        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceXs),
                        contentPadding = PaddingValues(bottom = AppDimens.ContentPaddingVertical)
                    ) {
                        // 서버가 최근에 저장한 것부터 준다
                        items(cards, key = { it.complex.complexId }) { card ->
                            ComplexCard(
                                model = card,
                                onClick = { onOpenComplex(card.complex.complexId) },
                                selected = card.complex.complexId == selectedComplexId,
                                showTypeTag = true,
                                flat = true
                            )
                        }
                    }
                }
            }
        }
    }
}

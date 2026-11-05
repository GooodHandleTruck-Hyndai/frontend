package com.gooodhandletruck.sidong.ui.saved

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
import com.gooodhandletruck.sidong.data.RouteTarget
import com.gooodhandletruck.sidong.data.toRouteTarget
import com.gooodhandletruck.sidong.ui.WindowLayout
import com.gooodhandletruck.sidong.ui.component.ComplexCard
import com.gooodhandletruck.sidong.ui.component.ComplexCardModel
import com.gooodhandletruck.sidong.ui.component.LoadingCards
import com.gooodhandletruck.sidong.ui.component.SecondaryButton
import com.gooodhandletruck.sidong.ui.component.SortChip
import com.gooodhandletruck.sidong.ui.component.StatusBox
import com.gooodhandletruck.sidong.ui.component.rememberSortedListState
import com.gooodhandletruck.sidong.ui.component.sortedByDistance
import com.gooodhandletruck.sidong.ui.component.sortedByPriceAscending
import com.gooodhandletruck.sidong.ui.theme.AppColors
import com.gooodhandletruck.sidong.ui.theme.AppDimens
import com.gooodhandletruck.sidong.ui.theme.AppType

/**
 * S-05를 ViewModel에 잇는다. 들어올 때와 1:2 창에서 상세를 닫을 때 목록을 새로 받아 오고,
 * 위치 권한이 있으면 보이는 동안 차 위치를 따라 방향·거리 칩을 고친다.
 */
@Composable
fun SavedRoute(
    window: WindowLayout,
    locationGranted: Boolean,
    selectedComplexId: String?,
    onOpenComplex: (String) -> Unit,
    onStartRoute: (RouteTarget) -> Unit,
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
        onStartRoute = onStartRoute,
        onGoNearby = onGoNearby,
        onSelectSort = viewModel::selectSort,
        onRetry = viewModel::retry
    )
}

/**
 * S-05 저장한 단지. 머리글 "저장한 단지 · 8곳"과 저장한 순 단지 카드 목록이다. 카드를 누르면 S-04가 열리고,
 * 카드 오른쪽 끝 [길 안내]는 S-04를 거치지 않고 바로 길 안내를 시작한다.
 * 1:2 창에서는 왼쪽 640에 목록을 두고 오른쪽 칸에 상세를 연다.
 * 정렬은 저장한 순(기본)·가까운 순·가격 순이다.
 * TODO: 유형 필터는 오피스텔·원룸 데이터가 생기면 더한다. 지금은 아파트만 있어 거를 것이 없다
 */
@Composable
fun SavedScreen(
    window: WindowLayout,
    state: SavedUiState,
    selectedComplexId: String?,
    onOpenComplex: (String) -> Unit,
    onStartRoute: (RouteTarget) -> Unit,
    onGoNearby: () -> Unit,
    onSelectSort: (SavedSort) -> Unit,
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
                top = AppDimens.ContentPaddingVertical
            ),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceM)
    ) {
        Text(
            // 빈 목록이면 "· 0곳" 없이 제목만 쓴다
            text = if (content is SavedContent.Loaded && content.cards.isNotEmpty()) {
                stringResource(R.string.saved_header_count, content.cards.size)
            } else {
                stringResource(R.string.screen_saved)
            },
            style = AppType.TitleS,
            color = AppColors.TextPrimary
        )
        if (content is SavedContent.Loaded && content.cards.size > 1) {
            SortChip(
                selected = state.sort,
                options = SavedSort.entries,
                label = { sortLabel(it) },
                onSelect = onSelectSort
            )
        }
        when (content) {
            SavedContent.Loading -> LoadingCards(modifier = Modifier.weight(1f))
            SavedContent.Failed -> StatusBox(
                message = stringResource(R.string.status_load_failed),
                actionLabel = stringResource(R.string.action_retry),
                onAction = onRetry
            )
            is SavedContent.Loaded -> if (content.cards.isEmpty()) {
                StatusBox(
                    message = stringResource(R.string.saved_empty),
                    detail = stringResource(R.string.saved_empty_detail),
                    actionLabel = stringResource(R.string.action_go_nearby),
                    onAction = onGoNearby
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = rememberSortedListState(state.sort.name),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceS),
                    contentPadding = PaddingValues(bottom = AppDimens.ContentPaddingVertical)
                ) {
                    items(content.cards.sortedBy(state.sort), key = { it.complex.complexId }) { card ->
                        ComplexCard(
                            model = card,
                            onClick = { onOpenComplex(card.complex.complexId) },
                            selected = card.complex.complexId == selectedComplexId,
                            trailing = {
                                SecondaryButton(
                                    text = stringResource(R.string.action_route),
                                    onClick = { onStartRoute(card.complex.toRouteTarget()) },
                                    height = AppDimens.TouchMin,
                                    horizontalPadding = AppDimens.SpaceXs
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun List<ComplexCardModel>.sortedBy(sort: SavedSort): List<ComplexCardModel> = when (sort) {
    SavedSort.SAVED -> this // 서버가 최근에 저장한 것부터 준다
    SavedSort.NEAREST -> sortedByDistance()
    SavedSort.PRICE -> sortedByPriceAscending()
}

@Composable
private fun sortLabel(sort: SavedSort): String = stringResource(
    when (sort) {
        SavedSort.SAVED -> R.string.sort_saved
        SavedSort.NEAREST -> R.string.sort_nearest
        SavedSort.PRICE -> R.string.sort_price
    }
)

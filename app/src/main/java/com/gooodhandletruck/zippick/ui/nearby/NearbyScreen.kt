package com.gooodhandletruck.zippick.ui.nearby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectableGroup
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
import com.gooodhandletruck.zippick.ui.WindowLayout
import com.gooodhandletruck.zippick.ui.component.ComplexCard
import com.gooodhandletruck.zippick.ui.component.ComplexCardModel
import com.gooodhandletruck.zippick.ui.component.LoadingCards
import com.gooodhandletruck.zippick.ui.component.NumberBadge
import com.gooodhandletruck.zippick.ui.component.SelectChip
import com.gooodhandletruck.zippick.ui.component.SortChip
import com.gooodhandletruck.zippick.ui.component.StatusBox
import com.gooodhandletruck.zippick.ui.component.rememberLocationPermissionRequest
import com.gooodhandletruck.zippick.ui.component.rememberSortedListState
import com.gooodhandletruck.zippick.ui.component.sortedByDistance
import com.gooodhandletruck.zippick.ui.component.sortedByPriceAscending
import com.gooodhandletruck.zippick.ui.component.sortedByPriceDescending
import com.gooodhandletruck.zippick.ui.format.formatDistance
import com.gooodhandletruck.zippick.ui.theme.AppColors
import com.gooodhandletruck.zippick.ui.theme.AppDimens
import com.gooodhandletruck.zippick.ui.theme.AppType

/**
 * S-03을 ViewModel에 잇는다. 위치 권한이 있고 이 화면이 앞에 보이는 동안만 차 위치를 따라간다.
 * 다른 탭·주행 화면(S-08)으로 바뀌거나 앱이 뒤로 가면 따라가기를 멈춘다.
 * 1:2 창에서는 오른쪽 칸에 상세를 연 단지([selectedComplexId])의 카드를 강조한다.
 */
@Composable
fun NearbyRoute(
    window: WindowLayout,
    locationGranted: Boolean,
    selectedComplexId: String?,
    onLocationPermissionResult: () -> Unit,
    onOpenComplex: (String) -> Unit
) {
    val viewModel: NearbyViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val requestPermission = rememberLocationPermissionRequest { onLocationPermissionResult() }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(locationGranted, lifecycleOwner) {
        if (locationGranted) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.followLocation() }
        }
    }
    NearbyScreen(
        window = window,
        state = state,
        locationGranted = locationGranted,
        selectedComplexId = selectedComplexId,
        onRequestLocationPermission = requestPermission,
        onSelectRadius = viewModel::selectRadius,
        onSelectSort = viewModel::selectSort,
        onRetry = viewModel::retry,
        onOpenComplex = onOpenComplex
    )
}

/**
 * 1:2 창에서 S-03 목록 옆 칸의 약도. 목록을 불러온 뒤에만 그리고, 카드나 핀을 누르면 이 자리에 S-04가 열린다.
 * 핀 번호는 왼쪽 목록의 번호와 같도록 목록과 같은 정렬로 매긴다.
 */
@Composable
fun NearbyMapPane(window: WindowLayout, onOpenComplex: (String) -> Unit) {
    val viewModel: NearbyViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val content = state.content as? NearbyContent.Loaded ?: return
    if (content.cards.isEmpty()) return
    NearbyMap(
        center = content.center,
        cards = content.cards.sortedBy(state.sort),
        radius = state.radius,
        selectedComplexId = null,
        onOpenComplex = onOpenComplex,
        modifier = Modifier
            .fillMaxSize()
            .padding(
                end = window.contentPadding,
                top = AppDimens.ContentPaddingTop,
                bottom = AppDimens.ContentPaddingVertical
            )
    )
}

/**
 * S-03 주변 단지. 정차 중 홈이다. 머리글, 범위 칩(500m·1km·3km), 단지 카드 최대 10개를 보여 준다.
 * 1:2 창에서는 왼쪽 640에 목록을 두고 오른쪽 칸에 약도를 둔다(카드를 누르면 약도 자리에 단지 상세가 열린다).
 * 1:1:1 창에서는 목록 맨 위에 약도(높이 480)를 둔다.
 * 카드 왼쪽 번호 배지와 약도 핀 번호는 같은 단지끼리 같다(시안 D-02). 정렬을 바꾸면 번호도 다시 매긴다.
 * 위치 권한이 없으면 목록 대신 상태 상자의 [위치 권한 허용]으로 권한 창을 다시 띄운다.
 * 위치를 받지 못하면 "지금 위치를 확인하지 못했어요 · [다시 시도]"를 보여 준다.
 * TODO: "허용하지 않고 다시 묻지 않음"을 고른 뒤에는 창이 뜨지 않아 버튼이 아무 일도 하지 않는다.
 *  그때 보여 줄 문구는 화면 설계서 '정할 것' 11번에서 정한다.
 */
@Composable
fun NearbyScreen(
    window: WindowLayout,
    state: NearbyUiState,
    locationGranted: Boolean,
    selectedComplexId: String?,
    onRequestLocationPermission: () -> Unit,
    onSelectRadius: (NearbyRadius) -> Unit,
    onSelectSort: (NearbySort) -> Unit,
    onRetry: () -> Unit,
    onOpenComplex: (String) -> Unit
) {
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
        NearbyHeader(
            radius = state.radius,
            totalCount = (state.content as? NearbyContent.Loaded)?.totalCount,
            showRange = locationGranted
        )
        if (locationGranted) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceS)) {
                RangeChips(selected = state.radius, onSelect = onSelectRadius)
                SortChip(
                    selected = state.sort,
                    options = NearbySort.entries,
                    label = { sortLabel(it) },
                    onSelect = onSelectSort
                )
            }
            NearbyContentArea(
                state = state,
                showMap = !window.isWide,
                selectedComplexId = selectedComplexId,
                onSelectRadius = onSelectRadius,
                onRetry = onRetry,
                onOpenComplex = onOpenComplex,
                modifier = Modifier.weight(1f)
            )
        } else {
            StatusBox(
                message = stringResource(R.string.status_location_denied),
                actionLabel = stringResource(R.string.action_allow_location),
                onAction = onRequestLocationPermission
            )
        }
    }
}

/** "주변 단지"(title2)와 "내 위치에서 1km 안 · 12곳"(body2) */
@Composable
private fun NearbyHeader(radius: NearbyRadius, totalCount: Int?, showRange: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceXs)) {
        Text(text = stringResource(R.string.screen_nearby), style = AppType.Title2, color = AppColors.TextPrimary)
        if (showRange) {
            val range = if (totalCount != null) {
                stringResource(R.string.nearby_header_count, radius.label, totalCount)
            } else {
                stringResource(R.string.nearby_header_range, radius.label)
            }
            Text(text = range, style = AppType.Body2, color = AppColors.TextTertiary)
        }
    }
}

/** 범위 칩 500m · 1km · 3km. 하나만 고를 수 있고, 고른 칩은 강조색이다 */
@Composable
private fun RangeChips(selected: NearbyRadius, onSelect: (NearbyRadius) -> Unit) {
    Row(
        modifier = Modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceS)
    ) {
        NearbyRadius.entries.forEach { radius ->
            SelectChip(text = radius.label, selected = radius == selected, onClick = { onSelect(radius) })
        }
    }
}

/** 불러오는 중·오류·빈 목록·단지 카드 목록 가운데 하나를 그린다 (화면 설계서 S-03 '상태별 문구') */
@Composable
private fun NearbyContentArea(
    state: NearbyUiState,
    showMap: Boolean,
    selectedComplexId: String?,
    onSelectRadius: (NearbyRadius) -> Unit,
    onRetry: () -> Unit,
    onOpenComplex: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when (val content = state.content) {
        NearbyContent.Idle, NearbyContent.Loading -> LoadingCards(
            modifier = modifier,
            message = stringResource(R.string.nearby_loading)
        )
        NearbyContent.LocationFailed -> StatusBox(
            message = stringResource(R.string.status_location_unavailable),
            actionLabel = stringResource(R.string.action_retry),
            onAction = onRetry
        )
        NearbyContent.Failed -> StatusBox(
            message = stringResource(R.string.status_load_failed),
            actionLabel = stringResource(R.string.action_retry),
            onAction = onRetry
        )
        is NearbyContent.Loaded -> if (content.cards.isEmpty()) {
            val wider = state.radius.wider
            StatusBox(
                message = stringResource(R.string.nearby_empty, state.radius.label),
                actionLabel = wider?.let { stringResource(R.string.action_widen_range, it.label) },
                onAction = { wider?.let(onSelectRadius) }
            )
        } else {
            val cards = content.cards.sortedBy(state.sort)
            LazyColumn(
                modifier = modifier,
                state = rememberSortedListState(state.sort.name),
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceS),
                contentPadding = PaddingValues(bottom = AppDimens.ContentPaddingVertical)
            ) {
                if (showMap) {
                    item(key = MAP_ITEM_KEY) {
                        NearbyMap(
                            center = content.center,
                            cards = cards,
                            radius = state.radius,
                            selectedComplexId = selectedComplexId,
                            onOpenComplex = onOpenComplex,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(AppDimens.NarrowMapHeight)
                        )
                    }
                }
                itemsIndexed(cards, key = { _, card -> card.complex.complexId }) { index, card ->
                    val selected = card.complex.complexId == selectedComplexId
                    ComplexCard(
                        model = card,
                        onClick = { onOpenComplex(card.complex.complexId) },
                        selected = selected,
                        leading = { NumberBadge(number = index + 1, filled = selected) }
                    )
                }
            }
        }
    }
}

private const val MAP_ITEM_KEY = "map"

private fun List<ComplexCardModel>.sortedBy(sort: NearbySort): List<ComplexCardModel> = when (sort) {
    NearbySort.NEAREST -> sortedByDistance()
    NearbySort.PRICE_LOW -> sortedByPriceAscending()
    NearbySort.PRICE_HIGH -> sortedByPriceDescending()
}

@Composable
private fun sortLabel(sort: NearbySort): String = stringResource(
    when (sort) {
        NearbySort.NEAREST -> R.string.sort_nearest
        NearbySort.PRICE_LOW -> R.string.sort_price_low
        NearbySort.PRICE_HIGH -> R.string.sort_price_high
    }
)

/** 범위 이름: "500m", "1km", "3km" */
private val NearbyRadius.label: String
    get() = formatDistance(meters.toDouble())

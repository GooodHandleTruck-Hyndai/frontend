package com.gooodhandletruck.zippick.ui.nearby

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gooodhandletruck.zippick.R
import com.gooodhandletruck.zippick.data.AreaRange
import com.gooodhandletruck.zippick.data.ComplexType
import com.gooodhandletruck.zippick.data.PriceRange
import com.gooodhandletruck.zippick.ui.WindowLayout
import com.gooodhandletruck.zippick.ui.component.ComplexCard
import com.gooodhandletruck.zippick.ui.component.ComplexCardModel
import com.gooodhandletruck.zippick.ui.component.LoadingCards
import com.gooodhandletruck.zippick.ui.component.SortChip
import com.gooodhandletruck.zippick.ui.component.StatusBox
import com.gooodhandletruck.zippick.ui.component.rememberLocationPermissionRequest
import com.gooodhandletruck.zippick.ui.component.rememberSortedListState
import com.gooodhandletruck.zippick.ui.component.sortedByDistance
import com.gooodhandletruck.zippick.ui.component.sortedByPriceAscending
import com.gooodhandletruck.zippick.ui.component.sortedByRecentTrade
import com.gooodhandletruck.zippick.ui.format.formatDistance
import com.gooodhandletruck.zippick.ui.theme.AppColors
import com.gooodhandletruck.zippick.ui.theme.AppDimens
import com.gooodhandletruck.zippick.ui.theme.AppType
import kotlin.math.roundToInt

// Figma 시안 S-03의 크기. 시안 캡처가 작아 대략 잰 값이다. TODO: Dev Mode 값과 다르면 여기만 고친다
private val LIST_PANEL_WIDTH = 688.dp // 지도 위 목록 패널 너비. 좁은 창에서는 판 너비에 맞춘다
private val SUMMARY_PILL_HEIGHT = 56.dp // 아래 가운데 요약 알약 높이
private val LIST_BOTTOM_SPACE = 104.dp // 목록 패널 아래를 요약 알약 자리만큼 비운다

/**
 * S-03을 ViewModel에 잇는다. 위치 권한이 있고 이 화면이 앞에 보이는 동안만 차 위치를 따라간다.
 * 다른 탭·주행 화면(S-08)으로 바뀌거나 앱이 뒤로 가면 따라가기를 멈춘다.
 * 1:2 창에서는 오른쪽에 상세를 연 단지([selectedComplexId])의 카드와 지도 핀을 강조한다.
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
        actions = NearbyActions(
            onRequestLocationPermission = requestPermission,
            onSelectRadius = viewModel::selectRadius,
            onToggleType = viewModel::toggleType,
            onTogglePrice = viewModel::togglePrice,
            onToggleArea = viewModel::toggleArea,
            onResetFilters = viewModel::resetFilters,
            onSelectSort = viewModel::selectSort,
            onToggleList = viewModel::toggleList,
            onRetry = viewModel::retry,
            onOpenComplex = onOpenComplex
        )
    )
}

/** S-03에서 누를 수 있는 것들. 인자가 많아 한데 묶는다 */
class NearbyActions(
    val onRequestLocationPermission: () -> Unit,
    val onSelectRadius: (NearbyRadius) -> Unit,
    val onToggleType: (ComplexType) -> Unit,
    val onTogglePrice: (PriceRange) -> Unit,
    val onToggleArea: (AreaRange) -> Unit,
    val onResetFilters: () -> Unit,
    val onSelectSort: (NearbySort) -> Unit,
    val onToggleList: () -> Unit,
    val onRetry: () -> Unit,
    val onOpenComplex: (String) -> Unit
)

/**
 * S-03 주변 단지(Figma 시안). 머리글 아래 둥근 판 전체에 약도를 깔고, 그 위 왼쪽에 칩 줄과 목록 패널을 띄운다.
 * - 칩 줄: [단지 N곳 ⌄](목록 패널 접기·펴기) · [아파트, 오피스텔 ⌄](종류 펼침) · [필터 N ⌄](가격대·면적·반경 펼침)
 * - 목록 패널: "총 N개 단지"와 정렬 칩(가까운 순·가격 낮은 순·최근 거래 순), 단지 카드
 * - 아래 가운데: "단지 N곳 · 내 위치 1km 안" 요약 알약
 * 카드나 핀을 누르면 S-04가 열린다. 1:2 창에서는 오른쪽에 상세 패널이 뜨고(AppRoot), 1:1:1 창에서는 창을 덮는다.
 */
@Composable
fun NearbyScreen(
    window: WindowLayout,
    state: NearbyUiState,
    locationGranted: Boolean,
    selectedComplexId: String?,
    actions: NearbyActions
) {
    val content = state.content
    val received = (content as? NearbyContent.Loaded)?.cards
    val cards = received?.filteredBy(state)?.sortedBy(state.sort)
    var openMenu by remember { mutableStateOf(MapMenu.NONE) }
    var panelX by remember { mutableIntStateOf(0) }
    val chipX = remember { mutableStateMapOf<MapMenu, Int>() }
    var chipRowHeight by remember { mutableIntStateOf(0) }
    val belowChips = with(LocalDensity.current) { chipRowHeight.toDp() } + AppDimens.SpaceS * 2
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(end = AppDimens.PanelInset, bottom = AppDimens.PanelInset)
            .clip(RoundedCornerShape(AppDimens.PanelRadius))
            .background(AppColors.Panel)
            .onPlaced { panelX = it.positionInRoot().x.roundToInt() }
    ) {
        if (content is NearbyContent.Loaded && cards != null) {
            NearbyMap(
                center = content.center,
                cards = cards,
                radius = state.radius,
                selectedComplexId = selectedComplexId,
                onOpenComplex = actions.onOpenComplex,
                modifier = Modifier.fillMaxSize(),
                framed = false
            )
        }
        if (!locationGranted) {
            StatusBox(
                message = stringResource(R.string.status_location_denied),
                actionLabel = stringResource(R.string.action_allow_location),
                onAction = actions.onRequestLocationPermission,
                modifier = Modifier
                    .widthIn(max = LIST_PANEL_WIDTH)
                    .padding(AppDimens.SpaceS)
            )
            return@Box
        }
        if (state.listExpanded) {
            ListPanel(
                state = state,
                received = received,
                cards = cards,
                selectedComplexId = selectedComplexId,
                actions = actions,
                modifier = Modifier
                    .widthIn(max = LIST_PANEL_WIDTH + AppDimens.SpaceS * 2)
                    .padding(
                        start = AppDimens.SpaceS,
                        end = AppDimens.SpaceS,
                        top = belowChips,
                        bottom = if (cards != null) LIST_BOTTOM_SPACE else AppDimens.SpaceS
                    )
            )
        }
        if (cards != null) {
            SummaryPill(
                text = stringResource(R.string.nearby_summary_pill, cards.size, state.radius.label),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = AppDimens.SpaceM)
            )
        }
        if (openMenu != MapMenu.NONE) {
            // 펼침 바깥(칩 줄은 빼고)을 누르면 닫는다. 지도 핀도 이때는 눌리지 않는다
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { openMenu = MapMenu.NONE }
                    )
            )
        }
        Row(
            modifier = Modifier
                .padding(AppDimens.SpaceS)
                .onSizeChanged { chipRowHeight = it.height },
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceS)
        ) {
            if (cards != null) {
                MapChip(
                    text = stringResource(R.string.nearby_list_chip, cards.size),
                    expanded = state.listExpanded,
                    icon = R.drawable.ic_building,
                    onClick = {
                        openMenu = MapMenu.NONE
                        actions.onToggleList()
                    }
                )
            }
            MapMenu.entries.filter { it != MapMenu.NONE }.forEach { menu ->
                MapChip(
                    text = when (menu) {
                        MapMenu.TYPE -> typesChipText(state.types)
                        else -> stringResource(R.string.nearby_filter_count, state.filterCount)
                    },
                    expanded = openMenu == menu,
                    icon = if (menu == MapMenu.FILTER) R.drawable.ic_filter else null,
                    onClick = { openMenu = if (openMenu == menu) MapMenu.NONE else menu },
                    modifier = Modifier.onPlaced { chipX[menu] = it.positionInRoot().x.roundToInt() }
                )
            }
        }
        val anchorX = (chipX[openMenu] ?: 0) - panelX
        // 칩 줄 바로 아래(8dp 띄움)에 펼친다
        val menuModifier = Modifier.dropdownBelow(
            top = belowChips - AppDimens.SpaceXs,
            anchorX = anchorX,
            margin = AppDimens.SpaceS
        )
        when (openMenu) {
            MapMenu.TYPE -> TypeMenu(types = state.types, onToggleType = actions.onToggleType, modifier = menuModifier)
            MapMenu.FILTER -> FilterPopover(
                state = state,
                onTogglePrice = actions.onTogglePrice,
                onToggleArea = actions.onToggleArea,
                onSelectRadius = actions.onSelectRadius,
                onReset = actions.onResetFilters,
                modifier = menuModifier
            )
            MapMenu.NONE -> Unit
        }
    }
}

/** 목록 패널: "총 N개 단지"와 정렬 칩, 그 아래 단지 카드. 불러오는 중·빈 목록·오류도 이 패널 안에 쓴다 */
@Composable
private fun ListPanel(
    state: NearbyUiState,
    received: List<ComplexCardModel>?,
    cards: List<ComplexCardModel>?,
    selectedComplexId: String?,
    actions: NearbyActions,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.CardRadius))
            .background(AppColors.Surface)
            .padding(AppDimens.SpaceS),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceS)
    ) {
        when (state.content) {
            NearbyContent.Idle, NearbyContent.Loading -> LoadingCards(message = stringResource(R.string.nearby_loading))
            NearbyContent.LocationFailed -> StatusBox(
                message = stringResource(R.string.status_location_unavailable),
                actionLabel = stringResource(R.string.action_retry),
                onAction = actions.onRetry
            )
            NearbyContent.Failed -> StatusBox(
                message = stringResource(R.string.status_load_failed),
                actionLabel = stringResource(R.string.action_retry),
                onAction = actions.onRetry
            )
            is NearbyContent.Loaded -> if (cards.isNullOrEmpty() && !received.isNullOrEmpty()) {
                // 받은 단지는 있는데 '종류'·'필터'로 모두 걸렀다
                StatusBox(
                    message = stringResource(R.string.nearby_filter_empty),
                    actionLabel = stringResource(R.string.filter_reset_action),
                    onAction = actions.onResetFilters
                )
            } else if (cards.isNullOrEmpty()) {
                val wider = state.radius.wider
                StatusBox(
                    message = stringResource(R.string.nearby_empty, state.radius.label),
                    actionLabel = wider?.let { stringResource(R.string.action_widen_range, it.label) },
                    onAction = { wider?.let(actions.onSelectRadius) }
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.nearby_total, cards.size),
                        style = AppType.Label1Medium,
                        color = AppColors.TextSecondary,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = AppDimens.SpaceXs)
                    )
                    SortChip(
                        selected = state.sort,
                        options = NearbySort.entries,
                        label = { sortLabel(it) },
                        onSelect = actions.onSelectSort,
                        background = AppColors.Surface
                    )
                }
                LazyColumn(
                    state = rememberSortedListState(state.listKey),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceS),
                    contentPadding = PaddingValues(bottom = AppDimens.SpaceXs)
                ) {
                    items(cards, key = { it.complex.complexId }) { card ->
                        ComplexCard(
                            model = card,
                            onClick = { actions.onOpenComplex(card.complex.complexId) },
                            selected = card.complex.complexId == selectedComplexId,
                            showTypeTag = true
                        )
                    }
                }
            }
        }
    }
}

/** 아래 가운데 요약 알약 "단지 9곳 · 내 위치 1km 안"(label1Medium). 누를 수 없다 */
@Composable
private fun SummaryPill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = SUMMARY_PILL_HEIGHT)
            .clip(CircleShape)
            .background(AppColors.Surface)
            .padding(horizontal = AppDimens.SpaceM),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = AppType.Label1Medium, color = AppColors.TextPrimary)
    }
}

private fun List<ComplexCardModel>.sortedBy(sort: NearbySort): List<ComplexCardModel> = when (sort) {
    NearbySort.NEAREST -> sortedByDistance()
    NearbySort.PRICE_LOW -> sortedByPriceAscending()
    NearbySort.RECENT_TRADE -> sortedByRecentTrade()
}

@Composable
private fun sortLabel(sort: NearbySort): String = stringResource(
    when (sort) {
        NearbySort.NEAREST -> R.string.sort_nearest
        NearbySort.PRICE_LOW -> R.string.sort_price_low
        NearbySort.RECENT_TRADE -> R.string.sort_recent_trade
    }
)

private val NearbyRadius.label: String
    get() = formatDistance(meters.toDouble())

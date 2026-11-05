package com.gooodhandletruck.sidong.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gooodhandletruck.sidong.R
import com.gooodhandletruck.sidong.data.RouteTarget
import com.gooodhandletruck.sidong.data.model.AreaPrice
import com.gooodhandletruck.sidong.data.model.ComplexDetail
import com.gooodhandletruck.sidong.data.model.Trade
import com.gooodhandletruck.sidong.data.toRouteTarget
import com.gooodhandletruck.sidong.ui.WindowLayout
import com.gooodhandletruck.sidong.ui.component.ComplexPhoto
import com.gooodhandletruck.sidong.ui.component.DirectionChip
import com.gooodhandletruck.sidong.ui.component.PrimaryButton
import com.gooodhandletruck.sidong.ui.component.SecondaryButton
import com.gooodhandletruck.sidong.ui.component.SelectChip
import com.gooodhandletruck.sidong.ui.component.StatusBox
import com.gooodhandletruck.sidong.ui.component.TagChip
import com.gooodhandletruck.sidong.ui.format.RelativePlace
import com.gooodhandletruck.sidong.ui.format.formatArea
import com.gooodhandletruck.sidong.ui.format.formatPrice
import com.gooodhandletruck.sidong.ui.navigation.Screen
import com.gooodhandletruck.sidong.ui.theme.AppColors
import com.gooodhandletruck.sidong.ui.theme.AppDimens
import com.gooodhandletruck.sidong.ui.theme.AppType
import java.time.LocalDate

private const val DAYS_PER_MONTH = 30
private const val PHOTO_ASPECT_RATIO = 16f / 9f

// 최근 거래 표의 세 칸(계약일 · 층 · 가격) 너비 비율
private const val DATE_COLUMN_WEIGHT = 1.3f
private const val FLOOR_COLUMN_WEIGHT = 0.7f
private const val PRICE_COLUMN_WEIGHT = 1.2f

/**
 * S-04를 ViewModel에 잇는다. 열릴 때마다 단지를 새로 받아 오고, 보이는 동안 차 위치를 따라 방향·거리 칩을 고친다.
 *
 * @param isSidePane 1:2 창에서 목록 옆 칸에 열렸으면 true. 머리글 버튼이 [닫기]가 된다
 */
@Composable
fun ComplexDetailRoute(
    screen: Screen.ComplexDetail,
    window: WindowLayout,
    isSidePane: Boolean,
    locationGranted: Boolean,
    onClose: () -> Unit,
    onStartRoute: (RouteTarget) -> Unit
) {
    val viewModel: ComplexDetailViewModel = viewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(screen.complexId) { viewModel.open(screen.complexId) }
    LaunchedEffect(locationGranted, lifecycleOwner) {
        if (locationGranted) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.followLocation() }
        }
    }
    ComplexDetailScreen(
        // 다른 단지를 막 열어 ViewModel이 아직 이전 단지를 들고 있으면 불러오는 중으로 그린다
        state = if (state.complexId == screen.complexId) state else ComplexDetailUiState(complexId = screen.complexId),
        window = window,
        isSidePane = isSidePane,
        arrived = screen.arrived,
        onClose = onClose,
        onSelectArea = viewModel::selectArea,
        onToggleSaved = viewModel::toggleSaved,
        onStartRoute = onStartRoute,
        onRetry = viewModel::retry
    )
}

/**
 * S-04 단지 상세. 위에서 아래로 머리글, 사진, 주소, 평형 칩, 가격 요약, 최근 거래 3건이고, [저장]·[길 안내]는 아래에 고정한다.
 * 1:2 창에서는 목록 옆 칸에, 1:1:1 창에서는 창 전체로 열린다. 1:1:1 창은 사진 높이를 320으로 줄여 한 화면에 들어오게 한다.
 * 길 안내로 도착해서 열었으면([arrived]) 머리글 위에 "도착했어요" 띠를 둔다.
 */
@Composable
fun ComplexDetailScreen(
    state: ComplexDetailUiState,
    window: WindowLayout,
    isSidePane: Boolean,
    arrived: Boolean,
    onClose: () -> Unit,
    onSelectArea: (Double) -> Unit,
    onToggleSaved: () -> Unit,
    onStartRoute: (RouteTarget) -> Unit,
    onRetry: () -> Unit
) {
    val content = state.content
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                // 옆 칸이면 목록 칸의 오른쪽 여백이 두 칸 사이를 띄운다
                start = if (isSidePane) 0.dp else window.contentPadding,
                end = window.contentPadding,
                top = AppDimens.ContentPaddingVertical,
                bottom = AppDimens.ContentPaddingVertical
            ),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceM)
    ) {
        if (arrived) ArrivedBand()
        DetailHeader(
            detail = (content as? DetailContent.Loaded)?.detail,
            place = state.place,
            closeLabel = stringResource(if (isSidePane) R.string.action_close else R.string.action_back),
            onClose = onClose
        )
        when (content) {
            DetailContent.Loading -> LoadingBlocks(window)
            DetailContent.NotFound -> StatusBox(message = stringResource(R.string.status_complex_not_found))
            DetailContent.Failed -> StatusBox(
                message = stringResource(R.string.status_load_failed),
                actionLabel = stringResource(R.string.action_retry),
                onAction = onRetry
            )
            is DetailContent.Loaded -> {
                DetailBody(
                    detail = content.detail,
                    selectedArea = state.selectedArea,
                    window = window,
                    onSelectArea = onSelectArea,
                    modifier = Modifier.weight(1f)
                )
                ActionButtons(
                    saved = state.saved,
                    onToggleSaved = onToggleSaved,
                    onStartRoute = { onStartRoute(content.detail.toRouteTarget()) }
                )
            }
        }
    }
}

/** 머리글: [뒤로]/[닫기], 단지 이름(titleM), 유형 칩 "아파트", 방향·거리 칩. 불러오는 중에는 버튼만 보인다 */
@Composable
private fun DetailHeader(detail: ComplexDetail?, place: RelativePlace?, closeLabel: String, onClose: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceS)
    ) {
        SecondaryButton(text = closeLabel, onClick = onClose, height = AppDimens.TouchMin)
        if (detail != null) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceXs)) {
                Text(
                    text = detail.name,
                    style = AppType.TitleM,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceXs)) {
                    complexTypeName(detail.type)?.let { TagChip(text = it, filled = false) }
                    place?.let { DirectionChip(place = it) }
                }
            }
        }
    }
}

/** 유형 이름. P0는 아파트만 있고, 모르는 유형이면 칩을 숨긴다 */
@Composable
private fun complexTypeName(type: String): String? = when (type) {
    "APT" -> stringResource(R.string.type_apartment)
    else -> null
}

/** 사진부터 최근 거래까지. 창이 낮으면 이 부분만 스크롤한다 */
@Composable
private fun DetailBody(
    detail: ComplexDetail,
    selectedArea: AreaPrice?,
    window: WindowLayout,
    onSelectArea: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceM)
    ) {
        ComplexPhoto(name = detail.name, modifier = Modifier.photoSize(window), letterStyle = AppType.TitleL)
        Text(text = detail.address, style = AppType.BodyS, color = AppColors.TextTertiary)
        if (selectedArea == null) {
            Text(
                text = stringResource(R.string.detail_no_trades),
                style = AppType.BodyM,
                color = AppColors.TextSecondary
            )
        } else {
            AreaChips(areas = detail.areas, selected = selectedArea, onSelect = onSelectArea)
            PriceSummary(area = selectedArea, periodDays = detail.periodDays, asOf = detail.asOf)
            RecentTrades(trades = selectedArea.recentTrades, asOf = detail.asOf)
        }
    }
}

/** 사진 크기: 1:2 창은 16:9, 1:1:1 창은 높이 320. 모서리는 카드와 같은 28 */
private fun Modifier.photoSize(window: WindowLayout): Modifier = fillMaxWidth()
    .then(if (window.isWide) Modifier.aspectRatio(PHOTO_ASPECT_RATIO) else Modifier.height(AppDimens.DetailPhotoHeight))
    .clip(RoundedCornerShape(AppDimens.CardRadius))

/** 평형 칩: "전용 59㎡ · 84㎡ · 114㎡". 거래가 있는 평형만 온다 */
@Composable
private fun AreaChips(areas: List<AreaPrice>, selected: AreaPrice, onSelect: (Double) -> Unit) {
    Row(
        modifier = Modifier
            .selectableGroup()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceS)
    ) {
        Text(text = stringResource(R.string.detail_area_label), style = AppType.LabelL, color = AppColors.TextSecondary)
        areas.forEach { area ->
            SelectChip(
                text = formatArea(area.areaM2),
                selected = area.areaM2 == selected.areaM2,
                onClick = { onSelect(area.areaM2) }
            )
        }
    }
}

/**
 * 가격 요약: "최근 3개월 평균"(labelL), "12억 3,000만"(titleL), "거래 4건 · 10월 31일 기준"(labelS), 공시가격.
 * 최근 3개월 거래가 없으면 마지막 거래 가격을 64% 글자로 쓰고 "마지막 거래 · 2026년 3월"을 붙인다.
 */
@Composable
private fun PriceSummary(area: AreaPrice, periodDays: Int, asOf: LocalDate) {
    val months = periodDays / DAYS_PER_MONTH
    val asOfText = monthDay(asOf)
    val average = area.avgPriceManwon
    val lastTrade = area.recentTrades.firstOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceXs)) {
        if (average != null) {
            Text(
                text = stringResource(R.string.detail_average_label, months),
                style = AppType.LabelL,
                color = AppColors.TextSecondary
            )
            Text(text = formatPrice(average), style = AppType.TitleL, color = AppColors.TextPrimary)
            Text(
                text = stringResource(R.string.detail_trade_meta, area.tradeCount, asOfText),
                style = AppType.LabelS,
                color = AppColors.TextTertiary
            )
        } else if (lastTrade != null) {
            val month = stringResource(
                R.string.date_year_month,
                lastTrade.contractDate.year,
                lastTrade.contractDate.monthValue
            )
            Text(
                text = stringResource(R.string.detail_last_trade_label, month),
                style = AppType.LabelL,
                color = AppColors.TextSecondary
            )
            Text(text = formatPrice(lastTrade.priceManwon), style = AppType.TitleL, color = AppColors.TextTertiary)
            Text(
                text = stringResource(R.string.detail_no_recent_meta, months, asOfText),
                style = AppType.LabelS,
                color = AppColors.TextTertiary
            )
        }
        area.officialPriceManwon?.let {
            Text(
                text = stringResource(R.string.detail_official_price, formatPrice(it)),
                style = AppType.LabelL,
                color = AppColors.OfficialPrice
            )
        }
    }
}

/** 최근 거래 3건: 계약일 · 층 · 가격 세 칸 표(bodyM). 기간과 상관없이 최신 순이다 */
@Composable
private fun RecentTrades(trades: List<Trade>, asOf: LocalDate) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceXs)) {
        HorizontalDivider(color = AppColors.Outline)
        Text(
            text = stringResource(R.string.detail_recent_trades),
            style = AppType.LabelL,
            color = AppColors.TextPrimary,
            modifier = Modifier.padding(top = AppDimens.SpaceXs)
        )
        trades.forEach { trade ->
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = tradeDate(trade.contractDate, asOf),
                    style = AppType.BodyM,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(DATE_COLUMN_WEIGHT)
                )
                Text(
                    text = stringResource(R.string.detail_floor, trade.floor),
                    style = AppType.BodyM,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.weight(FLOOR_COLUMN_WEIGHT)
                )
                Text(
                    text = formatPrice(trade.priceManwon),
                    style = AppType.BodyM,
                    color = AppColors.TextPrimary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(PRICE_COLUMN_WEIGHT)
                )
            }
        }
    }
}

/**
 * 아래 고정 버튼: [저장](주 버튼, 왼쪽) [길 안내](보조 버튼, 오른쪽). 운전석에 가까운 왼쪽에 저장을 둔다.
 * [저장]은 누르면 바로 "저장했어요"로 바뀌고, 한 번 더 누르면 저장을 취소한다(확인 팝업 없음).
 * 저장한 뒤에는 보조 버튼 모양으로 바꿔, 다시 누르라는 버튼이 아니라 지금 상태라는 것을 보인다.
 */
@Composable
private fun ActionButtons(saved: Boolean, onToggleSaved: () -> Unit, onStartRoute: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceM)) {
        if (saved) {
            SecondaryButton(
                text = stringResource(R.string.action_saved),
                onClick = onToggleSaved,
                modifier = Modifier.weight(1f)
            )
        } else {
            PrimaryButton(
                text = stringResource(R.string.action_save),
                onClick = onToggleSaved,
                modifier = Modifier.weight(1f)
            )
        }
        SecondaryButton(
            text = stringResource(R.string.action_route),
            onClick = onStartRoute,
            modifier = Modifier.weight(1f)
        )
    }
}

/** 길 안내로 도착했을 때 머리글 위에 두는 "도착했어요" 띠 */
@Composable
private fun ArrivedBand() {
    val shape = RoundedCornerShape(AppDimens.CardRadius)
    Text(
        text = stringResource(R.string.detail_arrived),
        style = AppType.BodyM,
        color = AppColors.TextPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface, shape)
            .border(1.dp, AppColors.Outline, shape)
            .padding(horizontal = AppDimens.SpaceM, vertical = AppDimens.SpaceS)
    )
}

/** 불러오는 중: 움직이지 않는 회색 자리 (반복 애니메이션 금지) */
@Composable
private fun LoadingBlocks(window: WindowLayout) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceM)) {
        Box(modifier = Modifier.photoSize(window).background(AppColors.ButtonSecondary))
        listOf(0.6f, 0.4f, 0.8f).forEach { widthFraction ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .height(AppDimens.SpaceXl)
                    .clip(RoundedCornerShape(AppDimens.ButtonRadius))
                    .background(AppColors.ButtonSecondary)
            )
        }
    }
}

/** 계약일. 기준일과 같은 해면 "10월 24일", 다른 해면 "2025년 12월 29일" */
@Composable
private fun tradeDate(date: LocalDate, asOf: LocalDate): String = if (date.year == asOf.year) {
    monthDay(date)
} else {
    stringResource(R.string.date_year_month_day, date.year, date.monthValue, date.dayOfMonth)
}

@Composable
private fun monthDay(date: LocalDate): String =
    stringResource(R.string.date_month_day, date.monthValue, date.dayOfMonth)

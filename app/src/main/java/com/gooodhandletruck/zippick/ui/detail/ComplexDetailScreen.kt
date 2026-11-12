package com.gooodhandletruck.zippick.ui.detail

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gooodhandletruck.zippick.R
import com.gooodhandletruck.zippick.data.RouteTarget
import com.gooodhandletruck.zippick.data.model.AreaPrice
import com.gooodhandletruck.zippick.data.model.ComplexDetail
import com.gooodhandletruck.zippick.data.model.Trade
import com.gooodhandletruck.zippick.data.toRouteTarget
import com.gooodhandletruck.zippick.ui.WindowLayout
import com.gooodhandletruck.zippick.ui.component.DirectionLabel
import com.gooodhandletruck.zippick.ui.component.PrimaryButton
import com.gooodhandletruck.zippick.ui.component.SecondaryButton
import com.gooodhandletruck.zippick.ui.component.SelectChip
import com.gooodhandletruck.zippick.ui.component.StatusBox
import com.gooodhandletruck.zippick.ui.component.TagChip
import com.gooodhandletruck.zippick.ui.format.RelativePlace
import com.gooodhandletruck.zippick.ui.format.formatArea
import com.gooodhandletruck.zippick.ui.format.formatPrice
import com.gooodhandletruck.zippick.ui.navigation.Screen
import com.gooodhandletruck.zippick.ui.theme.AppColors
import com.gooodhandletruck.zippick.ui.theme.AppDimens
import com.gooodhandletruck.zippick.ui.theme.AppType
import java.time.LocalDate

private const val DAYS_PER_MONTH = 30

// 최근 거래 표의 세 칸(계약일 · 층 · 거래금액) 너비 비율
private const val DATE_COLUMN_WEIGHT = 1.5f
private const val FLOOR_COLUMN_WEIGHT = 0.6f
private const val PRICE_COLUMN_WEIGHT = 1.5f

// 불러오는 중 회색 자리: 가격 카드와 거래 표 카드 높이
private val LOADING_PRICE_HEIGHT = 240.dp
private val LOADING_TRADES_HEIGHT = 320.dp

// 자료 기준 줄: 아이콘(32)을 글자 첫 줄(줄높이 36) 가운데에 맞춘다
private val SOURCE_ICON_NUDGE = 2.dp

/**
 * S-04를 ViewModel에 잇는다. 열릴 때마다 단지를 새로 받아 오고, 보이는 동안 차 위치를 따라 방향·거리 줄을 고친다.
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
 * S-04 단지 상세. 시안(D-03 단지 카드, D-04 도착 카드)을 따라 위에서 아래로 머리글(이름 displayL, 유형·방향·거리, 주소),
 * 가격 카드(평형 칩, 대표 가격, 공시가격), 최근 거래 표 카드, 자료 기준 카드이고, [저장]·[길 안내]는 아래에 고정한다.
 * 1:2 창에서는 목록 옆 칸에, 1:1:1 창에서는 창 전체로 열린다. 창이 낮으면 카드 부분만 스크롤한다.
 * 길 안내로 도착해서 열었으면([arrived]) 머리글 위에 "도착했어요" 띠를 둔다.
 * TODO: 사진은 imageUrl이 오고 그림을 불러올 라이브러리(Coil 등)를 정하면 머리글 아래에 다시 넣는다.
 *  라이브러리는 넣기 전에 먼저 묻는다. 사진 높이는 화면 설계서 '정할 것' 12번과 같이 정한다.
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
                top = AppDimens.ContentPaddingTop,
                bottom = AppDimens.ContentPaddingVertical
            ),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceM)
    ) {
        if (arrived) ArrivedBand()
        DetailHeader(
            detail = (content as? DetailContent.Loaded)?.detail,
            place = state.place,
            isSidePane = isSidePane,
            onClose = onClose
        )
        when (content) {
            DetailContent.Loading -> LoadingBlocks()
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

/**
 * 머리글: [‹ 뒤로]/[× 닫기], 단지 이름(displayL), 유형 칩 "아파트"와 방향·거리 줄, 주소(bodyS).
 * 불러오는 중에는 버튼만 보인다.
 */
@Composable
private fun DetailHeader(detail: ComplexDetail?, place: RelativePlace?, isSidePane: Boolean, onClose: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceM)) {
        SecondaryButton(
            text = stringResource(if (isSidePane) R.string.action_close else R.string.action_back),
            onClick = onClose,
            height = AppDimens.ButtonHeightM,
            icon = if (isSidePane) R.drawable.ic_close else R.drawable.ic_chevron_left
        )
        if (detail != null) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceXs)) {
                Text(
                    text = detail.name,
                    style = AppType.DisplayL,
                    color = AppColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceS)
                ) {
                    complexTypeName(detail.type)?.let { TagChip(text = it, filled = false) }
                    place?.let { DirectionLabel(place = it) }
                }
                Text(
                    text = detail.address,
                    style = AppType.BodyS,
                    color = AppColors.TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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

/** 가격 카드부터 자료 기준 카드까지. 창이 낮으면 이 부분만 스크롤한다 */
@Composable
private fun DetailBody(
    detail: ComplexDetail,
    selectedArea: AreaPrice?,
    onSelectArea: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceM)
    ) {
        if (selectedArea == null) {
            DetailCard {
                Text(
                    text = stringResource(R.string.detail_no_trades),
                    style = AppType.BodyM,
                    color = AppColors.TextSecondary
                )
            }
        } else {
            DetailCard {
                AreaChips(areas = detail.areas, selected = selectedArea, onSelect = onSelectArea)
                PriceSummary(area = selectedArea, periodDays = detail.periodDays, asOf = detail.asOf)
            }
            DetailCard { RecentTrades(trades = selectedArea.recentTrades, asOf = detail.asOf) }
        }
        DetailCard {
            SourceNotes(asOf = detail.asOf, showOfficialNote = selectedArea?.officialPriceManwon != null)
        }
    }
}

/** 흰 카드(테두리, 모서리 28, 안쪽 여백 32). S-04의 가격·거래·자료 기준을 하나씩 담는다 */
@Composable
private fun DetailCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(AppDimens.CardRadius)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.Surface, shape)
            .border(1.dp, AppColors.Outline, shape)
            .padding(AppDimens.SpaceL),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceS),
        content = content
    )
}

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
 * 가격 요약: "최근 3개월 평균"(labelL), "12억 3,000만"(displayL), "거래 4건 · 10월 31일 기준"(labelS),
 * [공시가격] 칩과 공시가격. 최근 3개월 거래가 없으면 마지막 거래 가격을 64% 글자로 쓰고 "마지막 거래 · 2026년 3월"을 붙인다.
 */
@Composable
private fun PriceSummary(area: AreaPrice, periodDays: Int, asOf: LocalDate) {
    val months = periodDays / DAYS_PER_MONTH
    val asOfText = monthDay(asOf)
    val average = area.avgPriceManwon
    val lastTrade = area.recentTrades.firstOrNull()
    Column(
        modifier = Modifier.padding(top = AppDimens.SpaceXs),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceXs)
    ) {
        if (average != null) {
            Text(
                text = stringResource(R.string.detail_average_label, months),
                style = AppType.LabelL,
                color = AppColors.TextSecondary
            )
            Text(text = formatPrice(average), style = AppType.DisplayL, color = AppColors.TextPrimary)
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
            Text(text = formatPrice(lastTrade.priceManwon), style = AppType.DisplayL, color = AppColors.TextTertiary)
            Text(
                text = stringResource(R.string.detail_no_recent_meta, months, asOfText),
                style = AppType.LabelS,
                color = AppColors.TextTertiary
            )
        }
        area.officialPriceManwon?.let { OfficialPrice(priceManwon = it) }
    }
}

/** [공시가격] 칩(공시가격 색)과 그 옆 가격(bodyL) */
@Composable
private fun OfficialPrice(priceManwon: Int) {
    Row(
        modifier = Modifier.padding(top = AppDimens.SpaceXs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceS)
    ) {
        Text(
            text = stringResource(R.string.official_price_chip),
            style = AppType.LabelBold,
            color = AppColors.OfficialPrice,
            modifier = Modifier
                .clip(CircleShape)
                .background(AppColors.OfficialPriceSoft)
                .padding(horizontal = AppDimens.SpaceS, vertical = AppDimens.SpaceXs)
        )
        Text(text = formatPrice(priceManwon), style = AppType.BodyL, color = AppColors.TextPrimary)
    }
}

/**
 * 최근 거래 표(최대 3건, 최신 순): 제목 "최근 거래"(titleS), 칸 이름 줄(labelS), 거래마다 계약일·층(bodyM)과
 * 거래금액(displayS, 오른쪽 정렬). 줄 사이에 2짜리 옅은 선을 긋는다(시안 D-03).
 */
@Composable
private fun RecentTrades(trades: List<Trade>, asOf: LocalDate) {
    Text(text = stringResource(R.string.detail_recent_trades), style = AppType.TitleS, color = AppColors.TextPrimary)
    TradeRow(
        date = stringResource(R.string.detail_col_date),
        floor = stringResource(R.string.detail_col_floor),
        price = stringResource(R.string.detail_col_price),
        isHeader = true
    )
    trades.forEach { trade ->
        HorizontalDivider(thickness = 2.dp, color = AppColors.ButtonSecondary)
        TradeRow(
            date = tradeDate(trade.contractDate, asOf),
            floor = stringResource(R.string.detail_floor, trade.floor),
            price = formatPrice(trade.priceManwon),
            isHeader = false
        )
    }
}

@Composable
private fun TradeRow(date: String, floor: String, price: String, isHeader: Boolean) {
    val textStyle = if (isHeader) AppType.LabelS else AppType.BodyM
    val textColor = if (isHeader) AppColors.TextTertiary else AppColors.TextSecondary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isHeader) 0.dp else AppDimens.SpaceXs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = date, style = textStyle, color = textColor, modifier = Modifier.weight(DATE_COLUMN_WEIGHT))
        Text(text = floor, style = textStyle, color = textColor, modifier = Modifier.weight(FLOOR_COLUMN_WEIGHT))
        Text(
            text = price,
            style = if (isHeader) textStyle else AppType.DisplayS,
            color = if (isHeader) textColor else AppColors.TextPrimary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(PRICE_COLUMN_WEIGHT)
        )
    }
}

/**
 * 자료 기준 카드(시안 D-03): 기준일, 신고가 늦게 들어올 수 있다는 안내, 공시가격 기준일.
 * 공시가격 줄은 공시가격을 보여 줄 때만 쓴다.
 */
@Composable
private fun SourceNotes(asOf: LocalDate, showOfficialNote: Boolean) {
    Text(text = stringResource(R.string.detail_source_title), style = AppType.TitleS, color = AppColors.TextPrimary)
    SourceLine(icon = R.drawable.ic_calendar, text = stringResource(R.string.detail_source_as_of, monthDay(asOf)))
    SourceLine(icon = R.drawable.ic_clock, text = stringResource(R.string.detail_source_delay))
    if (showOfficialNote) {
        SourceLine(
            icon = R.drawable.ic_tag,
            text = stringResource(R.string.detail_source_official),
            tint = AppColors.OfficialPrice
        )
    }
}

@Composable
private fun SourceLine(@DrawableRes icon: Int, text: String, tint: Color = AppColors.Accent) {
    Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceS)) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .padding(top = SOURCE_ICON_NUDGE)
                .size(AppDimens.IconL)
        )
        Text(text = text, style = AppType.BodyM, color = AppColors.TextSecondary, modifier = Modifier.weight(1f))
    }
}

/**
 * 아래 고정 버튼: [♡ 저장](주 버튼, 왼쪽) [길 안내](보조 버튼, 오른쪽). 운전석에 가까운 왼쪽에 저장을 둔다.
 * [저장]은 누르면 바로 "저장했어요"로 바뀌고, 한 번 더 누르면 저장을 취소한다(확인 팝업 없음).
 * 저장한 뒤에는 보조 버튼 모양에 강조색 꽉 찬 하트로 바꿔, 다시 누르라는 버튼이 아니라 지금 상태라는 것을 보인다.
 */
@Composable
private fun ActionButtons(saved: Boolean, onToggleSaved: () -> Unit, onStartRoute: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceM)) {
        if (saved) {
            SecondaryButton(
                text = stringResource(R.string.action_saved),
                onClick = onToggleSaved,
                modifier = Modifier.weight(1f),
                icon = R.drawable.ic_heart_filled,
                iconTint = AppColors.Accent
            )
        } else {
            PrimaryButton(
                text = stringResource(R.string.action_save),
                onClick = onToggleSaved,
                modifier = Modifier.weight(1f),
                icon = R.drawable.ic_heart
            )
        }
        SecondaryButton(
            text = stringResource(R.string.action_route),
            onClick = onStartRoute,
            modifier = Modifier.weight(1f),
            icon = R.drawable.ic_route
        )
    }
}

/** 길 안내로 도착했을 때 머리글 위에 두는 띠: 옅은 강조색 바탕과 테두리, 깃발과 "도착했어요"(시안 D-04) */
@Composable
private fun ArrivedBand() {
    val shape = RoundedCornerShape(AppDimens.CardRadius)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.AccentSoft, shape)
            .border(AppDimens.HighlightBorder, AppColors.AccentLine, shape)
            .padding(horizontal = AppDimens.SpaceL, vertical = AppDimens.SpaceM),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceS)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_flag),
            contentDescription = null,
            tint = AppColors.Accent,
            modifier = Modifier.size(AppDimens.IconL)
        )
        Text(text = stringResource(R.string.detail_arrived), style = AppType.TitleS, color = AppColors.Accent)
    }
}

/** 불러오는 중: 움직이지 않는 회색 카드 자리 두 개 (반복 애니메이션 금지) */
@Composable
private fun LoadingBlocks() {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceM)) {
        listOf(LOADING_PRICE_HEIGHT, LOADING_TRADES_HEIGHT).forEach { height ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .clip(RoundedCornerShape(AppDimens.CardRadius))
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

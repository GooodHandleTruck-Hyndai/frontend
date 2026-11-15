package com.gooodhandletruck.zippick.ui.nearby

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gooodhandletruck.zippick.R
import com.gooodhandletruck.zippick.data.AreaRange
import com.gooodhandletruck.zippick.data.ComplexType
import com.gooodhandletruck.zippick.data.PriceRange
import com.gooodhandletruck.zippick.ui.component.ToggleChip
import com.gooodhandletruck.zippick.ui.format.formatDistance
import com.gooodhandletruck.zippick.ui.onboarding.areaRangeLabel
import com.gooodhandletruck.zippick.ui.onboarding.priceRangeLabel
import com.gooodhandletruck.zippick.ui.onboarding.typeLabel
import com.gooodhandletruck.zippick.ui.theme.AppColors
import com.gooodhandletruck.zippick.ui.theme.AppDimens
import com.gooodhandletruck.zippick.ui.theme.AppType

/*
 * S-03 지도 위 칩과 칩 아래 펼침(Figma 시안 S-03 종류·필터 펼침).
 * 펼침은 창이 아니라 같은 판 위에 그린다. 바깥을 누르면 닫히고, 칩을 다시 누르면 닫히고, 다른 칩을 누르면 그 칩 것으로 바뀐다.
 */

// Figma 시안 S-03 펼침 크기. 시안 캡처로 잰 값이다. TODO: Dev Mode 값과 다르면 여기만 고친다
private val TYPE_MENU_WIDTH = 520.dp
private val FILTER_POPOVER_WIDTH = 600.dp
private val CHIP_ELEVATION = 4.dp
private val MENU_ELEVATION = 12.dp
private val CHIP_ICON_SIZE = 28.dp
private val CHEVRON_SIZE = 24.dp

/** 지도 위 칩 아래 펼침. 한 번에 하나만 열린다 */
enum class MapMenu { NONE, TYPE, FILTER }

/**
 * 지도 위 칩: [아이콘] 글자 [⌄]. 펼쳤으면([expanded]) 꺾쇠가 위를 향한다.
 * 지도 위에 떠 있어 판보다 밝은 바탕(Surface)에 그림자를 둔다. 높이는 누를 곳 규칙(TouchMin) 이상이다.
 */
@Composable
internal fun MapChip(
    text: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null
) {
    val shape = RoundedCornerShape(AppDimens.ChipRadius)
    Row(
        modifier = modifier
            .heightIn(min = AppDimens.TouchMin)
            .shadow(CHIP_ELEVATION, shape)
            .clip(shape)
            .background(AppColors.Surface)
            .clickable(role = Role.DropdownList, onClick = onClick)
            .padding(horizontal = AppDimens.SpaceM),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceXs)
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = AppColors.TextSecondary,
                modifier = Modifier.size(CHIP_ICON_SIZE)
            )
        }
        Text(text = text, style = AppType.Label1Medium, color = AppColors.TextPrimary)
        Icon(
            painter = painterResource(R.drawable.ic_chevron_down),
            contentDescription = null,
            tint = AppColors.TextPrimary,
            modifier = Modifier
                .size(CHEVRON_SIZE)
                .rotate(if (expanded) 180f else 0f)
        )
    }
}

/** '종류' 칩 글자: 고른 유형을 "아파트, 오피스텔"처럼 잇는다 */
@Composable
internal fun typesChipText(types: Set<ComplexType>): String =
    ComplexType.entries.filter { it in types }.map { typeLabel(it) }.joinToString(", ")

/**
 * '종류' 펼침: 아파트·오피스텔·원룸을 한 줄씩 두고, 고른 줄은 밝은 바탕에 강조색 체크를 붙인다.
 * 여러 개를 고를 수 있어 눌러도 닫히지 않는다. 원룸은 아직 고를 수 없어 흐리게 둔다.
 */
@Composable
internal fun TypeMenu(types: Set<ComplexType>, onToggleType: (ComplexType) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(AppDimens.ChipRadius)
    Column(
        modifier = modifier
            .widthIn(max = TYPE_MENU_WIDTH)
            .fillMaxWidth()
            .shadow(MENU_ELEVATION, shape)
            .clip(shape)
            .background(AppColors.Background)
            .border(1.dp, AppColors.Outline, shape)
            .padding(AppDimens.SpaceXs),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceXs)
    ) {
        ComplexType.entries.forEach { type ->
            val checked = type in types
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = AppDimens.TouchMin)
                    .clip(RoundedCornerShape(AppDimens.ButtonRadiusM))
                    .background(if (checked) AppColors.Surface else Color.Transparent)
                    .toggleable(
                        value = checked,
                        enabled = type.available,
                        role = Role.Checkbox,
                        onValueChange = { onToggleType(type) }
                    )
                    .padding(horizontal = AppDimens.SpaceS),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = typeLabel(type),
                    style = AppType.Label1Medium,
                    color = if (type.available) AppColors.TextPrimary else AppColors.TextTertiary,
                    modifier = Modifier.weight(1f)
                )
                if (checked) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        tint = AppColors.Accent,
                        modifier = Modifier.size(AppDimens.IconS)
                    )
                }
            }
        }
    }
}

/**
 * '필터' 펼침: 제목과 [초기화], 그 아래 가격대·면적·반경 칩.
 * 가격대·면적은 여러 개를 고를 수 있고 하나도 안 고르면 거르지 않는다. 반경은 하나만 고르고, 바꾸면 서버에서 다시 부른다.
 */
@OptIn(ExperimentalLayoutApi::class) // FilterSection의 칩 줄(FlowRowScope)이 아직 실험 API다
@Composable
internal fun FilterPopover(
    state: NearbyUiState,
    onTogglePrice: (PriceRange) -> Unit,
    onToggleArea: (AreaRange) -> Unit,
    onSelectRadius: (NearbyRadius) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(AppDimens.ChipRadius)
    Column(
        modifier = modifier
            .widthIn(max = FILTER_POPOVER_WIDTH)
            .fillMaxWidth()
            .shadow(MENU_ELEVATION, shape)
            .clip(shape)
            .background(AppColors.Surface)
            .border(1.dp, AppColors.Outline, shape)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppDimens.SpaceM, vertical = AppDimens.SpaceS),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceM)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.nearby_filter_chip),
                style = AppType.Title2,
                color = AppColors.TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .heightIn(min = AppDimens.TouchMin)
                    .clip(RoundedCornerShape(AppDimens.ButtonRadiusM))
                    .clickable(role = Role.Button, onClick = onReset)
                    .padding(horizontal = AppDimens.SpaceXs),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.filter_reset),
                    style = AppType.Label1Medium,
                    color = AppColors.Accent
                )
            }
        }
        FilterSection(R.string.filter_price) {
            PriceRange.entries.forEach { range ->
                ToggleChip(
                    text = priceRangeLabel(range),
                    checked = range in state.prices,
                    onCheckedChange = { onTogglePrice(range) }
                )
            }
        }
        FilterSection(R.string.filter_area) {
            AreaRange.entries.forEach { range ->
                ToggleChip(
                    text = areaRangeLabel(range),
                    checked = range in state.areas,
                    onCheckedChange = { onToggleArea(range) }
                )
            }
        }
        FilterSection(R.string.filter_radius) {
            NearbyRadius.entries.forEach { radius ->
                ToggleChip(
                    text = formatDistance(radius.meters.toDouble()),
                    checked = radius == state.radius,
                    onCheckedChange = { onSelectRadius(radius) }
                )
            }
        }
    }
}

/** 필터 한 칸: 이름(label1, 회색)과 줄을 넘기며 놓는 칩 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(@StringRes title: Int, chips: @Composable FlowRowScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceS)) {
        Text(text = stringResource(title), style = AppType.Label1, color = AppColors.TextSecondary)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceS),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceS),
            content = chips
        )
    }
}

/**
 * 펼침을 칩 아래에 놓는다. [top]만큼 내리고, 가로는 칩 왼쪽 끝([anchorX], px)에 맞춘다.
 * 판 밖으로 나가지 않게 양옆을 [margin]만큼 안쪽에 둔다.
 * 판 아래로 넘치면 펼침 높이를 줄인다(펼침 안이 스크롤된다).
 */
internal fun Modifier.dropdownBelow(top: Dp, anchorX: Int, margin: Dp): Modifier = this
    .padding(top = top)
    .layout { measurable, constraints ->
        val marginPx = margin.roundToPx()
        val placeable = measurable.measure(
            Constraints(
                maxWidth = (constraints.maxWidth - marginPx * 2).coerceAtLeast(0),
                maxHeight = (constraints.maxHeight - marginPx).coerceAtLeast(0)
            )
        )
        val x = anchorX.coerceAtMost(constraints.maxWidth - placeable.width - marginPx).coerceAtLeast(marginPx)
        layout(constraints.maxWidth, placeable.height) { placeable.place(x, 0) }
    }

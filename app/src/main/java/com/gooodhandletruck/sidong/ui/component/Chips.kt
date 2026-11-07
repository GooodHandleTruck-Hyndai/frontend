package com.gooodhandletruck.sidong.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gooodhandletruck.sidong.R
import com.gooodhandletruck.sidong.ui.theme.AppColors
import com.gooodhandletruck.sidong.ui.theme.AppDimens
import com.gooodhandletruck.sidong.ui.theme.AppType

/**
 * 누를 수 없는 작은 표시 칩(labelS). [filled]면 회색 바탕, 아니면 테두리만 그린다(S-04 유형 "아파트").
 */
@Composable
fun TagChip(text: String, modifier: Modifier = Modifier, filled: Boolean = true) {
    val shape = RoundedCornerShape(AppDimens.ButtonRadius)
    val background = if (filled) {
        Modifier.background(AppColors.ButtonSecondary, shape)
    } else {
        Modifier.border(1.dp, AppColors.Outline, shape)
    }
    Text(
        text = text,
        style = AppType.LabelS,
        color = AppColors.TextSecondary,
        modifier = modifier
            .clip(shape)
            .then(background)
            .padding(horizontal = AppDimens.SpaceS, vertical = AppDimens.SpaceXs)
    )
}

/**
 * 여러 개를 함께 고를 수 있는 칩(labelL, 76×76 이상). 고른 칩은 강조색이고, [enabled]가 false면 흐리게 두고 누를 수 없다.
 * S-02 알림 조건이 쓴다.
 */
@Composable
fun ToggleChip(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .heightIn(min = AppDimens.TouchMin)
            .widthIn(min = AppDimens.TouchMin)
            .clip(RoundedCornerShape(AppDimens.ButtonRadius))
            .background(if (checked && enabled) AppColors.Accent else AppColors.ButtonSecondary)
            .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(horizontal = AppDimens.SpaceM),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = AppType.LabelL,
            color = when {
                !enabled -> AppColors.TextTertiary
                checked -> AppColors.OnAccent
                else -> AppColors.TextPrimary
            }
        )
    }
}

/**
 * 여럿 중 하나를 고르는 칩(labelL, 76×76 이상). 고른 칩은 강조색이다.
 * S-03 범위 칩(500m·1km·3km)과 S-04 평형 칩이 쓴다. 묶음에는 Modifier.selectableGroup()을 붙인다.
 */
@Composable
fun SelectChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .heightIn(min = AppDimens.TouchMin)
            .widthIn(min = AppDimens.TouchMin)
            .clip(RoundedCornerShape(AppDimens.ButtonRadius))
            .background(if (selected) AppColors.Accent else AppColors.ButtonSecondary)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = AppDimens.SpaceM),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = AppType.LabelL,
            color = if (selected) AppColors.OnAccent else AppColors.TextPrimary
        )
    }
}

/**
 * 번호 배지(S-03 카드 왼쪽, 약도 핀): 목록 순서와 약도의 번호를 맞춘다.
 * [filled]면 강조색 바탕에 흰 숫자(1:2 창에서 상세를 연 단지), 아니면 옅은 강조색 바탕에 강조색 숫자다.
 */
@Composable
fun NumberBadge(
    number: Int,
    filled: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = AppDimens.BadgeSize,
    style: TextStyle = AppType.DisplayS
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (filled) AppColors.Accent else AppColors.AccentSoft),
        contentAlignment = Alignment.Center
    ) {
        Text(text = number.toString(), style = style, color = if (filled) AppColors.OnAccent else AppColors.Accent)
    }
}

/** "저장함" 칩: 옅은 강조색 바탕에 강조색 하트와 글자(labelS). 저장한 단지 카드에 붙인다(S-06) */
@Composable
fun SavedMark(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(AppColors.AccentSoft)
            .padding(horizontal = AppDimens.SpaceS, vertical = AppDimens.SpaceXs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceXs)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_heart_filled),
            contentDescription = null,
            tint = AppColors.Accent,
            modifier = Modifier.size(SAVED_MARK_ICON_SIZE)
        )
        Text(text = stringResource(R.string.card_saved_mark), style = AppType.LabelS, color = AppColors.Accent)
    }
}

private val SAVED_MARK_ICON_SIZE = 20.dp

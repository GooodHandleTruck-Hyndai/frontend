package com.gooodhandletruck.sidong.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gooodhandletruck.sidong.ui.theme.AppColors
import com.gooodhandletruck.sidong.ui.theme.AppDimens
import com.gooodhandletruck.sidong.ui.theme.AppType

/**
 * 누를 수 없는 작은 표시 칩(labelS). [filled]면 회색 바탕(방향·거리), 아니면 테두리만 그린다(유형 "아파트").
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

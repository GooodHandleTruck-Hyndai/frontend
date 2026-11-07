package com.gooodhandletruck.zippick.ui.component

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.gooodhandletruck.zippick.R
import com.gooodhandletruck.zippick.ui.navigation.Tab
import com.gooodhandletruck.zippick.ui.theme.AppColors
import com.gooodhandletruck.zippick.ui.theme.AppDimens
import com.gooodhandletruck.zippick.ui.theme.AppType

/**
 * 왼쪽 탭 4개(주변·저장·기록·설정). 앱 창이 화면 오른쪽에 있으므로 운전석에 가까운 왼쪽에 둔다.
 * 바탕은 창 배경과 같고, 고른 탭만 옅은 강조색 바탕에 강조색 아이콘·글자로 칠한다.
 */
@Composable
fun NavRail(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(AppDimens.RailWidth)
            .fillMaxHeight()
            .selectableGroup()
            .padding(vertical = AppDimens.SpaceXs),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceS),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Tab.entries.forEach { tab ->
            RailItem(tab = tab, selected = tab == selected, onClick = { onSelect(tab) })
        }
    }
}

@Composable
private fun RailItem(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) AppColors.Accent else AppColors.TextSecondary
    Column(
        modifier = Modifier
            .width(AppDimens.RailWidth - AppDimens.SpaceS * 2)
            .heightIn(min = AppDimens.TouchMin)
            .clip(RoundedCornerShape(AppDimens.ButtonRadius))
            .background(if (selected) AppColors.AccentSoft else Color.Transparent)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(vertical = AppDimens.SpaceS),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceXs)
    ) {
        Icon(
            painter = painterResource(tab.icon),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(AppDimens.RailIconSize)
        )
        Text(
            text = stringResource(tab.label),
            style = if (selected) AppType.LabelBold else AppType.LabelL,
            color = color
        )
    }
}

@get:StringRes
private val Tab.label: Int
    get() = when (this) {
        Tab.NEARBY -> R.string.tab_nearby
        Tab.SAVED -> R.string.tab_saved
        Tab.HISTORY -> R.string.tab_history
        Tab.SETTINGS -> R.string.tab_settings
    }

/** 저장 탭은 [저장] 버튼과 같은 하트를 쓴다 */
@get:DrawableRes
private val Tab.icon: Int
    get() = when (this) {
        Tab.NEARBY -> R.drawable.ic_pin
        Tab.SAVED -> R.drawable.ic_heart
        Tab.HISTORY -> R.drawable.ic_clock
        Tab.SETTINGS -> R.drawable.ic_settings
    }

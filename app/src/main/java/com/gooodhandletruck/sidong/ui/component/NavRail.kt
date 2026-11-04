package com.gooodhandletruck.sidong.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gooodhandletruck.sidong.R
import com.gooodhandletruck.sidong.ui.navigation.Tab
import com.gooodhandletruck.sidong.ui.theme.AppColors
import com.gooodhandletruck.sidong.ui.theme.AppDimens
import com.gooodhandletruck.sidong.ui.theme.AppType

/**
 * 왼쪽 탭 4개(주변·저장·기록·설정). 앱 창이 화면 오른쪽에 있으므로 운전석에 가까운 왼쪽에 둔다.
 * 고른 탭은 강조색으로 칠한다.
 */
@Composable
fun NavRail(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .width(AppDimens.RailWidth)
                .fillMaxHeight()
                .background(AppColors.Surface)
                .padding(vertical = AppDimens.SpaceL),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceS),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Tab.entries.forEach { tab ->
                RailItem(tab = tab, selected = tab == selected, onClick = { onSelect(tab) })
            }
        }
        // 탭과 본문 사이 구분선
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(AppColors.Outline)
        )
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
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(vertical = AppDimens.SpaceS),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceXs)
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(AppDimens.RailIconSize)
        )
        Text(text = stringResource(tab.label), style = AppType.LabelL, color = color)
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

private val Tab.icon: ImageVector
    get() = when (this) {
        Tab.NEARBY -> Icons.Filled.LocationOn
        Tab.SAVED -> Icons.Filled.Star
        Tab.HISTORY -> Icons.Filled.DateRange
        Tab.SETTINGS -> Icons.Filled.Settings
    }

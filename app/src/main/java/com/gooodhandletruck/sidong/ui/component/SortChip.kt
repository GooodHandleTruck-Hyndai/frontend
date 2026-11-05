package com.gooodhandletruck.sidong.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.gooodhandletruck.sidong.R
import com.gooodhandletruck.sidong.ui.theme.AppColors
import com.gooodhandletruck.sidong.ui.theme.AppDimens
import com.gooodhandletruck.sidong.ui.theme.AppType

/**
 * 정렬을 바꾸면 맨 위로 올라가는 목록 상태. 상세에 갔다 돌아올 때처럼 정렬이 그대로면 보던 자리를 지킨다.
 * [sortKey]는 지금 정렬의 이름이다(화면 상태와 함께 기억해야 해서 문자열로 받는다).
 */
@Composable
fun rememberSortedListState(sortKey: String): LazyListState {
    val listState = rememberLazyListState()
    var lastSort by rememberSaveable { mutableStateOf(sortKey) }
    LaunchedEffect(sortKey) {
        if (sortKey != lastSort) {
            lastSort = sortKey
            listState.scrollToItem(0)
        }
    }
    return listState
}

/**
 * 정렬 칩(S-03·S-05): 지금 정렬 이름("가까운 순 ▾")을 보여 주고, 누르면 고를 수 있는 정렬을 칩 아래에 펼친다.
 * 펼친 목록은 창을 덮지 않는 작은 메뉴이고, 줄마다 누를 곳이 76 이상이다. 정차 화면에서만 쓴다.
 */
@Composable
fun <T> SortChip(
    selected: T,
    options: List<T>,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .heightIn(min = AppDimens.TouchMin)
                .widthIn(min = AppDimens.TouchMin)
                .clip(RoundedCornerShape(AppDimens.ButtonRadius))
                .background(AppColors.ButtonSecondary)
                .clickable(role = Role.DropdownList) { expanded = true }
                .padding(horizontal = AppDimens.SpaceM),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.sort_chip, label(selected)),
                style = AppType.LabelL,
                color = AppColors.TextPrimary
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label(option),
                            style = AppType.LabelL,
                            color = if (option == selected) AppColors.Accent else AppColors.TextPrimary
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                    modifier = Modifier.heightIn(min = AppDimens.TouchMin)
                )
            }
        }
    }
}

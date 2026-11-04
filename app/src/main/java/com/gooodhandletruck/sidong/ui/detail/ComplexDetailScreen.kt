package com.gooodhandletruck.sidong.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gooodhandletruck.sidong.R
import com.gooodhandletruck.sidong.ui.WindowLayout
import com.gooodhandletruck.sidong.ui.component.PlaceholderScreen
import com.gooodhandletruck.sidong.ui.navigation.Screen

/**
 * S-04 단지 상세. 목록(S-03·S-05·S-06)이나 '방금 들은 단지' 띠에서 열린다.
 * [저장]·[길 안내]는 화면을 옮기지 않으므로 빈 화면에는 두지 않는다. 길 안내로 출발하면 주행 화면으로 바뀐다.
 */
@Composable
fun ComplexDetailScreen(screen: Screen.ComplexDetail, window: WindowLayout, onBack: (() -> Unit)?) {
    PlaceholderScreen(
        id = screen.id,
        title = stringResource(R.string.screen_complex_detail),
        description = stringResource(R.string.placeholder_complex_detail, screen.complexId),
        window = window,
        actions = emptyList(),
        onBack = onBack
    )
}

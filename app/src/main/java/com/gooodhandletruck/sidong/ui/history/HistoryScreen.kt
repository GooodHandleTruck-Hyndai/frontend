package com.gooodhandletruck.sidong.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gooodhandletruck.sidong.R
import com.gooodhandletruck.sidong.ui.SAMPLE_COMPLEX_ID
import com.gooodhandletruck.sidong.ui.WindowLayout
import com.gooodhandletruck.sidong.ui.component.PlaceholderAction
import com.gooodhandletruck.sidong.ui.component.PlaceholderScreen
import com.gooodhandletruck.sidong.ui.navigation.Screen

/**
 * S-06 오늘 들은 단지. 카드를 누르면 S-04로 간다.
 * 알림이 꺼져 있을 때 빈 목록의 [알림 조건 정하기]는 S-02로 간다. 빈 화면에서는 두 버튼을 모두 보여 준다.
 */
@Composable
fun HistoryScreen(window: WindowLayout, onOpenComplex: (String) -> Unit, onSetAlert: () -> Unit) {
    PlaceholderScreen(
        id = Screen.History.id,
        title = stringResource(R.string.screen_history),
        description = stringResource(R.string.placeholder_history),
        window = window,
        actions = listOf(
            PlaceholderAction(stringResource(R.string.action_open_detail)) { onOpenComplex(SAMPLE_COMPLEX_ID) },
            PlaceholderAction(stringResource(R.string.action_set_alert), onClick = onSetAlert)
        )
    )
}

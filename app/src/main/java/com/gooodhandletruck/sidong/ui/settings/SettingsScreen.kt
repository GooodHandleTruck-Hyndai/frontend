package com.gooodhandletruck.sidong.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gooodhandletruck.sidong.R
import com.gooodhandletruck.sidong.ui.WindowLayout
import com.gooodhandletruck.sidong.ui.component.PlaceholderAction
import com.gooodhandletruck.sidong.ui.component.PlaceholderScreen
import com.gooodhandletruck.sidong.ui.navigation.Screen

/** S-07 설정. [알림 조건 바꾸기]를 누르면 S-02가 열리고, 저장하면 여기로 돌아온다. */
@Composable
fun SettingsScreen(window: WindowLayout, onEditAlert: () -> Unit) {
    PlaceholderScreen(
        id = Screen.Settings.id,
        title = stringResource(R.string.screen_settings),
        description = stringResource(R.string.placeholder_settings),
        window = window,
        actions = listOf(
            PlaceholderAction(stringResource(R.string.action_edit_alert), onClick = onEditAlert)
        )
    )
}

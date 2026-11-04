package com.gooodhandletruck.sidong.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gooodhandletruck.sidong.R
import com.gooodhandletruck.sidong.ui.WindowLayout
import com.gooodhandletruck.sidong.ui.component.PlaceholderAction
import com.gooodhandletruck.sidong.ui.component.PlaceholderScreen
import com.gooodhandletruck.sidong.ui.navigation.Screen

/**
 * S-02 알림 조건.
 * 첫 실행에서는 [이 조건으로 알림 받기]·[나중에 할게요] 둘 다 첫 실행을 끝내고 S-03으로 간다.
 * 설정·기록에서 열었을 때는 [저장] 하나이고, 누르면 연 화면으로 돌아간다.
 */
@Composable
fun AlertSetupScreen(
    screen: Screen.AlertSetup,
    window: WindowLayout,
    onFinishOnboarding: () -> Unit,
    onSave: () -> Unit,
    onBack: (() -> Unit)?
) {
    val description: Int
    val actions: List<PlaceholderAction>
    if (screen.isOnboarding) {
        description = R.string.placeholder_alert_setup_onboarding
        actions = listOf(
            PlaceholderAction(
                label = stringResource(R.string.action_alert_accept),
                isPrimary = true,
                onClick = onFinishOnboarding
            ),
            PlaceholderAction(label = stringResource(R.string.action_alert_later), onClick = onFinishOnboarding)
        )
    } else {
        description = R.string.placeholder_alert_setup_settings
        actions = listOf(
            PlaceholderAction(label = stringResource(R.string.action_save), isPrimary = true, onClick = onSave)
        )
    }

    PlaceholderScreen(
        id = screen.id,
        title = stringResource(R.string.screen_alert_setup),
        description = stringResource(description),
        window = window,
        actions = actions,
        onBack = onBack
    )
}

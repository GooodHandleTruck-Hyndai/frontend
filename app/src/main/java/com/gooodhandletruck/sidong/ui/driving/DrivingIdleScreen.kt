package com.gooodhandletruck.sidong.ui.driving

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gooodhandletruck.sidong.R
import com.gooodhandletruck.sidong.ui.WindowLayout
import com.gooodhandletruck.sidong.ui.component.DrivingPlaceholderScreen
import com.gooodhandletruck.sidong.ui.component.PlaceholderAction
import com.gooodhandletruck.sidong.ui.navigation.Screen

/**
 * S-08 주행 중 대기. [주변 단지 듣기]를 누르면 가장 가까운 단지를 말하고 S-09로 간다.
 * 첫 실행을 끝내기 전에는 "정차하면 설정을 이어서 할 수 있어요"만 보이고 버튼도 알림도 없다.
 */
@Composable
fun DrivingIdleScreen(window: WindowLayout, onboardingDone: Boolean, onListenNearby: () -> Unit) {
    DrivingPlaceholderScreen(
        id = Screen.DrivingIdle.id,
        title = stringResource(R.string.screen_driving_idle),
        message = stringResource(
            if (onboardingDone) R.string.placeholder_driving_idle else R.string.driving_onboarding_pending
        ),
        window = window,
        actions = if (onboardingDone) {
            listOf(
                PlaceholderAction(
                    label = stringResource(R.string.action_listen_nearby),
                    isPrimary = true,
                    onClick = onListenNearby
                )
            )
        } else {
            emptyList()
        }
    )
}

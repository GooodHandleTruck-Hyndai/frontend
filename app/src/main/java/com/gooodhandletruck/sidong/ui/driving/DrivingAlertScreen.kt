package com.gooodhandletruck.sidong.ui.driving

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gooodhandletruck.sidong.R
import com.gooodhandletruck.sidong.ui.WindowLayout
import com.gooodhandletruck.sidong.ui.component.DrivingPlaceholderScreen
import com.gooodhandletruck.sidong.ui.navigation.Screen

/**
 * S-09 주행 중 단지 알림. 2분이 지나면 S-08로 돌아간다(Navigator가 처리).
 * [다시 듣기]·[저장]은 화면을 옮기지 않으므로 빈 화면에는 두지 않는다.
 */
@Composable
fun DrivingAlertScreen(screen: Screen.DrivingAlert, window: WindowLayout) {
    DrivingPlaceholderScreen(
        id = screen.id,
        title = stringResource(R.string.screen_driving_alert),
        message = stringResource(R.string.placeholder_driving_alert, screen.complexId),
        window = window,
        actions = emptyList()
    )
}

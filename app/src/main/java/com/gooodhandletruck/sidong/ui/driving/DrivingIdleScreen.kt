package com.gooodhandletruck.sidong.ui.driving

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.gooodhandletruck.sidong.R
import com.gooodhandletruck.sidong.ui.WindowLayout
import com.gooodhandletruck.sidong.ui.component.PrimaryButton
import com.gooodhandletruck.sidong.ui.theme.AppColors
import com.gooodhandletruck.sidong.ui.theme.AppDimens
import com.gooodhandletruck.sidong.ui.theme.AppType

/**
 * S-08 주행 중 대기. 가운데 상태 한 줄(bodyL)과 큰 버튼 [주변 단지 듣기](높이 160).
 * 버튼을 누르면 차 앞쪽 180도 안에서 가장 가까운 단지를 말하고 S-09로 간다.
 * 상태 문구는 자동 알림이 켜져 있으면 "단지를 지나면 소리로 알려 드릴게요", 꺼져 있으면 "정차하면 주변 단지 가격을 보여 드려요"다.
 * 첫 실행을 끝내기 전에는 "정차하면 설정을 이어서 할 수 있어요"만 보이고 버튼도 알림도 없다.
 */
@Composable
fun DrivingIdleScreen(window: WindowLayout, onboardingDone: Boolean, alertsOn: Boolean, onListenNearby: () -> Unit) {
    DrivingLayout(
        window = window,
        buttons = {
            if (onboardingDone) {
                PrimaryButton(
                    text = stringResource(R.string.action_listen_nearby),
                    onClick = onListenNearby,
                    modifier = Modifier.weight(1f),
                    height = AppDimens.DrivingButtonHeight
                )
            }
        }
    ) {
        Text(
            text = stringResource(
                when {
                    !onboardingDone -> R.string.driving_onboarding_pending
                    alertsOn -> R.string.driving_status_alerts_on
                    else -> R.string.driving_status_alerts_off
                }
            ),
            style = AppType.BodyL,
            color = AppColors.TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

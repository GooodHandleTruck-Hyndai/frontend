package com.gooodhandletruck.zippick.ui.driving

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gooodhandletruck.zippick.R
import com.gooodhandletruck.zippick.ui.WindowLayout
import com.gooodhandletruck.zippick.ui.component.IconBadge
import com.gooodhandletruck.zippick.ui.component.PrimaryButton
import com.gooodhandletruck.zippick.ui.theme.AppColors
import com.gooodhandletruck.zippick.ui.theme.AppDimens
import com.gooodhandletruck.zippick.ui.theme.AppType

/**
 * S-08 주행 중 대기. 가운데 아이콘과 상태 한 줄(bodyL), 큰 버튼 [주변 단지 듣기](높이 160).
 * 버튼을 누르면 차 앞쪽 180도 안에서 가장 가까운 단지를 말하고 S-09로 간다.
 * 상태 문구는 자동 알림이 켜져 있으면 "단지를 지나면 소리로 알려 드릴게요", 꺼져 있으면 "정차하면 주변 단지 가격을 보여 드려요"다.
 * 아이콘은 알림이 켜져 있으면 소리, 아니면 앱 로고의 건물이다(움직이지 않는다).
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
                    height = AppDimens.DrivingButtonHeight,
                    icon = R.drawable.ic_speaker
                )
            }
        }
    ) {
        val alertsActive = onboardingDone && alertsOn
        IconBadge(
            icon = if (alertsActive) R.drawable.ic_speaker else R.drawable.ic_app_logo,
            size = STATUS_BADGE_SIZE,
            filled = false
        )
        Spacer(Modifier.height(AppDimens.SpaceL))
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

// 상태 줄 위 아이콘 동그라미
private val STATUS_BADGE_SIZE = 96.dp

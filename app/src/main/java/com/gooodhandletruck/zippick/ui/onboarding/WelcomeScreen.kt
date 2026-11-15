package com.gooodhandletruck.zippick.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gooodhandletruck.zippick.R
import com.gooodhandletruck.zippick.ui.WindowLayout
import com.gooodhandletruck.zippick.ui.component.PrimaryButton
import com.gooodhandletruck.zippick.ui.component.rememberLocationPermissionRequest
import com.gooodhandletruck.zippick.ui.theme.AppColors
import com.gooodhandletruck.zippick.ui.theme.AppDimens
import com.gooodhandletruck.zippick.ui.theme.AppType

// Figma 시안 플로우_13 시작하기의 크기. TODO: Dev Mode 값과 다르면 여기만 고친다
private val LOGO_WIDTH = 159.dp // ic_logo_onboarding 크기(둘레 여백 포함)
private val LOGO_HEIGHT = 116.dp
private val LINE_ICON_CIRCLE = 72.dp // 설명 줄 왼쪽 동그라미
private val LINE_ICON = 40.dp // 동그라미 안 아이콘(ic_speaker·ic_Building1, 둘레 여백 포함)
private val BUTTON_GAP = 48.dp // 마지막 설명 줄과 시작 버튼 사이

/**
 * S-01 시작하기. 첫 실행 1단계(Figma 시안 플로우_13). 창 가운데에 위에서부터
 * 로고(강조색) → 단계 배지 "1 / 2" → 제목(title1) → 앱이 하는 일 두 줄 → 꽉 찬 너비 시작 버튼을 둔다.
 * [위치 권한 허용하고 시작하기]를 누르면 위치 권한을 묻고, 허용하든 거부하든 창이 닫히면 [onStart]로 S-02에 간다.
 * 거부했으면 S-03에 권한 요청 상태 상자가 보인다.
 */
@Composable
fun WelcomeScreen(window: WindowLayout, onStart: () -> Unit) {
    val requestPermission = rememberLocationPermissionRequest { onStart() }
    OnboardingContent(window) {
        // 시안 색 그대로 그린다(칠하지 않는다)
        Image(
            painter = painterResource(R.drawable.ic_logo_onboarding),
            contentDescription = stringResource(R.string.brand_name),
            modifier = Modifier.size(width = LOGO_WIDTH, height = LOGO_HEIGHT)
        )
        Spacer(Modifier.height(AppDimens.SpaceXl))
        StepBadge(step = 1, total = 2)
        Spacer(Modifier.height(AppDimens.SpaceL))
        Text(
            text = stringResource(R.string.welcome_title),
            style = AppType.Title1,
            color = AppColors.TextPrimary
        )
        Spacer(Modifier.height(AppDimens.SpaceXl))
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceM)) {
            WelcomeLine(icon = R.drawable.ic_speaker_outline, text = stringResource(R.string.welcome_body_driving))
            WelcomeLine(icon = R.drawable.ic_building, text = stringResource(R.string.welcome_body_parked))
        }
        Spacer(Modifier.height(BUTTON_GAP))
        PrimaryButton(
            text = stringResource(R.string.action_start),
            onClick = requestPermission,
            modifier = Modifier.fillMaxWidth(),
            icon = R.drawable.ic_map
        )
    }
}

/** 회색 동그라미 안 아이콘(64% 흰색)과 설명 한 줄(body2) */
@Composable
private fun WelcomeLine(@DrawableRes icon: Int, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceM)
    ) {
        Box(
            modifier = Modifier
                .size(LINE_ICON_CIRCLE)
                .clip(CircleShape)
                .background(AppColors.Surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = AppColors.TextTertiary,
                modifier = Modifier.size(LINE_ICON)
            )
        }
        Text(text = text, style = AppType.Body2, color = AppColors.TextSecondary, modifier = Modifier.weight(1f))
    }
}

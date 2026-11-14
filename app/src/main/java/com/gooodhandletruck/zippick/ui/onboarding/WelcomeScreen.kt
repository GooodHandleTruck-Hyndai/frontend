package com.gooodhandletruck.zippick.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gooodhandletruck.zippick.R
import com.gooodhandletruck.zippick.ui.WindowLayout
import com.gooodhandletruck.zippick.ui.component.IconBadge
import com.gooodhandletruck.zippick.ui.component.PrimaryButton
import com.gooodhandletruck.zippick.ui.component.rememberLocationPermissionRequest
import com.gooodhandletruck.zippick.ui.theme.AppColors
import com.gooodhandletruck.zippick.ui.theme.AppDimens
import com.gooodhandletruck.zippick.ui.theme.AppType

/**
 * S-01 시작하기. 첫 실행 1단계. 제목(title2) 아래에 주행 중·정차 중에 앱이 하는 일을 아이콘과 한 줄씩 쓴다.
 * [위치 권한 허용하고 시작하기]를 누르면 위치 권한을 묻고, 허용하든 거부하든 창이 닫히면 [onStart]로 S-02에 간다.
 * 거부했으면 S-03에 권한 요청 상태 상자가 보인다.
 */
@Composable
fun WelcomeScreen(window: WindowLayout, onStart: () -> Unit) {
    val requestPermission = rememberLocationPermissionRequest { onStart() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = window.contentPadding, vertical = AppDimens.ContentPaddingVertical),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.welcome_title),
            style = AppType.Title2,
            color = AppColors.TextPrimary
        )
        Spacer(Modifier.height(AppDimens.SpaceL))
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceM)) {
            WelcomeLine(icon = R.drawable.ic_speaker, text = stringResource(R.string.welcome_body_driving))
            WelcomeLine(icon = R.drawable.ic_app_logo, text = stringResource(R.string.welcome_body_parked))
        }
        Spacer(Modifier.height(AppDimens.SpaceXl))
        PrimaryButton(
            text = stringResource(R.string.action_start),
            onClick = requestPermission,
            icon = R.drawable.ic_pin
        )
    }
}

/** 옅은 강조색 동그라미 아이콘과 설명 한 줄(body2) */
@Composable
private fun WelcomeLine(@DrawableRes icon: Int, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceM)
    ) {
        IconBadge(icon = icon, filled = false)
        Text(text = text, style = AppType.Body2, color = AppColors.TextSecondary, modifier = Modifier.weight(1f))
    }
}

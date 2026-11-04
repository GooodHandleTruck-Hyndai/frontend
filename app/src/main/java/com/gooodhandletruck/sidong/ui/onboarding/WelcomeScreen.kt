package com.gooodhandletruck.sidong.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.gooodhandletruck.sidong.R
import com.gooodhandletruck.sidong.ui.WindowLayout
import com.gooodhandletruck.sidong.ui.component.PrimaryButton
import com.gooodhandletruck.sidong.ui.component.rememberLocationPermissionRequest
import com.gooodhandletruck.sidong.ui.theme.AppColors
import com.gooodhandletruck.sidong.ui.theme.AppDimens
import com.gooodhandletruck.sidong.ui.theme.AppType

/**
 * S-01 시작하기. 첫 실행 1단계.
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
            style = AppType.TitleM,
            color = AppColors.TextPrimary
        )
        Spacer(Modifier.height(AppDimens.SpaceM))
        Text(
            text = stringResource(R.string.welcome_body_driving),
            style = AppType.BodyM,
            color = AppColors.TextSecondary
        )
        Text(
            text = stringResource(R.string.welcome_body_parked),
            style = AppType.BodyM,
            color = AppColors.TextSecondary
        )
        Spacer(Modifier.height(AppDimens.SpaceXl))
        PrimaryButton(text = stringResource(R.string.action_start), onClick = requestPermission)
    }
}

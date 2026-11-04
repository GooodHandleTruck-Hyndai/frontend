package com.gooodhandletruck.sidong.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gooodhandletruck.sidong.R
import com.gooodhandletruck.sidong.ui.component.NavRail
import com.gooodhandletruck.sidong.ui.component.SecondaryButton
import com.gooodhandletruck.sidong.ui.detail.ComplexDetailScreen
import com.gooodhandletruck.sidong.ui.driving.DrivingAlertScreen
import com.gooodhandletruck.sidong.ui.driving.DrivingIdleScreen
import com.gooodhandletruck.sidong.ui.history.HistoryScreen
import com.gooodhandletruck.sidong.ui.navigation.NavState
import com.gooodhandletruck.sidong.ui.navigation.Screen
import com.gooodhandletruck.sidong.ui.navigation.Tab
import com.gooodhandletruck.sidong.ui.nearby.NearbyScreen
import com.gooodhandletruck.sidong.ui.onboarding.AlertSetupScreen
import com.gooodhandletruck.sidong.ui.onboarding.WelcomeScreen
import com.gooodhandletruck.sidong.ui.saved.SavedScreen
import com.gooodhandletruck.sidong.ui.settings.SettingsScreen
import com.gooodhandletruck.sidong.ui.theme.AppColors
import com.gooodhandletruck.sidong.ui.theme.AppDimens
import com.gooodhandletruck.sidong.ui.theme.AppType

/** 앱 창 크기. 너비가 1200dp 이상이면 넓은 창(1:2, 1700dp), 아니면 좁은 창(1:1:1, 840dp)으로 본다. */
data class WindowLayout(val width: Dp, val height: Dp) {
    val isWide: Boolean
        get() = width >= AppDimens.WideWindowMinWidth

    /** 본문 좌우 여백: 넓은 창 40, 좁은 창 32 */
    val contentPadding: Dp
        get() = if (isWide) AppDimens.ContentPaddingWide else AppDimens.ContentPaddingNarrow
}

/**
 * 앱의 맨 바깥 화면. 창 크기를 재고, 주행 중이면 주행 화면을, 정차 중이면 왼쪽 탭 + 정차 화면을 그린다.
 * 화면 전환 애니메이션은 쓰지 않는다.
 */
@Composable
fun AppRoot(viewModel: AppViewModel) {
    val nav by viewModel.nav.collectAsStateWithLifecycle()

    // 뒤로 갈 화면이 없거나 주행 중이면 뒤로 가기를 시스템에 맡긴다
    BackHandler(enabled = nav.canGoBack, onBack = viewModel::back)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        val window = WindowLayout(width = maxWidth, height = maxHeight)
        when (val screen = nav.current) {
            Screen.DrivingIdle -> DrivingIdleScreen(
                window = window,
                onboardingDone = nav.onboardingDone,
                onListenNearby = viewModel::listenNearby
            )
            is Screen.DrivingAlert -> DrivingAlertScreen(screen, window)
            is Screen.Parked -> ParkedContent(nav, screen, window, viewModel)
        }
    }
}

@Composable
private fun ParkedContent(nav: NavState, screen: Screen.Parked, window: WindowLayout, viewModel: AppViewModel) {
    Row(modifier = Modifier.fillMaxSize()) {
        // 첫 실행(S-01·S-02) 중에는 탭을 숨긴다
        nav.selectedTab?.let { tab -> NavRail(selected = tab, onSelect = viewModel::selectTab) }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            nav.heardBannerComplexId?.let { complexId ->
                HeardBanner(
                    complexId = complexId,
                    window = window,
                    onOpen = viewModel::openHeardComplex,
                    onDismiss = viewModel::dismissHeardBanner
                )
            }
            ParkedScreen(screen, window, viewModel, onBack = if (nav.canGoBack) viewModel::back else null)
        }
    }
}

/** 정차 중 화면 7개와 각 화면의 이동 버튼을 Navigator에 잇는다. */
@Composable
private fun ParkedScreen(screen: Screen.Parked, window: WindowLayout, viewModel: AppViewModel, onBack: (() -> Unit)?) {
    val locationGranted by viewModel.locationGranted.collectAsStateWithLifecycle()
    val openComplex: (String) -> Unit = { complexId -> viewModel.open(Screen.ComplexDetail(complexId)) }
    val openAlertSetup = { viewModel.open(Screen.AlertSetup(isOnboarding = false)) }

    when (screen) {
        Screen.Welcome -> WelcomeScreen(
            window = window,
            onStart = {
                viewModel.refreshLocationPermission()
                viewModel.open(Screen.AlertSetup(isOnboarding = true))
            }
        )
        is Screen.AlertSetup -> AlertSetupScreen(
            screen = screen,
            window = window,
            onFinishOnboarding = viewModel::finishOnboarding,
            onSave = viewModel::back,
            onBack = onBack
        )
        Screen.Nearby -> NearbyScreen(
            window = window,
            locationGranted = locationGranted,
            onLocationPermissionResult = viewModel::refreshLocationPermission,
            onOpenComplex = openComplex
        )
        is Screen.ComplexDetail -> ComplexDetailScreen(screen = screen, window = window, onBack = onBack)
        Screen.Saved -> SavedScreen(
            window = window,
            onOpenComplex = openComplex,
            onGoNearby = { viewModel.selectTab(Tab.NEARBY) }
        )
        Screen.History -> HistoryScreen(window = window, onOpenComplex = openComplex, onSetAlert = openAlertSetup)
        Screen.Settings -> SettingsScreen(window = window, onEditAlert = openAlertSetup)
    }
}

/** 정차하면 맨 위에 띄우는 '방금 들은 단지' 띠. [보기]는 S-04를 열고, [닫기]는 띠를 지운다. */
@Composable
private fun HeardBanner(complexId: String, window: WindowLayout, onOpen: () -> Unit, onDismiss: () -> Unit) {
    val shape = RoundedCornerShape(AppDimens.CardRadius)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = window.contentPadding, end = window.contentPadding, top = AppDimens.SpaceM)
            .background(AppColors.Surface, shape)
            .border(1.dp, AppColors.Outline, shape)
            .padding(AppDimens.SpaceS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceS)
    ) {
        Text(
            text = stringResource(R.string.banner_heard, complexId),
            style = AppType.BodyM,
            color = AppColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = AppDimens.SpaceS)
        )
        SecondaryButton(text = stringResource(R.string.action_view), onClick = onOpen, height = AppDimens.TouchMin)
        SecondaryButton(text = stringResource(R.string.action_close), onClick = onDismiss, height = AppDimens.TouchMin)
    }
}

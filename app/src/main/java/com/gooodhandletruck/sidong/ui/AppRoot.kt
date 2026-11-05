package com.gooodhandletruck.sidong.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
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
import com.gooodhandletruck.sidong.ui.detail.ComplexDetailRoute
import com.gooodhandletruck.sidong.ui.driving.DrivingAlertScreen
import com.gooodhandletruck.sidong.ui.driving.DrivingIdleScreen
import com.gooodhandletruck.sidong.ui.history.HistoryScreen
import com.gooodhandletruck.sidong.ui.navigation.NavState
import com.gooodhandletruck.sidong.ui.navigation.Screen
import com.gooodhandletruck.sidong.ui.navigation.Tab
import com.gooodhandletruck.sidong.ui.nearby.NearbyRoute
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
            is Screen.Parked -> ParkedContent(nav, window, viewModel)
        }
    }
}

@Composable
private fun ParkedContent(nav: NavState, window: WindowLayout, viewModel: AppViewModel) {
    // 화면마다 스크롤 위치 같은 상태를 따로 기억한다. 상세를 닫고 돌아오면 목록이 보던 자리에 그대로 있다
    val stateHolder = rememberSaveableStateHolder()
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
            val panes = if (window.isWide) nav.widePanes() else null
            if (panes != null) {
                WidePanesContent(panes, window, viewModel, stateHolder)
            } else {
                val screen = nav.backStack.last()
                val onBack = if (nav.canGoBack) viewModel::back else null
                // 상세는 상태를 기억하지 않는다. 다시 열면 맨 위부터 보인다
                if (screen is Screen.ComplexDetail) {
                    ParkedScreen(screen, window, viewModel, selectedComplexId = null, onBack = onBack)
                } else {
                    stateHolder.SaveableStateProvider(screen.stateKey) {
                        ParkedScreen(screen, window, viewModel, selectedComplexId = null, onBack = onBack)
                    }
                }
            }
        }
    }
}

/**
 * 1:2 창의 두 칸: 왼쪽 목록 칸(640 + 좌우 여백)과 오른쪽 상세 칸.
 * 상세가 열려도 왼쪽 칸은 같은 자리에 그대로 두어, 목록의 스크롤 위치와 고른 범위가 바뀌지 않는다.
 */
@Composable
private fun WidePanesContent(
    panes: WidePanes,
    window: WindowLayout,
    viewModel: AppViewModel,
    stateHolder: SaveableStateHolder
) {
    val locationGranted by viewModel.locationGranted.collectAsStateWithLifecycle()
    Row(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .width(AppDimens.ListPaneWidth + window.contentPadding * 2)
                .fillMaxHeight()
        ) {
            stateHolder.SaveableStateProvider(panes.list.stateKey) {
                // 왼쪽 칸이 목록이 아닌 화면(S-02)이어도 뒤로 가기는 오른쪽 상세를 먼저 닫는다
                ParkedScreen(panes.list, window, viewModel, selectedComplexId = panes.detail?.complexId, onBack = null)
            }
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            panes.detail?.let { detail ->
                // 다른 단지를 열면 스크롤을 맨 위로 되돌린다
                key(detail.complexId) {
                    ComplexDetailRoute(
                        screen = detail,
                        window = window,
                        isSidePane = true,
                        locationGranted = locationGranted,
                        onClose = viewModel::back
                    )
                }
            }
        }
    }
}

/** 1:2 창에서 두 칸으로 그릴 때 왼쪽 칸과 오른쪽 칸에 둘 화면 */
private class WidePanes(val list: Screen.Parked, val detail: Screen.ComplexDetail?)

/**
 * 1:2 창을 두 칸으로 그릴지 정한다. 목록 화면(S-03·S-05·S-06)은 상세를 열기 전에도 두 칸으로 그려
 * 카드를 누를 때 목록이 움직이지 않게 한다. 다른 화면 위에서 연 상세('방금 들은 단지' 띠)도 오른쪽 칸에 연다.
 */
private fun NavState.widePanes(): WidePanes? {
    val top = backStack.last()
    val below = backStack.getOrNull(backStack.lastIndex - 1)
    return when {
        top is Screen.ComplexDetail -> below?.let { WidePanes(list = it, detail = top) }
        top.hasDetailPane -> WidePanes(list = top, detail = null)
        else -> null
    }
}

/** 1:2 창에서 오른쪽에 상세 칸을 두는 목록 화면 (화면 설계서 '레이아웃') */
private val Screen.Parked.hasDetailPane: Boolean
    get() = this == Screen.Nearby || this == Screen.Saved || this == Screen.History

/** 화면 상태를 기억할 이름. S-02는 첫 실행과 설정에서 따로 기억한다 */
private val Screen.Parked.stateKey: String
    get() = if (this is Screen.AlertSetup && isOnboarding) "$id-onboarding" else id

/** 정차 중 화면 7개와 각 화면의 이동 버튼을 Navigator에 잇는다. */
@Composable
private fun ParkedScreen(
    screen: Screen.Parked,
    window: WindowLayout,
    viewModel: AppViewModel,
    selectedComplexId: String?,
    onBack: (() -> Unit)?
) {
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
        Screen.Nearby -> NearbyRoute(
            window = window,
            locationGranted = locationGranted,
            selectedComplexId = selectedComplexId,
            onLocationPermissionResult = viewModel::refreshLocationPermission,
            onOpenComplex = openComplex
        )
        // 좁은 창(1:1:1): 상세가 창 전체를 덮고, 왼쪽 탭은 연 곳(예: '주변')에 그대로 머문다
        is Screen.ComplexDetail -> key(screen.complexId) {
            ComplexDetailRoute(
                screen = screen,
                window = window,
                isSidePane = false,
                locationGranted = locationGranted,
                onClose = viewModel::back
            )
        }
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

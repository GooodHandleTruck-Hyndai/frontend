package com.gooodhandletruck.zippick.ui

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gooodhandletruck.zippick.R
import com.gooodhandletruck.zippick.ui.component.AppHeader
import com.gooodhandletruck.zippick.ui.component.IconBadge
import com.gooodhandletruck.zippick.ui.component.NavRail
import com.gooodhandletruck.zippick.ui.component.SecondaryButton
import com.gooodhandletruck.zippick.ui.detail.ComplexDetailRoute
import com.gooodhandletruck.zippick.ui.driving.DrivingAlertScreen
import com.gooodhandletruck.zippick.ui.driving.DrivingIdleScreen
import com.gooodhandletruck.zippick.ui.history.HistoryRoute
import com.gooodhandletruck.zippick.ui.navigation.NavState
import com.gooodhandletruck.zippick.ui.navigation.Screen
import com.gooodhandletruck.zippick.ui.navigation.Tab
import com.gooodhandletruck.zippick.ui.nearby.NearbyMapPane
import com.gooodhandletruck.zippick.ui.nearby.NearbyRoute
import com.gooodhandletruck.zippick.ui.onboarding.AlertSetupRoute
import com.gooodhandletruck.zippick.ui.onboarding.WelcomeScreen
import com.gooodhandletruck.zippick.ui.saved.SavedRoute
import com.gooodhandletruck.zippick.ui.settings.SettingsRoute
import com.gooodhandletruck.zippick.ui.theme.AppColors
import com.gooodhandletruck.zippick.ui.theme.AppDimens
import com.gooodhandletruck.zippick.ui.theme.AppType

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
            Screen.DrivingIdle -> {
                val alertSettings by viewModel.alertSettings.collectAsStateWithLifecycle()
                DrivingIdleScreen(
                    window = window,
                    onboardingDone = nav.onboardingDone,
                    alertsOn = alertSettings.isOn,
                    onListenNearby = viewModel::listenNearby
                )
            }
            is Screen.DrivingAlert -> {
                val spoken by viewModel.spoken.collectAsStateWithLifecycle()
                val current = spoken?.takeIf { it.complexId == screen.complexId }
                DrivingAlertScreen(
                    window = window,
                    name = current?.name.orEmpty(),
                    saved = current?.saved == true,
                    onReplay = viewModel::replaySpoken,
                    onSave = viewModel::saveSpoken
                )
            }
            is Screen.Parked -> ParkedContent(nav, window, viewModel)
        }
    }
}

@Composable
private fun ParkedContent(nav: NavState, window: WindowLayout, viewModel: AppViewModel) {
    // 화면마다 스크롤 위치 같은 상태를 따로 기억한다. 상세를 닫고 돌아오면 목록이 보던 자리에 그대로 있다
    val stateHolder = rememberSaveableStateHolder()
    // 첫 실행(S-01·S-02) 중에는 탭을 숨긴다
    val tab = nav.selectedTab
    Column(modifier = Modifier.fillMaxSize()) {
        AppHeader(
            // 탭이 있으면 로고를 탭 가운데에, 없으면 본문 왼쪽 여백에 맞춘다
            startPadding = if (tab != null) (AppDimens.RailWidth - AppDimens.LogoSize) / 2 else window.contentPadding,
            endPadding = window.contentPadding
        )
        Row(modifier = Modifier.weight(1f)) {
            tab?.let { NavRail(selected = it, onSelect = viewModel::selectTab) }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                nav.heardBannerComplexId?.let { complexId ->
                    val spoken by viewModel.spoken.collectAsStateWithLifecycle()
                    HeardBanner(
                        name = spoken?.takeIf { it.complexId == complexId }?.name ?: complexId,
                        window = window,
                        onOpen = viewModel::openHeardComplex,
                        onDismiss = viewModel::dismissHeardBanner
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    ParkedPanes(nav, window, viewModel, stateHolder)
                    val routeMessage by viewModel.routeMessage.collectAsStateWithLifecycle()
                    routeMessage?.let { RouteMessageBand(it, Modifier.align(Alignment.BottomCenter)) }
                }
            }
        }
    }
}

/** 1:2 창이면 두 칸으로, 1:1:1 창이면 백스택 맨 위 화면 하나를 그린다 */
@Composable
private fun ParkedPanes(
    nav: NavState,
    window: WindowLayout,
    viewModel: AppViewModel,
    stateHolder: SaveableStateHolder
) {
    val panes = if (window.isWide) nav.widePanes() else null
    if (panes != null) {
        WidePanesContent(panes, window, viewModel, stateHolder)
        return
    }
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

/**
 * [길 안내]를 누른 뒤 3초 동안 창 아래에 띄우는 한 줄. 전체 화면 팝업은 쓰지 않는다.
 * 시작했으면 강조색 길 안내 아이콘을 앞에 붙인다.
 */
@Composable
private fun RouteMessageBand(message: RouteMessage, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(AppDimens.CardRadius)
    Row(
        modifier = modifier
            .padding(bottom = AppDimens.ContentPaddingVertical)
            .background(AppColors.Surface, shape)
            .border(1.dp, AppColors.Outline, shape)
            .padding(horizontal = AppDimens.SpaceL, vertical = AppDimens.SpaceS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceS)
    ) {
        if (message == RouteMessage.STARTED) {
            Icon(
                painter = painterResource(R.drawable.ic_route),
                contentDescription = null,
                tint = AppColors.Accent,
                modifier = Modifier.size(AppDimens.IconL)
            )
        }
        Text(
            text = stringResource(
                when (message) {
                    RouteMessage.STARTED -> R.string.route_started
                    RouteMessage.FAILED -> R.string.route_failed
                }
            ),
            style = AppType.BodyM,
            color = AppColors.TextPrimary
        )
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
            val detail = panes.detail
            if (detail != null) {
                // 다른 단지를 열면 스크롤을 맨 위로 되돌린다
                key(detail.complexId) {
                    ComplexDetailRoute(
                        screen = detail,
                        window = window,
                        isSidePane = true,
                        locationGranted = locationGranted,
                        onClose = viewModel::back,
                        onStartRoute = viewModel::startRoute
                    )
                }
            } else if (panes.list == Screen.Nearby) {
                NearbyMapPane(window = window, onOpenComplex = { viewModel.open(Screen.ComplexDetail(it)) })
            } else if (panes.list == Screen.Saved || panes.list == Screen.History) {
                DetailPaneHint(window)
            }
        }
    }
}

/** 1:2 창에서 S-05·S-06 목록 옆 칸이 비어 있을 때의 안내: 옅은 강조색 동그라미 안 건물 그림과 한 줄 */
@Composable
private fun DetailPaneHint(window: WindowLayout) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(end = window.contentPadding),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceM, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconBadge(icon = R.drawable.ic_app_logo, size = AppDimens.TouchMin * 2, filled = false)
        Text(
            text = stringResource(R.string.detail_pane_hint),
            style = AppType.BodyM,
            color = AppColors.TextTertiary,
            textAlign = TextAlign.Center
        )
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
    val alertSettings by viewModel.alertSettings.collectAsStateWithLifecycle()
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
        is Screen.AlertSetup -> AlertSetupRoute(
            screen = screen,
            window = window,
            onFinishOnboarding = viewModel::finishOnboarding,
            onSaved = viewModel::back,
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
                onClose = viewModel::back,
                onStartRoute = viewModel::startRoute
            )
        }
        Screen.Saved -> SavedRoute(
            window = window,
            locationGranted = locationGranted,
            selectedComplexId = selectedComplexId,
            onOpenComplex = openComplex,
            onStartRoute = viewModel::startRoute,
            onGoNearby = { viewModel.selectTab(Tab.NEARBY) }
        )
        Screen.History -> HistoryRoute(
            window = window,
            locationGranted = locationGranted,
            alertsOn = alertSettings.isOn,
            selectedComplexId = selectedComplexId,
            onOpenComplex = openComplex,
            onSetAlert = openAlertSetup
        )
        Screen.Settings -> SettingsRoute(window = window, onEditAlert = openAlertSetup)
    }
}

/**
 * 정차하면 맨 위에 띄우는 '방금 들은 단지' 띠. [보기]는 S-04를 열고, [닫기]는 띠를 지운다.
 * 시안처럼 흰 카드에 옅은 강조색 테두리(3)를 두르고, 앞에 소리 아이콘을 둔다.
 */
@Composable
private fun HeardBanner(name: String, window: WindowLayout, onOpen: () -> Unit, onDismiss: () -> Unit) {
    val shape = RoundedCornerShape(AppDimens.CardRadius)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = window.contentPadding, end = window.contentPadding, bottom = AppDimens.SpaceXs)
            .background(AppColors.Surface, shape)
            .border(AppDimens.HighlightBorder, AppColors.AccentLine, shape)
            .padding(AppDimens.SpaceS),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceS)
    ) {
        IconBadge(icon = R.drawable.ic_speaker)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.banner_heard_label),
                style = AppType.LabelL,
                color = AppColors.TextTertiary
            )
            Text(
                text = name,
                style = AppType.TitleS,
                color = AppColors.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        SecondaryButton(text = stringResource(R.string.action_view), onClick = onOpen, height = AppDimens.ButtonHeightM)
        SecondaryButton(text = stringResource(R.string.action_close), onClick = onDismiss, height = AppDimens.ButtonHeightM)
    }
}

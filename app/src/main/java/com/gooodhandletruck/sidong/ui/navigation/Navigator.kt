package com.gooodhandletruck.sidong.ui.navigation

import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 지금 화면 이동 상태. 화면은 이 값만 보고 그린다. */
data class NavState(
    /** 정차 중 백스택. 첫 칸은 탭의 첫 화면(첫 실행 중에는 S-01), 마지막 칸이 지금 화면이다. */
    val backStack: List<Screen.Parked>,
    /** 첫 실행(S-01·S-02)을 끝냈는지 */
    val onboardingDone: Boolean,
    val isDriving: Boolean = false,
    /** 주행 중 단지 알림(S-09). null이면 대기(S-08)다. */
    val alert: Screen.DrivingAlert? = null,
    /** 주행 중에 마지막으로 들은 단지. 정차하면 '방금 들은 단지' 띠로 보여 준다. */
    val lastHeardComplexId: String? = null
) {
    /** 지금 창에 그릴 화면. 주행 중에는 정차 화면을 기억해 둔 채 주행 화면으로 덮는다. */
    val current: Screen
        get() = when {
            !isDriving -> backStack.last()
            alert != null -> alert
            else -> Screen.DrivingIdle
        }

    /** 왼쪽 탭에서 고른 탭. 첫 실행 중에는 null이고 탭을 숨긴다. */
    val selectedTab: Tab?
        get() = Tab.entries.firstOrNull { it.root == backStack.first() }

    /** 뒤로 갈 곳이 있는지. 주행 중에는 뒤로 가기를 받지 않는다. */
    val canGoBack: Boolean
        get() = !isDriving && backStack.size > 1

    /** 정차 중 맨 위에 띄울 '방금 들은 단지' */
    val heardBannerComplexId: String?
        get() = if (isDriving) null else lastHeardComplexId
}

/**
 * 화면 이동 규칙 (화면 설계서 '화면 흐름', '주행·정차 전환 규칙').
 * 새 라이브러리 없이 백스택을 직접 다룬다.
 */
class Navigator(
    private val scope: CoroutineScope,
    onboardingDone: Boolean
) {
    private val _state = MutableStateFlow(
        NavState(
            backStack = listOf(if (onboardingDone) Screen.Nearby else Screen.Welcome),
            onboardingDone = onboardingDone
        )
    )
    val state: StateFlow<NavState> = _state.asStateFlow()

    private var alertTimeout: Job? = null

    /** 정차 중 화면을 연다. 지금 화면과 같으면 다시 쌓지 않는다. */
    fun open(screen: Screen.Parked) = _state.update {
        if (it.backStack.last() == screen) it else it.copy(backStack = it.backStack + screen)
    }

    fun back() = _state.update {
        if (it.canGoBack) it.copy(backStack = it.backStack.dropLast(1)) else it
    }

    /** 왼쪽 탭을 누르면 그 탭의 첫 화면부터 다시 시작한다. */
    fun selectTab(tab: Tab) = _state.update { it.copy(backStack = listOf(tab.root)) }

    /** S-02에서 [이 조건으로 알림 받기]나 [나중에 할게요]를 누르면 첫 실행을 끝내고 S-03으로 간다. */
    fun finishOnboarding() = _state.update {
        it.copy(onboardingDone = true, backStack = listOf(Screen.Nearby))
    }

    /**
     * 주행 상태가 바뀔 때. 정차 백스택은 건드리지 않으므로 정차하면 보던 화면으로 바로 돌아간다.
     * 막히는 길에서 몇 초마다 바뀌어도 읽던 화면을 잃지 않는다.
     */
    fun onDrivingChanged(isDriving: Boolean) {
        if (!isDriving) {
            alertTimeout?.cancel()
            alertTimeout = null
        }
        _state.update { it.copy(isDriving = isDriving, alert = if (isDriving) it.alert else null) }
    }

    /**
     * 주행 중 단지를 말했을 때 S-09로 바꾸고 2분 뒤 S-08로 돌린다.
     * 2분 안에 다음 단지를 말하면 그 단지의 S-09로 바꾸고 2분을 새로 센다.
     * 첫 실행을 끝내기 전에는 알리지 않는다.
     */
    fun showAlert(complexId: String) {
        val now = _state.value
        if (!now.isDriving || !now.onboardingDone) return

        _state.update { it.copy(alert = Screen.DrivingAlert(complexId), lastHeardComplexId = complexId) }
        alertTimeout?.cancel()
        alertTimeout = scope.launch {
            delay(ALERT_DURATION)
            _state.update { it.copy(alert = null) }
        }
    }

    /** '방금 들은 단지' 띠의 [보기]: 그 단지의 S-04를 열고 띠를 닫는다. */
    fun openHeardComplex() = _state.update {
        val complexId = it.lastHeardComplexId ?: return@update it
        it.copy(backStack = it.backStack + Screen.ComplexDetail(complexId), lastHeardComplexId = null)
    }

    /** '방금 들은 단지' 띠의 [닫기] */
    fun dismissHeardBanner() = _state.update { it.copy(lastHeardComplexId = null) }

    private companion object {
        val ALERT_DURATION = 2.minutes
    }
}

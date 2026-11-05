package com.gooodhandletruck.sidong.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gooodhandletruck.sidong.data.DrivingStateRepository
import com.gooodhandletruck.sidong.data.LocationPermission
import com.gooodhandletruck.sidong.data.OnboardingStore
import com.gooodhandletruck.sidong.ui.navigation.NavState
import com.gooodhandletruck.sidong.ui.navigation.Navigator
import com.gooodhandletruck.sidong.ui.navigation.Screen
import com.gooodhandletruck.sidong.ui.navigation.Tab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** S-05·S-06·S-08 빈 화면이 쓰는 예시 단지 id(목업의 한빛마을 3단지). 그 화면들을 만들면 지운다. */
const val SAMPLE_COMPLEX_ID = "c-1001"

/**
 * 앱 전체 화면 이동과 주행 상태를 맡는다. 화면은 [nav]만 보고 그리고, 누르면 아래 함수를 부른다.
 * 의존성은 DI 프레임워크 없이 여기서 직접 만든다.
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val onboardingStore = OnboardingStore(app)
    private val drivingState = DrivingStateRepository(app)
    private val navigator = Navigator(viewModelScope, onboardingDone = onboardingStore.isDone)

    val nav: StateFlow<NavState> = navigator.state

    private val _locationGranted = MutableStateFlow(LocationPermission.isGranted(app))

    /** 위치 권한이 있는지. 없으면 S-03에 권한 요청 상태 상자가 보인다 */
    val locationGranted: StateFlow<Boolean> = _locationGranted.asStateFlow()

    init {
        // 주행이 시작되면 어느 화면에서든 S-08로 덮고, 정차하면 보던 화면으로 돌아간다
        viewModelScope.launch {
            drivingState.isDriving().collect { navigator.onDrivingChanged(it) }
        }
    }

    fun open(screen: Screen.Parked) = navigator.open(screen)

    fun back() = navigator.back()

    fun selectTab(tab: Tab) = navigator.selectTab(tab)

    /** 권한 창이 닫혔을 때와 앱으로 돌아왔을 때 다시 확인한다 */
    fun refreshLocationPermission() {
        _locationGranted.value = LocationPermission.isGranted(getApplication())
    }

    fun finishOnboarding() {
        // TODO: S-02에서 고른 알림 조건을 저장하고, [나중에 할게요]면 주행 중 알림을 끈 채로 둔다
        onboardingStore.isDone = true
        navigator.finishOnboarding()
    }

    /** S-08의 [주변 단지 듣기] */
    fun listenNearby() {
        // TODO: 차 앞쪽 180도 안에서 가장 가까운 단지를 찾아 TextToSpeech로 말한다. 지금은 예시 단지로 S-09만 연다
        navigator.showAlert(SAMPLE_COMPLEX_ID)
    }

    fun openHeardComplex() = navigator.openHeardComplex()

    fun dismissHeardBanner() = navigator.dismissHeardBanner()
}

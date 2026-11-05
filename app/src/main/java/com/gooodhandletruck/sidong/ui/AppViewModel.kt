package com.gooodhandletruck.sidong.ui

import android.app.Application
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gooodhandletruck.sidong.R
import com.gooodhandletruck.sidong.SidongApp
import com.gooodhandletruck.sidong.data.CarLocation
import com.gooodhandletruck.sidong.data.DrivingStateRepository
import com.gooodhandletruck.sidong.data.LocationPermission
import com.gooodhandletruck.sidong.data.LocationState
import com.gooodhandletruck.sidong.data.OnboardingStore
import com.gooodhandletruck.sidong.data.RouteTarget
import com.gooodhandletruck.sidong.data.model.ComplexSummary
import com.gooodhandletruck.sidong.ui.component.label
import com.gooodhandletruck.sidong.ui.component.placeOf
import com.gooodhandletruck.sidong.ui.format.RelativePlace
import com.gooodhandletruck.sidong.ui.format.formatAreaVoice
import com.gooodhandletruck.sidong.ui.format.formatPriceVoice
import com.gooodhandletruck.sidong.ui.navigation.NavState
import com.gooodhandletruck.sidong.ui.navigation.Navigator
import com.gooodhandletruck.sidong.ui.navigation.Screen
import com.gooodhandletruck.sidong.ui.navigation.Tab
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** [길 안내]를 누른 뒤 앱 안에 3초 띄우는 한 줄 */
enum class RouteMessage { STARTED, FAILED }

/** 주행 중에 말한 단지. S-09에 이름을 보여 주고, [다시 듣기]는 [speech]를 다시 말한다 */
data class SpokenComplex(
    val complexId: String,
    val name: String,
    val speech: String,
    val saved: Boolean
)

/**
 * 앱 전체 화면 이동과 주행 상태를 맡는다. 화면은 [nav]만 보고 그리고, 누르면 아래 함수를 부른다.
 * 의존성은 DI 프레임워크 없이 여기서 직접 만든다.
 */
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as SidongApp).container
    private val repository = container.complexRepository
    private val locationRepository = container.locationRepository
    private val speech = container.speechRepository
    private val navigation = container.navigationRepository
    private val onboardingStore = OnboardingStore(app)
    private val drivingState = DrivingStateRepository(app)
    private val navigator = Navigator(viewModelScope, onboardingDone = onboardingStore.isDone)

    val nav: StateFlow<NavState> = navigator.state

    private val _locationGranted = MutableStateFlow(LocationPermission.isGranted(app))

    /** 위치 권한이 있는지. 없으면 S-03에 권한 요청 상태 상자가 보인다 */
    val locationGranted: StateFlow<Boolean> = _locationGranted.asStateFlow()

    private val _routeMessage = MutableStateFlow<RouteMessage?>(null)

    /** [길 안내]를 누른 뒤 3초 동안 보이는 한 줄. 없으면 null */
    val routeMessage: StateFlow<RouteMessage?> = _routeMessage.asStateFlow()

    private var routeMessageJob: Job? = null

    private val _spoken = MutableStateFlow<SpokenComplex?>(null)

    /** 주행 중에 마지막으로 말한 단지. S-09와 '방금 들은 단지' 띠가 쓴다 */
    val spoken: StateFlow<SpokenComplex?> = _spoken.asStateFlow()

    private var listenJob: Job? = null

    init {
        // 주행이 시작되면 어느 화면에서든 S-08로 덮고, 정차하면 보던 화면으로 돌아간다
        viewModelScope.launch {
            drivingState.isDriving().collect { navigator.onDrivingChanged(it) }
        }
        // 길 안내로 도착하면 정차하는 대로 그 단지의 S-04를 연다
        viewModelScope.launch {
            navigation.arrival.filterNotNull().collect { complexId ->
                navigator.onArrived(complexId)
                navigation.consumeArrival()
            }
        }
    }

    /**
     * S-04 [길 안내]와 S-05 카드의 [길 안내]. 기본 내비에 목적지를 보내고 "길 안내를 시작했어요"를 3초 띄운다.
     * 출발하면 주행 상태가 바뀌어 S-08로 덮인다.
     */
    fun startRoute(target: RouteTarget) {
        _routeMessage.value = if (navigation.startRoute(target)) RouteMessage.STARTED else RouteMessage.FAILED
        routeMessageJob?.cancel()
        routeMessageJob = viewModelScope.launch {
            delay(ROUTE_MESSAGE_DURATION)
            _routeMessage.value = null
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

    /**
     * S-08의 [주변 단지 듣기]. 차 앞쪽 180도 안에서 가장 가까운 단지를 찾아(brief) 방향 → 이름 → 가격 순으로 말하고 S-09로 간다.
     * 이미 찾거나 말하는 중이면 누른 것을 버린다.
     */
    fun listenNearby() {
        if (listenJob?.isActive == true || speech.speaking.value) return
        listenJob = viewModelScope.launch {
            val car = currentCar() ?: return@launch say(R.string.voice_location_unavailable)
            val complex = try {
                repository.brief(car.lat, car.lon, car.heading)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "앞쪽 단지를 불러오지 못했어요", e)
                return@launch say(R.string.voice_load_failed)
            }
            if (complex == null) return@launch say(R.string.voice_none_nearby)

            val text = speechFor(complex, car.placeOf(complex.lat, complex.lon))
            _spoken.value = SpokenComplex(complex.complexId, complex.name, text, complex.saved)
            navigator.showAlert(complex.complexId)
            if (speech.speak(text)) recordHeard(complex.complexId)
        }
    }

    /** 말한 단지를 HEARD로 남겨 S-06 오늘 들은 단지에 보여 준다. 실패해도 주행 화면은 그대로 둔다 */
    private fun recordHeard(complexId: String) {
        viewModelScope.launch {
            try {
                repository.recordHeard(complexId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "들은 기록을 남기지 못했어요", e)
            }
        }
    }

    /** S-09의 [다시 듣기]. 말하는 중이면 버린다 */
    fun replaySpoken() {
        _spoken.value?.let { speech.speak(it.speech) }
    }

    /** S-09의 [저장]. 누르면 바로 "저장했어요"로 바뀌고 더 누를 수 없다. 취소는 정차 중 S-04에서 한다 */
    fun saveSpoken() {
        val spoken = _spoken.value?.takeUnless { it.saved } ?: return
        _spoken.value = spoken.copy(saved = true)
        viewModelScope.launch {
            try {
                repository.setSaved(spoken.complexId, true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "주행 중 저장을 보내지 못했어요", e)
                // TODO: 통신이 끊기면 다시 보낸다(API 검토 '고칠 것' 7번)
                _spoken.update { if (it?.complexId == spoken.complexId) it.copy(saved = false) else it }
            }
        }
    }

    /** 지금 차 위치. S-03이 안 보여 위치 구독이 멈춰 있으면 잠깐 켜서 첫 위치를 기다린다 */
    private suspend fun currentCar(): CarLocation? = withTimeoutOrNull(LOCATION_TIMEOUT) {
        locationRepository.state.filterIsInstance<LocationState.Found>().first().location
    }

    private fun say(@StringRes text: Int) {
        speech.speak(getApplication<Application>().getString(text))
    }

    /** 음성 문구(화면 설계서 S-09 '음성 문구'). 단지 이름 뒤에는 조사 대신 쉼표를 둔다 */
    private fun speechFor(complex: ComplexSummary, place: RelativePlace): String {
        val app = getApplication<Application>()
        val direction = app.getString(place.direction.label)
        val price = complex.price
        val average = price?.avgPriceManwon
        return when {
            price == null -> app.getString(R.string.voice_no_trades, direction, complex.name)
            average == null -> app.getString(
                R.string.voice_no_recent,
                direction,
                complex.name,
                price.periodDays / DAYS_PER_MONTH
            )
            else -> app.getString(
                R.string.voice_average,
                direction,
                complex.name,
                formatAreaVoice(price.areaM2),
                price.periodDays / DAYS_PER_MONTH,
                formatPriceVoice(average)
            )
        }
    }

    fun openHeardComplex() = navigator.openHeardComplex()

    fun dismissHeardBanner() = navigator.dismissHeardBanner()

    private companion object {
        const val TAG = "App"
        const val DAYS_PER_MONTH = 30
        val ROUTE_MESSAGE_DURATION = 3.seconds
        val LOCATION_TIMEOUT = 5.seconds
    }
}

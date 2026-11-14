package com.gooodhandletruck.zippick.ui

import android.app.Application
import android.os.SystemClock
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gooodhandletruck.zippick.R
import com.gooodhandletruck.zippick.ZippickApp
import com.gooodhandletruck.zippick.data.AlertSettings
import com.gooodhandletruck.zippick.data.CarLocation
import com.gooodhandletruck.zippick.data.DestinationEvent
import com.gooodhandletruck.zippick.data.DrivingStateRepository
import com.gooodhandletruck.zippick.data.LocationPermission
import com.gooodhandletruck.zippick.data.LocationState
import com.gooodhandletruck.zippick.data.OnboardingStore
import com.gooodhandletruck.zippick.data.ProximityTarget
import com.gooodhandletruck.zippick.data.RouteTarget
import com.gooodhandletruck.zippick.data.model.ComplexSummary
import com.gooodhandletruck.zippick.ui.component.label
import com.gooodhandletruck.zippick.ui.component.placeOf
import com.gooodhandletruck.zippick.ui.format.RelativePlace
import com.gooodhandletruck.zippick.ui.format.formatAreaVoice
import com.gooodhandletruck.zippick.ui.format.formatPriceVoice
import com.gooodhandletruck.zippick.ui.navigation.NavState
import com.gooodhandletruck.zippick.ui.navigation.Navigator
import com.gooodhandletruck.zippick.ui.navigation.Screen
import com.gooodhandletruck.zippick.ui.navigation.Tab
import java.time.LocalDate
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

    private val container = (app as ZippickApp).container
    private val repository = container.complexRepository
    private val locationRepository = container.locationRepository
    private val speech = container.speechRepository
    private val navigation = container.navigationRepository
    private val announcedStore = container.announcedStore
    private val onboardingStore = OnboardingStore(app)
    private val drivingState = DrivingStateRepository(app)
    private val navigator = Navigator(viewModelScope, onboardingDone = onboardingStore.isDone)

    val nav: StateFlow<NavState> = navigator.state

    /** 주행 중 자동 알림 설정(S-02·S-07). S-06 빈 목록과 S-08 상태 문구가 쓴다 */
    val alertSettings: StateFlow<AlertSettings> = container.alertSettingsRepository.settings

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

    /** 근접 알림을 건 단지. 들어갔다는 이벤트가 오면 여기서 이름·가격을 찾는다 */
    private var geofenceTargets: Map<String, ComplexSummary> = emptyMap()

    // 마지막 자동 알림 시각(SystemClock.elapsedRealtime). 알림 사이를 1분 이상 띄운다
    private var lastAutoAlertAt: Long? = null

    // 길 안내 목적지 둘레에 들어와 있는 단지. 여기서 정차하면 도착으로 본다
    private var nearDestinationId: String? = null

    init {
        // 주행이 시작되면 어느 화면에서든 S-08로 덮고, 정차하면 보던 화면으로 돌아간다
        viewModelScope.launch {
            drivingState.isDriving().collect { driving ->
                navigator.onDrivingChanged(driving)
                // 내비 도착 이벤트가 오지 않아도 목적지 둘레에서 정차하면 도착으로 본다
                if (!driving) nearDestinationId?.let(::arriveByProximity)
            }
        }
        // 길 안내로 도착하면 정차하는 대로 그 단지의 S-04를 연다
        viewModelScope.launch {
            navigation.arrival.filterNotNull().collect { complexId ->
                navigator.onArrived(complexId)
                navigation.consumeArrival()
            }
        }
        // 주행 중 자동 알림: 알림 설정·위치 권한이 바뀌거나 주행을 시작하면 근접 알림을 다시 건다
        viewModelScope.launch {
            combine(
                alertSettings,
                nav.map { it.isDriving }.distinctUntilChanged(),
                locationGranted
            ) { settings, driving, granted -> Triple(settings, driving, granted) }
                .collectLatest { (settings, driving, granted) -> refreshGeofences(settings, driving, granted) }
        }
        viewModelScope.launch {
            locationRepository.entered.collect(::onEnteredComplex)
        }
        // 길 안내를 보낸 단지 둘레에 근접 알림을 건다. 도착하거나 안내를 취소하면 지운다
        viewModelScope.launch {
            combine(navigation.destination, locationGranted) { target, granted -> target.takeIf { granted } }
                .collect { target ->
                    if (target == null) nearDestinationId = null
                    locationRepository.watchDestination(
                        target?.let { ProximityTarget(it.complexId, it.lat, it.lon) },
                        ARRIVAL_RADIUS_METERS
                    )
                }
        }
        viewModelScope.launch {
            locationRepository.destinationEvents.collect(::onDestinationEvent)
        }
    }

    private fun onDestinationEvent(event: DestinationEvent) {
        when (event) {
            is DestinationEvent.Entered -> {
                nearDestinationId = event.complexId
                // 이미 서 있는 채로 둘레에 들어왔으면 바로 도착이다
                if (!nav.value.isDriving) arriveByProximity(event.complexId)
            }
            is DestinationEvent.Exited -> if (nearDestinationId == event.complexId) nearDestinationId = null
        }
    }

    /**
     * 내비 도착 이벤트(NaviHelper)가 오지 않을 때의 도착(화면 설계서 '주행·정차 전환 규칙': 근접 알림 + 정차).
     * 그 단지의 S-04를 '도착했어요' 띠와 함께 열고, 뒤늦게 온 내비 도착 이벤트로 또 열지 않게 목적지를 지운다.
     */
    private fun arriveByProximity(complexId: String) {
        nearDestinationId = null
        navigation.clearDestination()
        navigator.onArrived(complexId)
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

    /** 첫 실행 S-02의 두 버튼. 알림 조건 저장(또는 끄기)은 S-02가 먼저 한다 */
    fun finishOnboarding() {
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
            announce(complex, car)
        }
    }

    /**
     * 단지를 방향 → 이름 → 가격 순으로 말하고 S-09로 바꾼다. 말했으면 HEARD로 남기고 오늘 알린 단지로 기억한다.
     * [주변 단지 듣기]와 자동 알림이 함께 쓴다. 말하지 못했으면(말하는 중, 오디오 포커스 실패) false다.
     */
    private fun announce(complex: ComplexSummary, car: CarLocation): Boolean {
        val text = speechFor(complex, car.placeOf(complex.lat, complex.lon))
        if (!speech.speak(text)) return false
        _spoken.value = SpokenComplex(complex.complexId, complex.name, text, complex.saved)
        navigator.showAlert(complex.complexId)
        announcedStore.markAnnounced(complex.complexId)
        recordHeard(complex.complexId)
        return true
    }

    /**
     * 자동 알림의 근접 알림을 다시 건다(화면 설계서 '알림 규칙'). 알림을 끄거나 위치 권한이 없으면 모두 지운다.
     * 주행을 시작할 때 지금 위치 둘레에서 조건에 맞는 단지를 받아(geofences) 반경 300m로 건다.
     * 정차 중에는 걸어 둔 것을 그대로 둔다. 정차 중에 들어간 단지는 말하지 않는다.
     */
    private suspend fun refreshGeofences(settings: AlertSettings, driving: Boolean, granted: Boolean) {
        val conditions = settings.conditions
        if (!settings.isOn || conditions == null || !granted) {
            geofenceTargets = emptyMap()
            locationRepository.watchProximity(emptyList(), ALERT_RADIUS_METERS)
            return
        }
        if (!driving) return
        val car = currentCar() ?: return
        val targets = try {
            repository.geofences(car.lat, car.lon, conditions)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "근접 알림을 걸 단지를 불러오지 못했어요", e)
            return
        }
        geofenceTargets = targets.associateBy { it.complexId }
        locationRepository.watchProximity(
            targets.map { ProximityTarget(it.complexId, it.lat, it.lon) },
            ALERT_RADIUS_METERS
        )
    }

    /**
     * 근접 알림을 건 단지 둘레에 들어갔을 때. 주행 중이고, 말하는 중이 아니고, 오늘 처음이고,
     * 마지막 알림에서 1분이 지났을 때만 말한다. 아니면 쌓지 않고 버린다.
     */
    private fun onEnteredComplex(complexId: String) {
        val complex = geofenceTargets[complexId] ?: return
        val state = nav.value
        if (!state.isDriving || !state.onboardingDone || !alertSettings.value.isOn) return
        if (speech.speaking.value || listenJob?.isActive == true) return
        if (announcedStore.wasAnnouncedToday(complexId)) return
        val now = SystemClock.elapsedRealtime()
        val last = lastAutoAlertAt
        if (last != null && now - last < MIN_ALERT_GAP_MS) return
        listenJob = viewModelScope.launch {
            val car = currentCar() ?: return@launch
            if (announce(complex, car)) lastAutoAlertAt = now
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
        val lastTrade = price?.lastTrade
        return when {
            price == null -> app.getString(R.string.voice_no_trades, direction, complex.name)
            average == null && lastTrade != null -> app.getString(
                R.string.voice_last_trade,
                direction,
                complex.name,
                formatAreaVoice(price.areaM2),
                tradeMonthVoice(lastTrade.contractDate),
                formatPriceVoice(lastTrade.priceManwon)
            )
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

    /** 마지막 거래의 계약 월. 올해면 "11월", 아니면 "2025년 12월"(최대 1년 전 거래가 올 수 있다) */
    private fun tradeMonthVoice(date: LocalDate): String =
        if (date.year == LocalDate.now().year) "${date.monthValue}월" else "${date.year}년 ${date.monthValue}월"

    fun openHeardComplex() = navigator.openHeardComplex()

    fun dismissHeardBanner() = navigator.dismissHeardBanner()

    private companion object {
        const val TAG = "App"
        const val DAYS_PER_MONTH = 30
        val ROUTE_MESSAGE_DURATION = 3.seconds
        val LOCATION_TIMEOUT = 5.seconds

        // 단지 반경 300m에 들어가면 말한다. 반경은 시연하며 조정한다(화면 설계서 '알림 규칙')
        const val ALERT_RADIUS_METERS = 300f
        const val MIN_ALERT_GAP_MS = 60_000L

        // 목적지 둘레 반경. 단지 대표 좌표에서 이만큼 안에 서면 도착으로 본다(시연하며 조정한다)
        const val ARRIVAL_RADIUS_METERS = 200f
    }
}

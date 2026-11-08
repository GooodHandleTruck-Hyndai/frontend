package com.gooodhandletruck.zippick.data

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.listener.ProximityAlertListener
import ai.pleos.playground.location.fused.model.LocationData
import ai.pleos.playground.location.fused.model.ProximityAlertEvent
import ai.pleos.playground.location.fused.model.ProximityAlertRequest
import ai.pleos.playground.location.fused.model.ProximityTransition
import android.content.Context
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 위치를 받는 상태 */
sealed interface LocationState {

    /** 첫 위치를 기다리는 중 */
    data object Searching : LocationState

    /** 위치를 받았다. 이후 위치가 바뀔 때마다 새 값이 온다 */
    data class Found(val location: CarLocation) : LocationState

    /** 위치 서비스가 없거나, 첫 위치 전에 오류가 났거나, 15초 안에 위치가 오지 않았다 */
    data object Unavailable : LocationState
}

/** 근접 알림을 걸 단지 하나. 이 둘레에 들어가면 주행 중에 소리로 알린다 */
data class ProximityTarget(val complexId: String, val lat: Double, val lon: Double)

/** 길 안내 목적지 둘레에 들어오거나 나갔다. 내비 도착 이벤트가 오지 않을 때 도착을 대신 판단하는 데 쓴다 */
sealed interface DestinationEvent {
    val complexId: String

    data class Entered(override val complexId: String) : DestinationEvent

    data class Exited(override val complexId: String) : DestinationEvent
}

/**
 * 차의 지금 위치와 향한 쪽을 알려 주는 곳. 플레오스 FusedLocation SDK를 감싼다(구글 Fused Location이 아니다).
 * 주행 중 자동 알림에 쓰는 근접 알림([watchProximity])도 같은 SDK 연결로 건다.
 * [state]를 구독하는 곳이 있을 때만 위치를 받고, 구독이 모두 끝나면 5초 뒤 멈춘다.
 * 위치 권한(FUSED_LOCATION)이 있을 때만 구독한다. 권한이 없으면 SDK가 SecurityException을 던진다.
 *
 * 에뮬레이터에서 위치 옮기기: adb emu geo fix <경도> <위도>. 목업 단지는 강남역(127.0276 37.4979) 둘레에 있다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LocationRepository(context: Context, scope: CoroutineScope) {

    private val fusedLocation = FusedLocation(context.applicationContext)

    // SDK 안내: initialize는 앱이 만들어질 때 한 번. 처음 구독할 때 연결하고 앱이 끝날 때까지 둔다
    private val initialized = AtomicBoolean(false)

    /** [retry]를 부를 때마다 1씩 올라 위치 요청을 새로 보낸다 */
    private val attempts = MutableStateFlow(0)

    /**
     * 차 위치. 구독하는 화면이 없어 위치 받기를 멈추면 마지막 위치를 비우고(Searching) 다음 구독 때 새로 받는다.
     * 주행 중 [주변 단지 듣기]가 S-03을 마지막으로 본 예전 위치를 쓰지 않게 하려는 것이다.
     */
    val state: StateFlow<LocationState> = attempts
        .flatMapLatest { locationUpdates() }
        .stateIn(
            scope,
            SharingStarted.WhileSubscribed(stopTimeoutMillis = STOP_DELAY_MS, replayExpirationMillis = 0),
            LocationState.Searching
        )

    /** 위치를 못 받았을 때 [다시 시도] */
    fun retry() = attempts.update { it + 1 }

    private val proximityLock = Any()

    // proximityLock 안에서만 쓴다. 단지 id → SDK가 알려 준 alertId
    private val proximityAlertIds = mutableMapOf<String, String>()

    // watchProximity를 부를 때마다 1씩 올라, 예전 목록에서 늦게 온 등록·이벤트를 버린다
    @Volatile
    private var proximityGeneration = 0

    // 길 안내 목적지의 근접 알림. proximityLock 안에서만 쓴다
    private var destinationAlertId: String? = null

    @Volatile
    private var destinationGeneration = 0

    private val _destinationEvents = MutableSharedFlow<DestinationEvent>(extraBufferCapacity = PROXIMITY_EVENT_BUFFER)

    /** 길 안내 목적지 둘레에 들어오고 나간 것 */
    val destinationEvents: SharedFlow<DestinationEvent> = _destinationEvents.asSharedFlow()

    private val _entered = MutableSharedFlow<String>(extraBufferCapacity = PROXIMITY_EVENT_BUFFER)

    /** 근접 알림을 건 단지 둘레에 들어갔을 때 그 단지 id */
    val entered: SharedFlow<String> = _entered.asSharedFlow()

    /**
     * 근접 알림(ENTER, 반경 [radiusMeters])을 [targets]로 바꿔 건다. 빈 목록이면 걸어 둔 것을 모두 지운다.
     * 위치 서비스가 없거나 위치 권한이 없으면 걸지 않는다. SDK 이벤트는 바인더 스레드에서 온다.
     */
    fun watchProximity(targets: List<ProximityTarget>, radiusMeters: Float) {
        synchronized(proximityLock) { replaceProximityAlerts(targets, radiusMeters) }
    }

    // proximityLock 안에서 부른다
    private fun replaceProximityAlerts(targets: List<ProximityTarget>, radiusMeters: Float) {
        val generation = ++proximityGeneration
        proximityAlertIds.values.forEach(::removeProximityAlert)
        proximityAlertIds.clear()
        if (targets.isEmpty() || !fusedLocation.isSupported()) return
        try {
            if (initialized.compareAndSet(false, true)) fusedLocation.initialize()
            targets.forEach { target ->
                val request = ProximityAlertRequest(
                    latitude = target.lat,
                    longitude = target.lon,
                    radiusMeters = radiusMeters,
                    transitionTypes = setOf(ProximityTransition.ENTER)
                )
                fusedLocation.addProximityAlert(request, proximityListener(target.complexId, generation))
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "위치 권한이 없어 근접 알림을 걸지 못했어요", e)
        }
    }

    private fun proximityListener(complexId: String, generation: Int) = object : ProximityAlertListener {
        override fun onRegistered(alertId: String) {
            synchronized(proximityLock) {
                // 그사이 목록이 바뀌었으면 늦게 등록된 것을 바로 지운다
                if (generation == proximityGeneration) {
                    proximityAlertIds[complexId] = alertId
                } else {
                    removeProximityAlert(alertId)
                }
            }
        }

        override fun onTransition(event: ProximityAlertEvent) {
            if (event.transition == ProximityTransition.ENTER && generation == proximityGeneration) {
                _entered.tryEmit(complexId)
            }
        }

        override fun onError(exception: Exception) {
            Log.w(TAG, "근접 알림 오류($complexId)", exception)
        }
    }

    /**
     * 길 안내 목적지 둘레(반경 [radiusMeters])에 근접 알림(ENTER·EXIT)을 건다. null이면 지운다.
     * 자동 알림의 근접 알림([watchProximity])과 따로 건다.
     */
    fun watchDestination(target: ProximityTarget?, radiusMeters: Float) {
        synchronized(proximityLock) {
            val generation = ++destinationGeneration
            destinationAlertId?.let(::removeProximityAlert)
            destinationAlertId = null
            if (target == null || !fusedLocation.isSupported()) return
            try {
                if (initialized.compareAndSet(false, true)) fusedLocation.initialize()
                val request = ProximityAlertRequest(
                    latitude = target.lat,
                    longitude = target.lon,
                    radiusMeters = radiusMeters,
                    transitionTypes = setOf(ProximityTransition.ENTER, ProximityTransition.EXIT)
                )
                fusedLocation.addProximityAlert(request, destinationListener(target.complexId, generation))
            } catch (e: SecurityException) {
                Log.w(TAG, "위치 권한이 없어 목적지 근접 알림을 걸지 못했어요", e)
            }
        }
    }

    private fun destinationListener(complexId: String, generation: Int) = object : ProximityAlertListener {
        override fun onRegistered(alertId: String) {
            synchronized(proximityLock) {
                if (generation == destinationGeneration) {
                    destinationAlertId = alertId
                } else {
                    removeProximityAlert(alertId)
                }
            }
        }

        override fun onTransition(event: ProximityAlertEvent) {
            if (generation != destinationGeneration) return
            val destinationEvent = when (event.transition) {
                ProximityTransition.ENTER -> DestinationEvent.Entered(complexId)
                ProximityTransition.EXIT -> DestinationEvent.Exited(complexId)
            }
            _destinationEvents.tryEmit(destinationEvent)
        }

        override fun onError(exception: Exception) {
            Log.w(TAG, "목적지 근접 알림 오류($complexId)", exception)
        }
    }

    private fun removeProximityAlert(alertId: String) {
        try {
            fusedLocation.removeProximityAlert(alertId)
        } catch (e: SecurityException) {
            Log.w(TAG, "근접 알림을 지우지 못했어요", e)
        }
    }

    /** 구독하는 동안 실시간 위치를 받는다. 실시간 위치가 늦을 수 있어 마지막 위치도 함께 묻는다(공식 샘플 안내) */
    private fun locationUpdates(): Flow<LocationState> = callbackFlow {
        send(LocationState.Searching)
        val gotFix = AtomicBoolean(false)

        // SDK 콜백은 바인더 스레드에서 온다. trySend는 어느 스레드에서 불러도 된다
        val live = object : FusedLocationListener {
            override fun onChanged(location: LocationData?) {
                // SDK 문서: null을 올바른 위치로 다루지 않는다
                val carLocation = location?.toCarLocation() ?: return
                gotFix.set(true)
                trySend(LocationState.Found(carLocation))
            }

            override fun onError(exception: Exception) {
                Log.w(TAG, "위치를 받지 못했어요", exception)
                // 이미 받은 위치가 있으면 그 위치를 계속 쓴다
                if (!gotFix.get()) trySend(LocationState.Unavailable)
            }
        }
        val lastKnown = object : FusedLocationListener {
            override fun onChanged(location: LocationData?) {
                val carLocation = location?.toCarLocation() ?: return
                // 실시간 위치가 먼저 왔으면 그쪽이 더 새롭다
                if (gotFix.compareAndSet(false, true)) trySend(LocationState.Found(carLocation))
            }

            override fun onError(exception: Exception) {
                // 저장된 위치가 없을 뿐일 수 있다. 실시간 위치와 시간 제한으로 판단한다
                Log.i(TAG, "마지막 위치가 없어요: ${exception.message}")
            }
        }

        val started = start(live, lastKnown)
        if (started) {
            launch {
                delay(FIRST_FIX_TIMEOUT_MS)
                if (!gotFix.get()) send(LocationState.Unavailable)
            }
        } else {
            send(LocationState.Unavailable)
        }

        awaitClose {
            if (started) {
                try {
                    fusedLocation.unregisterFusedLocationCallback(live)
                } catch (e: SecurityException) {
                    Log.w(TAG, "위치 구독을 풀지 못했어요", e)
                }
            }
        }
    }.conflate()

    /** 서비스에 연결하고 위치를 요청한다. 서비스가 없거나 권한이 없으면 false */
    private fun start(live: FusedLocationListener, lastKnown: FusedLocationListener): Boolean {
        if (!fusedLocation.isSupported()) {
            Log.w(TAG, "플레오스 위치 서비스가 없는 기기예요")
            return false
        }
        return try {
            if (initialized.compareAndSet(false, true)) fusedLocation.initialize()
            // 서비스 연결 전에 부르면 SDK가 줄 세워 두었다가 연결되면 보낸다
            fusedLocation.registerFusedLocationCallback(live)
            fusedLocation.getLastKnownLocation(lastKnown)
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "위치 권한이 없어요", e)
            false
        }
    }

    private companion object {
        const val TAG = "Location"
        const val STOP_DELAY_MS = 5_000L
        const val FIRST_FIX_TIMEOUT_MS = 15_000L
        const val PROXIMITY_EVENT_BUFFER = 8
    }
}

/**
 * SDK 위치를 앱 위치로 바꾼다. 좌표가 올바르지 않으면 null.
 * TODO: heading의 단위와 기준이 Pleos 문서에 없다. 북쪽 0도·시계 방향(도)으로 보고 0~360으로 맞춘다.
 *  실제 차에서 방향 칩이 창밖과 맞는지 확인해야 한다
 */
internal fun LocationData.toCarLocation(): CarLocation? {
    val valid = latitude.isFinite() && longitude.isFinite() && latitude in -90.0..90.0 && longitude in -180.0..180.0
    if (!valid) return null
    val degrees = heading.toDouble()
    return CarLocation(
        lat = latitude,
        lon = longitude,
        heading = if (degrees.isFinite()) (degrees % 360 + 360) % 360 else 0.0
    )
}

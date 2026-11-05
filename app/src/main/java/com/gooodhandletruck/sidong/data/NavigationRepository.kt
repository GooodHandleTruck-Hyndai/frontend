package com.gooodhandletruck.sidong.data

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.DestinationArrivedInfo
import ai.pleos.playground.navi.data.RouteInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.util.Log
import com.gooodhandletruck.sidong.data.model.ComplexDetail
import com.gooodhandletruck.sidong.data.model.ComplexSummary
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 길 안내로 보낼 단지. 단지 상세와 저장 목록의 카드가 같은 값을 갖고 있다 */
data class RouteTarget(
    val complexId: String,
    val name: String,
    val address: String,
    val lat: Double,
    val lon: Double
)

fun ComplexDetail.toRouteTarget() = RouteTarget(complexId, name, address, lat, lon)

fun ComplexSummary.toRouteTarget() = RouteTarget(complexId, name, address, lat, lon)

/**
 * 길 안내(플레오스 NaviHelper). 기본 내비 앱에 단지를 목적지로 보내고, 도착하면 [arrival]로 알린다.
 * 처음 길 안내를 보낼 때 NaviHelper를 연결한다. 연결 전에 보낸 요청은 SDK가 모아 두었다가 연결되면 보낸다.
 * NaviHelper 이벤트는 바인더 스레드에서 오므로 StateFlow로만 넘긴다.
 */
class NavigationRepository(context: Context) {

    private val naviHelper = NaviHelper(context.applicationContext)
    private val initialized = AtomicBoolean(false)

    /** 마지막으로 길 안내를 보낸 단지. 도착하면 이 단지의 S-04를 연다 */
    @Volatile
    private var destination: RouteTarget? = null

    private val _arrival = MutableStateFlow<String?>(null)

    /** 길 안내로 도착한 단지 id. 화면이 S-04를 열면 [consumeArrival]로 비운다 */
    val arrival: StateFlow<String?> = _arrival.asStateFlow()

    private val listener = object : NaviHelperEventListener {
        override fun onDestinationArrived(destinationArrivedInfo: DestinationArrivedInfo) {
            val target = destination ?: return
            // 도착 정보에는 좌표가 없다. 내비에서 직접 다른 곳으로 간 도착을 잘못 받지 않게 이름이나 주소로 맞춰 본다
            if (!destinationArrivedInfo.matches(target)) {
                Log.i(TAG, "보낸 단지가 아닌 곳에 도착: ${destinationArrivedInfo.name}")
                return
            }
            destination = null
            _arrival.value = target.complexId
        }

        // 내비에서 [안내 종료]를 누르면 onRouteEnded가 온다. 도착 직전에도 올 수 있어 목적지는 지우지 않는다
        override fun onRouteCancelled() {
            destination = null
        }

        override fun onError(errorCode: NaviErrorCode) {
            Log.w(TAG, "내비 오류: $errorCode")
        }
    }

    /** 기본 내비 앱에 길 안내를 보낸다. 권한이 없어 보내지 못하면 false다 */
    fun startRoute(target: RouteTarget): Boolean {
        if (initialized.compareAndSet(false, true)) {
            naviHelper.initialize()
            naviHelper.addListener(listener)
        }
        return try {
            naviHelper.requestRoute(target.toRouteInfo())
            destination = target
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "길 안내 권한(NAVI_ROUTE_SEARCH)이 없어요", e)
            false
        }
    }

    fun consumeArrival() {
        _arrival.value = null
    }

    private companion object {
        const val TAG = "Navigation"
    }
}

/** 도착한 곳이 우리가 보낸 단지인지. 내비는 보낸 poiName을 목적지 이름으로 쓴다(에뮬레이터 확인) */
internal fun DestinationArrivedInfo.matches(target: RouteTarget): Boolean =
    name == target.name || address == target.address

// TODO: poiId·poiSubId에 넣을 값이 Pleos 문서에 없다. 공식 샘플은 내비 POI id("36khp…")와 "0"을 쓰는데,
//  단지는 내비 POI가 아니라서 비워 보낸다. 실제 차의 내비가 좌표로 길을 찾는지 확인해야 한다
internal fun RouteTarget.toRouteInfo() = RouteInfo(
    latitude = lat,
    longitude = lon,
    poiName = name,
    poiId = "",
    address = address,
    poiSubId = "0"
)

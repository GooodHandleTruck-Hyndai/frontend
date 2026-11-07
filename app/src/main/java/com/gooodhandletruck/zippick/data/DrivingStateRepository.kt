package com.gooodhandletruck.zippick.data

import android.car.Car
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 주행 중인지 알려 주는 곳.
 * AAOS UX 제한 값(isRequiresDistractionOptimization) 하나로만 판단한다. 기어(P·D)는 쓰지 않는다.
 *
 * 에뮬레이터에서 바꾸기: Extended controls → Car data에서 기어를 D(주행)·P(정차)로 바꾼다.
 * 이 에뮬레이터(사용자 빌드)는 adb로 바꿀 수 없어서, 디버그 빌드에서는 [debugOverride]로 흉내 낼 수 있다(MainActivity 참고).
 */
class DrivingStateRepository(context: Context) {

    private val appContext = context.applicationContext

    /** true면 주행 중. 차량 서비스에 연결하지 못하면 정차(false)로 본다. 디버그로 흉내 내는 값이 있으면 그 값을 쓴다. */
    fun isDriving(): Flow<Boolean> = combine(carIsDriving(), debugOverride) { car, override -> override ?: car }
        .distinctUntilChanged()

    private fun carIsDriving(): Flow<Boolean> = callbackFlow {
        val car = Car.createCar(appContext)
        val manager = car?.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager

        if (manager == null) {
            Log.w(TAG, "UX 제한 서비스에 연결하지 못해 정차로 봐요")
            send(false)
        } else {
            send(manager.currentCarUxRestrictions?.isRequiresDistractionOptimization == true)
            manager.registerListener { restrictions ->
                trySend(restrictions.isRequiresDistractionOptimization)
            }
        }

        awaitClose {
            manager?.unregisterListener()
            car?.disconnect()
        }
    }
        .catch { e ->
            Log.w(TAG, "주행 상태를 읽지 못해 정차로 봐요", e)
            emit(false)
        }
        .distinctUntilChanged()

    companion object {
        private const val TAG = "DrivingState"

        /** 디버그 빌드에서만 쓰는 주행 상태 흉내. null이면 차의 실제 값을 쓴다. 앱을 다시 켜면 null로 돌아간다 */
        val debugOverride = MutableStateFlow<Boolean?>(null)
    }
}

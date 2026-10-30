package com.gooodhandletruck.sidong.data

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map

/**
 * 표준 AAOS 차량 API로 속도·기어를 읽어오는 곳.
 * (React로 치면 외부 이벤트를 구독하는 커스텀 훅 같은 역할이에요.)
 *
 * 도어, 공조, 배터리처럼 플레오스 전용 데이터는 PleosVehicle.kt 를 참고하세요.
 */
class CarDataRepository(context: Context) {

    private val car: Car = requireNotNull(Car.createCar(context)) {
        "차량 서비스에 연결할 수 없어요 (AAOS 기기인지 확인)"
    }
    private val propertyManager =
        car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager

    /** 현재 속도 (km/h). 차량은 m/s 로 주니까 3.6을 곱해요. */
    fun speedKmh(): Flow<Float> =
        observe<Float>(VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_NORMAL)
            .map { it * 3.6f }

    /** 현재 기어 (P/R/N/D) */
    fun gear(): Flow<String> = observe<Int>(VehiclePropertyIds.GEAR_SELECTION, CarPropertyManager.SENSOR_RATE_ONCHANGE)
        .map { gearLabel(it) }

    fun release() = car.disconnect()

    /** 차량 속성 하나를 구독해서 Flow 로 바꿔주는 공통 함수 */
    private fun <T> observe(propertyId: Int, rate: Float): Flow<T> = callbackFlow {
        val callback = object : CarPropertyManager.CarPropertyEventCallback {
            override fun onChangeEvent(value: CarPropertyValue<*>) {
                @Suppress("UNCHECKED_CAST")
                trySend(value.value as T)
            }

            override fun onErrorEvent(propId: Int, zone: Int) {
                close(IllegalStateException("차량 속성 읽기 실패 (id=$propId)"))
            }
        }
        try {
            propertyManager.registerCallback(callback, propertyId, rate)
        } catch (e: Exception) {
            close(e) // 권한이 없으면 여기서 SecurityException
        }
        awaitClose { propertyManager.unregisterCallback(callback, propertyId) }
    }

    private fun gearLabel(gear: Int): String = when (gear) {
        GEAR_PARK -> "P"
        GEAR_REVERSE -> "R"
        GEAR_NEUTRAL -> "N"
        GEAR_DRIVE -> "D"
        else -> "?"
    }

    private companion object {
        // AAOS VehicleGear 값
        const val GEAR_NEUTRAL = 0x0001
        const val GEAR_REVERSE = 0x0002
        const val GEAR_PARK = 0x0004
        const val GEAR_DRIVE = 0x0008
    }
}

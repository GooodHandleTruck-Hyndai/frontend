package com.gooodhandletruck.sidong.data

import kotlin.math.abs

/** 이만큼 움직이면 주변 단지를 다시 불러온다. 거리 표기(10m 단위)가 크게 어긋나지 않는 선 */
private const val RELOAD_DISTANCE_METERS = 50.0

/** 이만큼 돌면 다시 불러온다. 방향 칩 한 칸이 45도라서 그 절반쯤 */
private const val RELOAD_TURN_DEGREES = 30.0

/**
 * 차의 지금 위치. [heading]은 차가 향한 쪽으로, 북쪽이 0도이고 시계 방향이다(0 이상 360 미만).
 * 목록 카드의 "오른쪽 · 340m"는 이 값으로 앱에서 계산한다.
 */
data class CarLocation(val lat: Double, val lon: Double, val heading: Double)

/** [previous]에서 목록을 다시 불러올 만큼 움직였는지. 50m 넘게 옮겼거나 30도 넘게 돌았으면 true */
fun CarLocation.movedFrom(previous: CarLocation): Boolean =
    distanceMeters(previous.lat, previous.lon, lat, lon) >= RELOAD_DISTANCE_METERS ||
        turnDegrees(previous.heading, heading) >= RELOAD_TURN_DEGREES

/** 두 방향 사이의 작은 쪽 각도(0~180). 350도와 10도 사이는 20도다 */
private fun turnDegrees(from: Double, to: Double): Double {
    val diff = abs(to - from) % 360
    return if (diff > 180) 360 - diff else diff
}

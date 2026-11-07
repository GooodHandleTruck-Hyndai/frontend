package com.gooodhandletruck.zippick.ui.format

import kotlin.math.roundToInt

/** 거리 표기: 1km 미만은 10m 단위("340m"), 1km부터는 소수 첫째 자리("1.2km", 딱 떨어지면 "3km") */
fun formatDistance(meters: Double): String {
    val roundedMeters = (meters / 10).roundToInt() * 10
    if (roundedMeters < 1000) return "${roundedMeters}m"
    val tenthsOfKm = (meters / 100).roundToInt()
    val whole = tenthsOfKm / 10
    val tenth = tenthsOfKm % 10
    return if (tenth == 0) "${whole}km" else "$whole.${tenth}km"
}

/** 전용면적 표기: 소수점 아래를 버린다(84.97 → "84㎡"). 부동산에서 부르는 평형 이름과 같다. */
fun formatArea(areaM2: Double): String = "${areaM2.toInt()}㎡"

/** 음성의 전용면적: "84㎡" 대신 읽을 수 있게 "84제곱미터"라고 쓴다 */
fun formatAreaVoice(areaM2: Double): String = "${areaM2.toInt()}제곱미터"

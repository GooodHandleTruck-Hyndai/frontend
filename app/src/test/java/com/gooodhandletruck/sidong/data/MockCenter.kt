package com.gooodhandletruck.sidong.data

/**
 * 목업 단지(assets/mock/complexes_nearby.json)가 둘러싼 중심, 강남역.
 * 에뮬레이터에서 adb emu geo fix 127.0276 37.4979로 옮기면 앱도 이 테스트와 같은 목록을 보여 준다.
 */
object MockCenter {
    const val LAT = 37.4979
    const val LON = 127.0276
}

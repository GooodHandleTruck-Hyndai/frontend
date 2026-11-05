package com.gooodhandletruck.sidong.data

import com.gooodhandletruck.sidong.data.model.NearbyResponse
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** [주변 단지 듣기]의 앞쪽 단지: 차가 향한 쪽 180도 안에서 가장 가까운 단지 하나. */
class FakeBriefTest {

    // 단위 테스트는 app 모듈 폴더에서 돈다
    private val mock: NearbyResponse =
        AppJson.decodeFromString(File("src/main/assets/mock/complexes_nearby.json").readText())

    private val lat = MockCenter.LAT
    private val lon = MockCenter.LON

    @Test
    fun aheadIsWithinNinetyDegreesEachSide() {
        assertTrue(isAhead(bearing = 0.0, heading = 0.0))
        assertTrue(isAhead(bearing = 90.0, heading = 0.0))
        assertTrue(isAhead(bearing = 270.0, heading = 0.0))
        assertFalse(isAhead(bearing = 135.0, heading = 0.0))
        assertTrue(isAhead(bearing = 10.0, heading = 300.0)) // 북쪽을 넘어가도 70도 차이다
    }

    @Test
    fun facingNorthPicksNearestInFront() {
        // 강남역에서 북쪽을 보면 왼쪽(서쪽) 250m의 새솔마을 1단지가 가장 가까운 앞쪽 단지다
        assertEquals("c-1003", mock.nearestAhead(lat, lon, heading = 0.0, radiusMeters = 1000)?.complexId)
    }

    @Test
    fun facingSouthPicksNearestBehindNorth() {
        // 남쪽을 보면 은하수 2단지(남쪽 330m)가 앞쪽이다
        assertEquals("c-1004", mock.nearestAhead(lat, lon, heading = 180.0, radiusMeters = 1000)?.complexId)
    }

    @Test
    fun nothingAheadInRangeIsNull() = assertNull(mock.nearestAhead(37.60, 127.10, heading = 0.0, radiusMeters = 1000))
}

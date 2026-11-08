package com.gooodhandletruck.zippick.data

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoTest {

    private val lat = MockCenter.LAT
    private val lon = MockCenter.LON

    @Test
    fun oneKilometerNorth() {
        // 위도 1도 = 지구 반지름 × π / 180 ≈ 111,194.9m
        val north = lat + 1000 / 111_194.9
        assertEquals(1000.0, distanceMeters(lat, lon, north, lon), 1.0)
    }

    @Test
    fun bearingToNorthAndEast() {
        assertEquals(0.0, bearingDegrees(lat, lon, lat + 0.01, lon), 0.01)
        assertEquals(90.0, bearingDegrees(lat, lon, lat, lon + 0.01), 0.1)
    }
}

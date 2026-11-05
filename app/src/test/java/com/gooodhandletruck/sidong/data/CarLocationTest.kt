package com.gooodhandletruck.sidong.data

import ai.pleos.playground.location.fused.model.LocationData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 목록을 다시 불러올지(50m·30도), SDK 위치를 앱 위치로 바꾸는 규칙을 본다. */
class CarLocationTest {

    private val here = CarLocation(lat = MockCenter.LAT, lon = MockCenter.LON, heading = 0.0)

    // 위도 1도 ≈ 111,195m
    private fun north(meters: Double) = here.copy(lat = here.lat + meters / 111_195.0)

    @Test
    fun smallMoveKeepsList() = assertFalse(north(30.0).movedFrom(here))

    @Test
    fun moveOverFiftyMetersReloads() = assertTrue(north(60.0).movedFrom(here))

    @Test
    fun smallTurnKeepsList() = assertFalse(here.copy(heading = 20.0).movedFrom(here))

    @Test
    fun turnOverThirtyDegreesReloads() = assertTrue(here.copy(heading = 40.0).movedFrom(here))

    @Test
    fun turnAcrossNorthIsSmallSide() {
        // 350도 → 10도는 20도만 돈 것이다
        assertFalse(here.copy(heading = 10.0).movedFrom(here.copy(heading = 350.0)))
    }

    @Test
    fun sdkLocationMapsToCarLocation() {
        val car = LocationData(MockCenter.LAT, MockCenter.LON, 30.0, 90f, 1f, 0L).toCarLocation()
        assertEquals(CarLocation(MockCenter.LAT, MockCenter.LON, 90.0), car)
    }

    @Test
    fun headingIsWrappedIntoZeroTo360() {
        val car = LocationData(MockCenter.LAT, MockCenter.LON, 0.0, -10f, 1f, 0L).toCarLocation()
        assertEquals(350.0, car!!.heading, 1e-6)
    }

    @Test
    fun invalidCoordinatesAreDropped() {
        assertNull(LocationData(Double.NaN, MockCenter.LON, 0.0, 0f, 1f, 0L).toCarLocation())
        assertNull(LocationData(91.0, MockCenter.LON, 0.0, 0f, 1f, 0L).toCarLocation())
    }
}

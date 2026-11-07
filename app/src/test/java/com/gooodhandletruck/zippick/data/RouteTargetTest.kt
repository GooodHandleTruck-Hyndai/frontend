package com.gooodhandletruck.zippick.data

import ai.pleos.playground.navi.data.DestinationArrivedInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 단지를 내비 목적지(RouteInfo)로 바꿀 때 좌표·이름·주소를 그대로 보내고, 도착은 이름이나 주소로 맞춰 본다. */
class RouteTargetTest {

    private val target = RouteTarget("c-1001", "한빛마을 3단지", "서울특별시 강남구 예시로 12", 37.5003, 127.0311)

    @Test
    fun arrivalAtSentComplexMatches() {
        assertTrue(DestinationArrivedInfo(1500, 13000, "한빛마을 3단지", "").matches(target))
        assertTrue(DestinationArrivedInfo(1500, 13000, "", "서울특별시 강남구 예시로 12").matches(target))
    }

    @Test
    fun arrivalElsewhereDoesNotMatch() =
        assertFalse(DestinationArrivedInfo(600, 4000, "강남역", "서울특별시 강남구 강남대로 396").matches(target))

    @Test
    fun targetMapsToRouteInfo() {
        val info = target.toRouteInfo()
        assertEquals(37.5003, info.latitude, 0.0)
        assertEquals(127.0311, info.longitude, 0.0)
        assertEquals("한빛마을 3단지", info.poiName)
        assertEquals("서울특별시 강남구 예시로 12", info.address)
    }
}

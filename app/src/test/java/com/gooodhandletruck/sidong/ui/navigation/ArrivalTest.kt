package com.gooodhandletruck.sidong.ui.navigation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 길 안내로 도착하면 정차 중에 그 단지의 S-04를 '도착했어요'와 함께 연다. */
class ArrivalTest {

    // 도착·주행 전환은 타이머를 쓰지 않아서 아무 작업 범위나 넘겨도 된다
    private val navigator = Navigator(CoroutineScope(Job()), onboardingDone = true)
    private val arrivedDetail = Screen.ComplexDetail("c-1001", arrived = true)

    @Test
    fun arrivalWhileParkedOpensDetailNow() {
        navigator.onArrived("c-1001")
        assertEquals(listOf(Screen.Nearby, arrivedDetail), navigator.state.value.backStack)
    }

    @Test
    fun arrivalWhileDrivingWaitsUntilParked() {
        navigator.onDrivingChanged(true)
        navigator.onArrived("c-1001")
        assertEquals(listOf<Screen.Parked>(Screen.Nearby), navigator.state.value.backStack)

        navigator.onDrivingChanged(false)
        assertEquals(listOf(Screen.Nearby, arrivedDetail), navigator.state.value.backStack)
        assertNull(navigator.state.value.pendingArrivalComplexId)
    }

    @Test
    fun arrivalReplacesOpenDetail() {
        navigator.open(Screen.ComplexDetail("c-1003"))
        navigator.onArrived("c-1001")
        assertEquals(listOf(Screen.Nearby, arrivedDetail), navigator.state.value.backStack)
    }
}

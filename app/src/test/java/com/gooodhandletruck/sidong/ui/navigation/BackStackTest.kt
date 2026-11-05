package com.gooodhandletruck.sidong.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

/** 백스택에 화면을 쌓는 규칙: 같은 화면은 다시 쌓지 않고, 상세 위의 상세는 바꾼다. */
class BackStackTest {

    private val detailA = Screen.ComplexDetail("c-1001")
    private val detailB = Screen.ComplexDetail("c-1003")

    @Test
    fun detailOpensOverList() =
        assertEquals(listOf(Screen.Nearby, detailA), listOf<Screen.Parked>(Screen.Nearby).pushed(detailA))

    @Test
    fun sameScreenIsNotStacked() =
        assertEquals(listOf(Screen.Nearby, detailA), listOf(Screen.Nearby, detailA).pushed(detailA))

    @Test
    fun otherDetailReplacesDetail() {
        // 1:2 창에서 목록의 다른 카드를 누르면 오른쪽 칸만 바뀌고, 닫으면 바로 목록이다
        assertEquals(listOf(Screen.Nearby, detailB), listOf(Screen.Nearby, detailA).pushed(detailB))
    }

    @Test
    fun otherScreensStack() {
        val settings = listOf<Screen.Parked>(Screen.Settings)
        assertEquals(
            listOf(Screen.Settings, Screen.AlertSetup(isOnboarding = false)),
            settings.pushed(Screen.AlertSetup(isOnboarding = false))
        )
    }
}

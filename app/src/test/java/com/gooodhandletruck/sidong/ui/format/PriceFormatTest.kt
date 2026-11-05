package com.gooodhandletruck.sidong.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

/** 화면 설계서 S-04 '가격 표시 규칙'의 예를 그대로 확인한다 */
class PriceFormatTest {

    @Test
    fun eokAndManWithComma() = assertEquals("12억 3,000만", formatPrice(123000))

    @Test
    fun eokOnlyWhenManIsZero() = assertEquals("12억", formatPrice(120000))

    @Test
    fun manOnlyBelowOneEok() = assertEquals("9,500만", formatPrice(9500))

    @Test
    fun smallManHasNoComma() = assertEquals("1억 50만", formatPrice(10050))

    @Test
    fun largeEokHasComma() = assertEquals("1,234억 5,678만", formatPrice(12_345_678))
}

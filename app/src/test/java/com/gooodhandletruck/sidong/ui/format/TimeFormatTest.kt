package com.gooodhandletruck.sidong.ui.format

import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {

    @Test
    fun clockIsHourAndMinute() = assertEquals("14:32", formatClock(OffsetDateTime.parse("2026-11-05T14:32:45+09:00")))

    @Test
    fun morningHasLeadingZero() = assertEquals("09:05", formatClock(OffsetDateTime.parse("2026-11-05T09:05:00+09:00")))
}

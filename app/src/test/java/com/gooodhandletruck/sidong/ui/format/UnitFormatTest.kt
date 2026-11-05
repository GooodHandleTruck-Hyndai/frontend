package com.gooodhandletruck.sidong.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

class UnitFormatTest {

    @Test
    fun metersRoundToTen() {
        assertEquals("340m", formatDistance(344.0))
        assertEquals("350m", formatDistance(345.0))
    }

    @Test
    fun justBelowOneKilometerReadsOneKilometer() = assertEquals("1km", formatDistance(996.0))

    @Test
    fun kilometersHaveOneDecimal() = assertEquals("1.2km", formatDistance(1234.0))

    @Test
    fun wholeKilometersDropTheDecimal() = assertEquals("3km", formatDistance(3000.0))

    @Test
    fun areaDropsDecimals() = assertEquals("84㎡", formatArea(84.97))

    @Test
    fun voiceAreaReadsSquareMeters() = assertEquals("84제곱미터", formatAreaVoice(84.97))
}

package com.gooodhandletruck.sidong.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeDirectionTest {

    @Test
    fun eastWhileFacingNorthIsRight() = assertEquals(RelativeDirection.RIGHT, RelativeDirection.of(90.0, 0.0))

    @Test
    fun southWhileFacingNorthIsBack() = assertEquals(RelativeDirection.BACK, RelativeDirection.of(180.0, 0.0))

    @Test
    fun sectorsAreFortyFiveDegreesWide() {
        assertEquals(RelativeDirection.FRONT, RelativeDirection.of(22.0, 0.0))
        assertEquals(RelativeDirection.FRONT_RIGHT, RelativeDirection.of(23.0, 0.0))
        assertEquals(RelativeDirection.FRONT_LEFT, RelativeDirection.of(315.0, 0.0))
    }

    @Test
    fun wrapsAroundNorth() = assertEquals(RelativeDirection.FRONT, RelativeDirection.of(5.0, 355.0))

    @Test
    fun northWhileFacingEastIsLeft() = assertEquals(RelativeDirection.LEFT, RelativeDirection.of(0.0, 90.0))
}

package com.lovelyy.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DistanceMathTest {
    @Test
    fun ankaraToIzmirIsAbout500Km() {
        // Ankara (39.93, 32.86) to İzmir (38.42, 27.14): ~520 km in a straight line.
        val km = DistanceMath.km(39.93, 32.86, 38.42, 27.14)
        assertEquals(520.0, km, 15.0)
    }

    @Test
    fun samePlaceIsZero() {
        assertEquals(0.0, DistanceMath.km(41.0, 29.0, 41.0, 29.0), 0.0001)
    }

    @Test
    fun travelTimesMatchTheDesignExample() {
        // Design: 342 km → "4 sa 30 dk" by car, "1 sa 10 dk" by plane.
        assertEquals(274L, DistanceMath.driveMinutes(342.0))
        assertEquals(71L, DistanceMath.flightMinutes(342.0))
    }

    @Test
    fun noFlightForShortDistances() {
        assertNull(DistanceMath.flightMinutes(80.0))
    }

    @Test
    fun coordinatesAreRounded() {
        assertEquals(39.9, DistanceMath.coarse(39.9334, cityOnly = true), 1e-9)
        assertEquals(39.93, DistanceMath.coarse(39.9334, cityOnly = false), 1e-9)
    }
}

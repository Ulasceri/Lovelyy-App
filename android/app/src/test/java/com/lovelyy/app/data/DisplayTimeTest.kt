package com.lovelyy.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayTimeTest {
    private val now = 1_000_000_000L
    private val hour = 3_600_000L

    @Test
    fun pastStaysAsIs() = assertEquals(now - hour, DisplayTime.clamp(now - hour, now))

    @Test
    fun futureCountsAsNow() = assertEquals(now, DisplayTime.clamp(now + 54 * 24 * hour, now))

    @Test
    fun futureIsZeroMinutesAgo() = assertEquals(0L, DisplayTime.minutesAgo(now + 3 * hour, now))

    @Test
    fun pastMinutes() = assertEquals(90L, DisplayTime.minutesAgo(now - 90 * 60_000L, now))

    @Test
    fun futureSortsAsNowNotAboveRealNow() {
        val sorted = listOf(now - hour, now + 5 * hour, now).map { DisplayTime.clamp(it, now) }.sortedDescending()
        assertEquals(listOf(now, now, now - hour), sorted)
    }
}

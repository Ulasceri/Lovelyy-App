package com.lovelyy.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerTimeTest {
    private val server = 1_800_000_000_000L

    @Test fun baseAddsHalfTheRoundTrip() {
        // Sent 10 s after boot, answered 400 ms later; the server stamped it somewhere in between.
        val base = ServerTime.base(server, sentAt = 10_000, receivedAt = 10_400)
        assertEquals(server + 200, base + 10_400)
    }

    @Test fun serverTimeKeepsRunningOnTheBootClockWhateverThePhoneClockSays() {
        val base = ServerTime.base(server, sentAt = 5_000, receivedAt = 5_000)
        // An hour later on the boot clock it is an hour later on the server, even if the user moved the clock.
        assertEquals(server + 3_600_000, base + 5_000 + 3_600_000)
    }

    @Test fun skewIsHowFarThePhoneClockIsAhead() {
        val base = ServerTime.base(server, sentAt = 1_000, receivedAt = 1_000)
        val ahead25h = server + 25 * 3_600_000L
        assertEquals(25 * 3_600_000L, ServerTime.skew(base, ahead25h, 1_000))
        assertEquals(-60_000L, ServerTime.skew(base, server - 60_000, 1_000))
    }

    @Test fun aNegativeRoundTripIsTreatedAsZero() {
        assertEquals(server - 7_000, ServerTime.base(server, sentAt = 8_000, receivedAt = 7_000))
    }

    @Test fun beforeTheFirstSyncTheAppUsesThePhoneClock() {
        val before = System.currentTimeMillis()
        val now = TrustedClock.now()
        assertTrue(now >= before && now <= System.currentTimeMillis())
    }
}

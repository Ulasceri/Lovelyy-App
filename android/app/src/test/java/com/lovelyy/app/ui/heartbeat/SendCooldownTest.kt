package com.lovelyy.app.ui.heartbeat

import org.junit.Assert.assertEquals
import org.junit.Test

class SendCooldownTest {
    @Test fun neverSentHasNoCooldown() = assertEquals(0L, cooldownRemainingMs(null, 1_000L, 3_000L))
    @Test fun justSentWaitsFullCooldown() = assertEquals(3_000L, cooldownRemainingMs(1_000L, 1_000L, 3_000L))
    @Test fun partwayReturnsRemainder() = assertEquals(1_000L, cooldownRemainingMs(1_000L, 3_000L, 3_000L))
    @Test fun exactlyElapsedIsFree() = assertEquals(0L, cooldownRemainingMs(1_000L, 4_000L, 3_000L))
    @Test fun longAgoIsFree() = assertEquals(0L, cooldownRemainingMs(1_000L, 500_000L, 30_000L))
    @Test fun clockGoingBackwardsStaysBlocked() = assertEquals(3_000L, cooldownRemainingMs(10_000L, 5_000L, 3_000L))
}

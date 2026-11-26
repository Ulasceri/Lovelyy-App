package com.lovelyy.app.ui.heartbeat

/** Minimum gap between two heartbeats, so accidental repeat holds don't spam the partner. */
const val HEARTBEAT_COOLDOWN_MS = 3_000L

/** How long the nudge button stays in its "sent" state before it can be tapped again. */
const val NUDGE_COOLDOWN_MS = 30_000L

/** Milliseconds left before another send is allowed; 0 when [lastSentAt] is null or the cooldown has passed. */
fun cooldownRemainingMs(lastSentAt: Long?, now: Long, cooldownMs: Long): Long {
    if (lastSentAt == null) return 0L
    val elapsed = now - lastSentAt
    if (elapsed < 0) return cooldownMs // clock went backwards: stay conservative
    return (cooldownMs - elapsed).coerceAtLeast(0L)
}

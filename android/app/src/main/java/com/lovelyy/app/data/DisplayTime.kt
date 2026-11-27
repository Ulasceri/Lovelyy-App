package com.lovelyy.app.data

/**
 * Timestamps are written by each phone's own clock, so one from a partner whose clock runs ahead can lie
 * in the future. For display and ordering such a stamp counts as "now". Stored data is never changed.
 */
object DisplayTime {
    /** [at] limited to [now]. */
    fun clamp(at: Long, now: Long): Long = minOf(at, now)

    /** Whole minutes elapsed since [at]; 0 for a future [at]. */
    fun minutesAgo(at: Long, now: Long): Long = ((now - clamp(at, now)) / 60_000L).coerceAtLeast(0L)
}

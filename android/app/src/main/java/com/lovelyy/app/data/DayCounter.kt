package com.lovelyy.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Time together, split for the live counter. */
data class Together(
    val days: Long,
    val hours: Int,
    val minutes: Int,
    val seconds: Int,
    val millis: Int,
    val totalMillis: Long,
)

/** A day worth celebrating (100. gün, 1. yıl...). */
data class Milestone(val day: Long, val years: Int?) {
    val isYear get() = years != null
}

/** Pure math for the day counter (unit-tested in DayCounterTest). */
object DayCounter {
    private const val DAY = 86_400_000L

    /** Start of the given day in the phone's time zone. The couple picks a date, not a time. */
    fun startMillis(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun dateOf(millis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun together(startMillis: Long, now: Long): Together {
        val total = (now - startMillis).coerceAtLeast(0)
        var rest = total
        val days = rest / DAY; rest %= DAY
        val hours = (rest / 3_600_000L).toInt(); rest %= 3_600_000L
        val minutes = (rest / 60_000L).toInt(); rest %= 60_000L
        val seconds = (rest / 1000L).toInt()
        val millis = (rest % 1000L).toInt()
        return Together(days, hours, minutes, seconds, millis, total)
    }

    /** "Birlikte 412. gününüz": the first day together is day 1. */
    fun dayNumber(startMillis: Long, now: Long): Long = together(startMillis, now).days + 1

    /** Celebrated days: round hundreds, every 500/1000 later, and every anniversary. */
    fun milestones(upTo: Long): List<Milestone> {
        val hundreds = buildList {
            listOf(100L, 200L, 300L, 500L).forEach { add(Milestone(it, null)) }
            var d = 1000L
            while (d <= upTo) { add(Milestone(d, null)); d += if (d < 3000) 500 else 1000 }
        }
        val years = (1..((upTo / 365) + 1).toInt()).map { Milestone(it * 365L, it) }
        return (hundreds + years).distinctBy { it.day }.sortedBy { it.day }
    }

    /** The next milestone strictly after [days], and the one before it (for the progress bar). */
    fun next(days: Long): Pair<Milestone?, Milestone> {
        val all = milestones(days + 2000)
        val next = all.first { it.day > days }
        val prev = all.lastOrNull { it.day <= days }
        return prev to next
    }
}

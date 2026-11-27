package com.lovelyy.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

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
    /** Start of the given day in the couple's shared time zone. The couple picks a date, not a time. */
    fun startMillis(date: LocalDate, zone: ZoneId = CoupleClock.zone()): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    /** The day of [millis] in the couple's shared zone, so both phones show the same start date. */
    fun dateOf(millis: Long, zone: ZoneId = CoupleClock.zone()): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    /**
     * Calendar days from the start date to today in [zone] (DST-safe: the day rolls over at local midnight),
     * and hours/minutes/seconds as the local wall clock time of day. [totalMillis] is the raw elapsed time.
     */
    fun together(startMillis: Long, now: Long, zone: ZoneId = CoupleClock.zone()): Together {
        if (now <= startMillis) return Together(0, 0, 0, 0, 0, 0)
        val today = dateOf(now, zone)
        val days = ChronoUnit.DAYS.between(dateOf(startMillis, zone), today).coerceAtLeast(0)
        val time = Instant.ofEpochMilli(now).atZone(zone).toLocalTime()
        val hours = time.hour
        val minutes = time.minute
        val seconds = time.second
        val millis = time.nano / 1_000_000
        return Together(days, hours, minutes, seconds, millis, now - startMillis)
    }

    /** "Birlikte 412. gününüz": the first day together is day 1. */
    fun dayNumber(startMillis: Long, now: Long, zone: ZoneId = CoupleClock.zone()): Long =
        together(startMillis, now, zone).days + 1

    /** Celebrated days: round hundreds, every 500/1000 later, and every anniversary. */
    fun milestones(upTo: Long, start: LocalDate? = null): List<Milestone> {
        val hundreds = buildList {
            listOf(100L, 200L, 300L, 500L).forEach { add(Milestone(it, null)) }
            var d = 1000L
            while (d <= upTo) { add(Milestone(d, null)); d += if (d < 3000) 500 else 1000 }
        }
        val years = (1..((upTo / 365) + 1).toInt()).map { Milestone(yearDay(it, start), it) }
        // A year milestone that lands on a round day (e.g. day 1000) must stay a year milestone.
        return (years + hundreds).distinctBy { it.day }.sortedBy { it.day }
    }

    /** Elapsed days at the [n]th anniversary: calendar based when [start] is known (leap years), else n * 365. */
    private fun yearDay(n: Int, start: LocalDate?): Long =
        if (start == null) n * 365L else ChronoUnit.DAYS.between(start, start.plusYears(n.toLong()))

    /** The next milestone strictly after [days], and the one before it (for the progress bar). */
    fun next(days: Long, start: LocalDate? = null): Pair<Milestone?, Milestone> {
        val all = milestones(days + 2000, start)
        val next = all.first { it.day > days }
        val prev = all.lastOrNull { it.day <= days }
        return prev to next
    }
}

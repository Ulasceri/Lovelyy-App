package com.lovelyy.app.data

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Pure rules of the care streak's Lovelyy Plus protection (approved boards P5a, P5b, P5c; unit-tested in
 * StreakProtectionTest). Everything is computed from the stored [CareStreak] and a day, so both phones agree
 * without a server:
 *
 * - "Seri tehlikede" (P5a): after [DANGER_HOUR] in the couple's time zone, the streak is alive, today is not
 *   complete and not taken off.
 * - "İzin günü" (P5b, Plus): today is taken off. The streak keeps its count and continues tomorrow.
 *   [FREEZES_PER_MONTH] per calendar month, shared by the couple, renewed on the 1st.
 * - "Telafi" (P5c, Plus): the day after a streak broke (exactly one missed day), the missed day is made up and
 *   the old count comes back. The window is the rest of that day, i.e. 24 hours after the break at midnight.
 *
 * A protected day (off or made up) bridges the gap between two complete days; it never adds to the count.
 */
object StreakProtection {
    const val FREEZES_PER_MONTH = 2
    /** How many off / made-up days are stored (older ones can no longer matter for an alive streak). */
    const val KEPT_DAYS = 6
    /** P5a shows from this hour (couple time zone) until midnight. */
    const val DANGER_HOUR = 20
    /** Longest gap [alive] walks through; more protected days than this can't exist ([KEPT_DAYS] × 2). */
    private const val MAX_GAP = 2 * KEPT_DAYS

    /** "2026-11" for a day: the month days off count in. */
    fun month(day: LocalDate): String = YearMonth.from(day).toString()

    /** Days strictly between [last] and [day]. */
    fun gap(last: LocalDate, day: LocalDate): List<LocalDate> {
        val n = ChronoUnit.DAYS.between(last, day)
        if (n <= 1) return emptyList()
        return (1 until n).map { last.plusDays(it) }
    }

    /**
     * The streak whose last complete day is [last] is still alive on [day]: [day] is that day, or every day in
     * between is protected (none in between = yesterday was complete).
     */
    fun alive(last: LocalDate, day: LocalDate, protected: Set<String>): Boolean {
        if (last.isAfter(day)) return false
        val between = ChronoUnit.DAYS.between(last, day)
        if (between <= 1) return true
        if (between - 1 > MAX_GAP) return false
        return gap(last, day).all { it.toString() in protected }
    }

    fun completeToday(s: CareStreak, today: LocalDate): Boolean = s.lastCompleteDay == today.toString()

    fun offToday(s: CareStreak, today: LocalDate): Boolean = today.toString() in s.freezeDays

    // ---------- P5a: streak in danger ----------

    /**
     * The "Seri tehlikede" card: from [DANGER_HOUR] ([hour] is the hour in the couple's zone), while the streak is
     * alive and today is neither complete nor taken off. [trialCardShown]: the day-12 Plus card is on Home today;
     * it is the day's one Plus surface that appears by itself, so this card waits (board P2b note).
     */
    fun showDangerCard(s: CareStreak, today: LocalDate, hour: Int, trialCardShown: Boolean = false): Boolean =
        !trialCardShown && hour >= DANGER_HOUR && s.current(today) > 0 && !completeToday(s, today) && !offToday(s, today)

    /** Milliseconds until the couple's midnight (when today's streak day ends). */
    fun millisToMidnight(now: Long, zone: ZoneId): Long {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val midnight = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return (midnight - now).coerceAtLeast(0)
    }

    // ---------- P5b: days off ----------

    fun freezesUsed(s: CareStreak, today: LocalDate): Int = if (s.freezeMonth == month(today)) s.freezeUsed else 0

    fun freezesLeft(s: CareStreak, today: LocalDate): Int = (FREEZES_PER_MONTH - freezesUsed(s, today)).coerceAtLeast(0)

    /** Days taken off in today's month, oldest first ("Biri 8 Kasım'da kullanıldı"). */
    fun freezeDaysThisMonth(s: CareStreak, today: LocalDate): List<LocalDate> {
        val m = month(today)
        return s.freezeDays.filter { it.startsWith(m) }.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.sorted()
    }

    /** When the days off renew: the 1st of next month. */
    fun freezesRenewOn(today: LocalDate): LocalDate = today.withDayOfMonth(1).plusMonths(1)

    /** Why today can't be taken off, or null when it can. */
    enum class FreezeRefusal { NotPlus, NoStreak, AlreadyComplete, AlreadyOff, NoneLeft }

    fun freezeRefusal(s: CareStreak, today: LocalDate, isPlus: Boolean): FreezeRefusal? = when {
        !isPlus -> FreezeRefusal.NotPlus
        s.current(today) <= 0 -> FreezeRefusal.NoStreak
        completeToday(s, today) -> FreezeRefusal.AlreadyComplete
        offToday(s, today) -> FreezeRefusal.AlreadyOff
        freezesLeft(s, today) <= 0 -> FreezeRefusal.NoneLeft
        else -> null
    }

    /** The streak with today taken off, or null when that is not allowed ([freezeRefusal]). */
    fun freeze(s: CareStreak, today: LocalDate, isPlus: Boolean): CareStreak? {
        if (freezeRefusal(s, today, isPlus) != null) return null
        return s.copy(
            freezeMonth = month(today),
            freezeUsed = freezesUsed(s, today) + 1,
            freezeDays = keep(s.freezeDays + today.toString()),
        )
    }

    // ---------- P5c: make up a missed day ----------

    /** A streak that broke yesterday and can still be made up today. [count] is what comes back. */
    data class Broken(val count: Int, val missedDay: LocalDate, val restartedToday: Boolean)

    /**
     * The streak that can be made up today, or null. Exactly one unprotected missed day (yesterday) is allowed.
     * Two cases: nobody restarted the streak yet (the stored count is still the old one), or both partners already
     * cared today and a new streak of 1 started (the old one is in [CareStreak.brokenCount]).
     */
    fun repairable(s: CareStreak, today: LocalDate): Broken? {
        val yesterday = today.minusDays(1)
        val protected = s.protectedDays
        fun onlyYesterdayMissed(lastDay: String?): Boolean {
            val last = lastDay?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return false
            if (!last.isBefore(yesterday)) return false
            if (ChronoUnit.DAYS.between(last, today) - 1 > MAX_GAP) return false
            return gap(last, today).filter { it.toString() !in protected } == listOf(yesterday)
        }
        if (!completeToday(s, today)) {
            return if (s.count > 0 && onlyYesterdayMissed(s.lastCompleteDay)) Broken(s.count, yesterday, restartedToday = false) else null
        }
        return if (s.count == 1 && s.brokenCount > 0 && onlyYesterdayMissed(s.brokenLast)) {
            Broken(s.brokenCount + s.count, yesterday, restartedToday = true)
        } else {
            null
        }
    }

    /** The streak with yesterday made up, or null when nothing can be made up (or not Plus). */
    fun repair(s: CareStreak, today: LocalDate, isPlus: Boolean): CareStreak? {
        if (!isPlus) return null
        val b = repairable(s, today) ?: return null
        val days = keep(s.repairDays + b.missedDay.toString())
        if (!b.restartedToday) return s.copy(repairDays = days)
        return s.copy(count = b.count, best = maxOf(s.best, b.count), repairDays = days, brokenCount = 0, brokenLast = null)
    }

    /**
     * One key per break, so the P5c sheet opens by itself once per broken streak on this phone (and "Seriye 1.
     * günden başla" is remembered).
     */
    fun breakKey(b: Broken): String = b.missedDay.toString()

    private fun keep(days: List<String>): List<String> = days.distinct().sorted().takeLast(KEPT_DAYS)
}

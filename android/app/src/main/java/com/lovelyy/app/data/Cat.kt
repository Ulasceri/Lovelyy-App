package com.lovelyy.app.data

import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min

/**
 * The couple's shared cat. Firestore: couples/{coupleId}/cat/main
 *
 * Needs decay with time, so nothing has to run on a server (works on the free Spark plan):
 * each need stores the level it had at a moment ([Need.at]) and every phone computes the
 * current level from the clock. An action reads the current level, changes it and stores
 * the new snapshot. Both partners always see the same numbers.
 */
data class CatDoc(
    /** Never blank in practice: new cats and blank stored names fall back to [DEFAULT_CAT_NAME]. */
    val name: String? = DEFAULT_CAT_NAME,
    val hunger: Need = Need.fresh(),
    val thirst: Need = Need.fresh(),
    val energy: Need = Need.fresh(),
    val love: Need = Need.fresh(),
    /** Set while the cat sleeps. Energy refills while asleep; the cat wakes up by itself when full. */
    val sleepingSince: Long? = null,
    val lastAction: CatAction? = null,
    val recent: List<CatAction> = emptyList(),
    val streak: CareStreak = CareStreak(),
)

/** The name every cat starts with; the couple can rename it later. */
const val DEFAULT_CAT_NAME = "Mırmır"

/** The name to show for a stored cat name (null or blank shows the default). */
fun displayCatName(stored: String?): String = stored?.trim()?.takeIf { it.isNotEmpty() } ?: DEFAULT_CAT_NAME

/** A need's level (0..100) at the time [at] (epoch millis). */
data class Need(val level: Double, val at: Long) {
    companion object {
        fun fresh(now: Long = TrustedClock.now()) = Need(START_LEVEL, now)
        const val START_LEVEL = 80.0
    }
}

data class CatAction(val type: CatActionType, val uid: String, val name: String, val at: Long)

enum class CatActionType { Feed, Water, Play, Pet, Sleep, Wake, Name }

/**
 * Shared daily care streak: the streak grows by one for every day on which BOTH partners
 * took care of the cat. Lovelyy Plus can protect it (see [StreakProtection]): an "izin günü" (day off) or a
 * "telafi" (make-up) day bridges a missed day, so the streak neither grows nor breaks on it.
 *
 * Days come from [CoupleClock.today]: server time ([TrustedClock]) in the couple's shared zone, so a phone's own
 * clock (moved forward to cheat, back by mistake, or in another zone while travelling) does not change them, and
 * firestore.rules reject a [careDay]/[lastCompleteDay] that is not "today" at the server's time. Before server
 * time, phones counted with their own clocks; the rules below stay as a safety net for a phone that could not
 * sync yet and for days a wrong clock already stored:
 * - The stored days only move forward: a care stamped with a day EARLIER than [careDay] is merged into [careDay]
 *   (its uid is added to [careBy], nothing is replaced), and [lastCompleteDay] never moves backwards, so [count]
 *   is never reset by a late phone. When such a merge completes the day, the streak continues if the last
 *   complete day was the day before [careDay] OR the day before this phone's own day.
 * - A stored day more than one day ahead of today came from a wrong clock: [careDay] is then replaced by today,
 *   and such a [lastCompleteDay] counts as yesterday (the streak goes on). Otherwise the couple could not finish a
 *   day until real time caught up, because the rules only accept today.
 * - [current] treats a [lastCompleteDay] later than the local day as alive, and [caredToday] treats a [careDay]
 *   later than the local day as today, matching the merge.
 */
data class CareStreak(
    val count: Int = 0,
    val best: Int = 0,
    /** Last day (ISO yyyy-MM-dd) on which both partners cared. */
    val lastCompleteDay: String? = null,
    /** The day [careBy] refers to. */
    val careDay: String? = null,
    /** Who has cared on [careDay]. */
    val careBy: List<String> = emptyList(),
    /** Month (yyyy-MM) that [freezeUsed] counts in. Days off are shared by the couple and renew on the 1st. */
    val freezeMonth: String? = null,
    /** Days off used in [freezeMonth] (0..[StreakProtection.FREEZES_PER_MONTH]). */
    val freezeUsed: Int = 0,
    /** Days (yyyy-MM-dd) taken off. The latest few are kept. */
    val freezeDays: List<String> = emptyList(),
    /** Missed days that were made up within 24 hours (telafi). The latest few are kept. */
    val repairDays: List<String> = emptyList(),
    /** The streak that ended when a new one started (kept so it can still be made up after both cared again). */
    val brokenCount: Int = 0,
    /** [lastCompleteDay] of that ended streak. */
    val brokenLast: String? = null,
) {
    /** Days that keep the streak alive without counting: days off and made-up days. */
    val protectedDays: Set<String> get() = (freezeDays + repairDays).toSet()

    /** The streak as it should be shown today (it breaks if a whole unprotected day was missed). */
    fun current(today: LocalDate): Int {
        val last = lastCompleteDay.day() ?: return 0
        // Alive if done yesterday or later (a later day is clock skew), or if the gap is only protected days.
        return if (!last.isBefore(today.minusDays(1)) || StreakProtection.alive(last, today, protectedDays)) count else 0
    }

    fun caredToday(uid: String, today: LocalDate): Boolean {
        val day = careDay.day() ?: return false
        return !day.isBefore(today) && uid in careBy
    }

    /** The streak after [uid] cared on [today] (this phone's couple day). [members] are both partners. */
    fun afterCare(uid: String, members: List<String>, today: LocalDate): CareStreak {
        // Never move careDay backwards: a care from an earlier day joins the stored day. A day further ahead than
        // tomorrow was stored by a wrong clock and is dropped.
        val stored = careDay.day()?.takeIf { !it.isAfter(today.plusDays(1)) }
        val day = if (stored != null && stored.isAfter(today)) stored else today
        val dayStr = day.toString()
        val by = if (careDay == dayStr) (careBy + uid).distinct() else listOf(uid)
        val bothCared = members.size == 2 && by.containsAll(members)
        val last = lastCompleteDay.day()?.let { if (it.isAfter(today.plusDays(1))) today.minusDays(1) else it }
        if (!bothCared || (last != null && !last.isBefore(day))) return copy(careDay = dayStr, careBy = by)
        val continues = last != null &&
            (last == day.minusDays(1) || last == today.minusDays(1) || StreakProtection.alive(last, day, protectedDays))
        val newCount = if (continues) count + 1 else 1
        // copy, not a new CareStreak: the Plus protection fields (days off, made-up days) must survive a care.
        return copy(
            count = newCount,
            best = max(best, newCount),
            lastCompleteDay = dayStr,
            careDay = dayStr,
            careBy = by,
            // A new streak starts: remember the old one, it may still be made up today (StreakProtection.repair).
            brokenCount = if (!continues && last != null && count > 0) count else brokenCount,
            brokenLast = if (!continues && last != null && count > 0) lastCompleteDay else brokenLast,
        )
    }

    private fun String?.day(): LocalDate? = this?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
}

/** What the cat feels right now. Decides the looping animation, the widget picture and texts. */
enum class CatMood { Sleeping, Hungry, Thirsty, Sleepy, WantsLove, Happy, Idle }

/** Current, time-adjusted levels of every need. */
data class CatLevels(
    val hunger: Double,
    val thirst: Double,
    val energy: Double,
    val love: Double,
    val sleeping: Boolean,
) {
    val mood: CatMood
        get() = when {
            sleeping -> CatMood.Sleeping
            hunger < CatRules.LOW -> CatMood.Hungry
            thirst < CatRules.LOW -> CatMood.Thirsty
            energy < CatRules.LOW -> CatMood.Sleepy
            love < CatRules.LOW -> CatMood.WantsLove
            min(min(hunger, thirst), min(energy, love)) >= CatRules.HAPPY -> CatMood.Happy
            else -> CatMood.Idle
        }
}

/** All the numbers of the cat in one place. Both phones must use the same rules. */
object CatRules {
    private const val HOUR = 3_600_000.0

    // Hours from full to empty.
    const val HUNGER_HOURS = 12.0
    const val THIRST_HOURS = 10.0
    const val ENERGY_HOURS = 16.0
    const val LOVE_HOURS = 8.0
    /** Hours of sleep from empty to full energy. */
    const val SLEEP_REFILL_HOURS = 1.0

    const val LOW = 30.0
    const val HAPPY = 70.0

    const val FEED = 40.0
    const val WATER = 45.0
    const val PLAY_LOVE = 25.0
    const val PLAY_ENERGY = 10.0
    const val PLAY_HUNGER = 5.0
    const val PET_PER_STROKE = 4.0
    const val PET_MAX = 30.0
    /** Below this energy the cat is too tired to play. */
    const val PLAY_MIN_ENERGY = 15.0
    /** Above this energy the cat doesn't want to sleep. */
    const val SLEEP_MAX_ENERGY = 90.0

    private fun decayed(n: Need, hoursToEmpty: Double, now: Long): Double {
        val hours = max(0L, now - n.at) / HOUR
        return (n.level - hours * 100.0 / hoursToEmpty).coerceIn(0.0, 100.0)
    }

    /** Energy, taking an ongoing sleep into account. Returns (energy, stillSleeping). */
    private fun energyNow(doc: CatDoc, now: Long): Pair<Double, Boolean> {
        val since = doc.sleepingSince ?: return decayed(doc.energy, ENERGY_HOURS, now) to false
        val atSleep = decayed(doc.energy, ENERGY_HOURS, since)
        val refillPerMs = 100.0 / (SLEEP_REFILL_HOURS * HOUR)
        val wakeAt = since + ((100.0 - atSleep) / refillPerMs).toLong()
        return if (now < wakeAt) {
            (atSleep + (now - since) * refillPerMs).coerceIn(0.0, 100.0) to true
        } else {
            // Woke up by itself at wakeAt with full energy, decaying since.
            decayed(Need(100.0, wakeAt), ENERGY_HOURS, now) to false
        }
    }

    fun levels(doc: CatDoc, now: Long = TrustedClock.now()): CatLevels {
        val (energy, sleeping) = energyNow(doc, now)
        return CatLevels(
            hunger = decayed(doc.hunger, HUNGER_HOURS, now),
            thirst = decayed(doc.thirst, THIRST_HOURS, now),
            energy = energy,
            love = decayed(doc.love, LOVE_HOURS, now),
            sleeping = sleeping,
        )
    }

    /**
     * The cat after an action. Returns null when the action makes no sense right now
     * (e.g. playing while asleep); the UI explains why with [refusal].
     */
    fun apply(doc: CatDoc, type: CatActionType, now: Long, strokes: Int = 0): CatDoc? {
        if (refusal(doc, type, now) != null) return null
        val l = levels(doc, now)
        fun need(v: Double) = Need(v.coerceIn(0.0, 100.0), now)
        // Snapshot every need at "now" so the stored levels stay exact.
        val base = doc.copy(
            hunger = need(l.hunger),
            thirst = need(l.thirst),
            energy = need(l.energy),
            love = need(l.love),
            sleepingSince = if (l.sleeping) now else null,
        )
        return when (type) {
            CatActionType.Feed -> base.copy(hunger = need(l.hunger + FEED), love = need(l.love + 5))
            CatActionType.Water -> base.copy(thirst = need(l.thirst + WATER))
            CatActionType.Play -> base.copy(
                love = need(l.love + PLAY_LOVE),
                energy = need(l.energy - PLAY_ENERGY),
                hunger = need(l.hunger - PLAY_HUNGER),
            )
            CatActionType.Pet -> base.copy(love = need(l.love + min(PET_MAX, strokes * PET_PER_STROKE)))
            CatActionType.Sleep -> base.copy(sleepingSince = now)
            CatActionType.Wake -> base.copy(sleepingSince = null)
            CatActionType.Name -> base
        }
    }

    enum class Refusal { Asleep, TooTired, NotSleepy, Awake }

    fun refusal(doc: CatDoc, type: CatActionType, now: Long): Refusal? {
        val l = levels(doc, now)
        return when (type) {
            CatActionType.Feed, CatActionType.Water, CatActionType.Play ->
                when {
                    l.sleeping -> Refusal.Asleep
                    type == CatActionType.Play && l.energy < PLAY_MIN_ENERGY -> Refusal.TooTired
                    else -> null
                }
            CatActionType.Sleep -> when {
                l.sleeping -> Refusal.Asleep
                l.energy > SLEEP_MAX_ENERGY -> Refusal.NotSleepy
                else -> null
            }
            CatActionType.Wake -> if (!l.sleeping) Refusal.Awake else null
            CatActionType.Pet, CatActionType.Name -> null
        }
    }
}

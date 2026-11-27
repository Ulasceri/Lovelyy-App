package com.lovelyy.app.data

/**
 * Free-tier limits (approved plan v2, board P9). The one place that knows the numbers and the rule, so screens
 * and tests agree (unit-tested in PlusLimitsTest).
 *
 * The rule: nothing is ever hidden or deleted. Over a limit (or after Plus ended) a list stays readable, editable
 * and deletable; only ADDING a new item is blocked, with the P9 note and a way to Plus. Basic cat care is never
 * limited. Limits count what the couple has together, not per partner.
 */
object PlusLimits {

    /** A limited list. [key] goes into analytics ("lock_<key>" paywall source), never anything personal. */
    enum class Limit(val key: String, val free: Int) {
        Notes("notes", 50),
        /** Plans and special days on the calendar (the built-in anniversary does not count). */
        Events("events", 10),
        Dreams("dreams", 20),
        /** Sealed time capsule letters that have not opened yet. */
        Capsules("capsules", 1),
        /** Custom decision wheels (the built-in wheels are always free). */
        Wheels("wheels", 2),
        /** Drawings saved to Anılar. */
        Drawings("drawings", 10),
    }

    /** Pen colours a free couple can draw with (the palette has fewer today, so none is locked yet). */
    const val FREE_DRAWING_COLORS = 6

    /** Days of history the plan gives a free couple (see docs/architecture.md: history is never hidden). */
    const val FREE_HISTORY_DAYS = 30

    /** Whether one more item may be added. Plus (trial or paid) has no limit. */
    fun canAdd(limit: Limit, count: Int, isPlus: Boolean): Boolean = isPlus || count < limit.free

    /** Free and at (or over) the limit: the next add is blocked (the add button shows the P9 lock). */
    fun atLimit(limit: Limit, count: Int, isPlus: Boolean): Boolean = !canAdd(limit, count, isPlus)

    /**
     * Free and over the limit (typically after Plus ended): the list shows the P9 note on top ("sınırı aşan
     * listeler"). Exactly at the limit only the add button changes, so small limits don't nag.
     */
    fun overLimit(limit: Limit, count: Int, isPlus: Boolean): Boolean = !isPlus && count > limit.free

    /** A pen colour at [index] can be used. */
    fun colorAllowed(index: Int, isPlus: Boolean): Boolean = isPlus || index < FREE_DRAWING_COLORS
}

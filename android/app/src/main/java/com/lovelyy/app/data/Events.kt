package com.lovelyy.app.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** couples/{id}/events/{auto}: a plan on a date, or a special day that repeats every year. */
data class CalEvent(
    val id: String,
    val title: String,
    val date: LocalDate,
    val emoji: String,
    /** Special day (birthday, anniversary...): repeats every year and shows a countdown. */
    val yearly: Boolean,
    val by: String,
    val byName: String,
    /** Built in (e.g. the anniversary from the day counter): can't be edited here. */
    val builtIn: Boolean = false,
    /** Last edit (millis) and who made it; null on plans written before these fields existed. */
    val updatedAt: Long? = null,
    val updatedBy: String? = null,
) {
    /** The part an editor changes; compared to detect a concurrent edit. */
    val content: EventContent get() = EventContent(title, date, yearly)
}

data class EventContent(val title: String, val date: LocalDate, val yearly: Boolean)

/** The partner changed the plan after this edit was opened. [current] is the version on the server now. */
class EventConflictException(val current: CalEvent) : EditConflictException("Event changed meanwhile")

/** Pure date math for the calendar (unit-tested in CalendarMathTest). */
object CalendarMath {
    /** The next day (today or later) this event happens, or null if a one-time plan is past. */
    fun nextOccurrence(e: CalEvent, today: LocalDate): LocalDate? {
        if (!e.yearly) return e.date.takeIf { !it.isBefore(today) }
        // Never before the event's own first date (a yearly event created in 2028 has no 2027 occurrence).
        var year = maxOf(today.year, e.date.year)
        while (true) {
            val d = safeDate(year, e.date.monthValue, e.date.dayOfMonth)
            if (!d.isBefore(today)) return d
            year++
        }
    }

    fun daysUntil(e: CalEvent, today: LocalDate): Long? = nextOccurrence(e, today)?.let { ChronoUnit.DAYS.between(today, it) }

    /** Does the event fall on [day]? (Yearly ones on the same month/day, from their first year on.) */
    fun occursOn(e: CalEvent, day: LocalDate): Boolean =
        if (!e.yearly) e.date == day
        else day.year >= e.date.year && safeDate(day.year, e.date.monthValue, e.date.dayOfMonth) == day

    /** 29 Feb on a non-leap year becomes 28 Feb. */
    private fun safeDate(year: Int, month: Int, day: Int): LocalDate {
        val ym = java.time.YearMonth.of(year, month)
        return ym.atDay(minOf(day, ym.lengthOfMonth()))
    }

    /** How many times a yearly event has happened by [on] (e.g. 2 → "2. yıl"). */
    fun yearsAt(e: CalEvent, on: LocalDate): Int = maxOf(0, on.year - e.date.year)
}

class EventRepository(private val db: FirebaseFirestore) {

    private fun events(coupleId: String) =
        db.collection(Paths.COUPLES).document(coupleId).collection(Paths.EVENTS)

    /**
     * Adds a plan ([opened] null) or edits one ([opened] = the plan as it was when the editor opened).
     * Editing never re-creates a plan the partner deleted meanwhile (throws [ItemGoneException]), keeps the
     * original creator (by/byName/at), and throws [EventConflictException] if the partner changed it
     * meanwhile unless [force]. Offline, the edit is written without the conflict check.
     */
    suspend fun save(coupleId: String, me: UserProfile, opened: CalEvent?, title: String, date: LocalDate, emoji: String, yearly: Boolean, force: Boolean = false) {
        val data = mapOf(
            "title" to title.trim().take(MAX_TITLE),
            "date" to date.toString(),
            "emoji" to emoji,
            "yearly" to yearly,
        )
        if (opened == null) {
            events(coupleId).add(data + mapOf("by" to me.uid, "byName" to me.displayName, "at" to System.currentTimeMillis())).await()
        } else {
            events(coupleId).document(opened.id).updateUnlessChanged(
                db,
                data + mapOf("updatedAt" to System.currentTimeMillis(), "updatedBy" to me.uid),
                force,
                parse = { it.toEvent() },
                isConflict = { EditRules.isConflict(opened.content, opened.updatedAt, it.content, it.updatedAt, it.updatedBy, me.uid) },
                conflict = { EventConflictException(it) },
            )
        }
    }

    suspend fun delete(coupleId: String, id: String) {
        events(coupleId).document(id).delete().await()
    }

    /**
     * Up to 500 plans, ordered by [date] (ISO string, so single-field index, no composite index) newest
     * first: if a couple ever passes the cap, the oldest past plans are the ones that drop off.
     */
    fun observe(coupleId: String): Flow<List<CalEvent>> = callbackFlow {
        val reg = events(coupleId).orderBy("date", Query.Direction.DESCENDING).limit(500).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
            } else if (snap != null) {
                trySend(snap.documents.mapNotNull { it.toEvent() })
            }
        }
        awaitClose { reg.remove() }
    }

    /**
     * Plans dated [from] or later, soonest first (for "next meeting"): a few documents instead of all 500.
     * ISO date strings sort like dates, so this is a single-field range.
     */
    fun observeUpcoming(coupleId: String, from: LocalDate, limit: Long = 20): Flow<List<CalEvent>> = callbackFlow {
        val reg = events(coupleId).whereGreaterThanOrEqualTo("date", from.toString())
            .orderBy("date", Query.Direction.ASCENDING).limit(limit)
            .addSnapshotListener { snap, err ->
                if (err != null) close(err) else if (snap != null) trySend(snap.documents.mapNotNull { it.toEvent() })
            }
        awaitClose { reg.remove() }
    }

    private fun DocumentSnapshot.toEvent(): CalEvent? {
        val date = getString("date")?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        return CalEvent(
            id = id,
            title = getString("title").orEmpty(),
            date = date,
            emoji = getString("emoji") ?: "💛",
            yearly = getBoolean("yearly") == true,
            by = getString("by").orEmpty(),
            byName = getString("byName").orEmpty(),
            updatedAt = getLong("updatedAt"),
            updatedBy = getString("updatedBy"),
        )
    }

    companion object {
        const val MAX_TITLE = 60
    }
}

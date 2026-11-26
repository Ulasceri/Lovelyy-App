package com.lovelyy.app.data

import com.google.firebase.firestore.FirebaseFirestore
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
)

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
     * Adds a plan ([id] null) or edits one. Editing never re-creates a plan the partner deleted meanwhile
     * (throws [ItemGoneException]) and keeps the original creator (by/byName/at).
     */
    suspend fun save(coupleId: String, me: UserProfile, id: String?, title: String, date: LocalDate, emoji: String, yearly: Boolean) {
        val data = mapOf(
            "title" to title.trim().take(MAX_TITLE),
            "date" to date.toString(),
            "emoji" to emoji,
            "yearly" to yearly,
        )
        if (id == null) {
            events(coupleId).add(data + mapOf("by" to me.uid, "byName" to me.displayName, "at" to System.currentTimeMillis())).await()
        } else {
            events(coupleId).document(id).updateExisting(data)
        }
    }

    suspend fun delete(coupleId: String, id: String) {
        events(coupleId).document(id).delete().await()
    }

    fun observe(coupleId: String): Flow<List<CalEvent>> = callbackFlow {
        val reg = events(coupleId).limit(500).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
            } else if (snap != null) {
                trySend(
                    snap.documents.mapNotNull { d ->
                        val date = d.getString("date")?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return@mapNotNull null
                        CalEvent(
                            id = d.id,
                            title = d.getString("title").orEmpty(),
                            date = date,
                            emoji = d.getString("emoji") ?: "💛",
                            yearly = d.getBoolean("yearly") == true,
                            by = d.getString("by").orEmpty(),
                            byName = d.getString("byName").orEmpty(),
                        )
                    },
                )
            }
        }
        awaitClose { reg.remove() }
    }

    companion object {
        const val MAX_TITLE = 60
    }
}

package com.lovelyy.app.data

import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/** Collection names. Keep in sync with firebase/firestore.rules. */
object Paths {
    const val USERS = "users"
    const val COUPLES = "couples"
    const val PAIR_CODES = "pairCodes"
    const val CAT = "cat"
    const val CAT_DOC = "main"
    const val SIGNALS = "signals"
    const val MOODS = "moods"
    const val QUESTIONS = "questions"
    const val NOTES = "notes"
    const val EVENTS = "events"
    const val LOCATIONS = "locations"
    const val DREAMS = "dreams"
    const val WHEELS = "wheels"
    const val WHEEL_SPINS = "wheelSpins"
    const val WHEEL_LOG = "wheelLog"
    const val MOOD_LOG = "moodLog"
    const val CAPSULES = "capsules"
    const val STROKES = "strokes"
    const val DRAWING = "drawing"
    const val DRAWINGS = "drawings"
    const val SETTINGS = "settings"
    const val SETTINGS_DOC = "main"
    /** couples/{id}/limits/{uid}: my signal rate limit windows (see SignalLimits and firestore.rules). */
    const val LIMITS = "limits"
    /** couples/{id}/clock/{uid}: the server time, written and read back by ClockSync (see TrustedClock). */
    const val CLOCK = "clock"
    /** config/{doc}: app-wide settings edited in the Firebase console (read-only for the app). */
    const val CONFIG = "config"
    /** config/limits: signalsPerHour, signalsPerDay, minGapSeconds (see SignalLimits). */
    const val LIMITS_CONFIG_DOC = "limits"
}

internal fun DocumentSnapshot.toUserProfile(): UserProfile? =
    if (!exists()) null else UserProfile(
        uid = id,
        displayName = getString("displayName").orEmpty(),
        photoUrl = getString("photoUrl"),
        coupleId = getString("coupleId"),
        pairCode = getString("pairCode"),
    )

@Suppress("UNCHECKED_CAST")
internal fun DocumentSnapshot.toCouple(): Couple? =
    if (!exists()) null else Couple(
        id = id,
        members = (get("members") as? List<String>).orEmpty(),
        createdAt = getTimestamp("createdAt"),
        startDate = getTimestamp("startDate"),
        endedAt = getTimestamp("endedAt"),
        endedBy = getString("endedBy"),
        endedByName = getString("endedByName"),
        plus = PlusInfo.fromMap(get("plus") as? Map<*, *>),
    )

/** The shared item being edited was deleted (usually by the partner) before the edit reached the server. */
class ItemGoneException(cause: Throwable? = null) : Exception("Item was deleted", cause)

/**
 * Edits an existing document without ever re-creating it: update() fails with NOT_FOUND when the document
 * is gone, which is turned into [ItemGoneException] so screens can tell the user instead of failing silently.
 */
internal suspend fun DocumentReference.updateExisting(data: Map<String, Any?>) {
    try {
        update(data).await()
    } catch (e: FirebaseFirestoreException) {
        if (e.code == FirebaseFirestoreException.Code.NOT_FOUND) throw ItemGoneException(e)
        throw e
    }
}

/** Our own exceptions thrown inside a transaction can come back wrapped; this finds one in the cause chain. */
internal inline fun <reified T : Throwable> Throwable.causeOf(): T? =
    generateSequence(this) { it.cause }.filterIsInstance<T>().firstOrNull()

/** True if a Firestore call failed because the device is offline (transactions need the server). */
internal fun Throwable.isOffline(): Boolean =
    causeOf<FirebaseFirestoreException>()?.code == FirebaseFirestoreException.Code.UNAVAILABLE

/** Pure rule shared by notes, plans and dreams (unit-tested in NoteRulesTest / EditRulesTest). */
object EditRules {
    /**
     * True if saving an edit would silently overwrite someone else's change: the server version differs
     * from the one the editor was opened with ([opened] content / [openedUpdatedAt]) and the last edit was
     * not mine. Old documents have no updatedAt on either side, so then only the content is compared.
     * [T] is whatever the editable content is (text, a data class...), compared with equals.
     */
    fun <T> isConflict(opened: T, openedUpdatedAt: Long?, server: T, serverUpdatedAt: Long?, serverUpdatedBy: String?, me: String): Boolean {
        val changed = openedUpdatedAt != serverUpdatedAt || opened != server
        return changed && serverUpdatedBy != me
    }
}

/**
 * Edits an existing document in a transaction that refuses to overwrite a partner's change: throws
 * [ItemGoneException] if the document is gone, or what [conflict] builds if [isConflict] says the server
 * version changed meanwhile (skipped when [force]). Offline, a transaction can't run; then the edit is
 * written as a plain update, without the conflict check, and never throws because of being offline.
 */
internal suspend fun <T : Any> DocumentReference.updateUnlessChanged(
    db: FirebaseFirestore,
    data: Map<String, Any?>,
    force: Boolean,
    parse: (DocumentSnapshot) -> T?,
    isConflict: (T) -> Boolean,
    conflict: (T) -> Exception,
) {
    val ref = this
    try {
        db.runTransaction { tx ->
            val snap = tx.get(ref)
            if (!snap.exists()) throw ItemGoneException()
            val current = parse(snap) ?: throw ItemGoneException()
            if (!force && isConflict(current)) throw conflict(current)
            tx.update(ref, data)
            null
        }.await()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        e.causeOf<ItemGoneException>()?.let { throw it }
        e.causeOf<EditConflictException>()?.let { throw it }
        if (!e.isOffline()) throw e
        updateExisting(data)
    }
}

/** Base of the per-type conflict exceptions (they carry the server version). */
open class EditConflictException(message: String) : Exception(message)

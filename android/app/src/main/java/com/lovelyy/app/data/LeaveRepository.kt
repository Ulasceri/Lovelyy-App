package com.lovelyy.app.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/** Pure rules of leaving (unit-tested in LeaveRulesTest). */
object LeaveRules {
    /** Every shared collection under couples/{id}. Unpairing deletes all of them. Keep in sync with [Paths]. */
    val SHARED = listOf(
        Paths.CAT, Paths.SIGNALS, Paths.MOODS, Paths.MOOD_LOG, Paths.QUESTIONS, Paths.NOTES, Paths.EVENTS,
        Paths.LOCATIONS, Paths.DREAMS, Paths.WHEELS, Paths.WHEEL_SPINS, Paths.WHEEL_LOG, Paths.CAPSULES,
        Paths.STROKES, Paths.DRAWING, Paths.DRAWINGS, Paths.SETTINGS, Paths.LIMITS,
    )

    /**
     * Every local SharedPreferences file that holds couple or account data. Deleting the account clears all of
     * them (the "telemetry" file, the user's analytics choice, is kept on purpose). A new prefs file that holds
     * personal data must be added here.
     */
    val LOCAL_PREFS = listOf(
        "inbox", "signals", "lovelyy_onboarding", "capsule_draft", "couple_clock", "cat_widget", "widget_couple", "drawing_widget",
    )

    /** Paths that are not shared couple data. Everything else in [Paths] must be in [SHARED]. */
    val NOT_SHARED = setOf(Paths.USERS, Paths.COUPLES, Paths.PAIR_CODES)

    /**
     * Firebase only deletes an account signed in "recently" (about 5 minutes). Checking first, with a margin,
     * means we never delete the data and then fail on the account itself. The check uses server time only:
     * [authTimeSec] (when the user signed in) and [issuedAtSec] (when a just refreshed ID token was issued)
     * both come from Firebase's clock, so a phone clock that runs behind or ahead cannot make a fresh
     * sign-in look old (bug: "sign in again" right after signing in when the phone clock was behind).
     */
    fun signInFreshByToken(authTimeSec: Long?, issuedAtSec: Long?): Boolean =
        authTimeSec != null && issuedAtSec != null && authTimeSec > 0 && issuedAtSec > 0 &&
            (issuedAtSec - authTimeSec) * 1000L in -CLOCK_SLACK_MS..FRESH_MS

    /** Tolerance for rounding between the two server timestamps. */
    const val CLOCK_SLACK_MS = 5_000L

    const val FRESH_MS = 4 * 60_000L

    /** The other phone should leave: the couple was ended by someone else. */
    fun partnerEnded(couple: Couple?, myUid: String): Boolean =
        couple?.endedAt != null && couple.endedBy != myUid
}

/**
 * Unpairing and account deletion on the free Spark plan (no server code). The rules
 * (firebase/firestore.rules, "Unpairing") accept exactly these steps:
 * 1. [unpair]: end the couple and clear my coupleId in one batch, then delete every shared doc. Ending first means
 *    nothing is deleted when that batch is refused, and the rules block new shared writes (e.g. the cat doc an open
 *    screen recreates) while the purge runs.
 * 2. The partner's app sees the ended couple and calls [leaveEnded]: clear their coupleId and delete the couple.
 * Shared data is gone for both (Ulaş's decision, 2026-11-26).
 */
class LeaveRepository(private val db: FirebaseFirestore, private val auth: FirebaseAuth) {

    private fun userRef(uid: String) = db.collection(Paths.USERS).document(uid)
    private fun coupleRef(id: String) = db.collection(Paths.COUPLES).document(id)

    /** Ends the couple and leaves it, then deletes all shared data of the couple (both partners lose it). */
    suspend fun unpair(context: Context, me: UserProfile) {
        val coupleId = me.coupleId ?: return
        // End the couple first: if the rules refuse, nothing has been deleted yet.
        try {
            db.batch()
                .update(coupleRef(coupleId), mapOf("endedAt" to FieldValue.serverTimestamp(), "endedBy" to me.uid, "endedByName" to me.displayName.take(40)))
                .update(userRef(me.uid), "coupleId", null)
                .commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Both partners pressed "unpair" at the same moment: the other one already ended (or even removed)
            // the couple, so ours is refused. The goal is reached anyway; just leave like the partner's app would.
            if (!endedByOtherSide(coupleId)) throw e
            Timber.i("Couple was already ended by the partner; leaving it")
            leaveEndedCouple(coupleId, me.uid)
        }
        // Already left: a failed purge must not stop account deletion. The partner's leaveEnded purges again.
        runCatching { purge(coupleId) }.onFailure { Timber.w(it, "Deleting the shared data after unpairing failed") }
        clearLocal(context)
    }

    /** True when the couple doc is gone or already ended (by the partner, in a simultaneous unpair). */
    private suspend fun endedByOtherSide(coupleId: String): Boolean {
        val snap = runCatching { coupleRef(coupleId).get(Source.SERVER).await() }.getOrElse { e ->
            // A removed couple can no longer be read (we are not a member of a missing doc).
            return e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        return !snap.exists() || snap.get("endedAt") != null
    }

    /** Clears my coupleId and removes the ended couple doc; tolerates the partner having removed it already. */
    private suspend fun leaveEndedCouple(coupleId: String, myUid: String) {
        val removed = runCatching {
            db.batch().update(userRef(myUid), "coupleId", null).delete(coupleRef(coupleId)).commit().await()
        }
        if (removed.isFailure) userRef(myUid).update("coupleId", null).await()
    }

    /** The partner ended the couple: delete whatever was written meanwhile, leave and remove the couple doc. */
    suspend fun leaveEnded(context: Context, me: UserProfile, coupleId: String) {
        runCatching { purge(coupleId) }.onFailure { Timber.w(it, "Cleaning up the ended couple failed") }
        leaveEndedCouple(coupleId, me.uid)
        clearLocal(context)
    }

    sealed interface DeleteResult {
        data object Deleted : DeleteResult
        /** Signed in too long ago: sign in again, then delete. Nothing was deleted. */
        data object NeedsFreshSignIn : DeleteResult
    }

    /** Unpairs (if paired), deletes users/{uid} and the Firebase account. Checks the sign-in age first. */
    suspend fun deleteAccount(context: Context, me: UserProfile): DeleteResult {
        val user = auth.currentUser ?: return DeleteResult.NeedsFreshSignIn
        // Server clock only: a refreshed token's issue time vs. its sign-in time (the phone clock may be wrong).
        // Network errors surface as a plain failure (not "sign in again", which would sign the user out).
        val token = user.getIdToken(true).await()
        if (!LeaveRules.signInFreshByToken(token.authTimestamp, token.issuedAtTimestamp)) return DeleteResult.NeedsFreshSignIn
        if (me.coupleId != null) unpair(context, me)
        me.pairCode?.let { code -> runCatching { db.collection(Paths.PAIR_CODES).document(code).delete().await() } }
        userRef(me.uid).delete().await()
        user.delete().await()
        clearLocal(context)
        wipeLocal(context)
        return DeleteResult.Deleted
    }

    /** Deletes every doc of every shared collection, in batches (Firestore allows 500 writes per batch). */
    private suspend fun purge(coupleId: String) {
        for (name in LeaveRules.SHARED) {
            val col = coupleRef(coupleId).collection(name)
            while (true) {
                val docs = col.limit(PAGE).get().await().documents
                if (docs.isEmpty()) break
                val batch = db.batch()
                docs.forEach { batch.delete(it.reference) }
                batch.commit().await()
                if (docs.size < PAGE) break
            }
        }
    }

    /** Account deleted: nothing of ours may stay on the phone (preferences, widget picture, telemetry ids). */
    private fun wipeLocal(context: Context) {
        LeaveRules.LOCAL_PREFS.forEach { name ->
            runCatching { context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit() }
        }
        runCatching { java.io.File(context.filesDir, com.lovelyy.app.widget.DrawingWidget.FILE).delete() }
        com.lovelyy.app.widget.DrawingWidget.updateAll(context)
        com.lovelyy.app.widget.DaysWidget.updateAll(context)
        com.lovelyy.app.widget.CatWidget.updateAll(context)
        com.lovelyy.app.telemetry.Telemetry.wipe(context)
    }

    /** The widgets and the cached home screen must not show the old couple. */
    private fun clearLocal(context: Context) {
        com.lovelyy.app.widget.WidgetData.clear(context)
        // Stop listening to the old couple's signals and forget what was seen there.
        (context.applicationContext as? com.lovelyy.app.LovelyyApplication)?.signalCenter?.reset()
        context.getSharedPreferences("inbox", Context.MODE_PRIVATE).edit().clear().apply()
    }

    private companion object {
        const val PAGE = 400L
    }
}

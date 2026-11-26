package com.lovelyy.app.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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
     * Firebase only deletes an account signed in "recently" (about 5 minutes). Checking first, with a margin,
     * means we never delete the data and then fail on the account itself.
     */
    fun signInFresh(lastSignInMillis: Long?, now: Long): Boolean =
        lastSignInMillis != null && now - lastSignInMillis in 0..FRESH_MS

    const val FRESH_MS = 4 * 60_000L

    /** The other phone should leave: the couple was ended by someone else. */
    fun partnerEnded(couple: Couple?, myUid: String): Boolean =
        couple?.endedAt != null && couple.endedBy != myUid
}

/**
 * Unpairing and account deletion on the free Spark plan (no server code). The rules
 * (firebase/firestore.rules, "Unpairing") accept exactly these steps:
 * 1. [unpair]: delete every shared doc, then end the couple and clear my coupleId in one batch.
 * 2. The partner's app sees the ended couple and calls [leaveEnded]: clear their coupleId and delete the couple.
 * Shared data is gone for both (Ulaş's decision, 2026-11-26).
 */
class LeaveRepository(private val db: FirebaseFirestore, private val auth: FirebaseAuth) {

    private fun userRef(uid: String) = db.collection(Paths.USERS).document(uid)
    private fun coupleRef(id: String) = db.collection(Paths.COUPLES).document(id)

    /** Deletes all shared data of the couple (both partners lose it), then ends the couple and leaves it. */
    suspend fun unpair(context: Context, me: UserProfile) {
        val coupleId = me.coupleId ?: return
        purge(coupleId)
        db.batch()
            .update(coupleRef(coupleId), mapOf("endedAt" to FieldValue.serverTimestamp(), "endedBy" to me.uid, "endedByName" to me.displayName.take(40)))
            .update(userRef(me.uid), "coupleId", null)
            .commit().await()
        clearLocal(context)
    }

    /** The partner ended the couple: delete whatever was written meanwhile, leave and remove the couple doc. */
    suspend fun leaveEnded(context: Context, me: UserProfile, coupleId: String) {
        runCatching { purge(coupleId) }.onFailure { Timber.w(it, "Cleaning up the ended couple failed") }
        db.batch()
            .update(userRef(me.uid), "coupleId", null)
            .delete(coupleRef(coupleId))
            .commit().await()
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
        if (!LeaveRules.signInFresh(user.metadata?.lastSignInTimestamp, System.currentTimeMillis())) return DeleteResult.NeedsFreshSignIn
        if (me.coupleId != null) unpair(context, me)
        me.pairCode?.let { code -> runCatching { db.collection(Paths.PAIR_CODES).document(code).delete().await() } }
        userRef(me.uid).delete().await()
        user.delete().await()
        clearLocal(context)
        CapsuleDraft.clear(context)
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

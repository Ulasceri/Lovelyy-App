package com.lovelyy.app.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Writes the couple's Lovelyy Plus map (couples/{id}.plus). Reading needs no listener of its own: the couple
 * document is already observed by AppViewModel, so [Couple.plus] arrives with it.
 */
class PlusRepository(private val db: FirebaseFirestore) {

    private fun couple(id: String) = db.collection(Paths.COUPLES).document(id)

    /**
     * Starts the 14-day trial if the couple has no Plus yet. Runs in a transaction so the two phones (or the
     * post-pairing screen and Home) never start it twice. Returns true only for the call that started it.
     */
    suspend fun ensureTrial(coupleId: String, now: Long): Boolean {
        val ref = couple(coupleId)
        return db.runTransaction { tx ->
            val snap = tx.get(ref)
            if (!snap.exists() || snap.get("endedAt") != null || snap.get("plus") != null) {
                false
            } else {
                tx.update(ref, "plus", PlusInfo(trialStart = now).toMap())
                true
            }
        }.await() == true
    }

    /** Replaces the plus map (after a purchase, restore, sync or takeover; see PlusRules.afterPurchase/sync). */
    suspend fun write(coupleId: String, plus: PlusInfo) {
        couple(coupleId).update("plus", plus.toMap()).await()
    }
}

package com.lovelyy.app.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * couples/{id}/wheels/{wheelId}: a decision wheel. The three built-in wheels ("yemek", "gezi", "izle")
 * exist without a document until the couple edits them; custom wheels get an auto id.
 */
data class Wheel(val id: String, val label: String, val items: List<String>, val builtIn: Boolean, val at: Long = 0L)

/** Pure wheel math (unit-tested in WheelMathTest). Wedges start at the top and go clockwise. */
object WheelMath {
    const val MIN_ITEMS = 2
    const val MAX_ITEMS = 12

    /** Which item is under the pointer (top) after the wheel was rotated clockwise by [rotation] degrees. */
    fun indexAt(rotation: Float, count: Int): Int {
        if (count <= 0) return 0
        val wedge = 360f / count
        val r = ((rotation % 360f) + 360f) % 360f
        return (((360f - r) % 360f) / wedge).toInt().coerceIn(0, count - 1)
    }

    /** A new rotation that lands in the middle-ish of item [target]: several full turns plus a small jitter. */
    fun spinTo(current: Float, target: Int, count: Int, turns: Int = 4, jitter: Float = 0f): Float {
        val wedge = 360f / count
        // Rotation r puts angle (360 - r) under the pointer; the middle of [target] is (target + 0.5) * wedge.
        val wanted = ((360f - (target + 0.5f) * wedge + jitter * wedge * 0.35f) % 360f + 360f) % 360f
        val base = current - (((current % 360f) + 360f) % 360f)
        var next = base + 360f * turns + wanted
        while (next < current + 360f * turns) next += 360f
        return next
    }

    /** How long one spin animates; both phones use it. */
    const val SPIN_MS = 3200

    /** A spin started within this window of another one is the same spin (slightly longer than [SPIN_MS]). */
    const val SPIN_WINDOW_MS = 4_000L

    /**
     * True when a spin stamped [at] (another phone's clock) is still "in the air" at [now].
     * Uses the absolute difference so a partner clock that runs a little ahead also counts as recent.
     */
    fun isRecent(at: Long, now: Long): Boolean = at > 0L && kotlin.math.abs(now - at) < SPIN_WINDOW_MS

    /** Whether a new spin may replace the stored one: only when no other spin of that wheel is still running. */
    fun acceptsNewSpin(lastAt: Long?, now: Long): Boolean = lastAt == null || !isRecent(lastAt, now)

    /** How long a late viewer should animate a spin stamped [at] so it ends about when the spinner's did. */
    fun remainingMs(at: Long, now: Long, minMs: Int = 1_200): Int =
        (SPIN_MS - (now - at).coerceIn(0L, SPIN_MS.toLong())).toInt().coerceAtLeast(minMs)
}

/** The last result of one wheel, shared by both partners (couples/{id}/wheelSpins/{wheelId}). */
data class WheelSpin(val wheelId: String, val label: String, val result: String, val by: String, val byName: String, val at: Long)

/** Outcome of trying to spin: the spin both phones agree on, and whether it is ours ([won]). */
data class SpinClaim(val spin: WheelSpin, val won: Boolean)

class WheelRepository(private val db: FirebaseFirestore) {

    private fun spins(coupleId: String) =
        db.collection(Paths.COUPLES).document(coupleId).collection(Paths.WHEEL_SPINS)

    /**
     * Saves the decision before the wheel stops, so the partner sees the same result. Runs as a transaction:
     * when both partners spin at the same moment only the first spin is stored, and the other phone gets
     * that spin back ([SpinClaim.won] false) to show instead of its own.
     */
    suspend fun claimSpin(coupleId: String, me: UserProfile, wheelId: String, label: String, result: String): SpinClaim {
        val ref = spins(coupleId).document(wheelId)
        return db.runTransaction { tx ->
            val snap = tx.get(ref)
            val now = System.currentTimeMillis()
            val existing = if (snap.exists()) snap.toSpin() else null
            if (existing != null && !WheelMath.acceptsNewSpin(existing.at, now)) {
                SpinClaim(existing, won = false)
            } else {
                val spin = WheelSpin(wheelId, label.take(MAX_LABEL), result.take(MAX_ITEM), me.uid, me.displayName, now)
                tx.set(ref, spin.toMap())
                SpinClaim(spin, won = true)
            }
        }.await()
    }

    /** Plain write used when the transaction cannot run (e.g. offline): Firestore queues it. */
    suspend fun saveSpin(coupleId: String, me: UserProfile, wheelId: String, label: String, result: String) {
        val spin = WheelSpin(wheelId, label.take(MAX_LABEL), result.take(MAX_ITEM), me.uid, me.displayName, System.currentTimeMillis())
        spins(coupleId).document(wheelId).set(spin.toMap()).await()
    }

    private fun WheelSpin.toMap() = mapOf("label" to label, "result" to result, "by" to by, "byName" to byName, "at" to at)

    private fun DocumentSnapshot.toSpin() = WheelSpin(
        id, getString("label").orEmpty(), getString("result").orEmpty(),
        getString("by").orEmpty(), getString("byName").orEmpty(), getLong("at") ?: 0L,
    )

    /** The last result of every wheel, by wheel id. */
    fun observeSpins(coupleId: String): Flow<Map<String, WheelSpin>> = callbackFlow {
        val reg = spins(coupleId).limit(40).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
            } else if (snap != null) {
                trySend(
                    snap.documents.associate { d -> d.id to d.toSpin() },
                )
            }
        }
        awaitClose { reg.remove() }
    }


    private fun wheels(coupleId: String) =
        db.collection(Paths.COUPLES).document(coupleId).collection(Paths.WHEELS)

    /** Creates or replaces a wheel. Built-in ids are kept, so editing "yemek" overrides the default. */
    suspend fun save(coupleId: String, id: String?, label: String, items: List<String>) {
        val data = mapOf(
            "label" to label.trim().take(MAX_LABEL),
            "items" to items.map { it.trim().take(MAX_ITEM) }.filter { it.isNotEmpty() }.distinct().take(WheelMath.MAX_ITEMS),
            "at" to System.currentTimeMillis(),
        )
        if (id == null) wheels(coupleId).add(data).await() else wheels(coupleId).document(id).set(data).await()
    }

    suspend fun delete(coupleId: String, id: String) {
        wheels(coupleId).document(id).delete().await()
    }

    /** Stored wheels (edited built-ins and custom ones), oldest first. */
    fun observe(coupleId: String): Flow<List<Wheel>> = callbackFlow {
        val reg = wheels(coupleId).orderBy("at", Query.Direction.ASCENDING).limit(30).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
            } else if (snap != null) {
                trySend(
                    snap.documents.mapNotNull { d ->
                        @Suppress("UNCHECKED_CAST")
                        val items = (d.get("items") as? List<Any?>).orEmpty().mapNotNull { it as? String }
                        Wheel(d.id, d.getString("label").orEmpty(), items, builtIn = d.id in BUILT_IN_IDS, at = d.getLong("at") ?: 0L)
                    },
                )
            }
        }
        awaitClose { reg.remove() }
    }

    companion object {
        const val MAX_LABEL = 24
        const val MAX_ITEM = 20
        val BUILT_IN_IDS = listOf("yemek", "gezi", "izle")
    }
}

package com.lovelyy.app.data

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
}

class WheelRepository(private val db: FirebaseFirestore) {

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

package com.lovelyy.app.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.math.hypot

/**
 * One line on the shared canvas. Points are normalized (0..1 of the canvas width/height) as x,y pairs,
 * so both phones draw the same picture whatever their screen size. [width] is a fraction of the width.
 * An eraser stroke is simply a white stroke.
 */
data class Stroke(val id: String, val by: String, val color: Long, val width: Float, val points: List<Float>, val at: Long)

/** A drawing saved to Anılar. */
data class SavedDrawing(val id: String, val strokes: List<Stroke>, val by: String, val byName: String, val at: Long)

/** Pure helpers for the canvas (unit-tested in DrawingMathTest). */
object DrawingMath {
    /** Drops points closer than [minStep] (normalized) to the last kept one, and caps the count. Keeps the last point. */
    fun simplify(points: List<Float>, minStep: Float = 0.004f, max: Int = MAX_POINTS): List<Float> {
        if (points.size <= 4) return points
        val out = ArrayList<Float>(points.size)
        out += points[0]; out += points[1]
        var i = 2
        while (i + 1 < points.size) {
            val x = points[i]; val y = points[i + 1]
            if (hypot((x - out[out.size - 2]).toDouble(), (y - out[out.size - 1]).toDouble()) >= minStep) { out += x; out += y }
            i += 2
        }
        if (out[out.size - 2] != points[points.size - 2] || out[out.size - 1] != points[points.size - 1]) {
            out += points[points.size - 2]; out += points[points.size - 1]
        }
        if (out.size / 2 <= max) return out
        // Too long: keep every n-th point.
        val n = (out.size / 2 + max - 1) / max
        return out.chunked(2).filterIndexed { idx, _ -> idx % n == 0 }.flatten()
    }

    const val MAX_POINTS = 400
    /** How long after the last stroke someone still counts as "drawing now". */
    const val ACTIVE_MS = 45_000L
}

class DrawingRepository(private val db: FirebaseFirestore) {

    private fun couple(coupleId: String) = db.collection(Paths.COUPLES).document(coupleId)
    private fun strokes(coupleId: String) = couple(coupleId).collection(Paths.STROKES)
    private fun presence(coupleId: String) = couple(coupleId).collection(Paths.DRAWING).document("presence")
    private fun saved(coupleId: String) = couple(coupleId).collection(Paths.DRAWINGS)

    suspend fun add(coupleId: String, uid: String, color: Long, width: Float, points: List<Float>) {
        strokes(coupleId).add(
            mapOf(
                "by" to uid,
                "color" to color,
                "width" to width.toDouble(),
                "points" to DrawingMath.simplify(points).map { (it * 10000).toInt() / 10000.0 },
                "at" to System.currentTimeMillis(),
            ),
        ).await()
        touch(coupleId, uid)
    }

    suspend fun delete(coupleId: String, id: String) {
        strokes(coupleId).document(id).delete().await()
    }

    /** Marks [uid] as drawing right now. */
    suspend fun touch(coupleId: String, uid: String) {
        presence(coupleId).set(mapOf(uid to System.currentTimeMillis()), SetOptions.merge()).await()
    }

    /** Copies the canvas to Anılar and clears it for a new drawing. */
    suspend fun saveAndClear(coupleId: String, me: UserProfile, current: List<Stroke>) {
        if (current.isEmpty()) return
        saved(coupleId).add(
            mapOf(
                "strokes" to current.map { it.toMap() },
                "by" to me.uid,
                "byName" to me.displayName,
                "at" to System.currentTimeMillis(),
            ),
        ).await()
        current.chunked(400).forEach { part ->
            val batch = db.batch()
            part.forEach { batch.delete(strokes(coupleId).document(it.id)) }
            batch.commit().await()
        }
    }

    fun observe(coupleId: String): Flow<List<Stroke>> = callbackFlow {
        val reg = strokes(coupleId).orderBy("at", Query.Direction.ASCENDING).limit(MAX_STROKES.toLong()).addSnapshotListener { snap, err ->
            if (err != null) close(err) else if (snap != null) trySend(snap.documents.mapNotNull { d -> d.data?.toStroke(d.id) })
        }
        awaitClose { reg.remove() }
    }

    /** One read of the canvas (for the widget refresh job). */
    suspend fun current(coupleId: String): List<Stroke> =
        strokes(coupleId).orderBy("at", Query.Direction.ASCENDING).limit(MAX_STROKES.toLong()).get().await()
            .documents.mapNotNull { d -> d.data?.toStroke(d.id) }

    /** uid → last time they drew. */
    fun observePresence(coupleId: String): Flow<Map<String, Long>> = callbackFlow {
        val reg = presence(coupleId).addSnapshotListener { snap, err ->
            if (err != null) close(err)
            else trySend(snap?.data.orEmpty().mapNotNull { (k, v) -> (v as? Number)?.let { k to it.toLong() } }.toMap())
        }
        awaitClose { reg.remove() }
    }

    fun observeSaved(coupleId: String): Flow<List<SavedDrawing>> = callbackFlow {
        val reg = saved(coupleId).orderBy("at", Query.Direction.DESCENDING).limit(20).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
            } else if (snap != null) {
                trySend(
                    snap.documents.map { d ->
                        @Suppress("UNCHECKED_CAST")
                        val list = (d.get("strokes") as? List<Map<String, Any?>>).orEmpty()
                        SavedDrawing(
                            d.id, list.mapIndexedNotNull { i, m -> m.toStroke("$i") },
                            d.getString("by").orEmpty(), d.getString("byName").orEmpty(), d.getLong("at") ?: 0L,
                        )
                    },
                )
            }
        }
        awaitClose { reg.remove() }
    }

    private fun Stroke.toMap() = mapOf("by" to by, "color" to color, "width" to width.toDouble(), "points" to points.map { it.toDouble() }, "at" to at)

    private fun Map<String, Any?>.toStroke(id: String): Stroke? {
        @Suppress("UNCHECKED_CAST")
        val pts = (this["points"] as? List<Any?>)?.mapNotNull { (it as? Number)?.toFloat() } ?: return null
        if (pts.size < 2) return null
        return Stroke(
            id, this["by"] as? String ?: "", (this["color"] as? Number)?.toLong() ?: 0xFF4A3B35,
            (this["width"] as? Number)?.toFloat() ?: 0.015f, pts, (this["at"] as? Number)?.toLong() ?: 0L,
        )
    }

    companion object {
        const val MAX_STROKES = 600
    }
}

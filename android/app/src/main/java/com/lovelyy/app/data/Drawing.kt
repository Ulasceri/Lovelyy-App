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

    /** Oldest first (ties by id) — the order strokes are painted in. */
    fun inDrawingOrder(strokes: List<Stroke>): List<Stroke> = strokes.sortedWith(compareBy<Stroke> { it.at }.thenBy { it.id })

    /**
     * A cheap fingerprint of the canvas: stroke count + newest stroke id. Adding, undoing or clearing always
     * changes it, so a background job can skip re-reading every stroke while it stays the same.
     */
    fun stamp(count: Long, newestId: String?): String = "$count:${newestId.orEmpty()}"

    /** [stamp] of a canvas loaded in full (drawing order, oldest first). */
    fun stampOf(ordered: List<Stroke>): String = stamp(ordered.size.toLong(), ordered.lastOrNull()?.id)

    /**
     * Whether a locally cached canvas ([ordered], at most [max] strokes) is still the server's canvas described by
     * [serverStamp]. Past [max] strokes only the newest [max] are ever loaded, so then the newest id decides.
     */
    fun stampMatches(serverStamp: String, ordered: List<Stroke>, max: Int): Boolean {
        val count = serverStamp.substringBefore(':').toLongOrNull() ?: return false
        val newest = serverStamp.substringAfter(':').ifEmpty { null }
        return if (count > max) ordered.size == max && ordered.lastOrNull()?.id == newest
        else serverStamp == stampOf(ordered)
    }

    /** Id of the saved drawing for an ordered canvas: same canvas => same id, so a double save is a no-op. */
    fun savedId(ordered: List<Stroke>): String = "d_" + ordered.first().id

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
        // No presence write here: the caller marks "drawing now" at most every 20 s (DrawingViewModel.touch),
        // instead of a second write (and a read on the partner's phone) for every stroke.
    }

    suspend fun delete(coupleId: String, id: String) {
        strokes(coupleId).document(id).delete().await()
    }

    /** Marks [uid] as drawing right now. */
    suspend fun touch(coupleId: String, uid: String) {
        presence(coupleId).set(mapOf(uid to System.currentTimeMillis()), SetOptions.merge()).await()
    }

    /**
     * Copies [current] to Anılar and clears exactly those strokes, atomically. The saved doc id is derived from the
     * oldest stroke, so two partners saving the same canvas can never create two drawings: the loser of the race
     * finds the drawing already saved (or the strokes gone) and does nothing. Strokes added meanwhile are not in
     * [current] and stay on the canvas. Returns false when it was already saved by someone else.
     */
    suspend fun saveAndClear(coupleId: String, me: UserProfile, current: List<Stroke>): Boolean {
        if (current.isEmpty()) return false
        val ordered = DrawingMath.inDrawingOrder(current)
        val savedRef = saved(coupleId).document(DrawingMath.savedId(ordered))
        val firstRef = strokes(coupleId).document(ordered.first().id)
        return db.runTransaction { tx ->
            if (tx.get(savedRef).exists() || !tx.get(firstRef).exists()) return@runTransaction false
            tx.set(
                savedRef,
                mapOf(
                    "strokes" to ordered.map { it.toMap() },
                    "by" to me.uid,
                    "byName" to me.displayName,
                    "at" to System.currentTimeMillis(),
                ),
            )
            ordered.forEach { tx.delete(strokes(coupleId).document(it.id)) }
            true
        }.await()
    }

    /** The newest [MAX_STROKES] strokes (newest-first query, single-field index), in drawing order. */
    fun observe(coupleId: String): Flow<List<Stroke>> = callbackFlow {
        val reg = strokes(coupleId).orderBy("at", Query.Direction.DESCENDING).limit(MAX_STROKES.toLong()).addSnapshotListener { snap, err ->
            if (err != null) close(err) else if (snap != null) trySend(DrawingMath.inDrawingOrder(snap.documents.mapNotNull { d -> d.data?.toStroke(d.id) }))
        }
        awaitClose { reg.remove() }
    }

    /** One read of the canvas (for the widget refresh job). */
    suspend fun current(coupleId: String): List<Stroke> =
        DrawingMath.inDrawingOrder(
            strokes(coupleId).orderBy("at", Query.Direction.DESCENDING).limit(MAX_STROKES.toLong()).get().await()
                .documents.mapNotNull { d -> d.data?.toStroke(d.id) },
        )

    /**
     * [DrawingMath.stamp] of the canvas for two reads (the newest stroke + a count aggregation, which costs one
     * read per 1000 strokes) instead of up to [MAX_STROKES].
     */
    suspend fun stamp(coupleId: String): String {
        val newest = strokes(coupleId).orderBy("at", Query.Direction.DESCENDING).limit(1).get().await().documents.firstOrNull()?.id
        val count = strokes(coupleId).count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count
        return DrawingMath.stamp(count, newest)
    }

    /**
     * The canvas once, for a small preview, without a listener on every stroke: the local cache when it still matches
     * the server's [stamp] (2 reads), else one full read. Offline: whatever the cache has.
     */
    suspend fun canvasOnce(coupleId: String): List<Stroke> {
        val cached = try {
            DrawingMath.inDrawingOrder(
                strokes(coupleId).orderBy("at", Query.Direction.DESCENDING).limit(MAX_STROKES.toLong())
                    .get(com.google.firebase.firestore.Source.CACHE).await().documents.mapNotNull { d -> d.data?.toStroke(d.id) },
            )
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (notCached: Exception) {
            null
        }
        if (cached != null) {
            val server = try {
                stamp(coupleId)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (offline: Exception) {
                return cached
            }
            if (DrawingMath.stampMatches(server, cached, MAX_STROKES)) return cached
        }
        return current(coupleId)
    }

    /** uid → last time they drew. */
    fun observePresence(coupleId: String): Flow<Map<String, Long>> = callbackFlow {
        val reg = presence(coupleId).addSnapshotListener { snap, err ->
            if (err != null) close(err)
            else trySend(snap?.data.orEmpty().mapNotNull { (k, v) -> (v as? Number)?.let { k to it.toLong() } }.toMap())
        }
        awaitClose { reg.remove() }
    }

    /** How many drawings are saved (count aggregation: one read per 1000, not every heavy drawing); null if unavailable. */
    suspend fun savedCount(coupleId: String): Long? = try {
        saved(coupleId).count().get(com.google.firebase.firestore.AggregateSource.SERVER).await().count
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        timber.log.Timber.w(e, "Saved drawing count unavailable")
        null
    }

    /** Removes a drawing from Anılar (both partners). */
    suspend fun deleteSaved(coupleId: String, id: String) {
        saved(coupleId).document(id).delete().await()
    }

    fun observeSaved(coupleId: String, limit: Long = 60): Flow<List<SavedDrawing>> = callbackFlow {
        val reg = saved(coupleId).orderBy("at", Query.Direction.DESCENDING).limit(limit).addSnapshotListener { snap, err ->
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

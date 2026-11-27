package com.lovelyy.app.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

/** Reads and changes the shared cat at couples/{coupleId}/cat/main. */
class CatRepository(private val db: FirebaseFirestore, private val clockSync: ClockSync) {

    private fun catRef(coupleId: String) =
        db.collection(Paths.COUPLES).document(coupleId).collection(Paths.CAT).document(Paths.CAT_DOC)

    /** Live cat. Creates it the first time a couple opens the app. */
    fun observe(coupleId: String): Flow<CatDoc> = callbackFlow {
        val ref = catRef(coupleId)
        val reg = ref.addSnapshotListener { snap, err ->
            when {
                err != null -> close(err)
                snap == null -> Unit
                snap.exists() -> trySend(snap.toCatDoc())
                // Both partners may race here; the transaction keeps the first one.
                !snap.metadata.isFromCache -> db.runTransaction { tx ->
                    // TrustedClock: firestore.rules want the new cat's times near the server clock.
                    if (!tx.get(ref).exists()) tx.set(ref, CatDoc().toMap())
                    null
                }
            }
        }
        awaitClose { reg.remove() }
    }

    /** One-time read, used by the widget's background refresh. */
    suspend fun get(coupleId: String): CatDoc? = catRef(coupleId).get().await().takeIf { it.exists() }?.toCatDoc()

    /**
     * Runs an action in a transaction (so both partners acting at once never lose an update).
     * Returns the new cat, or null if the action was refused (see [CatRules.refusal]).
     */
    suspend fun act(
        coupleId: String,
        me: UserProfile,
        members: List<String>,
        type: CatActionType,
        strokes: Int = 0,
        newName: String? = null,
        dayNow: Long = TrustedClock.now(),
        plus: Boolean = false,
    ): CatDoc? {
        val ref = catRef(coupleId)
        // Every time written here must be near the server clock (firestore.rules), so learn it first if needed.
        clockSync.ensure(coupleId, me.uid)
        return db.runTransaction { tx ->
            val snap = tx.get(ref)
            val doc = if (snap.exists()) snap.toCatDoc() else CatDoc()
            val now = TrustedClock.now()
            val changed = CatRules.apply(doc, type, now, strokes, plus) ?: return@runTransaction null
            val action = CatAction(type, me.uid, me.displayName, now)
            val result = changed.copy(
                name = newName?.trim()?.take(MAX_NAME)?.takeIf { it.isNotEmpty() } ?: changed.name,
                lastAction = action,
                recent = (listOf(action) + doc.recent).take(MAX_RECENT),
                streak = if (type == CatActionType.Name) doc.streak
                else doc.streak.afterCare(me.uid, members, CoupleClock.today(dayNow)),
            )
            tx.set(ref, result.toMap())
            result
        }.await()
    }

    /**
     * Lovelyy Plus streak protection (P5b "izin günü", P5c "telafi"): changes only the streak map, in a
     * transaction on the latest cat so both partners never use the same day off twice. [change] is
     * StreakProtection.freeze or .repair; it returns null when the action is no longer possible (then nothing is
     * written and null is returned). firestore.rules check the count and that the couple has Plus.
     */
    suspend fun changeStreak(coupleId: String, change: (CareStreak) -> CareStreak?): CareStreak? {
        val ref = catRef(coupleId)
        return db.runTransaction { tx ->
            val snap = tx.get(ref)
            if (!snap.exists()) return@runTransaction null
            val doc = snap.toCatDoc()
            val next = change(doc.streak) ?: return@runTransaction null
            tx.update(ref, "streak", next.toMap())
            next
        }.await()
    }

    /**
     * Saves Mırmır's items (P7a "Yerleştir"). Only the item fields change, in a transaction on the latest cat so
     * the reaction item is computed against what is really stored. firestore.rules check that Plus models need
     * an active Plus. Returns the item Mırmır reacts to (null when nothing new was placed).
     */
    suspend fun setItems(coupleId: String, items: CatItems): CatItem? {
        val ref = catRef(coupleId)
        return db.runTransaction { tx ->
            val snap = tx.get(ref)
            val before = if (snap.exists()) snap.toCatDoc().items else null
            val placed = CatItemRules.newlyPlaced(before, items)
            val fields = mapOf(
                "items" to items.toMap(),
                "itemsAt" to TrustedClock.now().ts(),
                "itemsNew" to placed?.id,
            )
            if (snap.exists()) tx.update(ref, fields) else tx.set(ref, CatDoc().toMap() + fields)
            placed
        }.await()
    }

    companion object {
        const val MAX_NAME = 20
        const val MAX_RECENT = 10
    }
}

// ---------- Firestore mapping ----------

private fun Long.ts() = Timestamp(Date(this))

private fun Need.toMap() = mapOf("level" to level, "at" to at.ts())

private fun CatAction.toMap() = mapOf("type" to type.name, "uid" to uid, "name" to name, "at" to at.ts())

internal fun CatDoc.toMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "hunger" to hunger.toMap(),
    "thirst" to thirst.toMap(),
    "energy" to energy.toMap(),
    "love" to love.toMap(),
    "sleepingSince" to sleepingSince?.ts(),
    "lastAction" to lastAction?.toMap(),
    "recent" to recent.map { it.toMap() },
    "streak" to streak.toMap(),
) + listOfNotNull(
    // The item fields only when set: a care action then writes no new keys, so it still passes rules that
    // predate items (set() replaces the doc, so a field left out is cleared just like a null).
    items?.let { "items" to it.toMap() },
    itemsAt?.let { "itemsAt" to it.ts() },
    itemsNew?.let { "itemsNew" to it.id },
    sleepRefillMin?.let { "sleepRefillMin" to it },
)

/** Field names match firestore.rules `validStreak`. */
internal fun CareStreak.toMap(): Map<String, Any?> = mapOf(
    "count" to count,
    "best" to best,
    "lastCompleteDay" to lastCompleteDay,
    "careDay" to careDay,
    "careBy" to careBy,
    "freezeMonth" to freezeMonth,
    "freezeUsed" to freezeUsed,
    "freezeDays" to freezeDays,
    "repairDays" to repairDays,
    "brokenCount" to brokenCount,
    "brokenLast" to brokenLast,
)

private fun Any?.millis(): Long? = when (this) {
    is Timestamp -> toDate().time
    is Date -> time
    is Number -> toLong()
    else -> null
}

@Suppress("UNCHECKED_CAST")
private fun Any?.asMap(): Map<String, Any?>? = this as? Map<String, Any?>

private fun Map<String, Any?>?.toNeed(): Need {
    val level = (this?.get("level") as? Number)?.toDouble() ?: return Need.fresh()
    val at = this["at"].millis() ?: return Need.fresh()
    return Need(level, at)
}

private fun Map<String, Any?>?.toAction(): CatAction? {
    this ?: return null
    val type = runCatching { CatActionType.valueOf(get("type") as String) }.getOrNull() ?: return null
    return CatAction(type, get("uid") as? String ?: "", get("name") as? String ?: "", get("at").millis() ?: 0L)
}

@Suppress("UNCHECKED_CAST")
internal fun DocumentSnapshot.toCatDoc(): CatDoc {
    val s = get("streak").asMap()
    return CatDoc(
        name = displayCatName(getString("name")),
        hunger = get("hunger").asMap().toNeed(),
        thirst = get("thirst").asMap().toNeed(),
        energy = get("energy").asMap().toNeed(),
        love = get("love").asMap().toNeed(),
        sleepingSince = get("sleepingSince").millis(),
        lastAction = get("lastAction").asMap().toAction(),
        recent = (get("recent") as? List<Any?>).orEmpty().mapNotNull { it.asMap().toAction() },
        streak = CareStreak(
            count = (s?.get("count") as? Number)?.toInt() ?: 0,
            best = (s?.get("best") as? Number)?.toInt() ?: 0,
            lastCompleteDay = s?.get("lastCompleteDay") as? String,
            careDay = s?.get("careDay") as? String,
            careBy = (s?.get("careBy") as? List<String>).orEmpty(),
            freezeMonth = s?.get("freezeMonth") as? String,
            freezeUsed = (s?.get("freezeUsed") as? Number)?.toInt() ?: 0,
            freezeDays = (s?.get("freezeDays") as? List<String>).orEmpty(),
            repairDays = (s?.get("repairDays") as? List<String>).orEmpty(),
            brokenCount = (s?.get("brokenCount") as? Number)?.toInt() ?: 0,
            brokenLast = s?.get("brokenLast") as? String,
        ),
        items = CatItems.fromMap(get("items") as? Map<*, *>),
        itemsAt = get("itemsAt").millis(),
        itemsNew = CatItem.of(getString("itemsNew")),
        sleepRefillMin = (get("sleepRefillMin") as? Number)?.toInt(),
    )
}

package com.lovelyy.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Pure day math in the couple's shared time zone (unit-tested in CoupleDayTest). Shared "today"s (care
 * streak, daily question, capsule open day, calendar) must not depend on each phone's own zone, or partners
 * in different zones disagree around midnight.
 */
object CoupleDay {
    /** The calendar day at [nowMillis] in [zone]. */
    fun today(zone: ZoneId, nowMillis: Long): LocalDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()

    /** A stored zone id, or null if it is missing or not a valid zone. */
    fun zoneOf(id: String?): ZoneId? =
        id?.takeIf { it.isNotBlank() }?.let { runCatching { ZoneId.of(it) }.getOrNull() }
}

/**
 * The couple's shared time zone for this process (couples/{id}/settings/main `zone`). Until it is loaded it
 * is the zone saved last time, else the phone's own zone. Saved on the phone so a cold start and the
 * home-screen widgets use it without Firestore. Followed from HomeScreen, cleared on sign-out.
 */
object CoupleClock {
    private const val PREFS = "couple_clock"
    private const val K_COUPLE = "coupleId"
    private const val K_ZONE = "zone"

    private val _zone = MutableStateFlow<ZoneId>(ZoneId.systemDefault())
    val zone: StateFlow<ZoneId> = _zone.asStateFlow()

    @Volatile private var prefs: SharedPreferences? = null
    @Volatile private var coupleId: String? = null

    /** Application.onCreate: load the zone saved last time. */
    fun init(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = p
        coupleId = p.getString(K_COUPLE, null)
        CoupleDay.zoneOf(p.getString(K_ZONE, null))?.let { _zone.value = it }
    }

    fun zone(): ZoneId = _zone.value

    /** Today in the couple's zone, at server time ([TrustedClock]). */
    fun today(nowMillis: Long = TrustedClock.now()): LocalDate = CoupleDay.today(_zone.value, nowMillis)

    /** Following [id]: a zone saved for another couple must not be used for this one. */
    fun select(id: String) {
        val saved = coupleId
        if (saved != null && saved != id) reset()
    }

    /** The couple's zone as loaded from Firestore (null while it is still being created). */
    fun apply(id: String, zoneId: String?) {
        val z = CoupleDay.zoneOf(zoneId) ?: return
        coupleId = id
        _zone.value = z
        prefs?.edit { putString(K_COUPLE, id); putString(K_ZONE, z.id) }
    }

    /** Sign-out or another couple: back to the phone's zone. */
    fun reset() {
        coupleId = null
        _zone.value = ZoneId.systemDefault()
        prefs?.edit { clear() }
    }
}

/** couples/{id}/settings/main: couple-wide settings. Today only the shared time zone. */
class CoupleSettingsRepository(private val db: FirebaseFirestore) {

    private fun ref(coupleId: String) =
        db.collection(Paths.COUPLES).document(coupleId).collection(Paths.SETTINGS).document(Paths.SETTINGS_DOC)

    /**
     * The couple's time zone id, live. The first phone that finds it missing (on the server, not just in the
     * cache) writes its own zone in a transaction; both partners may race, the transaction keeps the first.
     */
    fun observeZone(coupleId: String, myUid: String): Flow<String> = callbackFlow {
        val ref = ref(coupleId)
        val reg = ref.addSnapshotListener { snap, err ->
            val zone = snap?.getString("zone")?.takeIf { CoupleDay.zoneOf(it) != null }
            when {
                err != null -> close(err)
                snap == null -> Unit
                zone != null -> trySend(zone)
                !snap.metadata.isFromCache -> db.runTransaction { tx ->
                    if (CoupleDay.zoneOf(tx.get(ref).getString("zone")) == null) {
                        tx.set(
                            ref,
                            mapOf("zone" to ZoneId.systemDefault().id, "setBy" to myUid, "at" to FieldValue.serverTimestamp()),
                            SetOptions.merge(),
                        )
                    }
                    null
                }.addOnFailureListener { Timber.w(it, "Creating the couple time zone failed") }
            }
        }
        awaitClose { reg.remove() }
    }
}

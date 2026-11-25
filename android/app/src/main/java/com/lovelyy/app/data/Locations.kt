package com.lovelyy.app.data

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * couples/{id}/locations/{uid}: where a partner roughly is, only while they share it.
 * Coordinates are rounded before they leave the phone (city level by default), and the app
 * shows only the city and the distance. Turning sharing off deletes the document.
 */
data class SharedLocation(
    val uid: String,
    val city: String,
    val lat: Double,
    val lng: Double,
    /** true = rounded to ~10 km ("Sadece şehir ve mesafe"), false = ~1 km. */
    val cityOnly: Boolean,
    val at: Long,
)

/** Pure distance math for the Mesafe screen (unit-tested in DistanceMathTest). */
object DistanceMath {
    private const val EARTH_KM = 6371.0
    /** Roads are longer than a straight line. */
    private const val ROAD_FACTOR = 1.2
    private const val CAR_KMH = 90.0
    private const val PLANE_KMH = 800.0
    /** Taxi, climb and descent. */
    private const val PLANE_OVERHEAD_MIN = 45.0
    /** Below this a flight makes no sense: the card shows a dash. */
    const val MIN_FLIGHT_KM = 150.0

    /** Great-circle distance in km. */
    fun km(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return 2 * EARTH_KM * asin(sqrt(a))
    }

    fun driveMinutes(km: Double): Long = (km * ROAD_FACTOR / CAR_KMH * 60).roundToLong()

    fun flightMinutes(km: Double): Long? =
        if (km < MIN_FLIGHT_KM) null else (PLANE_OVERHEAD_MIN + km / PLANE_KMH * 60).roundToLong()

    /** Rounds a coordinate: 1 decimal (~10 km) for city only, 2 decimals (~1 km) otherwise. */
    fun coarse(v: Double, cityOnly: Boolean): Double {
        val f = if (cityOnly) 10.0 else 100.0
        return (v * f).roundToInt() / f
    }
}

class LocationRepository(private val db: FirebaseFirestore) {

    private fun locations(coupleId: String) =
        db.collection(Paths.COUPLES).document(coupleId).collection(Paths.LOCATIONS)

    suspend fun share(coupleId: String, uid: String, city: String, lat: Double, lng: Double, cityOnly: Boolean) {
        locations(coupleId).document(uid).set(
            mapOf(
                "city" to city.take(MAX_CITY),
                "lat" to DistanceMath.coarse(lat, cityOnly),
                "lng" to DistanceMath.coarse(lng, cityOnly),
                "cityOnly" to cityOnly,
                "at" to System.currentTimeMillis(),
            ),
        ).await()
    }

    suspend fun stop(coupleId: String, uid: String) {
        locations(coupleId).document(uid).delete().await()
    }

    /** Both partners' shared locations, keyed by uid. */
    fun observe(coupleId: String): Flow<Map<String, SharedLocation>> = callbackFlow {
        val reg = locations(coupleId).addSnapshotListener { snap, err ->
            if (err != null) {
                close(err)
            } else if (snap != null) {
                trySend(
                    snap.documents.mapNotNull { d ->
                        val lat = d.getDouble("lat") ?: return@mapNotNull null
                        val lng = d.getDouble("lng") ?: return@mapNotNull null
                        SharedLocation(
                            d.id, d.getString("city").orEmpty(), lat, lng,
                            d.getBoolean("cityOnly") ?: true, d.getLong("at") ?: 0L,
                        )
                    }.associateBy { it.uid },
                )
            }
        }
        awaitClose { reg.remove() }
    }

    companion object {
        const val MAX_CITY = 60
    }
}

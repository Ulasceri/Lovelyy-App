package com.lovelyy.app.location

import com.lovelyy.app.text.AppLocale
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import kotlin.coroutines.resume

/**
 * Rough location with only ACCESS_COARSE_LOCATION and no Play Services dependency:
 * the platform LocationManager (fused / network / passive) plus Geocoder for the city name.
 */
object CoarseLocation {

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** A recent last-known fix if there is one, otherwise a fresh one (up to ~15 s). Null when none. */
    @SuppressLint("MissingPermission")
    suspend fun current(context: Context): Location? {
        if (!hasPermission(context)) return null
        val lm = context.getSystemService(LocationManager::class.java) ?: return null
        val providers = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.PASSIVE_PROVIDER)
        }.filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }

        val now = System.currentTimeMillis()
        providers.firstNotNullOfOrNull { p ->
            runCatching { lm.getLastKnownLocation(p) }.getOrNull()?.takeIf { now - it.time < FRESH_MS }
        }?.let { return it }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            for (p in providers.filter { it != LocationManager.PASSIVE_PROVIDER }) {
                val fix = withTimeoutOrNull(TIMEOUT_MS) {
                    suspendCancellableCoroutine<Location?> { cont ->
                        val signal = CancellationSignal()
                        cont.invokeOnCancellation { signal.cancel() }
                        runCatching {
                            lm.getCurrentLocation(p, signal, ContextCompat.getMainExecutor(context)) { cont.resume(it) }
                        }.onFailure { cont.resume(null) }
                    }
                }
                if (fix != null) return fix
            }
        }
        // Old Android or no fresh fix: fall back to any last-known one.
        return providers.firstNotNullOfOrNull { p -> runCatching { lm.getLastKnownLocation(p) }.getOrNull() }
    }

    /** "Ankara": the province (admin area) or else the city of a point; empty if the geocoder can't tell. */
    suspend fun city(context: Context, lat: Double, lng: Double): String = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext ""
        runCatching {
            @Suppress("DEPRECATION")
            val a = Geocoder(context, AppLocale.current()).getFromLocation(lat, lng, 1)?.firstOrNull()
            a?.adminArea ?: a?.locality ?: a?.subAdminArea ?: ""
        }.onFailure { Timber.w(it, "Geocoder failed") }.getOrDefault("")
    }

    private const val FRESH_MS = 30 * 60_000L
    private const val TIMEOUT_MS = 15_000L
}

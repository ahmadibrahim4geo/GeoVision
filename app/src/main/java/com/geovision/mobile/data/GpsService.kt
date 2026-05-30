package com.geovision.mobile.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

object GpsService {

    data class GpsState(
        val location: Location? = null,
        val isRunning: Boolean = false
    )

    fun observeLocation(context: Context): Flow<GpsState> = callbackFlow {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            trySend(GpsState())
            close()
            return@callbackFlow
        }

        val listener = object : android.location.LocationListener {
            override fun onLocationChanged(loc: Location) {
                trySend(GpsState(location = loc, isRunning = true))
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        val minTimeMs = 1000L
        val minDistM = 1f

        try {
            if (hasFine) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTimeMs, minDistM, listener, Looper.getMainLooper())
            }
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTimeMs, minDistM, listener, Looper.getMainLooper())
            lm.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, minTimeMs * 10, minDistM * 10, listener, Looper.getMainLooper())

            val lastGps = if (hasFine) lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) else null
            val lastNet = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val lastPassive = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            val bestLast = listOfNotNull(lastGps, lastNet, lastPassive).maxByOrNull { it.accuracy }
            if (bestLast != null) {
                trySend(GpsState(location = bestLast, isRunning = true))
            }
        } catch (e: SecurityException) {
            trySend(GpsState())
        }

        awaitClose {
            lm.removeUpdates(listener)
        }
    }.distinctUntilChanged()

    fun getBestLastKnownLocation(context: Context): Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        return providers.mapNotNull { provider ->
            try { lm.getLastKnownLocation(provider) } catch (_: SecurityException) { null }
        }.maxByOrNull { it.accuracy }
    }
}

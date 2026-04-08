package com.kraeutertee.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object LocationHelper {

    fun harvestLatitudeOffset(latitude: Double): Int = when {
        latitude > 60.0 ->  2
        latitude > 54.0 ->  1
        latitude > 48.0 ->  0
        latitude > 42.0 -> -1
        else            -> -2
    }

    fun climateZoneName(latitude: Double): String = when {
        latitude > 60.0 -> "Skandinavisch (> 60°N)"
        latitude > 54.0 -> "Norddeutschland / Nordeuropa"
        latitude > 48.0 -> "Mitteleuropa (Baseline)"
        latitude > 42.0 -> "Süddeutschland / Österreich / Schweiz"
        else            -> "Mediterran"
    }

    fun harvestOffsetDescription(latitude: Double): String {
        val offset = harvestLatitudeOffset(latitude)
        return when {
            offset > 0 -> "Ernte ca. $offset Monat${if (offset > 1) "e" else ""} später als Mitteleuropa"
            offset < 0 -> "Ernte ca. ${-offset} Monat${if (-offset > 1) "e" else ""} früher als Mitteleuropa"
            else       -> "Mitteleuropäischer Erntezeitraum"
        }
    }

    /** Returns null instead of throwing when location unavailable or permission denied. */
    @SuppressLint("MissingPermission")
    suspend fun getLastLocation(context: Context): Location? =
        try {
            suspendCancellableCoroutine { cont ->
                val client = LocationServices.getFusedLocationProviderClient(context)
                client.lastLocation
                    .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                    .addOnCanceledListener  { if (cont.isActive) cont.resume(null) }
            }
        } catch (e: Exception) {
            null
        }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Location? =
        try {
            val cts = CancellationTokenSource()
            suspendCancellableCoroutine { cont ->
                val client = LocationServices.getFusedLocationProviderClient(context)
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                    .addOnSuccessListener { loc -> if (cont.isActive) cont.resume(loc) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                cont.invokeOnCancellation { cts.cancel() }
            }
        } catch (e: Exception) {
            null
        }
}

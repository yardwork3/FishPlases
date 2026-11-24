package ru.fishplaces.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import android.os.CancellationSignal
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

/** One foreground request, stopped on cancellation or after 15 seconds. No background service. */
suspend fun currentLocation(context: Context): Location? = withTimeoutOrNull(15_000) {
    if (!hasLocationPermission(context)) return@withTimeoutOrNull null
    val manager = context.getSystemService(LocationManager::class.java)
    val provider = when {
        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
        manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
        else -> return@withTimeoutOrNull null
    }
    suspendCancellableCoroutine { continuation ->
        val signal = CancellationSignal()
        continuation.invokeOnCancellation { signal.cancel() }
        try {
            LocationManagerCompat.getCurrentLocation(manager, provider, signal, ContextCompat.getMainExecutor(context)) {
                if (continuation.isActive) continuation.resume(it)
            }
        } catch (_: SecurityException) {
            if (continuation.isActive) continuation.resume(null)
        } catch (_: IllegalArgumentException) {
            if (continuation.isActive) continuation.resume(null)
        }
    }
}

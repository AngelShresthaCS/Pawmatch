package com.example.petinder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

/** A foreground-only fix; every listener is removed on success, timeout, or disposal. */
class LocationHelper(private val context: Context) {
    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val handler = Handler(Looper.getMainLooper())
    private var listener: LocationListener? = null
    private var timeout: Runnable? = null
    fun cancel() {
        listener?.let { manager.removeUpdates(it) }
        timeout?.let { handler.removeCallbacks(it) }
        listener = null; timeout = null
    }
    @Suppress("MissingPermission")
    fun locate(onSuccess: (Location) -> Unit, onError: (String) -> Unit) {
        cancel()
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) { onError("Allow location access to find pets near you."); return }
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER).filter { (fine || it != LocationManager.GPS_PROVIDER) && manager.isProviderEnabled(it) }
        if (providers.isEmpty()) { onError("Turn on Location in Android settings, then try again."); return }
        val cached = providers.mapNotNull { manager.getLastKnownLocation(it) }.filter { android.os.SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos < 120_000_000_000L }.minByOrNull { it.accuracy }
        if (cached != null) { onSuccess(cached); return }
        val callback = object : LocationListener {
            override fun onLocationChanged(location: Location) { cancel(); onSuccess(location) }
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            @Deprecated("Legacy Android callback")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }
        listener = callback
        try {
            providers.forEach { manager.requestLocationUpdates(it, 0L, 0f, callback, Looper.getMainLooper()) }
            timeout = Runnable { cancel(); onError("No location fix yet. Try outdoors or use the Chicago sample location.") }.also { handler.postDelayed(it, 15_000) }
        } catch (_: SecurityException) { cancel(); onError("Location permission changed. Please try again.") }
    }
}

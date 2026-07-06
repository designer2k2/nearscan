package at.designer2k2.nearscan.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Acquires a single location fix then immediately stops listening — the whole point of NearScan
 * is that the GPS radio does NOT stay on during scanning. This is only used to prefill the
 * "Set Location" dialog with the user's current position, so a fast, rough network-based fix
 * is preferred over GPS: GPS can take 30+ seconds for a cold fix (especially indoors), while
 * network location (cell/WiFi-based) resolves almost instantly and is accurate enough for
 * a manually-set base location.
 */
@Singleton
class LocationHelper @Inject constructor() {

    @Suppress("MissingPermission")
    suspend fun getSingleFix(context: Context): Location? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null

        return suspendCancellableCoroutine { cont ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    lm.removeUpdates(this)
                    if (cont.isActive) cont.resume(location)
                }

                @Deprecated("Deprecated in API 29")
                override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {
                }

                override fun onProviderDisabled(provider: String) {}
                override fun onProviderEnabled(provider: String) {}
            }

            // Network first: much faster than GPS, and plenty accurate for a manually-set location.
            val providers = listOfNotNull(
                LocationManager.NETWORK_PROVIDER.takeIf { lm.isProviderEnabled(it) },
                LocationManager.GPS_PROVIDER.takeIf { lm.isProviderEnabled(it) },
            )

            if (providers.isEmpty()) {
                cont.resume(null)
                return@suspendCancellableCoroutine
            }

            // Return any cached last-known fix immediately; otherwise wait for one live update
            // from the fastest available provider.
            val lastKnown = providers.firstNotNullOfOrNull { lm.getLastKnownLocation(it) }
            if (lastKnown != null) {
                cont.resume(lastKnown)
                return@suspendCancellableCoroutine
            }

            lm.requestSingleUpdate(providers.first(), listener, Looper.getMainLooper())
            cont.invokeOnCancellation { lm.removeUpdates(listener) }
        }
    }
}

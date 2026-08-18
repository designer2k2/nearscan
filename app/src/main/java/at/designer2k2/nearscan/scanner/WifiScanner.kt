package at.designer2k2.nearscan.scanner

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import at.designer2k2.nearscan.db.WifiScanEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * WiFi scanner. Triggers [WifiManager.startScan] and collects the results delivered
 * via [WifiManager.SCAN_RESULTS_AVAILABLE_ACTION]. Returns an empty list if scanning is
 * unavailable / throttled / not permitted.
 */
@Singleton
class WifiScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wifiManager: WifiManager,
) {
    @Suppress("DEPRECATION", "MissingPermission")
    suspend fun scan(): List<WifiScanEntity> {
        // Android 9+ requires ACCESS_FINE_LOCATION to receive populated WiFi scan results.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }

        val now = System.currentTimeMillis()
        return withTimeoutOrNull(SCAN_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(c: Context?, intent: Intent?) {
                        runCatching { context.unregisterReceiver(this) }
                        if (cont.isActive) cont.resume(currentScanRows(now))
                    }
                }
                context.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
                cont.invokeOnCancellation { runCatching { context.unregisterReceiver(receiver) } }
                if (!wifiManager.startScan()) {
                    // Android 9+ throttles foreground apps to 4 scans per 2 minutes; a throttled
                    // request never fires SCAN_RESULTS_AVAILABLE_ACTION, so fall back to whatever
                    // results are already cached from the last successful scan instead of an
                    // empty round.
                    runCatching { context.unregisterReceiver(receiver) }
                    if (cont.isActive) cont.resume(currentScanRows(now))
                }
            }
        } ?: emptyList()
    }

    private fun currentScanRows(timestamp: Long): List<WifiScanEntity> {
        val results = runCatching { wifiManager.scanResults }.getOrDefault(emptyList())
        return results.map { r ->
            WifiScanEntity(
                timestamp = timestamp,
                latitude = null,
                longitude = null,
                altitude = null,
                ssid = r.SSID,
                bssid = r.BSSID ?: "",
                rssi = r.level,
                frequency = r.frequency,
                channel = frequencyToChannel(r.frequency),
                capabilities = r.capabilities,
                band = bandFor(r.frequency),
            )
        }
    }

    private fun frequencyToChannel(freq: Int): Int = when {
        freq == 2484 -> 14
        freq in 2412..2472 -> (freq - 2412) / 5 + 1
        freq in 5170..5825 -> (freq - 5000) / 5
        freq in 5955..7115 -> (freq - 5950) / 5
        else -> -1
    }

    private fun bandFor(freq: Int): String = when {
        freq < 2500 -> "2.4"
        freq < 5925 -> "5"
        else -> "6"
    }

    private companion object {
        const val SCAN_TIMEOUT_MS = 8_000L
    }
}

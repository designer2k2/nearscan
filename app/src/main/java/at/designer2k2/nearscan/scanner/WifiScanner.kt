package at.designer2k2.nearscan.scanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import at.designer2k2.nearscan.db.WifiScanEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Stub WiFi scanner. Triggers [WifiManager.startScan] and collects the results delivered
 * via [WifiManager.SCAN_RESULTS_AVAILABLE_ACTION]. Returns an empty list if scanning is
 * unavailable / throttled.
 */
@Singleton
class WifiScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val wifiManager: WifiManager,
) {
    @Suppress("DEPRECATION", "MissingPermission")
    suspend fun scan(): List<WifiScanEntity> {
        val now = System.currentTimeMillis()
        return withTimeoutOrNull(SCAN_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(c: Context?, intent: Intent?) {
                        runCatching { context.unregisterReceiver(this) }
                        val results = runCatching { wifiManager.scanResults }.getOrDefault(emptyList())
                        val rows = results.map { r ->
                            WifiScanEntity(
                                timestamp = now,
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
                        if (cont.isActive) cont.resume(rows)
                    }
                }
                context.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
                cont.invokeOnCancellation { runCatching { context.unregisterReceiver(receiver) } }
                if (!wifiManager.startScan()) {
                    // Throttled: fall back to whatever cached results exist.
                    runCatching { context.unregisterReceiver(receiver) }
                    if (cont.isActive) cont.resume(emptyList())
                }
            }
        } ?: emptyList()
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

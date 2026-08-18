package at.designer2k2.nearscan.scanner

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import at.designer2k2.nearscan.db.BtScanEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BLE scanner. Scans in a short low-latency burst, deduplicating by device address and
 * keeping the latest RSSI per device, then stops and returns the collected results.
 */
@Singleton
class BleScanner @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter?,
) {
    @Suppress("MissingPermission")
    suspend fun scan(): List<BtScanEntity> {
        val adapter = bluetoothAdapter ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()
        val scanner = adapter.bluetoothLeScanner ?: return emptyList()

        val now = System.currentTimeMillis()
        val found = LinkedHashMap<String, BtScanEntity>()
        val failed = AtomicBoolean(false)

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                result ?: return
                val device = result.device ?: return
                synchronized(found) {
                    found[device.address] = BtScanEntity(
                        timestamp = now,
                        latitude = null,
                        longitude = null,
                        altitude = null,
                        address = device.address,
                        name = device.name,
                        rssi = result.rssi,
                        deviceClass = 0,
                        isBle = true,
                    )
                }
            }

            override fun onScanFailed(errorCode: Int) {
                failed.set(true)
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        // Since Android 8.1, AOSP suspends delivery of *unfiltered* BLE scans while the screen is
        // off — passing an (empty-matching) filter list instead of null keeps results flowing.
        val filters = listOf(ScanFilter.Builder().build())

        val result = withTimeoutOrNull(OUTER_TIMEOUT_MS) {
            val started = runCatching { scanner.startScan(filters, settings, callback) }
            if (started.isFailure) return@withTimeoutOrNull emptyList()
            delay(SCAN_WINDOW_MS)
            if (failed.get()) emptyList() else synchronized(found) { found.values.toList() }
        }

        runCatching { scanner.stopScan(callback) }
        return result ?: synchronized(found) { found.values.toList() }
    }

    private companion object {
        const val SCAN_WINDOW_MS = 5_000L
        const val OUTER_TIMEOUT_MS = 6_000L
    }
}

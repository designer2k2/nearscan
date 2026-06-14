package at.designer2k2.nearscan.scanner

import android.bluetooth.BluetoothAdapter
import at.designer2k2.nearscan.db.BtScanEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stub BLE scanner. A full implementation would use
 * [BluetoothAdapter.getBluetoothLeScanner] with SCAN_MODE_LOW_POWER and collect results
 * via ScanCallback within the interval window, deduplicating by address.
 */
@Singleton
class BleScanner @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter?,
) {
    @Suppress("MissingPermission")
    suspend fun scan(): List<BtScanEntity> {
        val adapter = bluetoothAdapter ?: return emptyList()
        val scanner = adapter.bluetoothLeScanner ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()
        // TODO: scanner.startScan(...); collect onScanResult; stopScan() after window.
        @Suppress("UNUSED_EXPRESSION") scanner
        return emptyList()
    }
}

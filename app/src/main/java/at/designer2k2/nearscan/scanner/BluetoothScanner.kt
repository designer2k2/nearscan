package at.designer2k2.nearscan.scanner

import android.bluetooth.BluetoothAdapter
import android.content.Context
import at.designer2k2.nearscan.db.BtScanEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stub Bluetooth Classic scanner. A full implementation would call
 * [BluetoothAdapter.startDiscovery] and collect devices via an ACTION_FOUND receiver.
 */
@Singleton
class BluetoothScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?,
) {
    @Suppress("MissingPermission")
    suspend fun scan(): List<BtScanEntity> {
        val adapter = bluetoothAdapter ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()
        // TODO: register ACTION_FOUND receiver, startDiscovery(), collect within interval window.
        return emptyList()
    }
}

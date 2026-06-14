package at.designer2k2.nearscan.scanner

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import at.designer2k2.nearscan.db.BtScanEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Bluetooth Classic scanner. Runs a single discovery cycle, collecting devices via an
 * [BluetoothDevice.ACTION_FOUND] receiver and returning once
 * [BluetoothAdapter.ACTION_DISCOVERY_FINISHED] fires (or the timeout elapses).
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
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }

        val now = System.currentTimeMillis()
        val found = LinkedHashMap<String, BtScanEntity>()

        return withTimeoutOrNull(DISCOVERY_TIMEOUT_MS) {
            suspendCancellableCoroutine { cont ->
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(c: Context?, intent: Intent?) {
                        when (intent?.action) {
                            BluetoothDevice.ACTION_FOUND -> {
                                @Suppress("DEPRECATION")
                                val device: BluetoothDevice? =
                                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                                val rssi = intent.getShortExtra(
                                    BluetoothDevice.EXTRA_RSSI,
                                    (-127).toShort(),
                                ).toInt()
                                if (device != null) {
                                    found[device.address] = BtScanEntity(
                                        timestamp = now,
                                        latitude = null,
                                        longitude = null,
                                        altitude = null,
                                        address = device.address,
                                        name = device.name,
                                        rssi = rssi,
                                        deviceClass = device.bluetoothClass?.deviceClass ?: 0,
                                        isBle = false,
                                    )
                                }
                            }

                            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                                runCatching { context.unregisterReceiver(this) }
                                if (cont.isActive) cont.resume(found.values.toList())
                            }
                        }
                    }
                }

                val filter = IntentFilter().apply {
                    addAction(BluetoothDevice.ACTION_FOUND)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                }
                context.registerReceiver(receiver, filter)

                cont.invokeOnCancellation {
                    runCatching { adapter.cancelDiscovery() }
                    runCatching { context.unregisterReceiver(receiver) }
                }

                if (!adapter.startDiscovery()) {
                    runCatching { context.unregisterReceiver(receiver) }
                    if (cont.isActive) cont.resume(emptyList())
                }
            }
        } ?: run {
            runCatching { adapter.cancelDiscovery() }
            found.values.toList()
        }
    }

    private companion object {
        const val DISCOVERY_TIMEOUT_MS = 15_000L
    }
}

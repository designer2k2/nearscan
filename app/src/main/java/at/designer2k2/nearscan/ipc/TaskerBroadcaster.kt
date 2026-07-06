package at.designer2k2.nearscan.ipc

import android.content.Context
import android.content.Intent
import at.designer2k2.nearscan.db.BtScanEntity
import at.designer2k2.nearscan.db.CellScanEntity
import at.designer2k2.nearscan.db.WifiScanEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends standard Android broadcast intents so automation apps (Tasker, MacroDroid, Automate)
 * can react to scan events without any proprietary plugin.
 *
 * Seen-sets are reset on each [reset] call (i.e. when a scan session starts), so NEW_* events
 * only fire the first time a BSSID / address / cell CID is seen in the current session.
 */
@Singleton
class TaskerBroadcaster @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val seenBssids: MutableSet<String> =
        Collections.newSetFromMap(ConcurrentHashMap())
    private val seenBtAddresses: MutableSet<String> =
        Collections.newSetFromMap(ConcurrentHashMap())
    private val seenBleAddresses: MutableSet<String> =
        Collections.newSetFromMap(ConcurrentHashMap())
    private val seenCellCids: MutableSet<Long> =
        Collections.newSetFromMap(ConcurrentHashMap())

    /** Clear all seen-sets; call this when a new scan session starts. */
    fun reset() {
        seenBssids.clear()
        seenBtAddresses.clear()
        seenBleAddresses.clear()
        seenCellCids.clear()
    }

    fun onScanStarted() = send(ACTION_SCAN_STARTED)

    fun onScanStopped(wifiTotal: Int, btTotal: Int, cellTotal: Int, durationS: Long) {
        send(ACTION_SCAN_STOPPED) {
            putExtra("wifi_total", wifiTotal)
            putExtra("bt_total", btTotal)
            putExtra("cell_total", cellTotal)
            putExtra("duration_s", durationS)
        }
    }

    fun onNewWifi(rows: List<WifiScanEntity>) {
        rows.filter { seenBssids.add(it.bssid) }.forEach { row ->
            send(ACTION_NEW_WIFI) {
                putExtra("ssid", row.ssid ?: "")
                putExtra("bssid", row.bssid)
                putExtra("rssi", row.rssi)
                putExtra("channel", row.channel)
                putExtra("lat", row.latitude ?: 0.0)
                putExtra("lon", row.longitude ?: 0.0)
            }
        }
    }

    /** [rows] must be BT Classic entries (isBle == false). */
    fun onNewBt(rows: List<BtScanEntity>) {
        rows.filter { seenBtAddresses.add(it.address) }.forEach { row ->
            send(ACTION_NEW_BT) {
                putExtra("address", row.address)
                putExtra("name", row.name ?: "")
                putExtra("rssi", row.rssi)
            }
        }
    }

    /** [rows] must be BLE entries (isBle == true). */
    fun onNewBle(rows: List<BtScanEntity>) {
        rows.filter { seenBleAddresses.add(it.address) }.forEach { row ->
            send(ACTION_NEW_BLE) {
                putExtra("address", row.address)
                putExtra("name", row.name ?: "")
                putExtra("rssi", row.rssi)
            }
        }
    }

    fun onNewCell(rows: List<CellScanEntity>) {
        rows.filter { r -> r.cid != null && seenCellCids.add(r.cid) }.forEach { row ->
            send(ACTION_NEW_CELL) {
                putExtra("mcc", row.mcc ?: -1)
                putExtra("mnc", row.mnc ?: -1)
                putExtra("cid", row.cid ?: -1L)
                putExtra("rssi", row.rssi)
                putExtra("tech", row.technology ?: "")
            }
        }
    }

    /**
     * Fires after each individual scan type's own cycle finishes with the latest cumulative
     * counts. Since WiFi/BT/BLE/Cell run on independent intervals, this fires once per
     * scan-type tick — not once per synchronized "round" across all four types (a true
     * synchronized round isn't meaningful when intervals differ, e.g. WiFi every 10s vs.
     * Cell every 60s).
     */
    fun onRoundComplete(wifiCount: Int, btCount: Int, cellCount: Int) {
        send(ACTION_ROUND_COMPLETE) {
            putExtra("wifi_count", wifiCount)
            putExtra("bt_count", btCount)
            putExtra("cell_count", cellCount)
            putExtra("timestamp", System.currentTimeMillis())
        }
    }

    fun onExportComplete(file: File, format: String, recordCount: Long) {
        send(ACTION_EXPORT_COMPLETE) {
            putExtra("file_path", file.absolutePath)
            putExtra("format", format)
            putExtra("record_count", recordCount)
        }
    }

    private fun send(action: String, block: Intent.() -> Unit = {}) {
        context.sendBroadcast(Intent(action).apply(block))
    }

    companion object {
        const val ACTION_SCAN_STARTED = "at.designer2k2.nearscan.SCAN_STARTED"
        const val ACTION_SCAN_STOPPED = "at.designer2k2.nearscan.SCAN_STOPPED"
        const val ACTION_NEW_WIFI = "at.designer2k2.nearscan.NEW_WIFI_FOUND"
        const val ACTION_NEW_BT = "at.designer2k2.nearscan.NEW_BT_FOUND"
        const val ACTION_NEW_BLE = "at.designer2k2.nearscan.NEW_BLE_FOUND"
        const val ACTION_NEW_CELL = "at.designer2k2.nearscan.NEW_CELL_FOUND"
        const val ACTION_ROUND_COMPLETE = "at.designer2k2.nearscan.ROUND_COMPLETE"
        const val ACTION_EXPORT_COMPLETE = "at.designer2k2.nearscan.EXPORT_COMPLETE"
    }
}

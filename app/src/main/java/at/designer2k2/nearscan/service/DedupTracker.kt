package at.designer2k2.nearscan.service

/**
 * Tracks the last logged RSSI per WiFi BSSID / BT address so [ScanService] can skip
 * near-duplicate observations when the user enables "Deduplicate" in Advanced Settings.
 *
 * Per spec: skip a record if the same key was logged with an RSSI within ±3 dBm; log it
 * (and update the tracked value) otherwise. Cell records are never deduplicated — cell info
 * changes are considered always significant. Pure Kotlin, no Android dependency, so it's
 * trivially unit-testable.
 */
class DedupTracker {
    private val lastWifiRssi = HashMap<String, Int>()
    private val lastBtRssi = HashMap<String, Int>()

    /** True if this WiFi observation should be logged (i.e. is not a near-duplicate). */
    fun shouldLogWifi(bssid: String, rssi: Int): Boolean = shouldLog(lastWifiRssi, bssid, rssi)

    /** True if this BT (Classic or BLE) observation should be logged. */
    fun shouldLogBt(address: String, rssi: Int): Boolean = shouldLog(lastBtRssi, address, rssi)

    private fun shouldLog(last: MutableMap<String, Int>, key: String, rssi: Int): Boolean {
        val previous = last[key]
        val changed = previous == null || kotlin.math.abs(previous - rssi) > RSSI_WINDOW
        if (changed) last[key] = rssi
        return changed
    }

    /** Clears tracked state; call when a new scan session starts. */
    fun reset() {
        lastWifiRssi.clear()
        lastBtRssi.clear()
    }

    private companion object {
        const val RSSI_WINDOW = 3
    }
}

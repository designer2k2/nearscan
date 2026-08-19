package at.designer2k2.nearscan.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wifi_scans")
data class WifiScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    val altitude: Double?,
    val ssid: String?,
    val bssid: String,
    val rssi: Int,
    val frequency: Int,
    val channel: Int,
    val capabilities: String?,
    val band: String?,
    // Optional self-logged fields (see prefs.NearScanSettings extra* toggles / extra.ExtraFields).
    // Null when the corresponding toggle is off or the value couldn't be read.
    val extraBatteryLevel: Int? = null,
    val extraBatteryCharging: Boolean? = null,
    val extraBatteryTemperature: Float? = null,
    val extraScreenOn: Boolean? = null,
    val extraMobileDataActive: Boolean? = null,
    val extraActiveNetworkType: String? = null,
    val extraConnectedSsid: String? = null,
    val extraHeading: Float? = null,
    val extraTilt: Float? = null,
    val extraScanDurationMs: Long? = null,
    val extraMemoryAvailableMb: Long? = null,
)

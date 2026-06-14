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
)

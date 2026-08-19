package at.designer2k2.nearscan.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bt_scans")
data class BtScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    val altitude: Double?,
    val address: String,
    val name: String?,
    val rssi: Int,
    val deviceClass: Int,
    val isBle: Boolean,
    // BLE advertising data (null for BT Classic, or when the capture setting is off): comma-
    // separated advertised service UUIDs, and "<companyId>:<hexPayload>" manufacturer-specific
    // data entries joined by ";". More reliable than name/COD for fingerprinting a device model
    // (e.g. smart glasses) since many peripherals only advertise a name during pairing mode.
    val serviceUuids: String? = null,
    val manufacturerData: String? = null,
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

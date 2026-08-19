package at.designer2k2.nearscan.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cell_scans")
data class CellScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val latitude: Double?,
    val longitude: Double?,
    val altitude: Double?,
    val mcc: Int?,
    val mnc: Int?,
    val lac: Int?,
    val cid: Long?,
    val rssi: Int,
    val technology: String?,
    // Whether this is the cell the phone is actually camped on, vs. a detected neighbor.
    val isRegistered: Boolean? = null,
    // Signal quality beyond the coarse rssi/dbm above. asuLevel/signalLevel are populated for
    // every technology; the rest are technology-specific and null where not applicable:
    // rsrp/rsrq/snr (LTE + NR, unified — NR's ssRsrp/ssRsrq/ssSinr map onto the same fields),
    // ecNo (WCDMA only), bitErrorRate/timingAdvance (GSM; timingAdvance also LTE).
    val asuLevel: Int? = null,
    val signalLevel: Int? = null,
    val rsrp: Int? = null,
    val rsrq: Int? = null,
    val snr: Int? = null,
    val ecNo: Int? = null,
    val bitErrorRate: Int? = null,
    val timingAdvance: Int? = null,
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

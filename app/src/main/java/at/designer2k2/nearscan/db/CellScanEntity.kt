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
)

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
)

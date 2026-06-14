package at.designer2k2.nearscan.scanner

import android.telephony.TelephonyManager
import at.designer2k2.nearscan.db.CellScanEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stub cell tower scanner. A full implementation would parse [TelephonyManager.getAllCellInfo]
 * into per-technology (GSM/LTE/NR) records with MCC/MNC/LAC/CID/dBm.
 */
@Singleton
class CellScanner @Inject constructor(
    private val telephonyManager: TelephonyManager?,
) {
    @Suppress("MissingPermission")
    suspend fun scan(): List<CellScanEntity> {
        val tm = telephonyManager ?: return emptyList()
        // TODO: read tm.allCellInfo and map each CellInfo subtype to CellScanEntity.
        @Suppress("UNUSED_EXPRESSION") tm
        return emptyList()
    }
}

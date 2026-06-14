package at.designer2k2.nearscan.scanner

import android.os.Build
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyManager
import at.designer2k2.nearscan.db.CellScanEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cell tower scanner. Parses [TelephonyManager.getAllCellInfo] into per-technology
 * (GSM / LTE / NR / WCDMA) [CellScanEntity] records.
 */
@Singleton
class CellScanner @Inject constructor(
    private val telephonyManager: TelephonyManager?,
) {
    @Suppress("MissingPermission")
    suspend fun scan(): List<CellScanEntity> = withContext(Dispatchers.IO) {
        val tm = telephonyManager ?: return@withContext emptyList()
        val now = System.currentTimeMillis()
        val cellInfo = tm.allCellInfo ?: return@withContext emptyList()

        cellInfo.mapNotNull { info -> info.toEntity(now) }
    }

    private fun CellInfo.toEntity(now: Long): CellScanEntity? = when (this) {
        is CellInfoGsm -> {
            val id = cellIdentity
            val signal = cellSignalStrength
            CellScanEntity(
                timestamp = now,
                latitude = null,
                longitude = null,
                altitude = null,
                mcc = id.mccString?.toIntOrNull(),
                mnc = id.mncString?.toIntOrNull(),
                lac = id.lac.sanitizeInt(),
                cid = id.cid.toLong().sanitizeLong(),
                rssi = signal.dbm,
                technology = "GSM",
            )
        }

        is CellInfoLte -> {
            val id = cellIdentity
            val signal = cellSignalStrength
            CellScanEntity(
                timestamp = now,
                latitude = null,
                longitude = null,
                altitude = null,
                mcc = id.mccString?.toIntOrNull(),
                mnc = id.mncString?.toIntOrNull(),
                lac = id.tac.sanitizeInt(),
                cid = id.ci.toLong().sanitizeLong(),
                rssi = signal.dbm,
                technology = "LTE",
            )
        }

        is CellInfoWcdma -> {
            val id = cellIdentity
            val signal = cellSignalStrength
            CellScanEntity(
                timestamp = now,
                latitude = null,
                longitude = null,
                altitude = null,
                mcc = id.mccString?.toIntOrNull(),
                mnc = id.mncString?.toIntOrNull(),
                lac = id.lac.sanitizeInt(),
                cid = id.cid.toLong().sanitizeLong(),
                rssi = signal.dbm,
                technology = "WCDMA",
            )
        }

        else -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && this is CellInfoNr) {
                val id = cellIdentity as CellIdentityNr
                val signal = cellSignalStrength as CellSignalStrengthNr
                CellScanEntity(
                    timestamp = now,
                    latitude = null,
                    longitude = null,
                    altitude = null,
                    mcc = id.mccString?.toIntOrNull(),
                    mnc = id.mncString?.toIntOrNull(),
                    lac = id.tac.sanitizeInt(),
                    cid = id.nci.sanitizeLong(),
                    rssi = signal.dbm,
                    technology = "NR",
                )
            } else {
                null
            }
        }
    }

    private fun Int.sanitizeInt(): Int = if (this == Int.MAX_VALUE) -1 else this

    private fun Long.sanitizeLong(): Long =
        if (this == Int.MAX_VALUE.toLong() || this == Long.MAX_VALUE) -1L else this
}

package at.designer2k2.nearscan.scanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrengthNr
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import at.designer2k2.nearscan.db.CellScanEntity
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
    private val telephonyManager: TelephonyManager?,
) {
    @Suppress("MissingPermission")
    suspend fun scan(): List<CellScanEntity> = withContext(Dispatchers.IO) {
        val tm = telephonyManager ?: return@withContext emptyList()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext emptyList()
        }
        val now = System.currentTimeMillis()
        val cellInfo = tm.allCellInfo ?: return@withContext emptyList()

        cellInfo.mapNotNull { info -> info.toEntity(now) }
    }

    // id.mcc / id.mnc (the legacy Int getters used as the pre-API-28 fallback below) are
    // deprecated in favor of the String getters, but are still the only way to read this
    // field on Android 8.x (minSdk 26) — see resolveMccOrMnc.
    @Suppress("DEPRECATION")
    private fun CellInfo.toEntity(now: Long): CellScanEntity? = when (this) {
        is CellInfoGsm -> {
            val id = cellIdentity
            val signal = cellSignalStrength
            CellScanEntity(
                timestamp = now,
                latitude = null,
                longitude = null,
                altitude = null,
                mcc = resolveMccOrMnc(
                    sdkInt = Build.VERSION.SDK_INT,
                    modernValue = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mccString else null,
                    legacyValue = id.mcc,
                ),
                mnc = resolveMccOrMnc(
                    sdkInt = Build.VERSION.SDK_INT,
                    modernValue = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mncString else null,
                    legacyValue = id.mnc,
                ),
                lac = id.lac.sanitizeInt(),
                cid = id.cid.toLong().sanitizeLong(),
                rssi = signal.dbm,
                technology = "GSM",
                isRegistered = isRegistered,
                asuLevel = signal.asuLevel.orNullIfUnavailable(),
                signalLevel = signal.level,
                bitErrorRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    signal.bitErrorRate.orNullIfUnavailable()
                } else {
                    null
                },
                timingAdvance = signal.timingAdvance.orNullIfUnavailable(),
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
                mcc = resolveMccOrMnc(
                    sdkInt = Build.VERSION.SDK_INT,
                    modernValue = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mccString else null,
                    legacyValue = id.mcc,
                ),
                mnc = resolveMccOrMnc(
                    sdkInt = Build.VERSION.SDK_INT,
                    modernValue = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mncString else null,
                    legacyValue = id.mnc,
                ),
                lac = id.tac.sanitizeInt(),
                cid = id.ci.toLong().sanitizeLong(),
                rssi = signal.dbm,
                technology = "LTE",
                isRegistered = isRegistered,
                asuLevel = signal.asuLevel.orNullIfUnavailable(),
                signalLevel = signal.level,
                rsrp = signal.rsrp.orNullIfUnavailable(),
                rsrq = signal.rsrq.orNullIfUnavailable(),
                snr = signal.rssnr.orNullIfUnavailable(),
                timingAdvance = signal.timingAdvance.orNullIfUnavailable(),
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
                mcc = resolveMccOrMnc(
                    sdkInt = Build.VERSION.SDK_INT,
                    modernValue = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mccString else null,
                    legacyValue = id.mcc,
                ),
                mnc = resolveMccOrMnc(
                    sdkInt = Build.VERSION.SDK_INT,
                    modernValue = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) id.mncString else null,
                    legacyValue = id.mnc,
                ),
                lac = id.lac.sanitizeInt(),
                cid = id.cid.toLong().sanitizeLong(),
                rssi = signal.dbm,
                technology = "WCDMA",
                isRegistered = isRegistered,
                asuLevel = signal.asuLevel.orNullIfUnavailable(),
                signalLevel = signal.level,
                ecNo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    signal.ecNo.orNullIfUnavailable()
                } else {
                    null
                },
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
                    isRegistered = isRegistered,
                    asuLevel = signal.asuLevel.orNullIfUnavailable(),
                    signalLevel = signal.level,
                    rsrp = signal.ssRsrp.orNullIfUnavailable(),
                    rsrq = signal.ssRsrq.orNullIfUnavailable(),
                    snr = signal.ssSinr.orNullIfUnavailable(),
                )
            } else {
                null
            }
        }
    }

    // The string-based MCC/MNC getters were added in API 28 (P); below that, this
    // technology's identity class only ever exposed the deprecated Int getter, which
    // every CellIdentity{Gsm,Lte,Wcdma} subclass has had since well before minSdk 26.
    // sdkInt is a parameter (rather than reading Build.VERSION.SDK_INT internally) so
    // this branch logic is unit-testable without Robolectric — see CellScannerTest.
    @Suppress("DEPRECATION")
    internal fun resolveMccOrMnc(sdkInt: Int, modernValue: String?, legacyValue: Int): Int? =
        if (sdkInt >= Build.VERSION_CODES.P) {
            modernValue?.toIntOrNull()
        } else {
            legacyValue.takeIf { it != Int.MAX_VALUE }
        }

    private fun Int.sanitizeInt(): Int = if (this == Int.MAX_VALUE) -1 else this

    private fun Long.sanitizeLong(): Long =
        if (this == Int.MAX_VALUE.toLong() || this == Long.MAX_VALUE) -1L else this

    // Signal-quality getters use Int.MAX_VALUE (== CellInfo.UNAVAILABLE) as their "not reported by
    // this device/technology" sentinel. Unlike lac/cid, -1 is a plausible real value for some of
    // these (dB/dBm measures), so map the sentinel to null instead of a magic number.
    private fun Int.orNullIfUnavailable(): Int? = if (this == Int.MAX_VALUE) null else this
}

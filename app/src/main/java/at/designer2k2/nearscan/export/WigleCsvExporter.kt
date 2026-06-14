package at.designer2k2.nearscan.export

import at.designer2k2.nearscan.db.WifiScanDao
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stub WiGLE-compatible CSV exporter. Writes the fixed WiGLE header followed by one row
 * per WiFi observation. Cell/BT rows would use WiGLE's network type column in a full impl.
 */
@Singleton
class WigleCsvExporter @Inject constructor(
    private val wifiScanDao: WifiScanDao,
) {
    suspend fun export(outputDir: File): File {
        if (!outputDir.exists()) outputDir.mkdirs()
        val file = File(outputDir, "nearscan_wigle_${System.currentTimeMillis()}.csv")
        val rows = wifiScanDao.getAll()
        file.bufferedWriter().use { w ->
            w.appendLine(PRE_HEADER)
            w.appendLine(COLUMN_HEADER)
            for (r in rows) {
                w.appendLine(
                    listOf(
                        r.bssid,
                        r.ssid ?: "",
                        r.capabilities ?: "",
                        r.timestamp.toString(),
                        r.channel.toString(),
                        r.rssi.toString(),
                        r.latitude?.toString() ?: "",
                        r.longitude?.toString() ?: "",
                        r.altitude?.toString() ?: "",
                        "", // accuracy
                        "WIFI",
                    ).joinToString(",")
                )
            }
        }
        return file
    }

    private companion object {
        const val PRE_HEADER =
            "WigleWifi-1.4,appRelease=0.1.0,model=NearScan,release=0.1.0,device=NearScan,display=NearScan,board=NearScan,brand=NearScan"
        const val COLUMN_HEADER =
            "MAC,SSID,AuthMode,FirstSeen,Channel,RSSI,CurrentLatitude,CurrentLongitude,AltitudeMeters,AccuracyMeters,Type"
    }
}

package at.designer2k2.nearscan.export

import at.designer2k2.nearscan.db.BtScanDao
import at.designer2k2.nearscan.db.CellScanDao
import at.designer2k2.nearscan.db.WifiScanDao
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WiGLE-compatible CSV exporter. Writes the fixed WiGLE header followed by one row per
 * observation across all three scan tables (WiFi/BT/Cell), using WiGLE's Type column to
 * distinguish them. Tables are read page-by-page so memory use stays bounded regardless of
 * how many rows a multi-day session has accumulated.
 */
@Singleton
class WigleCsvExporter @Inject constructor(
    private val wifiScanDao: WifiScanDao,
    private val btScanDao: BtScanDao,
    private val cellScanDao: CellScanDao,
) {
    suspend fun export(outputDir: File): File {
        if (!outputDir.exists()) outputDir.mkdirs()
        val file = File(outputDir, "nearscan_wigle_${System.currentTimeMillis()}.csv")

        file.bufferedWriter().use { w ->
            w.appendLine(PRE_HEADER)
            w.appendLine(COLUMN_HEADER)

            forEachPage({ limit, offset -> wifiScanDao.getPage(limit, offset) }) { r ->
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
                    ).joinToString(","),
                )
            }

            forEachPage({ limit, offset -> btScanDao.getPage(limit, offset) }) { r ->
                w.appendLine(
                    listOf(
                        r.address,
                        r.name ?: "",
                        "",
                        r.timestamp.toString(),
                        "",
                        r.rssi.toString(),
                        r.latitude?.toString() ?: "",
                        r.longitude?.toString() ?: "",
                        r.altitude?.toString() ?: "",
                        "", // accuracy
                        "BT",
                    ).joinToString(","),
                )
            }

            forEachPage({ limit, offset -> cellScanDao.getPage(limit, offset) }) { r ->
                w.appendLine(
                    listOf(
                        "",
                        "${r.mcc ?: ""}-${r.mnc ?: ""}-${r.cid ?: ""}",
                        "",
                        r.timestamp.toString(),
                        r.lac?.toString() ?: "",
                        r.rssi.toString(),
                        r.latitude?.toString() ?: "",
                        r.longitude?.toString() ?: "",
                        r.altitude?.toString() ?: "",
                        "", // accuracy
                        r.technology ?: "",
                    ).joinToString(","),
                )
            }
        }
        return file
    }

    private companion object {
        const val PAGE_SIZE = 2_000

        /** Pages through [fetchPage] until a short (or empty) page signals the end. */
        suspend fun <T> forEachPage(fetchPage: suspend (limit: Int, offset: Int) -> List<T>, action: (T) -> Unit) {
            var offset = 0
            while (true) {
                val page = fetchPage(PAGE_SIZE, offset)
                page.forEach(action)
                if (page.size < PAGE_SIZE) break
                offset += page.size
            }
        }

        const val PRE_HEADER =
            "WigleWifi-1.4,appRelease=0.1.0,model=NearScan,release=0.1.0,device=NearScan,display=NearScan,board=NearScan,brand=NearScan"
        const val COLUMN_HEADER =
            "MAC,SSID,AuthMode,FirstSeen,Channel,RSSI,CurrentLatitude,CurrentLongitude,AltitudeMeters,AccuracyMeters,Type"
    }
}

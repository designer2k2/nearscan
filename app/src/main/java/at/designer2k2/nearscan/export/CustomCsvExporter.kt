package at.designer2k2.nearscan.export

import at.designer2k2.nearscan.db.BtScanDao
import at.designer2k2.nearscan.db.CellScanDao
import at.designer2k2.nearscan.db.WifiScanDao
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Self-documenting CSV exporter that writes all three scan tables into a single file using a
 * superset header. Scan-type-specific columns are left empty for rows of other types.
 */
@Singleton
class CustomCsvExporter @Inject constructor(
    private val wifiScanDao: WifiScanDao,
    private val btScanDao: BtScanDao,
    private val cellScanDao: CellScanDao,
) {
    suspend fun export(outputDir: File): File {
        if (!outputDir.exists()) outputDir.mkdirs()
        val file = File(outputDir, "nearscan_custom_${System.currentTimeMillis()}.csv")

        val wifi = wifiScanDao.getAll()
        val bt = btScanDao.getAll()
        val cell = cellScanDao.getAll()

        file.bufferedWriter().use { w ->
            w.appendLine(HEADER)

            for (r in wifi) {
                w.appendLine(
                    listOf(
                        "WIFI",
                        r.timestamp.toString(),
                        r.latitude?.toString() ?: "",
                        r.longitude?.toString() ?: "",
                        r.altitude?.toString() ?: "",
                        // WiFi-specific
                        r.ssid.csvField(),
                        r.bssid.csvField(),
                        r.rssi.toString(),
                        r.frequency.toString(),
                        r.channel.toString(),
                        r.capabilities.csvField(),
                        (r.band ?: "").csvField(),
                        // BT-specific
                        "", "", "", "", "",
                        // Cell-specific
                        "", "", "", "", "", "",
                    ).joinToString(",")
                )
            }

            for (r in bt) {
                w.appendLine(
                    listOf(
                        "BT",
                        r.timestamp.toString(),
                        r.latitude?.toString() ?: "",
                        r.longitude?.toString() ?: "",
                        r.altitude?.toString() ?: "",
                        // WiFi-specific
                        "", "", "", "", "", "", "",
                        // BT-specific
                        r.address.csvField(),
                        r.name.csvField(),
                        r.rssi.toString(),
                        r.deviceClass.toString(),
                        r.isBle.toString(),
                        // Cell-specific
                        "", "", "", "", "", "",
                    ).joinToString(",")
                )
            }

            for (r in cell) {
                w.appendLine(
                    listOf(
                        "CELL",
                        r.timestamp.toString(),
                        r.latitude?.toString() ?: "",
                        r.longitude?.toString() ?: "",
                        r.altitude?.toString() ?: "",
                        // WiFi-specific
                        "", "", "", "", "", "", "",
                        // BT-specific
                        "", "", "", "", "",
                        // Cell-specific
                        r.mcc?.toString() ?: "",
                        r.mnc?.toString() ?: "",
                        r.lac?.toString() ?: "",
                        r.cid?.toString() ?: "",
                        r.rssi.toString(),
                        (r.technology ?: "").csvField(),
                    ).joinToString(",")
                )
            }
        }
        return file
    }

    private companion object {
        const val HEADER =
            "type,timestamp,latitude,longitude,altitude," +
                "ssid,bssid,wifi_rssi,frequency,channel,capabilities,band," +
                "bt_address,bt_name,bt_rssi,device_class,is_ble," +
                "mcc,mnc,lac,cid,cell_rssi,technology"
    }
}

/** Quotes a field for CSV output if it contains a comma, quote, or newline; escapes inner quotes. */
private fun String?.csvField(): String {
    val s = this ?: return ""
    return if (s.contains(',') || s.contains('"') || s.contains('\n') || s.contains('\r')) {
        "\"" + s.replace("\"", "\"\"") + "\""
    } else {
        s
    }
}

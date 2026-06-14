package at.designer2k2.nearscan.export

import at.designer2k2.nearscan.db.BtScanDao
import at.designer2k2.nearscan.db.CellScanDao
import at.designer2k2.nearscan.db.WifiScanDao
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exports all scan records as a GeoJSON FeatureCollection. Each record becomes a Point Feature
 * with its scan fields exposed as properties. JSON is assembled manually as the project has no
 * JSON serialization dependency.
 */
@Singleton
class GeoJsonExporter @Inject constructor(
    private val wifiScanDao: WifiScanDao,
    private val btScanDao: BtScanDao,
    private val cellScanDao: CellScanDao,
) {
    suspend fun export(outputDir: File): File {
        if (!outputDir.exists()) outputDir.mkdirs()
        val file = File(outputDir, "nearscan_geo_${System.currentTimeMillis()}.geojson")

        val wifi = wifiScanDao.getAll()
        val bt = btScanDao.getAll()
        val cell = cellScanDao.getAll()

        file.bufferedWriter().use { w ->
            w.append("{\"type\":\"FeatureCollection\",\"features\":[")
            var first = true

            for (r in wifi) {
                if (!first) w.append(",")
                first = false
                val props = StringBuilder()
                appendProp(props, "scan_type", "WIFI")
                appendProp(props, "id", r.id)
                appendProp(props, "timestamp", r.timestamp)
                appendProp(props, "ssid", r.ssid)
                appendProp(props, "bssid", r.bssid)
                appendProp(props, "rssi", r.rssi)
                appendProp(props, "frequency", r.frequency)
                appendProp(props, "channel", r.channel)
                appendProp(props, "capabilities", r.capabilities)
                appendProp(props, "band", r.band)
                w.append(feature(r.longitude, r.latitude, r.altitude, props.toString()))
            }

            for (r in bt) {
                if (!first) w.append(",")
                first = false
                val props = StringBuilder()
                appendProp(props, "scan_type", "BT")
                appendProp(props, "id", r.id)
                appendProp(props, "timestamp", r.timestamp)
                appendProp(props, "address", r.address)
                appendProp(props, "name", r.name)
                appendProp(props, "rssi", r.rssi)
                appendProp(props, "device_class", r.deviceClass)
                appendProp(props, "is_ble", r.isBle)
                w.append(feature(r.longitude, r.latitude, r.altitude, props.toString()))
            }

            for (r in cell) {
                if (!first) w.append(",")
                first = false
                val props = StringBuilder()
                appendProp(props, "scan_type", "CELL")
                appendProp(props, "id", r.id)
                appendProp(props, "timestamp", r.timestamp)
                appendProp(props, "mcc", r.mcc)
                appendProp(props, "mnc", r.mnc)
                appendProp(props, "lac", r.lac)
                appendProp(props, "cid", r.cid)
                appendProp(props, "rssi", r.rssi)
                appendProp(props, "technology", r.technology)
                w.append(feature(r.longitude, r.latitude, r.altitude, props.toString()))
            }

            w.append("]}")
        }
        return file
    }

    /** Builds a Point Feature. Null lon/lat produce a null coordinate array + has_location=false. */
    private fun feature(lon: Double?, lat: Double?, alt: Double?, props: String): String {
        val hasLocation = lon != null && lat != null
        val coords = if (hasLocation) {
            "[${lon},${lat},${alt ?: 0.0}]"
        } else {
            "null"
        }
        val locProp = ",\"has_location\":$hasLocation"
        return "{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":$coords}," +
            "\"properties\":{$props$locProp}}"
    }

    private companion object {
        fun appendProp(sb: StringBuilder, key: String, value: Any?) {
            if (sb.isNotEmpty()) sb.append(",")
            sb.append("\"").append(key).append("\":").append(jsonValue(value))
        }

        fun jsonValue(value: Any?): String = when (value) {
            null -> "null"
            is String -> "\"" + escape(value) + "\""
            is Boolean -> value.toString()
            is Int, is Long, is Double, is Float -> value.toString()
            else -> "\"" + escape(value.toString()) + "\""
        }

        fun escape(s: String): String {
            val sb = StringBuilder(s.length + 8)
            for (c in s) {
                when (c) {
                    '\\' -> sb.append("\\\\")
                    '"' -> sb.append("\\\"")
                    '\n' -> sb.append("\\n")
                    '\r' -> sb.append("\\r")
                    '\t' -> sb.append("\\t")
                    else -> if (c < ' ') sb.append("\\u%04x".format(c.code)) else sb.append(c)
                }
            }
            return sb.toString()
        }
    }
}

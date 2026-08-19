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
 * JSON serialization dependency. Tables are read page-by-page so memory use stays bounded
 * regardless of session length.
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

        file.bufferedWriter().use { w ->
            w.append("{\"type\":\"FeatureCollection\",\"features\":[")
            var first = true

            forEachPage({ limit, offset -> wifiScanDao.getPage(limit, offset) }) { r ->
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
                appendExtraProps(
                    props, r.extraBatteryLevel, r.extraBatteryCharging, r.extraBatteryTemperature,
                    r.extraScreenOn, r.extraMobileDataActive, r.extraActiveNetworkType, r.extraConnectedSsid,
                    r.extraHeading, r.extraTilt, r.extraScanDurationMs, r.extraMemoryAvailableMb,
                )
                w.append(feature(r.longitude, r.latitude, r.altitude, props.toString()))
            }

            forEachPage({ limit, offset -> btScanDao.getPage(limit, offset) }) { r ->
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
                appendProp(props, "service_uuids", r.serviceUuids)
                appendProp(props, "manufacturer_data", r.manufacturerData)
                appendExtraProps(
                    props, r.extraBatteryLevel, r.extraBatteryCharging, r.extraBatteryTemperature,
                    r.extraScreenOn, r.extraMobileDataActive, r.extraActiveNetworkType, r.extraConnectedSsid,
                    r.extraHeading, r.extraTilt, r.extraScanDurationMs, r.extraMemoryAvailableMb,
                )
                w.append(feature(r.longitude, r.latitude, r.altitude, props.toString()))
            }

            forEachPage({ limit, offset -> cellScanDao.getPage(limit, offset) }) { r ->
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
                appendProp(props, "is_registered", r.isRegistered)
                appendProp(props, "asu_level", r.asuLevel)
                appendProp(props, "signal_level", r.signalLevel)
                appendProp(props, "rsrp", r.rsrp)
                appendProp(props, "rsrq", r.rsrq)
                appendProp(props, "snr", r.snr)
                appendProp(props, "ec_no", r.ecNo)
                appendProp(props, "bit_error_rate", r.bitErrorRate)
                appendProp(props, "timing_advance", r.timingAdvance)
                appendExtraProps(
                    props, r.extraBatteryLevel, r.extraBatteryCharging, r.extraBatteryTemperature,
                    r.extraScreenOn, r.extraMobileDataActive, r.extraActiveNetworkType, r.extraConnectedSsid,
                    r.extraHeading, r.extraTilt, r.extraScanDurationMs, r.extraMemoryAvailableMb,
                )
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

        fun appendProp(sb: StringBuilder, key: String, value: Any?) {
            if (sb.isNotEmpty()) sb.append(",")
            sb.append("\"").append(key).append("\":").append(jsonValue(value))
        }

        /** Appends the optional self-logged extra fields, shared by all three feature types. */
        fun appendExtraProps(
            sb: StringBuilder,
            batteryLevel: Int?,
            batteryCharging: Boolean?,
            batteryTemperature: Float?,
            screenOn: Boolean?,
            mobileDataActive: Boolean?,
            activeNetworkType: String?,
            connectedSsid: String?,
            heading: Float?,
            tilt: Float?,
            scanDurationMs: Long?,
            memoryAvailableMb: Long?,
        ) {
            appendProp(sb, "battery_level", batteryLevel)
            appendProp(sb, "battery_charging", batteryCharging)
            appendProp(sb, "battery_temperature", batteryTemperature)
            appendProp(sb, "screen_on", screenOn)
            appendProp(sb, "mobile_data_active", mobileDataActive)
            appendProp(sb, "active_network_type", activeNetworkType)
            appendProp(sb, "connected_ssid", connectedSsid)
            appendProp(sb, "heading", heading)
            appendProp(sb, "tilt", tilt)
            appendProp(sb, "scan_duration_ms", scanDurationMs)
            appendProp(sb, "memory_available_mb", memoryAvailableMb)
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

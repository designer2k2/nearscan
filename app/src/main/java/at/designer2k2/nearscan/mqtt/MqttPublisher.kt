package at.designer2k2.nearscan.mqtt

import at.designer2k2.nearscan.db.BtScanEntity
import at.designer2k2.nearscan.db.CellScanEntity
import at.designer2k2.nearscan.db.WifiScanEntity
import at.designer2k2.nearscan.extra.ExtraFields
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Converts scan entities to a JSON payload and publishes them live via [MqttClient].
 * Coordinates fall back to the supplied extra values when the entity has none.
 */
@Singleton
class MqttPublisher @Inject constructor(
    private val mqttClient: MqttClient,
) {
    fun publish(
        topic: String,
        entity: Any,
        extraLat: Double?,
        extraLon: Double?,
        extraAlt: Double?,
        extras: ExtraFields? = null,
    ) {
        val payload = when (entity) {
            is WifiScanEntity -> {
                val lat = entity.latitude ?: extraLat
                val lon = entity.longitude ?: extraLon
                val alt = entity.altitude ?: extraAlt
                buildString {
                    append("{")
                    append(common("WIFI", entity.timestamp, lat, lon, alt))
                    append(",\"ssid\":").append(jsonStr(entity.ssid))
                    append(",\"bssid\":").append(jsonStr(entity.bssid))
                    append(",\"rssi\":").append(entity.rssi)
                    append(",\"frequency\":").append(entity.frequency)
                    append(",\"channel\":").append(entity.channel)
                    append(",\"capabilities\":").append(jsonStr(entity.capabilities))
                    append(",\"band\":").append(jsonStr(entity.band))
                    appendExtras(extras)
                    append("}")
                }
            }

            is BtScanEntity -> {
                val lat = entity.latitude ?: extraLat
                val lon = entity.longitude ?: extraLon
                val alt = entity.altitude ?: extraAlt
                buildString {
                    append("{")
                    append(common("BT", entity.timestamp, lat, lon, alt))
                    append(",\"address\":").append(jsonStr(entity.address))
                    append(",\"name\":").append(jsonStr(entity.name))
                    append(",\"rssi\":").append(entity.rssi)
                    append(",\"device_class\":").append(entity.deviceClass)
                    append(",\"is_ble\":").append(entity.isBle)
                    entity.serviceUuids?.let { append(",\"service_uuids\":").append(jsonStr(it)) }
                    entity.manufacturerData?.let { append(",\"manufacturer_data\":").append(jsonStr(it)) }
                    appendExtras(extras)
                    append("}")
                }
            }

            is CellScanEntity -> {
                val lat = entity.latitude ?: extraLat
                val lon = entity.longitude ?: extraLon
                val alt = entity.altitude ?: extraAlt
                buildString {
                    append("{")
                    append(common("CELL", entity.timestamp, lat, lon, alt))
                    append(",\"mcc\":").append(jsonNum(entity.mcc))
                    append(",\"mnc\":").append(jsonNum(entity.mnc))
                    append(",\"lac\":").append(jsonNum(entity.lac))
                    append(",\"cid\":").append(jsonNum(entity.cid))
                    append(",\"rssi\":").append(entity.rssi)
                    append(",\"technology\":").append(jsonStr(entity.technology))
                    appendExtras(extras)
                    append("}")
                }
            }

            else -> return
        }

        mqttClient.publish(topic, payload)
    }

    /** Appends each non-null extra field as a JSON key. No-op when [extras] is null. */
    private fun StringBuilder.appendExtras(extras: ExtraFields?) {
        if (extras == null) return
        extras.batteryLevel?.let { append(",\"battery_level\":").append(it) }
        extras.batteryCharging?.let { append(",\"battery_charging\":").append(it) }
        extras.batteryTemperature?.let { append(",\"battery_temperature\":").append(it) }
        extras.screenOn?.let { append(",\"screen_on\":").append(it) }
        extras.mobileDataActive?.let { append(",\"mobile_data_active\":").append(it) }
        extras.activeNetworkType?.let { append(",\"active_network_type\":").append(jsonStr(it)) }
        extras.connectedSsid?.let { append(",\"connected_ssid\":").append(jsonStr(it)) }
        extras.heading?.let { append(",\"heading\":").append(it) }
        extras.tilt?.let { append(",\"tilt\":").append(it) }
        extras.scanDurationMs?.let { append(",\"scan_duration_ms\":").append(it) }
        extras.memoryAvailableMb?.let { append(",\"memory_available_mb\":").append(it) }
    }

    private fun common(type: String, timestamp: Long, lat: Double?, lon: Double?, alt: Double?): String =
        "\"type\":\"$type\"" +
            ",\"timestamp\":$timestamp" +
            ",\"lat\":${jsonNum(lat)}" +
            ",\"lon\":${jsonNum(lon)}" +
            ",\"alt\":${jsonNum(alt)}"

    private fun jsonNum(value: Number?): String = value?.toString() ?: "null"

    private fun jsonStr(value: String?): String {
        if (value == null) return "null"
        val sb = StringBuilder(value.length + 2)
        sb.append('"')
        for (c in value) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c < ' ') sb.append("\\u%04x".format(c.code)) else sb.append(c)
            }
        }
        sb.append('"')
        return sb.toString()
    }
}

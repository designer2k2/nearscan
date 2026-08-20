package at.designer2k2.nearscan.prefs

/**
 * Export format options, persisted as the string in [ExportFormat.key]. [ExportManager] gzips
 * every exported file regardless of format (they're all text or a growable SQLite dump, and
 * compress well), so [mimeType] reflects the gzip wrapper actually shared/attached, not the
 * uncompressed content type.
 */
enum class ExportFormat(val key: String, val label: String, val mimeType: String) {
    WIGLE_CSV("wigle_csv", "WiGLE CSV", "application/gzip"),
    CUSTOM_CSV("custom_csv", "Custom CSV", "application/gzip"),
    GEOJSON("geojson", "GeoJSON", "application/gzip"),
    SQLITE("sqlite", "SQLite dump", "application/gzip");

    companion object {
        fun fromKey(key: String?): ExportFormat =
            entries.firstOrNull { it.key == key } ?: WIGLE_CSV
    }
}

/**
 * Immutable snapshot of all NearScan settings. Mirrors the SharedPreferences key set
 * from the spec, but persisted via DataStore.
 */
data class NearScanSettings(
    // Location
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,

    // Scan type enable flags
    val scanWifiEnabled: Boolean = true,
    val scanBtEnabled: Boolean = true,
    val scanBleEnabled: Boolean = true,
    val scanCellEnabled: Boolean = true,

    // Intervals (seconds)
    val intervalWifiSec: Int = 10,
    val intervalBtSec: Int = 30,
    val intervalBleSec: Int = 15,
    val intervalCellSec: Int = 60,

    // WiFi filtering
    val wifiMinRssi: Int = -90,
    val wifiBands: Set<String> = setOf("2.4", "5", "6"),

    // Export
    val exportFormat: ExportFormat = ExportFormat.WIGLE_CSV,
    val outputFolder: String = "",
    val autoExportIntervalMin: Int = 0,

    // MQTT
    val mqttEnabled: Boolean = false,
    val mqttBroker: String = "",
    val mqttTopic: String = "nearscan/data",

    // Misc
    val keepScreenOn: Boolean = false,
    val dedupEnabled: Boolean = false,
    val batteryOptPromptShown: Boolean = false,

    // BLE advertising data (service UUIDs + manufacturer-specific data) capture, used to
    // fingerprint device models (e.g. wearables) that don't broadcast a recognizable name.
    val captureBleAdvertisingData: Boolean = true,

    // Extra logged fields
    val extraBatteryLevel: Boolean = false,
    val extraBatteryCharging: Boolean = false,
    val extraBatteryTemp: Boolean = false,
    val extraScreenOn: Boolean = false,
    val extraMobileData: Boolean = false,
    val extraNetworkType: Boolean = false,
    val extraConnectedSsid: Boolean = false,
    val extraHeading: Boolean = false,
    val extraTilt: Boolean = false,
    val extraScanDuration: Boolean = false,
    val extraMemory: Boolean = false,
) {
    /** True when at least one optional extra logged field is enabled. */
    val anyExtraFieldEnabled: Boolean
        get() = extraBatteryLevel || extraBatteryCharging || extraBatteryTemp ||
            extraScreenOn || extraMobileData || extraNetworkType || extraConnectedSsid ||
            extraHeading || extraTilt || extraScanDuration || extraMemory
}

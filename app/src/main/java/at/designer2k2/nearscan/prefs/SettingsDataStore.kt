package at.designer2k2.nearscan.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "nearscan_settings")

/** DataStore-backed wrapper exposing [NearScanSettings] as a [Flow] plus update helpers. */
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val LAT = doublePreferencesKey("nearscan_lat")
        val LON = doublePreferencesKey("nearscan_lon")
        val ALT = doublePreferencesKey("nearscan_alt")

        val SCAN_WIFI = booleanPreferencesKey("scan_wifi_enabled")
        val SCAN_BT = booleanPreferencesKey("scan_bt_enabled")
        val SCAN_BLE = booleanPreferencesKey("scan_ble_enabled")
        val SCAN_CELL = booleanPreferencesKey("scan_cell_enabled")

        val INT_WIFI = intPreferencesKey("interval_wifi_sec")
        val INT_BT = intPreferencesKey("interval_bt_sec")
        val INT_BLE = intPreferencesKey("interval_ble_sec")
        val INT_CELL = intPreferencesKey("interval_cell_sec")

        val WIFI_MIN_RSSI = intPreferencesKey("wifi_min_rssi")
        val WIFI_BANDS = stringSetPreferencesKey("wifi_bands")

        val EXPORT_FORMAT = stringPreferencesKey("export_format")
        val OUTPUT_FOLDER = stringPreferencesKey("output_folder")
        val AUTO_EXPORT = intPreferencesKey("auto_export_interval_min")

        val MQTT_ENABLED = booleanPreferencesKey("mqtt_enabled")
        val MQTT_BROKER = stringPreferencesKey("mqtt_broker")
        val MQTT_TOPIC = stringPreferencesKey("mqtt_topic")

        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val DEDUP = booleanPreferencesKey("dedup_enabled")
        val BATTERY_OPT_PROMPT_SHOWN = booleanPreferencesKey("battery_opt_prompt_shown")
        val CAPTURE_BLE_ADV_DATA = booleanPreferencesKey("capture_ble_advertising_data")

        // Deliberately outside NearScanSettings/update(): this tracks whether a scan session is
        // still logically active across process death, not a user-facing setting, so it must
        // never be clobbered by an unrelated settings save.
        val SESSION_ACTIVE = booleanPreferencesKey("session_active")
        val SESSION_START_MS = longPreferencesKey("session_start_ms")

        val EX_BATT_LEVEL = booleanPreferencesKey("extra_battery_level")
        val EX_BATT_CHARGING = booleanPreferencesKey("extra_battery_charging")
        val EX_BATT_TEMP = booleanPreferencesKey("extra_battery_temp")
        val EX_SCREEN_ON = booleanPreferencesKey("extra_screen_on")
        val EX_MOBILE_DATA = booleanPreferencesKey("extra_mobile_data")
        val EX_NETWORK_TYPE = booleanPreferencesKey("extra_network_type")
        val EX_CONNECTED_SSID = booleanPreferencesKey("extra_connected_ssid")
        val EX_HEADING = booleanPreferencesKey("extra_heading")
        val EX_TILT = booleanPreferencesKey("extra_tilt")
        val EX_SCAN_DURATION = booleanPreferencesKey("extra_scan_duration")
        val EX_MEMORY = booleanPreferencesKey("extra_memory")
    }

    val settings: Flow<NearScanSettings> = context.dataStore.data.map { p ->
        val defaults = NearScanSettings()
        NearScanSettings(
            latitude = p[Keys.LAT],
            longitude = p[Keys.LON],
            altitude = p[Keys.ALT],
            scanWifiEnabled = p[Keys.SCAN_WIFI] ?: defaults.scanWifiEnabled,
            scanBtEnabled = p[Keys.SCAN_BT] ?: defaults.scanBtEnabled,
            scanBleEnabled = p[Keys.SCAN_BLE] ?: defaults.scanBleEnabled,
            scanCellEnabled = p[Keys.SCAN_CELL] ?: defaults.scanCellEnabled,
            intervalWifiSec = p[Keys.INT_WIFI] ?: defaults.intervalWifiSec,
            intervalBtSec = p[Keys.INT_BT] ?: defaults.intervalBtSec,
            intervalBleSec = p[Keys.INT_BLE] ?: defaults.intervalBleSec,
            intervalCellSec = p[Keys.INT_CELL] ?: defaults.intervalCellSec,
            wifiMinRssi = p[Keys.WIFI_MIN_RSSI] ?: defaults.wifiMinRssi,
            wifiBands = p[Keys.WIFI_BANDS] ?: defaults.wifiBands,
            exportFormat = ExportFormat.fromKey(p[Keys.EXPORT_FORMAT]),
            outputFolder = p[Keys.OUTPUT_FOLDER] ?: defaults.outputFolder,
            autoExportIntervalMin = p[Keys.AUTO_EXPORT] ?: defaults.autoExportIntervalMin,
            mqttEnabled = p[Keys.MQTT_ENABLED] ?: defaults.mqttEnabled,
            mqttBroker = p[Keys.MQTT_BROKER] ?: defaults.mqttBroker,
            mqttTopic = p[Keys.MQTT_TOPIC] ?: defaults.mqttTopic,
            keepScreenOn = p[Keys.KEEP_SCREEN_ON] ?: defaults.keepScreenOn,
            dedupEnabled = p[Keys.DEDUP] ?: defaults.dedupEnabled,
            batteryOptPromptShown = p[Keys.BATTERY_OPT_PROMPT_SHOWN] ?: defaults.batteryOptPromptShown,
            captureBleAdvertisingData = p[Keys.CAPTURE_BLE_ADV_DATA] ?: defaults.captureBleAdvertisingData,
            extraBatteryLevel = p[Keys.EX_BATT_LEVEL] ?: defaults.extraBatteryLevel,
            extraBatteryCharging = p[Keys.EX_BATT_CHARGING] ?: defaults.extraBatteryCharging,
            extraBatteryTemp = p[Keys.EX_BATT_TEMP] ?: defaults.extraBatteryTemp,
            extraScreenOn = p[Keys.EX_SCREEN_ON] ?: defaults.extraScreenOn,
            extraMobileData = p[Keys.EX_MOBILE_DATA] ?: defaults.extraMobileData,
            extraNetworkType = p[Keys.EX_NETWORK_TYPE] ?: defaults.extraNetworkType,
            extraConnectedSsid = p[Keys.EX_CONNECTED_SSID] ?: defaults.extraConnectedSsid,
            extraHeading = p[Keys.EX_HEADING] ?: defaults.extraHeading,
            extraTilt = p[Keys.EX_TILT] ?: defaults.extraTilt,
            extraScanDuration = p[Keys.EX_SCAN_DURATION] ?: defaults.extraScanDuration,
            extraMemory = p[Keys.EX_MEMORY] ?: defaults.extraMemory,
        )
    }

    suspend fun updateLocation(lat: Double, lon: Double, alt: Double) {
        context.dataStore.edit {
            it[Keys.LAT] = lat
            it[Keys.LON] = lon
            it[Keys.ALT] = alt
        }
    }

    /** Marks the one-time battery optimization exemption prompt as shown. */
    suspend fun markBatteryOptPromptShown() {
        context.dataStore.edit { it[Keys.BATTERY_OPT_PROMPT_SHOWN] = true }
    }

    /**
     * Whether a scan session is still logically active (started but not yet cleanly stopped) and
     * the timestamp it started at. Read by [at.designer2k2.nearscan.service.ScanService] on start
     * to tell an OS-triggered `START_STICKY` restart (should resume the same session) apart from
     * a genuine fresh start (should begin a new one).
     */
    val sessionActive: Flow<Pair<Boolean, Long>> = context.dataStore.data.map { p ->
        (p[Keys.SESSION_ACTIVE] ?: false) to (p[Keys.SESSION_START_MS] ?: 0L)
    }

    suspend fun markSessionActive(startMs: Long) {
        context.dataStore.edit {
            it[Keys.SESSION_ACTIVE] = true
            it[Keys.SESSION_START_MS] = startMs
        }
    }

    suspend fun markSessionInactive() {
        context.dataStore.edit { it[Keys.SESSION_ACTIVE] = false }
    }

    /** Persists every field of [settings]. */
    suspend fun update(settings: NearScanSettings) {
        context.dataStore.edit { p ->
            settings.latitude?.let { p[Keys.LAT] = it } ?: p.remove(Keys.LAT)
            settings.longitude?.let { p[Keys.LON] = it } ?: p.remove(Keys.LON)
            settings.altitude?.let { p[Keys.ALT] = it } ?: p.remove(Keys.ALT)
            p[Keys.SCAN_WIFI] = settings.scanWifiEnabled
            p[Keys.SCAN_BT] = settings.scanBtEnabled
            p[Keys.SCAN_BLE] = settings.scanBleEnabled
            p[Keys.SCAN_CELL] = settings.scanCellEnabled
            p[Keys.INT_WIFI] = settings.intervalWifiSec
            p[Keys.INT_BT] = settings.intervalBtSec
            p[Keys.INT_BLE] = settings.intervalBleSec
            p[Keys.INT_CELL] = settings.intervalCellSec
            p[Keys.WIFI_MIN_RSSI] = settings.wifiMinRssi
            p[Keys.WIFI_BANDS] = settings.wifiBands
            p[Keys.EXPORT_FORMAT] = settings.exportFormat.key
            p[Keys.OUTPUT_FOLDER] = settings.outputFolder
            p[Keys.AUTO_EXPORT] = settings.autoExportIntervalMin
            p[Keys.MQTT_ENABLED] = settings.mqttEnabled
            p[Keys.MQTT_BROKER] = settings.mqttBroker
            p[Keys.MQTT_TOPIC] = settings.mqttTopic
            p[Keys.KEEP_SCREEN_ON] = settings.keepScreenOn
            p[Keys.DEDUP] = settings.dedupEnabled
            p[Keys.BATTERY_OPT_PROMPT_SHOWN] = settings.batteryOptPromptShown
            p[Keys.CAPTURE_BLE_ADV_DATA] = settings.captureBleAdvertisingData
            p[Keys.EX_BATT_LEVEL] = settings.extraBatteryLevel
            p[Keys.EX_BATT_CHARGING] = settings.extraBatteryCharging
            p[Keys.EX_BATT_TEMP] = settings.extraBatteryTemp
            p[Keys.EX_SCREEN_ON] = settings.extraScreenOn
            p[Keys.EX_MOBILE_DATA] = settings.extraMobileData
            p[Keys.EX_NETWORK_TYPE] = settings.extraNetworkType
            p[Keys.EX_CONNECTED_SSID] = settings.extraConnectedSsid
            p[Keys.EX_HEADING] = settings.extraHeading
            p[Keys.EX_TILT] = settings.extraTilt
            p[Keys.EX_SCAN_DURATION] = settings.extraScanDuration
            p[Keys.EX_MEMORY] = settings.extraMemory
        }
    }
}

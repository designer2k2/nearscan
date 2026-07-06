# NearScan — Android RF Environment Logger
## CLAUDE.md — Full Project Specification

---

## Project Overview

**NearScan** is a stationary RF environment logger for Android. It scans nearby WiFi networks, Bluetooth devices, and cell towers, logging everything to a structured file. It is designed for long-running stationary sessions (hours/days) without GPS drain.

It is **not affiliated with WiGLE** but exports a WiGLE-compatible CSV by default, as that is a well-known open format. Additional export formats are supported.

**Target user:** Tech-savvy users who want to log the RF environment around a fixed location — at home, at a monitoring point, or outdoors with a power bank.

**Key design principle:** Simple UI for casual use, hidden advanced section for power users.

---

## App Name & Identity

- **App name:** NearScan
- **Package name:** `at.designer2k2.nearscan`
- **Developer:** designer2k2 (Stephan)
- **Repository:** github.com/designer2k2/nearscan
- **Language:** Kotlin
- **Min SDK:** 26 (Android 8.0)
- **Target SDK / Compile SDK:** 35
- **Kotlin:** 2.0.21 (uses `org.jetbrains.kotlin.plugin.compose` — NOT legacy `composeOptions`)
- **AGP:** 8.6.1 · **KSP:** 2.0.21-1.0.28 · **Hilt:** 2.51.1 (KSP — kapt is incompatible with Kotlin 2.x)
- **Gradle:** 8.7 (AGP 8.6.x requires Gradle 8.7+; bumped from 8.5 alongside the AGP upgrade)
- Kotlin/KSP/Hilt/Room/Paho versions are deliberately pinned — they're a matched set (KSP version
  is tied to the exact Kotlin patch; Hilt/Room bumps risk destabilizing without a reason forcing
  the change). Only AGP + plain library versions (Compose BOM, Lifecycle, DataStore, coroutines,
  core-ktx, test libs) were bumped — see Dependencies section.

---

## Core Features

### Scan Types (all toggleable independently)
1. **WiFi** — SSID, BSSID, RSSI, frequency/channel, capabilities (security), band (2.4/5/6 GHz)
2. **Bluetooth Classic** — address, name, RSSI, device class (COD)
3. **Bluetooth LE** — address, name, RSSI, advertised services
4. **Cell Towers** — MCC, MNC, LAC/TAC, CID, signal (dBm), technology (GSM/LTE/NR/5G)

### Location Input
- **Manual coordinate entry** — lat/lon/altitude text fields, set once
- **Single GPS fix button** — acquires one GPS fix, then immediately disables GPS
- **No continuous GPS** — this is the entire point; GPS radio stays off during scanning
- Fixed coordinates are embedded in all logged records

### Scan Intervals
- Each scan type has its own configurable interval
- Default intervals:
  - WiFi: 10 seconds
  - BT Classic: 30 seconds
  - BT LE: 15 seconds
  - Cell: 60 seconds
- Range: 1 second to 300 seconds per type (UI slider: `1f..300f`)
- Implemented via coroutine loops (`while (scope.isActive) { block(); delay(intervalMs) }`) inside a foreground Service
- **Live-reactive**: toggling a scan type or dragging its interval slider takes effect immediately
  mid-session — no stop/restart needed. Each type is supervised by `ScanService.superviseScanType()`,
  which watches `(enabled, intervalSec)` from the live settings `Flow` and cancels/relaunches the
  inner loop job whenever either changes. The same live-reactivity applies to the Tasker
  `CMD_SET_INTERVAL` command.

### Foreground Service
- Runs as Android Foreground Service with persistent notification
- Notification shows: status (running/idle), counts, current interval
- Survives screen-off; requests exemption from battery optimization / Doze on first launch via
  `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (`MainActivity.requestBatteryOptimizationExemption()`),
  gated on a persisted `batteryOptPromptShown` flag so it only ever prompts once
- Does NOT use WorkManager — continuous scanning requires a real foreground service

---

## Data Storage

### Primary Storage: Room Database
- Store all scan results in SQLite via Room
- Separate tables per scan type:
  - `wifi_scans`
  - `bt_scans`
  - `cell_scans`
- Common fields per record:
  - `id` (auto-increment)
  - `timestamp` (epoch ms)
  - `latitude`, `longitude`, `altitude`
  - scan-type-specific fields
  - optional extra fields (see Extra Fields below)

### Extra Logged Fields (optional, toggleable per field)
When enabled, these are **collected at scan time and included in MQTT payloads and Custom CSV export**.
They are **NOT stored as columns in the Room database** (DB entities only hold RF + location fields).
They do NOT appear in WiGLE CSV export (fixed schema).

**Device State:**
- `battery_level` — integer percent (0–100)
- `battery_charging` — boolean (charging or not)
- `battery_temperature` — float °C
- `screen_on` — boolean

**Network State:**
- `mobile_data_active` — boolean
- `active_network_type` — string (WIFI / LTE / NR / NONE)
- `connected_ssid` — string (own WiFi SSID if connected)

**Sensors:**
- `heading` — float degrees (compass, if sensor available)
- `tilt` — float degrees (accelerometer-derived)

**System:**
- `scan_duration_ms` — how long the scan round took
- `memory_available_mb` — available RAM

---

## UI Layout

### Main Screen (Simple — always visible)

```
┌─────────────────────────────────┐
│  NearScan                 📤 ⚙️  │
├─────────────────────────────────┤
│                                 │
│  📍 47.2692° N, 11.4041° E     │
│     [Set Location]              │
│                                 │
│  ┌─────────────────────────┐   │
│  │      ▶ START            │   │
│  └─────────────────────────┘   │
│                                 │
│  WiFi      BT       Cell        │
│   142      38        4          │
│  found    found    found        │
│                                 │
│  Session: 00:42:17              │
│  Logged: 1,847 records          │
│                                 │
├─────────────────────────────────┤
│  Advanced Settings          ▼   │  ← tap to expand
└─────────────────────────────────┘
```

- START/STOP button is the dominant element
- Location shown with "Set Location" button to open coordinate dialog
- Live counters per scan type (total unique found this session)
- Session timer and total record count
- "Advanced Settings" collapsible card at the bottom
- The ⚙️ top-bar icon toggles/scrolls to the Advanced Settings card (its `expanded` state is
  hoisted up to `MainScreen`, not owned internally by the card) — there is no separate Settings
  screen
- The 📤 top-bar icon (left of the gear) triggers `MainViewModel.exportNow()` directly, one tap
  from the main screen — added so export isn't only reachable by first expanding Advanced
  Settings. Shows a small `CircularProgressIndicator` in place of the icon while `isExporting`.
  Uses whichever export format is currently selected in Advanced Settings; the format dropdown
  and a second **Export Now** button remain there too for users who want to pick a format first.
- **Missing-permission banner** ✅ implemented — an `errorContainer`-styled card appears above the
  location row (pushing everything else down, not overlaid) whenever any of
  `util/RequiredPermissions.forScanning()` (`ACCESS_FINE_LOCATION`, `BLUETOOTH_SCAN`/`_CONNECT` on
  API 31+, `READ_PHONE_STATE`) is denied — re-checked on every `Lifecycle.Event.ON_RESUME` so
  returning from the system permission dialog or Settings updates it live. Names exactly which
  scan types are affected and has an "Open Settings" button
  (`ACTION_APPLICATION_DETAILS_SETTINGS`). Fixes a prior gap where denying a permission left the
  counters silently frozen at 0 forever with no explanation.

### Set Location Dialog
- Lat / Lon / Altitude text fields (decimal degrees)
- "Get GPS Fix" button — acquires single fix, populates fields, stops GPS
- "Save" / "Cancel"
- Shows accuracy of last GPS fix ✅ implemented — `location.GpsFix` carries `accuracyMeters` from
  `Location.hasAccuracy()`/`.accuracy`, displayed below the GPS fix button
- Save shows an inline validation error (`location_invalid` string) instead of silently doing
  nothing when lat/lon can't be parsed

### Advanced Settings (collapsible card, collapsed by default)

**Scan Types & Intervals**
```
[ ✓ ] WiFi           [──●────────] 10s   (1-60s)
[ ✓ ] BT Classic     [────●──────] 30s   (1-60s)
[ ✓ ] BT LE          [───●───────] 15s   (1-60s)
[ ✓ ] Cell Towers    [───────●───] 60s   (1-300s)
```
WiFi/BT/BLE sliders cap at 60s — long wireless-scan intervals have little value and waste battery.
Cell towers change slowly enough to keep a 300s ceiling.

**Output**
- Export format: `[WiGLE CSV ▼]` (WiGLE CSV / Custom CSV / GeoJSON / SQLite dump) ✅ implemented
- **Export Now** button ✅ implemented — triggers `MainViewModel.exportNow()` directly (no Tasker
  required). On success, opens the Android share sheet (`ACTION_SEND` chooser) with the exported
  file attached via a `FileProvider` (authority `at.designer2k2.nearscan.fileprovider`, paths
  declared in `res/xml/file_paths.xml`) — lets the user immediately share the export by email,
  messenger, etc., matching `ExportFormat.mimeType` (`text/csv` / `application/geo+json` /
  `application/vnd.sqlite3`). Falls back to a Snackbar if the share sheet can't be launched (e.g.
  a user-typed `outputFolder` path outside the declared FileProvider paths) or on export failure.
  This is the only in-app export trigger besides the Tasker `CMD_EXPORT` broadcast (which does not
  share — it's meant for headless automation).
- MQTT: [ ] Enable → broker / topic fields appear ✅ implemented; connects/disconnects live —
  enabling mid-session actually connects instead of silently no-op'ing
- Keep screen on while running: [ ] toggle ✅ implemented
- Deduplicate: [ ] toggle ✅ implemented in UI **and** in `ScanService` (see Deduplication below)
- **WiFi min RSSI** ✅ implemented — slider (-100 to -30 dBm), enforced in `ScanService`'s WiFi
  supervision block (`it.rssi >= currentSettings.wifiMinRssi`). Previously the `wifiMinRssi`
  DataStore key was persisted but never read anywhere — a code-review finding fixed together with
  the UI so the setting isn't reachable only via direct DataStore manipulation.
- **WiFi bands** ✅ implemented — 2.4/5/6 GHz `FilterChip`s, enforced alongside the RSSI filter
  (`it.band in currentSettings.wifiBands`). The UI refuses to let you deselect the last remaining
  band, since an empty set would silently drop every WiFi result.
- **Auto-export interval** ✅ implemented — slider (0-120 min, 0 = off), supervised the same
  reactive way as the scan-type intervals (`ScanService`'s `superviseScanType`, minutes converted
  to seconds for the shared `loop()` helper). Uses `ExportManager.resolveOutputDir()` /
  `exportFormat` from current settings and fires a `TaskerBroadcaster.onExportComplete` on success,
  same as `CMD_EXPORT`.

**Not yet in UI (settings exist in DataStore, UI controls not yet built):**
- BT device class filter
- Output folder picker — `outputFolder` key persisted (settable via Tasker `CMD_EXPORT`'s
  implicit use of it, or direct DataStore manipulation, but no in-app text field yet)
- Extra Logged Fields checkboxes (battery, screen, network, sensors, memory) — all 11 keys persisted, `ExtraFieldsCollector` fully implemented; just no UI controls yet

---

## Export Formats

### 1. WiGLE CSV (default)
Standard WiGLE format. Fixed schema — extra fields not included.

Header line 1 (app info):
```
WigleWifi-1.4,appRelease=1.0,model=NearScan,release=1.0,device=NearScan,display=NearScan,board=NearScan,brand=designer2k2
```

Header line 2 (columns):
```
MAC,SSID,AuthMode,FirstSeen,Channel,RSSI,CurrentLatitude,CurrentLongitude,AltitudeMeters,AccuracyMeters,Type
```

Type values: `WIFI` / `BT` (Classic + BLE) / `GSM` / `LTE` / `WCDMA` / `NR` (cell rows use the
entity's `technology` field directly as Type)

### 2. Custom CSV
All fields including extra logged fields. Schema is self-documenting (header row describes all columns present).

### 3. GeoJSON
FeatureCollection of Points. Each network/device is a Feature with coordinates and all scan properties as feature properties. Suitable for QGIS, Leaflet, Mapbox.

### 4. SQLite Dump
Direct export of the Room database file. Full fidelity, queryable with any SQLite tool.

### Streaming / memory bound (all file exporters)
WiGLE CSV, Custom CSV, and GeoJSON all read each table **page-by-page** (`ScanDao.getPage(limit, offset)`,
2,000 rows/page) rather than loading the full table into memory via `getAll()`. This keeps memory
use bounded regardless of how many rows a multi-day session has accumulated. `getAll()` is kept on
each DAO only because `NearScanContentProvider` still uses it for live Tasker queries.

### 5. MQTT (live, not a file export)
Publishes JSON payload per scan result to configured broker/topic.
Payload example:
```json
{
  "type": "WIFI",
  "timestamp": 1718352000000,
  "ssid": "MyNetwork",
  "bssid": "AA:BB:CC:DD:EE:FF",
  "rssi": -65,
  "channel": 6,
  "lat": 47.2692,
  "lon": 11.4041,
  "alt": 574.0,
  "battery_level": 87
}
```

---

## Project Structure

```
nearscan/
├── app/
│   ├── robo-script.json                     # Firebase Robo Test script
│   └── src/main/
│       ├── java/at/designer2k2/nearscan/
│       │   ├── NearScanApplication.kt       # @HiltAndroidApp entry point
│       │   ├── MainActivity.kt              # Sets Compose content, requests permissions
│       │   ├── di/
│       │   │   └── AppModule.kt             # Hilt @Singleton providers (DB, DAOs, system services)
│       │   ├── service/
│       │   │   ├── ScanService.kt           # Foreground service; per-type reactive supervisors
│       │   │   └── DedupTracker.kt          # Pure-Kotlin ±3dBm dedup logic (unit-tested directly)
│       │   ├── scanner/
│       │   │   ├── WifiScanner.kt           # WifiManager + SCAN_RESULTS_AVAILABLE_ACTION
│       │   │   ├── BluetoothScanner.kt      # Classic BT startDiscovery() + ACTION_FOUND
│       │   │   ├── BleScanner.kt            # BluetoothLeScanner, SCAN_MODE_LOW_LATENCY, 5s window
│       │   │   └── CellScanner.kt           # TelephonyManager.getAllCellInfo() (GSM/LTE/WCDMA/NR)
│       │   ├── extra/
│       │   │   └── ExtraFieldsCollector.kt  # Battery, screen, network, compass/tilt, memory
│       │   ├── location/
│       │   │   ├── LocationHelper.kt        # Single GPS fix (requestSingleUpdate) + manual entry
│       │   │   └── GpsFix.kt                # lat/lon/alt/accuracy data class returned by a fix
│       │   ├── db/
│       │   │   ├── AppDatabase.kt           # Room database (name: nearscan.db, version 1)
│       │   │   ├── ScanDao.kt               # WifiScanDao, BtScanDao, CellScanDao interfaces
│       │   │   ├── WifiScanEntity.kt        # wifi_scans table
│       │   │   ├── BtScanEntity.kt          # bt_scans table (isBle flag distinguishes Classic/BLE)
│       │   │   └── CellScanEntity.kt        # cell_scans table
│       │   ├── export/
│       │   │   ├── ExportManager.kt         # Orchestrates export + totalRecordCount()
│       │   │   ├── WigleCsvExporter.kt      # WiGLE-compatible CSV (fixed schema)
│       │   │   ├── CustomCsvExporter.kt     # Full schema CSV (all RF fields)
│       │   │   └── GeoJsonExporter.kt       # GeoJSON FeatureCollection
│       │   ├── mqtt/
│       │   │   ├── MqttClient.kt            # Eclipse Paho v3 client wrapper (singleton)
│       │   │   └── MqttPublisher.kt         # entity → JSON + extra fields → publish
│       │   ├── ipc/
│       │   │   ├── TaskerBroadcaster.kt     # Sends 8 outgoing event broadcasts; manages seen-sets
│       │   │   ├── CommandReceiver.kt       # Manifest BroadcastReceiver for 6 CMD_* actions
│       │   │   └── NearScanContentProvider.kt # Read-only ContentProvider (/wifi /bt /cell /stats)
│       │   ├── prefs/
│       │   │   ├── NearScanSettings.kt      # Immutable settings data class + ExportFormat enum
│       │   │   └── SettingsDataStore.kt     # DataStore<Preferences> wrapper; settings Flow + update helpers
│       │   └── ui/
│       │       ├── MainViewModel.kt         # AndroidViewModel; StateFlow<MainUiState>; GPS fix logic
│       │       ├── MainScreen.kt            # Root composable: START/STOP, counters, session stats
│       │       ├── LocationDialog.kt        # Manual lat/lon/alt entry + GPS fix button
│       │       ├── AdvancedSettingsCard.kt  # Collapsible card: scan toggles, export format, MQTT
│       │       └── theme/
│       │           ├── Color.kt
│       │           ├── Theme.kt
│       │           └── Type.kt
│       ├── res/
│       │   └── values/                      # strings.xml + values-XX/ for 10 languages
│       └── AndroidManifest.xml
├── gradle/
│   └── libs.versions.toml                   # Version catalog
├── CLAUDE.md                                # This file
└── README.md
```

---

## Build Commands

```bash
# Debug APK (WSL2: build to /tmp to avoid AAPT2 filesystem flakiness)
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/opt/android-sdk \
  ./gradlew assembleDebug -PbuildDir=/tmp/nearscan-build --no-daemon

# Output: /tmp/nearscan-build/outputs/apk/debug/app-debug.apk

# Release APK (minified + resource-shrunk; verified buildable, unsigned)
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/opt/android-sdk \
  ./gradlew assembleRelease -PbuildDir=/tmp/nearscan-build --no-daemon

# Unit tests
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/opt/android-sdk \
  ./gradlew testDebugUnitTest -PbuildDir=/tmp/nearscan-build --no-daemon

# Firebase Robo Test
gcloud firebase test android run \
  --app /tmp/nearscan-build/outputs/apk/debug/app-debug.apk \
  --robo-script app/robo-script.json \
  --device model=Pixel6,version=33
```

---

## Dependencies (libs.versions.toml / build.gradle.kts)

```kotlin
// Compose BOM — pins all Compose library versions together
implementation(platform("androidx.compose:compose-bom:2024.12.01"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.compose.material3:material3")
implementation("androidx.activity:activity-compose:1.9.3")
debugImplementation("androidx.compose.ui:ui-tooling")

// ViewModel + StateFlow (no LiveData needed)
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

// Hilt DI (KSP — NOT kapt; kapt is incompatible with Kotlin 2.x)
implementation("com.google.dagger:hilt-android:2.51.1")
ksp("com.google.dagger:hilt-android-compiler:2.51.1")
implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

// Room (KSP)
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")

// DataStore (replaces SharedPreferences)
implementation("androidx.datastore:datastore-preferences:1.1.2")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

// MQTT (Eclipse Paho v3 client only — Paho Android Service is deprecated/broken on Android 12+)
implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
```

Kotlin 2.0.21 / KSP 2.0.21-1.0.28 / Hilt 2.51.1 / Room 2.6.1 / Paho 1.2.5 are deliberately left
pinned (see App Name & Identity section) — only AGP and the plain library versions above were
bumped in the 2026 dependency refresh.

---

## Permissions (AndroidManifest.xml)

```xml
<!-- WiFi scanning -->
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>

<!-- Bluetooth -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"
    android:usesPermissionFlags="neverForLocation"/>
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT"/>

<!-- Cell towers -->
<uses-permission android:name="android.permission.READ_PHONE_STATE"/>

<!-- Foreground service -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION"/>

<!-- Storage -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28"/>

<!-- Keep CPU alive -->
<uses-permission android:name="android.permission.WAKE_LOCK"/>

<!-- MQTT -->
<uses-permission android:name="android.permission.INTERNET"/>

<!-- Persistent foreground-service notification (Android 13+) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>

<!-- Doze/App Standby exemption prompt (see Foreground Service section) -->
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"/>

<!-- Tasker ContentProvider callers -->
<uses-permission android:name="at.designer2k2.nearscan.permission.READ_DATA"/>
<permission android:name="at.designer2k2.nearscan.permission.READ_DATA"
    android:protectionLevel="normal"/>
```

Runtime permissions to request on first launch:
- `ACCESS_FINE_LOCATION` (required for WiFi scan results on Android 9+)
- `BLUETOOTH_SCAN`
- `BLUETOOTH_CONNECT`
- `POST_NOTIFICATIONS` (API 33+ only)
- `READ_PHONE_STATE`

Plus the one-time (non-runtime-permission-dialog) `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
system prompt described in Foreground Service above.

---

## Key Implementation Notes

### WiFi Scanning
- Use `WifiManager.startScan()` + `SCAN_RESULTS_AVAILABLE_ACTION` broadcast receiver
- On Android 9+: scan throttled to 4 scans per 2 minutes in foreground — acceptable for stationary use
- `WifiManager.getScanResults()` returns `List<ScanResult>`
- Extract: `SSID`, `BSSID`, `level` (RSSI), `frequency`, `capabilities`
- Derive channel from frequency: 2412MHz = ch1, 5180MHz = ch36, etc.

### BT Classic Scanning
- `BluetoothAdapter.startDiscovery()` — takes ~12 seconds per cycle
- Listen for `BluetoothDevice.ACTION_FOUND` broadcast
- Call `startDiscovery()` again after discovery finished (`ACTION_DISCOVERY_FINISHED`)
- Respect configured interval — don't spam discovery

### BLE Scanning
- `BluetoothLeScanner.startScan()` with `ScanSettings.SCAN_MODE_LOW_POWER` for battery efficiency
- `ScanSettings.SCAN_MODE_BALANCED` if user wants faster results
- Results via `ScanCallback.onScanResult()`
- Deduplicate by address within each interval window

### Cell Scanning
- `TelephonyManager.getAllCellInfo()` returns `List<CellInfo>`
- Handle: `CellInfoGsm`, `CellInfoLte`, `CellInfoNr`, `CellInfoWcdma`
- Extract signal strength from each type's `CellSignalStrength`
- Requires `READ_PHONE_STATE` + `ACCESS_FINE_LOCATION` — both explicitly checked in
  `CellScanner.scan()` before touching `TelephonyManager`; `WifiScanner.scan()` explicitly checks
  `ACCESS_FINE_LOCATION` too (Android 9+ requires it for populated WiFi scan results). A missing
  permission returns an empty list rather than throwing.

### Deduplication (optional, Advanced setting) ✅ implemented
- Logic lives in `service/DedupTracker.kt` — pure Kotlin, no Android dependency, unit-tested
  directly (`DedupTrackerTest`)
- WiFi: skip record if same BSSID + RSSI within ±3 dBm since last **logged** value (updates the
  tracked RSSI each time a record is logged, so slow drift stays deduped)
- BT (Classic + BLE share one tracker keyed by address): skip if same address + RSSI within ±3 dBm
- Cell: always log (cell info changes are significant) — never passed through `DedupTracker`
- Default: OFF (log everything)
- Dedup only affects what's written to the DB and published over MQTT — the UI's live "found"
  counters and the Tasker `NEW_*_FOUND` first-sighting broadcasts are based on raw scan results,
  not the deduped set, so they're unaffected by this setting
- `DedupTracker.reset()` is called on every scan session start/stop so state doesn't leak between
  sessions

### Android Scan Throttle Workaround
On Android 9+ WiFi scan throttling applies. In Developer Options there is a toggle to disable throttling — mention this in the app's help/about section for power users.

### Data Retention ✅ implemented
Each time a scan session starts (`ScanService.startScanning()`), records older than **30 days**
are deleted from all three tables via `dao.deleteOlderThan(cutoff)` (already existed on each DAO,
was previously dead code — now actually called). Keeps a long-running installation's DB size
bounded without needing a settings UI for it. Not user-configurable in v1.

---

## Settings Persistence (DataStore keys)

All settings are persisted via `androidx.datastore:datastore-preferences` in `SettingsDataStore.kt`.
The class exposes a `settings: Flow<NearScanSettings>` and `update()` / `updateLocation()` helpers.
**Not SharedPreferences** — see `prefs/SettingsDataStore.kt` for the full key map.

```
nearscan_lat              (float)
nearscan_lon              (float)
nearscan_alt              (float)
scan_wifi_enabled         (boolean, default true)
scan_bt_enabled           (boolean, default true)
scan_ble_enabled          (boolean, default true)
scan_cell_enabled         (boolean, default true)
interval_wifi_sec         (int, default 10)
interval_bt_sec           (int, default 30)
interval_ble_sec          (int, default 15)
interval_cell_sec         (int, default 60)
wifi_min_rssi             (int, default -90)
wifi_bands                (string set, default {2.4, 5, 6})
export_format             (string, default "wigle_csv")
output_folder             (string, default external/NearScan/)
auto_export_interval_min  (int, default 0 = never)
mqtt_enabled              (boolean, default false)
mqtt_broker               (string)
mqtt_topic                (string, default "nearscan/data")
keep_screen_on            (boolean, default false)
dedup_enabled             (boolean, default false)
battery_opt_prompt_shown  (boolean, default false)  -- gates the one-time Doze-exemption prompt
extra_battery_level       (boolean, default false)
extra_battery_charging    (boolean, default false)
extra_battery_temp        (boolean, default false)
extra_screen_on           (boolean, default false)
extra_mobile_data         (boolean, default false)
extra_network_type        (boolean, default false)
extra_connected_ssid      (boolean, default false)
extra_heading             (boolean, default false)
extra_tilt                (boolean, default false)
extra_scan_duration       (boolean, default false)
extra_memory              (boolean, default false)
```

---

## App Icon Concept

- **Style:** Flat / Material You
- **Shape:** Standard adaptive icon (foreground + background layers)
- **Concept:** Radar sweep — dark navy/teal background, circular radar grid, bright cyan sweep line at ~45°, 2–3 small dots appearing at sweep edge representing detected signals
- **Colors:** Background `#0D1B2A` (deep navy), sweep `#00E5FF` (cyan), dots `#FFFFFF`
- **Size:** Works clearly at 48×48dp — keep elements bold and minimal

---

## Blog Post / README Angle

**Title idea:** *"NearScan — A Battery-Friendly Android RF Environment Logger"*

Key points to highlight:
- GPS-free stationary scanning
- All four RF types in one app
- Self-logging (battery, sensors) alongside RF data
- Open export formats, no cloud dependency
- MQTT integration for Home Assistant / Grafana pipelines
- Open source on GitHub

---

## Tasker / Automation Integration

NearScan exposes two standard Android IPC surfaces — **Broadcast Intents** and a **ContentProvider** — so automation apps (Tasker, MacroDroid, Automate, Locale) can both control NearScan and react to scan events without root or a proprietary plugin.

### Design Principles

- **No proprietary plugin protocol** — standard Android broadcasts and ContentProvider work out-of-the-box in every automation app
- **Manifest-declared receiver** — `CommandReceiver` is declared in `AndroidManifest.xml` with `exported="true"` so Tasker can send commands even when NearScan's UI is closed or the service is stopped
- **Self-contained outgoing events** — each outgoing broadcast carries all relevant data in extras; Tasker does not need a follow-up query
- **Read-only ContentProvider** — guarded by a custom `normal`-level permission (acts as a namespace guard); change to `dangerous` if stricter isolation is needed

---

### 1. Outgoing Broadcasts — NearScan → Tasker

Tasker profile trigger: **Event › App › Intent Received**, fill in the action string.

| Action | Extras | Fires when |
|--------|--------|------------|
| `at.designer2k2.nearscan.SCAN_STARTED` | — | Scanning session begins |
| `at.designer2k2.nearscan.SCAN_STOPPED` | `wifi_total` (int), `bt_total` (int), `cell_total` (int), `duration_s` (long) | Scanning session ends |
| `at.designer2k2.nearscan.NEW_WIFI_FOUND` | `ssid`, `bssid`, `rssi` (int), `channel` (int), `lat` (double), `lon` (double) | First time a BSSID is seen this session |
| `at.designer2k2.nearscan.NEW_BT_FOUND` | `address`, `name`, `rssi` (int) | First time a BT Classic address is seen this session |
| `at.designer2k2.nearscan.NEW_BLE_FOUND` | `address`, `name`, `rssi` (int) | First time a BLE address is seen this session |
| `at.designer2k2.nearscan.NEW_CELL_FOUND` | `mcc` (int), `mnc` (int), `cid` (long), `rssi` (int), `tech` (String) | First time a cell CID is seen this session |
| `at.designer2k2.nearscan.ROUND_COMPLETE` | `wifi_count` (int), `bt_count` (int), `cell_count` (int), `timestamp` (long) | One scan **type's** cycle finishes (see note below) |
| `at.designer2k2.nearscan.EXPORT_COMPLETE` | `file_path` (String), `format` (String), `record_count` (Long) | An export file is written |

`NEW_*` fires only on the **first** sighting per session; repeat detections do not fire individual events (use `ROUND_COMPLETE` counters instead).

**`ROUND_COMPLETE` is per scan-type, not a synchronized 4-way round:** WiFi/BT/BLE/Cell run on
independent intervals (e.g. WiFi every 10s, Cell every 60s), so there's no meaningful instant when
all four are simultaneously "done." It fires once after each individual type's cycle, carrying the
latest cumulative counts for all three counters at that moment — not once per synchronized round.

**Implementation:** `ipc/TaskerBroadcaster.kt` — singleton helper injected into `ScanService`; calls `context.sendBroadcast(Intent(action).apply { putExtra(…) })`.

---

### 2. Incoming Command Broadcasts — Tasker → NearScan

Tasker task action: **Action › Send Intent**. Set **Package** = `at.designer2k2.nearscan` (so Android routes it explicitly to NearScan even with the implicit broadcast ban).

| Action | Extras | Effect |
|--------|--------|--------|
| `at.designer2k2.nearscan.CMD_START` | — | Start scanning (no-op if already running) |
| `at.designer2k2.nearscan.CMD_STOP` | — | Stop scanning |
| `at.designer2k2.nearscan.CMD_TOGGLE` | — | Toggle scanning on/off |
| `at.designer2k2.nearscan.CMD_EXPORT` | `format` (String, optional) | Trigger export; format overrides current setting if provided |
| `at.designer2k2.nearscan.CMD_SET_LOCATION` | `lat`, `lon`, `alt` | Update static coordinates (persisted to DataStore) |
| `at.designer2k2.nearscan.CMD_SET_INTERVAL` | `type` (String: wifi/bt/ble/cell), `interval_sec` | Change a scan interval at runtime |

**Implementation:** `ipc/CommandReceiver.kt` — `BroadcastReceiver`, delegates to `ScanService.start()` / `stop()` for scan control, or a `goAsync()` + `Dispatchers.IO` coroutine calling `SettingsDataStore` for settings mutations (`CMD_SET_LOCATION`, `CMD_SET_INTERVAL`) and `ExportManager` (`CMD_EXPORT`).

- **`lat`/`lon`/`alt`/`interval_sec` accept either a numeric or String extra.** Tasker's Send
  Intent action sends extras as plain strings by default (e.g. `lat:%LOCN`) — reading them with
  `Intent.getDoubleExtra`/`getIntExtra` alone silently returns the type's default (`0.0`/`-1`)
  instead of the real value, since those getters do a type-check and don't parse strings. Both
  handlers try `getStringExtra(...).toDoubleOrNull()`/`toIntOrNull()` first and fall back to the
  typed getter, so both a string extra and a genuinely-typed numeric extra work.
- **`CMD_START` can hit Android 12+'s background foreground-service-start restriction** if
  Tasker fires it while NearScan isn't in the foreground (e.g. the scheduled-session recipe
  below). `ScanService.start()` catches the resulting `IllegalStateException` and posts a
  notification ("Couldn't start scanning in the background — tap to open NearScan") instead of
  crashing the caller. There's no way to auto-recover the start from inside a `BroadcastReceiver`.

**Manifest:**
```xml
<receiver
    android:name=".ipc.CommandReceiver"
    android:exported="true">
    <intent-filter>
        <action android:name="at.designer2k2.nearscan.CMD_START"/>
        <action android:name="at.designer2k2.nearscan.CMD_STOP"/>
        <action android:name="at.designer2k2.nearscan.CMD_TOGGLE"/>
        <action android:name="at.designer2k2.nearscan.CMD_EXPORT"/>
        <action android:name="at.designer2k2.nearscan.CMD_SET_LOCATION"/>
        <action android:name="at.designer2k2.nearscan.CMD_SET_INTERVAL"/>
    </intent-filter>
</receiver>
```

---

### 3. ContentProvider — Live Data Query Interface

Read-only `ContentProvider` for Tasker's **Content Query** action or any app using `contentResolver.query()`.

**Authority:** `at.designer2k2.nearscan.provider`

| URI | Columns | Description |
|-----|---------|-------------|
| `.../wifi` | `bssid`, `ssid`, `rssi`, `channel`, `lat`, `lon`, `timestamp` | WiFi records, newest first |
| `.../bt` | `address`, `name`, `rssi`, `lat`, `lon`, `timestamp` | BT Classic + BLE records, newest first |
| `.../cell` | `mcc`, `mnc`, `cid`, `rssi`, `tech`, `lat`, `lon`, `timestamp` | Cell records, newest first |
| `.../stats` | `is_running` (0/1), `wifi_total`, `bt_total`, `cell_total`, `duration_s`, `lat`, `lon` | Single-row live session summary |

**Note:** `selection` / `selectionArgs` are accepted but **not filtered**. `wifi`/`bt`/`cell` return the most recent rows, newest first, bounded by an optional `?limit=` URI query parameter (default 500, capped at 5000) — e.g. `content://at.designer2k2.nearscan.provider/wifi?limit=50`. This replaced an earlier version that loaded and reversed the *entire* table on every query, which didn't scale for multi-day sessions.

**Implementation:** `ipc/NearScanContentProvider.kt` — uses Hilt `@EntryPoint` pattern (ContentProvider cannot use `@AndroidEntryPoint`); wraps Room DAO queries via `runBlocking` (Binder thread — safe, does not ANR). `wifi`/`bt`/`cell` use `getRecent(limit)` (`ORDER BY timestamp DESC LIMIT :limit` at the SQL level, not loaded-then-reversed in Kotlin). Returns a `MatrixCursor` built from entity lists.

**Manifest:**
```xml
<!-- Custom permission: any app can declare it (normal level = namespace guard, not a security gate) -->
<permission
    android:name="at.designer2k2.nearscan.permission.READ_DATA"
    android:protectionLevel="normal"/>

<provider
    android:name=".ipc.NearScanContentProvider"
    android:authorities="at.designer2k2.nearscan.provider"
    android:exported="true"
    android:readPermission="at.designer2k2.nearscan.permission.READ_DATA"/>
```

Querying app must add to its own manifest:
```xml
<uses-permission android:name="at.designer2k2.nearscan.permission.READ_DATA"/>
```
Tasker declares this automatically when you fill in the Content Query action; the user grants it silently (normal protection level).

---

### 4. New Files

```
ipc/
├── TaskerBroadcaster.kt        # Sends outgoing event broadcasts from ScanService
├── CommandReceiver.kt          # Receives Tasker control commands (manifest BroadcastReceiver)
└── NearScanContentProvider.kt  # ContentProvider for live data queries
```

---

### 5. Tasker Recipe Examples

**A) Auto-stop at midnight:**
- Profile: Time 00:00
- Task: Send Intent → action `at.designer2k2.nearscan.CMD_STOP`, package `at.designer2k2.nearscan`

**B) Alert when an unknown Bluetooth device appears:**
- Profile: Intent Received → `at.designer2k2.nearscan.NEW_BT_FOUND`
- Task: If `%address` not in known list → Flash "`%name` appeared!"

**C) Show live WiFi count in a Tasker widget:**
- Action: Content Query → `content://at.designer2k2.nearscan.provider/stats`
- Variable: read column `wifi_total` → display in widget label

**D) Set location from Tasker's built-in GPS:**
- Tasker gets GPS → Send Intent `CMD_SET_LOCATION`, extras `lat=%LOCN lon=%LOCN2 alt=%LOCALT`

**E) Scheduled session (office hours only):**
- Profile 1: Time 08:00 → CMD_START
- Profile 2: Time 18:00 → CMD_STOP + CMD_EXPORT (extras `format=wigle_csv`)

---

### 6. Android Version Notes

| Concern | Details |
|---------|---------|
| Outgoing broadcasts reaching Tasker | Tasker registers its receiver at runtime → unaffected by Android 8+ manifest broadcast restrictions |
| CommandReceiver (incoming) | Custom action strings are exempt from the implicit broadcast ban; `android:exported="true"` required on API 31+ (enforced by AGP lint) |
| ContentProvider `exported` | Must be explicit `android:exported="true"` on API 31+; the `READ_DATA` permission prevents blind access from unrelated apps |
| `runBlocking` in ContentProvider | Acceptable — ContentProvider `query()` is called on a Binder thread, not the main thread; blocking there does not ANR the UI |

---

## Unit Testing Notes

No Robolectric dependency — unit tests run against the plain Android SDK stub jar with
`testOptions.unitTests.isReturnDefaultValues = true` in `app/build.gradle.kts`. This has one
important consequence: **a real (non-mocked) framework object's methods are no-ops that return
defaults** (`null`/`0`/`false`), not real working implementations. So constructing a real
`Intent`, calling `.putExtra(...)` on it, then reading it back via `.getStringExtra(...)` will NOT
round-trip — both calls silently hit the stub. This is why `TaskerBroadcasterTest` only asserts on
*how many times* `Context.sendBroadcast()` was called (a MockK-intercepted method on a mocked
`Context`, which works fine), never on the real `Intent`'s field contents.

The workaround used throughout: mock the Android framework objects your code depends on
(`Context`, `WifiManager`, `TelephonyManager`, `LocationManager`, `Location`, etc.) via
`mockk()`/`mockk(relaxed = true)` — MockK intercepts calls on *mocked* instances correctly
regardless of the stub jar, since it never reaches the real stub method body. Only *real*
(self-constructed) Android objects are affected by the stub-default behavior. If a test needs to
assert on a real framework object's post-construction state (Intent extras, Cursor rows,
ContentValues), it needs Robolectric — not currently a dependency.

Newer test coverage added under this constraint: `DedupTrackerTest` (pure Kotlin, no Android
deps — the most reliable kind), `TaskerBroadcasterTest` (call-count based), `LocationHelperTest`
and `CellScannerTest`/`WifiScannerTest` (permission-guard-clause paths, using mocked `Context`).
`ScanService`, `CommandReceiver`, and `NearScanContentProvider` remain untested at the unit level —
they're thin orchestration wrappers around already-tested pieces (`DedupTracker`,
`TaskerBroadcaster`, `ExportManager`, the scanners), and meaningfully testing the
`Service`/`BroadcastReceiver`/`ContentProvider` lifecycle itself would need Robolectric or
instrumented tests.

**Gotcha: `runTest` hangs forever if a ViewModel's `init` block launches an unbounded
`while (true) { delay(...) }` loop.** `MainViewModelTest` sets `Dispatchers.setMain(testDispatcher)`,
so `viewModelScope` (which resolves to `Dispatchers.Main.immediate`) shares the exact same
`TestCoroutineScheduler` as the test's own `runTest` block — not just the same dispatcher type, the
same virtual clock. `MainViewModel`'s session-timer ticker coroutine perpetually reschedules itself
every virtual second and never completes. `runTest` calls an implicit `advanceUntilIdle()` when the
test body returns, and since the scheduler is shared, it tries to drain that infinite ticker too —
spinning forever at ~100% CPU with zero output (looks exactly like a stalled JVM fork, not an
assertion failure or timeout). Fix: cancel `vm.viewModelScope` (via the `cancel()` extension on
`CoroutineScope`) *before* the test body returns, not in `@After` — `@After` runs only once `runTest`
itself returns, which never happens if the ticker is still alive. `MainViewModelTest` centralizes
this in a `runVmTest { vm -> ... }` helper that cancels in a `finally` block.

---

## Firebase Testing

Robo script: `app/robo-script.json`

Covers (in order): set location manually → save, start scan (8 s), stop scan, expand Advanced Settings, toggle BT Classic + BLE switches, cycle export format dropdown (WiGLE CSV → GeoJSON → Custom CSV → WiGLE CSV), enable MQTT + fill broker/topic fields + disable MQTT, toggle Keep screen on + Deduplicate, collapse Advanced Settings, second start/stop cycle, location dialog cancel flow.

Element targeting uses `text` and `contentDescription` — no `testTag` annotations required. BT Classic and BLE labels are unambiguous (counter labels are "BT" and "Cell", scan type labels are "BT Classic" and "BLE").

Firebase Test Lab auto-grants all manifest permissions — the foreground service will actually start on test devices.

---

## Future / Nice-to-Have (not in v1)

- [ ] Live signal strength graph per BSSID (mini chart in expanded list)
- [ ] Network list view (show all currently-visible networks)
- [ ] InfluxDB Line Protocol export
- [ ] Scheduled scan sessions (start at X, stop at Y)
- [ ] Notification tap → open app with session stats
- [ ] Android widget showing live count
- [ ] Compare sessions (delta view — what appeared / disappeared)

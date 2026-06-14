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
- **Target SDK:** Latest stable

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
- Range: 5 seconds to 300 seconds per type
- Implemented via `Handler.postDelayed()` loops inside a foreground Service

### Foreground Service
- Runs as Android Foreground Service with persistent notification
- Notification shows: status (running/idle), counts, current interval
- Survives screen-off, survives battery optimization (user prompted to exempt on first launch)
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
When enabled, these are added as additional columns to the database and custom CSV export.
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
│  NearScan                    ⚙️  │
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

### Set Location Dialog
- Lat / Lon / Altitude text fields (decimal degrees)
- "Get GPS Fix" button — acquires single fix, populates fields, stops GPS
- "Save" / "Cancel"
- Shows accuracy of last GPS fix

### Advanced Settings (collapsible card, collapsed by default)

**Scan Types & Intervals**
```
[ ✓ ] WiFi           [──●────────] 10s
[ ✓ ] BT Classic     [────●──────] 30s
[ ✓ ] BT LE          [───●───────] 15s
[ ✓ ] Cell Towers    [───────●───] 60s
```

**WiFi Options**
- Band filter: [ ✓ ] 2.4 GHz  [ ✓ ] 5 GHz  [ ✓ ] 6 GHz
- Min RSSI threshold: -90 dBm (slider)

**BT Options**
- Device class filter (All / Audio / Phone / Computer / Network)

**Output**
- Export format: `[WiGLE CSV ▼]` (WiGLE CSV / Custom CSV / GeoJSON / SQLite dump)
- Output folder: `/sdcard/NearScan/` [Change]
- Auto-export every: `[Never ▼]` (Never / 15min / 30min / 1hr / On Stop)
- MQTT: [ ] Enable → broker / topic fields appear

**Extra Logged Fields** (checkboxes, all off by default)
- [ ] Battery level
- [ ] Battery charging state
- [ ] Battery temperature
- [ ] Screen state
- [ ] Mobile data active
- [ ] Active network type
- [ ] Connected WiFi SSID
- [ ] Compass heading
- [ ] Device tilt
- [ ] Scan duration
- [ ] Memory available

*Note shown: "Extra fields are not included in WiGLE CSV export"*

**Keep screen on while running:** [ ] toggle

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

Type values: `WIFI` / `BT` / `GSM` / `LTE` / `NR`

### 2. Custom CSV
All fields including extra logged fields. Schema is self-documenting (header row describes all columns present).

### 3. GeoJSON
FeatureCollection of Points. Each network/device is a Feature with coordinates and all scan properties as feature properties. Suitable for QGIS, Leaflet, Mapbox.

### 4. SQLite Dump
Direct export of the Room database file. Full fidelity, queryable with any SQLite tool.

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
│   └── src/main/
│       ├── java/at/designer2k2/nearscan/
│       │   ├── MainActivity.kt              # Entry point, sets Compose content
│       │   ├── service/
│       │   │   └── ScanService.kt           # Foreground service, orchestrates all scanners
│       │   ├── scanner/
│       │   │   ├── WifiScanner.kt           # WifiManager scanning
│       │   │   ├── BluetoothScanner.kt      # Classic BT discovery
│       │   │   ├── BleScanner.kt            # BLE scan via BluetoothLeScanner
│       │   │   └── CellScanner.kt           # TelephonyManager.getAllCellInfo()
│       │   ├── extra/
│       │   │   └── ExtraFieldsCollector.kt  # Battery, sensors, network state
│       │   ├── location/
│       │   │   └── LocationHelper.kt        # Single GPS fix + manual entry
│       │   ├── db/
│       │   │   ├── AppDatabase.kt           # Room database
│       │   │   ├── WifiScanEntity.kt
│       │   │   ├── BtScanEntity.kt
│       │   │   └── CellScanEntity.kt
│       │   ├── export/
│       │   │   ├── ExportManager.kt         # Orchestrates export
│       │   │   ├── WigleCsvExporter.kt      # WiGLE-compatible CSV
│       │   │   ├── CustomCsvExporter.kt     # Full schema CSV
│       │   │   ├── GeoJsonExporter.kt       # GeoJSON FeatureCollection
│       │   │   └── MqttPublisher.kt         # Live MQTT publish
│       │   ├── mqtt/
│       │   │   └── MqttClient.kt            # Eclipse Paho MQTT client wrapper
│       │   ├── prefs/
│       │   │   └── SettingsManager.kt       # DataStore wrapper for all settings
│       │   └── ui/
│       │       ├── MainViewModel.kt         # StateFlow<MainUiState> for counters, session state
│       │       ├── MainScreen.kt            # Root composable, START/STOP, counters, session info
│       │       ├── LocationDialog.kt        # Set location dialog composable
│       │       ├── AdvancedSettingsCard.kt  # Collapsible advanced settings composable
│       │       └── theme/
│       │           ├── Color.kt
│       │           ├── Theme.kt
│       │           └── Type.kt
│       └── res/
│           └── values/
│               ├── strings.xml
│               └── themes.xml               # Shell theme (window background only; Compose owns the rest)
├── CLAUDE.md                                # This file
└── README.md
```

---

## Dependencies (build.gradle)

```kotlin
// Compose BOM — pins all Compose library versions together
implementation(platform("androidx.compose:compose-bom:2026.02.01"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.compose.material3:material3")
implementation("androidx.activity:activity-compose:1.9.0")
debugImplementation("androidx.compose.ui:ui-tooling")

// ViewModel + StateFlow (no LiveData needed)
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")

// Room (KSP — not kapt; kapt is incompatible with Kotlin 2.x)
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")

// DataStore (replaces SharedPreferences)
implementation("androidx.datastore:datastore-preferences:1.1.1")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

// MQTT (Eclipse Paho v3 client only — the Paho Android Service is deprecated/broken on Android 12+)
implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
```

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
```

Runtime permissions to request on first launch:
- `ACCESS_FINE_LOCATION` (required for WiFi scan results on Android 9+)
- `BLUETOOTH_SCAN`
- `BLUETOOTH_CONNECT`
- `READ_PHONE_STATE`

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
- Requires `READ_PHONE_STATE` + `ACCESS_FINE_LOCATION`

### Deduplication (optional, Advanced setting)
- WiFi: skip record if same BSSID + RSSI within ±3 dBm since last log
- BT: skip if same address + RSSI within ±3 dBm
- Cell: always log (cell info changes are significant)
- Default: OFF (log everything)

### Android Scan Throttle Workaround
On Android 9+ WiFi scan throttling applies. In Developer Options there is a toggle to disable throttling — mention this in the app's help/about section for power users.

---

## Settings Persistence (SharedPreferences keys)

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

## Future / Nice-to-Have (not in v1)

- [ ] Live signal strength graph per BSSID (mini chart in expanded list)
- [ ] Network list view (show all currently-visible networks)
- [ ] InfluxDB Line Protocol export
- [ ] Scheduled scan sessions (start at X, stop at Y)
- [ ] Notification tap → open app with session stats
- [ ] Android widget showing live count
- [ ] Compare sessions (delta view — what appeared / disappeared)

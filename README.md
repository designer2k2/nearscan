# NearScan

**NearScan** is a battery-friendly Android app for logging the RF environment around a fixed
location — WiFi networks, Bluetooth devices, and cell towers — over long stationary sessions
(hours or days) without draining the battery on continuous GPS.

Not affiliated with WiGLE, but exports a WiGLE-compatible CSV by default since it's a well-known
open format. Additional export formats are supported.

## Features

- **Four scan types, independently toggleable**: WiFi, Bluetooth Classic, Bluetooth LE, cell
  towers (GSM/LTE/WCDMA/NR)
- **GPS-free by design** — set your location once (manual entry or a single GPS fix that
  immediately turns GPS back off), and every logged record is stamped with it. No continuous
  location polling.
- **Configurable per-type scan intervals**, live-reactive — change them mid-session, no
  stop/restart needed
- **Runs as a foreground service with a persistent notification** showing live counts and
  elapsed time, and holds a wake lock so scanning keeps running with the screen off
- **Battery-optimization exemption prompt** so Android's Doze mode doesn't suspend long sessions
- **Export formats**: WiGLE CSV, Custom CSV (full schema incl. optional extra fields), GeoJSON,
  raw SQLite dump
- **MQTT publishing** — stream results live to a broker (e.g. for Home Assistant / Grafana)
- **Optional extra fields**: battery level/charging/temperature, screen state, network state,
  compass heading/tilt, scan duration, available memory
- **Deduplication** — optionally skip re-logging the same BSSID/address within ±3 dBm of the last
  logged value
- **Tasker / automation integration** — control NearScan and react to scan events from Tasker,
  MacroDroid, or any app, via standard Android broadcasts and a read-only ContentProvider. No
  proprietary plugin required. Full reference is built into the app (tap the **?** icon).
- **Resilient to reboots and OS kills** — detects an interrupted session on next launch and offers
  to resume it
- **10 languages**: English, Spanish, Chinese (Simplified), Hindi, Portuguese (Brazil), Russian,
  Japanese, German, French, Korean

## Screenshots

_Coming soon._

## Building

Requires JDK 17 and the Android SDK (compileSdk/targetSdk 35).

```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=/opt/android-sdk

# Debug APK
./gradlew assembleDebug

# Release APK (minified, resource-shrunk)
./gradlew assembleRelease

# Unit tests
./gradlew testDebugUnitTest
```

Output: `app/build/outputs/apk/{debug,release}/`

## Tasker / Automation Quick Reference

Package name: `at.designer2k2.nearscan`

| Direction | Mechanism | Examples |
|-----------|-----------|----------|
| Tasker → NearScan | Send Intent (action, package `at.designer2k2.nearscan`) | `CMD_START`, `CMD_STOP`, `CMD_TOGGLE`, `CMD_EXPORT`, `CMD_SET_LOCATION`, `CMD_SET_INTERVAL`, `CMD_CLEAR_DATA` |
| NearScan → Tasker | Intent Received | `SCAN_STARTED`, `SCAN_STOPPED`, `NEW_WIFI_FOUND`, `NEW_BT_FOUND`, `NEW_BLE_FOUND`, `NEW_CELL_FOUND`, `ROUND_COMPLETE`, `EXPORT_COMPLETE` |
| Tasker → NearScan | Content Query | `content://at.designer2k2.nearscan.provider/{wifi,bt,cell,stats}` |

Full details, all extras, and recipe examples are in the in-app Help screen and in
[`CLAUDE.md`](CLAUDE.md).

## Tech Stack

Kotlin, Jetpack Compose, MVVM + Clean Architecture, Hilt, Room, DataStore, Coroutines, Eclipse
Paho MQTT client. Min SDK 26 (Android 8.0).

## Project Structure

See [`CLAUDE.md`](CLAUDE.md) for the full project specification, architecture, and
implementation notes.

## License

No license has been chosen yet — all rights reserved by default.

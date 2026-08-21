# Play Store Listing — Drafts

Copy-paste drafts for the Play Console submission. These are starting points, not final legal
text — review before publishing, especially the privacy policy if you want a lawyer's eyes on it.

---

## 1. Privacy Policy

Host this as a static page (e.g. GitHub Pages from this repo, or a `PRIVACY.md` rendered on
GitHub) and link its URL in Play Console → App content → Privacy policy.

```markdown
# NearScan Privacy Policy

_Last updated: 2026-08-21_

NearScan is a local-only RF environment logger. This policy explains what data the app
accesses, why, and — most importantly — that none of it is sent to us. NearScan has no
backend server, no analytics SDK, no crash-reporting SDK, and no advertising SDK.

## What NearScan accesses on your device

- **Location (precise)** — you set your location manually or with a single GPS fix. NearScan
  does not continuously track your location; the GPS radio is used only for the moment you
  tap "Get GPS Fix," then turned back off. Every logged WiFi/Bluetooth/cell record is stamped
  with whichever location you last set.
- **WiFi networks nearby** — SSID, BSSID, signal strength, channel/band, security type.
- **Bluetooth devices nearby** (Classic and BLE) — address, name, signal strength, and,
  optionally, advertised service UUIDs and manufacturer data (toggle in Advanced Settings).
- **Cell tower information** — carrier codes, cell ID, technology, and signal quality, read via
  Android's telephony APIs. This requires the READ_PHONE_STATE permission, but NearScan never
  reads your phone number, device identifiers (IMEI/IMSI), or call state — only cell-tower info.
- **Optional device state** (off by default, toggle per field in Advanced Settings) — battery
  level/charging/temperature, screen-on state, active network type, compass heading/tilt,
  available memory. These are attached to log records if you turn them on.

## Where this data goes

**Nowhere, unless you send it yourself.** All scan results are stored in a local database on
your device only. NearScan has no server and cannot receive your data even if it wanted to.

Data leaves your device only when you take an explicit action:
- **Export** — you tap the share icon or "Export Now," and choose where the exported file goes
  (email, messaging app, cloud storage) via Android's own share sheet. NearScan does not see or
  control what happens after you pick a destination.
- **MQTT** (optional, off by default) — if you enable it and enter your own broker address,
  NearScan streams scan results to that broker. You control this address; it is typically a
  broker you run yourself (e.g. for Home Assistant). NearScan's developer never operates or has
  access to any broker.
- **Tasker / automation integration** (optional) — NearScan exposes a read-only ContentProvider
  and standard Android broadcasts so automation apps you install (e.g. Tasker) can query your
  scan data or control the app, entirely on-device, with no network involved.

## Data retention and deletion

Records older than 30 days are automatically deleted from the local database each time you
start a new scan session. You can also delete everything immediately with the "Clear Data"
button in Advanced Settings, or by uninstalling the app.

## Permissions summary

| Permission | Why |
|---|---|
| Location (fine/coarse) | Required by Android to return WiFi/Bluetooth scan results, and for the single GPS fix feature |
| Bluetooth scan/connect | Detect nearby Bluetooth Classic and BLE devices |
| Phone state | Read cell tower info (not your phone number or device identifiers) |
| Notifications | Show the ongoing scan-session notification |
| Foreground service (+ location type) | Keep scanning running reliably in the background, including with the screen off |

## Children's privacy

NearScan is a general-purpose technical tool, not directed at children, and collects no
personal information about any user of any age.

## Changes to this policy

If this policy changes, the "Last updated" date above will change accordingly. Material changes
will be noted in the app's release notes.

## Contact

Questions about this policy: [your contact email/GitHub issues link here]
```

---

## 2. Data Safety form — answers

Play Console → App content → Data safety. Google's exact question wording shifts over time —
match intent, not literal phrasing, against whatever's live when you fill it in.

**Does your app collect or share any of the required user data types?** → **Yes** (location, at
minimum — Play counts on-device use for core functionality as "collected" even though it never
leaves the device).

**Is all of the user data collected by your app encrypted in transit?** → Not applicable / no
data is transmitted by the app itself (only user-initiated export/MQTT, which the user
configures and controls).

**Do you provide a way for users to request that their data is deleted?** → **Yes** — the
in-app "Clear Data" button deletes everything immediately; uninstalling the app also removes
all local data (no server-side copy exists to separately delete).

### Data types to declare

| Category | Data type | Collected? | Shared? | Purpose | Optional? |
|---|---|---|---|---|---|
| Location | Approximate or precise location | Yes | No | App functionality (RF logging) | Required for core feature, but the value itself is user-entered, not tracked |
| Personal info | Name, email, etc. | No | No | — | — |
| Device or other IDs | Nearby WiFi/Bluetooth identifiers (BSSID/MAC of *other* devices/networks), cell tower IDs | Yes | No | App functionality (RF logging) | This is the app's core purpose, not incidental |
| App activity | App interactions | No | No | — | — |
| App info and performance | Crash logs, diagnostics | No | No | — | No crash-reporting/analytics SDK is present |

Everything is marked **"Collected"** in the sense of "used on-device for the app's function,"
and **"Not shared"** — nothing is transmitted to NearScan's developer or any third party by the
app itself. If Play's form forces a "shared" answer because of the optional MQTT/export feature,
qualify it in the free-text field: *"Only if the user explicitly configures an MQTT broker they
control, or exports and manually shares a file via Android's share sheet — the app itself has no
server and does not transmit data anywhere by default."*

**Security practices section:**
- Data deletion request mechanism: in-app (Clear Data button) + uninstall
- Data encrypted in transit: N/A (no network transmission by the app itself, except
  user-configured MQTT which is outside the app's control)
- Independent security review: No

---

## 3. Permission justification notes

Play Console → App content → Permissions declaration form (only needed if flagged for
restricted-permission review).

### READ_PHONE_STATE

> NearScan reads cellular tower information (MCC, MNC, LAC/TAC, cell ID, technology, and signal
> quality) via Android's `TelephonyManager.getAllCellInfo()` API, which requires
> `READ_PHONE_STATE` on this OS version. Cell tower scanning is one of NearScan's three core,
> user-facing scan types (alongside WiFi and Bluetooth) — the app is an RF environment logger,
> and cell tower visibility is a primary feature, not incidental. The permission is used
> **exclusively** for the cell-info API; NearScan never reads the phone number, IMEI/IMSI,
> SIM serial, or call state, and has no telephony functionality beyond reading nearby cell
> tower metadata. If the permission is denied, cell scanning is simply disabled and the rest of
> the app (WiFi/Bluetooth scanning, export, etc.) continues to work normally — see
> `CellScanner.kt`, which returns an empty list rather than crashing when the permission is
> missing.

### FOREGROUND_SERVICE_LOCATION

> NearScan runs a foreground service (`ScanService`) so that long, stationary logging sessions
> (hours to multiple days) keep running reliably with the screen off, similar to a fitness- or
> navigation-tracking app. Every record the service logs — WiFi, Bluetooth, and cell sightings —
> is stamped with the location the user set (via manual entry or a single on-demand GPS fix), so
> the service's ongoing work is inherently location-adjacent even though NearScan does not
> continuously poll GPS. This matches the `location` foreground service type's intended use: an
> ongoing task whose output is tied to the user's location. Users are shown a persistent,
> ongoing notification for the entire duration (with a direct Stop action), and starting the
> service always requires explicit user action (tapping START) — it never starts itself in the
> background.

---

## 4. Store listing copy

### Short description (≤ 80 characters)

```
Stationary WiFi, Bluetooth & cell tower RF logger — no GPS drain
```
(64 characters)

### Full description (≤ 4000 characters)

```
NearScan logs the RF environment around a fixed location — WiFi networks, Bluetooth devices,
and cell towers — over long stationary sessions, without draining your battery on continuous
GPS.

Set your location once (type it in, or grab a single GPS fix that immediately turns GPS back
off), tap START, and NearScan keeps scanning in the background for as long as you need — hours
or even days — while stamping every result with that location.

KEY FEATURES

• Four scan types, independently configurable: WiFi, Bluetooth Classic, Bluetooth LE, and cell
  towers (GSM/LTE/WCDMA/NR), each with its own interval — change them live, mid-session
• GPS-free by design — no continuous location polling, ever
• Runs as a proper foreground service with a persistent notification showing live counts, and
  holds a wake lock so scanning survives with the screen off
• WiFi filtering by minimum signal strength and by band (2.4/5/6 GHz)
• Export to WiGLE-compatible CSV, a full-schema Custom CSV, GeoJSON, or a raw SQLite dump — all
  gzip-compressed automatically, shareable straight from the export dialog
• Optional MQTT publishing — stream results live to your own broker (e.g. for Home Assistant or
  Grafana dashboards)
• Optional extra fields: battery level/charging/temperature, screen state, network state,
  compass heading/tilt, scan duration, available memory
• Deduplication — skip re-logging the same network/device within ±3 dBm of its last value
• Deep Tasker / automation support — control NearScan and react to scan events from Tasker,
  MacroDroid, or any automation app, via standard Android broadcasts and a read-only content
  provider. No proprietary plugin needed; full reference built into the app
• Resilient to reboots and OS kills — detects an interrupted session on next launch and offers
  to resume it
• 10 languages supported
• No ads. No account. No analytics. No data ever leaves your device unless you explicitly
  export or share it

NearScan is not affiliated with WiGLE, but exports a WiGLE-compatible CSV by default since it's
a well-known open format used by wardriving and site-survey tools.

WHO IT'S FOR

Anyone who wants to characterize the RF environment at a fixed point — at home, at a monitoring
station, during a site survey, or outdoors with a power bank — without babysitting the app or
burning battery on GPS you don't need once your location is set.

PRIVACY

NearScan has no backend server. All data stays on your device unless you explicitly export or
share it, or configure your own MQTT broker. See the in-app Help screen (the ? icon) for a full
usage guide, including the complete Tasker command/event/URI reference.
```

### Category

**Tools** — fits an RF-scanning utility better than Maps & Navigation (no turn-by-turn/mapping
UI) or Productivity.

### Content rating questionnaire guidance

- No user-generated content, no violence, no user communication features, no in-app purchases,
  no ads.
- Does collect location — answer the location-sharing question honestly (collected for app
  functionality, not shared with third parties by the app itself).
- Expected outcome: PEGI 3 / Everyone, with a location-data disclosure on the store listing
  (handled automatically by Data Safety answers above, not the content rating itself).

### Target audience

General audience / not designed for or targeted at children.

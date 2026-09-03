# Getting NearScan onto F-Droid

F-Droid builds every app **from source** on its own infrastructure and ships it from its own
repository. You don't upload an APK. You submit a small metadata file to
[`fdroiddata`](https://gitlab.com/fdroid/fdroiddata) describing where the source is and how to
build it; a maintainer reviews it, CI builds it, and it goes live.

## 1. Eligibility — where NearScan stands

| Requirement | Status |
|---|---|
| FOSS license, `LICENSE` file in repo | ✅ MIT |
| Source publicly available, builds with standard tooling | ✅ Gradle wrapper, `./gradlew assembleRelease` |
| No proprietary dependencies (no GMS / Firebase / analytics / ads) | ✅ verified — Hilt, Room, Compose, Coroutines, Eclipse Paho MQTT (EPL) only |
| No tracking / anti-features | ✅ none. MQTT connects only to a broker the user configures |
| Versioned git tags | ✅ `vX.Y.Z` (`v0.1.2` tagged) |
| `versionCode` increments per release | ✅ |
| Store metadata (Fastlane structure) | ✅ added under `fastlane/metadata/android/en-US/` |

Nothing else is required. Reproducible builds and a signed APK matching the Play Store signature
are **optional** and can be added later (see §5).

## 2. What was added to this repo for F-Droid

```
fastlane/metadata/android/en-US/
├── title.txt
├── short_description.txt          (≤ 80 chars)
├── full_description.txt           (≤ 4000 chars, limited HTML)
├── changelogs/
│   └── 3.txt                      (one file per versionCode)
└── images/
    ├── icon.png                   (512×512)
    ├── featureGraphic.png         (1024×500)
    └── phoneScreenshots/
        └── 1.png … 5.png
```

F-Droid reads this directly from the tagged commit, so the listing text and screenshots are
maintained here, in this repo, going forward. **Every new release needs a
`changelogs/<versionCode>.txt`** file or F-Droid shows no release notes.

## 3. Submitting — pick one route

### Route A — open the merge request yourself (fastest)

1. Register at <https://gitlab.com>, fork <https://gitlab.com/fdroid/fdroiddata>.
2. Add the metadata file (draft is in [`at.designer2k2.nearscan.yml`](at.designer2k2.nearscan.yml)):

   ```
   fdroiddata/metadata/at.designer2k2.nearscan.yml
   ```

3. Validate locally (needs the `fdroidserver` package — `pipx install fdroidserver` or the
   `registry.gitlab.com/fdroid/fdroidserver:buildserver` Docker image):

   ```bash
   cd fdroiddata
   fdroid readmeta
   fdroid rewritemeta at.designer2k2.nearscan   # normalises formatting
   fdroid lint at.designer2k2.nearscan          # must pass clean
   fdroid build at.designer2k2.nearscan         # full build in the buildserver container
   fdroid checkupdates --auto at.designer2k2.nearscan   # verifies tag detection
   ```

4. Commit as `New app: NearScan (at.designer2k2.nearscan)` and open the MR against `master`.
   GitLab CI re-runs lint + build. A maintainer reviews; expect a round or two of small
   requested changes (category choice, description wording).

### Route B — request packaging (let a volunteer do it)

Open an issue on `fdroiddata` using the **"Request for Packaging"** template with the repo URL.
Slower, but zero GitLab/tooling setup. The Fastlane metadata already in this repo means whoever
picks it up has almost nothing to write.

## 4. After it's merged — how updates work

The metadata uses:

```yaml
UpdateCheckMode: Tags
AutoUpdateMode: Version v%v
```

So for each future release you only need to, in this repo:

1. bump `versionCode` + `versionName` in `app/build.gradle.kts`
2. add `fastlane/metadata/android/en-US/changelogs/<new versionCode>.txt`
3. push a `vX.Y.Z` tag

F-Droid's bot picks up the new tag within a day, opens its own MR, builds, and publishes. No
further action in `fdroiddata` unless the build recipe itself needs to change.

## 5. Optional follow-ups

- **Signed git tags.** `v0.1.2` is annotated but not cryptographically signed. F-Droid prefers
  signed tags (`git tag -s`); set up a GPG or SSH signing key and use it for future releases.
  Add `AllowedAPKSigningKeys` / rely on tag signing so F-Droid can verify authenticity.
- **Reproducible builds with the upstream signature.** Because NearScan is also published on
  Google Play and GitHub Releases with your keystore, F-Droid can be configured to verify its
  build byte-for-byte against your signed APK and ship it with **your** signature instead of
  F-Droid's. That lets users move between Play / GitHub / F-Droid installs without uninstalling.
  Requires adding a `Binaries:` URL (the GitHub release APK) and passing F-Droid's
  reproducibility check. Worth doing once the release cadence settles.
- **More languages.** The app ships 10 locales; only `en-US` metadata exists. Add
  `fastlane/metadata/android/<locale>/` dirs (e.g. `de-DE`) with translated
  `short_description.txt` / `full_description.txt` when you have reviewed translations.

## 6. Notes / caveats

- Until reproducible builds are set up (§5), the F-Droid build is signed with **F-Droid's** key,
  not yours. A device with the Play/GitHub build installed must uninstall it before installing
  the F-Droid build. This is normal and expected for a first submission.
- F-Droid's buildserver installs the required SDK platform/build-tools automatically. The
  AGP 9.1 / Gradle 9.4 / JDK 17 toolchain is supported.
- The release `signingConfig` in `app/build.gradle.kts` is guarded by
  `if (keystorePropertiesFile.exists())`, so a fresh clone with no `keystore.properties` builds
  an unsigned release APK — exactly what F-Droid needs.

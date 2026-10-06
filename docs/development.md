# Build and verification

## Current publication: V1.5 (2026-10-06)

Normal and offline-demo APK builds passed; 91 JVM tests passed with no failures. The separate demo APK was installed on Rabbit r1 for refreshed screenshots with fictional data. The normal public APK was not installed over the user's private app. Lint still reports 15 errors (13 MissingPermission, 2 SuspiciousIndentation) in pre-existing pairing/recorder/cipher sources; this is not a clean lint result. See [verification.json](verification.json). Earlier entries below are historical checks, not the V1.5 test count.


Use Java 17 and Android SDK platform 36. Set `ANDROID_HOME` or create an ignored `local.properties`. The Gradle wrapper is included.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest
./gradlew :app:lintDebug
```

Normal APK: `app/build/outputs/apk/debug/app-debug.apk`. This repository publishes source, not a production-signed binary or private signing key. Keep the same signing key for an installed user's updates and use `adb install -r`. Never uninstall/clear a paired app as a routine test step.

## Public-export checks

The October 6, 2026 export built the normal and offline-demo APKs and passed **67 JVM tests**. The demo package was installed on the owner's Rabbit r1 for synthetic screenshots. The normal export APK was **not** installed over the owner's customized private build; the owner's original avatar, credentials and data are preserved. The preceding feature build had been installed and its reading/speed preferences checked on-device.

Current lint: **15 errors / 38 warnings** (13 permission-analysis errors and 2 vendored indentation errors). See [verification.json](verification.json) for executed checks. APK/test success does not imply clean lint. Live Muse image understanding, memo extraction quality, physical wheel timing, speech-speed listening comparisons, battery life and other firmware builds remain separate checks.

No GitHub Actions workflow is enabled: builds/tests run locally, avoiding runner use. Existing upstream instrumentation is retained for reference but has not been rerun in this export, and may require adaptation for the customized layout. Never run its default/live recording modes on a personal device just to capture pictures.

The subsequent avatar refresh restores the original upstream `muse_character.png` byte-for-byte, rebuilds both APK variants, and recaptures the offline gallery. JVM tests were not rerun for this artwork/documentation-only refresh. See CREDITS for the artwork license boundary.

## Offline demo and screenshots

```sh
./gradlew :app:assembleDemo
adb install -r app/build/outputs/apk/demo/app-demo.apk
adb shell am start -n dev.cameronpak.muser1.demo/dev.cameronpak.muser1.MainActivity \
 --es demo_language EN --es demo_scene chat
```

Language: `EN`, `KO`, `JA`. Scene: `chat`, `translate`, `photo`, `languages`, `tasks`, `favorites`, `settings`, `keyboard`. Stop only the demo between fixtures:

```sh
adb shell am force-stop dev.cameronpak.muser1.demo
```

The demo's manifest has no Internet, microphone or camera permission; it does not pair, read the main package's data or become the Home app. It seeds fictional conversation/memo/task data. Photo-scene answers are fixture text, not results of taking or understanding a photo.

Capture only while the demo is visibly foreground. `scripts/capture-demo.py` checks the foreground XML package, then saves PNGs; run it with an authorized USB device. Do not point it at the normal package. It does not enable system services or change density, keylayout, lock, or account settings.

The gallery is captured at the existing r1 480×640 / density 200 configuration. Keep demo labels and captions when sharing; avoid implying the fixtures are live model output.

## Source checks

`python3 scripts/check-public-tree.py` checks local documentation links and obvious private-artifact patterns. It is a targeted prepublication check, not a guarantee of a complete secret/security audit. Vendored licenses and provenance must be retained.

The generalized ElevenLabs provisioning tool is syntax/mock checked; this publication task did not submit an API key or generate audio. Actual model/voice availability must be checked using the reader's account.

## Embedded walking map checks (October 6, 2026)

Normal and demo APK builds pass; **73 JVM tests pass**. New tests cover the pedestrian endpoint, coordinate validation, response parsing, stale/inaccurate fixes, next-direction progress, off-route detection and duplicate geometry. Lint remains **15 errors / 38 warnings**, with no new navigation-code errors. The normal public export APK is not installed over the private avatar build.

The private feature APK is installed on the owner's r1. A dedicated opt-in device check launches only NavigationActivity, supplies a synthetic public-landmark origin, obtains a live pedestrian route and checks actual WebView tile/route/position rendering plus Android geocoding. It never opens a microphone or sends a Muse turn. The capture is labeled DEMO and uses Tokyo Station and Tokyo International Forum, not the owner's actual location. This is an online public-landmark test, distinct from the offline conversation demo. Actual outdoor motion and road-by-road guidance remain unverified.

After building `:app:assembleDebugAndroidTest`, this explicit hardware check is available to a device owner who authorizes it:

```sh
adb shell am instrument -w -r -e embeddedMap true \
  dev.cameronpak.muser1.test/dev.cameronpak.muser1.DeviceChecks
```

Require both a `PASS` stream and `INSTRUMENTATION_CODE: -1`. Do not run the runner without `embeddedMap true`; its other modes have different device/audio requirements. The fixture uses live public map/routing/geocoding services and must not be run in bulk or unattended loops. A single labeled capture is written to app cache; no credentials or user conversations are included.

## Native Google map update (October 6, 2026)

The map renderer is now Maps SDK for Android 19.2.0. On the owner's r1, the opt-in native map fixture passed with `INSTRUMENTATION_CODE: -1`: Google tile loading, the public-landmark route/position and Android geocoding were verified. The fixture keeps its own screen awake and resets its synthetic fix after map initialization to avoid a real GPS update entering the demo capture. This does not change the user's screen timeout. An initial fixture run was rejected by its location guard and produced no shared screenshot.

Source-only public builds use `MUSE_MAPS_SDK_KEY=''` and the offline demo always overrides the manifest key to empty. Normal keyless builds display a setup notice on the map screen. Private builds read the owner's external key file; keyed APKs and merged manifests are not published. JVM tests: 73 passing. Lint remains failed with 15 errors / 40 warnings, including the existing permission/indentation errors. Google Routes API authentication, driving-mode routes and Japanese transit integration remain pending. The new map SDK alone does not enable these features.

### Google Routes integration (2026-10-06)

78 JVM tests pass. WALK and DRIVE API responses and embedded map rendering passed on the physical r1 using public-landmark coordinates; mode changes cleared the old route. The route key was encrypted and its staging file absent. Wrong Android app identity was rejected with HTTP 403. The final route-start wording adjustment was installed and visually checked after USB reconnected. Current-location routing to the user-selected Akebonobashi Station was checked in Walk and Drive on the device; reported location accuracy was about 100 m. Actual outdoor movement remains unverified. Credentials, history and saved library hashes remained unchanged. Lint still fails (15 errors, 39 warnings). Earlier test/count notes above are historical.

### Voice directions (2026-10-06)

86 JVM tests pass. A synthetic Japanese command opened Drive mode and destination candidates on the physical r1; the taller address field stayed fully visible with the keyboard confirmed visible. Microphone-to-route end-to-end recognition is not yet verified. Existing credentials, history, library and encrypted Routes key were preserved. No microphone or Muse message was used in this check.

### Navigation removed (2026-10-06)

Navigation and voice routing were removed at the owner’s request. Earlier navigation sections above describe historical builds only. The current app has no navigation activity, location permissions, Maps SDK dependency or map-key injection. 67 remaining JVM tests passed; private and public APK builds passed. Installed on r1 with credentials, display history and saved library hashes unchanged. The quick menu was inspected to its final settings item and contains no Directions entry. Installed package inspection confirms no navigation activity or location permissions. The device’s app-private Routes key file was removed. Google Cloud configuration was not changed.

### Local clocks and voice controls (2026-10-06)

Private r1 installation preserved credentials, conversation history and saved-library hashes. Hardware checks passed for timer pause/resume/cancellation, firing after screen lock, Ringtone playback state, notification posting, snooze rescheduling, and next-minute alarm firing/stopping. Test schedules were removed. Local queued/TTS playback exposed Stop voice; tapping cleared playback, suppressed that reply and restored Quick actions without changing quiet mode. No Muse message or ElevenLabs API request was made by these checks. Cloud TTS cancellation follows the existing stop path but was not separately exercised.

The simplified inline alarm/timer forms were visually inspected on the 480×640 r1, including preset selection and visible top/bottom controls. Spoken-command end-to-end, human acoustic confirmation and reboot recovery remain unverified. 78 JVM tests pass. Public source builds and security checks are repeated for this publication. Historical verification counts above apply to their respective earlier builds.

Publication lint: 15 existing errors / 38 warnings; no lint findings in the new Clock/LocalClock source files. General lint remains failing and is not represented as a passed check.

### Recreate V1.5 sharing assets

After building and installing the separate demo APK on an authorized r1, run `python3 scripts/capture-demo.py`. It checks demo ownership and the expected clock/reminder scenes before capturing. Then run `python3 scripts/make-showcase.py` in a Python environment with Pillow installed. It writes the overview sheets into `docs/images` and the Discord kit into the ignored `build` directory. Only use fictional demo content for public captures.

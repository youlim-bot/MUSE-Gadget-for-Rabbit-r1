# Build and verification

Use Java 17 and Android SDK platform 36. Set `ANDROID_HOME` or create an ignored `local.properties`. The Gradle wrapper is included.

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest
./gradlew :app:lintDebug
```

Normal APK: `app/build/outputs/apk/debug/app-debug.apk`. This repository publishes source, not a production-signed binary or private signing key. Keep the same signing key for an installed user's updates and use `adb install -r`. Never uninstall/clear a paired app as a routine test step.

## Public-export checks

The October 6, 2026 export built the normal and offline-demo APKs and passed **67 JVM tests**. The demo package was installed on the owner's Rabbit r1 for synthetic screenshots. The normal export APK was **not** installed over the owner's customized private build; the owner's original avatar, credentials and data are preserved. The preceding feature build had been installed and its reading/speed preferences checked on-device.

Current lint: **15 errors / 36 warnings** (13 permission-analysis errors and 2 vendored indentation errors). See [verification.json](verification.json) for executed checks. APK/test success does not imply clean lint. Live Muse image understanding, memo extraction quality, physical wheel timing, speech-speed listening comparisons, battery life and other firmware builds remain separate checks.

No GitHub Actions workflow is enabled: builds/tests run locally, avoiding runner use. Existing upstream instrumentation is retained for reference but has not been rerun in this export, and may require adaptation for the customized layout. Never run its default/live recording modes on a personal device just to capture pictures.

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

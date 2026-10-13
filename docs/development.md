# Build and verification

## Current publication: V2.0 (2026-10-07)

See [verification.json](verification.json) for executed V2.0 checks. Both normal and isolated offline-demo APKs and the Android-test APK build successfully. All 116 JVM tests pass. Lint retains 15 errors (13 MissingPermission and 2 SuspiciousIndentation) and 46 warnings; a successful build is not a clean lint result. All earlier sections below refer to their historical builds.

The normal V2.0 APK is not installed on the personal R1 for this publication. New screenshots are native views from a disposable, unpaired Android 16 emulator configured to 480×640 / density 200 with no display cutout. They are not physical R1 captures. The offline demo has no Internet, location, microphone or camera permission; no live assistant or ElevenLabs calls are made. Pet stages use simulated age and fixed care values, not a multi-day observation.

### Capture V2.0

Build `:app:assembleDemo` and install only `dev.cameronpak.muser1.demo` in a disposable emulator. Remove any phone-specific cutout overlay and enable `wm set-ignore-orientation-request true` on that disposable emulator so Android 16 does not letterbox this portrait app. Do not alter your personal device's screen configuration.

```sh
python3 scripts/capture-demo.py --serial emulator-5554
python3 scripts/make-showcase.py
```

Replace the serial with your disposable emulator's identifier. The capture script verifies the demo package is foreground. Pillow is needed for the contact sheets; NumPy is needed only to regenerate the bundled procedural audio. The kit is written under ignored `build/`. Sources and share images, not keyed APKs, are published.

Demo scenes now also include `pet-egg`, `pet-baby`, `pet-adult`, `pet-food`, `pet-play`, `pet-music`, `pet-agent`, `quick`, `settings` and `reading`. Pet menu captures use actual taps after opening the fixture. Only the isolated demo responds to the fixture extras; the normal app uses saved state. Demo alarm saves and Wi-Fi-system entry are blocked.

The V2.0 `petLifecycle` run passed with a PASS result and `INSTRUMENTATION_CODE: -1`. The opt-in `petLifecycle` instrumentation requires an unpaired disposable emulator and exercises migration, persistence, hidden progress, animated frames, simulated hatching/stages, food, games, illness, death and new-egg archiving. Never run the default instrumentation entry on a personal device: other modes may record audio or access an account.


## Historical publication: V1.5 (2026-10-06)

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

## Physical R1 update caveat

An in-place, same-signed `adb install -r` preserves app data, but an observed R1
update removed Side button controls from Android's
`enabled_accessibility_services` setting. With the device owner's authorization,
record the exact service list and `accessibility_enabled` value privately before
replacement, then verify both enablement and binding afterward. Installation
success alone does not prove that Side button controls is running.

Accessibility recovery needs separate owner authorization. Restore only the
previously enabled Muse r1 service and its prior accessibility flag, preserving
all other enabled services. If the pre-install record is missing, stop and ask
the owner before changing settings. Keep these records out of public logs and
documentation; do not clear app data or remove PIN protection to recover input.

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

### Historical V1.5 sharing workflow

After building and installing the separate demo APK on an authorized r1, run `python3 scripts/capture-demo.py`. It checks demo ownership and the expected clock/reminder scenes before capturing. Then run `python3 scripts/make-showcase.py` in a Python environment with Pillow installed. It writes the overview sheets into `docs/images` and the Discord kit into the ignored `build` directory. Only use fictional demo content for public captures.

## Post-V1.5 source update

Normal/demo builds and 94 JVM tests pass for the reactive avatar, treasure hunt, conversation partner and configurable clock delay additions. Native menu checks and a 10-second clock transition were performed on the private development build. No live photo judgment or Muse practice response was requested during these checks. Existing V1.5 screenshot fixtures remain unchanged.

### Pet reply mute and volume verification (2026-10-07)

Debug and Android test APK builds passed, with all 116 JVM tests passing in
both working copies. The offline `petControls` instrumentation fixture passed
on a disposable emulator: shared/persisted reply mute, localized View chat
control, DPAD and generic rotary volume routing, cancellation, and mini-game
selection isolation. It made no microphone or live Muse requests.

Installed on the R1 with `adb install -r`; pre-launch hashes confirmed preserved
credentials, conversation history, saved library, pet records, and reply options.
The actual R1 pet toolbar and mute toggle were inspected, and the original mute
setting was restored after checking. Side button controls was initially off and
was enabled after the owner explicitly approved it. Automated physical-device
volume assertions were inconsistent; a subsequent injected up combination did
increase media volume. The owner then confirmed on the physical R1 that
holding the side button while turning the wheel changes both the on-screen
volume number and audible volume normally.

Physical R1 follow-up: after the owner paired again on the access-first build,
one app restart completed account HTTP 200, WebSocket HTTP 101, the Noise
handshake, and ready without refreshing or modifying encrypted credentials.
The side-button accessibility service remained bound after restoration. This
confirms the tested restart; long-term token expiry has not been exercised.


### Pet dialogue and expressions (2026-10-07, local)

Both source trees build and pass 129 JVM tests (8 new pet dialogue checks).
The unpaired emulator `petDialogue` fixture passed with `INSTRUMENTATION_CODE: -1`:
partial structured replies remain hidden, final replies normalize to text, one
expression runs per turn, mute and care state remain intact, stale dialog replies
cannot animate a replacement dialog, and KO/JA/EN show avatar and text without
overlap at 480×640. Native captures at two dance times differ and were inspected.
This uses fictional responses, not live model compliance or physical R1 proof.

Lint reports 20 errors in unchanged pairing/recorder permission handling, camera
API compatibility and vendored Java indentation; no errors in the pet dialogue
changes. Device installation and live voice/Muse behavior remain unverified for
this change. No publication was performed.

The existing `petControls` regression also passed (mute persistence, side-button
plus DPAD/generic wheel volume, cancellation and mini-game isolation). Its first
run checked volume before Android AudioService applied the fixture's initial
volume; another run lacked dialog input focus. The fixture now waits for
both explicit preconditions with bounded polls and passed on rerun. Production volume behavior was not changed.


### Pet dialogue R1 deployment (2026-10-07)

After the owner's explicit build/deploy request, the latest debug APK built and
all 129 JVM tests passed. The USB target was verified as vendor model r1 on
mt6765. The installed and replacement signing certificates matched. Installation
used `adb install -r`; the installed APK hash matches the built APK.

The Home app automatically restarted after replacement, updating its connection
diagnostics. Excluding only that runtime diagnostic file, a repeated comparison
verified all 20 private data/preference files unchanged across installation.
Encrypted pairing credentials remained unchanged after launch. Account HTTP 200,
WebSocket HTTP 101, Noise handshake and ready were recorded; MainActivity was
resumed. The exact prior accessibility setting was restored and Side button
controls was bound. No app-data clearing, uninstall, firmware changes or device
reboot occurred. No microphone recording or live user question was submitted;
actual model expression selection and audible playback remain hands-on checks.
No GitHub commit, push or public release was performed.


### Pet tools deployment (2026-10-07)

Both source builds and their 129 JVM tests passed. The verified APK was installed
on the connected R1 with matching signing certificate and installed APK hash.
Twenty user data/preference files remained unchanged across update (runtime
connection diagnostics excluded); saved credentials survived launch, Muse
reached ready, and the side-button service was bound with its setting preserved.

The actual R1 egg-stage Tools menu was opened and inspected: egg care, avatar
questions/requests, music/effects, voice-reply toggle and care help appeared,
with no general alarm, camera or library actions. The menu fits the 480x640
screen. No care action or live assistant question was submitted. Adult/sleep
menu branches were source-reviewed, not exercised on the owner's saved pet.
No GitHub publication was performed.


### Six special activities (2026-10-07)

See [special-activities.md](special-activities.md) for behaviour and data boundaries.
Private and public-mirror APK builds passed, with 137 JVM tests in each. On a
disposable unpaired 480x640 emulator, special-activity persistence/rewards/photo
reply validation/local notifications, KO/JA/EN pages and time-picker confirmation
passed. Existing pet mute/wheel-volume and dialogue/animation regression fixtures
also passed. No live cloud requests or microphone recordings were used.

The first emulator UI checks exposed cached Pixel display/cutout overrides;
removing those and refreshing display dimensions produced the native R1 viewport.
Visual inspection then found low-contrast time digits and an occluded shelf
decoration; both were corrected before the final screenshots and deployment.

Lint retains the previous 20 errors (pairing/recording permissions, camera API
compatibility and vendored code). New special-activity files have no lint errors;
three synchronous-prefs warnings reflect deliberate atomic reward persistence.
Lint is not clean.

Installed the signed APK on the connected Rabbit r1 via an in-place update. The
installed APK hash matched the build, and all 20 protected existing private data
files remained unchanged across installation (runtime connection diagnostics
excluded). Credentials also remained unchanged after launch. Muse reached ready
after HTTP 200, WebSocket 101 and Noise handshake; the original side-button
accessibility setting and bound service were verified.

Actual R1 inspection verified all six menu entries, the existing egg state and
hatched-only departure guard, and visible hour/minute and confirmation controls.
No promise was scheduled on the owner's device; no photo, microphone, care action
or live assistant question was submitted. Live photo-model accuracy and a real
time-elapsed reminder remain hands-on checks. No commit or push was performed.


### Source publication checks (2026-10-07)

The public source normal/debug APK, isolated demo APK and Android test APK build
passed, with 137 JVM tests. The public unpaired-emulator special-activities
fixture passed again, including a new regression: after a photo mission, starting
an ordinary text/voice turn clears its verifier so the next answer remains a
normal conversation. This source correction was not installed on the owner's R1
as part of the publication request.

The staged tree passed targeted artifact/secret/private-path/contact checks and
local documentation-link checks. Existing custom public avatar art and demo
separation were retained. This is a source commit; the V2.0 release binaries and
showcase assets were not republished.


### Mission camera navigation regression (2026-10-07)

The `petCameraReturn` unpaired-emulator fixture exercises actual activity
transitions, including MainActivity.onStop and CameraActivity.finish. A confirmed
photo handoff restores the pet dialogue and displays a fictional model result
there. Cancel returns to the mission page. Recreating the stopped parent while
the camera is open retains the destination. An ordinary camera still returns to
Home. The fixture does not send a real photo or record microphone input.

Debug/Android test builds and 137 JVM tests passed in the private tree; public
debug/demo/Android test builds and 137 JVM tests also passed. The navigation
fixture passed with Android RESULT_OK. A native fictional-result screenshot was
inspected. This follow-up is a source change; existing release assets are unchanged.

The fix was subsequently installed on the owner's connected R1 via `adb install -r`.
The installed hash and signing certificate were verified, all 21 existing private
data files stayed unchanged across the update (runtime diagnostics excluded),
and encrypted credentials remained unchanged after launch. Muse reached ready
and the original side-button service remained bound. Lint retains 20 baseline
errors, with none in the camera-return changes. Actual owner-photo submission
was not performed; on-device live judgement remains a hands-on check.


### Photo mission verdict handling (2026-10-08)

The previous generic photo-verification failure was also emitted when the model
reply did not match the JSON schema; it did not prove that the image was unreadable.
Mission prompts now use a valid JSON example and explicitly include circular
clock faces among round objects. One complete verdict object may be surrounded
by prose or code fences. Boolean strings, multiple verdicts and incomplete
objects cannot grant a reward. Ordinary prose is shown with an unconfirmed-verdict
notice; malformed structured text is not displayed or read aloud.

Six regressions cover wrapped Japanese clock verdicts, escaped braces, ambiguous
responses, prose-only replies, malformed JSON and the photo transport size limit.
Both source trees passed 143 JVM tests; private debug and public debug/demo APKs
built successfully. The same-signed update was installed on the owner's R1,
with all protected user-data files and accessibility settings preserved. Muse
reached ready. The owner's original photo and raw model reply were unavailable;
live image judgement requires another owner-confirmed submission.


### Pet little world activities (2026-10-08)

Five pet-only pages add a time-based three-planter garden, wheel pond fishing
(and existing wheel games), multi-day learned tricks with optional custom cues,
an explicitly saved private photo discovery album, and seven daily branching
story chapters. KO/JA/EN labels and prompts follow the selected display language.
New records are scoped to each companion and do not replace life or credentials.

Both source trees passed 155 JVM tests. Private debug/Android test and public
debug/demo/Android test builds succeeded. The unpaired-emulator `petWorld`
fixture passed persistence, exactly-once garden collection, skills and cue
collisions, story timing, album limits/delete/reply correlation, three-language
UI within the OS viewport of a 480x640 test emulator, wheel/side volume routing
without a microphone request, and actual album camera cancel/send navigation.
The existing `petCameraReturn` activity-lifecycle regression also passed, including
parent recreation and ordinary-camera return. Fixtures use synthetic dates,
photos and replies; they do not establish live Muse vision or speech accuracy.

A delayed dialog-dismiss callback can no longer clear a newer pet-room reference.
Camera review retains the discovery language when reopened after cancellation.
The same-signed update was installed on the owner's R1 with the installed APK
hash verified. All 21 protected user-data files remained unchanged across
installation (runtime diagnostics excluded), encrypted credentials were unchanged
after launch, Muse reached ready, and the side-button accessibility service
remained bound. Real-time growth, learned voice cues and live photo recognition
remain owner hands-on checks. No GitHub commit/push or release update was made.

Actual R1 inspection also verified all five new menu entries and opened the
garden page. Its native 480x640 screenshot was inspected for visible header,
scrollable content and fixed return control. No care action, camera capture or
microphone input was performed during this device UI check.


### Elapsed time and growth media prebuild (2026-10-08)

Calendar time since the current life began is displayed as years/months/days/whole
hours, including closed-app time. An exact reset archive or uncapped egg record
restores the start; ambiguous legacy starts are marked approximate. Migration
retains identity and care. Six transparent stage PNGs, five project-composed
24-second music loops, six effects and twelve KO/JA/EN locally synthesized voice
prototype clips are packaged. Growth and hatching rules are unchanged.

Private debug/Android test and public debug/demo/Android test builds succeeded;
both trees passed 164 JVM tests. The unpaired-emulator `petGrowth` check passed
start persistence/migration, unclipped three-language elapsed labels, six native
stage views, bounded sprite memory/eye landmarks, growth track choice, local
voice playback in all three languages, music-off voice playback, mute/focus
cancellation and disabled-voice guards. The music selection panel was changed
to a scrolling full-screen list with fixed sound switches after native inspection
found clipped alert-dialog buttons. Its final native view was inspected.
The existing `petWorld` persistence/input/camera regression passed on the final
APK with synthetic photos/replies. No network/microphone requests were performed.

The signature-verified local APK includes all 29 new media assets. Screenshots
use fictional unpaired-emulator care/start dates within the OS viewport of a
480x640 test display. No owner R1 state was reset or changed, no physical-device
installation or live audio/vision accuracy check was performed, and no commit,
push or release publication was made. See [growth resource notes](pet-growth-assets.md).


### Distinct growth stages and original adult installation (2026-10-08)

Baby/child/teen silhouettes now differ clearly in body shape and head proportion.
At the owner's request, the adult uses the unmodified, legless original Muse art;
its lower-resolution source uses a smaller decode sample to remain sharp. The
child/teen chest accents and blink landmarks match the new sprites. Adult care
emblems float beside the original body when a form has been selected.

Final private debug/AndroidTest and public debug/demo/AndroidTest builds succeeded;
both trees passed 164 JVM tests with zero failures/errors. The final unpaired
`petGrowth` fixture passed and its native growth captures were inspected. All
29 media entries were verified inside the APK, including a byte-identical
original adult PNG.

The owner authorized an actual R1 update. Signing identity and installed APK
hash matched; all 22 protected user-data files remained unchanged across install
(runtime connection diagnostics excluded). Credentials were unchanged after
launch, Muse reached ready, and Side button controls remained bound with its
accessibility setting preserved. The actual pet home showed an unclipped elapsed
time row; life identity and the saved start remained unchanged. No owner life
reset, camera or microphone input was performed. Speaker quality and long-term
growth remain hands-on checks. No GitHub commit/push or release update was made.

### All character stages without feet or legs (2026-10-08, local only)

At the owner's request, child, teenager and old-friend sprites were regenerated
with one continuous rounded lower body and no feet or legs. Baby/egg remain
unchanged; adult remains byte-identical to the original Muse image. Blink
landmarks and chest-accent positions match the replacement art in both local
source trees. Life, credentials, audio and existing unrelated edits are preserved.

Private debug/AndroidTest and public debug/demo/AndroidTest builds succeeded.
Each tree executed all 164 JVM tests with zero failures/errors. The unpaired,
audio-disabled disposable emulator passed petGrowth with RESULT_OK (-1), and
all six native stage captures plus the music panel were inspected. The first
fixture attempt failed to focus the pet room because Android's first-use
fullscreen tutorials obscured the app; after dismissing those tutorials on the
disposable emulator, the fixture passed. No product or fixture change was needed
for that startup issue. Existing unrelated lint failures were not rechecked
and these successful builds do not imply clean lint.

The new APK and image kit are local artifacts only. No physical-device operation,
owner life reset, microphone/cloud test, Git commit, GitHub push or release
update was performed for this revision. Earlier R1 installation evidence applies
to the preceding image pack, not this footless replacement.

### Footless growth update installed on R1 (2026-10-08)

Following the owner's explicit installation request, the verified private APK
was installed with adb install -r. Signing identity matched the existing package,
and the installed APK hash matched the locally verified footless build. All 26
protected files in no_backup, shared_prefs and files were byte-identical across
installation; only runtime connection diagnostics were excluded. Credentials
were unchanged after launch, Muse reached ready, the existing Side button controls
service remained bound, and accessibility enablement was preserved.

No life reset, microphone/camera use, live Muse turn, PIN/keylayout change, reboot,
Git commit, GitHub push or release update was performed. Physical growth-stage
previews and acoustic quality were not tested by this installation check; earlier
unpaired-emulator captures cover the replacement sprites. The public debug/demo
artifacts remain local builds; the installed device package is the private build.

### Public app brightness and feeding port (2026-10-13)

The public source adds Settings > Screen brightness (System, 20/40/60/80/100%)
and applies the saved app-window override to activities and dialogs. System
restores Android's brightness policy; no system-setting write or permission is
added. Feeding advances life first, accepts food below 95 without a meal cooldown,
and reports full/sleeping/egg/dead reasons while retaining bounded rewards.
Public DEMO fixtures, permission separation and the conversation dolphin remain
unchanged. See [architecture](architecture.md) and [exchange rules](feature-exchange.md).

Executed in this public tree with Java 17 and the Android SDK:

```sh
./gradlew :app:assembleDebug :app:assembleDemo :app:assembleDebugAndroidTest :app:testDebugUnitTest
```

All four tasks passed; JUnit XML reports **204 tests, zero failures/errors/skips**.
AndroidTest was compiled and packaged, not executed. Private-tree emulator and
R1 installation evidence applies to the private build; this port did not perform
emulator/device installation, instrumentation, live Muse tests, microphone or
camera use, or lint. Physical brightness and wheel feel remain hands-on checks.
The documentation distinguishes the conversation dolphin from the byte-identical
original Muse adult sprite; all artwork remains outside the MIT code license.

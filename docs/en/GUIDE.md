# Installation and user guide

[English](GUIDE.md) · [한국어](../ko/GUIDE.md) · [日本語](../ja/GUIDE.md) · [Home](../../README.md)

## V2.0 — Raise your Muse

Open **Quick actions → Raise Muse** to enter a dedicated virtual-pet room. The companion starts as an animated egg and grows over days, including time away from the app. The interface deliberately hides exact hatching requirements and growth thresholds. Care choices influence temperament and the eventual adult form; extended neglect can cause illness and death. A new egg archives the previous local pet record.

After hatching, choose from six foods (milk, porridge, berries, fish, vegetables and cake), three mini-games (star catching, light memory and bird rhythm), cleaning, medicine and sleep. Repeated actions have cooldowns; growth is not an instant tap-to-level checklist. Adult forms share the custom dolphin base with different accessories and behavior.

The **♫** control offers Auto, Forest, Music box, Garden and Off, with separate sound effects. Auto changes the soundtrack with growth and sleep. Speech/recording and audio-focus changes take priority over pet audio.

Muse remains your assistant inside the pet room: use Talk, Type or Tools without leaving the mode. Successful assistant interactions contribute to care. **Local pet care, motion and music work offline; Muse answers and cloud speech still need a valid connection.** An offline room is not proof that pairing has recovered.

The bottom toolbar is now **Camera → Keyboard → Quick actions → Sound → Settings**. Settings groups Wi-Fi, pairing/side-button controls, text size, voice speed, volume, screen timeout, charging-clock delay, avatar motion and display language. Wi-Fi passwords stay in Android's system panel, opened from Muse.

Weather distinguishes **UV now** from **today's peak UV**, marks nighttime, preserves missing readings as unknown, and refreshes when the weather area is tapped. Connection recovery has bounded retries and a re-pairing notice; it cannot restore revoked pairing without the supported phone flow. Navigation remains removed.

[Trilingual V2.0 release notes](../RELEASE-V2.0.md) · [Offline screenshot gallery](../SHOWCASE.md)


## 1. Scope and prerequisites

This records one Rabbit r1 installation, not an official device support promise. The tested system is **LineageOS 21 / Android 14, `lineage-21.0-20250621-UNOFFICIAL-arm64_bgN`**. The original vendor/kernel remain; this project builds an Android app, not firmware.

Flashing erases userdata and lowers boot-chain security. Keep a verified backup of **your own unit**, including its original firmware/partition inventory, off-repository. Never share modem/calibration/identity partitions or write another unit's backup. Leave the bootloader unlocked with this GSI/vbmeta combination. A successful boot is not proof of current security support.

You need a data-capable USB cable, macOS (or a suitably adapted host), Android platform-tools, at least 5 GB of disk space, Java 17 and Android SDK platform 36 to build the app. Connect only the intended r1. Close WebUSB flashers before using adb/fastboot. On macOS, Homebrew users can install `android-platform-tools`; Android Studio can install the SDK.

Bootloader unlocking is a separate prerequisite: consult [r1_escape](https://github.com/RabbitHoleEscapeR1/r1_escape). Do not assume the unlock procedure is unchanged across firmware. Read [Android GSI guidance](https://source.android.com/docs/core/tests/vts/gsi), [the community walkthrough](https://substrate.dougbelshaw.com/rabbit-r1-android), and the [firmware archive guide](https://github.com/TurboTheTurtle/rabbit-r1-firmware/blob/main/docs/flashing-guide.md).

If your device shows dm-verity corruption, powers off, or cannot enter fastbootd, read the [recovery case](JOURNEY.md) before proceeding. Repeatedly flashing older boot/vbmeta images is not a supported fix.

## 2. Download the tested images

Run in a new working directory. This old build is documented for reproducibility, not recommended as the newest/most secure release.

```sh
curl -fL --retry 2 -o lineage.img.gz \
 'https://downloads.sourceforge.net/project/andyyan-gsi/lineage-21-td/lineage-21.0-20250621-UNOFFICIAL-arm64_bgN-signed.img.gz'
echo '2ad81102b6902737c182d791f0f88c0c1e31aa11e7f7d0c3e3864c49b04a4aa6  lineage.img.gz' | shasum -a 256 -c -
gzip -t lineage.img.gz
# Continue only after both checks succeed.
gzip -dc lineage.img.gz > lineage.img
curl -fL -o google-gsi-vbmeta.img \
 'https://dl.google.com/developers/android/qt/images/gsi/vbmeta.img'
echo 'f6da5489fd877cb69cf61fa721cfd6d77e530084aefe9b96664f818947ff61f6  google-gsi-vbmeta.img' | shasum -a 256 -c -
```

Stop on any failure. Hashes are installation-session fingerprints, not independent publisher signatures. The expanded system image was 3,090,542,592 bytes; vbmeta was 4,096 bytes. No firmware binary is hosted in this repository.

## 3. Verify modes and slot, then flash

Enter bootloader fastboot using the unlock project's procedure, or `adb reboot bootloader` from a working, authorized Android installation.

```sh
fastboot devices
fastboot getvar unlocked
fastboot getvar current-slot
fastboot reboot fastboot
fastboot getvar is-userspace
fastboot getvar current-slot
fastboot getvar is-logical:system_a
fastboot reboot bootloader
fastboot getvar current-slot
```

**Required:** one target, unlocked bootloader, `is-userspace: yes` in fastbootd, logical `system_a`, and slot `a` consistently in both modes. Stop if any condition differs. A bootloader `FASTBOOT` screen is not fastbootd. This recipe does not cover slot B or another layout.

From bootloader fastboot:

```sh
fastboot flash vbmeta_a google-gsi-vbmeta.img
fastboot reboot fastboot
fastboot getvar is-userspace
fastboot getvar current-slot
# Continue only if yes / a:
fastboot -S 100M flash system_a lineage.img
```

Every write must return `OKAY`. The tested install used 30 sparse transfers. A `FAILED` result is a stop condition. Do not delete other logical partitions to force a fit. The checked Google vbmeta already disables verification; it was flashed unchanged. No boot/vendor/modem/preloader/slot-B rewrite is part of this successful GSI procedure.

**The following wipe permanently erases userdata and encryption metadata.** Confirm slot A again:

```sh
fastboot reboot bootloader
fastboot getvar current-slot
# Only after confirming a and accepting the wipe:
fastboot -w
fastboot reboot
```

Wait for Android setup, connect Wi-Fi, configure screen security and enable Developer options → USB debugging. Authorize the Mac. Check Android 14 / the stated Lineage build / `_a`:

```sh
adb devices
adb shell getprop ro.build.version.release
adb shell getprop ro.lineage.version
adb shell getprop ro.boot.slot_suffix
```

## 4. Build and install Muse

Clone this repository. With Java 17 and `ANDROID_HOME` configured:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n dev.cameronpak.muser1/.MainActivity
```

Use the same signing key for future updates. A debug key is host-specific; retain yours privately. Do not delete the installed app to bypass signature mismatch. Clearing data loses pairing, local history, favorites and memos.

## 5. SDK token and pairing

Obtain your own [Muse SDK token](https://gadgets.muse.ai/settings/sdk-tokens) and review the [SDK terms](https://gadgets.muse.ai/sdk-terms). An eligible account and the phone Muse app are required; availability may vary.

Run in **Bash**, not with a literal token in your command history:

```bash
adb shell am force-stop dev.cameronpak.muser1
read -r -s -p 'Muse SDK token: ' MUSE_SDK_TOKEN
printf '\n'
printf '%s' "$MUSE_SDK_TOKEN" | adb shell \
 'run-as dev.cameronpak.muser1 sh -c "umask 077; mkdir -p files; cat > files/pending-sdk-token"'
unset MUSE_SDK_TOKEN
adb shell am start -n dev.cameronpak.muser1/.MainActivity
adb shell 'run-as dev.cameronpak.muser1 sh -c "test ! -e files/pending-sdk-token && test -s no_backup/credentials.enc && echo ENCRYPTED_IMPORT_OK"'
```

The app imports into Android Keystore AES-GCM encrypted storage and deletes the pending file. Never publish keys or pairing screenshots. Already paired devices can skip provisioning.

Hold the empty background → **Pair with Muse**. Allow Bluetooth. In the phone Muse app, open Settings → Devices, enable Developer mode, and add the gadget matching the r1's displayed name. Pairing window: two minutes. The r1 uses its own Wi-Fi/LTE connection.

## 6. Side button, wheel and display

This keylayout replaces the native side-button power action. If it is already installed, do not reinstall it. Otherwise enable LineageOS **Rooted debugging** temporarily:

```sh
adb root
adb wait-for-device
adb shell 'mkdir -p /data/system/devices/keylayout; chown system:system /data/system/devices /data/system/devices/keylayout; chmod 755 /data/system/devices /data/system/devices/keylayout'
adb push hardware/mtk-kpd.kl /data/system/devices/keylayout/mtk-kpd.kl
adb shell 'chown system:system /data/system/devices/keylayout/mtk-kpd.kl; chmod 644 /data/system/devices/keylayout/mtk-kpd.kl; restorecon -RF /data/system/devices'
adb reboot
adb wait-for-device
adb shell dumpsys input
```

Verify `mtk-kpd` uses `/data/system/devices/keylayout/mtk-kpd.kl`, then **turn Rooted debugging off**. Enable Android Accessibility → **Side button controls**. If blocked, allow restricted settings in the app's Android settings first. The service filters keys; it does not request screen-content access. Setting Muse as Home is optional:

```sh
adb shell cmd package set-home-activity dev.cameronpak.muser1/.MainActivity
```

- Hold side button: record; release: send. Tap: Android lock/screen-off action.
- Hold side button + turn wheel: media volume; that press does not submit recording.
- Camera open: side button takes a photo; wheel up/down requests back/front camera. Front/back selection physically rotated the tested unit; wheel timing/direction still needs hands-on checks.
- Tap/hold avatar/background: device controls or push-to-talk, depending on gesture.
- If controls are clipped on the 480×640 panel, the tested density was `adb shell wm density 200`; use `adb shell wm density reset` to undo.

Screen timeout and screen lock are separate Android settings. If you prefer no unlock screen, change **Settings → Security → Screen lock → None** yourself (labels vary). This removes device protection; the app does not silently remove a PIN. Screen timeout can remain enabled. Restore a PIN in the same settings.

To restore native button mapping, temporarily enable Rooted debugging, run `adb root`, remove only `/data/system/devices/keylayout/mtk-kpd.kl`, reboot, and verify the original keylayout. Disable Rooted debugging again.

## 7. ElevenLabs and languages

Optional ElevenLabs integration uses `scribe_v2` at `/v1/speech-to-text` and `eleven_v4` at `/v1/text-to-dialogue`. These are the model identifiers in the implementation, not a guarantee that every account supports them. Check [ElevenLabs documentation](https://elevenlabs.io/docs) and your model/voice availability. API usage can incur charges.

Run `python3 scripts/provision-elevenlabs.py` with one r1 attached. It prompts privately for an API key and asks for your voice ID, validates model/voice access, and transfers an app-private pending file for encrypted import. It does not generate audio. Add a library voice to your account beforehand if needed. Key permissions must cover STT/TTS generation and model/voice reads. **HTTP 400 alone does not prove a permissions issue**; `voice_not_found` can mean the selected voice is not accessible in the account.

- Top-right flag: interface language (Korean/Japanese/English).
- **Translate**: choose spoken input language (Auto/KO/JA/EN) and target; swap arrows switch explicit languages.
- **Live**: repeat recording → transcription → translation → speech. This is sentence-by-sentence, not simultaneous low-latency streaming. Stop with the same button.
- **Chat** hides the translation language row. Normal chat uses automatic recognition and the current implementation requests Korean speech output; switching the UI flag alone does not set normal-chat reply language.
- **Quiet mode** mutes app reply speech, not microphone input or global Android volume.
- **Quick actions → Text size & voice speed**: 18/20/22/24/26sp and 0.75/1/1.25/1.5×. Speed applies to the next reply; settings persist.

For poor recognition, compare the exact spoken and recognized phrase, allow microphone startup, and compare the same phrase with the phone app. Changing TTS voice does not fix STT. Noise and device microphone placement matter.

## 8. Camera, typing and local library

| Feature | How to use / boundary |
|---|---|
| Photo question | Camera → capture → Ask Muse → enter question. It sends a resized JPEG; capture alone does not send it. |
| Photo saving | Choose Save explicitly to save in Android Pictures/Muse. Asking does not automatically save a gallery photo. |
| Photo text translation | Quick actions → Translate photo text → capture → target language. Translation is a Muse prompt, not offline OCR. |
| Follow-up photo | Quick actions → Follow up on photo → Ask with same photo. Resends the last image. It is memory-only; process death or Forget photo clears it. |
| Keyboard | Bottom-left keyboard icon. Enable Korean, Japanese and English in your Android keyboard (e.g. Gboard); switch with its globe or the Language button. No keyboard engine is bundled. |
| Quick actions | Editable prompts for conversation summary and simpler explanation. Review before sending. |
| Favorites | Quick actions → Search conversations → select a turn → Favorite. Favorites are independent saved copies. |
| Search | Searches locally displayed questions/answers and saved items; space-separated terms must all match. It does not search all remote Muse history. |
| Voice memo | Quick actions → Voice memos → Record (up to 20s) → transcribe with ElevenLabs → edit/save text locally. No audio archive and no automatic Muse submission. |
| Memo to tasks | Open saved memo → Make to-dos → confirm sending memo to Muse → review one task per line → Save list. Up to 50 tasks; original memo remains. Completion checks persist. No reminders/calendar integration. |
| Avatar | V2.0 uses the owner-provided Muse-generated dolphin in conversation and pet views. See CREDITS for the separate artwork notice. Account-avatar automatic synchronization is not implemented. |

## 9. LTE and troubleshooting

Rakuten Mobile Japan data worked on the owner's unit. It did not require a manual APN edit in that test. This does not establish carrier certification, voice/SMS support, roaming, or all regional bands. Check your carrier's current APN documentation if automatic setup fails. Prove LTE data by disabling Wi-Fi, confirming cellular is the active validated transport, and testing connectivity; a SIM icon is insufficient.

- **USB missing:** data cable/other port, unlocked display, USB debugging authorization, close browser USB tools. Distinguish adb, bootloader fastboot, and fastbootd drivers/modes.
- **dm-verity / power-off:** stop speculative flashing; see [case study](JOURNEY.md).
- **No side button:** verify keylayout and enabled/bound accessibility service. A successful APK install is not proof of physical button input.
- **No speech:** check quiet mode, media volume, ElevenLabs access/quota, or Android Korean TTS data if using fallback.
- **No picture understanding:** transport support and model availability can change. Source tests and demo screenshots do not prove a live visual answer.
- **Task extraction failed:** original memo remains. Retry explicitly; no invented checklist is saved automatically.

See [privacy/data flow](../../SECURITY.md), [verification](../development.md), and the [development journey](JOURNEY.md).

## Local alarms, timers and voice stop

Open **Quick actions → Alarms & timers**. All controls stay in a Muse panel: select hours/minutes, optional weekday repeat, or a 3/5/10-minute timer preset. Swipe the number wheels, then Save/Start. Timers also accept seconds. Pause, resume, delete, snooze five minutes and alarm volume are available. Initial Android permission approval is a system screen.

In conversation mode with ElevenLabs transcription configured, say “timer for 3 minutes”, “wake me at 7 am tomorrow” or “weekday alarm at 8 am”. Typed commands work too. Review the local confirmation and press Start. Supported command phrases are intercepted before sending text to Muse, so they schedule on r1, not a linked Mac. Interpretation mode does not execute them; without ElevenLabs use manual controls for local scheduling.

Schedules are stored on r1 and require no network after confirmation. The device must remain powered on; force-stop prevents alarms, and boot recovery requires unlock. Overdue one-shot schedules are marked off on recovery. Reboot recovery and end-to-end spoken commands remain unverified. Silent alarm volume/DND may suppress sound. Ringing stops automatically after five minutes; notification Stop stops all ringing items.

During speech preparation/playback, the bottom Quick actions button becomes **■ Stop voice**. It stops the current reply and queued audio but preserves the text; the next reply can speak normally. Voice replies default to ON. Orange speaker means ON; dark means OFF. Your explicit quiet-mode choice is retained.

## V1.5 — daily controls and a charging clock

- **Interrupt a spoken reply:** tap the side button to stop playback; hold it to stop playback and begin a new voice input. Release to send. This is button-operated interruption, not an always-listening wake word.
- **Schedule reminders:** Quick actions → schedule/appointment reminder opens a description, date and time editor inside Muse. Supported spoken/typed phrases open a confirmation screen before scheduling; unsupported dates need the manual editor. Reminders ring locally on the r1, including without an active Muse connection. They do not synchronize with a calendar account.
- **Home status:** see battery percentage/charging state and the next active alarm, reminder or timer. Tap the clock summary to manage it.
- **Charging desk clock:** enable it in Quick actions. After the selected idle delay while plugged in (20 seconds by default), Muse shows time, date/weekday, local weather icon, temperature, humidity and hourly forecast UV index. Tap to return. Display language follows English/Korean/Japanese settings. Weather uses approximate rounded coordinates with Open-Meteo; initial Android location approval and internet access are required. Missing values show a dash; weather failure does not stop the clock. Location is requested only while this screen is open, and weather refreshes every 15 minutes. No location history is stored.
- **Reading and screen controls:** conversation wheel scrolling stays inside the transcript; supported speech timestamps follow the spoken passage. Manual scrolling pauses following, and the down-arrow resumes it. Android TTS depends on engine range callbacks; unsupported cloud timestamp responses fall back to audio without precise tracking. Bold Markdown renders without literal `**`. Quick actions can keep the Muse screen awake; this is an app preference, not a global Android timeout change.

The V2.0 gallery uses the custom dolphin and fictional offline examples. Sample City, weather numbers, battery level and reminders in screenshots are fixtures. This release does not include navigation or Google Maps integration.

## Latest source additions

- **Reactive avatar:** Quick actions → Reactive avatar. Enabled by default; the existing character gently moves while idle and reacts to listening, thinking and speaking, with a small charging indicator. This does not infer emotion from speech. Turn the extra reactions off from the same menu.
- **Camera treasure hunt:** Quick actions → Camera treasure hunt. Find something red, round, a plant, a book or something striped. Choose another target, or take a photo, review it and explicitly send it to Muse for a short evidence-based judgment. Unclear photos should receive an uncertain answer. There is no automatic score or stored completion record. The mode strip reopens the controls or ends the activity.
- **Conversation partner:** choose Korean, Japanese or English, then cafe ordering, travel/directions or everyday chat. Selecting a scenario sends a start request to Muse. Reply using the side button or keyboard; prompts request a brief reply, one expression correction and one follow-up question. Voice practice requires ElevenLabs STT; keyboard practice needs only a Muse connection. Pronunciation is not scored. Alarm examples spoken during practice do not create real alarms. Tap the mode strip to change scenario or end practice. Starting practice turns off interpretation.
- **Clock idle delay:** Quick actions → Charging desk clock → Idle delay. Choose 10, 20 or 30 seconds, or 1, 2 or 5 minutes. The default is 20 seconds. The choice persists and counting restarts when settings close; idle/charging and no-active-recording/playback conditions still apply.

Validation: normal/demo builds and 94 unit tests passed. Development-device checks covered mode menus, target switching, language/scenario selection and an actual 10-second clock transition; the device was returned to its original 20-second setting. Live photo judging and conversation quality were not exercised for this update. The V1.5 release tag and its screenshot kit are unchanged; these additions are on the main branch.

Main-screen status now shows the active Wi-Fi/mobile connection and signal bars beside the battery. LTE, 5G, 3G and 2G labels follow the available radio information; unknown information is not displayed as a full signal. An exclamation mark indicates that internet access is not validated. VPN and Ethernet are identified separately. Updates run only while the main activity is foreground; no SSID, phone number or SIM identifier is stored. Development-device verification confirmed LTE signal 4/4 and validated internet; live Wi-Fi switching was not tested in this update. The offline demo uses a fictional fixed signal. Activity banners now use a content-sized height and dedicated padding to prevent clipped text.

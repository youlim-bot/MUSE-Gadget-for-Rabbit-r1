# Recovery and development journey

[English](JOURNEY.md) · [한국어](../ko/JOURNEY.md) · [日本語](../ja/JOURNEY.md) · [Install guide](GUIDE.md)

This is a sanitized account of the owner's October 5–6, 2026 work. Earlier upstream verification is not presented as a new test of this fork. Device IDs, backup paths, raw logs, credentials and private photos are intentionally omitted.

## Recovery before LineageOS

1. The unit could not enter fastbootd after older May 2025 (`v0.8.293`) boot/vbmeta images were mixed with an original September 2025-era firmware chain. The owner reported a dm-verity corruption warning; later, one boot attempt followed by power-off, rather than an endless loop.
2. The unit's own stock backup was checked against device identity and partition data. 52 of 53 checksum entries matched. The mismatched `frp-unlock.bin` was excluded, not treated as a valid restore image.
3. Original `boot_a/b` and `vbmeta_a/b`, `vbmeta_system_a/b`, `vbmeta_vendor_a/b` were restored and read back. The original `para` was also restored and checked.
4. The full `super` partition was read and matched the original backup. It was **not rewritten** just to test a theory.
5. A persistent managed-verity error state remained in this unit's authenticated V4 `seccfg`. Its state was examined and a narrowly scoped, authenticated correction was written and verified by full readback, keeping the bootloader unlocked. The owner then confirmed normal stock boot.
6. In the subsequent installation, fastbootd was confirmed with `is-userspace: yes`, slot A and logical `system_a`. The tested GSI/vbmeta procedure and intentional userdata/metadata wipe completed. The owner reached LineageOS setup, then installed and paired Muse.

**This is not a universal seccfg patch.** Do not zero arbitrary offsets, copy this unit's partition, relock a modified boot chain, or infer the same cause solely from a warning screen. Authentication, firmware and device state matter. No repair binary, hardware identity or one-click recovery tool is distributed.

The original video alone was not conclusively classified frame-by-frame. We do not use it as proof of a fastbootd screen. Observed warnings, mode queries, readbacks and the owner's boot confirmation form the evidence boundary.

References used during investigation: [firmware guide](https://github.com/TurboTheTurtle/rabbit-r1-firmware/blob/main/docs/flashing-guide.md), [mtkclient discussion #306](https://github.com/bkerler/mtkclient/issues/306), [community de-verity write-up](https://github.com/jonathanprocter/rabbit-r1-deverity-recovery/blob/main/docs/rabbit-r1-de-verity-recovery-community-writeup.md). They are background, not automatic authorization or a substitute for inspecting your unit.

## App evolution

| Stage | Added or changed | Evidence / remaining limit |
|---|---|---|
| Baseline | Native Home app, Muse pairing, encrypted credentials, side button, wheel volume, history | Derived from Cameron Pak's MIT project; retain its protocol notices. |
| Korean speech | Android Korean TTS, then optional ElevenLabs recognition/speech | Owner confirmed Korean speech. Voice output and recognition are different pipelines. |
| USB and LTE | USB cable/port/debug authorization, Rakuten Japan data | Cellular data was validated with Wi-Fi off; no claim about voice/SMS or all carriers. |
| Interpretation | KO/JA/EN input/target, direction swap, continuous sentence turns | Not simultaneous streaming; input row appears only in Translate mode. |
| Interface | Smaller controls, language flags, quiet mode, adjustable body text and speech speed | Persistent settings were checked on-device. Some legacy status/error strings are not fully localized. |
| Camera | Front/back camera, capture/review/save, photo question, text translation, last-photo follow-up | Camera selection physically rotated the unit. Wheel control and live visual answers need further hands-on/end-to-end checks. |
| Keyboard/avatar | Android IME support, custom local avatar, size adjustments | KO/JA/EN keyboard selection was configured on the owner's device. Account-avatar auto-sync was not implemented. V2.0 uses the owner-provided dolphin; earlier releases used the upstream avatar. |
| Library | Favorites, local search, voice-to-text memo | Memo capture is at most 20s; saved content is text, not an audio archive. |
| Tasks/settings | Reviewed memo-to-task extraction, checklist completion, 18–26sp and 0.75–1.5× | Source tests and settings persistence checked; live task quality and listening comparison remain user tests. |

The feature APK passed 67 JVM tests and was updated with encrypted credentials preserved. These checks do not replace live recognition, photo-answer, physical wheel, battery or network-failure testing. [Current public-export checks](../development.md#public-export-checks) are tracked separately.

## V2.0 publication

The virtual-pet room, local sound assets, settings reorganization, UV clarification and connection recovery changes are now included in the public source. Showcase images are newly captured in an isolated Android emulator using fictional data; they are not live R1 network tests. See [V2.0 notes](../RELEASE-V2.0.md).

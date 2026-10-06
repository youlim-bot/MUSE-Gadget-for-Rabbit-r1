# Source map and data flow

The native Android app remains package `dev.cameronpak.muser1` so same-key updates can preserve existing data. The offline showcase is a separate `dev.cameronpak.muser1.demo` package. This fork is an app, not RabbitOS or a custom ROM.

| Source | Responsibility |
|---|---|
| `MainActivity.kt`, `MuseScreen.kt`, `UiText.kt` | Main conversation, reserved controls, flags, prompts, avatar and state rendering |
| `SideButtonService.kt`, `SideButtonGesture.kt`, `hardware/mtk-kpd.kl` | Physical-button accessibility routing, hold/tap/volume; camera foreground routing |
| `VoiceRecorder.kt`, `SentenceBoundary.kt` | Bounded PCM/WAV capture and continuous-mode endpoint detection |
| `LanguageMode.kt` | Input/target language, translation prompt and swap behavior |
| `ElevenLabsClient.kt`, `SpeechOutput.kt`, `AndroidSpeechOutput.kt` | Optional STT/TTS requests, playback and Android fallback |
| `CameraActivity.kt`, `PhotoQuestion.kt`, `PhotoPrompts.kt`, `transport/PhotoPayload.kt` | Camera selection, capture/review, bounded image payload, translation and in-memory follow-up |
| `LibraryActivity.kt`, `LibraryStore.kt`, `TodoDraft.kt` | Local search/favorites/memos/checklists, task extraction validation |
| `ReadingSettings.kt` | Persistent conversation text size and playback speed |
| `CredentialStore.kt`, pairing and transport packages | Encrypted credentials, Bluetooth provisioning and Muse connection |
| `DisplayHistory.kt` | Ordered local text-history persistence; no deletion of remote conversation |

With ElevenLabs configured: microphone → Scribe → text → Muse → reply text → ElevenLabs speech. Quiet mode skips reply speech. Without configuration: voice goes through Muse and playback uses Android TTS. Switching voice output does not change recognizer behavior.

Continuous interpretation is sequential: listen, stop/endpoint, transcribe, translate, speak, listen again. Camera questions explicitly attach an optimized JPEG (maximum 1024px dimension, 180KB payload budget). The last photo exists only in process memory and is resent for each follow-up; it is not a persistent gallery reference.

Library favorites are independent snapshots. Task extraction sends a memo to Muse via a short-lived connection, accepts at most 50 bounded strings, asks the user to review/edit, then saves a separate checklist. Original text survives parsing/network failures. No automatic deadline inference, reminders or calendar access is implemented.

The demo build strips Internet/microphone/camera permissions and the accessibility service, skips account import/connection, and seeds only hard-coded fictional data. Its native UI uses the same view classes. Fixture content must not be confused with a live result.

Read [privacy](../SECURITY.md), [build/verification](development.md), and the language guides for behavior and limitations. Protocol/cryptography provenance remains next to the vendored source.

### Walking navigation

`NavigationActivity` embeds `GoogleMapCanvas`, a native Google Maps SDK MapView with lifecycle forwarding, automatic foreground position, gesture-aware following and route polylines. There is no navigation WebView or JavaScript bridge. Map long-press requires confirmation before requesting a route. The SDK key is injected from local configuration into the manifest; missing configuration produces a setup notice. Google attribution remains visible.

Android Geocoder supplies destination search. FOSSGIS/OSRM still calculates the pedestrian route; Google Routes API authentication is a separate pending integration. No driving or Japanese public-transit routing is claimed. Location and route data are not sent to Muse AI. External-service data handling is described in SECURITY.md.

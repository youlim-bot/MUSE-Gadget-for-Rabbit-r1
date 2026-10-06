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

Library favorites are independent snapshots. Task extraction sends a memo to Muse via a short-lived connection, accepts at most 50 bounded strings, asks the user to review/edit, then saves a separate checklist. Original text survives parsing/network failures. Checklist extraction does not infer deadlines or access external calendars. Separate local appointment reminders are available in V1.5.

The demo build strips Internet/microphone/camera permissions and the accessibility service, skips account import/connection, and seeds only hard-coded fictional data. Its native UI uses the same view classes. Fixture content must not be confused with a live result.

Read [privacy](../SECURITY.md), [build/verification](development.md), and the language guides for behavior and limitations. Protocol/cryptography provenance remains next to the vendored source.

## Local clock and speech controls

LocalClockCommand parses supported phrases before Muse text transport. ClockPanel is shared between a dialog over the conversation and the Muse-owned lock-screen ClockActivity. LocalClock stores schedules separately in app-private preferences and uses AlarmManager alarm-clock pending intents. ClockReceiver restores schedules on supported lifecycle broadcasts. ClockRingService plays alarm audio as a foreground service with a bounded wake lock. No clock data is sent over the network after scheduling.

MainActivity exposes the stop-voice control while SpeechOutput has queued/active playback, clears it via the existing stop path, and suppresses further speech for that conversation turn. It does not change the persisted quiet preference. Offline demos omit clock permissions/components and cannot schedule real alarms.

## V1.5 components

`ReminderCommand` parses supported reminder phrases and opens `ClockPanel` for confirmation. `ClockHomeStatus` supplies the home summary. `DeskClockDialog` owns the charging display and cancels its location/weather coroutine on dismissal. `ClockWeather` requests current temperature, humidity and weather code plus hourly UV index from [Open-Meteo](https://open-meteo.com/en/docs). UV selection matches the current Unix timestamp to its hourly interval; missing values remain unknown. `SpeechTiming` maps supported speech alignment to transcript offsets; manual scrolling suspends following.

## V2.0 pet and settings components

`PetState` implements deterministic five-minute simulation steps, hidden incubation, needs, health, temperament, adult forms and death. `PetStore` persists the simulation in app-private preferences and archives migrations/new eggs. `PetRoom`, `PetScene` and `PetMotion` render the room, animation and mini-games. `PetAudio` and `PetSoundtrack` manage local music/effects with speech priority. Agent requests remain owned by MainActivity and the existing Muse session; the pet never receives a second credential store. `MuseSettingsDialog` groups settings and opens Android's Wi-Fi panel without handling passwords.

ClockWeather now requests hourly UV plus daily maximum UV and keeps cache freshness bounded by the forecast hour. Connection diagnostics retain stage names and HTTP codes only, never tokens, response bodies or URLs. Reconnect attempts are bounded and do not silently replay an interrupted user request.

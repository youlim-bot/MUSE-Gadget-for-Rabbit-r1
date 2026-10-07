# MUSE Gadget for Rabbit r1 — V2.0

[**V2.0 release**](https://github.com/youlim-bot/MUSE-Gadget-for-Rabbit-r1/releases/tag/v2.0) · [Discord image kit](https://github.com/youlim-bot/MUSE-Gadget-for-Rabbit-r1/releases/download/v2.0/MUSE-r1-V2.0-Discord-kit.zip)

**[English guide](docs/en/GUIDE.md) · [한국어 가이드](docs/ko/GUIDE.md) · [日本語ガイド](docs/ja/GUIDE.md)**

A community Android Home app for a Rabbit r1 running LineageOS. Talk to Muse, interpret between Korean/Japanese/English, ask about photos, type messages, keep a searchable local library, and raise a virtual companion.

Based on [Cameron Pak's muse-r1](https://github.com/cameronapak/muse-r1), snapshot [`c0dd741`](https://github.com/cameronapak/muse-r1/commit/c0dd741f0896f85540625d52be01750672e00eca). This repository contains the extended source and a documented installation journey. It is not an official Rabbit, Meta, ElevenLabs, or LineageOS project, and it is **not a custom ROM**.

<p align="center">
  <img src="docs/images/en-chat.png" width="230" alt="English chat with fictional offline sample data">
  <img src="docs/images/ko-translate.png" width="230" alt="Korean to Japanese interpretation with fictional offline sample data">
  <img src="docs/images/en-tasks.png" width="230" alt="Local checklist with fictional sample data">
</p>

**Screenshots:** actual Android views captured on a disposable 480×640 emulator using a separate offline demo package. All conversations, translations, photo answers and tasks are fixtures, not live AI results. The demo has no Internet, microphone or camera permission. [Gallery and Discord captions](docs/SHOWCASE.md).

## Start here

| | English | 한국어 | 日本語 |
|---|---|---|---|
| Installation, setup, features, troubleshooting | [Guide](docs/en/GUIDE.md) | [가이드](docs/ko/GUIDE.md) | [ガイド](docs/ja/GUIDE.md) |
| Recovery case and development journey | [Journey](docs/en/JOURNEY.md) | [설치·개발 기록](docs/ko/JOURNEY.md) | [導入・開発記録](docs/ja/JOURNEY.md) |

- [Source map and data flow](docs/architecture.md)
- [Build, verification and demo capture](docs/development.md)
- [Privacy and security](SECURITY.md) · [Credits and asset license](CREDITS.md)

## New in V2.0 — Raise your Muse

An animated egg becomes a companion over days. Feed, play, care and talk together: your choices shape its personality and adult form. A dedicated pet-room interface keeps Muse's agent controls within reach. Local care and music work offline; assistant answers still need Muse connectivity.

- Six foods, three mini-games, sleep, cleaning and medicine; neglect has consequences.
- Hidden growth requirements, multiple life stages and branching adult forms.
- Three local BGM choices, growth-aware Auto, birdlike effects and speech priority.
- Custom dolphin avatar generated with Muse, without a reference image, as reported by the project owner.
- A dedicated Settings menu, reorganized quick actions and Camera → Keyboard → Quick actions → Sound → Settings toolbar.
- Current/peak UV clarity, network status, bounded reconnection and re-pairing guidance.
- Reactive avatar, camera treasure hunt and Korean/Japanese/English conversation practice.

[**V2.0 release notes — English / 한국어 / 日本語**](docs/RELEASE-V2.0.md)

<p align="center">
  <img src="docs/images/en-pet-egg.png" width="230" alt="V2.0 egg — offline demo">
  <img src="docs/images/en-pet-adult.png" width="230" alt="V2.0 grown companion — simulated age">
  <img src="docs/images/en-pet-agent.png" width="230" alt="Muse agent inside pet mode — fictional response">
</p>

Alarms, timers, appointment reminders, speech-following scroll and the weather desk clock remain included from [V1.5](docs/RELEASE-V1.5.md). Navigation remains removed.

## Latest source updates

Pet mode now includes **Special activities**: solo expeditions, a daily diary,
a memory chest, earned room decorations, photo missions and daily R1 promises.
It also has state-aware Muse replies and animations, shared reply mute and
side-button + wheel volume. Conversation avatar effects are larger, and startup
checks the existing access token before requesting a refresh.

[Special activities and usage limits](docs/special-activities.md) ·
[Validation details](docs/development.md#six-special-activities-2026-10-07).
These are source updates after the existing V2.0 release; the release APK and
showcase images have not been replaced by this commit.

## Features

- **Local r1 alarms and timers:** in-Muse controls, multilingual command confirmation, weekday alarms, timer presets, pause/resume and snooze. Scheduling stays on r1; initial Android permissions are required.
- **Stop voice:** interrupt the current reply without deleting text. Voice defaults to ON, shown by an orange speaker.

- Side-button push-to-talk.
- **Physical volume control:** hold the side button and turn the wheel; an on-screen indicator shows the media volume.
- Korean, Japanese and English interface; separate speech-input and translation-target controls.
- Sentence-by-sentence continuous interpretation, with visible start/stop controls.
- Optional ElevenLabs recognition and speech; Android TTS fallback when ElevenLabs is not configured.
- Camera capture, front/back switching, camera wheel routing, photo questions, text translation, and follow-up on the last photo.
- Keyboard input through the Android keyboard/IME; quiet mode; editable quick questions.
- Favorites, local conversation search, voice-to-text memos, reviewed memo-to-task checklists.
- Persistent conversation text size (18–26sp) and speech speed (0.75–1.5×).

## Volume control · 볼륨 조절 · 音量調整

**English:** With Muse open and the screen unlocked, **hold the side button and turn the scroll wheel**. Up raises media volume; down lowers it. An on-screen speaker indicator shows the level. Once the wheel moves, that button press controls volume: any recording started by the hold is discarded, and releasing the button does not send it or lock the screen.

**한국어:** Muse 화면이 열리고 잠금이 해제된 상태에서 **측면 버튼을 누른 채 스크롤 휠을 돌리세요.** 위로 돌리면 미디어 볼륨이 커지고, 아래로 돌리면 작아집니다. 화면에 스피커와 볼륨 단계가 표시됩니다. 휠을 돌린 누름은 볼륨 조작으로 처리하므로, 이미 시작된 녹음은 버리고 버튼을 떼어도 전송하거나 화면을 잠그지 않습니다.

**日本語:** Museを開き、画面のロックを解除した状態で、**サイドボタンを押しながらスクロールホイールを回します。** 上でメディア音量を上げ、下で下げます。画面のスピーカー表示で音量を確認できます。ホイールを動かした操作は音量調整として扱われ、開始済みの録音は破棄されます。ボタンを離しても送信や画面ロックは行いません。

| | English | 한국어 | 日本語 |
|---|---|---|---|
| Setup | Enable **Side button controls** and the keylayout mapping described in the guide. | 가이드의 버튼 매핑과 **Side button controls** 활성화가 필요합니다. | ガイドのボタン割り当てと **Side button controls** の有効化が必要です。 |
| Scope | Adjusts Android media volume. Quiet mode separately controls whether Muse reads replies aloud. | Android 미디어 볼륨을 조절합니다. 조용한 모드는 Muse 답변을 읽을지 별도로 정합니다. | Androidのメディア音量を調整します。サイレントモードはMuseの回答読み上げを別途切り替えます。 |
| Camera | Camera mode uses the wheel for front/back camera selection instead. | 카메라 모드에서는 휠을 전면·후면 전환에 사용합니다. | カメラモードではホイールを前面・背面の切り替えに使います。 |

## Build

Java 17, Android SDK platform 36, and network access for dependency downloads are required.

```sh
git clone https://github.com/youlim-bot/MUSE-Gadget-for-Rabbit-r1.git
cd MUSE-Gadget-for-Rabbit-r1
# Set ANDROID_HOME to your SDK directory, or use an ignored local.properties.
./gradlew :app:assembleDebug :app:testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Keep your signing key stable. `install -r` preserves data only when Android accepts the package/signature; **do not uninstall a paired app to work around a signature mismatch**. No private signing key or prebuilt production APK is included.

## Verification boundaries

The app was developed on a LineageOS 21 / Android 14 Rabbit r1. V2.0 public-source checks and emulator captures are listed in [verification](docs/verification.json). These captures do not demonstrate live assistant replies, physical camera rotation, outdoor weather accuracy or recovered pairing. Pet time is simulated for lifecycle tests; real multi-day observation remains separate. Continuous interpretation is turn-based, not simultaneous streaming translation.

Firmware flashing wipes data and may prevent booting. Follow the guide's firmware/slot/fastbootd checks; never apply this unit's recovery state to another unit. Firmware images, stock backups, identifiers, credentials, personal screenshots and personal data are excluded.

한국어: LineageOS 설치부터 Muse 연결, 한국어·일본어·영어 통역, 사진 질문, 키보드, 메모·할 일까지 정리한 커뮤니티 프로젝트입니다. 위의 한국어 가이드에서 설치와 사용법을 확인하세요.

日本語: LineageOSの導入からMuseとの接続、韓国語・日本語・英語の通訳、写真への質問、キーボード、メモ・タスクまでをまとめたコミュニティプロジェクトです。日本語ガイドをご覧ください。

## License

First-party code/docs: [MIT](LICENSE), preserving Cameron Pak's notice. Vendored protocol and cryptography files retain their own licenses and provenance. V2.0 uses the owner-provided Muse-generated dolphin in both conversation and pet views. Artwork is excluded from the code MIT license; this repository does not establish exclusive copyright or independently verify the generating service’s output terms. Original upstream character artwork is removed from the current tree. See [CREDITS](CREDITS.md).

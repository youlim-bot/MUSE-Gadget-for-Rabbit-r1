# Credits and licenses

- **Cameron Pak — [muse-r1](https://github.com/cameronapak/muse-r1)**: native Android Home app, pairing, transport, display history, physical-button foundation, tests and original installation research. Export based on commit `c0dd741f0896f85540625d52be01750672e00eca`. The original [MIT notice](LICENSE) is preserved.
- **youlim-bot customization**: Korean/Japanese/English UI and interpretation, ElevenLabs integration, camera/photo workflow, keyboard, library, tasks, reading settings, trilingual documentation and offline showcase.
- **Meta Muse SDK-derived transport**: see [transport provenance](app/src/main/java/dev/cameronpak/muser1/transport/PROVENANCE.md) and in-file Apache-2.0 notices.
- **Noise Java**: see [cryptography provenance and license](app/src/main/java/com/southernstorm/noise/PROVENANCE.md); preserve vendored headers and license files.
- **Firmware references**: [r1_escape](https://github.com/RabbitHoleEscapeR1/r1_escape), [TurboTheTurtle's archive](https://github.com/TurboTheTurtle/rabbit-r1-firmware), [Andy Yan GSI builds](https://sourceforge.net/projects/andyyan-gsi/), Android GSI/platform-tools and the community recovery sources linked in the guides. No firmware is redistributed.

## Artwork in V2.0

`app/src/main/res/drawable-nodpi/muse_dolphin.png` was supplied by the project owner, who reports that Muse generated it without an uploaded reference image. The owner requested its publication for this non-commercial community customization. It is used in conversation and virtual-pet views. Automatic account-avatar synchronization is not implemented.

The image is excluded from the MIT code license. Publication here is not a representation of exclusive copyright or an independent verification of Muse's generated-output terms. Do not interpret this credit as an unrestricted third-party artwork license. The original upstream `muse_character.png` is no longer included in the current source tree; older tags/history retain their original provenance and notices.

한국어: 프로젝트 소유자가 Muse에서 참고 이미지 없이 생성했다고 설명한 돌고래를 요청에 따라 공개했습니다. 이미지는 MIT 코드 라이선스에서 제외하며, 독점 저작권이나 생성 서비스 약관을 별도로 확인했다는 의미는 아닙니다. 현재 대화·육성 화면은 같은 돌고래를 사용합니다.

日本語: 所有者がMuseで参照画像なしに生成したと説明するイルカを、依頼に基づき公開しています。画像はMITコードライセンスの対象外で、独占的著作権や生成サービス規約を独自に確認したという意味ではありません。会話・育成画面で共通して使用します。

## Local music and effects

`pet_*.wav` files are synthesized by the included `scripts/make-pet-audio.py` using mathematical tones/noise. No third-party recordings or song samples are used by that generator. These project-created sound assets are distributed under the repository MIT license.

## Screenshots and services

V2.0 screenshots show native Android views on an isolated emulator with fictional data and simulated pet ages. They are not live model responses or real weather measurements. Product names identify the services involved; this is not an official Rabbit or Meta product, nor affiliated with Bandai or Tamagotchi.

Weather data: [Open-Meteo](https://open-meteo.com/), [CC BY 4.0 attribution](https://open-meteo.com/en/terms). Public screenshots use fictional weather.

# Credits and licenses

- **Cameron Pak — [muse-r1](https://github.com/cameronapak/muse-r1)**: native Android Home app, pairing, transport, display history, physical-button foundation, tests and original installation research. Export based on commit `c0dd741f0896f85540625d52be01750672e00eca`. The original [MIT notice](LICENSE) is preserved.
- **youlim-bot customization**: Korean/Japanese/English UI and interpretation, ElevenLabs integration, camera/photo workflow, keyboard, library, tasks, reading settings, trilingual documentation and offline showcase.
- **Meta Muse SDK-derived transport**: see [transport provenance](app/src/main/java/dev/cameronpak/muser1/transport/PROVENANCE.md) and in-file Apache-2.0 notices.
- **Noise Java**: see [cryptography provenance and license](app/src/main/java/com/southernstorm/noise/PROVENANCE.md); preserve vendored headers and license files.
- **Firmware references**: [r1_escape](https://github.com/RabbitHoleEscapeR1/r1_escape), [TurboTheTurtle's archive](https://github.com/TurboTheTurtle/rabbit-r1-firmware), [Andy Yan GSI builds](https://sourceforge.net/projects/andyyan-gsi/), Android GSI/platform-tools and the community recovery sources linked in the guides. No firmware is redistributed.

## Artwork

`app/src/main/res/drawable-nodpi/muse_character.png` is the original default avatar from [Cameron Pak's muse-r1 at c0dd741](https://github.com/cameronapak/muse-r1/blob/c0dd741f0896f85540625d52be01750672e00eca/app/src/main/res/drawable-nodpi/muse_character.png), copied without pixel changes at the owner's request. The upstream documentation describes it as generated from a supplied Muse avatar reference and explicitly excludes it from the MIT code license; redistribution rights were not established there. Inclusion here does not grant a new license to that illustration or imply official endorsement. Check the applicable artwork rights before further redistribution.

The owner's private dolphin/reference images are not included. The previous geometric placeholder and its generator have been removed. Automatic Muse account-avatar synchronization is not implemented.

Screenshots show real native app views with fictional offline fixtures and the restored upstream default avatar. They are not evidence of live model responses. Product names identify the services involved; this is not an official Rabbit or Meta product.

한국어: 원본 Muse r1의 기본 아바타를 픽셀 변경 없이 복원했습니다. 원본 문서에서 이 이미지는 MIT 코드 라이선스 대상이 아니며 재배포 권리가 확정되지 않았다고 명시합니다. 이 저장소가 이미지에 새로운 이용 허락을 부여하는 것은 아닙니다. 개인 돌고래 이미지는 포함하지 않습니다.

日本語: 元のMuse r1の標準アバターを画像変更なしで復元しました。上流資料では、この画像はMITコードライセンスの対象外で再配布権は未確定とされています。このリポジトリは画像に新たな利用許諾を付与しません。個人のイルカ画像は含めません。

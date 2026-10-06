# 설치 및 사용 가이드

[English](../en/GUIDE.md) · [한국어](GUIDE.md) · [日本語](../ja/GUIDE.md) · [처음으로](../../README.md)

## 1. 적용 범위와 준비

Rabbit r1 한 대에서 진행한 설치 기록입니다. 공식 지원이나 모든 펌웨어에서의 성공을 보장하지 않습니다. 사용한 시스템은 **LineageOS 21 / Android 14, `lineage-21.0-20250621-UNOFFICIAL-arm64_bgN`**입니다. 기존 커널·vendor 위에 GSI를 설치했으며, 이 저장소는 커스텀 ROM이 아닌 Android 앱 소스입니다.

플래시는 사용자 데이터를 지우며 부팅 불능을 일으킬 수 있습니다. **본인 기기**의 원본 펌웨어·파티션 정보를 백업하고 체크섬을 검증하세요. 통신·보정·식별 파티션은 공개하거나 다른 기기에 복원하지 마세요. 이 GSI/vbmeta 조합에서는 부트로더를 다시 잠그지 않습니다. 정상 부팅은 최신 보안 지원을 뜻하지 않습니다.

데이터 전송용 USB 케이블, macOS 또는 명령을 조정한 호스트, Android platform-tools, 여유 공간 5GB 이상이 필요합니다. 앱 빌드에는 Java 17과 Android SDK platform 36이 필요합니다. Mac의 Homebrew에서는 `android-platform-tools`를 설치할 수 있고 SDK는 Android Studio로 준비할 수 있습니다. 대상 r1만 연결하고 브라우저 WebUSB 플래셔는 닫으세요.

부트로더 언락은 별도 준비 과정입니다. [r1_escape](https://github.com/RabbitHoleEscapeR1/r1_escape), [Android GSI 문서](https://source.android.com/docs/core/tests/vts/gsi), [커뮤니티 설치 사례](https://substrate.dougbelshaw.com/rabbit-r1-android), [펌웨어 플래싱 가이드](https://github.com/TurboTheTurtle/rabbit-r1-firmware/blob/main/docs/flashing-guide.md)를 확인하세요. 펌웨어별 차이가 있으므로 언락 절차를 임의로 일반화하지 않습니다.

`dm-verity corruption`, 한 번 부팅 후 꺼짐, fastbootd 진입 실패 상태라면 [복구 사례](JOURNEY.md)를 먼저 읽으세요. 구버전 boot/vbmeta를 반복해서 쓰는 것은 해결 절차가 아닙니다.

## 2. 이미지 다운로드 및 검증

새 작업 폴더에서 실행합니다. 아래는 재현성을 위한 과거 빌드이며 최신·최고 보안 버전이라는 뜻이 아닙니다.

```sh
curl -fL --retry 2 -o lineage.img.gz \
 'https://downloads.sourceforge.net/project/andyyan-gsi/lineage-21-td/lineage-21.0-20250621-UNOFFICIAL-arm64_bgN-signed.img.gz'
echo '2ad81102b6902737c182d791f0f88c0c1e31aa11e7f7d0c3e3864c49b04a4aa6  lineage.img.gz' | shasum -a 256 -c -
gzip -t lineage.img.gz
# 두 검사가 모두 성공한 뒤에만 압축 해제
gzip -dc lineage.img.gz > lineage.img
curl -fL -o google-gsi-vbmeta.img \
 'https://dl.google.com/developers/android/qt/images/gsi/vbmeta.img'
echo 'f6da5489fd877cb69cf61fa721cfd6d77e530084aefe9b96664f818947ff61f6  google-gsi-vbmeta.img' | shasum -a 256 -c -
```

오류가 나오면 중단합니다. 해시는 설치 당시 계산한 파일 지문이며 배포자의 독립 서명 검증은 아닙니다. 압축 해제한 system은 3,090,542,592바이트, vbmeta는 4,096바이트였습니다. 펌웨어 파일은 이 저장소에 포함하지 않습니다.

## 3. 모드·슬롯 확인 및 LineageOS 설치

언락 프로젝트의 절차로 bootloader fastboot에 들어갑니다. Android가 정상 실행되고 USB 디버깅이 승인되어 있다면 `adb reboot bootloader`를 사용할 수 있습니다.

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

**필수 조건:** 대상 1대, 부트로더 언락, fastbootd의 `is-userspace: yes`, 논리 파티션 `system_a`, 양쪽 모드에서 일관된 슬롯 `a`. 하나라도 다르면 중단합니다. 화면의 `FASTBOOT` 글자만으로 fastbootd 진입을 판단하지 않습니다. 슬롯 B나 다른 파티션 구조는 이 절차의 범위 밖입니다.

bootloader fastboot에서:

```sh
fastboot flash vbmeta_a google-gsi-vbmeta.img
fastboot reboot fastboot
fastboot getvar is-userspace
fastboot getvar current-slot
# yes / a 확인 후에만 진행
fastboot -S 100M flash system_a lineage.img
```

모든 쓰기가 `OKAY`여야 합니다. 실제 설치는 sparse 전송 30회로 끝났습니다. `FAILED`나 크기 조정 오류가 나면 다른 논리 파티션을 지우지 말고 중단합니다. 검증한 Google vbmeta는 이미 검증 해제 상태여서 그대로 기록했습니다. 성공한 이 GSI 설치 과정에서는 boot/vendor/modem/preloader/슬롯 B를 쓰지 않았습니다.

**아래 초기화는 userdata와 암호화 메타데이터를 영구 삭제합니다.**

```sh
fastboot reboot bootloader
fastboot getvar current-slot
# a 확인 및 데이터 삭제를 수용한 뒤 실행
fastboot -w
fastboot reboot
```

초기 설정 화면까지 기다려 Wi-Fi, 화면 보안, 개발자 옵션 → USB 디버깅을 설정하고 Mac을 승인합니다.

```sh
adb devices
adb shell getprop ro.build.version.release
adb shell getprop ro.lineage.version
adb shell getprop ro.boot.slot_suffix
```

확인값은 Android `14`, 위의 Lineage 빌드, `_a`입니다.

## 4. Muse 빌드·설치

```sh
git clone https://github.com/youlim-bot/MUSE-Gadget-for-Rabbit-r1.git
cd MUSE-Gadget-for-Rabbit-r1
# Java 17 및 ANDROID_HOME 설정 후
./gradlew :app:assembleDebug :app:testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n dev.cameronpak.muser1/.MainActivity
```

업데이트에는 동일한 서명 키가 필요합니다. Mac이 바뀌면 debug 서명 키도 달라질 수 있으므로 개인적으로 보관하세요. 서명 불일치를 해결하려고 페어링된 앱을 삭제하지 마세요. `install -r`는 호환 서명일 때 데이터를 보존하며, 앱 삭제·데이터 초기화는 페어링과 로컬 기록을 지웁니다.

## 5. SDK 토큰과 페어링

본인의 [Muse SDK 토큰](https://gadgets.muse.ai/settings/sdk-tokens)을 발급받고 [이용 조건](https://gadgets.muse.ai/sdk-terms)을 확인하세요. 지원 계정과 휴대폰 Muse 앱이 필요하며 접근 가능 여부는 달라질 수 있습니다. 이미 페어링되어 있으면 재등록하지 않습니다.

다음은 **Bash**에서 실행합니다. 토큰을 명령줄에 직접 적지 않습니다.

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

앱은 Android Keystore AES-GCM으로 암호화하고 임시 파일을 삭제합니다. 토큰이나 페어링 정보가 보이는 화면은 공개하지 않습니다.

R1의 빈 배경 길게 누르기 → **Muse 페어링** → Bluetooth 허용. 휴대폰 Muse의 Settings → Devices에서 Developer mode를 켜고 R1에 표시된 이름과 같은 기기를 추가합니다. 페어링 대기 시간은 2분이며 R1은 자신의 Wi-Fi/LTE 연결을 사용합니다.

## 6. 측면 버튼·휠·화면

이미 keylayout이 적용되어 있다면 반복 설치하지 않습니다. 아래 작업은 측면 버튼의 기본 전원 동작을 바꿉니다. 처음 적용할 때만 LineageOS **Rooted debugging**을 잠시 켭니다.

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

`mtk-kpd`가 `/data/system/devices/keylayout/mtk-kpd.kl`을 쓰는지 확인하고 **Rooted debugging을 끕니다**. Android 접근성에서 **Side button controls**를 활성화합니다. 제한된 설정 경고가 나오면 앱 정보에서 제한된 설정 허용 후 다시 켭니다. 이 서비스는 버튼 입력을 처리하며 화면 내용 읽기 권한을 요청하지 않습니다.

홈 앱 지정은 선택 사항입니다.

```sh
adb shell cmd package set-home-activity dev.cameronpak.muser1/.MainActivity
```

- 측면 버튼 길게: 녹음, 떼기: 전송. 짧게: Android 잠금/화면 끄기.
- 측면 버튼을 누른 채 휠: 미디어 볼륨 조정. 해당 누름에서는 녹음을 전송하지 않습니다.
- 카메라 화면: 측면 버튼 촬영, 휠 위·아래는 후면·전면 전환 요청. 전면/후면 전환 시 실제 회전은 사용자 확인을 받았고, 휠 반응·방향은 별도 실기기 확인이 필요합니다.
- 480×640 화면에서 토글이 잘리면 실제 사용한 배율은 `adb shell wm density 200`입니다. 원복은 `adb shell wm density reset`입니다.

화면 꺼짐과 화면 잠금은 별개입니다. 잠금 해제를 생략하려면 Android 설정 → 보안 → 화면 잠금 → 없음을 직접 선택할 수 있습니다(메뉴명은 빌드별 차이). 기기 보호가 해제되며 앱이 PIN을 자동으로 삭제하지 않습니다. 화면 꺼짐 시간은 유지할 수 있고 같은 메뉴에서 PIN을 복구할 수 있습니다.

버튼 원복은 Rooted debugging을 잠시 켜고 `adb root` 실행 후 `/data/system/devices/keylayout/mtk-kpd.kl`만 삭제 → 재부팅 → 기존 keylayout 사용 확인 → Rooted debugging 끄기 순서입니다.

## 7. ElevenLabs·언어·음성

선택 기능인 ElevenLabs는 STT에 `scribe_v2`(`/v1/speech-to-text`), TTS에 `eleven_v4`(`/v1/text-to-dialogue`)를 요청합니다. 이는 소스에 구현된 식별자이며 모든 계정에서 사용 가능하다는 뜻은 아닙니다. [공식 문서](https://elevenlabs.io/docs)와 본인 계정의 모델·음성 접근 권한을 확인하세요. API 비용이 발생할 수 있습니다.

R1 한 대를 연결한 뒤 `python3 scripts/provision-elevenlabs.py`를 실행합니다. API 키는 숨김 입력, Voice ID는 본인의 음성을 입력합니다. 모델·음성 접근을 확인한 후 앱 전용 임시 파일을 거쳐 암호화 저장합니다. 음성 생성은 실행하지 않습니다. 라이브러리 음성은 필요하면 먼저 본인 계정에 추가하세요. STT/TTS 생성, Models/Voices 읽기 권한이 필요합니다. **HTTP 400 자체를 권한 문제로 단정하지 않습니다.** `voice_not_found`는 해당 계정에서 Voice ID를 쓸 수 없다는 경우도 포함합니다.

- 오른쪽 위 국기: 표시 언어 한국어·일본어·영어.
- **통역**: 입력 언어 자동/한국어/일본어/영어와 번역 언어를 선택. 방향 교환은 입력 언어가 명시되어 있을 때 사용합니다.
- **연속 통역**: 녹음 → 인식 → 번역 → 재생을 반복합니다. 문장 단위이며 동시 스트리밍 통역은 아닙니다. 같은 버튼으로 중지합니다.
- **대화**에서는 통역 언어 줄을 숨깁니다. 현재 일반 대화는 자동 언어 인식과 한국어 음성 출력을 요청합니다. 표시 언어 국기를 바꾸는 것만으로 일반 답변 언어가 바뀌지는 않습니다.
- **조용한 모드**: 앱의 답변 음성만 끕니다. 마이크나 Android 전체 볼륨을 끄지 않습니다.
- **빠른 질문 → 글자 크기·음성 속도**: 18/20/22/24/26sp, 0.75/1/1.25/1.5배. 다음 음성부터 적용하고 설정은 유지합니다.

인식이 나쁘면 정확한 발화와 인식 결과를 비교하고, 마이크 시작을 기다린 뒤 같은 문장을 휴대폰 앱에서도 비교하세요. 목소리(TTS)를 바꿔도 음성 인식(STT)이 개선되는 것은 아닙니다.

## 8. 사진·키보드·보관함

| 기능 | 사용법과 범위 |
|---|---|
| 사진 질문 | 카메라 → 촬영 → Muse에게 질문 → 질문 입력. 크기를 줄인 JPEG를 보냅니다. 촬영만 하면 전송하지 않습니다. |
| 사진 저장 | 저장 버튼을 선택할 때 Android Pictures/Muse에 저장합니다. 질문했다고 갤러리에 자동 저장하지 않습니다. |
| 사진 속 글자 번역 | 빠른 질문 → 사진 속 글자 번역 → 촬영 → 대상 언어. Muse에 번역을 요청하며 오프라인 OCR은 아닙니다. |
| 사진 후속 질문 | 빠른 질문 → 이전 사진에 이어 질문 → 같은 사진으로 질문. 같은 이미지를 다시 전송합니다. 메모리에만 보관하므로 앱 프로세스 종료 또는 사진 기억 지우기로 사라집니다. |
| 키보드 | 하단 왼쪽 키보드 버튼. Android 키보드(예: Gboard)에 한·일·영을 추가하고 지구본/입력 언어 버튼으로 전환합니다. 앱 자체 키보드는 포함하지 않습니다. |
| 빠른 질문 | 대화 요약·쉬운 설명 문구를 수정한 뒤 전송할 수 있습니다. |
| 즐겨찾기 | 빠른 질문 → 대화 검색 → 항목 선택 → 즐겨찾기. 화면 기록과 별개의 복사본으로 저장합니다. |
| 대화 검색 | 기기에 있는 질문·답변·저장 항목을 검색합니다. 띄어쓰기로 나눈 검색어가 모두 포함되어야 합니다. 원격 Muse 전체 기록 검색은 아닙니다. |
| 음성 메모 | 최대 20초 녹음 → ElevenLabs로 문자 변환 → 확인·수정 → 기기 저장. 음성 파일 보관이나 자동 Muse 전송은 하지 않습니다. |
| 메모 → 할 일 | 저장된 메모 → 할 일로 정리 → Muse에 전송 확인 → 한 줄에 하나씩 검토 → 목록 저장. 최대 50개, 원본 메모 유지, 완료 체크 저장. 알림·캘린더 기능은 없습니다. |
| 아바타 | 공개판은 원본 Muse r1 프로젝트의 기본 아바타를 사용합니다(CREDITS의 이미지 권리 안내 참조). 개인 기기의 돌고래는 로컬 이미지 적용이며 Muse 계정 아바타 자동 동기화가 아닙니다. 배포 권한이 있는 이미지로 교체하세요. |

## 9. LTE와 문제 해결

사용자 기기에서는 일본 Rakuten Mobile 데이터 연결이 확인되었고 해당 테스트에서 APN 수동 변경이 필요하지 않았습니다. 통신사 인증, 통화·SMS·로밍·모든 주파수 호환성을 확인한 것은 아닙니다. 자동 연결 실패 시 통신사의 최신 APN 안내를 확인하세요. Wi-Fi를 끄고 셀룰러가 실제 기본·검증된 연결인지 확인한 뒤 통신을 시험해야 합니다. SIM 아이콘만으로 LTE 성공을 판단하지 않습니다.

- **USB 인식 실패:** 데이터 케이블·포트, 잠금 해제, USB 디버깅 승인, WebUSB 종료. adb/bootloader/fastbootd를 구분합니다.
- **dm-verity·전원 꺼짐:** 추측 플래시를 멈추고 [복구 사례](JOURNEY.md)를 확인합니다.
- **측면 버튼 무반응:** keylayout, 접근성 서비스의 활성·연결 상태를 확인합니다.
- **음성 없음:** 조용한 모드, 볼륨, ElevenLabs 권한·한도, 대체 Android TTS 한국어 데이터를 확인합니다.
- **사진 답변 실패:** 서버·모델 지원이 바뀔 수 있습니다. 테스트 통과와 더미 캡처는 실시간 사진 이해 성공의 증거가 아닙니다.
- **할 일 추출 실패:** 원본 메모는 유지하며 사용자가 다시 시도합니다. 가짜 결과를 자동 저장하지 않습니다.

[데이터 처리](../../SECURITY.md) · [검증 범위](../development.md) · [설치·개발 기록](JOURNEY.md)

## 도보 길찾기

**빠른 질문 → 도보 길찾기**를 여세요(메뉴 맨 아래). **Muse 앱 안에 지도**가 표시됩니다. 앱 사용 중 정확한 위치를 허용하면 파란 점이 이동에 따라 갱신됩니다. 목적지 이름·주소를 입력하고 **찾기**를 누른 뒤 검색 결과를 선택하세요. 지도를 길게 눌러 목적지를 정할 수도 있습니다. 주황색 도보 경로와 남은 거리·예상 시간·다음 방향이 표시됩니다. 하단 안내를 누르면 전체 방향 목록을 볼 수 있습니다. **내 위치 / 전체 경로 / 재탐색** 버튼으로 조작합니다.

화면을 보는 동안의 시각 안내 기능입니다. 음성·백그라운드 안내와 자동 재탐색은 없습니다. 경로를 벗어나면 재탐색을 누르세요. 화면을 끄거나 지도를 벗어나면 위치 추적을 중단하고 돌아올 때 재개합니다. 2분 이내·정확도 100m 이내 위치를 사용하며 GPS·지도·검색 품질은 환경에 따라 달라집니다. 검색에는 Android Geocoder 제공자가 필요합니다. 검색이 불가능하면 지도를 길게 눌러 선택하세요.

지도는 OpenStreetMap, 도보 경로는 FOSSGIS/OSRM의 보행자 전용 서버를 사용합니다. 인터넷 연결이 필요하며 서비스 가용성을 보장하지 않습니다. 화면에 보이는 지도만 불러와 HTTP 캐시를 사용하고 오프라인 다운로드는 제공하지 않습니다. 경로는 사용자가 요청할 때만 계산하며 요청 간격을 제한합니다. 대규모 배포 전에는 서비스 용량을 별도로 확보해야 합니다.

**위치 처리:** 검색어는 Android 검색 제공자에게, 지도 표시 지역·IP는 지도 서비스에, 경로 요청 시 출발지·목적지 좌표는 FOSSGIS에 전달됩니다. 서비스 측 로그가 남을 수 있습니다. Muse AI에는 보내지 않습니다. 앱의 위치·경로·검색 결과는 메모리에만 보관하며 지도 타일은 WebView 캐시에 남을 수 있습니다. **정보**에서 안내와 위치 설정을 확인하세요. Google 지도 앱이나 API 키는 필요 없습니다. 오프라인 데모는 실제 지도를 열지 않습니다.

> Historical V1.5 text. For current images and behavior, see [V2.0](RELEASE-V2.0.md). The original V1.5 kit remains attached to its release tag.

# MUSE Gadget for Rabbit r1 — V1.5

2026-10-06

## V1.5 — daily controls and a charging clock

- **Interrupt a spoken reply:** tap the side button to stop playback; hold it to stop playback and begin a new voice input. Release to send. This is button-operated interruption, not an always-listening wake word.
- **Schedule reminders:** Quick actions → schedule/appointment reminder opens a description, date and time editor inside Muse. Supported spoken/typed phrases open a confirmation screen before scheduling; unsupported dates need the manual editor. Reminders ring locally on the r1, including without an active Muse connection. They do not synchronize with a calendar account.
- **Home status:** see battery percentage/charging state and the next active alarm, reminder or timer. Tap the clock summary to manage it.
- **Charging desk clock:** enable it in Quick actions. After 20 seconds of idle time while plugged in, Muse shows time, date/weekday, local weather icon, temperature, humidity and hourly forecast UV index. Tap to return. Display language follows English/Korean/Japanese settings. Weather uses approximate rounded coordinates with Open-Meteo; initial Android location approval and internet access are required. Missing values show a dash; weather failure does not stop the clock. Location is requested only while this screen is open, and weather refreshes every 15 minutes. No location history is stored.
- **Reading and screen controls:** conversation wheel scrolling stays inside the transcript; supported speech timestamps follow the spoken passage. Manual scrolling pauses following, and the down-arrow resumes it. Android TTS depends on engine range callbacks; unsupported cloud timestamp responses fall back to audio without precise tracking. Bold Markdown renders without literal `**`. Quick actions can keep the Muse screen awake; this is an app preference, not a global Android timeout change.

The gallery uses the original Muse avatar and fictional offline examples. Sample City, weather numbers, battery level and reminders in screenshots are fixtures. This release does not include navigation or Google Maps integration.

## V1.5 — 일상 기능과 충전 중 시계

- **답변 도중 끼어들기:** 측면 버튼을 짧게 누르면 읽기를 멈춥니다. 길게 누르면 읽기를 중단하고 새 음성 입력을 시작하며, 떼면 전송합니다. 항상 마이크를 켜 두는 호출어 방식은 아닙니다.
- **일정·약속 리마인더:** 빠른 기능에서 내용·날짜·시간을 Muse 안에서 설정합니다. 지원되는 음성·키보드 명령은 확인 화면을 열며, 확인 후에만 등록됩니다. 지원하지 않는 날짜는 수동 편집기를 이용하세요. 알림은 R1에서 울리며 외부 캘린더 계정과 동기화하지 않습니다.
- **홈 상태 표시:** 배터리 잔량·충전 상태와 다음 알람·리마인더·타이머를 확인하고, 요약을 눌러 관리할 수 있습니다.
- **충전 중 탁상시계:** 빠른 기능에서 켜면 충전 중 20초간 조작하지 않을 때 나타납니다. 시간·날짜·요일과 현재 지역의 날씨 아이콘·기온·습도·시간대별 자외선 예보를 표시합니다. 터치하면 대화로 돌아옵니다. 한국어·일본어·영어 표시 언어를 따릅니다. 날씨에는 최초 위치 권한 허용과 인터넷이 필요하며, 반올림한 대략적인 좌표를 Open-Meteo로 보냅니다. 누락값은 대시로 표시하고 날씨 조회에 실패해도 시계는 동작합니다. 시계가 열려 있을 때만 위치를 조회하며 날씨는 15분마다 갱신합니다. 위치 기록은 저장하지 않습니다.
- **읽기·화면 개선:** 휠이 대화 영역만 스크롤하며, 지원되는 음성 타임스탬프에 맞춰 읽는 부분을 따라갑니다. 직접 스크롤하면 따라가기가 일시 정지되고 아래 화살표로 재개됩니다. Android TTS는 엔진 지원에 따라 다르며, 클라우드 타임스탬프 미지원 시 정밀 따라가기 없이 음성을 재생합니다. 굵은 글씨의 `**` 기호는 화면에서 숨깁니다. 빠른 기능에서 Muse 화면 자동 꺼짐을 해제할 수 있으며 Android 전체 설정은 바꾸지 않습니다.

공개 이미지는 기본 Muse 아바타와 오프라인 더미 데이터입니다. Sample City, 날씨·배터리 수치, 일정은 예시입니다. 길찾기·Google Maps 연동은 포함하지 않습니다.

## V1.5 — 日常機能と充電中の時計

- **読み上げ中の割り込み:** サイドボタンを短く押すと停止、長押しすると停止して新しい音声入力を開始し、離すと送信します。常時マイクで待ち受けるウェイクワード方式ではありません。
- **予定リマインダー:** クイック機能から内容・日付・時刻をMuse内で設定できます。対応する音声・文字コマンドは確認画面を開き、確認後に登録します。未対応の日付は手動入力してください。通知はr1本体で鳴り、外部カレンダーとは同期しません。
- **ホームの状態表示:** 電池残量・充電状態と次のアラーム／リマインダー／タイマーを表示。時計の要約をタップすると管理画面を開きます。
- **充電中の卓上時計:** クイック機能で有効にすると、充電中に20秒操作しない場合に表示されます。時刻・日付・曜日、現在地の天気アイコン・気温・湿度・時間別UV予報を表示し、タップで会話に戻ります。表示言語は英語・韓国語・日本語に対応。天気には位置情報の初回許可とネット接続が必要で、丸めた座標をOpen-Meteoへ送信します。取得できない値はダッシュ表示となり、天気の取得に失敗しても時計は動作します。時計画面が開いている間だけ位置を取得し、天気は15分ごとに更新します。位置履歴は保存しません。
- **読み上げ・画面操作:** ホイール操作を会話内に限定し、対応する音声タイムスタンプで読み上げ箇所を追従します。手動スクロールで追従を一時停止、下矢印で再開できます。Android TTSはエンジン依存で、クラウドのタイムスタンプ未対応時は精密な追従なしで再生します。太字の`**`記号を表示せず、クイック機能からMuse画面の自動消灯を無効化できます。Android全体の設定は変更しません。

公開画像は標準Museアバターとオフラインのダミーデータです。Sample City、天気・電池の数値、予定はサンプルです。ナビゲーション・Google Maps連携は含みません。

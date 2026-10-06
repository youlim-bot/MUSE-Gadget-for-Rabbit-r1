# インストール・使い方ガイド

[English](../en/GUIDE.md) · [한국어](../ko/GUIDE.md) · [日本語](GUIDE.md) · [ホーム](../../README.md)

## V2.0 — Museを育てる

**クイック機能 → Museを育てる**で専用の育成画面に入ります。動くたまごから始まり、アプリを閉じている時間も含めて数日かけて成長します。孵化までの正確な条件や成長の数値は画面に表示しません。食事・遊び・お世話で性格や成体の姿が変わり、長く放置すると病気や死につながります。新しいたまごを迎えると、それまでの記録を端末内に保存します。

孵化後はミルク・おかゆ・ベリー・魚・野菜・ケーキの6種類の食事、星取り・光の記憶・鳥のリズムの3種類の遊び、お風呂・薬・睡眠が使えます。連続したお世話には待ち時間があり、連打で即座に成長する仕組みではありません。成体はカスタムのイルカをベースに装飾や動作が変化します。

**♫**で自動・森・オルゴール・庭・オフと効果音を選べます。自動BGMは成長や睡眠に合わせて変わり、読み上げ・録音が優先されます。

育成中も**話す・入力・ツール**からMuseのエージェント機能を利用でき、成功した対話がお世話に反映されます。**育成・動き・音楽はオフラインで動作しますが、Museの回答とクラウド音声には正常な接続が必要です。** 育成画面の表示はペアリング復旧の証明ではありません。

下部の順序は**カメラ → キーボード → クイック機能 → 音声 → 設定**です。設定にWi-Fi、ペアリング・サイドボタン、文字サイズ・音声速度、音量、画面消灯、充電時計の待ち時間、アバターの動き、表示言語をまとめました。Wi-FiパスワードはMuseから開くAndroidのシステム画面で入力します。

天気では**現在UV・今日の最大UV**を区別し、夜間を明示します。不明な値を0に置き換えず、天気欄のタップで更新できます。接続の再試行には上限があり、認証が失効した場合は再ペアリングを案内します。対応するスマートフォンでの操作なしに失効したペアリングを復元する機能ではありません。経路案内は含みません。

[3言語のV2.0リリースノート](../RELEASE-V2.0.md) · [オフライン画面集](../SHOWCASE.md)


## 1. 対象と準備

Rabbit r1 1台で実施した導入記録です。公式サポートや全ファームウェアでの動作を保証するものではありません。使用したシステムは **LineageOS 21 / Android 14、`lineage-21.0-20250621-UNOFFICIAL-arm64_bgN`** です。既存のカーネル・vendor上でGSIを動かしており、このリポジトリはカスタムROMではなくAndroidアプリです。

書き込みはユーザーデータを消去し、起動不能になる可能性があります。**自分の端末**の元のファームウェアとパーティション情報をバックアップし、チェックサムを確認してください。通信・校正・識別情報のパーティションを公開したり、別の端末に書き込んだりしないでください。このGSI/vbmeta構成ではブートローダーを再ロックしません。起動成功は最新のセキュリティサポートを意味しません。

データ通信対応USBケーブル、macOS（他のOSはコマンドの調整が必要）、Android platform-tools、5GB以上の空き容量が必要です。アプリのビルドにはJava 17とAndroid SDK platform 36を使用します。MacのHomebrewでは `android-platform-tools` を導入でき、SDKはAndroid Studioで用意できます。対象のr1だけを接続し、WebUSBフラッシャーを開いたブラウザーは閉じてください。

ブートローダーのアンロックは別の前提作業です。[r1_escape](https://github.com/RabbitHoleEscapeR1/r1_escape)、[Android GSI資料](https://source.android.com/docs/core/tests/vts/gsi)、[コミュニティの導入例](https://substrate.dougbelshaw.com/rabbit-r1-android)、[ファームウェア書き込みガイド](https://github.com/TurboTheTurtle/rabbit-r1-firmware/blob/main/docs/flashing-guide.md)を確認してください。ファームウェア世代による違いがあります。

`dm-verity corruption`、一度起動して電源が切れる、fastbootdに入れない場合は、先に[復旧事例](JOURNEY.md)を読んでください。古いboot/vbmetaを繰り返し書き込む方法を一般的な対処法として扱わないでください。

## 2. イメージの取得と検証

新しい作業フォルダーで実行します。以下は再現用の過去ビルドであり、最新・最も安全な版という意味ではありません。

```sh
curl -fL --retry 2 -o lineage.img.gz \
 'https://downloads.sourceforge.net/project/andyyan-gsi/lineage-21-td/lineage-21.0-20250621-UNOFFICIAL-arm64_bgN-signed.img.gz'
echo '2ad81102b6902737c182d791f0f88c0c1e31aa11e7f7d0c3e3864c49b04a4aa6  lineage.img.gz' | shasum -a 256 -c -
gzip -t lineage.img.gz
# 両方の検証が成功してから展開
gzip -dc lineage.img.gz > lineage.img
curl -fL -o google-gsi-vbmeta.img \
 'https://dl.google.com/developers/android/qt/images/gsi/vbmeta.img'
echo 'f6da5489fd877cb69cf61fa721cfd6d77e530084aefe9b96664f818947ff61f6  google-gsi-vbmeta.img' | shasum -a 256 -c -
```

失敗したら中止します。ハッシュは導入時に計算した照合値であり、配布元の独立した署名ではありません。展開後のsystemは3,090,542,592バイト、vbmetaは4,096バイトでした。ファームウェア本体はこのリポジトリで配布しません。

## 3. モード・スロット確認とLineageOS導入

アンロック手順に従ってbootloader fastbootに入ります。Androidが正常起動しUSBデバッグを承認済みなら `adb reboot bootloader` も使えます。

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

**必須条件：**対象1台、ブートローダーがアンロック済み、fastbootdで `is-userspace: yes`、`system_a` が論理パーティション、両モードでスロット `a` が一致すること。異なる場合は中止します。画面に `FASTBOOT` と表示されるだけではfastbootdとは限りません。スロットBや別の構成は対象外です。

bootloader fastbootから：

```sh
fastboot flash vbmeta_a google-gsi-vbmeta.img
fastboot reboot fastboot
fastboot getvar is-userspace
fastboot getvar current-slot
# yes / a を確認してから実行
fastboot -S 100M flash system_a lineage.img
```

全書き込みが `OKAY` で完了する必要があります。実例では30回のsparse転送が行われました。`FAILED` やサイズ変更エラーでは中止し、他の論理パーティションを削除して解決しようとしないでください。確認済みGoogle vbmetaは検証無効化済みのため、そのまま書き込みました。この成功したGSI導入手順ではboot/vendor/modem/preloader/スロットBを変更しません。

**次の初期化はuserdataと暗号化メタデータを完全に消去します。**

```sh
fastboot reboot bootloader
fastboot getvar current-slot
# a を確認し、データ消去を受け入れた場合のみ実行
fastboot -w
fastboot reboot
```

初期設定まで待ち、Wi-Fi、画面のセキュリティ、開発者向けオプション → USBデバッグを設定してMacを承認します。

```sh
adb devices
adb shell getprop ro.build.version.release
adb shell getprop ro.lineage.version
adb shell getprop ro.boot.slot_suffix
```

確認値はAndroid `14`、上記のLineageビルド、`_a`です。

## 4. Museのビルド・インストール

```sh
git clone https://github.com/youlim-bot/MUSE-Gadget-for-Rabbit-r1.git
cd MUSE-Gadget-for-Rabbit-r1
# Java 17 と ANDROID_HOME を設定してから実行
./gradlew :app:assembleDebug :app:testDebugUnitTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n dev.cameronpak.muser1/.MainActivity
```

更新には同じ署名鍵が必要です。Macを替えるとdebug署名鍵も変わる場合があるため、鍵を非公開で保管してください。署名不一致の回避目的でペアリング済みアプリを削除しないでください。`install -r` は署名が一致する場合にデータを保持します。アプリ削除やデータ消去でペアリング、履歴、メモ等を失います。

## 5. SDKトークンとペアリング

自分の [Muse SDKトークン](https://gadgets.muse.ai/settings/sdk-tokens)を取得し、[SDK利用条件](https://gadgets.muse.ai/sdk-terms)を確認します。対応アカウントとスマートフォンのMuseアプリが必要です。利用可否は変わる場合があります。ペアリング済みなら再登録は不要です。

以下は **Bash** で実行します。トークンをコマンドに直接書かないでください。

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

アプリはAndroid Keystore AES-GCMで暗号化保存し、一時ファイルを削除します。鍵やペアリング情報を画面・ログに公開しないでください。

r1の空白部分を長押し → **Museとペアリング** → Bluetoothを許可。スマートフォンのMuseでSettings → Devices → Developer modeを有効にし、r1に表示された名前のガジェットを追加します。待機時間は2分です。r1は自身のWi-Fi/LTE接続を使います。

## 6. サイドボタン・ホイール・画面

すでにkeylayoutを適用している場合は再実施不要です。以下はサイドボタンの標準電源操作を変更します。初回のみLineageOSの **Rooted debugging** を一時的に有効にしてください。

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

`mtk-kpd` が `/data/system/devices/keylayout/mtk-kpd.kl` を使用していることを確認し、**Rooted debuggingを無効化**します。Androidのユーザー補助で **Side button controls** を有効にしてください。制限付き設定の警告が出る場合はアプリ情報から許可して戻ります。サービスはキー入力を処理し、画面内容へのアクセスは要求しません。

ホームアプリへの設定は任意です。

```sh
adb shell cmd package set-home-activity dev.cameronpak.muser1/.MainActivity
```

- 長押し：録音、離す：送信。短押し：Androidのロック／画面オフ操作。
- サイドボタンを押しながらホイール：メディア音量。この操作では録音を送信しません。
- カメラ画面：サイドボタンで撮影、ホイール上下で背面／前面カメラを要求。前面・背面選択による実際の回転は所有者が確認済みですが、ホイール操作の方向・反応は別途実機確認が必要です。
- 480×640の画面で操作部が切れる場合、実例の密度設定は `adb shell wm density 200`。戻すには `adb shell wm density reset`。

画面消灯と画面ロックは別設定です。ロック解除操作を省きたい場合はAndroid設定 → セキュリティ → 画面ロック → なしを自分で選びます（名称はビルドによって異なります）。端末保護は解除されますが、画面消灯時間は設定できます。アプリがPINを自動削除することはありません。同じ画面からPINを再設定できます。

標準ボタンに戻すにはRooted debuggingを一時的に有効化し、`adb root` 後に `/data/system/devices/keylayout/mtk-kpd.kl` だけを削除 → 再起動 → 元のkeylayoutを確認 → Rooted debugging無効化の順で行います。

## 7. ElevenLabs・言語・音声

任意のElevenLabs連携はSTTに `scribe_v2`（`/v1/speech-to-text`）、TTSに `eleven_v4`（`/v1/text-to-dialogue`）を要求します。ソース上の識別子であり、すべてのアカウントで利用できる保証ではありません。[公式資料](https://elevenlabs.io/docs)とアカウントのモデル・音声へのアクセスを確認してください。API利用料金が発生する場合があります。

r1を1台接続し `python3 scripts/provision-elevenlabs.py` を実行します。APIキーは非表示入力で、Voice IDは自分の音声を指定します。モデル・音声を確認し、アプリ専用一時ファイル経由で暗号化保存します。音声生成は実行しません。必要ならライブラリの音声を先に自分のアカウントへ追加します。STT/TTS生成、Models/Voices読み取り権限が必要です。**HTTP 400だけで権限不足と断定しないでください。** `voice_not_found` はそのアカウントで音声が利用できない場合にも発生します。

- 右上の国旗：表示言語（韓国語・日本語・英語）。
- **通訳**：入力言語（自動／韓国語／日本語／英語）と翻訳先を選択。入力言語を指定した場合は矢印で方向交換できます。
- **連続通訳**：録音 → 文字起こし → 翻訳 → 再生を繰り返します。文単位の処理であり同時ストリーミング通訳ではありません。同じボタンで停止します。
- **会話**では通訳用の言語行を隠します。現在の通常会話は自動言語認識と韓国語音声出力を要求します。表示言語だけを変えても通常会話の回答言語は変わりません。
- **サイレントモード**：回答音声だけを無効化。マイクやAndroid全体の音量は無効にしません。
- **クイック機能 → 文字サイズ・音声速度**：18/20/22/24/26sp、0.75/1/1.25/1.5倍。次の音声から反映し、設定を保持します。

認識が悪い場合は実際の発話と認識結果を記録し、録音開始を待ってから同じ文をスマートフォンでも比較してください。読み上げ音声（TTS）の変更は音声認識（STT）の改善とは別です。

## 8. 写真・キーボード・ライブラリ

| 機能 | 操作と範囲 |
|---|---|
| 写真への質問 | カメラ → 撮影 → Museに質問 → 質問入力。縮小JPEGを送信。撮影だけでは送信しません。 |
| 写真保存 | 保存ボタンを選ぶとAndroid Pictures/Museに保存。質問時に自動でギャラリー保存しません。 |
| 写真の文字翻訳 | クイック機能 → 写真の文字を翻訳 → 撮影 → 翻訳先。Museへの依頼でありオフラインOCRではありません。 |
| 写真への追加質問 | クイック機能 → 前の写真に続けて質問 → 同じ写真で質問。画像を再送。メモリー内のみのためプロセス終了／写真を忘れる操作で消去されます。 |
| キーボード | 左下のキーボード。Androidキーボード（例：Gboard）で韓・日・英を追加し、地球アイコン／入力言語ボタンで切替。独自IMEは同梱しません。 |
| クイック機能 | 会話要約や簡単な説明のプロンプトを編集して送れます。 |
| お気に入り | クイック機能 → 会話検索 → 会話を選択 → お気に入り。画面履歴と独立したコピーを保存します。 |
| 検索 | 端末上の質問・回答・保存項目が対象。空白区切りの語がすべて一致する項目を表示。Museの全クラウド履歴検索ではありません。 |
| 音声メモ | 最大20秒録音 → ElevenLabsで文字化 → 確認・修正 → 端末保存。音声ファイルの保管や自動Muse送信はしません。 |
| メモからタスク | 保存メモ → タスクに整理 → Museへの送信確認 → 1行1項目で確認 → リスト保存。最大50件、元メモは保持し完了チェックを保存。通知・カレンダー連携はありません。 |
| アバター | V2.0は所有者提供のMuse生成イルカを会話・育成画面に使用します。画像についてはCREDITS参照。アカウント画像の自動同期は未実装です。 |

## 9. LTEとトラブル対応

所有者の端末では日本のRakuten Mobileデータ通信を確認し、その試験ではAPNの手動変更は不要でした。キャリア認証、通話・SMS・ローミング・全周波数帯の対応を確認したものではありません。自動接続できない場合は通信会社の最新APN資料を参照してください。Wi-Fiを無効にし、セルラーが実際のデフォルト・検証済み通信経路か確認して通信試験を行います。SIM表示だけでは不十分です。

- **USB認識なし：**データケーブル・ポート、画面のロック解除、USBデバッグ承認、WebUSB終了を確認。adb/bootloader/fastbootdを区別します。
- **dm-verity・電源断：**推測での書き込みを中止し[復旧事例](JOURNEY.md)を参照。
- **ボタン無反応：**keylayoutとユーザー補助サービスの有効・接続状態を確認。
- **音声なし：**サイレントモード、音量、ElevenLabsの権限・上限、代替Android TTSの韓国語データを確認。
- **写真の理解に失敗：**サーバー・モデル対応は変わる場合があります。テスト成功やダミー画面は実際の画像回答成功の証明ではありません。
- **タスク抽出失敗：**元メモは保持され、明示的に再試行します。架空の結果を自動保存しません。

[データ処理](../../SECURITY.md) · [検証範囲](../development.md) · [導入・開発記録](JOURNEY.md)

## R1のアラーム・タイマーと音声停止

**クイック機能 → アラーム・タイマー**を開きます。Muse内のパネルで時・分、平日の繰り返し、または3分・5分・10分タイマーを選びます。数字を上下にスワイプして保存・開始してください。秒指定、一時停止・再開・削除、5分後の再通知、アラーム音量も操作できます。初回のAndroid権限承認のみシステム画面を使います。

ElevenLabs音声認識を設定した会話モードで「3分タイマー」「明日午前7時に起こして」「平日午前8時アラーム」と話すか入力し、R1の確認画面で開始を押します。対応する命令はMuse送信前に処理し、連携MacではなくR1に設定します。通訳モードでは実行しません。ElevenLabs未設定の場合は画面から操作してください。

確定した予約は端末保存され、その後のインターネット接続は不要です。電源オフ・強制停止中は鳴りません。再起動後の復元にはロック解除が必要で、期限を過ぎた一回限りの予約はオフになります。再起動復元と実際の発話からの一連動作は未検証です。音量ゼロや通知制限で音が出ない場合があります。鳴動は5分で自動停止し、通知の停止ボタンは鳴動中の全項目を止めます。

音声準備・再生中の下部ボタンは **■ 音声停止** になります。現在の回答の残り音声を止めて文章は残し、次の回答は通常どおり読み上げます。初期値は音声ON、オレンジのスピーカーがON、暗い表示がOFFです。手動のサイレント設定は保持します。

## V1.5 — 日常機能と充電中の時計

- **読み上げ中の割り込み:** サイドボタンを短く押すと停止、長押しすると停止して新しい音声入力を開始し、離すと送信します。常時マイクで待ち受けるウェイクワード方式ではありません。
- **予定リマインダー:** クイック機能から内容・日付・時刻をMuse内で設定できます。対応する音声・文字コマンドは確認画面を開き、確認後に登録します。未対応の日付は手動入力してください。通知はr1本体で鳴り、外部カレンダーとは同期しません。
- **ホームの状態表示:** 電池残量・充電状態と次のアラーム／リマインダー／タイマーを表示。時計の要約をタップすると管理画面を開きます。
- **充電中の卓上時計:** クイック機能で有効にすると、充電中に選択した待ち時間（初期値20秒）操作しない場合に表示されます。時刻・日付・曜日、現在地の天気アイコン・気温・湿度・時間別UV予報を表示し、タップで会話に戻ります。表示言語は英語・韓国語・日本語に対応。天気には位置情報の初回許可とネット接続が必要で、丸めた座標をOpen-Meteoへ送信します。取得できない値はダッシュ表示となり、天気の取得に失敗しても時計は動作します。時計画面が開いている間だけ位置を取得し、天気は15分ごとに更新します。位置履歴は保存しません。
- **読み上げ・画面操作:** ホイール操作を会話内に限定し、対応する音声タイムスタンプで読み上げ箇所を追従します。手動スクロールで追従を一時停止、下矢印で再開できます。Android TTSはエンジン依存で、クラウドのタイムスタンプ未対応時は精密な追従なしで再生します。太字の`**`記号を表示せず、クイック機能からMuse画面の自動消灯を無効化できます。Android全体の設定は変更しません。

公開画像は標準Museアバターとオフラインのダミーデータです。Sample City、天気・電池の数値、予定はサンプルです。ナビゲーション・Google Maps連携は含みません。

## 最新ソースの追加機能

- **反応するアバター:** クイック機能でオン/オフ。標準ではオンです。外見を維持したまま待機・聞き取り・思考・発話に応じて動き、充電マークも表示します。声から感情を推定する機能ではありません。
- **カメラ宝探し:** 赤いもの・丸いもの・植物・本・しま模様のものを探します。撮影後に写真と質問を確認し、送信を押すとMuseが画像の根拠に基づいて判断します。不明な場合は判断できないと回答するよう指示します。自動採点や達成記録はありません。上部のモード表示で目標変更・終了ができます。
- **韓・日・英の会話パートナー:** 言語とカフェ注文・旅行/道案内・日常会話を選ぶとMuseに開始を送信します。サイドボタンまたは文字で返答し、短い応答・表現の訂正1つ・次の質問を受け取ります。音声練習にはElevenLabsの音声認識設定が必要です。発音採点は行わず、練習中のアラーム例文は実際のアラームを作成しません。上部のモード表示から変更・終了できます。練習開始時は通訳をオフにします。
- **時計の待ち時間:** クイック機能 → 充電中の置き時計 → 待ち時間。10秒・20秒・30秒・1分・2分・5分を選べます。初期値は20秒で設定を保存します。設定画面を閉じた時点から数え直し、充電中かつ操作・録音・再生のない状態で切り替わります。

通常/デモのビルドと94件の単体テストが成功しました。開発端末でメニュー、目標変更、言語/場面選択、実際の10秒切替を確認し、20秒に戻しました。実際の写真判定・会話品質は追加検証が必要です。既存のV1.5タグと画像キットは変更せず、今回の変更はmainブランチに反映します。

メイン画面の電池表示の隣に、現在のWi-Fi/モバイル接続と電波強度を表示します。LTE・5G・3G・2Gは取得できる通信情報に応じて表示し、不明な値を強い電波として表示しません。感嘆符はインターネット接続が未確認の状態です。VPNと有線接続も区別します。メイン画面表示中のみ更新し、SSID・電話番号・SIM識別子は保存しません。開発端末でLTE 4段階とインターネット接続を確認しましたが、今回Wi-Fiへの実際の切替は未検証です。公開デモは固定のダミー信号を使います。宝探し・会話バナーは内容に合わせた高さと専用余白で文字の欠けを修正しました。

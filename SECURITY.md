# Privacy and security

This is a community prototype. Do not attach tokens, raw device backups, identifiers, private transcripts or pairing screenshots to issues.

| Data | Destination / retention |
|---|---|
| Muse SDK token, pairing credentials, ElevenLabs key/voice ID | Android Keystore AES-GCM protected app storage; pending plaintext provisioning files deleted after import. Android backup disabled. |
| Voice question with ElevenLabs configured | Audio to ElevenLabs for STT; recognized text to Muse; reply text to ElevenLabs for speech unless quiet. |
| Voice question without ElevenLabs | Audio to Muse; local Android TTS reads replies. Engine/voice availability determines offline speech support. |
| Photo question/translation/follow-up | Resized JPEG and question to Muse after explicit send. Last photo is memory-only. |
| Explicit photo Save | Android shared media gallery, Pictures/Muse. |
| Voice memo | Audio to ElevenLabs, then editable text saved locally. No audio archive; not automatically sent to Muse. |
| Make to-dos | Selected memo text to Muse after confirmation. Reviewed checklist saved separately; original memo retained. |
| Display history, favorites, memos, checklists | App-private local text files, not separately application-encrypted. Android sandbox/storage security still applies. |
| Walking navigation | Android geocoder receives destination search text. OSM tiles receive viewed area/IP; normal HTTP tile cache may remain locally. User-triggered route requests send start/destination coordinates to FOSSGIS (server logs may be retained). App position/routes/search results are memory-only; tracking stops when the map is not visible. No location sent to Muse AI. |
| Showcase demo | Separate `.demo` package, synthetic fixtures; no Internet, camera, microphone or location permission. No account needed. |

Clearing local display history does not delete Muse's server-side conversation or separate favorite copies. Uninstalling/clearing app data loses local content and credentials. `adb install -r` preserves app data only with a compatible signature. Keep signing keys private.

An unlocked bootloader, disabled AVB verification, rooted debugging or removal of screen lock reduces protection. None is hidden behind an app feature; firmware and security settings are separate user decisions. The side-button keylayout replaces native power behavior and depends on the accessibility service for its replacement behavior. The service does not request window-content access.

Public demo captures do not contain real device serials, SIM identifiers, network names, account names, keys or personal content. No stock backup or private signing key is distributed. Report a suspected secret privately to the repository owner before posting reproductions publicly.

## 한국어

음성은 설정에 따라 Muse 또는 ElevenLabs로 전송됩니다. 사진은 질문 전송을 눌렀을 때 Muse로 보내며, 메모는 문자 변환 뒤 기기에 저장합니다. `할 일로 정리`는 별도 확인 후 메모를 Muse에 보냅니다. 기록·즐겨찾기·메모·체크리스트는 앱 전용 텍스트 파일이며 인증 키와 달리 앱 자체 암호화 파일은 아닙니다. 화면 기록 삭제는 Muse 서버 기록 삭제가 아닙니다. 공개 캡처는 별도 오프라인 앱의 더미 데이터입니다. 토큰·백업·식별값·개인 대화를 이슈에 올리지 마세요.

## 日本語

音声は設定に応じてMuseまたはElevenLabsへ送信します。写真は質問送信時にMuseへ送り、音声メモは文字化後に端末保存します。タスク抽出は別の確認後にメモをMuseへ送ります。履歴・お気に入り・メモ・タスクはアプリ専用テキストであり、認証鍵のようなアプリ独自暗号化は施していません。画面履歴の消去はMuseサーバー履歴の削除ではありません。公開画像は別のオフラインアプリのダミーデータです。鍵・バックアップ・識別情報・個人会話をIssueに投稿しないでください。

Navigation privacy / 길찾기 / ナビ: 경로 계산 시 출발지·목적지 좌표가 FOSSGIS에 전송되고, 지도 표시 지역·IP는 지도 서비스에 전달됩니다. 검색어는 Android 검색 제공자에게 전달됩니다. Muse AI에는 보내지 않습니다. ルート計算時に出発地・目的地の座標をFOSSGISへ、表示地域とIPを地図サービスへ、検索語をAndroid検索プロバイダーへ送ります。Muse AIには送りません。サービス側にログが残る場合があります。

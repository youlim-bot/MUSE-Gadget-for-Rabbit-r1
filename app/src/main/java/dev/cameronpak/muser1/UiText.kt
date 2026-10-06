package dev.cameronpak.muser1

import android.content.Context

internal enum class DisplayLanguage(val flag: String, val label: String) {
    KO("🇰🇷", "한국어"), JA("🇯🇵", "日本語"), EN("🇺🇸", "English")
}

internal object UiText {
    fun language(context: Context) = DisplayLanguage.entries.firstOrNull {
        it.name == context.getSharedPreferences("display_language", Context.MODE_PRIVATE).getString("language", "KO")
    } ?: DisplayLanguage.KO
    fun set(context: Context, language: DisplayLanguage) {
        context.getSharedPreferences("display_language", Context.MODE_PRIVATE).edit().putString("language", language.name).apply()
    }
    fun text(context: Context, ko: String, ja: String, en: String) = when (language(context)) {
        DisplayLanguage.KO -> ko; DisplayLanguage.JA -> ja; DisplayLanguage.EN -> en
    }
    fun translate(context: Context, value: String): String = translate(language(context), value)
    fun translate(language: DisplayLanguage, value: String): String {
        val row = rows.firstOrNull { value in it } ?: return value
        return row[language.ordinal]
    }
    private val rows = listOf(
        listOf("대화", "会話", "Chat"), listOf("통역", "通訳", "Translate"),
        listOf("자동", "自動", "Auto"), listOf("한국어", "韓国語", "Korean"),
        listOf("일본어", "日本語", "Japanese"), listOf("영어", "英語", "English"),
        listOf("표시 언어", "表示言語", "Display language"),
        listOf("입력 언어", "入力言語", "Input language"), listOf("번역 언어", "翻訳言語", "Target language"),
        listOf("말하는 언어", "話す言語", "Spoken language"), listOf("번역할 언어", "翻訳先の言語", "Translate into"),
        listOf("닫기", "閉じる", "Close"), listOf("원문", "原文", "Original"), listOf("번역", "翻訳", "Translation"),
        listOf("나", "自分", "You"), listOf("인식 결과 없음", "文字起こしなし", "Transcript unavailable"),
        listOf("측면 버튼을 길게 눌러 말하기", "サイドボタンを長押しして話す", "Hold the side button to talk"),
        listOf("▶ 연속 통역", "▶ 連続通訳", "▶ Live"), listOf("■ 중지", "■ 停止", "■ Stop"),
        listOf("● 듣는 중 · 중지", "● 録音中 · 停止", "● Listening · Stop"),
        listOf("연속 통역 중지", "連続通訳を停止", "Stop interpretation"), listOf("연속 통역 시작", "連続通訳を開始", "Start interpretation"),
        listOf("입력 언어와 번역 언어 교환", "入力言語と翻訳言語を交換", "Swap languages"),
        listOf("기기 설정", "端末の設定", "Device controls"), listOf("Muse 다시 연결", "Museに再接続", "Reconnect to Muse"),
        listOf("Muse 페어링", "Museとペアリング", "Pair with Muse"), listOf("Android 설정", "Android設定", "Android settings"),
        listOf("대기 화면", "待機画面", "Return to idle"), listOf("대화 보기", "会話を表示", "Show conversation"),
        listOf("사이드 버튼 설정", "サイドボタン設定", "Side button settings"), listOf("Muse로 돌아가기", "Museに戻る", "Back to Muse"),
        listOf("화면 기록 지우기", "画面の履歴を消去", "Clear display history"),
        listOf("화면 기록을 지울까요?", "画面の履歴を消去しますか？", "Clear display history?"),
        listOf("이 기기의 화면 기록만 지웁니다. Meta Muse의 대화와 기억은 유지됩니다.", "この端末の画面履歴だけを消去します。Meta Museの会話と記憶は残ります。", "This clears only this r1's display history. Your conversation remains in Meta Muse, and Muse still remembers earlier turns."),
        listOf("취소", "キャンセル", "Cancel"), listOf("지우기", "消去", "Clear"),
        listOf("최신 메시지로 이동", "最新のメッセージへ", "Scroll to latest message"),
        listOf("연결 중", "接続中", "CONNECTING"), listOf("연결 끊김", "接続なし", "DISCONNECTED"),
        listOf("연결 실패", "接続失敗", "CONNECTION FAILED"), listOf("마이크 오류", "マイクエラー", "MICROPHONE ERROR"),
        listOf("마이크 사용 불가", "マイクを使用できません", "MICROPHONE UNAVAILABLE"),
        listOf("조금 더 길게 눌러 주세요", "もう少し長く押してください", "HOLD A LITTLE LONGER"),
        listOf("놓으면 전송합니다", "離すと送信します", "RELEASE TO SEND"),
        listOf("응답 시간 초과", "応答タイムアウト", "REPLY TIMED OUT"),
        listOf("● 마이크 켜짐 · 말이 끝나면 자동 번역", "● 録音中 · 発話後に自動翻訳", "● Listening · translates after each sentence"),
        listOf("MUSE에 전송 중", "Museに送信中", "Sending to Muse"),
        listOf("ELEVEN V4 음성 생성 중", "Eleven v4 音声生成中", "Eleven v4 generating speech"),
        listOf("방향 교환 전 입력 언어를 선택하세요.", "交換する前に入力言語を選んでください。", "Choose an input language before swapping."),
        listOf("Muse와 ElevenLabs 연결을 확인하세요.", "MuseとElevenLabsの接続を確認してください。", "Check the Muse and ElevenLabs connections."),
        listOf("ElevenLabs 등록이 필요합니다.", "ElevenLabsの設定が必要です。", "Set up ElevenLabs first."),
        listOf("ElevenLabs 등록 후 사용할 수 있습니다.", "ElevenLabsの設定後に使用できます。", "Available after ElevenLabs setup.")
    )
}

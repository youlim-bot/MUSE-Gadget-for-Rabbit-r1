package dev.cameronpak.muser1

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.TextView
import android.widget.Button
import android.widget.LinearLayout
import dev.cameronpak.muser1.pairing.PairingServer
import dev.cameronpak.muser1.transport.MuseConnection
import kotlinx.coroutines.*

class MainActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val store get() = (application as MuseApp).store
    private var pairing: PairingServer? = null
    private var connection: MuseConnection? = null
    private var recorder: VoiceRecorder? = null
    private var sideButtonRecording = false
    private var cappedVoiceNote: ByteArray? = null
    private var connected = false
    private var recording = false
    private var sending = false
    private var pairingRequested = false
    private var repairPairingRequested = false
    private var pairingWindow: Job? = null
    private var turnTimeout: Job? = null
    private var transcriptFetch: Job? = null
    private var controls: AlertDialog? = null
    private var clearDialog: AlertDialog? = null
    private var composer: AlertDialog? = null
    private var typedDraft = ""
    private var quickDialog: AlertDialog? = null
    private lateinit var quietButton: android.widget.ImageButton
    private lateinit var quickButton: Button
    private lateinit var settingsButton: android.widget.ImageButton
    private var settingsDialog:MuseSettingsDialog?=null
    private var returnToSettings=false
    private var quietMode = false
    private var replySpeechStopped=false
    private var stopControlVisible=false
    private val playbackControls=object:Runnable {
        override fun run(){
            if(foreground!==this@MainActivity)return
            val visible=::speech.isInitialized && speech.hasPlayback
            if(visible!=stopControlVisible)refreshQuickControls()
            quickButton.postDelayed(this,200)
        }
    }
    private lateinit var clockStatus: Button
    private val clockStatusTicker = object : Runnable {
        override fun run() {
            if (foreground !== this@MainActivity) return
            refreshClockStatus()
            updateDeskClock()
            clockStatus.postDelayed(this, 1000)
        }
    }
    private fun refreshClockStatus() {
        val summary = ClockHomeStatus.text(this)
        clockStatus.visibility = if (summary.isEmpty()) View.GONE else View.VISIBLE
        if (clockStatus.text.toString() != summary) clockStatus.text = summary
        clockStatus.contentDescription = summary + UiText.text(this, " · 알람·타이머 열기", " · アラーム・タイマーを開く", " · Open alarms and timers")
    }
    private var deskClock: DeskClockDialog? = null
    private var charging = false
    private var batteryPercent = 0
    private val funModes by lazy { FunModes(this) }
    private lateinit var funStatus: Button
    private fun funText(ko:String,ja:String,en:String)=UiText.text(this,ko,ja,en)
    private fun refreshFunStatus() {
        if(!::funStatus.isInitialized)return
        funStatus.visibility=if(funModes.mode.isEmpty())View.GONE else View.VISIBLE
        funStatus.text=if(funModes.mode=="practice") funText("회화 연습","会話練習","Practice")+" · "+funModes.language.label+" ▾"
            else funText("보물찾기","宝探し","Treasure hunt")+" · "+huntLabel()+" ▾"
    }
    private fun huntLabel()=arrayOf(
        funText("빨간 물건","赤いもの","Something red"),funText("동그란 물건","丸いもの","Something round"),
        funText("식물","植物","A plant"),funText("책","本","A book"),funText("줄무늬 물건","しま模様のもの","Something striped"))[funModes.mission]
    private fun endFunMode(){funModes.mode="";refreshFunStatus();stopReplySpeech()}
    private fun showHunt() {
        stopContinuous(false);speech.stop();funModes.mode="hunt";refreshFunStatus()
        controls=AlertDialog.Builder(this).setTitle(funText("카메라 보물찾기","カメラ宝探し","Camera treasure hunt"))
            .setMessage(huntLabel()+"\n\n"+funText("주변의 물건을 찍어 Muse에게 확인받으세요. 사진 전송 전 확인할 수 있습니다.","身近なものを撮ってMuseに確認しましょう。送信前に写真を確認できます。","Photograph a nearby object and ask Muse to check it. Review the photo before sending."))
            .setPositiveButton(funText("사진 찍기","撮影","Take photo")){_,_->
                val lang=when(UiText.language(this)){DisplayLanguage.KO->InputLanguage.KOREAN;DisplayLanguage.JA->InputLanguage.JAPANESE;DisplayLanguage.EN->InputLanguage.ENGLISH}
                startActivity(Intent(this,CameraActivity::class.java).putExtra("hunt_prompt",FunPrompts.hunt(huntLabel(),lang)).putExtra("hunt_language",lang.name))
            }.setNeutralButton(funText("다른 미션","別のミッション","Another target")){_,_->funModes.mission=(funModes.mission+1)%5;showHunt()}
            .setNegativeButton(funText("종료","終了","End")){_,_->endFunMode()}.show()
    }
    private fun choosePractice() {
        stopContinuous(false);speech.stop()
        controls=AlertDialog.Builder(this).setTitle(funText("연습할 언어","練習する言語","Practice language"))
            .setItems(arrayOf("한국어","日本語","English")){_,which->
                val chosen=arrayOf(InputLanguage.KOREAN,InputLanguage.JAPANESE,InputLanguage.ENGLISH)[which]
                controls=AlertDialog.Builder(this).setTitle(funText("상황 선택 · 연습 시작","場面を選んで開始","Choose a scenario to start"))
                    .setItems(arrayOf(funText("카페 주문","カフェで注文","Order at a cafe"),funText("여행·길 묻기","旅行・道を尋ねる","Travel / directions"),funText("일상 대화","日常会話","Everyday chat"))){_,scene->
                        if(!connected || sending || recording){screen.showNotice(funText("Muse 연결 후 다시 시작해 주세요.","Muse接続後に再開してください。","Connect to Muse before starting."));return@setItems}
                        funModes.language=chosen;funModes.scene=scene;funModes.mode="practice"
                        languageMode=languageMode.copy(interpreting=false);saveLanguageMode();refreshFunStatus()
                        sendTypedMessage(funText("역할극을 시작해 줘.","ロールプレイを始めて。","Let's start the role-play."))
                    }.setNegativeButton(tr("닫기"),null).show()
            }.setNegativeButton(tr("닫기"),null).show()
    }
    private fun showPracticeControls() {
        controls=AlertDialog.Builder(this).setTitle(funText("회화 파트너","会話パートナー","Conversation partner"))
            .setMessage(funText("측면 버튼으로 말하거나 키보드로 답하세요. 짧은 답변과 표현 교정을 받습니다. 발음 평가는 하지 않습니다.","サイドボタンで話すか文字で返答。短い応答と表現の訂正を行います。発音採点はしません。","Use the side button or keyboard to reply. Get brief replies and expression corrections, not pronunciation scores."))
            .setPositiveButton(funText("언어·상황 변경","言語・場面を変更","Change scenario")){_,_->choosePractice()}
            .setNeutralButton(funText("키보드로 답하기","文字で返答","Type reply")){_,_->showComposer()}
            .setNegativeButton(funText("연습 종료","練習終了","End practice")){_,_->endFunMode()}.show()
    }
    private fun practicePayload(text:String)=FunPrompts.practice(funModes.language,funModes.scenarios[funModes.scene],text)
    private var batteryRegistered = false
    private lateinit var networkStatus: NetworkStatusView
    private lateinit var batteryStatus: TextView
    private fun refreshBatteryStatus() {
        if (!::batteryStatus.isInitialized) return
        val state = if (charging) {
            if (batteryPercent == 100) UiText.text(this,"충전 완료","充電完了","Fully charged")
            else UiText.text(this,"충전 중","充電中","Charging")
        } else UiText.text(this,"배터리","バッテリー","Battery")
        batteryStatus.text = "$state · ${batteryPercent}%"
        batteryStatus.contentDescription = UiText.text(this,"배터리","バッテリー","Battery") + " ${batteryPercent}% · $state"
        batteryStatus.setTextColor(if (!charging && batteryPercent <= 20) android.graphics.Color.rgb(255,164,99) else android.graphics.Color.rgb(165,173,168))
    }
    private var lastInteraction = android.os.SystemClock.elapsedRealtime()
    private val deskEnabled get() = getSharedPreferences("reading", MODE_PRIVATE).getBoolean("desk_clock", true)
    private val batteryReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: Intent) {
            charging = intent.getIntExtra(android.os.BatteryManager.EXTRA_PLUGGED, 0) != 0
            val scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            batteryPercent = (intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, 0) * 100 / scale).coerceIn(0,100)
            refreshBatteryStatus()
            if(::screen.isInitialized)screen.setCharging(charging)
            if (!charging) closeDeskClock()
        }
    }
    private fun closeDeskClock() { deskClock?.dismiss(); deskClock = null; lastInteraction = android.os.SystemClock.elapsedRealtime() }
    private fun openDeskClock() {
        if (deskClock != null || !charging) return
        deskClock = DeskClockDialog(this, { batteryPercent }) { deskClock = null; lastInteraction = android.os.SystemClock.elapsedRealtime() }.also { it.show() }
    }
    private val deskDelayOptions = intArrayOf(10,20,30,60,120,300)
    private val deskDelaySeconds get() = getSharedPreferences("reading", MODE_PRIVATE).getInt("desk_clock_delay_seconds",20).takeIf { it in deskDelayOptions } ?: 20
    private fun deskDelayLabel(seconds:Int):String = if(seconds<60) funText("${seconds}초","${seconds}秒","${seconds} sec") else funText("${seconds/60}분","${seconds/60}分","${seconds/60} min")
    private fun showDeskDelaySettings() {
        controls=AlertDialog.Builder(this).setTitle(funText("시계 전환 대기 시간","時計への切替待ち時間","Clock idle delay"))
            .setSingleChoiceItems(deskDelayOptions.map { deskDelayLabel(it) }.toTypedArray(),deskDelayOptions.indexOf(deskDelaySeconds)){dialog,index->
                getSharedPreferences("reading",MODE_PRIVATE).edit().putInt("desk_clock_delay_seconds",deskDelayOptions[index]).apply()
                lastInteraction=android.os.SystemClock.elapsedRealtime();dialog.dismiss();showDeskClockSettings()
            }.setNegativeButton(tr("닫기"),null).create().also { d->
                d.setOnDismissListener { lastInteraction=android.os.SystemClock.elapsedRealtime() };d.show()
            }
    }
    private fun updateDeskClock() {
        if (deskEnabled && charging && deskClock == null && hasWindowFocus() &&
            android.os.SystemClock.elapsedRealtime() - lastInteraction >= deskDelaySeconds * 1000L &&
            !recording && !sending && !continuous && !speech.hasPlayback &&
            clockDialog == null && controls?.isShowing != true && composer == null && quickDialog == null &&
            clearDialog?.isShowing != true && languageDialog?.isShowing != true && displayLanguagePopup?.isShowing != true) openDeskClock()
    }
    private fun showDeskClockSettings() {
        controls = AlertDialog.Builder(this).setTitle(UiText.text(this,"충전 중 탁상시계","充電中の置き時計","Charging desk clock"))
            .setSingleChoiceItems(arrayOf(
                UiText.text(this,"켜기 · ${deskDelayLabel(deskDelaySeconds)} 후 표시","オン · ${deskDelayLabel(deskDelaySeconds)}後に表示","On · after ${deskDelayLabel(deskDelaySeconds)} idle"),
                UiText.text(this,"끄기","オフ","Off")), if(deskEnabled)0 else 1) { dialog, index ->
                getSharedPreferences("reading",MODE_PRIVATE).edit().putBoolean("desk_clock",index==0).apply()
                lastInteraction=android.os.SystemClock.elapsedRealtime();dialog.dismiss()
            }.setNeutralButton(funText("대기 시간","待ち時間","Idle delay")){_,_->showDeskDelaySettings()}
            .setPositiveButton(UiText.text(this,"지금 보기","今すぐ表示","Show now")){_,_->
                if(charging)openDeskClock() else android.widget.Toast.makeText(this,UiText.text(this,"충전기를 연결해 주세요","充電器を接続してください","Connect a charger first"),android.widget.Toast.LENGTH_SHORT).show()
            }.setNegativeButton(tr("닫기"),null).create().also { d->
                d.setOnDismissListener { lastInteraction=android.os.SystemClock.elapsedRealtime() };d.show()
            }
    }
    override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
        if(event.actionMasked==android.view.MotionEvent.ACTION_DOWN) lastInteraction=android.os.SystemClock.elapsedRealtime()
        return super.dispatchTouchEvent(event)
    }
    private var transcriptPending = false
    private val history = mutableListOf<ConversationTurn>()
    private var activeTurn: ConversationTurn? = null
    private var historyLoaded = false
    private var historyLoad: Job? = null
    private val hasConversation get() = history.isNotEmpty()
    private val historyStore get() = (application as MuseApp).displayHistory
    private val shake = ShakeDetector()
    private val sensors by lazy { getSystemService(SensorManager::class.java) }
    private val shakeListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (!historyLoaded || !hasConversation || continuous || recording || sending ||
                clearDialog?.isShowing == true || controls?.isShowing == true) {
                shake.reset()
                return
            }
            if (shake.sample(event.timestamp / 1_000_000, event.values[0], event.values[1], event.values[2]))
                confirmClearHistory()
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }
    private val languagePreferences by lazy { getSharedPreferences("language_mode", MODE_PRIVATE) }
    private var languageMode = LanguageMode()
    private var activeLanguageMode = LanguageMode()
    private var localTranscript = false
    private var continuous = false
    private var resumeListening: Job? = null
    private var sendJob: Job? = null
    private lateinit var displayLanguageButton: Button
    private var displayLanguagePopup: android.widget.PopupWindow? = null
    private fun tr(value: String) = UiText.translate(this, value)
    private lateinit var conversationButton: Button
    private lateinit var continuousButton: Button
    private lateinit var modeButton: Button
    private lateinit var inputButton: Button
    private lateinit var targetButton: Button
    private lateinit var swapButton: Button
    private lateinit var languageRow: LinearLayout
    private var languageDialog: AlertDialog? = null
    private var turn = 0L
    private lateinit var speech: SpeechOutput
    private lateinit var screen: MuseScreen
    private lateinit var status: TextView
    private lateinit var message: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        petCameraOrigin=savedInstanceState?.getBundle("pet_camera_origin")
        petCameraReturned=savedInstanceState?.getBoolean("pet_camera_returned",false)?:false
        if (BuildConfig.DEMO) UiText.set(this, DisplayLanguage.entries.firstOrNull { it.name == intent.getStringExtra("demo_language") } ?: DisplayLanguage.EN)
        volumeControlStream = AudioManager.STREAM_MUSIC
        screen = MuseScreen(this, ::beginRecording, ::finishRecording, ::showControls, ::confirmClearHistory)
        status = screen.status
        message = screen.message
        languageMode = LanguageMode(
            languagePreferences.getBoolean("interpreting", false),
            InputLanguage.entries.firstOrNull { it.name == languagePreferences.getString("input", "AUTO") } ?: InputLanguage.AUTO,
            InputLanguage.entries.firstOrNull { it.name == languagePreferences.getString("target", "JAPANESE") && it != InputLanguage.AUTO } ?: InputLanguage.JAPANESE)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(android.graphics.Color.rgb(10,11,10)) }
        fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
        fun button(label: String = "", action: () -> Unit) = Button(this).apply {
            text = label; textSize = 12f; isAllCaps = false; minWidth = 0; minimumWidth = 0
            minHeight = 0; minimumHeight = 0; setPadding(dp(6), 0, dp(6), 0)
            elevation = 0f; stateListAnimator = null
            setOnClickListener { if (!recording && !sending && !continuous) { speech.stop(); action() } }
        }
        val modeRow = LinearLayout(this).apply { gravity = android.view.Gravity.CENTER; setPadding(dp(16), 0, dp(16), 0) }
        conversationButton = button("대화") { languageMode = languageMode.copy(interpreting = false); saveLanguageMode() }
        modeButton = button("통역") {
            if(funModes.mode=="practice")endFunMode()
            if (store.elevenLabs() == null) { screen.showNotice("ElevenLabs 등록 후 사용할 수 있습니다."); return@button }
            languageMode = languageMode.copy(interpreting = true); saveLanguageMode()
        }
        continuousButton = button {}
        continuousButton.setOnClickListener { if (continuous) stopContinuous() else startContinuous() }
        modeRow.addView(conversationButton, LinearLayout.LayoutParams(0, -1, 1f))
        modeRow.addView(modeButton, LinearLayout.LayoutParams(0, -1, 1f))
        modeRow.addView(continuousButton, LinearLayout.LayoutParams(0, -1, 1.6f).apply { leftMargin = dp(4) })
        displayLanguageButton = button { showDisplayLanguages() }
        modeRow.addView(displayLanguageButton, LinearLayout.LayoutParams(dp(56), -1).apply { leftMargin = dp(4) })
        languageRow = LinearLayout(this).apply { gravity = android.view.Gravity.CENTER; setPadding(dp(16), 0, dp(16), 0) }
        inputButton = button { chooseLanguage(false) }
        targetButton = button { chooseLanguage(true) }
        swapButton = button {
            if (languageMode.input == InputLanguage.AUTO) screen.showNotice("방향 교환 전 입력 언어를 선택하세요.")
            else { languageMode = languageMode.swapped(); saveLanguageMode() }
        }
        languageRow.addView(inputButton, LinearLayout.LayoutParams(0, -1, 1f))
        languageRow.addView(swapButton, LinearLayout.LayoutParams(dp(48), -1))
        languageRow.addView(targetButton, LinearLayout.LayoutParams(0, -1, 1f))
        batteryStatus = TextView(this).apply {
            textSize = 11f; gravity = android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, dp(16), 0); includeFontPadding = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }
        val deviceStatusRow=LinearLayout(this).apply { gravity=android.view.Gravity.CENTER_VERTICAL;setPadding(dp(16),0,0,0) }
        networkStatus=NetworkStatusView(this)
        deviceStatusRow.addView(networkStatus,LinearLayout.LayoutParams(dp(120),-1))
        deviceStatusRow.addView(batteryStatus,LinearLayout.LayoutParams(0,-1,1f))
        root.addView(deviceStatusRow, LinearLayout.LayoutParams(-1, dp(20)))
        refreshBatteryStatus()
        root.addView(modeRow, LinearLayout.LayoutParams(-1, dp(48)))
        root.addView(languageRow, LinearLayout.LayoutParams(-1, dp(48)))
        funStatus=button { if(funModes.mode=="practice")showPracticeControls() else showHunt() }.apply {
            textSize=12f; gravity=android.view.Gravity.CENTER
            includeFontPadding=false; maxLines=2
            minHeight=dp(40);minimumHeight=dp(40)
            setPadding(dp(12),dp(6),dp(12),dp(6))
            setTextColor(android.graphics.Color.rgb(242,233,221))
            background=android.graphics.drawable.GradientDrawable().apply {
                cornerRadius=dp(12).toFloat()
                setColor(android.graphics.Color.rgb(23,24,23))
                setStroke(dp(1),android.graphics.Color.rgb(55,56,54))
            }
        }
        funStatus.setOnClickListener { if(!recording) { stopContinuous(false); if(funModes.mode=="practice")showPracticeControls() else showHunt() } }
        root.addView(funStatus,LinearLayout.LayoutParams(-1,-2).apply { leftMargin=dp(16);rightMargin=dp(16);topMargin=dp(2);bottomMargin=dp(2) })
        refreshFunStatus()
        root.addView(screen, LinearLayout.LayoutParams(-1, 0, 1f))
        clockStatus = Button(this).apply {
            textSize = 12f; isAllCaps = false; gravity = android.view.Gravity.CENTER
            minWidth = 0; minimumWidth = 0; minHeight = dp(40); minimumHeight = dp(40)
            setPadding(dp(8), dp(5), dp(8), dp(5))
            elevation = 0f; stateListAnimator = null
            setTextColor(android.graphics.Color.rgb(255, 164, 99))
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = dp(12).toFloat()
                setColor(android.graphics.Color.rgb(29, 28, 25))
                setStroke(dp(1), android.graphics.Color.rgb(65, 54, 43))
            }
            visibility = View.GONE
            setOnClickListener { stopContinuous(false); showClockPanel() }
        }
        root.addView(clockStatus, LinearLayout.LayoutParams(-1, -2).apply {
            leftMargin = dp(16); rightMargin = dp(16); bottomMargin = dp(2)
        })
        val cameraBar = android.widget.FrameLayout(this)
        val cameraButton = android.widget.ImageButton(this).apply {
            setImageResource(R.drawable.ic_camera)
            scaleType = android.widget.ImageView.ScaleType.CENTER
            contentDescription = UiText.text(this@MainActivity, "카메라 열기", "カメラを開く", "Open camera")
            minimumWidth = 0; minimumHeight = 0
            setPadding(0, 0, 0, 0); elevation = 0f; stateListAnimator = null
            val circle = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(android.graphics.Color.rgb(255,139,66))
            }
            background = android.graphics.drawable.InsetDrawable(circle, dp(8))
            setOnClickListener {
                stopContinuous(false); finishRecording(false); speech.stop()
                startActivity(Intent(this@MainActivity, CameraActivity::class.java))
            }
        }
        cameraBar.addView(cameraButton, android.widget.FrameLayout.LayoutParams(dp(48), dp(48), android.view.Gravity.START).apply { leftMargin = dp(12) })
        val keyboardButton = android.widget.ImageButton(this).apply {
            setImageResource(R.drawable.ic_keyboard)
            contentDescription = UiText.text(this@MainActivity, "글로 대화", "文字で会話", "Type a message")
            setPadding(0, 0, 0, 0); minimumWidth = 0; minimumHeight = 0
            elevation = 0f; stateListAnimator = null
            val circle = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(android.graphics.Color.rgb(255,139,66))
            }
            background = android.graphics.drawable.InsetDrawable(circle, dp(8))
            setOnClickListener { showComposer() }
        }
        cameraBar.addView(keyboardButton, android.widget.FrameLayout.LayoutParams(dp(48), dp(48), android.view.Gravity.START).apply { leftMargin = dp(60) })
        settingsButton=android.widget.ImageButton(this).apply {
            setImageResource(R.drawable.ic_settings);contentDescription=funText("설정","設定","Settings")
            minimumWidth=0;minimumHeight=0;setPadding(0,0,0,0);elevation=0f;stateListAnimator=null
            background=android.graphics.drawable.InsetDrawable(android.graphics.drawable.GradientDrawable().apply {
                shape=android.graphics.drawable.GradientDrawable.OVAL;setColor(android.graphics.Color.rgb(255,139,66))
            },dp(8))
            setOnClickListener { showSettingsMenu() }
        }
        cameraBar.addView(settingsButton,android.widget.FrameLayout.LayoutParams(dp(48),dp(48),android.view.Gravity.END).apply{rightMargin=dp(12)})
        quietMode = getSharedPreferences("reply_options", MODE_PRIVATE).getBoolean("quiet", false)
        quickButton = Button(this).apply {
            textSize = 12f; isAllCaps = false; minWidth = 0; minimumWidth = 0
            setPadding(0, 0, 0, 0); elevation = 0f; stateListAnimator = null
            setOnClickListener { if (stopControlVisible || (::speech.isInitialized && speech.hasPlayback)) stopReplySpeech() else showQuickQuestions() }
        }
        paintButton(quickButton, false)
        cameraBar.addView(quickButton, android.widget.FrameLayout.LayoutParams(dp(120), dp(48), android.view.Gravity.CENTER))
        quietButton = android.widget.ImageButton(this).apply {
            setPadding(0, 0, 0, 0); minimumWidth = 0; minimumHeight = 0; elevation = 0f; stateListAnimator = null
            setOnClickListener {
                toggleReplyMute()
                android.widget.Toast.makeText(this@MainActivity, UiText.text(this@MainActivity, if (quietMode) "조용한 모드 켜짐" else "음성 답변 켜짐", if (quietMode) "サイレントモード ON" else "音声応答 ON", if (quietMode) "Quiet mode on" else "Voice replies on"), android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        cameraBar.addView(quietButton, android.widget.FrameLayout.LayoutParams(dp(48), dp(48), android.view.Gravity.END).apply { rightMargin = dp(60) })
        root.addView(cameraBar, LinearLayout.LayoutParams(-1, dp(48)))
        setContentView(root)
        refreshLanguageControls()
        enterFullscreen()
        speech = SpeechOutput(this) { state ->
            if (recording || sending) return@SpeechOutput
            updateStatus(state)
            if (continuous) {
                if (state == "READY") scheduleListening()
                else if (state.contains("실패") || state == "AUDIO BUSY" || state == "AUDIO INTERRUPTED") stopContinuous()
            }
            if (state != "MUSE IS SPEAKING" && !recording && !sending)
                releaseScreenAwake()
        }
        speech.onReading = { text, offset -> screen.followSpeech(text, offset) }
        speech.onReadingEnd = { screen.endSpeechFollow() }
    }

    private fun paintButton(button: Button, selected: Boolean) {
        val orange = android.graphics.Color.rgb(255,139,66)
        val body = android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = 100f
            setColor(if (selected) orange else android.graphics.Color.rgb(23,24,23))
            setStroke(1, if (selected) orange else android.graphics.Color.rgb(55,56,54))
        }
        val inset = (8 * resources.displayMetrics.density).toInt()
        button.background = android.graphics.drawable.InsetDrawable(body, 3, inset, 3, inset)
        button.setTextColor(if (selected) android.graphics.Color.BLACK else android.graphics.Color.rgb(242,233,221))
        button.alpha = if (button.isEnabled) 1f else .45f
    }

    private fun showDisplayLanguages() {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.rgb(23,24,23)); cornerRadius = 20f; setStroke(1, android.graphics.Color.DKGRAY)
            }
        }
        content.addView(TextView(this).apply { text = tr("표시 언어"); textSize = 12f; setTextColor(android.graphics.Color.LTGRAY); setPadding(12, 8, 12, 10) })
        val selected = UiText.language(this)
        for (language in DisplayLanguage.entries) {
            content.addView(TextView(this).apply {
                text = "${language.flag}  ${language.label}" + if (selected == language) "  ✓" else ""
                textSize = 16f; gravity = android.view.Gravity.CENTER_VERTICAL
                setTextColor(if (selected == language) android.graphics.Color.rgb(255,139,66) else android.graphics.Color.WHITE)
                setPadding(12, 0, 12, 0)
                contentDescription = language.label
                setOnClickListener {
                    UiText.set(this@MainActivity, language)
                    displayLanguagePopup?.dismiss()
                    refreshLanguageControls(); screen.refreshDisplayLanguage()
                    if (hasConversation) renderConversation()
                }
            }, LinearLayout.LayoutParams(-1, (48 * resources.displayMetrics.density).toInt()))
        }
        displayLanguagePopup = android.widget.PopupWindow(content, (190 * resources.displayMetrics.density).toInt(), -2, true).apply {
            setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
            elevation = 8f; isOutsideTouchable = true
            showAsDropDown(displayLanguageButton, 0, 0, android.view.Gravity.END)
        }
    }

    private fun refreshQuickControls() {
        if (!::quietButton.isInitialized) return
        stopControlVisible=::speech.isInitialized && speech.hasPlayback
        quickButton.text = if(stopControlVisible)UiText.text(this,"■ 음성 정지","■ 音声停止","■ Stop voice")else UiText.text(this, "빠른 기능", "クイック機能", "Quick actions")
        quickButton.contentDescription=quickButton.text
        paintButton(quickButton,stopControlVisible)
        quietButton.setImageResource(if (quietMode) R.drawable.ic_quiet else R.drawable.ic_speaker)
        quietButton.imageTintList=android.content.res.ColorStateList.valueOf(if(quietMode)android.graphics.Color.rgb(162,154,142)else android.graphics.Color.BLACK)
        quietButton.contentDescription = UiText.text(this, if (quietMode) "음성 OFF: 눌러서 켜기" else "음성 ON: 눌러서 끄기", if (quietMode) "音声 OFF: タップしてオン" else "音声 ON: タップしてオフ", if (quietMode) "Voice OFF: tap to enable" else "Voice ON: tap to disable")
        val circle = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.OVAL
            setColor(if (!quietMode) android.graphics.Color.rgb(255,139,66) else android.graphics.Color.rgb(23,24,23))
            setStroke(1, android.graphics.Color.rgb(55,56,54))
        }
        quietButton.background = android.graphics.drawable.InsetDrawable(circle, (8 * resources.displayMetrics.density).toInt())
    }

    internal fun stopReplySpeech(){
        replySpeechStopped=true
        speech.stop()
        releaseScreenAwake()
        refreshQuickControls()
        updateStatus("READY")
        if(continuous && !sending && !recording)scheduleListening()
    }

    private var petRoom:PetRoom?=null
    private val inPetMode get()=petRoom?.isShowing==true
    private var petRewardedTurn:ConversationTurn?=null
    private var activePetTurn=false
    private var petDialogueTurn:PetDialogueTurn?=null
    private var petDialogueRoom:PetRoom?=null
    private fun preparePetDialogue() {
        missionPhoto=null;missionConversation=null;missionChunks.clear();missionDone.clear()
        petDialogueRoom=petRoom?.takeIf { it.isShowing }
        petDialogueTurn=petDialogueRoom?.let { PetDialogueTurn(it.dialogueState(),it.practiceLanguage ?: UiText.language(this).name.lowercase(java.util.Locale.ROOT),it.activityContext()) }
    }
    internal val petConversationActivity get()=when { recording->"listening";sending->"thinking";speech.hasPlayback->"speaking";else->"" }

    private fun showPetRoom() {
        if(recording || sending || petRoom!=null)return
        stopContinuous(false);speech.stop();closeDeskClock()
        val room=PetRoom(this);petRoom=room
        room.setOnDismissListener { petRoom=null;lastInteraction=android.os.SystemClock.elapsedRealtime() }
        room.show()
    }
    internal fun petPractice(language:String?) { petRoom?.practiceLanguage=language }
    internal fun petMemoryText():String? {
        val current=history.lastOrNull { !it.user.isNullOrBlank() && it.replies.values.any { reply->reply.isNotBlank() } }?:return null
        return (current.user.orEmpty()+"\n\n"+current.replies.values.joinToString("\n")).take(1200)
    }
    private var petCameraOrigin:Bundle?=null
    private var petCameraReturned=false
    private val petCameraRequest=421
    internal fun petMissionCamera(seed:Long,token:String,target:Int) {
        if(!connected || sending || recording){android.widget.Toast.makeText(this,funText("Muse 연결 후 시도해 주세요","Museに接続してから試してね","Connect to Muse first"),android.widget.Toast.LENGTH_LONG).show();return}
        launchPetMissionCamera(seed,token,target)
    }
    @Suppress("DEPRECATION")
    private fun launchPetMissionCamera(seed:Long,token:String,target:Int) {
        petCameraOrigin=Bundle().apply { putLong("seed",seed);putString("token",token) }
        petCameraReturned=false
        val lang=when(UiText.language(this)){DisplayLanguage.JA->InputLanguage.JAPANESE;DisplayLanguage.EN->InputLanguage.ENGLISH;else->InputLanguage.KOREAN}
        try {
            startActivityForResult(Intent(this,CameraActivity::class.java).putExtra("hunt_prompt",PetActivityText.target(this,target))
                .putExtra("hunt_language",lang.name).putExtra("pet_seed",seed).putExtra("pet_mission_token",token).putExtra("pet_mission_target",target),petCameraRequest)
        } catch(error:RuntimeException) { petCameraOrigin=null;throw error }
    }
    @Deprecated("Activity result callback for the in-app mission camera")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==petCameraRequest && petCameraOrigin!=null)petCameraReturned=true
    }
    override fun onSaveInstanceState(state:Bundle) {
        state.putBundle("pet_camera_origin",petCameraOrigin)
        state.putBoolean("pet_camera_returned",petCameraReturned)
        super.onSaveInstanceState(state)
    }
    private fun restorePetCamera() {
        if(!petCameraReturned)return
        val origin=petCameraOrigin?:run {petCameraReturned=false;return}
        val seed=origin.getLong("seed")
        if(PetStore(this).load(System.currentTimeMillis()).seed!=seed) {
            if(PhotoQuestion.pending?.let {it.petSeed==seed && it.petMissionToken==origin.getString("token")}==true)PhotoQuestion.pending=null
            petCameraOrigin=null;petCameraReturned=false;return
        }
        showPetRoom()
        val room=petRoom?.takeIf {it.isShowing}?:return
        petCameraOrigin=null;petCameraReturned=false
        val photo=PhotoQuestion.pending
        if(photo!=null && photo.petSeed==seed && photo.petMissionToken==origin.getString("token")) {
            room.agentUpdate(funText("미션 사진 · 연결을 기다리고 있어요","ミッション写真 · 接続を待っています","Mission photo · Waiting for connection"),"",reveal=true)
        } else room.showMission()
    }
    private var missionPhoto:PhotoQuestion.Request?=null
    private var missionConversation:ConversationTurn?=null
    private val missionChunks=mutableMapOf<String,String>()
    private val missionDone=mutableSetOf<String>()
    override fun onNewIntent(next:Intent) { super.onNewIntent(next);setIntent(next);openPetPromise() }
    private fun openPetPromise() {
        val id=intent?.getStringExtra(PetPromises.EXTRA)?:return
        if(recording || sending)return
        intent.removeExtra(PetPromises.EXTRA)
        showPetRoom();petRoom?.showPromise(id)
    }
    internal fun petType(preset:String?=null) { showComposer(preset,plainConversation=true) }
    internal val petInputBusy get()=recording || sending
    internal fun petStopSpeech() { stopReplySpeech() }
    internal val replyMuted get() = quietMode
    internal fun toggleReplyMute() {
        quietMode = !quietMode
        getSharedPreferences("reply_options", MODE_PRIVATE).edit().putBoolean("quiet", quietMode).apply()
        if (quietMode) {
            speech.stop()
            if (continuous && !sending && !recording) scheduleListening()
        }
        refreshQuickControls()
        petRoom?.refreshReplyMute()
    }

    internal fun petRefresh() {
        val current=activeTurn ?: history.lastOrNull()
        val text=current?.let { listOf(it.user.orEmpty(), it.replies.values.joinToString("\n\n")).filter { part -> part.isNotBlank() }.joinToString("\n\n") }
        petRoom?.agentUpdate(if(connected)funText("Muse 연결됨","Muse接続済み","Muse connected") else funText("연결 안 됨 · 눌러 재연결","未接続 · タップで再接続","Offline · Tap to reconnect"),
            text?.takeIf { it.isNotBlank() }?.let { it.replace("**", "") } ?: funText("Muse에게 질문하거나 일을 부탁해 보세요.","Museに質問やお願いをしてみましょう。","Ask Muse a question or give it a task."))
    }
    internal fun petReconnect() {
        if(connected || sending || recording)return
        disconnect();connect();petRoom?.agentUpdate(funText("Muse 연결 중…","Museに接続中…","Connecting to Muse…"))
    }
    internal fun petBeginVoice():Boolean {
        if(petRoom?.hasWindowFocus()!=true || getSystemService(android.app.KeyguardManager::class.java).isKeyguardLocked)return false
        if(!connected || sending || recording || !historyLoaded) {
            petRoom?.agentUpdate(funText("연결 또는 응답을 기다려 주세요","接続・応答をお待ちください","Wait for connection or reply"),reveal=true);return false
        }
        petRoom?.muteAudio()
        stopContinuous(false);stopReplySpeech();beginRecording();sideButtonRecording=recording
        return recording
    }
    internal fun petEndVoice(send:Boolean) { finishRecording(send) }
    internal val petAudioBlocked get()=recording || sending || speech.hasPlayback


    private fun showQuickQuestions() {
        if (recording || sending || quickDialog != null || composer != null) return
        stopContinuous(); speech.stop()
        val labels = arrayOf(
            UiText.text(this, "사진 속 글자 번역", "写真の文字を翻訳", "Translate photo text"),
            UiText.text(this, "사진으로 물어보기", "写真で質問", "Ask about a photo"),
            UiText.text(this, "대화 요약", "会話を要約", "Summarize conversation"),
            UiText.text(this, "쉽게 설명해 줘", "わかりやすく説明", "Explain simply"),
            UiText.text(this, "즐겨찾기", "お気に入り", "Favorites"),
            UiText.text(this, "대화 검색", "会話検索", "Search conversations"),
            UiText.text(this, "음성 메모", "音声メモ", "Voice memos"),
            UiText.text(this, "이전 사진에 이어 질문", "前の写真に続けて質問", "Follow up on photo"),
            UiText.text(this, "글자 크기·음성 속도", "文字サイズ・音声速度", "Text size & voice speed"),
            UiText.text(this,"알람·타이머","アラーム・タイマー","Alarms & timers"),
            UiText.text(this,"화면 자동 꺼짐","画面の自動消灯","Screen timeout"),
            UiText.text(this,"일정·약속 리마인더","予定リマインダー","Appointment reminders"),
            UiText.text(this,"충전 중 탁상시계","充電中の置き時計","Charging desk clock"),
            funText("반응하는 아바타","アバターの動き","Reactive avatar"),funText("카메라 보물찾기","カメラ宝探し","Camera treasure hunt"),funText("한·일·영 회화 파트너","韓・日・英 会話パートナー","Conversation partner"),
            funText("뮤즈 키우기","Museを育てる","Raise Muse"))
        // Display order is separate from action IDs so reordered rows retain their behavior.
        val actionOrder = listOf(
            9, 11, 6,       // Daily tools: alarms, reminders, voice memos.
            0, 1, 7,        // Photo translation, questions, follow-up.
            2, 3, 4, 5,     // Conversation helpers and saved history.
            16, 15, 14      // Pet room, practice and play. Preferences live in Settings.
        )
        val orderedLabels = actionOrder.map { labels[it] }.toTypedArray()
        quickDialog = AlertDialog.Builder(this).setTitle(UiText.text(this, "빠른 기능", "クイック機能", "Quick actions"))
            .setItems(orderedLabels) { _, position ->
                val which = actionOrder[position]
                when (which) {
                    16 -> showPetRoom()
                    7 -> {
                        val photo = PhotoQuestion.last
                        if (photo == null) screen.showNotice(UiText.text(this, "먼저 사진을 찍어 Muse에 보내 주세요. 앱이 종료되면 사진은 기억하지 않습니다.", "先に写真をMuseに送ってください。アプリ終了後は写真を保持しません。", "Send a photo first. Photos are not retained after the app process ends."))
                        else controls = AlertDialog.Builder(this).setTitle(UiText.text(this, "이전 사진", "前の写真", "Previous photo"))
                            .setItems(arrayOf(UiText.text(this, "같은 사진으로 질문", "同じ写真で質問", "Ask with same photo"), UiText.text(this, "사진 기억 지우기", "写真を忘れる", "Forget photo"))) { _, choice ->
                                if (choice == 0) showComposer("", true, photo) else PhotoQuestion.last = null
                            }.show()
                    }
                    8 -> showReadingSettings()
                    9 -> showClockPanel()
                    10 -> showScreenTimeoutSettings()
                    11 -> showClockPanel(ClockCommand("reminder"))
                    12 -> showDeskClockSettings()
                    13 -> controls=AlertDialog.Builder(this).setTitle(funText("반응하는 아바타","アバターの動き","Reactive avatar"))
                        .setSingleChoiceItems(arrayOf(funText("켜기","オン","On"),funText("끄기","オフ","Off")),if(getSharedPreferences("gadget_ui",MODE_PRIVATE).getBoolean("reactive_avatar",true))0 else 1){d,index->getSharedPreferences("gadget_ui",MODE_PRIVATE).edit().putBoolean("reactive_avatar",index==0).apply();screen.invalidateAvatar();d.dismiss()}.show()
                    14 -> showHunt()
                    15 -> choosePractice()
                    4, 5, 6 -> startActivity(Intent(this, LibraryActivity::class.java).putExtra("mode", when(which) { 4 -> 1; 6 -> 2; else -> 0 }))
                    0, 1 -> startActivity(Intent(this, CameraActivity::class.java).putExtra("translate_photo", which == 0))
                    2 -> showComposer(UiText.text(this, "지금까지 대화의 핵심을 한국어로 짧게 요약해 줘.", "これまでの会話の要点を日本語で短くまとめて。", "Briefly summarize the key points of our conversation in English."), true)
                    3 -> showComposer(UiText.text(this, "방금 답변을 쉬운 말과 예시로 한국어로 설명해 줘.", "直前の回答をやさしい日本語と例で説明して。", "Explain your last answer in simple English with an example."), true)
                }
            }.setNegativeButton(tr("닫기"), null).create().also { dialog ->
                dialog.setOnDismissListener { quickDialog = null }; dialog.show()
            }
    }

    private val keepScreenAwake get() = getSharedPreferences("reading", MODE_PRIVATE).getBoolean("keep_screen_awake", false)

    private fun releaseScreenAwake() {
        if (keepScreenAwake) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    @Suppress("DEPRECATION")
    private fun wifiSettingsSummary():String {
        if(BuildConfig.DEMO)return "Offline demo · Sample Wi-Fi"
        val wifi=getSystemService(android.net.wifi.WifiManager::class.java)
        if(!wifi.isWifiEnabled)return funText("꺼짐 · 눌러 연결","オフ · タップして接続","Off · Tap to connect")
        val cm=getSystemService(android.net.ConnectivityManager::class.java)
        val caps=cm.allNetworks.mapNotNull { cm.getNetworkCapabilities(it) }
            .firstOrNull { it.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) && it.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) }
        return when {
            caps==null->funText("연결되지 않음 · 네트워크 선택","未接続 · ネットワークを選択","Not connected · Choose a network")
            caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED)->funText("연결됨 · 인터넷 사용 가능","接続済み · インターネット利用可","Connected · Internet available")
            caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)->funText("Wi-Fi 연결됨 · 로그인 필요","Wi-Fi接続済み · ログインが必要","Wi-Fi connected · Sign-in needed")
            else->funText("Wi-Fi 연결됨 · 인터넷 미확인","Wi-Fi接続済み · インターネット未確認","Wi-Fi connected · Internet unconfirmed")
        }
    }
    private fun showSettingsMenu() {
        if(recording || sending || composer!=null || settingsDialog?.isShowing==true)return
        stopContinuous(false);speech.stop();closeDeskClock()
        val connection=funText("연결","接続","Connection")
        val appearance=funText("화면·소리","画面・サウンド","Display & sound")
        fun entry(id:String,title:String,subtitle:String,section:String=appearance,action:()->Unit)=MuseSettingsDialog.Entry(id,title,subtitle,section,action)
        val entries=listOf(
            entry("wifi","Wi-Fi","",connection){openWifiPanel()},
            entry("reading",funText("글자 크기·음성 속도","文字サイズ・音声速度","Text size & voice speed"),funText("편하게 읽고 듣기","読みやすさ・聞きやすさ","Reading and speech preferences")){showReadingSettings()},
            entry("volume",funText("음량","音量","Volume"),funText("음성 답변·배경음 크기","音声応答・BGMの音量","Voice and music volume")){showVolumeSettings()},
            entry("screen",funText("화면 자동 꺼짐","画面の自動消灯","Screen timeout"),funText("Muse 화면 계속 켜기 선택","Muse画面の常時点灯","Choose whether Muse stays awake")){showScreenTimeoutSettings()},
            entry("clock",funText("충전 중 탁상시계","充電中の置き時計","Charging desk clock"),funText("시계 표시·전환 대기 시간","時計表示・切替待ち時間","Clock display and idle delay")){showDeskClockSettings()},
            entry("avatar",funText("반응하는 아바타","アバターの動き","Reactive avatar"),funText("대화 중 아바타 움직임","会話中のアバターの動き","Avatar animation during conversation")){showAvatarSettings()},
            entry("language",funText("표시 언어","表示言語","Display language"),"한국어 · 日本語 · English"){showSettingsLanguage()}
        )
        settingsDialog=MuseSettingsDialog(this,entries,::wifiSettingsSummary){
            if(connected)funText("Muse 연결됨 · 사이드 버튼·시스템","Muse接続済み · ボタン・システム","Muse connected · Buttons and system")
            else funText("Muse 연결 안 됨 · 페어링·사이드 버튼","Muse未接続 · ペアリング・ボタン","Muse offline · Pairing and side button")
        }.also { d-> d.setOnDismissListener{settingsDialog=null;lastInteraction=android.os.SystemClock.elapsedRealtime()};d.show() }
    }
    private fun openWifiPanel() {
        if(BuildConfig.DEMO)return
        // Password entry stays in Android's trusted Wi-Fi dialog; Muse never receives it.
        returnToSettings=true
        try { startActivity(Intent(Settings.Panel.ACTION_WIFI)) }
        catch(_:android.content.ActivityNotFoundException) {
            try { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
            catch(_:android.content.ActivityNotFoundException) { returnToSettings=false;android.widget.Toast.makeText(this,funText("Wi-Fi 설정을 열 수 없습니다","Wi-Fi設定を開けません","Wi-Fi settings unavailable"),android.widget.Toast.LENGTH_SHORT).show() }
        }
    }
    private fun showAvatarSettings() {
        controls=AlertDialog.Builder(this).setTitle(funText("반응하는 아바타","アバターの動き","Reactive avatar"))
            .setSingleChoiceItems(arrayOf(funText("켜기","オン","On"),funText("끄기","オフ","Off")),if(getSharedPreferences("gadget_ui",MODE_PRIVATE).getBoolean("reactive_avatar",true))0 else 1){d,index->
                getSharedPreferences("gadget_ui",MODE_PRIVATE).edit().putBoolean("reactive_avatar",index==0).apply();screen.invalidateAvatar();d.dismiss()
            }.setNegativeButton(tr("닫기"),null).show()
    }
    private fun showSettingsLanguage() {
        controls=AlertDialog.Builder(this).setTitle(funText("표시 언어","表示言語","Display language"))
            .setSingleChoiceItems(DisplayLanguage.entries.map{"${it.flag} ${it.label}"}.toTypedArray(),UiText.language(this).ordinal){d,index->
                UiText.set(this,DisplayLanguage.entries[index]);refreshLanguageControls();screen.refreshDisplayLanguage();if(hasConversation)renderConversation()
                d.dismiss();settingsDialog?.dismiss();showSettingsMenu()
            }.setNegativeButton(tr("닫기"),null).show()
    }
    private fun showVolumeSettings() {
        val audio=getSystemService(AudioManager::class.java)
        val content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(24,8,24,8)}
        val label=TextView(this).apply{textSize=16f;gravity=android.view.Gravity.CENTER}
        val slider=android.widget.SeekBar(this).apply{max=audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);progress=audio.getStreamVolume(AudioManager.STREAM_MUSIC)}
        fun update(){label.text="${slider.progress} / ${slider.max}"}
        slider.setOnSeekBarChangeListener(object:android.widget.SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(bar:android.widget.SeekBar,value:Int,fromUser:Boolean){if(fromUser)audio.setStreamVolume(AudioManager.STREAM_MUSIC,value,0);update()}
            override fun onStartTrackingTouch(bar:android.widget.SeekBar){}
            override fun onStopTrackingTouch(bar:android.widget.SeekBar){}
        })
        content.addView(label);content.addView(slider,LinearLayout.LayoutParams(-1,(64*resources.displayMetrics.density).toInt()));update()
        controls=AlertDialog.Builder(this).setTitle(funText("음량","音量","Volume")).setView(content).setNegativeButton(tr("닫기"),null).show()
    }

    private fun showScreenTimeoutSettings() {
        controls = AlertDialog.Builder(this)
            .setTitle(UiText.text(this, "화면 자동 꺼짐", "画面の自動消灯", "Screen timeout"))
            .setSingleChoiceItems(arrayOf(
                UiText.text(this, "켜기 · 일정 시간 후 꺼짐", "オン · 一定時間で消灯", "On · turn off after inactivity"),
                UiText.text(this, "끄기 · Muse 화면 계속 켜기", "オフ · Muse画面を点灯したまま", "Off · keep Muse screen on")
            ), if (keepScreenAwake) 1 else 0) { dialog, choice ->
                getSharedPreferences("reading", MODE_PRIVATE).edit().putBoolean("keep_screen_awake", choice == 1).apply()
                releaseScreenAwake()
                dialog.dismiss()
            }
            .setNegativeButton(tr("닫기"), null).show()
        if (keepScreenAwake) controls?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun showReadingSettings() {
        controls = AlertDialog.Builder(this).setTitle(UiText.text(this, "읽기·음성 설정", "文字・音声設定", "Reading & voice"))
            .setItems(arrayOf(UiText.text(this, "글자 크기", "文字サイズ", "Text size"), UiText.text(this, "음성 속도", "音声速度", "Voice speed"))) { _, choice ->
                val values = if(choice == 0) ReadingSettings.sizes else ReadingSettings.speeds
                val current = if(choice == 0) ReadingSettings.size(this) else ReadingSettings.speed(this)
                val labels = values.map { if(choice == 0) "${it.toInt()} sp" else "${it}×" }.toTypedArray()
                controls = AlertDialog.Builder(this).setTitle(if(choice == 0) UiText.text(this,"글자 크기","文字サイズ","Text size") else UiText.text(this,"음성 속도","音声速度","Voice speed"))
                    .setSingleChoiceItems(labels, values.indexOfFirst { it == current }) { d, index ->
                        getSharedPreferences("reading", MODE_PRIVATE).edit().putFloat(if(choice == 0) "size" else "speed", values[index]).apply()
                        if(choice == 0) { screen.refreshTextSize(); if(hasConversation) renderConversation() }
                        else speech.stop()
                        d.dismiss()
                    }.setNegativeButton(tr("닫기"),null).show()
            }.setNegativeButton(tr("닫기"),null).show()
    }

    private fun refreshLanguageControls() {
        refreshBatteryStatus()
        refreshFunStatus()
        refreshQuickControls()
        conversationButton.text = tr("대화"); modeButton.text = tr("통역")
        settingsButton.contentDescription=funText("설정","設定","Settings")
        displayLanguageButton.text = "${UiText.language(this).flag} ⌄"
        displayLanguageButton.contentDescription = tr("표시 언어")
        displayLanguageButton.isEnabled = !recording && !sending && !continuous
        paintButton(displayLanguageButton, false)
        languageRow.visibility = if (languageMode.interpreting) View.VISIBLE else View.GONE
        inputButton.text = "${tr(languageMode.input.label)} ⌄"
        inputButton.contentDescription = "입력 언어: ${languageMode.input.label}"
        targetButton.text = "${tr(languageMode.target.label)} ⌄"
        targetButton.contentDescription = "번역 언어: ${languageMode.target.label}"
        targetButton.visibility = if (languageMode.interpreting) View.VISIBLE else View.GONE
        swapButton.visibility = targetButton.visibility
        listOf(conversationButton, modeButton, inputButton, targetButton, swapButton).forEach { it.isEnabled = !recording && !sending && !continuous }
        continuousButton.visibility = if (languageMode.interpreting) View.VISIBLE else View.INVISIBLE
        continuousButton.isEnabled = continuous || (!recording && !sending)
        continuousButton.text = if (continuous) { if (recording) "● 듣는 중 · 중지" else "■ 중지" } else "▶ 연속 통역"
        continuousButton.text = tr(continuousButton.text.toString())
        continuousButton.contentDescription = if (continuous) "연속 통역 중지" else "연속 통역 시작"
        continuousButton.contentDescription = tr(continuousButton.contentDescription.toString())
        swapButton.text = "⇄"
        swapButton.contentDescription = "입력 언어와 번역 언어 교환"
        paintButton(conversationButton, !languageMode.interpreting)
        paintButton(modeButton, languageMode.interpreting)
        paintButton(inputButton, false); paintButton(targetButton, false); paintButton(swapButton, false)
        paintButton(continuousButton, continuous)
    }

    private fun startContinuous() {
        if (!languageMode.interpreting || recording || sending || !historyLoaded) return
        if (!connected || store.elevenLabs() == null) { screen.showNotice("Muse와 ElevenLabs 연결을 확인하세요."); return }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1); return
        }
        speech.stop()
        continuous = true
        startCapture(true)
        if (!recording) { continuous = false; refreshLanguageControls() }
    }

    private fun scheduleListening() {
        resumeListening?.cancel()
        resumeListening = scope.launch {
            delay(500) // Let speaker playback and its room echo settle.
            if (continuous && foreground === this@MainActivity && connected && !sending && !recording && !speech.hasPlayback) {
                startCapture(true)
                if (!recording) { continuous = false; refreshLanguageControls() }
            }
        }
    }

    private fun stopContinuous(reconnect: Boolean = true) {
        if (!continuous) return
        continuous = false; resumeListening?.cancel(); resumeListening = null
        val pending = sending
        sendJob?.cancel(); sendJob = null
        finishRecording(false); speech.stop(); turn++; activeTurn = null
        sending = false; transcriptPending = false; turnTimeout?.cancel()
        releaseScreenAwake()
        if (pending) { disconnect(); if (reconnect && foreground === this) connect() }
        updateStatus("연속 통역 중지")
    }

    private fun saveLanguageMode() {
        languagePreferences.edit().putBoolean("interpreting", languageMode.interpreting)
            .putString("input", languageMode.input.name).putString("target", languageMode.target.name).apply()
        refreshLanguageControls()
    }

    private fun chooseLanguage(target: Boolean) {
        if (store.elevenLabs() == null) { screen.showNotice("ElevenLabs 등록이 필요합니다."); return }
        val languages = InputLanguage.entries.filter { !target || it != InputLanguage.AUTO }
        languageDialog = AlertDialog.Builder(this).setTitle(tr(if (target) "번역할 언어" else "말하는 언어"))
            .setItems(languages.map { tr(it.label) }.toTypedArray()) { _, which ->
                languageMode = if (target) languageMode.copy(target = languages[which]) else languageMode.copy(input = languages[which])
                saveLanguageMode()
            }.setNegativeButton(tr("닫기"), null).show()
    }

    private fun loadHistory() {
        if (historyLoaded || historyLoad?.isActive == true) return
        historyLoad = scope.launch {
            try {
                history.addAll(historyStore.load())
                historyLoaded = true
                sendPendingPhoto()
                if (hasConversation) { renderConversation(); screen.jumpToLatest() }
                screen.setHistoryState(hasConversation, !recording && !sending)
            } catch (error: CancellationException) { throw error }
            catch (_: Exception) {
                updateStatus("HISTORY UNAVAILABLE")
                screen.showNotice("Couldn't open display history. Reopen Muse to try again.")
            }
        }
    }

    private fun enterFullscreen() {
        if (Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.apply {
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsets.Type.systemBars())
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterFullscreen()
    }

    private fun updateStatus(value: String) {
        screen.setState(value)
        petRoom?.agentUpdate(tr(value),reveal=recording || sending)
        refreshLanguageControls()
        screen.setHistoryState(hasConversation, historyLoaded && !recording && !sending && !continuous)
    }

    private fun confirmClearHistory() {
        if (!historyLoaded || !hasConversation || continuous || recording || sending ||
            controls?.isShowing == true || clearDialog?.isShowing == true) return
        clearDialog = AlertDialog.Builder(this)
            .setTitle(tr("Clear display history?"))
            .setMessage(tr("This clears only this r1's display history. Your conversation remains in Meta Muse, and Muse still remembers earlier turns."))
            .setNegativeButton(tr("Cancel"), null)
            .setPositiveButton(tr("Clear")) { _, _ ->
                if (recording || sending) return@setPositiveButton
                speech.stop()
                transcriptFetch?.cancel()
                transcriptPending = false
                turn++
                activeTurn = null
                history.clear()
                historyStore.save(history)
                screen.message.text = ""
                screen.showIdle()
                screen.setHistoryState(false, false)
            }
            .show()
    }

    private fun showControls() {
        if (continuous || recording || controls?.isShowing == true || clearDialog?.isShowing == true) return
        val paired = store.credentials() != null
        controls = AlertDialog.Builder(this)
            .setTitle(tr("Device controls"))
            .setItems(arrayOf(if (paired) "Reconnect to Muse" else "Pair with Muse", "Android settings",
                "Return to idle", "Show conversation", "Side button settings",
                funText("Muse 다시 페어링", "Museと再ペアリング", "Pair with Muse again")).map(::tr).toTypedArray()) { _, item ->
                when (item) {
                    0 -> if (paired) { disconnect(); connect() } else startPairing()
                    1 -> startActivity(Intent(Settings.ACTION_SETTINGS))
                    2 -> screen.showIdle()
                    3 -> if (hasConversation) renderConversation()
                    4 -> startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    5 -> { disconnect();repairPairingRequested=true;startPairing() }
                }
            }
            .setNegativeButton(tr("Back to Muse"), null)
            .show()
    }

    private fun renderConversation() {
        screen.showConversation(history, transcriptPending)
        if(inPetMode) {
            val current=activeTurn ?: history.lastOrNull()
            val text=current?.let { listOf(it.user.orEmpty(),it.replies.values.joinToString("\n\n")).filter { part -> part.isNotBlank() }.joinToString("\n\n") }
            petRoom?.agentUpdate(funText("Muse 대화","Museとの会話","Muse conversation"),text?.let { it.replace("**", "") },reveal=sending || recording)
        }
        screen.setHistoryState(hasConversation, historyLoaded && !recording && !sending && !continuous)
    }

    private fun receiveReply(id: String, text: String, done: Boolean) {
        val current = activeTurn ?: return
        var missionReply:String?=null
        val mission=missionPhoto?.takeIf { current===missionConversation }
        if(mission!=null) {
            if(id in missionDone)return
            val full=if(done && text.isNotEmpty())text else missionChunks.getOrDefault(id,"")+text
            if(!done){missionChunks[id]=full;return}
            missionDone.add(id);missionChunks.remove(id)
            val result=PetMission.parse(full)
            missionReply=result?.reply ?: funText("사진을 판정하지 못했어요. 보상은 지급되지 않았어요. 다시 촬영해 주세요.","写真を判定できませんでした。報酬は付与されていません。もう一度撮ってね。","I couldn't verify the photo. No reward was granted. Please try another photo.")
            val seed=mission.petSeed
            if(result?.matched==true && seed!=null && !PetStore(this).load(System.currentTimeMillis()).let { it.seed!=seed || it.dead }) {
                val granted=PetActivitiesStore(this).completeMission(seed,mission.petMissionToken.orEmpty())
                if(granted)missionReply+="\n"+funText("미션 성공! 방 꾸미기에 새 장식이 생겼어요.","ミッション達成！お部屋に新しい飾りが増えたよ。","Mission complete! A new room decoration is available.")
            }
        }
        val petAnswer = petDialogueTurn?.receive(id,text,done)
        if(petDialogueTurn!=null && petAnswer==null)return
        current.replies[id] = missionReply ?: petAnswer?.text ?: if (done && text.isNotEmpty()) text else current.replies.getOrDefault(id, "") + text
        historyStore.save(history)
        renderConversation()
        if (done) {
            if(inPetMode && current !== petRewardedTurn && current.replies.getValue(id).isNotBlank()) {
                petRoom?.agentReward();petRewardedTurn=current
            }
            if(petAnswer!=null && petRoom===petDialogueRoom && inPetMode)petRoom?.answerExpression(petAnswer.expression)
            sending = false; turnTimeout?.cancel(); updateStatus("REPLY RECEIVED")
            if (current.replies.getValue(id).isBlank() && continuous) { stopContinuous(); return }
            if (!quietMode && !replySpeechStopped) speech.speak(current.replies.getValue(id), petDialogueTurn?.language ?: if (activeLanguageMode.interpreting) activeLanguageMode.target.code!! else if(funModes.mode=="practice" && !activePetTurn)funModes.language.code!! else "ko")
            else {
                releaseScreenAwake()
                if (continuous) scheduleListening()
            }
        }
    }

    private var clockPanel:ClockPanel?=null
    private var clockDialog:android.app.Dialog?=null
    internal fun showClockPanel(command:ClockCommand?=null){
        closeDeskClock()
        if(recording)finishRecording(false)
        if(clockDialog!=null){command?.let{clockPanel?.command(it)};return}
        speech.stop()
        val d=android.app.Dialog(this,R.style.Theme_Muse)
        val panel=ClockPanel(this){d.dismiss()}
        clockPanel=panel;clockDialog=d;d.setContentView(panel.view)
        d.setOnDismissListener{panel.pause();clockPanel=null;clockDialog=null;refreshClockStatus()}
        d.show()
        d.window?.apply{
            setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT,android.view.ViewGroup.LayoutParams.MATCH_PARENT)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
        panel.resume();command?.let{panel.command(it)}
    }

    private fun handleLocalClock(text:String,voice:Boolean):Boolean {
        val interpreting=if(inPetMode || (voice && activePetTurn))false else if(voice)activeLanguageMode.interpreting else languageMode.interpreting
        val command=ReminderCommand.parse(text,interpreting) ?: LocalClockCommand.parse(text,interpreting) ?: return false
        sending=false;transcriptPending=false;turnTimeout?.cancel()
        releaseScreenAwake()
        val notice=UiText.text(this,"R1 알람·타이머 화면에서 확인해 주세요. 아직 설정을 확정하지 않았습니다.","R1のアラーム画面で確認してください。まだ確定していません。","Confirm on the R1 clock screen. Not scheduled yet.")
        if(!voice)activeTurn=ConversationTurn().also{it.user=text;history.add(it)}
        activeTurn?.replies?.set("local-clock",notice);historyStore.save(history);renderConversation();updateStatus("READY")
        showClockPanel(command)
        return true
    }

    private fun receiveTranscript(text: String) {
        val current = activeTurn ?: return
        current.user = text
        transcriptPending = false
        transcriptFetch?.cancel()
        historyStore.save(history)
        renderConversation()
    }

    override fun onResume() {
        super.onResume()
        foreground = this
        restorePetCamera()
        if (BuildConfig.DEMO) { networkStatus.showDemo(); showOfflineDemo(); return }
        networkStatus.start()
        if(!networkWatching) {
            networkWatching=true
            getSystemService(android.net.ConnectivityManager::class.java).registerDefaultNetworkCallback(connectionNetworkCallback)
        }
        lastInteraction=android.os.SystemClock.elapsedRealtime()
        if(returnToSettings) { returnToSettings=false;showSettingsMenu() }
        openPetPromise()
        settingsDialog?.refreshStatus()
        if(!batteryRegistered){registerReceiver(batteryReceiver,android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED));batteryRegistered=true}
        if (keepScreenAwake) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        quickButton.removeCallbacks(playbackControls);quickButton.post(playbackControls)
        clockStatus.removeCallbacks(clockStatusTicker);clockStatus.post(clockStatusTicker)
        clockPanel?.resume()
        loadHistory()
        shake.reset()
        sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensors.registerListener(shakeListener, it, SensorManager.SENSOR_DELAY_UI)
        }
        try {
            store.importPendingToken()
            store.importPendingElevenLabs()
            if (store.credentials() == null) {
                updateStatus("NOT PAIRED")
                if (hasConversation) renderConversation()
                else screen.showNotice(if (store.sdkToken() == null) "SDK token needed.\nThen pair with Muse on your phone."
                    else "Hold the empty background, choose Pair with Muse, then add a gadget on your phone.")
                if (pairingRequested && getSystemService(android.bluetooth.BluetoothManager::class.java).adapter.isEnabled) startPairing()
            } else if(pairingRequested && repairPairingRequested) startPairing()
            else if (connection == null && !repairPairingRequested) connect()
        } catch (_: Exception) { updateStatus("SECURE STORAGE ERROR"); screen.showNotice("Couldn't open the device credentials.") }
    }

    private var demoShown = false
    private fun showOfflineDemo() {
        if (demoShown) return
        demoShown = true
        val scene = intent.getStringExtra("demo_scene") ?: "chat"
        history.clear()
        if(scene.startsWith("pet-")) {
            val now=System.currentTimeMillis()
            val age=if(scene=="pet-adult") 10*PetState.DAY else 0L
            PetStore(this).save(PetState(updatedAt=now,seed=42L,hatched=scene!="pet-egg",ageMs=age,
                food=65.0,mood=88.0,energy=82.0,clean=90.0,warmth=78.0,
                form=if(age>0)PetForm.COMPANION else PetForm.UNDECIDED))
            getSharedPreferences("pet_audio",MODE_PRIVATE).edit().putString("mode","auto").putBoolean("effects",true).apply()
        }
        batteryPercent=86; charging=true; refreshBatteryStatus()
        getSharedPreferences("local_clock", MODE_PRIVATE).edit().remove("entries").commit()
        if(scene=="alarms" || scene=="home") {
            val due=System.currentTimeMillis()+3600000
            val fixtures=org.json.JSONArray().put(org.json.JSONObject().put("id","demo-reminder").put("kind","alarm").put("due",due).put("title","Weekend walk").put("hour",9).put("minute",0))
            getSharedPreferences("local_clock", MODE_PRIVATE).edit().putString("entries",fixtures.toString()).commit()
        }
        refreshClockStatus()
        val user = UiText.text(this, "산책 준비물을 알려줘.", "散歩の持ち物を教えて。", "What should I bring for a walk?")
        val answer = UiText.text(this, "물과 편한 신발을 챙기세요. 날씨도 확인해요.", "水と歩きやすい靴を。天気も確認しましょう。", "Bring water and comfortable shoes. Check the weather before leaving.")
        val turn = when (scene) {
            "translate" -> ConversationTurn("가까운 카페가 어디인가요?", linkedMapOf("demo" to "近くのカフェはどこですか？"), "일본어")
            "photo" -> ConversationTurn(UiText.text(this,"[사진] 이 안내문을 번역해 줘.","[写真] この案内を翻訳して。","[Photo] Translate this sign."), linkedMapOf("demo" to UiText.text(this,"OPEN 9:00–18:00\n영업시간: 오전 9시~오후 6시","OPEN 9:00–18:00\n営業時間：午前9時〜午後6時","OPEN 9:00–18:00\nOpening hours: 9 AM to 6 PM")))
            else -> ConversationTurn(user, linkedMapOf("demo" to answer))
        }
        history.add(turn); historyLoaded = true
        languageMode = LanguageMode(scene == "translate", InputLanguage.KOREAN, InputLanguage.JAPANESE)
        quietMode = scene != "avatar-speaking"; refreshLanguageControls(); refreshQuickControls(); renderConversation()
        status.text = "OFFLINE DEMO · SAMPLE DATA"
        scope.launch {
            historyStore.save(history)
            val memo = UiText.text(this@MainActivity,"물 사기\n산책 경로 확인\n카메라 충전","水を買う\n散歩コースを確認\nカメラを充電","Buy water\nCheck the walking route\nCharge the camera")
            LibraryStore(this@MainActivity).save(listOf(
                SavedItem("demo-favorite","favorite",user,answer,0),
                SavedItem("demo-memo","memo",UiText.text(this@MainActivity,"주말 준비","週末の準備","Weekend plan"),memo,0),
                SavedItem("demo-todo","todo",UiText.text(this@MainActivity,"주말 할 일","週末のタスク","Weekend checklist"),memo,0,memo.lines().mapIndexed { i,t -> TaskItem(t,i==0) })
            ))
            delay(300)
            when (scene) {
                "avatar-speaking" -> screen.setState("MUSE IS SPEAKING")
                "avatar-charging" -> { screen.setState("READY");screen.setCharging(true) }
                "clock" -> { deskClock=DeskClockDialog(this@MainActivity,{86}) { deskClock=null };deskClock?.show() }
                "alarms" -> showClockPanel()
                "reminder" -> showClockPanel(ClockCommand("reminder",title="Weekend walk"))
                "settings" -> showSettingsMenu()
                "reading" -> showReadingSettings()
                "quick" -> showQuickQuestions()
                "pet-egg", "pet-baby", "pet-adult", "pet-food", "pet-play", "pet-music", "pet-agent" -> {
                    showPetRoom()
                    if(scene=="pet-agent")petRoom?.agentUpdate("OFFLINE DEMO · SAMPLE DATA", "$user\n\n$answer", true)
                }
                "languages" -> showDisplayLanguages()
                "keyboard" -> showComposer(UiText.text(this@MainActivity,"일본어로 인사하는 법을 알려줘.","韓国語の挨拶を教えて。","How do I say hello in Korean?"))
                "tasks", "favorites" -> startActivity(Intent(this@MainActivity,LibraryActivity::class.java).putExtra("mode",if(scene=="tasks")2 else 1))
            }
        }
    }

    private fun startPairing() {
        if (store.sdkToken() == null) { screen.showNotice("Install your SDK token securely over USB first."); return }
        if (store.credentials() != null && !repairPairingRequested) { connect(); return }
        pairingRequested = true
        val needed = arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE)
            .filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isNotEmpty()) { requestPermissions(needed.toTypedArray(), 2); return }
        if (!getSystemService(android.bluetooth.BluetoothManager::class.java).adapter.isEnabled) {
            updateStatus("TURN ON BLUETOOTH"); startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); return
        }
        pairingWindow?.cancel()
        pairing?.close()
        pairing = PairingServer(this, store.identity, store::sdkToken, { credentials ->
            try { store.save(credentials); runOnUiThread { repairPairingRequested=false;disconnect();connect() }; true } catch (_: Exception) { false }
        }, { value -> runOnUiThread { updateStatus(value.replace('_', ' ').uppercase()) } })
        if (pairing!!.start()) {
            pairingRequested = false
            screen.showNotice("In Muse on your phone, add a gadget.\n\nChoose ${store.identity.bleName}.\n\nThis r1 uses its current Wi-Fi connection.")
            pairingWindow = scope.launch { delay(120_000); if (store.credentials() == null || repairPairingRequested) { pairing?.close(); pairing = null;repairPairingRequested=false; updateStatus("PAIRING CLOSED") } }
        }
    }

    private var reconnectJob:Job?=null
    private var reconnectAttempt=0
    private var networkWatching=false
    private var lastValidatedNetwork:android.net.Network?=null
    private val connectionNetworkCallback=object:android.net.ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(network:android.net.Network,caps:android.net.NetworkCapabilities) {
            if(!caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED))return
            runOnUiThread {
                if(!networkWatching || foreground!==this@MainActivity || network==lastValidatedNetwork)return@runOnUiThread
                lastValidatedNetwork=network
                if(!connected && connection==null) { reconnectAttempt=0;scheduleReconnect() }
            }
        }
        override fun onLost(network:android.net.Network) { runOnUiThread { if(network==lastValidatedNetwork)lastValidatedNetwork=null } }
    }
    private fun scheduleReconnect(authenticationRejected:Boolean=false) {
        if(foreground!==this || repairPairingRequested || connection!=null || reconnectJob?.isActive==true || reconnectAttempt>=3)return
        val wait=maxOf(longArrayOf(2000,5000,15000)[reconnectAttempt++],if(authenticationRejected)15000L else 0L)
        reconnectJob=scope.launch {
            delay(wait);reconnectJob=null
            if(foreground===this@MainActivity && connection==null)connect()
        }
    }
    private val connectionDiagnostics=mutableListOf<String>()
    @Synchronized private fun recordConnectionDiagnostic(value:String) {
        // Only stage names, HTTP codes and exception class names; never URLs, bodies or credentials.
        val safe=value.takeIf { it.matches(Regex("[A-Za-z0-9_]+")) } ?: "unknown"
        if(safe=="account_start")connectionDiagnostics.clear()
        connectionDiagnostics.add(safe)
        if(connectionDiagnostics.size>20)connectionDiagnostics.removeAt(0)
        val edit=getSharedPreferences("connection_diagnostic",MODE_PRIVATE).edit()
            .putString("steps",connectionDiagnostics.joinToString(" > ")).putLong("checked",System.currentTimeMillis())
        if(safe.startsWith("refresh_http_"))edit.putString("last_refresh",safe)
        if(safe.startsWith("websocket_failure_") || safe.startsWith("gateway_reason_"))edit.putString("last_failure",safe)
        edit.apply()
    }
    private fun connect() {
        if(BuildConfig.DEMO)return
        if (connection != null || repairPairingRequested) return
        val credentials = store.credentials() ?: return
        lateinit var current: MuseConnection
        current = MuseConnection(credentials, store.sdkToken(), store::save,
            { value -> runOnUiThread {
                if (connection !== current) return@runOnUiThread
                updateStatus(value.uppercase())
                if (value == "Disconnected" || value == "Connection lost") {
                    // A closed transport must not block connect() after the network recovers.
                    connection=null;connected=false
                    val wasSending=sending
                    stopContinuous(false);finishRecording(false)
                    sending=false;sendJob?.cancel();turnTimeout?.cancel();transcriptFetch?.cancel()
                    transcriptPending=false
                    if(hasConversation)renderConversation()
                    if(wasSending)petRoom?.agentUpdate(funText("연결 끊김 · 자동 재전송하지 않음","接続切断 · 自動再送しません","Disconnected · No automatic resend"),reveal=true)
                    if(current.pairingRejected) {
                        screen.showNotice(funText("Muse 인증을 다시 연결해야 합니다.\n빈 배경을 길게 누르고 ‘Muse 다시 페어링’을 선택하세요.\n대화 기록과 음성 설정은 유지됩니다.",
                            "Museの再ペアリングが必要です。\n背景を長押しして「Museと再ペアリング」を選んでください。履歴と音声設定は保持されます。",
                            "Muse pairing needs renewal.\nHold the background and choose Pair with Muse again. History and voice settings are preserved."))
                    } else scheduleReconnect(current.authenticationRejected)
                }
            } },
            { id, text, done -> runOnUiThread {
                if (connection !== current || recording) return@runOnUiThread
                receiveReply(id, text, done)
            } },
            { text -> runOnUiThread {
                if (connection !== current || recording) return@runOnUiThread
                if (!localTranscript) receiveTranscript(text)
            } }, ::recordConnectionDiagnostic, refreshDeviceId=store.identity.nodeId)
        connection = current
        scope.launch {
            try {
                withContext(Dispatchers.IO) { current.connect() }
                if (connection !== current) return@launch
                connected = true;reconnectAttempt=0;reconnectJob?.cancel();reconnectJob=null
                updateStatus("READY")
                if (hasConversation) renderConversation() else screen.showIdle()
                sendPendingPhoto()
            } catch (_: Exception) {
                if (connection === current) { connection = null; connected = false; updateStatus("CONNECTION FAILED"); screen.showNotice("Couldn't connect to Muse.\n\nHold the empty background to check Wi-Fi or reconnect.") }
            }
        }
    }

    private fun beginRecording() {
        if (continuous) { stopContinuous(); return }
        startCapture(false)
    }

    private fun startCapture(automatic: Boolean) {
        if(!inPetMode && funModes.mode=="practice" && store.elevenLabs()==null){screen.showNotice(funText("음성 회화 연습에는 ElevenLabs 음성 인식 설정이 필요합니다. 키보드로도 연습할 수 있습니다.","音声練習にはElevenLabsの設定が必要です。文字でも練習できます。","Voice practice needs ElevenLabs speech recognition. You can also practice with the keyboard."));return}
        if (!historyLoaded || recording || languageDialog?.isShowing == true || controls?.isShowing == true || clearDialog?.isShowing == true) return
        if (!connected || sending) {
            if (!connected) screen.showNotice(if (store.credentials() == null) "Pair this r1 with Muse on your phone first."
                else "Not connected.\nHold the empty background to reconnect.")
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1); return
        }
        speech.stop()
        lateinit var capture: VoiceRecorder
        capture = VoiceRecorder({ runOnUiThread {
            if (recorder === capture) recordingLimitReached()
        } }, { runOnUiThread {
            if (recorder === capture) { stopContinuous(false); finishRecording(false); updateStatus("MICROPHONE ERROR") }
        } }, automatic)
        recorder = capture
        try {
            capture.start(); recording = true
            transcriptFetch?.cancel()
            transcriptPending = false
            turn++
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            updateStatus(if (automatic) "● 마이크 켜짐 · 말이 끝나면 자동 번역" else "LISTENING")
            screen.showRecording(history)
        } catch (_: Exception) { recorder = null; updateStatus("MICROPHONE UNAVAILABLE") }
    }

    internal fun sideButtonNotice(text: String) { screen.showNotice(text) }

    internal val hasPlayback get() = speech.hasPlayback

    internal fun beginSideButtonRecording(): Boolean {
        if (deskClock?.isShowing == true) { closeDeskClock(); return false }
        if (clockDialog?.isShowing == true) return false
        if (hasPlayback) { stopContinuous(false); stopReplySpeech() }
        if (continuous) { stopContinuous(); return false }
        when {
            !hasWindowFocus() || languageDialog?.isShowing == true || controls?.isShowing == true || clearDialog?.isShowing == true ->
                sideButtonNotice("Close the dialog, then hold the side button to talk.")
            !historyLoaded -> sideButtonNotice("Display history is still loading. Try holding again in a moment.")
            recording -> sideButtonNotice("A recording is already in progress.")
            sending -> sideButtonNotice("Muse is still preparing your reply. Try holding again after it arrives.")
            else -> { beginRecording(); sideButtonRecording = recording; return recording }
        }
        return false
    }

    internal fun finishSideButtonRecording(send: Boolean) { finishRecording(send) }

    internal fun adjustMediaVolume(steps: Int) {
        val audio = getSystemService(AudioManager::class.java)
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val min = audio.getStreamMinVolume(AudioManager.STREAM_MUSIC)
        val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, (current.toLong() + steps).coerceIn(min.toLong(), max.toLong()).toInt(), 0)
        val volume = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        if (inPetMode) petRoom?.showMediaVolume(volume, max) else screen.showVolume(volume, max)
    }

    internal fun prepareForLock() {
        stopContinuous(false)
        finishRecording(false)
        speech.stop()
        releaseScreenAwake()
    }

    private fun recordingLimitReached() {
        if (!sideButtonRecording) { finishRecording(true); return }
        // Stop the microphone, but keep the capped audio cancellable until button release.
        cappedVoiceNote = recorder?.finish(); recorder = null
        updateStatus("RELEASE TO SEND")
        screen.showNotice("20-second limit reached.\n\nRelease to send.\nTurn the wheel to discard and adjust volume.")
    }

    private fun finishRecording(send: Boolean) {
        if (!recording) return
        recording = false
        sideButtonRecording = false
        val wav = cappedVoiceNote ?: recorder?.finish()
        cappedVoiceNote = null; recorder = null
        releaseScreenAwake()
        if (!send || wav == null) {
            if (hasConversation) renderConversation() else screen.showIdle()
            updateStatus(if (send) "HOLD A LITTLE LONGER" else "READY")
            if (continuous && send) scheduleListening()
            return
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        screen.markVoiceUsed()
        startVoiceTurn()
        val current = connection
        val thisTurn = turn
        sendJob = scope.launch {
            try {
                val eleven = store.elevenLabs()
                if (eleven != null) {
                    updateStatus(UiText.text(this@MainActivity, "${tr(activeLanguageMode.input.label)} 음성 인식 중", "${tr(activeLanguageMode.input.label)} 音声認識中", "Transcribing ${tr(activeLanguageMode.input.label)}"))
                    val text = ElevenLabsClient(eleven).transcribe(wav, activeLanguageMode.input.code)
                    ensureActive()
                    if (connection !== current || turn != thisTurn) return@launch
                    receiveTranscript(text)
                    if ((activePetTurn || funModes.mode!="practice") && handleLocalClock(text, true)) return@launch
                    updateStatus("MUSE에 전송 중")
                    withContext(Dispatchers.IO) { checkNotNull(current).sendText(petDialogueTurn?.payload(text) ?: if(funModes.mode=="practice" && !activePetTurn) practicePayload(text) else activeLanguageMode.message(text)) }
                } else {
                    val userId = withContext(Dispatchers.IO) { checkNotNull(current).sendVoice(wav, petDialogueTurn?.payload(null).orEmpty()) }
                    if (connection === current && turn == thisTurn && transcriptPending) fetchTranscript(current!!, userId, thisTurn)
                }
            }
            catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                if (connection === current && turn == thisTurn) {
                    stopContinuous(false)
                    sending = false; transcriptPending = false; turnTimeout?.cancel()
                    releaseScreenAwake()
                    updateStatus(if (error is ElevenLabsFailure) "음성 인식 실패 (HTTP ${error.status})" else "인식·전송 실패")
                    screen.showNotice("연결 상태와 ElevenLabs API 권한·잔여 사용량을 확인한 뒤 다시 말해 주세요.")
                }
            }
        }
        turnTimeout?.cancel()
        turnTimeout = scope.launch { delay(300_000); if (sending) { sending = false; disconnect(); updateStatus("REPLY TIMED OUT") } }
    }

    private fun showComposer(preset: String? = null, plainConversation: Boolean = false, photo: PhotoQuestion.Request? = null) {
        if (recording || sending || !historyLoaded || composer != null) {
            screen.showNotice(UiText.text(this, "현재 작업이 끝난 뒤 입력해 주세요.", "処理が終わってから入力してください。", "Wait for the current turn to finish.")); return
        }
        stopContinuous(); speech.stop()
        val input = android.widget.EditText(this).apply {
            setText(preset ?: typedDraft); setSelection(text.length)
            hint = UiText.text(this@MainActivity, "Muse에게 물어보세요", "Museに質問してください", "Ask Muse")
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            filters = arrayOf(android.text.InputFilter.LengthFilter(if(photo == null) 4000 else 1000))
            minLines = 2; maxLines = 4; textSize = 16f
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(if (photo == null) UiText.text(this, "글로 대화", "文字で会話", "Type a message") else UiText.text(this, "사진에 이어 질문", "写真に続けて質問", "Follow up on photo"))
            .setMessage(if (photo != null) UiText.text(this, "같은 사진을 질문과 함께 Muse에 다시 보냅니다.", "同じ写真を質問と一緒にMuseへ再送します。", "The same photo will be sent to Muse again with your question.") else null)
            .setView(input)
            .setNeutralButton(UiText.text(this, "입력 언어", "入力言語", "Language"), null)
            .setNegativeButton(UiText.text(this, "닫기", "閉じる", "Close"), null)
            .setPositiveButton(UiText.text(this, "보내기", "送信", "Send"), null).create()
        composer = dialog
        dialog.setOnDismissListener { typedDraft = input.text.toString(); composer = null }
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                input.requestFocus()
                getSystemService(android.view.inputmethod.InputMethodManager::class.java).showInputMethodPicker()
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val message = input.text.toString().trim()
                if (message.isBlank()) { input.error = UiText.text(this, "내용을 입력해 주세요", "内容を入力してください", "Enter a message"); return@setOnClickListener }
                if (((!connected || connection == null) && LocalClockCommand.parse(message,if(inPetMode)false else languageMode.interpreting)==null && ReminderCommand.parse(message,if(inPetMode)false else languageMode.interpreting)==null) || sending || recording) {
                    input.error = UiText.text(this, "Muse 연결 후 다시 보내 주세요", "Muse接続後に再度送信してください", "Connect to Muse before sending"); return@setOnClickListener
                }
                if (photo == null) sendTypedMessage(message, plainConversation)
                else { PhotoQuestion.pending = photo.copy(question = message.take(1000)); sendPendingPhoto() }
                input.setText(""); dialog.dismiss()
            }
            input.requestFocus()
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
            input.postDelayed({ if (composer === dialog) getSystemService(android.view.inputmethod.InputMethodManager::class.java).showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT) }, 150)
        }
        dialog.show()
    }

    private fun sendTypedMessage(message: String, plainConversation: Boolean = false) {
        if ((inPetMode || (funModes.mode!="practice" && !plainConversation)) && handleLocalClock(message, false)) return
        val current = connection ?: return
        activeLanguageMode = if (plainConversation || inPetMode) languageMode.copy(interpreting = false) else languageMode
        localTranscript = true
        replySpeechStopped=false
        activePetTurn=inPetMode
        preparePetDialogue()
        activeTurn = ConversationTurn(translationTarget = if (activeLanguageMode.interpreting) activeLanguageMode.target.label else null).also { it.user = message; history.add(it) }
        sending = true; transcriptPending = false
        historyStore.save(history); screen.jumpToLatest(); renderConversation()
        updateStatus(UiText.text(this, "Muse에 전송 중", "Museに送信中", "Sending to Muse"))
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val payload = petDialogueTurn?.payload(message) ?: if(funModes.mode=="practice" && !plainConversation && !inPetMode) practicePayload(message) else activeLanguageMode.message(message)
        sendJob = scope.launch {
            try { withContext(Dispatchers.IO) { current.sendText(payload) } }
            catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                if (connection === current) {
                    sending = false; turnTimeout?.cancel()
                    releaseScreenAwake()
                    updateStatus("SEND FAILED")
                    screen.showNotice(UiText.text(this@MainActivity, "전송을 확인하지 못했습니다. 자동 재전송하지 않습니다.", "送信を確認できませんでした。自動再送はしません。", "Delivery could not be confirmed. No automatic retry."))
                }
            }
        }
        turnTimeout?.cancel()
        turnTimeout = scope.launch { delay(180_000); if (sending) { disconnect(); updateStatus("REPLY TIMED OUT") } }
    }

    private fun sendPendingPhoto() {
        if(foreground===this)restorePetCamera()
        if(petCameraReturned)return
        if (!connected || !historyLoaded || sending || recording || foreground !== this) return
        val request = PhotoQuestion.pending ?: return
        PhotoQuestion.pending = null
        val current = connection ?: return
        PhotoQuestion.last = request.copy(petSeed=null,petMissionToken=null,petMissionTarget=-1)
        missionPhoto=request.takeIf { it.petSeed!=null && it.petMissionToken!=null && it.petMissionTarget in 0..2 };missionChunks.clear();missionDone.clear()
        speech.stop()
        activeLanguageMode = languageMode.copy(interpreting = true, target = request.replyLanguage)
        localTranscript = true
        replySpeechStopped=false
        activePetTurn=inPetMode
        petDialogueTurn=null;petDialogueRoom=null
        activeTurn = ConversationTurn().also { it.user = "[사진] " + request.question; history.add(it) }
        missionConversation=if(missionPhoto!=null)activeTurn else null
        sending = true; transcriptPending = false
        historyStore.save(history); screen.jumpToLatest(); renderConversation()
        updateStatus(UiText.text(this, "사진을 Muse에 전송 중", "写真をMuseに送信中", "Sending photo to Muse"))
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        sendJob = scope.launch {
            try { withContext(Dispatchers.IO) { current.sendPhoto(request.jpeg, if(missionPhoto===request)PetMission.prompt(request.petMissionTarget,request.replyLanguage.code?:"ko") else request.question) } }
            catch (error: Exception) {
                currentCoroutineContext().ensureActive()
                if (connection === current) {
                    sending = false; turnTimeout?.cancel()
                    releaseScreenAwake()
                    updateStatus("PHOTO SEND FAILED")
                    screen.showNotice(UiText.text(this@MainActivity, "사진 전송을 확인하지 못했습니다. 자동으로 재전송하지 않습니다.", "写真の送信を確認できませんでした。自動再送はしません。", "Photo delivery could not be confirmed. It will not be resent automatically."))
                }
            }
        }
        turnTimeout?.cancel()
        turnTimeout = scope.launch { delay(180_000); if (sending) { disconnect(); updateStatus("REPLY TIMED OUT") } }
    }

    private fun startVoiceTurn() {
        activeLanguageMode = if(inPetMode)languageMode.copy(interpreting=false,input=InputLanguage.entries.firstOrNull { it.code==petRoom?.practiceLanguage }?:languageMode.input) else if(funModes.mode=="practice")languageMode.copy(interpreting=false,input=funModes.language) else languageMode
        localTranscript = store.elevenLabs() != null
        replySpeechStopped=false
        activePetTurn=inPetMode
        preparePetDialogue()
        activeTurn = ConversationTurn(translationTarget = if (activeLanguageMode.interpreting) activeLanguageMode.target.label else null).also { history.add(it) }
        sending = true; transcriptPending = true
        historyStore.save(history)
        screen.jumpToLatest()
        renderConversation(); updateStatus("SENDING VOICE NOTE")
    }

    private fun fetchTranscript(current: MuseConnection, userId: String?, thisTurn: Long) {
        transcriptFetch = scope.launch {
            // History may be forbidden while live STT still arrives. Bound both paths together.
            withTimeoutOrNull(30_000) {
                var pollHistory = userId != null
                repeat(15) {
                    val text = if (pollHistory) {
                        try { withContext(Dispatchers.IO) { current.userTranscript(userId!!) } }
                        catch (_: Exception) {
                            currentCoroutineContext().ensureActive()
                            pollHistory = false
                            null
                        }
                    } else null
                    if (connection !== current || recording || turn != thisTurn) return@withTimeoutOrNull
                    if (!text.isNullOrBlank()) {
                        receiveTranscript(text); return@withTimeoutOrNull
                    }
                    delay(2_000)
                }
            }
            if (connection === current && !recording && turn == thisTurn && transcriptPending) {
                transcriptPending = false
                renderConversation()
            }
        }
    }

    internal fun conversationWheel(event: KeyEvent): Boolean {
        lastInteraction=android.os.SystemClock.elapsedRealtime()
        if (!hasWindowFocus() || clockDialog != null || composer?.isShowing == true ||
            quickDialog?.isShowing == true || controls?.isShowing == true) return false
        if (event.keyCode !in intArrayOf(KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN)) return false
        return screen.scrollConversation(if (event.keyCode == KeyEvent.KEYCODE_DPAD_UP) -1 else 1,
            event.action == KeyEvent.ACTION_DOWN)
    }

    override fun dispatchGenericMotionEvent(event: android.view.MotionEvent): Boolean {
        if (event.action == android.view.MotionEvent.ACTION_SCROLL && hasWindowFocus() && clockDialog == null) {
            val delta = event.getAxisValue(android.view.MotionEvent.AXIS_VSCROLL).let {
                if (it != 0f) it else event.getAxisValue(android.view.MotionEvent.AXIS_SCROLL)
            }
            if (delta != 0f && screen.scrollConversation(if (delta > 0) -1 else 1, true)) return true
        }
        return super.dispatchGenericMotionEvent(event)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        lastInteraction=android.os.SystemClock.elapsedRealtime()
        if (conversationWheel(event)) return true
        // The accessibility service owns this gesture globally. Never record via a second path.
        if (event.keyCode == KeyEvent.KEYCODE_PAIRING) {
            if (event.action == KeyEvent.ACTION_UP && SideButtonService.instance == null)
                sideButtonNotice("Enable Side button controls in Android accessibility settings.\n\nOpen device controls, then Side button settings. You can still hold the character to talk.")
            return true
        }
        if (event.keyCode == KeyEvent.KEYCODE_MENU) {
            if (event.action == KeyEvent.ACTION_UP) showControls()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun disconnect() {
        reconnectJob?.cancel();reconnectJob=null;reconnectAttempt=0
        continuous = false; resumeListening?.cancel(); sendJob?.cancel()
        if (recording) finishRecording(false)
        connected = false; sending = false; turnTimeout?.cancel(); transcriptFetch?.cancel()
        transcriptPending = false
        if (hasConversation && !recording) renderConversation()
        releaseScreenAwake();val previous=connection;connection=null;previous?.close()
    }
    override fun onPause() {
        petRoom?.cancelAgentInput()
        reconnectJob?.cancel();reconnectJob=null
        if(networkWatching) {
            networkWatching=false;lastValidatedNetwork=null
            getSystemService(android.net.ConnectivityManager::class.java).unregisterNetworkCallback(connectionNetworkCallback)
        }
        networkStatus.stop()
        closeDeskClock()
        if(batteryRegistered){unregisterReceiver(batteryReceiver);batteryRegistered=false}
        quickButton.removeCallbacks(playbackControls)
        clockStatus.removeCallbacks(clockStatusTicker)
        clockPanel?.pause()
        displayLanguagePopup?.dismiss()
        stopContinuous(false)
        SideButtonService.instance?.activityPaused(this)
        screen.hideVolume()
        if (foreground === this) foreground = null
        super.onPause()
    }
    override fun onStop() {
        settingsDialog?.dismiss()
        petRoom?.dismiss()
        quickDialog?.dismiss()
        composer?.dismiss()
        PhotoQuestion.pending = null
        sensors.unregisterListener(shakeListener)
        shake.reset()
        languageDialog?.dismiss()
        clearDialog?.dismiss()
        controls?.dismiss(); finishRecording(false); speech.stop(); pairing?.close(); pairing = null; disconnect()
        scope.coroutineContext.cancelChildren()
        super.onStop()
    }
    override fun onDestroy() { settingsDialog?.dismiss();clockDialog?.dismiss();speech.close(); scope.cancel(); super.onDestroy() }
    override fun onRequestPermissionsResult(code: Int, permissions: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, permissions, results)
        clockPanel?.permissionResult(code,results)
        if (code == 2 && results.all { it == PackageManager.PERMISSION_GRANTED }) startPairing()
    }

    companion object {
        internal var foreground: MainActivity? = null
            private set
    }
}

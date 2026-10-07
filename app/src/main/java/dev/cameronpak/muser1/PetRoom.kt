package dev.cameronpak.muser1

import android.animation.ValueAnimator
import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.*

/** Pet UI over the existing Muse session; all agent operations remain owned by MainActivity. */
class PetRoom(private val host: MainActivity) : Dialog(host, R.style.Theme_Muse) {
    private val resources get() = context.resources
    private val roomWindow get() = checkNotNull(window)
    internal fun hasWindowFocus() = window?.decorView?.hasWindowFocus() == true
    private val ink=Color.rgb(48,56,49)
    private val green=Color.rgb(77,110,76)
    private val cream=Color.rgb(246,241,223)
    private val handler=Handler(Looper.getMainLooper())
    private lateinit var audio:PetAudio
    private lateinit var store: PetStore
    private lateinit var pet: PetState
    private lateinit var room: PetScene
    private lateinit var growth: TextView
    private lateinit var notice: TextView
    private lateinit var hint: TextView
    private lateinit var stats: LinearLayout
    private lateinit var playRow: LinearLayout
    private val meters=mutableListOf<Pair<TextView,ProgressBar>>()
    private val actions=mutableListOf<Button>()
    private val tiles=mutableListOf<Button>()
    private var selected=0
    private var selectedTile=1
    private var press=-1L
    private val sideGesture=SideButtonGesture()
    private lateinit var replyMuteButton:ImageButton
    private var gameEnds=0L
    private var nextStar=0L
    private var star=0
    private var catches=0
    private var game=PetGame.STARS
    private var gameStarted=0L
    private var melody=listOf<Int>()
    private var melodyIndex=0
    private var missed=0
    private var previousSave=0L
    private var previousTick=0L
    private var messageUntil=0L
    private var effect=""
    private lateinit var dialogueScene:PetScene
    private lateinit var agentPane:LinearLayout
    private lateinit var agentText:TextView
    private lateinit var agentStatus:TextView
    private lateinit var agentScroll:ScrollView
    private var held=false
    private var voiceOwned=false
    private val voiceHold=Runnable {
        if(sideGesture.hold(SystemClock.uptimeMillis(),hasWindowFocus() && !inGame)==SideButtonGesture.Action.HOLD) {
            held=true;voiceOwned=host.petBeginVoice();showAgent()
        }
    }
    private fun endVoice(send:Boolean) { if(voiceOwned)host.petEndVoice(send);voiceOwned=false;held=false }
    internal fun cancelAgentInput() { handler.removeCallbacks(voiceHold);sideGesture.cancel();endVoice(false);press=-1 }
    private fun showAgent() { if(inGame)finishGame(false,false);room.hatchStarted=0L;agentPane.visibility=View.VISIBLE }
    internal fun agentUpdate(status:String, text:String?=null, reveal:Boolean=false) {
        if(!::agentPane.isInitialized)return
        agentStatus.showText(when(status) {
            "REPLY RECEIVED" -> t("답변 도착", "返事が届きました", "Reply received")
            "LISTENING" -> t("듣고 있어요", "聞いているよ", "Listening")
            "SENDING VOICE NOTE" -> t("생각하고 있어요", "考えているよ", "Thinking")
            else -> status
        })
        text?.let { agentText.showText(it) }
        if(reveal)showAgent()
    }
    internal fun agentReward() { record("chat");val before=pet.level;pet=pet.agentInteraction(System.currentTimeMillis());room.react(if(pet.level>before)"level" else "heart");save();render() }

    internal var practiceLanguage:String?=null
    internal fun activityContext():String = org.json.JSONObject()
        .put("away_on_expedition",onTrip)
        .put("room_decoration",PetActivityText.item(context,activitiesStore.decoration(pet.seed)))
        .put("conversation_practice_language",practiceLanguage ?: "none").toString()
    internal fun dialogueState():PetState { pet=pet.advance(System.currentTimeMillis());return pet }
    internal fun answerExpression(expression:PetExpression) {
        val action=PetDialogue.allowed(expression,dialogueState())
        if(action==PetExpression.NONE)return
        room.react(action.motion);dialogueScene.react(action.motion)
    }
    private val inGame get()=gameEnds>0L
    private fun t(ko:String,ja:String,en:String)=UiText.text(context,ko,ja,en)
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    private fun panel(color:Int,radius:Float=16f)=GradientDrawable().apply { setColor(color);cornerRadius=dp(radius.toInt()).toFloat() }
    private fun TextView.showText(value:CharSequence) { if(text.toString()!=value.toString()) text=value }
    private fun label(size:Float)=TextView(context).apply { textSize=size;setTextColor(ink);gravity=Gravity.CENTER;includeFontPadding=false }
    private fun button(text:String,action:()->Unit)=Button(context).apply {
        this.text=text;textSize=13f;isAllCaps=false;setTextColor(ink);minWidth=0;minimumWidth=0
        minHeight=0;minimumHeight=0;setPadding(dp(2),0,dp(2),0)
        background=panel(Color.rgb(232,229,207),12f);setOnClickListener { action() }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        roomWindow.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        audio=PetAudio(context)
        store=PetStore(context);pet=store.load(System.currentTimeMillis())
        val root=LinearLayout(context).apply { orientation=LinearLayout.VERTICAL;setPadding(dp(12),dp(6),dp(12),dp(6));setBackgroundColor(cream) }
        val header=LinearLayout(context).apply { gravity=Gravity.CENTER_VERTICAL }
        val title=label(17f).apply { text=if(BuildConfig.DEMO) "MUSE · DEMO" else "MUSE";gravity=Gravity.START or Gravity.CENTER_VERTICAL;typeface=Typeface.DEFAULT_BOLD;maxLines=1;setAutoSizeTextTypeUniformWithConfiguration(8,17,1,android.util.TypedValue.COMPLEX_UNIT_SP) }
        header.addView(title,LinearLayout.LayoutParams(0,-1,1f))
        header.addView(button("↺"){confirmNewEgg()}.apply { contentDescription=t("알부터 다시 시작","たまごからやり直す","Start from an egg") },LinearLayout.LayoutParams(dp(48),dp(48)).apply { rightMargin=dp(4) })
        replyMuteButton=ImageButton(context).apply {
            setPadding(dp(12),dp(12),dp(12),dp(12))
            background=panel(Color.rgb(232,229,207),12f)
            setOnClickListener {
                host.toggleReplyMute()
                if(host.replyMuted)say("음성 답변 꺼짐 · 말하기는 가능해요","音声応答オフ · 話しかけられます","Voice replies off · You can still talk")
                else say("음성 답변 켜짐","音声応答オン","Voice replies on")
            }
        }
        header.addView(replyMuteButton,LinearLayout.LayoutParams(dp(48),dp(48)).apply { rightMargin=dp(4) })
        refreshReplyMute()
        header.addView(button("♫"){showSoundSettings()}.apply { contentDescription=t("배경음과 효과음","BGMと効果音","Music and sounds") },LinearLayout.LayoutParams(dp(48),dp(48)).apply { rightMargin=dp(4) })
        header.addView(button(t("나가기","戻る","Exit")){dismiss()},LinearLayout.LayoutParams(dp(64),dp(48)))
        root.addView(header,LinearLayout.LayoutParams(-1,dp(48)))
        growth=label(11f).apply { gravity=Gravity.START or Gravity.CENTER_VERTICAL }
        root.addView(growth,LinearLayout.LayoutParams(-1,dp(26)))
        stats=LinearLayout(context)
        listOf(t("포만감","満腹","Food"),t("기분","気分","Mood"),t("체력","体力","Energy"),t("청결","清潔","Clean")).forEach { name ->
            val column=LinearLayout(context).apply { orientation=LinearLayout.VERTICAL;setPadding(dp(5),dp(3),dp(5),dp(5)) }
            val text=label(11f).apply { tag=name }
            val bar=ProgressBar(context,null,android.R.attr.progressBarStyleHorizontal).apply { max=100;progressTintList=android.content.res.ColorStateList.valueOf(green);progressBackgroundTintList=android.content.res.ColorStateList.valueOf(0xffdedfc8.toInt()) }
            column.addView(text,LinearLayout.LayoutParams(-1,dp(22)));column.addView(bar,LinearLayout.LayoutParams(-1,dp(7)))
            stats.addView(column,LinearLayout.LayoutParams(0,-1,1f));meters.add(text to bar)
        }
        root.addView(stats,LinearLayout.LayoutParams(-1,dp(42)))
        room=PetScene(context)
        val roomStack=FrameLayout(context)
        roomStack.addView(room,FrameLayout.LayoutParams(-1,-1))
        agentPane=LinearLayout(context).apply { orientation=LinearLayout.VERTICAL;setPadding(dp(12),dp(6),dp(12),dp(6));background=panel(0xffe8ecd6.toInt());visibility=View.GONE }
        agentStatus=label(11f).apply { gravity=Gravity.START or Gravity.CENTER_VERTICAL;setTextColor(green);setOnClickListener { host.petReconnect() } }
        agentPane.addView(agentStatus,LinearLayout.LayoutParams(-1,dp(28)))
        val dialogueBody=LinearLayout(context).apply { gravity=Gravity.CENTER_VERTICAL }
        dialogueScene=PetScene(context)
        dialogueBody.addView(dialogueScene,LinearLayout.LayoutParams(0,-1,0.38f).apply { rightMargin=dp(8) })
        agentText=label(14f).apply { gravity=Gravity.START;setPadding(dp(3),dp(5),dp(3),dp(5));setTextIsSelectable(true) }
        agentScroll=ScrollView(context).apply { addView(agentText);isFillViewport=true }
        dialogueBody.addView(agentScroll,LinearLayout.LayoutParams(0,-1,0.62f))
        agentPane.addView(dialogueBody,LinearLayout.LayoutParams(-1,0,1f))
        val returnRoom=button(t("방으로 · 음성 정지","お部屋へ · 音声停止","Room · Stop voice")){ host.petStopSpeech();practiceLanguage=null;agentPane.visibility=View.GONE }
        agentPane.addView(returnRoom,LinearLayout.LayoutParams(-1,dp(48)))
        roomStack.addView(agentPane,FrameLayout.LayoutParams(-1,-1))
        root.addView(roomStack,LinearLayout.LayoutParams(-1,0,1f).apply { topMargin=dp(6);bottomMargin=dp(4) })
        notice=label(12f).apply { maxLines=2;setPadding(dp(3),0,dp(3),0);accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE }
        root.addView(notice,LinearLayout.LayoutParams(-1,dp(36)))
        playRow=LinearLayout(context).apply { visibility=View.GONE }
        repeat(3) { i ->
            val b=button("·"){ selectedTile=i;catchStar() }.apply { contentDescription=t("별잡기 칸","星取りマス","Star tile")+" ${i+1}" }
            tiles.add(b);playRow.addView(b,LinearLayout.LayoutParams(0,dp(48),1f).apply { setMargins(dp(3),0,dp(3),dp(4)) })
        }
        root.addView(playRow,LinearLayout.LayoutParams(-1,dp(52)))
        val controls=LinearLayout(context)
        listOf(t("먹이","ごはん","Feed"),t("놀이","遊ぶ","Play"),t("돌봄","お世話","Care"),t("잠자기","寝る","Sleep")).forEachIndexed { i,name ->
            val b=button(name){ selected=i;activate(i) }
            actions.add(b);controls.addView(b,LinearLayout.LayoutParams(0,dp(52),1f).apply { setMargins(dp(3),dp(4),dp(3),0) })
        }
        root.addView(controls,LinearLayout.LayoutParams(-1,dp(60)))
        val agentBar=LinearLayout(context)
        val talk=button(t("말하기","話す","Talk")){}.apply {
            contentDescription=t("길게 눌러 Muse에게 말하기","長押しでMuseに話す","Hold to speak to Muse")
            setOnTouchListener { _,event ->
                when(event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> { if(!inGame) { held=true;voiceOwned=host.petBeginVoice();showAgent() } }
                    MotionEvent.ACTION_UP -> { endVoice(true);performClick() }
                    MotionEvent.ACTION_CANCEL -> endVoice(false)
                };true
            }
        }
        listOf(talk,
            button(t("글쓰기","入力","Type")){if(!inGame)host.petType()},
            button(t("대화 보기","会話を見る","View chat")){if(!inGame) { host.petRefresh();showAgent() }},
            button(t("특별 활동","特別活動","Activities")){showSpecialActivities()}
        ).forEach { b -> agentBar.addView(b,LinearLayout.LayoutParams(0,dp(48),1f).apply { setMargins(dp(3),0,dp(3),0) }) }
        root.addView(agentBar,LinearLayout.LayoutParams(-1,dp(50)))
        hint=label(10f).apply { text=t("길게 눌러 말하기 · 측면 버튼 + 휠: 음량","長押しで話す · サイドボタン＋ホイール：音量","Hold to talk · Side button + wheel: volume") }
        root.addView(hint,LinearLayout.LayoutParams(-1,dp(24)))
        setContentView(root);render()
    }
    internal fun refreshReplyMute() {
        if(!::replyMuteButton.isInitialized)return
        val muted=host.replyMuted
        replyMuteButton.setImageResource(if(muted)R.drawable.ic_quiet else R.drawable.ic_speaker)
        replyMuteButton.imageTintList=android.content.res.ColorStateList.valueOf(if(muted)Color.rgb(128,124,109) else green)
        replyMuteButton.isSelected=muted
        replyMuteButton.contentDescription=if(muted)t("음성 OFF: 눌러서 켜기","音声 OFF: タップしてオン","Voice OFF: tap to enable")
            else t("음성 ON: 눌러서 끄기","音声 ON: タップしてオフ","Voice ON: tap to disable")
    }
    internal fun showMediaVolume(volume:Int,max:Int) {
        say("음량 $volume / $max","音量 $volume / $max","Volume $volume / $max")
    }
    internal fun muteAudio() { if(::audio.isInitialized)audio.mute() }
    private val activitiesStore by lazy { PetActivitiesStore(context) }
    private val activities by lazy { PetActivities(host,::dialogueState) { room.react("heart");refreshActivities();render() } }
    private var onTrip=false
    private fun refreshActivities() {
        onTrip=activitiesStore.trip(pet.seed)!=null
        room.travelling=onTrip;dialogueScene.travelling=onTrip
        room.decoration=activitiesStore.decoration(pet.seed);dialogueScene.decoration=room.decoration
    }
    private fun record(kind:String,value:String="") { activitiesStore.event(pet.seed,kind,value) }
    internal fun showSpecialActivities() { if(!inGame && !host.petInputBusy){muteAudio();activities.show()} }
    internal fun showPromise(id:String) { if(!inGame && !host.petInputBusy)activities.promiseDetail(id) }
    private fun showSoundSettings() {
        val labels=arrayOf(t("성장에 맞춤","成長に合わせる","Follow growth"),t("숲 속 새소리","森の鳥の声","Forest birds"),t("포근한 오르골","やさしいオルゴール","Cozy music box"),t("밤의 정원","夜の庭","Night garden"),t("BGM 끄기","BGMオフ","Music off"))
        android.app.AlertDialog.Builder(context).setTitle(t("배경음 · 효과음","BGM・効果音","Music & sounds"))
            .setSingleChoiceItems(labels,PetSoundtrack.modes.indexOf(audio.mode)){dialog,index->audio.mode=PetSoundtrack.modes[index];dialog.dismiss()}
            .setNeutralButton(if(audio.effects)t("효과음 끄기","効果音オフ","Effects off") else t("효과음 켜기","効果音オン","Effects on")){_,_->audio.effects=!audio.effects}
            .setNegativeButton(t("닫기","閉じる","Close"),null).show()
    }
    private fun say(ko:String,ja:String,en:String) { notice.text=t(ko,ja,en);messageUntil=SystemClock.elapsedRealtime()+3500L }
    private fun render() {
        val stage=when(pet.level) { 0->t("알","たまご","Egg");1->t("아기 뮤즈","ベビーMuse","Baby Muse");2->t("어린 뮤즈","こどもMuse","Young Muse");3->t("자라는 뮤즈","育ちざかりMuse","Growing Muse");4->t("어른 뮤즈","おとなMuse","Adult Muse");else->t("오랜 친구","長年の友","Old friend") }
        val nature=when(pet.temperament) {
            PetTemperament.GRUMPY->t("삐쳐 있어요","すねています","Feeling grumpy")
            PetTemperament.SHY->t("조심스러워요","少し人見知り","Feeling shy")
            PetTemperament.AFFECTIONATE->t("다정해요","甘えん坊","Affectionate")
            PetTemperament.CURIOUS->t("호기심이 많아요","好奇心いっぱい","Curious")
            PetTemperament.PLAYFUL->t("장난꾸러기예요","遊び好き","Playful")
            else->t("느긋해요","のんびり","Easygoing")
        }
        growth.showText(when {pet.dead->t("함께한 시간을 기억해요","いっしょの時間を忘れない","Remembering our time together");!pet.hatched->t("작은 알과 함께하는 하루","小さなたまごとの一日","A day with a little egg");else->"$stage · $nature"})
        stats.visibility=if(!pet.hatched || pet.dead)View.GONE else View.VISIBLE
        listOf(pet.food,pet.mood,pet.energy,pet.clean).forEachIndexed { i,value ->
            val feeling=if(value<25)t("부족","不足","Low") else if(value<60)t("보통","普通","Okay") else t("좋음","良好","Good")
            meters[i].first.showText("${meters[i].first.tag} $feeling")
            meters[i].second.progress=when {value<25->15;value<60->50;else->85}
        }
        if(pet.hatched && pet.sleeping && !inGame) selected=3
        actions.forEachIndexed { i,b ->
            if(b.tag != (i==selected)) {
                b.tag=i==selected
                b.setTextColor(if(i==selected) Color.WHITE else ink)
                b.background=panel(if(i==selected) green else 0xffe6e3cf.toInt(),12f)
            }
            b.isEnabled=!onTrip && !pet.dead && !inGame && room.hatchStarted==0L && !host.petAudioBlocked && (!pet.hatched || !pet.sleeping || i==3)
            b.alpha=if(b.isEnabled)1f else 0.4f
        }
        val names=if(!pet.hatched)listOf(t("온기","温める","Warm"),t("토닥","なでる","Pat"),t("닦기","拭く","Wipe"),t("자장가","子守歌","Lullaby")) else listOf(t("먹이","ごはん","Feed"),t("놀이","遊ぶ","Play"),t("돌봄","お世話","Care"),if(pet.sleeping)t("깨우기","起こす","Wake") else t("잠자기","寝る","Sleep"))
        names.forEachIndexed { i,name->actions[i].showText(name) }
        if(SystemClock.elapsedRealtime()>messageUntil && !inGame) {
            effect=""
            notice.showText(when {
                onTrip->t("탐험 중 · 특별 활동에서 확인해 주세요","探検中 · 特別活動で確認してね","Exploring · Check Special activities")
                room.hatchStarted>0L->t("톡… 톡… 뮤즈가 나오고 있어요!","ぴき…ぴき…Museが生まれるよ！","Crack… crack… Muse is hatching!")
                pet.dead->t("뮤즈가 별이 되었어요. ↺로 새 알을 맞이할 수 있어요","Museは星になりました。↺で新しいたまごを迎えられます","Muse has become a star. Welcome a new egg with ↺.")
                !pet.hatched->t("어떤 친구를 만나게 될까요? 천천히 함께 기다려요","どんな子に会えるかな？ゆっくり待とう","Who will we meet? Let's wait together.")
                pet.ill->t("몸이 많이 안 좋아요. 밥과 휴식, 돌봄이 필요해요","具合が悪いよ。ごはんと休息、お世話が必要","I'm unwell. I need food, rest and care.")
                pet.food<=0->t("너무 배고파요… 오래 굶으면 생명이 위험해요","おなかぺこぺこ…長く食べないと危険です","I'm starving… going without food can be fatal.")
                pet.temperament==PetTemperament.GRUMPY->t("흥… 혼자 있는 건 싫어요","ふん…ひとりはいやだよ","Hmph… I don't like being left alone.")
                pet.sleeping->t("쿨쿨… 쉬면서 체력을 회복해요","すやすや…体力を回復中","Zzz… resting restores energy")
                pet.food<30->t("배가 고파요. 같이 밥 먹을까요?","おなかすいた！ごはんにしよう","I'm hungry. Time for a snack?")
                pet.energy<20->t("조금 졸려요. 쉬고 싶어요","ちょっと眠いな…休みたい","Feeling sleepy. Let's rest.")
                pet.clean<30->t("뽀송하게 씻고 싶어요","お風呂でさっぱりしたいな","I'd love a bath!")
                else->t("오늘도 같이 놀아요!","今日もいっしょに遊ぼう！","Let's spend some time together!")
            })
        }
        val description=t("뮤즈의 방","Museのお部屋","Muse's room")+" · $stage · "+notice.text
        if(room.contentDescription != description) room.contentDescription=description
        val awake=inGame || room.hatchStarted>0L || context.getSharedPreferences("reading",android.content.Context.MODE_PRIVATE).getBoolean("keep_screen_awake",false)
        val wasAwake=roomWindow.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0
        if(awake != wasAwake) {
            if(awake) roomWindow.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            else roomWindow.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        room.pet=pet
        dialogueScene.pet=pet
        dialogueScene.activity=host.petConversationActivity
        room.invalidate()
    }
    private fun save() { store.save(pet);previousSave=SystemClock.elapsedRealtime() }
    private fun activate(index:Int) {
        if(onTrip || pet.dead || inGame || room.hatchStarted>0L || host.petAudioBlocked)return
        val now=System.currentTimeMillis();pet=pet.advance(now)
        if(!pet.hatched) {
            val before=pet;pet=pet.care(PetAction.entries[index],now);save();if(pet!=before)record("care")
            room.react(when(index){0->"heart";1->"chirp";2->"wash";else->"sleep"})
            audio.effect(when(index){2->"wash";3->"sleep";else->"chirp"})
            say("알이 기분 좋게 흔들려요!","たまごがうれしそうに揺れてる！","The egg wiggles happily!");render();return
        }
        if(pet.sleeping && index!=3)return
        if(index==1) {
            if(pet.energy<10) { say("먼저 잠을 자고 체력을 채워 주세요","先に眠って体力を回復してね","Rest first to restore energy");render();return }
            chooseGame();return
        }
        if(index==0){chooseFood();return}
        if(index==2){chooseCare();return}
        val before=pet
        effect=""
        pet=pet.care(PetAction.entries[index],now);save();if(pet!=before && pet.sleeping)record("sleep")
        when(index) {
            0->{effect="food";if(before.food>=99)say("배불러요! 조금 있다 먹어요","おなかいっぱい！またあとで","I'm full! A snack later, please.") else say("냠냠! 맛있어요","もぐもぐ！おいしいね","Yum! That was delicious.")}
            2->{effect="wash";say("보송보송, 기분 좋아요!","ぴかぴか！気持ちいいね","Fresh and clean. Thank you!")}
            3->{if(pet.sleeping) say("잘 자요. 나가도 계속 쉴게요","おやすみ。画面を閉じても休むよ","Good night. I'll rest while you're away.") else say("잘 잤어요! 같이 놀아요","よく寝たよ！いっしょに遊ぼう","I'm awake! Let's play.")}
        }
        audio.effect(if(pet.level>before.level)"level" else when(index){0->"feed";2->"wash";else->if(pet.sleeping)"sleep" else "chirp"})
        room.react(if(pet.level>before.level)"level" else effect.ifEmpty { if(pet.sleeping)"sleep" else "chirp" })
        if(pet.level>before.level)say("레벨 업! 뮤즈가 성장했어요","レベルアップ！Museが成長したよ","Level up! Muse has grown.")
        render()
    }
    private fun choices(title:String,labels:List<String>,pick:(Int)->Unit) {
        android.app.AlertDialog.Builder(context).setTitle(title).setItems(labels.toTypedArray()){_,i->pick(i)}
            .setNegativeButton(t("닫기","閉じる","Close"),null).show()
    }
    private fun chooseFood() {
        val names=listOf(t("따뜻한 우유","あたたかいミルク","Warm milk"),t("포근한 죽","おかゆ","Porridge"),t("숲속 베리","森のベリー","Forest berries"),t("구운 생선","焼き魚","Grilled fish"),t("채소 한 접시","野菜プレート","Vegetable plate"),t("작은 케이크 · 가끔만","小さなケーキ · たまにね","Small cake · an occasional treat"))
        choices(t("오늘은 무엇을 먹을까요?","今日は何を食べよう？","What's on the menu?"),names){i->
            val now=System.currentTimeMillis();val before=pet.advance(now);pet=before.feed(PetFood.entries[i],now);save()
            if(pet.fedAt!=before.fedAt){record("food");room.react("food");audio.effect("feed");say("냠냠… 함께 먹으니 좋아요","もぐもぐ…いっしょでうれしい","Yum… it's nice to eat together.")}
            else say("아직 배불러요. 조금 있다 먹어요","まだおなかいっぱい。またあとで","Still full. Let's eat a little later.")
            render()
        }
    }
    private fun chooseCare() {
        choices(t("돌봐 주기","お世話","Care"),listOf(t("따뜻한 목욕","あたたかいお風呂","Warm bath"),t("아플 때 약 주기","具合が悪い時のお薬","Medicine when ill"))){i->
            val now=System.currentTimeMillis();val before=pet.advance(now)
            pet=if(i==0)before.care(PetAction.CLEAN,now)else before.medicine(now);save()
            if(pet!=before){record("care");room.react("wash");audio.effect("wash");say("고마워요. 조금 편안해졌어요","ありがとう。少し楽になったよ","Thank you. That feels better.")}
            else say("지금은 괜찮아요. 곁에 있어 주세요","今は大丈夫。そばにいてね","I'm okay for now. Stay with me.")
            render()
        }
    }
    private fun chooseGame() {
        choices(t("어떻게 놀까요?","何して遊ぶ？","Let's play"),listOf(t("별잡기","星あつめ","Star catch"),t("반짝임 기억하기","光を覚えよう","Memory lights"),t("새와 리듬 맞추기","小鳥とリズム","Bird rhythm"))){startGame(PetGame.entries[it])}
    }
    private fun startGame(kind:PetGame) {
        pet=pet.advance(System.currentTimeMillis())
        if(pet.dead || pet.sleeping || pet.energy<10)return
        game=kind;catches=0;missed=0;star=(0..2).random();selectedTile=1;melodyIndex=0
        melody=List(3){(0..2).random()};gameStarted=SystemClock.elapsedRealtime()
        gameEnds=gameStarted+30_000;nextStar=gameStarted+1500
        playRow.visibility=View.VISIBLE
        hint.text=when(game){PetGame.STARS->t("별이 있는 칸을 눌러 주세요","星のマスをタップ","Tap the star tile");PetGame.MEMORY->t("반짝인 순서대로 따라 눌러요","光った順にタップ","Repeat the glowing sequence");PetGame.RHYTHM->t("가운데 새가 빛날 때 눌러요","真ん中の鳥が光ったらタップ","Tap the middle bird when it glows")}
        render();updateGame()
    }
    private fun catchStar() {
        if(!inGame)return
        val now=SystemClock.elapsedRealtime()
        if(now>=gameEnds){finishGame(false);return}
        val correct=when(game){
            PetGame.STARS->selectedTile==star
            PetGame.MEMORY->{if(now-gameStarted<3600)return;selectedTile==melody[melodyIndex]}
            PetGame.RHYTHM->selectedTile==1 && (now-gameStarted)%1600 in 650..1100 && now>=nextStar
        }
        if(correct){
            catches++;audio.effect("chirp");room.react("chirp");melodyIndex++
            if(catches>=if(game==PetGame.MEMORY)3 else 5){finishGame(true);return}
            if(game==PetGame.STARS){star=(star+1+(0..1).random())%3;nextStar=now+1500}
            if(game==PetGame.RHYTHM)nextStar=now+650
        }else{missed++;if(game==PetGame.MEMORY || missed>=5){finishGame(false);return}}
        updateGame()
    }
    private fun updateGame() {
        val now=SystemClock.elapsedRealtime();val age=now-gameStarted
        if(now>=gameEnds){finishGame(false);return}
        if(game==PetGame.STARS && now>=nextStar){star=(star+1+(0..1).random())%3;nextStar=now+1500}
        val demo=game==PetGame.MEMORY && age<3600
        val active=when(game){PetGame.STARS->star;PetGame.MEMORY->if(demo && age%1200<800)melody[(age/1200).toInt().coerceIn(0,2)]else -1;PetGame.RHYTHM->if(age%1600 in 650..1100)1 else -1}
        notice.showText(when {demo->t("잘 보고 기억해 주세요…","よく見て覚えてね…","Watch and remember…");game==PetGame.MEMORY->t("이제 같은 순서로 눌러요","同じ順にタップしてね","Now repeat the sequence");game==PetGame.RHYTHM->t("새와 함께 박자를 맞춰요 ♪","小鳥とリズムを合わせよう ♪","Keep the beat with the bird ♪");else->t("반짝이는 별을 찾아요","きらきらの星を探そう","Find the sparkling star")})
        tiles.forEachIndexed{i,b->
            b.isEnabled=!demo
            b.showText(if(i==active)if(game==PetGame.RHYTHM)"♪" else "★" else if(game==PetGame.MEMORY)listOf("●","▲","■")[i]else "·")
            val key=(i==selectedTile) to (i==active)
            if(b.tag!=key){b.tag=key;b.setTextColor(if(i==selectedTile)Color.WHITE else ink);b.background=panel(if(i==selectedTile)green else if(i==active)0xffedcd83.toInt()else 0xffe6e3cf.toInt(),12f)}
            b.contentDescription=t("놀이 칸","ゲームのマス","Game tile")+" ${i+1}"+if(i==active)t(" 반짝임"," 光る"," glowing")else ""
        }
    }
    private fun finishGame(won:Boolean,completed:Boolean=true) {
        gameEnds=0;playRow.visibility=View.GONE
        if(completed){record("play");pet=pet.play(game,won,System.currentTimeMillis());save();audio.effect(if(won)"win" else "chirp");room.react("heart")}
        if(won)say("해냈어요! 같이 놀아서 즐거워요","できた！いっしょで楽しいね","We did it! That was fun together.")
        else say("함께 연습해요. 이기지 않아도 괜찮아요","いっしょに練習しよう。勝てなくても大丈夫","Let's practise together. It's okay not to win.")
        hint.text=t("길게 눌러 말하기 · 측면 버튼 + 휠: 음량","長押しで話す · サイドボタン＋ホイール：音量","Hold to talk · Side button + wheel: volume")
        render()
    }
    private val tick=object:Runnable { override fun run() {
        val elapsed=SystemClock.elapsedRealtime()
        val visible=hasWindowFocus() && agentPane.visibility!=View.VISIBLE && context.getSystemService(android.os.PowerManager::class.java).isInteractive
        pet=pet.advance(System.currentTimeMillis())
        // Offline wall time advances in PetState, never from frame count.
        previousTick=elapsed
        if(!visible)room.hatchStarted=0L
        if(visible && pet.hatchReady) {
            if(room.hatchStarted==0L) { room.hatchStarted=elapsed;messageUntil=0L }
            if(!ValueAnimator.areAnimatorsEnabled() || elapsed-room.hatchStarted>=2600L) {
                pet=pet.hatch(System.currentTimeMillis());record("hatch");room.hatchStarted=0L;room.react("hatch");audio.effect("level");save()
                say("안녕! 아기 뮤즈가 태어났어요","こんにちは！ベビーMuseが生まれたよ","Hello! Baby Muse has hatched.")
            }
        }
        audio.update(pet.level,pet.sleeping,pet.dead || !hasWindowFocus() || host.petAudioBlocked)
        if(inGame)updateGame()
        if(SystemClock.elapsedRealtime()-previousSave>if(pet.hatched)60_000 else 5_000)save()
        render();handler.postDelayed(this,250)
    } }
    override fun onStart() {
        super.onStart();foreground=this;pet=store.load(System.currentTimeMillis());previousTick=0L;handler.post(tick)
        roomWindow.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT)
        roomWindow.decorView.post { if(isShowing)roomWindow.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT) }
        activitiesStore.welcome(pet.seed);refreshActivities();host.petRefresh()
    }
    override fun onStop() {
        if(foreground===this)foreground=null
        activities.close()
        cancelAgentInput();handler.removeCallbacksAndMessages(null)
        if(inGame)finishGame(false,false)
        pet=pet.advance(System.currentTimeMillis());save();audio.close();super.onStop()
    }
    override fun onWindowFocusChanged(hasFocus:Boolean) { super.onWindowFocusChanged(hasFocus);if(hasFocus)refreshReplyMute();if(!hasFocus){cancelAgentInput();muteAudio();if(::room.isInitialized)room.hatchStarted=0L;previousTick=0L} }
    @Deprecated("Legacy Android back handling")
    override fun onBackPressed() { if(inGame)finishGame(false,false) else if(agentPane.visibility==View.VISIBLE)agentPane.visibility=View.GONE else super.onBackPressed() }
    private fun move(delta:Int) {
        if(voiceOwned || held)return
        if(agentPane.visibility==View.VISIBLE){agentScroll.smoothScrollBy(0,dp(60)*delta);return}
        if(inGame) { selectedTile=(selectedTile+delta+3)%3;updateGame() }
        else { selected=if(pet.hatched && pet.sleeping)3 else (selected+delta+4)%4;render() }
    }
    private fun wheel(delta:Int) {
        if(sideGesture.canAdjustVolume && hasWindowFocus()) {
            handler.removeCallbacks(voiceHold)
            sideGesture.wheel()
            endVoice(false) // Discard any pending recording; release must not send or activate care.
            host.adjustMediaVolume(-delta)
        } else move(delta)
    }
    internal fun petKey(event:KeyEvent):Boolean {
        when(event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,KeyEvent.KEYCODE_DPAD_DOWN -> {
                if(event.action==KeyEvent.ACTION_DOWN)wheel(if(event.keyCode==KeyEvent.KEYCODE_DPAD_UP)-1 else 1)
                return true
            }
            KeyEvent.KEYCODE_PAIRING -> {
                if(event.action==KeyEvent.ACTION_DOWN && event.repeatCount==0 && hasWindowFocus()) {
                    if(sideGesture.downTime==event.downTime)return true
                    handler.removeCallbacks(voiceHold)
                    endVoice(false)
                    sideGesture.down(event.downTime,true)
                    if(!inGame)handler.postDelayed(voiceHold,SideButtonGesture.HOLD_MS)
                }
                if(event.action==KeyEvent.ACTION_UP && sideGesture.downTime==event.downTime) {
                    handler.removeCallbacks(voiceHold)
                    when(sideGesture.up(event.downTime,event.eventTime,event.isCanceled)) {
                        SideButtonGesture.Action.FINISH -> endVoice(true)
                        SideButtonGesture.Action.CANCEL -> endVoice(false)
                        SideButtonGesture.Action.LOCK -> if(inGame)catchStar() else if(agentPane.visibility!=View.VISIBLE)activate(selected)
                        else -> Unit
                    }
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER,KeyEvent.KEYCODE_ENTER -> {
                if(sideGesture.downTime!=null || held)return true
                if(event.action==KeyEvent.ACTION_DOWN && event.repeatCount==0)press=event.downTime
                if(event.action==KeyEvent.ACTION_UP && press==event.downTime) {
                    press=-1
                    if(!event.isCanceled) { if(inGame)catchStar() else if(agentPane.visibility!=View.VISIBLE)activate(selected) }
                }
                return true
            }
        }
        return false
    }
    override fun dispatchKeyEvent(event:KeyEvent)=petKey(event)||super.dispatchKeyEvent(event)
    override fun dispatchGenericMotionEvent(event:MotionEvent):Boolean {
        if(event.action==MotionEvent.ACTION_SCROLL) {
            val delta=event.getAxisValue(MotionEvent.AXIS_SCROLL).takeIf { it!=0f } ?: event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            if(delta!=0f){wheel(if(delta>0)-1 else 1);return true}
        }
        return super.dispatchGenericMotionEvent(event)
    }
    private fun confirmNewEgg() {
        if(inGame || host.petAudioBlocked)return
        android.app.AlertDialog.Builder(context).setTitle(t("알부터 다시 시작", "たまごからやり直す", "Start from an egg"))
            .setMessage(t("현재 육성 기록은 보관하고 새 알을 맞이합니다. 대화와 음성 설정은 유지됩니다.","今の育成記録を保管して、新しいたまごを迎えます。会話と音声設定は変わりません。","Archive this pet and welcome a new egg. Conversation and voice settings stay unchanged."))
            .setNegativeButton(t("취소","キャンセル","Cancel"),null)
            .setPositiveButton(t("새 알 맞이하기","たまごを迎える","Welcome an egg")){_,_->
                pet=store.startNewEgg(System.currentTimeMillis());room.hatchStarted=0;previousTick=0;selected=0;messageUntil=0;effect=""
                activitiesStore.welcome(pet.seed);PetPromises.restore(context);refreshActivities();agentPane.visibility=View.GONE;render()
            }.show()
    }
    companion object { internal var foreground:PetRoom?=null;private set }
}

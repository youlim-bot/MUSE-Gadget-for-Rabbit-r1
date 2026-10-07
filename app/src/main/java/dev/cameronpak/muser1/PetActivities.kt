package dev.cameronpak.muser1

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.*
import android.widget.*
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.UUID

/** Compact, scrolling pages sized for the R1. Each activity stays inside the Muse app. */
internal class PetActivities(private val host:MainActivity,private val state:()->PetState,private val changed:()->Unit) {
    private val store=PetActivitiesStore(host)
    private var dialog:Dialog?=null
    private var picker:Dialog?=null
    private val ink=0xff303831.toInt()
    private val green=0xff4d6e4c.toInt()
    private fun dp(n:Int)=(n*host.resources.displayMetrics.density).toInt()
    private fun t(k:String,j:String,e:String)=UiText.text(host,k,j,e)
    fun close(){picker?.dismiss();picker=null;dialog?.dismiss();dialog=null}
    private fun page(title:String,build:(LinearLayout)->Unit) {
        dialog?.dismiss();host.petStopSpeech()
        val d=Dialog(host,R.style.Theme_Muse);dialog=d
        val root=LinearLayout(host).apply { orientation=LinearLayout.VERTICAL;setPadding(dp(12),dp(8),dp(12),dp(8));setBackgroundColor(0xfff6f1df.toInt()) }
        val header=LinearLayout(host).apply { gravity=Gravity.CENTER_VERTICAL }
        header.addView(TextView(host).apply { text=title;textSize=18f;setTextColor(ink);typeface=Typeface.DEFAULT_BOLD;maxLines=2 },LinearLayout.LayoutParams(0,dp(56),1f))
        header.addView(Button(host).apply { text=t("닫기","閉じる","Close");isAllCaps=false;textSize=12f;setTextColor(ink);background=GradientDrawable().apply {setColor(0xffe6e3cf.toInt());cornerRadius=dp(12).toFloat()};setOnClickListener { close() } },LinearLayout.LayoutParams(dp(68),dp(48)))
        root.addView(header)
        val content=LinearLayout(host).apply { orientation=LinearLayout.VERTICAL;setPadding(0,dp(6),0,dp(16)) }
        root.addView(ScrollView(host).apply { isFillViewport=true;addView(content) },LinearLayout.LayoutParams(-1,0,1f))
        build(content)
        if(title!=t("특별 활동","特別な活動","Special activities"))button(root,t("‹ 특별 활동","‹ 特別な活動","‹ Special activities")){show()}
        d.setContentView(root);d.show();d.window?.apply { setLayout(-1,-1);decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY }
    }
    private fun text(parent:LinearLayout,value:String,size:Float=14f) {
        parent.addView(TextView(host).apply { text=value;textSize=size;setTextColor(ink);setPadding(dp(8),dp(8),dp(8),dp(12));setLineSpacing(dp(3).toFloat(),1f) },LinearLayout.LayoutParams(-1,-2))
    }
    private fun button(parent:LinearLayout,label:String,enabled:Boolean=true,action:()->Unit){
        parent.addView(Button(host).apply { text=label;isAllCaps=false;textSize=14f;setTextColor(Color.WHITE);minHeight=dp(52);setPadding(dp(12),dp(8),dp(12),dp(8));isEnabled=enabled;alpha=if(enabled)1f else .45f
            background=GradientDrawable().apply { setColor(green);cornerRadius=dp(12).toFloat() };setOnClickListener { action() }
        },LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(8) })
    }
    private fun ask(question:String){close();host.petType(question)}
    fun show(){
        store.welcome(state().seed)
        page(t("특별 활동","特別な活動","Special activities")){v->
            text(v,t("함께 쌓아가는 우리만의 이야기","いっしょに紡ぐ、ふたりの物語","A little world we build together"))
            button(v,t("탐험 보내기","探検に送り出す","Solo exploration")){explore()}
            button(v,t("뮤즈의 일기","Museの日記","Muse's diary")){diary()}
            button(v,t("추억 상자","思い出の箱","Memory chest")){memories()}
            button(v,t("방 꾸미기","お部屋の飾りつけ","Decorate room")){decorate()}
            button(v,t("현실 미션","現実のミッション","Real-world missions")){mission()}
            val pending=store.promises().any { it.seed==state().seed && it.pending }
            button(v,t("우리만의 약속","ふたりの約束","Our promises")+if(pending)" •" else ""){promises()}
        }
    }
    internal fun explore(){
        val p=state();val trip=store.trip(p.seed)
        page(t("탐험 보내기","探検に送り出す","Solo exploration")){v->
            if(trip!=null){
                text(v,PetActivityText.place(host,trip.place),18f)
                val ready=PetAdventure.ready(trip,System.currentTimeMillis())
                text(v,if(ready)t("탐험을 마쳤어요. 돌아온 이야기를 만나 보세요.","探検が終わったよ。お話を聞いてね。","The trip is complete. Discover the story.") else t("혼자 탐험 중이에요. 앱을 닫아도 탐험은 계속돼요. 돌봄은 돌아온 뒤에 할 수 있어요.","ひとりで探検中。アプリを閉じても続きます。お世話は帰ってから。","Exploring independently. The trip continues while the app is closed. Care resumes after returning."))
                if(ready)button(v,t("맞이하기","おかえり！","Welcome home")){
                    store.claim(state());changed()
                    val event=store.events(p.seed).lastOrNull { it.kind=="trip" }
                    page(t("탐험 이야기","探検のお話","Expedition story")){body->
                        val story=event?.let { PetActivityText.event(host,it) }.orEmpty();text(body,story)
                        button(body,t("Muse와 이야기하기","Museと話す","Talk with Muse")){ask(story+"\n"+t("이 게임 속 탐험에서 가장 기억에 남는 순간을 이야기해 줘.","このゲームの探検で心に残った瞬間を話して。","Tell me about a memorable moment in this fictional expedition."))}
                        button(body,t("기념품으로 방 꾸미기","おみやげを飾る","Decorate with souvenirs")){decorate()}
                    }
                } else button(v,t("상태 확인","様子を見る","Check return status")){explore()}
                button(v,t("일찍 데려오기 · 보상 없음","早めに呼び戻す · 報酬なし","Recall early · No reward")) {store.recall(p.seed);changed();explore()}
            }else{
                text(v,t("숲 30분 · 해변 1시간 · 언덕 2시간\n성격에 따라 발견과 이야기가 달라져요. 탐험 중에도 배고픔과 시간은 흐르니 먼저 돌봐 주세요.","森30分・浜辺1時間・丘2時間\n性格で発見や物語が変わります。探検中もおなかは空くので、出発前にお世話してね。","Forest 30 min · Beach 1 h · Hill 2 h\nPersonality shapes discoveries and stories. Needs continue during a trip; care for your companion before departure."))
                val ready=PetAdventure.canLeave(p)
                if(!ready)text(v,t("알에서 태어난 뒤, 깨어 있고 건강하며 배와 체력이 충분할 때 떠날 수 있어요.","生まれてから、起きていて元気で、おなかと体力に余裕がある時に出発できます。","Available after hatching, when awake, healthy, well fed and rested."))
                (0..2).forEach { i->button(v,PetActivityText.place(host,i),ready){if(store.begin(state(),i)){changed();explore()}else explore()} }
            }
        }
    }
    internal fun diary(){
        val seed=state().seed
        val groups=store.events(seed).groupBy { Instant.ofEpochMilli(it.at).atZone(ZoneId.systemDefault()).toLocalDate() }.toSortedMap(compareByDescending { it })
        page(t("뮤즈의 일기","Museの日記","Muse's diary")){v->
            text(v,t("실제 함께한 활동을 바탕으로 R1에 기록해요.","実際にいっしょにした活動をR1に記録します。","Recorded locally from activities we actually shared."))
            groups.entries.take(30).forEach { (day,events)->
                val entry=events.map { PetActivityText.event(host,it) }.filter { it.isNotBlank() }.distinct().joinToString("\n")
                button(v,day.toString()) {page(day.toString()){body->text(body,entry);button(body,t("이날 이야기 나누기","この日の話をする","Talk about this day")){ask(entry+"\n"+t("이날을 함께 돌아보자. 나에게 질문 하나 해 줘.","この日を振り返ろう。ひとつ質問して。","Let's reflect on this day. Ask me one question."))}}}
            }
        }
    }
    internal fun memories(){
        val seed=state().seed;val memories=store.memories(seed)
        page(t("추억 상자","思い出の箱","Memory chest")){v->
            button(v,t("최근 대화 보관하기","最近の会話を残す","Save latest conversation")){
                val conversation=host.petMemoryText()
                if(conversation.isNullOrBlank()){Toast.makeText(host,t("먼저 Muse와 대화해 주세요.","まずMuseとお話ししてね。","Have a conversation with Muse first."),Toast.LENGTH_SHORT).show()}
                else page(t("추억 보관 확인","保存する内容を確認","Review memory")){body->
                    text(body,conversation);text(body,t("이 내용은 R1에만 보관돼요. 다시 이야기할 때는 Muse로 전송돼요.","R1だけに保存されます。話題にするとMuseへ送信されます。","Stored only on this R1. Discussing it sends the selected memory to Muse."))
                    button(body,t("추억으로 저장","思い出に保存","Save memory")){store.event(seed,"memory",conversation);memories()}
                }
            }
            memories.take(80).forEach { e->
                val story=PetActivityText.event(host,e)
                val date=Instant.ofEpochMilli(e.at).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MM/dd"))
                button(v,"$date · ${story.take(42)}") {page(t("소중한 기억","大切な思い出","A treasured memory")){body->text(body,story);button(body,t("이 기억에 대해 말하기","この思い出を話す","Recall with Muse")){ask(t("우리의 기록이야:\n","ふたりの記録だよ：\n","Here is our saved memory:\n")+story+"\n"+t("이 기억에 대해 이야기해 줘.","この思い出について話して。","Talk with me about this memory."))}}}
            }
        }
    }
    internal fun decorate(){
        val p=state();val items=store.items(p.seed)
        page(t("방 꾸미기","お部屋の飾りつけ","Decorate room")){v->
            text(v,t("탐험과 현실 미션에서 받은 장식을 방에 놓아 보세요.","探検と現実のミッションで集めた飾りを置いてみよう。","Display decorations earned from expeditions and real-world missions."))
            val scene=PetScene(host).apply { pet=p;decoration=store.decoration(p.seed) }
            v.addView(scene,LinearLayout.LayoutParams(-1,dp(160)))
            (listOf(-1)+items).forEach { item->button(v,(if(store.decoration(p.seed)==item)"✓ " else "")+PetActivityText.item(host,item)){
                store.decorate(p.seed,item);changed();decorate()
                Toast.makeText(host,t("방이 새로워졌어!","お部屋が新しくなったね！","Our room feels new!"),Toast.LENGTH_SHORT).show()
            } }
            if(items.isNotEmpty())button(v,t("새 방은 어때?","新しいお部屋はどう？","What do you think of our room?")){ask(t("방에 ${PetActivityText.item(host,store.decoration(p.seed))} 장식을 놓았어. 어때?","お部屋に${PetActivityText.item(host,store.decoration(p.seed))}を飾ったよ。どう？","Our room now has ${PetActivityText.item(host,store.decoration(p.seed))}. What do you think?"))}
        }
    }
    internal fun mission(){
        val p=state();val target=store.mission(p.seed)
        page(t("현실 미션","現実のミッション","Real-world missions")){v->
            text(v,t("주변에서 찾아 사진으로 보여 주세요. 보내기 전 사진과 질문을 확인하며, Muse 연결이 필요해요. 보상은 하루 한 번이에요.","身近なものを写真で見せてね。送信前に写真と質問を確認します。Muse接続が必要です。報酬は一日一回です。","Find something nearby and show a photo. Review the photo and question before sending. Requires Muse connectivity. One reward per day."))
            if(store.missionDoneToday(p.seed)){text(v,t("오늘의 미션을 마쳤어요. 내일 또 만나요!","今日のミッションは達成。また明日！","Today's mission is complete. Come back tomorrow!"));return@page}
            if(p.dead){text(v,t("새 친구와 시작해 주세요.","新しい友だちと始めてね。","Start with a new companion."));return@page}
            if(target>=0){
                text(v,PetActivityText.target(host,target),18f)
                button(v,t("카메라로 찾기","カメラで探す","Find with camera")){
                    val token=store.missionToken(p.seed)?:return@button
                    close();host.petMissionCamera(p.seed,token,target)
                }
            }
            (0..2).forEach { i->button(v,PetActivityText.target(host,i)){store.chooseMission(p.seed,i);mission()} }
        }
    }
    internal fun promises(){
        val p=state();val all=store.promises().filter { it.seed==p.seed }
        page(t("우리만의 약속","ふたりの約束","Our promises")){v->
            text(v,t("매일 정한 시각에 R1 알림으로 알려요. 절전 상태에서는 늦어질 수 있어요. 대화 시작은 직접 선택해 주세요.","毎日決めた時刻にR1へ通知します。省電力中は遅れる場合があります。会話は自分で開始してね。","A daily reminder appears on this R1. Battery saving may delay it. Choose when to start the conversation."))
            all.forEach { promise->
                val label=String.format(java.util.Locale.ROOT,"%02d:%02d",promise.hour,promise.minute)+" · "+PetActivityText.promise(host,promise.kind)+(if(promise.pending)" •" else "")
                button(v,label){promiseDetail(promise.id)}
            }
            button(v,t("새 약속","新しい約束","New promise"),all.size<6 && !p.dead){
                page(t("함께할 활동","いっしょにすること","What shall we do?")){body->(0..3).forEach { kind->button(body,PetActivityText.promise(host,kind)){
                    page(t("매일 알릴 시간","毎日お知らせする時刻","Daily reminder time")){timePage->
                        text(timePage,PetActivityText.promise(host,kind))
                        val row=LinearLayout(host).apply { gravity=Gravity.CENTER }
                        val hour=NumberPicker(host).apply { minValue=0;maxValue=23;value=19;setTextColor(ink);setTextSize(dp(20).toFloat());descendantFocusability=NumberPicker.FOCUS_BLOCK_DESCENDANTS;contentDescription=t("시","時","Hour") }
                        val minute=NumberPicker(host).apply { minValue=0;maxValue=59;value=0;setTextColor(ink);setTextSize(dp(20).toFloat());setFormatter { String.format(java.util.Locale.ROOT,"%02d",it) };descendantFocusability=NumberPicker.FOCUS_BLOCK_DESCENDANTS;contentDescription=t("분","分","Minute") }
                        row.addView(hour,LinearLayout.LayoutParams(dp(100),dp(140)));row.addView(TextView(host).apply { text=":";textSize=22f;setTextColor(ink) });row.addView(minute,LinearLayout.LayoutParams(dp(100),dp(140)));timePage.addView(row)
                        button(timePage,t("매일 이 시간에 알림","毎日この時刻に通知","Remind me daily")){
                            val h=hour.value;val m=minute.value
                            val promise=PetPromise(UUID.randomUUID().toString(),p.seed,kind,h,m,PetAdventure.nextPromise(h,m,ZonedDateTime.now()))
                            runCatching { PetPromises.add(host,promise) }.onFailure { Toast.makeText(host,t("저장하지 못했어요. 다시 시도해 주세요.","保存できませんでした。もう一度お試しください。","Couldn't save. Please try again."),Toast.LENGTH_LONG).show() }
                            if(android.os.Build.VERSION.SDK_INT>=33 && host.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)host.requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),91)
                            changed();promises()
                        }
                    }
                }} }
            }
        }
    }
    fun promiseDetail(id:String){
        val p=store.promises().find { it.id==id && it.seed==state().seed }?:run{promises();return}
        page(PetActivityText.promise(host,p.kind)){v->
            text(v,String.format(java.util.Locale.ROOT,"%02d:%02d",p.hour,p.minute)+" · "+t("매일","毎日","Every day"))
            button(v,t("함께 시작하기","いっしょに始める","Start together")){
                store.putPromise(p.copy(pending=false));store.event(p.seed,"promise",p.kind.toString());host.getSystemService(NotificationManager::class.java).cancel(p.id,81);changed()
                host.petPractice(listOf("ko","ja","en").getOrNull(p.kind))
                val language=listOf("Korean","Japanese","English").getOrNull(p.kind)
                ask(if(language!=null)"Let's practise $language together. Be my friendly beginner conversation partner in $language. Ask one simple question at a time, gently correct my replies, and keep each reply short." else t("오늘 하루를 함께 돌아보자. 내 하루에 대해 질문 하나 해 줘.","今日を一緒に振り返ろう。一日のことをひとつ質問して。","Let's reflect on our day. Ask me one question about today."))
            }
            button(v,t("약속 삭제","約束を削除","Delete promise")){PetPromises.remove(host,p.id);changed();promises()}
        }
    }
}

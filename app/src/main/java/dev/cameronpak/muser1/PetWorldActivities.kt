package dev.cameronpak.muser1

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.KeyEvent
import android.widget.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Local game pages use the same scrollable shell, original avatar and display language. */
internal class PetWorldActivities(private val pages:PetActivities,private val host:MainActivity,
    private val state:()->PetState,private val changed:()->Unit) {
    private val store=PetWorldStore(host)
    private fun t(k:String,j:String,e:String)=UiText.text(host,k,j,e)
    private fun dp(n:Int)=(n*host.resources.displayMetrics.density).toInt()
    private fun say(k:String,j:String,e:String)=Toast.makeText(host,t(k,j,e),Toast.LENGTH_SHORT).show()
    private fun available()=state().let {it.hatched && !it.sleeping && !it.dead && !it.ill && it.energy>=10 && PetActivitiesStore(host).trip(it.seed)==null}
    internal fun garden(){
        val p=state();val plots=store.garden(p.seed)
        pages.page(t("작은 정원","小さな庭","Little garden")){v->
            pages.text(v,t("며칠 동안 함께 돌보면 꽃이 피어요. 물을 너무 자주 줄 필요는 없어요. 오래 잊으면 시들 수 있어요.","何日もいっしょに育てると花が咲くよ。水は頻繁にあげなくて大丈夫。長く忘れると枯れることも。","Care together over several days and flowers will bloom. No need to water constantly. Long neglect may wilt them."))
            v.addView(PetGardenView(host,plots),LinearLayout.LayoutParams(-1,dp(160)))
            plots.forEachIndexed {i,plant->
                val label="${i+1} · "+when {plant==null->t("빈 화분","空の鉢","Empty planter");plant.wilted->t("시들었어요","枯れてしまった","Wilted");plant.blooming->PetWorldText.flower(host,plant.flower);plant.stage>=2->t("무럭무럭 자라는 중","すくすく成長中","Growing happily");else->t("작은 싹을 기다려요","小さな芽を待とう","Waiting for a sprout")}
                pages.button(v,label,!p.dead){
                    when {
                        plant==null->pages.page(label){body->(0..2).forEach {kind->pages.button(body,PetWorldText.seed(host,kind)){store.plant(p.seed,i,kind);PetRoom.foreground?.activitySound("plant");changed();garden()}}}
                        plant.wilted->MuseAlertDialogBuilder(host).setTitle(t("새 씨앗을 심을까요?","新しい種を植える？","Start a new plant?"))
                            .setNegativeButton(t("취소","キャンセル","Cancel"),null).setPositiveButton(t("화분 비우기","鉢を空にする","Clear planter")){_,_->store.clearWilted(p.seed,i);garden()}.show()
                        plant.blooming->{store.collect(p.seed,i);PetRoom.foreground?.activitySound("bloom","praise");changed();garden()}
                        else->{if(store.water(p.seed,i))PetRoom.foreground?.activitySound("water") else say("아직 물이 충분해요","まだお水は足りているよ","The plant still has enough water");changed();garden()}
                    }
                }
            }
            pages.text(v,t("싹을 누르면 물 주기 · 꽃을 누르면 수집","芽をタップして水やり・花をタップして集める","Tap a sprout to water · Tap a flower to collect"),12f)
            val flowers=store.flowers(p.seed)
            if(flowers.isNotEmpty())pages.text(v,flowers.joinToString(" · "){PetWorldText.flower(host,it)})
            pages.button(v,t("정원 이야기 나누기","お庭のお話","Talk about our garden")){pages.ask(t("우리 정원의 실제 상태야: ","今のお庭の状態だよ：","Here are our garden's actual facts: ")+store.context(p.seed)+t("\n함께 돌보는 마음에 대해 짧게 이야기해 줘.","\nいっしょに育てる気持ちを短く話して。","\nBriefly talk about caring for it together."))}
        }
    }
    internal fun games(){
        pages.page(t("휠 미니게임","ホイールゲーム","Wheel minigames")){v->
            pages.text(v,t("휠로 이동 · 측면 버튼으로 선택\n측면 버튼을 누른 채 휠: 음량 조절","ホイールで移動・サイドボタンで決定\nボタンを押しながらホイール：音量","Wheel to move · Side button to select\nHold side button + wheel: volume"))
            if(!available())pages.text(v,t("태어난 뒤, 깨어 있고 건강할 때 같이 놀아요.","生まれてから、起きていて元気な時に遊ぼう。","Let's play after hatching, when awake and healthy."))
            pages.button(v,t("연못 낚시","池で釣り","Pond fishing"),available()){fishing()}
            listOf(PetGame.STARS,PetGame.MEMORY,PetGame.RHYTHM).forEach {kind->
                val label=when(kind){PetGame.STARS->t("별잡기","星あつめ","Star catch");PetGame.MEMORY->t("반짝임 기억하기","光を覚えよう","Memory lights");PetGame.RHYTHM->t("새와 리듬 맞추기","小鳥とリズム","Bird rhythm")}
                pages.button(v,label,available()){pages.close();PetRoom.foreground?.startWheelGame(kind)}
            }
        }
    }
    private fun fishing(){
        if(!available())return
        val handler=Handler(Looper.getMainLooper());val game=WheelFishing(SystemClock.elapsedRealtime());var active=true
        lateinit var status:TextView;lateinit var pond:PetPondView
        fun finish(){if(!active)return;active=false;pages.pageKeys=null;handler.removeCallbacksAndMessages(null)
            PetRoom.foreground?.completeWheelGame(game.catches>=5)
            status.text=if(game.catches>=5)t("해냈어요! 연못 친구들과 인사했어요","できた！池の友だちにあいさつしたよ","We did it! We met the pond friends")else t("즐거운 연습이었어요. 다시 놀아요!","楽しい練習だったね。また遊ぼう！","That was fun practice. Let's play again!")
        }
        fun cast(){if(!active)return;val catches=game.catches;game.catch(SystemClock.elapsedRealtime());if(game.catches>catches)PetRoom.foreground?.activitySound("catch");if(game.finished)finish()}
        val tick=object:Runnable {override fun run(){
            if(!active)return
            if(!pages.focused || !host.getSystemService(android.os.PowerManager::class.java).isInteractive){active=false;pages.pageKeys=null;status.text=t("놀이가 멈췄어요. 다시 시작해 주세요.","遊びを中断したよ。もう一度始めてね。","Game paused. Start a new round.");return}
            val now=SystemClock.elapsedRealtime();pond.frame=now;pond.invalidate()
            if(game.expired(now) || game.finished){finish();return}
            status.text=t("만난 물고기 ${game.catches} · 남은 시간 ${(30_000-(now-game.started))/1000}초","会ったお魚 ${game.catches}・あと ${(30_000-(now-game.started))/1000}秒","Fish met ${game.catches} · ${(30_000-(now-game.started))/1000}s left")
            handler.postDelayed(this,120)
        }}
        pages.page(t("연못 낚시","池で釣り","Pond fishing"),onClose={active=false;handler.removeCallbacksAndMessages(null)}){v->
            pages.text(v,t("물고기 위로 이동한 뒤, 반짝일 때 버튼을 눌러요.","魚の上へ移動して、光った時にボタンを押してね。","Move above a fish and press when it glows."),13f)
            pond=PetPondView(host,game);v.addView(pond,LinearLayout.LayoutParams(-1,dp(150)))
            status=TextView(host).apply {textSize=14f;setTextColor(0xff303831.toInt());setPadding(dp(8),dp(8),0,dp(8))};v.addView(status)
            val row=LinearLayout(host)
            listOf("‹",t("낚시","釣る","Cast"),"›").forEachIndexed {i,label->row.addView(Button(host).apply {text=label;isAllCaps=false;setOnClickListener {if(active)when(i){0->game.move(-1);1->cast();else->game.move(1)}}},LinearLayout.LayoutParams(0,dp(52),1f))};v.addView(row)
            pages.button(v,t("다시 하기","もう一度","Play again")){fishing()}
        }
        pages.pageKeys={event->when(event.keyCode){
            KeyEvent.KEYCODE_DPAD_UP,KeyEvent.KEYCODE_DPAD_DOWN->{if(event.action==KeyEvent.ACTION_DOWN && active)game.move(if(event.keyCode==KeyEvent.KEYCODE_DPAD_UP)-1 else 1);true}
            KeyEvent.KEYCODE_DPAD_CENTER,KeyEvent.KEYCODE_ENTER->{if(event.action==KeyEvent.ACTION_UP && !event.isCanceled)cast();true}
            else->false
        }}
        handler.postDelayed(tick,160)
    }
    internal fun training(){
        val p=state();val skills=store.skills(p.seed)
        pages.page(t("기술 가르치기","技を教える","Teach a trick")){v->
            pages.text(v,t("여러 날 함께 연습하면 익숙해져요. 배운 뒤에는 말이나 글로 불러 주세요. 배우는 속도는 비밀이에요.","何日もいっしょに練習すると覚えるよ。覚えたら声や文字で呼んでね。覚える速さはひみつ。","Practise together over several days. Once learned, ask by voice or text. Learning takes its own time."))
            skills.forEach {s->pages.button(v,PetWorldText.skill(host,s.id)+if(s.learned)t(" · 익숙해요"," · 覚えたよ"," · Learned")else t(" · 배우는 중"," · 練習中"," · Learning")){skill(s.id)}}
        }
    }
    private fun skill(id:Int){
        val p=state();val s=store.skills(p.seed)[id];val scene=PetScene(host).apply {pet=p}
        pages.page(PetWorldText.skill(host,id)){v->
            v.addView(scene,LinearLayout.LayoutParams(-1,dp(160)))
            pages.text(v,if(s.learned)t("배웠어요! 이 말을 들으면 동작해요:","覚えたよ！この言葉で動くよ：","Learned! Ask with these words:")+"\n"+PetSkills.triggers(s).joinToString(" · ")else t("조금씩 함께 연습해요. 서두르지 않아도 괜찮아요.","少しずつ練習しよう。急がなくていいよ。","Let's practise little by little. No need to rush."))
            pages.button(v,t("함께 연습하기","いっしょに練習","Practise together"),available()){
                val before=store.skills(p.seed)[id];val learned=store.practice(state(),id)?:return@button
                if(learned!=before)PetRoom.foreground?.activitySound("learn",if(learned.learned)"praise" else null)
                scene.react(if(learned.learned)PetSkills.actions[id].motion else "nod")
                if(learned==before)say("오늘은 연습했어요. 내일 또 해요!","今日は練習したよ。また明日！","We practised today. Let's try tomorrow!")else say("함께 연습한 걸 기억할게요","いっしょの練習を覚えておくね","I'll remember our practice")
                changed();if(!before.learned && learned.learned)skill(id)
            }
            pages.button(v,t("배운 기술 보여 주기","覚えた技を見せる","Show learned trick"),s.learned && available()){scene.react(PetSkills.actions[id].motion);PetRoom.foreground?.answerExpression(PetSkills.actions[id])}
            pages.button(v,t("부르는 말 설정","呼ぶ言葉を設定","Set a cue")){
                val input=EditText(host).apply {setText(s.phrase);hint=PetSkills.triggers(s).first();filters=arrayOf(android.text.InputFilter.LengthFilter(60))}
                MuseAlertDialogBuilder(host).setTitle(t("짧은 말을 정해 주세요","短い言葉を決めてね","Choose a short phrase"))
                    .setMessage(t("비워 두면 한·일·영 기본 말을 사용해요.","空欄なら韓・日・英の標準の言葉を使うよ。","Leave blank for default Korean, Japanese and English cues."))
                    .setView(input).setNegativeButton(t("취소","キャンセル","Cancel"),null).setPositiveButton(t("저장","保存","Save")){_,_->
                        if(store.setPhrase(p.seed,id,input.text.toString()))skill(id)else say("다른 기술과 겹치지 않는 말을 골라 주세요","他の技と違う言葉を選んでね","Choose a cue different from other tricks")
                    }.show()
            }
            if(s.learned)pages.button(v,t("Muse에게 말로 부탁하기","Museに話しかける","Ask Muse")){pages.ask(PetSkills.triggers(s).first())}
        }
    }
    internal fun story(){
        val p=state();val history=store.story(p.seed);val ready=PetStory.canChoose(history,System.currentTimeMillis())
        pages.page(t("선택형 이야기","選んで進む物語","Choose our story")){v->
            if(history.size==PetStory.chapters)pages.text(v,PetWorldText.ending(host,PetStory.route(history)),17f)
            else if(ready){
                pages.text(v,PetWorldText.chapter(host,history.size,PetStory.route(history)),16f)
                (0..1).forEach {option->pages.button(v,PetWorldText.choice(host,history.size,option),!p.dead){if(store.choose(p.seed,option)){changed();story()}}}
            }else {
                history.lastOrNull()?.let {pages.text(v,PetWorldText.outcome(host,it),16f)}
                pages.text(v,t("오늘의 선택을 기억할게요. 다음 이야기는 내일 만나요.","今日の選択を覚えておくね。続きはまた明日。","I'll remember today's choice. The next chapter awaits tomorrow."))
            }
            if(history.isNotEmpty())pages.button(v,t("우리 이야기 되돌아보기","ふたりの物語を振り返る","Read our story")){pages.page(t("우리 이야기","ふたりの物語","Our story")){body->history.forEach {pages.text(body,PetWorldText.outcome(host,it))}}}
            if(history.isNotEmpty())pages.button(v,t("선택에 대해 Muse와 이야기하기","選択をMuseと話す","Discuss our choices with Muse")){pages.ask(t("우리 게임 속 이야기야:\n","ゲームの物語だよ：\n","Here is our fictional game story:\n")+history.joinToString("\n"){PetWorldText.outcome(host,it)}+t("\n우리 선택에 대해 짧게 이야기해 줘.","\nふたりの選択について短く話して。","\nBriefly reflect on our choices."))}
        }
    }
    internal fun discoveries(){
        val p=state();val photos=store.photos(p.seed)
        pages.page(t("사진 발견 도감","写真の発見図鑑","Photo discoveries")){v->
            pages.text(v,t("전송을 확인한 사진과 Muse의 설명을 R1에 보관해요. 최대 24장. 이름과 설명은 Muse의 답변이니 직접 확인해 주세요.","送信を確認した写真とMuseの説明をR1に保存。24枚まで。名前や説明はMuseの回答なので、自分でも確かめてね。","Reviewed photos and Muse's explanations stay on this R1. Up to 24 photos. Check names and descriptions yourself."),13f)
            if(photos.size<24 && !p.dead)(0..2).forEach {kind->pages.button(v,"＋ "+PetWorldText.discovery(host,kind)){pages.close();host.petDiscoveryCamera(p.seed,kind)}}
            photos.reversed().forEach {photo->
                val date=Instant.ofEpochMilli(photo.at).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))
                pages.button(v,"$date · "+PetWorldText.discovery(host,photo.kind)){discovery(photo)}
            }
            if(photos.isEmpty())pages.text(v,t("첫 발견을 사진으로 남겨 보세요","最初の発見を写真に残そう","Capture your first discovery"))
        }
    }
    private fun discovery(photo:PetDiscovery){
        val seed=state().seed
        pages.page(PetWorldText.discovery(host,photo.kind)){v->
            val bitmap=BitmapFactory.decodeFile(store.file(seed,photo.id).absolutePath)
            if(bitmap!=null)v.addView(ImageView(host).apply {setImageBitmap(bitmap);scaleType=ImageView.ScaleType.FIT_CENTER;contentDescription=PetWorldText.discovery(host,photo.kind)},LinearLayout.LayoutParams(-1,dp(170)))
            pages.text(v,photo.reply.ifBlank {t("설명이 아직 도착하지 않았어요. 다시 질문할 수 있어요.","説明はまだ届いていません。もう一度質問できるよ。","The explanation hasn't arrived yet. You can ask again.")})
            pages.button(v,t("이 사진에 이어 질문하기","この写真について聞く","Ask about this photo"),bitmap!=null){pages.close();host.petDiscoveryQuestion(seed,photo.id)}
            pages.button(v,t("도감에서 삭제","図鑑から削除","Delete from album")){
                MuseAlertDialogBuilder(host).setTitle(t("이 사진을 삭제할까요?","この写真を削除する？","Delete this photo?"))
                    .setNegativeButton(t("취소","キャンセル","Cancel"),null).setPositiveButton(t("삭제","削除","Delete")){_,_->store.deletePhoto(seed,photo.id);discoveries()}.show()
            }
        }
    }
}

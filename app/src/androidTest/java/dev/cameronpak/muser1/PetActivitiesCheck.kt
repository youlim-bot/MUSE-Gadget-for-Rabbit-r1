package dev.cameronpak.muser1

import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.io.File

/** Disposable emulator only. Fixtures never contact Muse or use a real camera/account. */
internal object PetActivitiesCheck {
    fun run(test:Instrumentation,result:Bundle) {
        val c=test.targetContext
        check(Build.HARDWARE in listOf("ranchu","goldfish"))
        check(!File(c.noBackupFilesDir,"credentials.enc").exists())
        fun ui(block:()->Unit){var error:Throwable?=null;test.runOnMainSync { try{block()}catch(t:Throwable){error=t} };error?.let{throw it}}
        val store=PetActivitiesStore(c);val now=System.currentTimeMillis()
        c.getSharedPreferences("pet_activities",0).edit().clear().commit()
        c.getSharedPreferences("pet_audio",0).edit().putString("mode","off").putBoolean("effects",false).commit()
        c.getSharedPreferences("reply_options",0).edit().putBoolean("quiet",true).commit()
        val p=PetState(seed=888,hatched=true,ageMs=10*PetState.DAY,updatedAt=now)
        PetStore(c).save(p)
        store.welcome(p.seed);store.welcome(p.seed);check(store.events(p.seed).size==1)
        check(!store.begin(p.copy(hatched=false),0,now))
        check(store.begin(p,0,now));check(!store.begin(p,1,now));check(store.claim(p,now)==null)
        val end=store.trip(p.seed)!!.due
        val reloaded=PetActivitiesStore(c);val item=checkNotNull(reloaded.claim(p,end));check(item in reloaded.items(p.seed));check(reloaded.claim(p,end)==null)
        check(reloaded.events(p.seed).count { it.kind=="trip" }==1)
        check(!store.decorate(p.seed,99));check(store.decorate(p.seed,item));check(PetActivitiesStore(c).decoration(p.seed)==item)
        check(store.begin(p,1,now));store.recall(p.seed);check(store.trip(p.seed)==null)
        store.chooseMission(p.seed,0);val stale=store.missionToken(p.seed)!!;store.chooseMission(p.seed,1);val token=store.missionToken(p.seed)!!
        check(!store.completeMission(p.seed,stale));check(store.completeMission(p.seed,token));check(!store.completeMission(p.seed,token))
        store.chooseMission(p.seed,2);check(!store.completeMission(p.seed,store.missionToken(p.seed)!!))
        check(store.events(999).isEmpty() && store.items(999).isEmpty() && store.decoration(999)==-1)
        val promise=PetPromise("fixture-promise",p.seed,1,19,0,now-1000)
        PetPromises.add(c,promise);PetPromises.fire(c,promise.id)
        check(store.promises().single().let { it.pending && it.due>now })
        val notifications=c.getSystemService(NotificationManager::class.java)
        repeat(20){if(notifications.activeNotifications.none { it.tag==promise.id })Thread.sleep(50)}
        check(notifications.activeNotifications.any { it.tag==promise.id }) { "R1 promise notification was not posted" }
        val due=store.promises().single().due;PetPromises.fire(c,promise.id);check(store.promises().single().due==due)
        PetPromises.restore(c);check(store.promises().single().pending)
        PetPromises.remove(c,promise.id);check(store.promises().isEmpty())
        store.event(p.seed,"food");store.event(p.seed,"play");store.event(p.seed,"memory","We watched the stars together. Fictional test memory.")
        c.startActivity(Intent(c,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        var home:MainActivity?=null
        repeat(80){if(home==null){ui{home=MainActivity.foreground?.takeIf { it.hasWindowFocus() }};if(home==null)Thread.sleep(100)}}
        val activity=checkNotNull(home)
        val open=MainActivity::class.java.getDeclaredMethod("showPetRoom").apply { isAccessible=true }
        fun field(name:String)=MainActivity::class.java.getDeclaredField(name).apply { isAccessible=true }
        fun descendants(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap { descendants(v.getChildAt(it)) }else emptyList()
        var controller:PetActivities?=null
        fun dialog()=PetActivities::class.java.getDeclaredField("dialog").apply{isAccessible=true}.get(controller) as Dialog
        fun capture(name:String){
            Thread.sleep(250);ui{
                val view=dialog().window!!.decorView;check(view.width==480 && view.height==640){"Unexpected fixture dimensions ${view.width}x${view.height}"}
                val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888);view.draw(Canvas(bitmap));File(c.cacheDir,"special-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) };bitmap.recycle()
            }
        }
        try{
            for(lang in DisplayLanguage.entries){
                ui { UiText.set(c,lang);open.invoke(activity) };Thread.sleep(250)
                val room=checkNotNull(PetRoom.foreground)
                ui{controller=PetActivities(activity,room::dialogueState){};controller!!.show()}
                capture(lang.name.lowercase())
                ui {
                    val labels=descendants(dialog().window!!.decorView).filterIsInstance<Button>().map { it.text.toString() }
                    check(labels.size==7){"Expected six activities and close"}
                    check(labels.none { it in listOf("Feed","Play","Care","먹이","놀이","돌봄") })
                    controller!!.explore()
                };capture("explore-${lang.name.lowercase()}")
                ui{controller!!.diary()};capture("diary-${lang.name.lowercase()}")
                ui{controller!!.memories()};capture("memories-${lang.name.lowercase()}")
                ui{controller!!.decorate()};capture("decor-${lang.name.lowercase()}")
                ui{controller!!.mission()};capture("mission-${lang.name.lowercase()}")
                ui{controller!!.promises()};capture("promises-${lang.name.lowercase()}")
                ui {
                    descendants(dialog().window!!.decorView).filterIsInstance<Button>().single { it.text.toString() in listOf("새 약속","新しい約束","New promise") }.performClick()
                    descendants(dialog().window!!.decorView).filterIsInstance<Button>().single { it.text.toString()==PetActivityText.promise(c,1) }.performClick()
                };capture("time-${lang.name.lowercase()}")
                ui {
                    val buttons=descendants(dialog().window!!.decorView).filterIsInstance<Button>()
                    val confirm=buttons.single { it.text.toString() in listOf("매일 이 시간에 알림","毎日この時刻に通知","Remind me daily") }
                    val visible=android.graphics.Rect();check(confirm.getGlobalVisibleRect(visible) && visible.height()>=40)
                    confirm.performClick();check(store.promises().any { it.seed==p.seed && it.hour==19 && it.minute==0 })
                }
                ui{controller!!.close();room.dismiss()}
            }
            // The real receive pipeline must suppress JSON chunks and grant only a correlated verified mission.
            val fresh=p.copy(seed=889);PetStore(c).save(fresh);store.chooseMission(fresh.seed,0)
            val request=PhotoQuestion.Request(byteArrayOf(),"fixture",petSeed=fresh.seed,petMissionToken=store.missionToken(fresh.seed),petMissionTarget=0)
            val turn=ConversationTurn().also { it.user="Fictional camera mission" }
            val receive=MainActivity::class.java.getDeclaredMethod("receiveReply",String::class.java,String::class.java,Boolean::class.javaPrimitiveType).apply{isAccessible=true}
            ui{
                field("activeTurn").set(activity,turn);field("missionConversation").set(activity,turn);field("missionPhoto").set(activity,request);field("petDialogueTurn").set(activity,null)
                receive.invoke(activity,"photo","{\"matched\":",false);check(turn.replies.isEmpty())
                receive.invoke(activity,"photo","{\"matched\":true,\"reply\":\"A green plant is visible.\"}",true)
                check(store.items(fresh.seed)==listOf(3));check(turn.replies.values.none { it.contains("matched") })
                receive.invoke(activity,"photo","{\"matched\":true,\"reply\":\"duplicate\"}",true)
                check(store.events(fresh.seed).count { it.kind=="mission" }==1)
                // Starting a normal typed/voice turn clears the previous photo verifier.
                MainActivity::class.java.getDeclaredMethod("preparePetDialogue").apply { isAccessible=true }.invoke(activity)
                check(field("missionPhoto").get(activity)==null && field("missionConversation").get(activity)==null)
                val ordinary=ConversationTurn().also { it.user="How are you?" }
                field("activeTurn").set(activity,ordinary)
                receive.invoke(activity,"ordinary","I'm doing well.",true)
                check(ordinary.replies["ordinary"]=="I'm doing well.")
                check(store.events(fresh.seed).count { it.kind=="mission" }==1)
            }
            // Alarm rows and saved decorations from a previous companion never leak into a new egg.
            PetPromises.add(c,PetPromise("old-life",p.seed,1,19,0,now+100000));PetPromises.restore(c);check(store.promises().isEmpty())
            result.putString("stream","PASS: persisted trip/reload, one-time rewards, early recall, per-companion isolation, decoration ownership, stale mission tokens, daily cap, correlated photo reply pipeline, recurring R1 notification scheduling/cancel, and six scrolling pages in KO/JA/EN at 480x640. Offline fixtures only.")
        }finally{ui{controller?.close();PetRoom.foreground?.dismiss()};store.promises().forEach { PetPromises.remove(c,it.id) }}
    }
}

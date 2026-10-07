package dev.cameronpak.muser1

import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.TextView
import java.io.File

/** Native UI/reply pipeline with fictional responses. No microphone, credentials or live requests. */
internal object PetDialogueCheck {
    fun run(test:Instrumentation,result:Bundle) {
        val ctx=test.targetContext
        check(Build.HARDWARE in listOf("ranchu","goldfish"))
        check(!File(ctx.noBackupFilesDir,"credentials.enc").exists())
        fun ui(block:()->Unit){var failure:Throwable?=null;test.runOnMainSync{try{block()}catch(t:Throwable){failure=t}};failure?.let{throw it}}
        ctx.getSharedPreferences("pet_audio",0).edit().putString("mode","off").putBoolean("effects",false).commit()
        ctx.getSharedPreferences("reply_options",0).edit().putBoolean("quiet",true).commit()
        PetStore(ctx).save(PetState(hatched=true,updatedAt=System.currentTimeMillis(),food=18.0,ageMs=10*PetState.DAY))
        ctx.startActivity(Intent(ctx,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        var home:MainActivity?=null
        repeat(80){if(home==null){ui{home=MainActivity.foreground?.takeIf{it.hasWindowFocus()}};if(home==null)Thread.sleep(100)}}
        val activity=checkNotNull(home)
        fun field(name:String)=MainActivity::class.java.getDeclaredField(name).apply{isAccessible=true}
        fun roomField(name:String)=PetRoom::class.java.getDeclaredField(name).apply{isAccessible=true}
        fun call(name:String)=MainActivity::class.java.getDeclaredMethod(name).apply{isAccessible=true}.invoke(activity)
        val receive=MainActivity::class.java.getDeclaredMethod("receiveReply",String::class.java,String::class.java,Boolean::class.javaPrimitiveType).apply{isAccessible=true}
        lateinit var room:PetRoom
        fun open(){ui{call("showPetRoom");room=PetRoom.foreground!!};Thread.sleep(300)}
        fun capture(name:String){val v=room.window!!.decorView;val b=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888);v.draw(Canvas(b));File(ctx.cacheDir,"pet-dialogue-$name.png").outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)};b.recycle()}
        try {
            for(language in listOf(DisplayLanguage.KO,DisplayLanguage.JA,DisplayLanguage.EN)) {
                ui{UiText.set(ctx,language)};open()
                ui {
                    val question=when(language){DisplayLanguage.KO->"춤춰 줘!";DisplayLanguage.JA->"踊ってみて！";else->"Can you dance?"}
                    val reply=when(language){DisplayLanguage.KO->"좋아! 너랑 같이 춤추니까 신나!";DisplayLanguage.JA->"もちろん！いっしょに踊ろう！";else->"Of course! Let's dance together!"}
                    val current=ConversationTurn().also{it.user=question}
                    field("activeTurn").set(activity,current)
                    field("activePetTurn").setBoolean(activity,true);call("preparePetDialogue")
                    field("sending").setBoolean(activity,true)
                    val before=room.dialogueState()
                    room.agentUpdate("OFFLINE TEST",question,true)
                    receive.invoke(activity,"test","{\"reply\":",false)
                    check(current.replies.isEmpty())
                    receive.invoke(activity,"test","{\"reply\":${org.json.JSONObject.quote(reply)},\"action\":\"dance\"}",true)
                    check(current.replies["test"]==reply && activity.replyMuted)
                    val text=roomField("agentText").get(room) as TextView
                    check(text.text.contains(reply) && !text.text.contains("\"action\""))
                    val scene=roomField("dialogueScene").get(room) as PetScene
                    check(scene.lastExpression=="dance")
                    val after=room.dialogueState();check(after.food==before.food && after.ageMs==before.ageMs)
                    // No duplicate reply can replace the response or trigger a second expression.
                    receive.invoke(activity,"test","{\"reply\":\"duplicate\",\"action\":\"jump\"}",true)
                    check(scene.lastExpression=="dance" && current.replies["test"]==reply)
                }
                Thread.sleep(300)
                ui {
                    val scene=roomField("dialogueScene").get(room) as PetScene
                    val text=roomField("agentText").get(room) as TextView
                    check(scene.isShown && scene.width>0 && scene.height>0)
                    check(text.isShown && text.width>0 && text.height>0)
                    val s=IntArray(2);val t=IntArray(2);scene.getLocationOnScreen(s);text.getLocationOnScreen(t)
                    check(s[0]+scene.width<=t[0])
                    capture(language.name.lowercase())
                    if(language==DisplayLanguage.KO) {
                        val started=PetScene::class.java.getDeclaredField("reactionStarted").apply{isAccessible=true}.getLong(scene)
                        scene.previewTime=started+300;capture("dance-a")
                        scene.previewTime=started+850;capture("dance-b");scene.previewTime=null
                    }
                    room.dismiss()
                }
            }
            // A completed reply from a closed room must not animate a newly opened room.
            open();ui{call("preparePetDialogue");room.dismiss()};open()
            ui {
                field("activeTurn").set(activity,ConversationTurn().also{it.user="old request"})
                receive.invoke(activity,"late","{\"reply\":\"old answer\",\"action\":\"jump\"}",true)
                check((roomField("dialogueScene").get(room) as PetScene).lastExpression!="jump")
                room.dismiss()
            }
            result.putString("stream","PASS: pet reply buffering, normalized text, one-shot dance, shared mute, care-state preservation, visible non-overlapping avatar/chat in KO/JA/EN, and stale-room expression guard. Offline fixtures only.")
        } finally { ui{PetRoom.foreground?.dismiss()} }
    }
}

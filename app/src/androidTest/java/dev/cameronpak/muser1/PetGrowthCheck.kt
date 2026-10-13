package dev.cameronpak.muser1

import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import org.json.JSONObject
import java.io.File

/** Unpaired disposable emulator only. Ages and reset dates are fictional. */
internal object PetGrowthCheck {
    fun run(test:Instrumentation,result:Bundle){
        val c=test.targetContext
        check(Build.HARDWARE in listOf("ranchu","goldfish"));check(!File(c.noBackupFilesDir,"credentials.enc").exists())
        fun ui(block:()->Unit){var error:Throwable?=null;test.runOnMainSync {try{block()}catch(t:Throwable){error=t}};error?.let {throw it}}
        fun waitFor(description:String,predicate:()->Boolean){var ok=false;repeat(100){if(!ok){ui{ok=predicate()};if(!ok)Thread.sleep(70)}};check(ok){description}}
        val now=System.currentTimeMillis();val started=now-2*PetState.DAY-3*PetState.HOUR
        val pet=PetState(updatedAt=now,seed=6001,startedAt=started,incubationMs=40*PetState.HOUR)
        val store=PetStore(c);store.save(pet);check(store.load(now).startedAt==started)
        // Restore a pre-feature life from its exact reset archive without changing care/identity.
        val prefs=c.getSharedPreferences("muse_pet",0);val old=JSONObject(prefs.getString("life_v2",null)!!).apply {remove("startedAt");remove("startEstimated")}
        prefs.edit().putString("life_v2",old.toString()).commit()
        c.getSharedPreferences("muse_pet_archive",0).edit().clear().putString("pet_${started}_new_egg","{}").commit()
        val restored=store.load(now);check(restored.startedAt==started && !restored.startEstimated && restored.seed==pet.seed && restored.eggCare==pet.eggCare)
        store.save(restored);check(store.load(now).startedAt==started)
        c.getSharedPreferences("pet_audio",0).edit().putString("mode","off").putBoolean("effects",false).commit()
        c.getSharedPreferences("reply_options",0).edit().putBoolean("quiet",true).commit()
        c.startActivity(Intent(c,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        waitFor("Home did not resume"){MainActivity.foreground?.hasWindowFocus()==true}
        val home=MainActivity.foreground!!
        fun field(name:String)=PetRoom::class.java.getDeclaredField(name).apply {isAccessible=true}
        ui{MainActivity::class.java.getDeclaredMethod("showPetRoom").apply {isAccessible=true}.invoke(home)}
        waitFor("Pet room did not show"){PetRoom.foreground?.hasWindowFocus()==true}
        val room=PetRoom.foreground!!;val audio=PetAudio(home)
        fun audioField(name:String)=PetAudio::class.java.getDeclaredField(name).apply {isAccessible=true}
        try {
            for(language in DisplayLanguage.entries){
                ui{UiText.set(c,language);PetRoom::class.java.getDeclaredMethod("render").apply {isAccessible=true}.invoke(room)}
                ui{val label=field("lifeElapsed").get(room) as TextView;check(label.text.contains(PetElapsed.between(started,System.currentTimeMillis()).text(language)));check(label.layout.getEllipsisCount(0)==0){"Elapsed time clipped: $language"}}
            }
            UiText.set(c,DisplayLanguage.KO)
            val ages=listOf(0L,PetState.DAY,4*PetState.DAY,9*PetState.DAY,21*PetState.DAY)
            for(stage in 0..5){
                ui{val s=if(stage==0)restored else restored.copy(hatched=true,ageMs=ages[stage-1]);field("pet").set(room,s)
                    PetRoom::class.java.getDeclaredMethod("render").apply {isAccessible=true}.invoke(room)
                    val view=room.window!!.decorView
                    check(view.width in 320..480 && view.height in 400..640)
                    val image=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888);view.draw(Canvas(image))
                    File(c.cacheDir,"growth-stage-$stage.png").outputStream().use {image.compress(Bitmap.CompressFormat.PNG,100,it)};image.recycle()
                }
                Thread.sleep(150)
            }
            ui {
                val roomAudio=field("audio").get(room) as PetAudio
                roomAudio.mode="off";roomAudio.update(1,false,false);roomAudio.voice("hello",DisplayLanguage.KO)
                room.refreshReplyMute()
                check(PetAudio::class.java.getDeclaredField("voicePlayer").apply {isAccessible=true}.get(roomAudio)==null)
                PetRoom::class.java.getDeclaredMethod("showSoundSettings").apply {isAccessible=true}.invoke(room)
            }
            waitFor("Sound panel did not focus"){(field("soundDialog").get(room) as? android.app.Dialog)?.window?.decorView?.hasWindowFocus()==true}
            ui {
                val dialog=field("soundDialog").get(room) as android.app.Dialog
                val view=dialog.window!!.decorView
                fun descendants(v:android.view.View):List<android.view.View> = listOf(v)+if(v is android.view.ViewGroup)(0 until v.childCount).flatMap {descendants(v.getChildAt(it))}else emptyList()
                val nodes=descendants(view)
                val switches=nodes.filterIsInstance<android.widget.Switch>();check(switches.size==2)
                switches.forEach {switch->val rect=android.graphics.Rect();check(switch.getGlobalVisibleRect(rect) && rect.bottom<=640)}
                check(switches.any {it.text.toString()=="짧은 캐릭터 음성"})
                val image=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888);view.draw(Canvas(image))
                File(c.cacheDir,"growth-music.png").outputStream().use {image.compress(Bitmap.CompressFormat.PNG,100,it)};image.recycle()
                nodes.filterIsInstance<android.widget.Button>().single {it.text.toString()=="닫기"}.performClick()
            }
            ui{room.dismiss()}
            waitFor("Home did not resume for isolated audio checks"){home.hasWindowFocus()}
            ui {
                val art=PetArt(c.resources);val dimensions=mutableSetOf<Pair<Int,Int>>()
                for(level in 1..5){val sprite=art.sprite(level);check(sprite.bitmap.width in 100..400 && sprite.bitmap.height in 100..400);check(sprite.eyes.size==2);check(sprite.eyes.all {it.left>=0 && it.right<=1 && it.top>=0 && it.bottom<=1});dimensions+=sprite.bitmap.width to sprite.bitmap.height}
                check(dimensions.size>=4)
                audio.effects=true;audio.characterVoice=true;audio.mode="auto";audio.update(1,false,false)
            }
            waitFor("Growth music did not start"){(audioField("player").get(audio) as? MediaPlayer)?.isPlaying==true}
            ui{check(audioField("playingTrack").get(audio)=="lullaby");audio.update(4,false,false);check(audioField("playingTrack").get(audio)=="meadow");audio.mode="off";audio.update(4,false,false)}
            for(language in DisplayLanguage.entries){
                ui{audioField("voiceAt").setLong(audio,-10000);audio.voice("hello",language)}
                waitFor("Local voice did not start: $language"){(audioField("voicePlayer").get(audio) as? MediaPlayer)?.isPlaying==true}
                ui{audio.mute();check(audioField("voicePlayer").get(audio)==null);audio.update(4,false,false);check(audioField("voicePlayer").get(audio)==null)}
            }
            ui{audio.characterVoice=false;audioField("voiceAt").setLong(audio,-10000);audio.voice("thanks",DisplayLanguage.KO);check(audioField("voicePlayer").get(audio)==null);audio.close()}
            result.putString("stream","PASS: exact legacy-reset restoration without care reset, persisted start time, unclipped KO/JA/EN calendar elapsed time, six native stage screens, five bounded sprite decodes and eye landmarks, growth-selected music and all three local voice languages, music-off voice playback, mute cancellation, main mute button cancellation, music-picker controls and disabled-voice guard. Fictional unpaired emulator only; no network or microphone request.")
        } finally {ui{audio.close();room.dismiss()}}
    }
}

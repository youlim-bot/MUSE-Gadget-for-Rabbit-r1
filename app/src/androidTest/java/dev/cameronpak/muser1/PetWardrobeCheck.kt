package dev.cameronpak.muser1

import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import java.io.File
import java.security.MessageDigest

/** Disposable account-less emulator only. Fixtures never record, send a turn, or reset an owner life. */
internal object PetWardrobeCheck {
    fun run(test:Instrumentation,result:Bundle){
        val c=test.targetContext
        check(Build.HARDWARE in listOf("ranchu","goldfish"))
        for(name in listOf("credentials.enc","credentials.enc.bak","credentials.enc.new"))check(!File(c.noBackupFilesDir,name).exists())
        check(!File(c.filesDir,"pending-sdk-token").exists())
        fun ui(block:()->Unit){var error:Throwable?=null;test.runOnMainSync {try{block()}catch(t:Throwable){error=t}};error?.let {throw it}}
        fun waitFor(label:String,predicate:()->Boolean){var ok=false;repeat(100){if(!ok){ui{ok=predicate()};if(!ok)Thread.sleep(60)}};if(!ok){val shot=test.uiAutomation.takeScreenshot();File(c.cacheDir,"dressup-failure.png").outputStream().use {shot.compress(Bitmap.CompressFormat.PNG,100,it)};shot.recycle();error(label)}}
        fun nodes(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap {nodes(v.getChildAt(it))}else emptyList()
        fun capture(view:View,name:String){check(view.width==480 && view.height==640);val b=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888);view.draw(Canvas(b));File(c.cacheDir,"dressup-$name.png").outputStream().use {b.compress(Bitmap.CompressFormat.PNG,100,it)};b.recycle()}
        val now=System.currentTimeMillis();val store=PetStore(c)
        val plain=PetState(updatedAt=now,hatched=true,seed=6503,food=50.0,startedAt=now)
        store.save(plain)
        c.getSharedPreferences("pet_audio",0).edit().putString("mode","off").putBoolean("effects",false).putBoolean("character_voice",false).commit()
        c.getSharedPreferences("reply_options",0).edit().putBoolean("quiet",true).commit()
        c.startActivity(Intent(c,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        waitFor("Home focus"){MainActivity.foreground?.hasWindowFocus()==true}
        val home=MainActivity.foreground!!
        ui{UiText.set(c,DisplayLanguage.KO);MainActivity::class.java.getDeclaredMethod("showPetRoom").apply {isAccessible=true}.invoke(home)}
        waitFor("Room focus"){PetRoom.foreground?.hasWindowFocus()==true}
        val room=PetRoom.foreground!!
        fun field(name:String)=PetRoom::class.java.getDeclaredField(name).apply {isAccessible=true}
        fun s()=field("pet").get(room) as PetState
        fun render(){PetRoom::class.java.getDeclaredMethod("render").apply {isAccessible=true}.invoke(room)}
        fun dialog()=field("wardrobeDialog").get(room) as PetWardrobeDialog
        fun click(label:String){nodes(dialog().window!!.decorView).filterIsInstance<Button>().single {it.text.toString()==label}.performClick()}
        fun sideTap(){val down=SystemClock.uptimeMillis();check(room.petKey(KeyEvent(down,down,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_PAIRING,0)));check(room.petKey(KeyEvent(down,down+80,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_PAIRING,0)))}
        val adultBefore=c.resources.openRawResource(R.drawable.pet_adult).use {it.readBytes()}
        try {
            // Wheel entry from the room and side-button activation, no voice hold.
            ui{repeat(4){val down=SystemClock.uptimeMillis();room.petKey(KeyEvent(down,down,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_DOWN,0))};check(field("selected").getInt(room)==4);sideTap()}
            waitFor("Wardrobe focus"){dialog().focused}
            ui {
                check(nodes(dialog().window!!.decorView).filterIsInstance<TextView>().any {it.text.contains("어린 뮤즈로")})
                capture(dialog().window!!.decorView,"wardrobe-locked")
                // Initially selected category HEAD; wheel down to NECK and tap.
                val down=SystemClock.uptimeMillis();room.petKey(KeyEvent(down,down,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_DOWN,0));sideTap()
                check(nodes(dialog().window!!.decorView).filterIsInstance<TextView>().any {it.text.contains("함께 첫 식사")})
                val before=s();click("입히기");check(s()==before)
                dialog().dismiss()
            }
            waitFor("Room returned"){room.window!!.decorView.hasWindowFocus()}
            // Earn all first-care/game rewards using production rules and non-overlapping cooldown times.
            val rewarded=plain.feed(PetFood.PORRIDGE,now).care(PetAction.CLEAN,now).play(PetGame.STARS,true,now)
                .copy(playedAt=0L).play(PetGame.MEMORY,true,now)
            ui{field("pet").set(room,rewarded);render();room.showWardrobe()}
            waitFor("Unlocked wardrobe focus"){dialog().focused}
            ui {
                fun wheel(steps:Int){repeat(kotlin.math.abs(steps)){val down=SystemClock.uptimeMillis();room.petKey(KeyEvent(down,down,KeyEvent.ACTION_DOWN,if(steps>0)KeyEvent.KEYCODE_DPAD_DOWN else KeyEvent.KEYCODE_DPAD_UP,0))}}
                wheel(4);sideTap();check(s().wardrobe.equipped.isEmpty());wheel(3);sideTap();check(s().wardrobe.equipped["HEAD"]=="beanie")
                // Service/focus cancellation prevents a stale release from equipping or removing.
                val stale=SystemClock.uptimeMillis();room.petKey(KeyEvent(stale,stale,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_PAIRING,0));room.cancelAgentInput();room.petKey(KeyEvent(stale,stale+80,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_PAIRING,0));check(s().wardrobe.equipped["HEAD"]=="beanie")
                wheel(-1);sideTap();check("HEAD" !in s().wardrobe.equipped)
                wheel(-2);sideTap();wheel(3);sideTap();check(s().wardrobe.equipped["HEAD"]=="beanie")
                click("목");click("빨간 목도리");click("입히기")
                click("옷");click("노란 우비");click("입히기")
                click("안경");click("동그란 안경");click("입히기")
                check(s().wardrobe.equipped.size==4);capture(dialog().window!!.decorView,"wardrobe-outfit")
                click("벗기기");check("FACE" !in s().wardrobe.equipped);click("동그란 안경");click("입히기")
                check(store.load(now).wardrobe==s().wardrobe)
            }
            for(language in listOf(DisplayLanguage.JA,DisplayLanguage.EN)){
                ui{dialog().dismiss();UiText.set(c,language);room.showWardrobe()};waitFor("Localized wardrobe focus"){dialog().focused}
                ui{capture(dialog().window!!.decorView,"wardrobe-${language.name.lowercase()}");check(nodes(dialog().window!!.decorView).filterIsInstance<TextView>().any {it.text.toString()==PetWardrobeText.title(c)})}
            }
            ui{dialog().dismiss();UiText.set(c,DisplayLanguage.KO)}
            waitFor("Room capture focus"){room.window!!.decorView.hasWindowFocus()}
            val ages=listOf(0L,PetState.DAY,4*PetState.DAY,9*PetState.DAY,21*PetState.DAY)
            for(stage in 1..5){
                ui {
                    var p=PetWardrobe.refresh(rewarded.copy(ageMs=ages[stage-1]),now)
                    for((slot,id)in listOf(PetWardrobeSlot.HEAD to "beanie",PetWardrobeSlot.NECK to "scarf",PetWardrobeSlot.BODY to "raincoat",PetWardrobeSlot.FACE to "glasses"))p=PetWardrobe.equip(p,slot,id)
                    field("pet").set(room,p);render();(field("room").get(room) as PetScene).previewTime=1000L
                    capture(room.window!!.decorView,"stage-$stage-outfit")
                    (field("room").get(room) as PetScene).previewTime=0L;capture(room.window!!.decorView,"stage-$stage-blink")
                    // Second full outfit exercises the stage-specific sweater and other accessories.
                    var alt=p.copy(wardrobe=p.wardrobe.copy(unlocked=PetWardrobe.items.associate {it.id to now}))
                    for((slot,id)in listOf(PetWardrobeSlot.HEAD to "flowers",PetWardrobeSlot.NECK to "bow",PetWardrobeSlot.BODY to "sweater",PetWardrobeSlot.FACE to "sunglasses"))alt=PetWardrobe.equip(alt,slot,id)
                    field("pet").set(room,alt);render();(field("room").get(room) as PetScene).previewTime=1000L;capture(room.window!!.decorView,"stage-$stage-alternate")
                    // Removing every slot returns exactly the same base renderer state.
                    var bare=alt;PetWardrobeSlot.entries.forEach {bare=PetWardrobe.equip(bare,it,null)};field("pet").set(room,bare);render();capture(room.window!!.decorView,"stage-$stage-bare")
                }
            }
            ui {
                val scene=field("room").get(room) as PetScene;scene.previewTime=null
                field("pet").set(room,plain.copy(hatched=false));render();room.showWardrobe()
            }
            waitFor("Egg wardrobe focus"){dialog().focused}
            ui{check(nodes(dialog().window!!.decorView).filterIsInstance<TextView>().any {it.text.contains("알에서 태어난 뒤")});check(!nodes(dialog().window!!.decorView).filterIsInstance<Button>().single {it.text.toString()=="입히기"}.isEnabled);capture(dialog().window!!.decorView,"wardrobe-egg");dialog().dismiss()}
            check(adultBefore.contentEquals(c.resources.openRawResource(R.drawable.pet_adult).use {it.readBytes()}))
            check(MessageDigest.getInstance("SHA-256").digest(adultBefore).joinToString(""){"%02x".format(it)}=="4beb9bc2ebe6d2664e5571f850d677222e0f92ae0989e65ec3f77c0b656ccfaf")
            result.putString("stream","PASS: wardrobe wheel entry and side-button tap, locked hints and guard, four-slot preview/equip/remove and persistence, KO/JA/EN wardrobe screens, all five current stages in two full outfits plus blink/bare captures, egg guard, byte-identical adult PNG. Fictional account-less emulator only; no microphone, camera or live Muse turn.")
        } finally {ui{field("wardrobeDialog").get(room)?.let {(it as PetWardrobeDialog).dismiss()};room.dismiss()}}
    }
}

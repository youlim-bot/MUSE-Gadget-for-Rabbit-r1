package dev.cameronpak.muser1

import android.app.Activity
import android.app.AlertDialog
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import java.io.File

/** Opt-in runner for a fresh disposable unpaired emulator; never reset an owner life. */
internal object BrightnessFeedCheck {
    fun run(test:Instrumentation,result:Bundle,compact:Boolean=false) {
        val c=test.targetContext
        check(Build.HARDWARE in listOf("ranchu","goldfish"))
        for(name in listOf("credentials.enc","credentials.enc.bak","credentials.enc.new"))check(!File(c.noBackupFilesDir,name).exists())
        check(!File(c.filesDir,"pending-sdk-token").exists())
        if(!compact)check(!c.getSharedPreferences("muse_pet",0).contains("life_v2")){"Use a NEW emulator without a pet record"}
        fun ui(block:()->Unit){var error:Throwable?=null;test.runOnMainSync {try{block()}catch(t:Throwable){error=t}};error?.let {throw it}}
        fun await(label:String,predicate:()->Boolean){repeat(120){var done=false;ui{done=predicate()};if(done)return;Thread.sleep(50)};error(label)}
        fun nodes(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap {nodes(v.getChildAt(it))}else emptyList()
        fun capture(window:Window,name:String){
            val v=window.decorView;val b=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888);v.draw(Canvas(b))
            File(c.cacheDir,"brightness-feed-${if(compact)"compact-" else ""}$name.png").outputStream().use {b.compress(Bitmap.CompressFormat.PNG,100,it)};b.recycle()
            if(compact) {
                val screen=checkNotNull(test.uiAutomation.takeScreenshot())
                check(screen.width==240 && screen.height==320){"Expected 240x320 physical emulator viewport"}
                File(c.cacheDir,"brightness-feed-compact-viewport-$name.png").outputStream().use {screen.compress(Bitmap.CompressFormat.PNG,100,it)};screen.recycle()
            }
        }
        fun field(owner:Any,name:String)=owner.javaClass.getDeclaredField(name).apply {isAccessible=true}
        fun call(owner:Any,name:String)=owner.javaClass.getDeclaredMethod(name).apply {isAccessible=true}.invoke(owner)
        fun focusedWindow():Window {
            @Suppress("UNCHECKED_CAST")
            val windows=AppBrightness::class.java.getDeclaredField("windows").apply {isAccessible=true}.get(AppBrightness) as Map<Window,Unit>
            return windows.keys.single {it.decorView.hasWindowFocus()}
        }
        val systemBrightness=android.provider.Settings.System.getInt(c.contentResolver,"screen_brightness",-1)
        val systemMode=android.provider.Settings.System.getInt(c.contentResolver,"screen_brightness_mode",-1)
        c.getSharedPreferences("pet_audio",0).edit().putString("mode","off").putBoolean("effects",false).putBoolean("character_voice",false).commit()
        c.getSharedPreferences("reply_options",0).edit().putBoolean("quiet",true).commit()
        ui{UiText.set(c,DisplayLanguage.KO)}
        c.startActivity(Intent(c,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        await("Home focus"){MainActivity.foreground?.hasWindowFocus()==true}
        var home=MainActivity.foreground!!
        for(language in DisplayLanguage.entries) {
            ui{UiText.set(c,language);call(home,"showSettingsMenu")}
            await("Settings focus"){(field(home,"settingsDialog").get(home) as? MuseSettingsDialog)?.window?.decorView?.hasWindowFocus()==true}
            ui {
                val settings=field(home,"settingsDialog").get(home) as MuseSettingsDialog
                val row=nodes(settings.window!!.decorView).single {it.tag=="settings_brightness"}
                row.requestFocus();capture(settings.window!!,"directory-${language.name.lowercase()}");row.performClick()
            }
            await("Brightness focus"){(field(home,"controls").get(home) as? BrightnessDialog)?.window?.decorView?.hasWindowFocus()==true}
            ui {
                val dialog=field(home,"controls").get(home) as BrightnessDialog
                check(dialog.listView.adapter.count==6)
                capture(dialog.window!!,"settings-${language.name.lowercase()}")
                // Real list DPAD navigation uses the same Android key codes as the wheel.
                dialog.listView.requestFocusFromTouch();dialog.listView.setSelection(1)
            }
            await("Initial list selection"){(field(home,"controls").get(home) as BrightnessDialog).listView.selectedItemPosition==1}
            ui {
                val dialog=field(home,"controls").get(home) as BrightnessDialog
                dialog.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_DOWN))
                dialog.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_DOWN))
            }
            await("Wheel did not select 40%"){(field(home,"controls").get(home) as BrightnessDialog).listView.selectedItemPosition==2}
            ui {
                val dialog=field(home,"controls").get(home) as BrightnessDialog
                dialog.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER))
                dialog.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_CENTER))
            }
            await("Wheel activation did not apply 40%"){AppBrightness.level(c)==40}
            ui {
                check(home.window.attributes.screenBrightness==.4f){"Home override"}
                val settings=field(home,"settingsDialog").get(home) as MuseSettingsDialog
                check(settings.window!!.attributes.screenBrightness==.4f){"Settings override"};settings.dismiss()
            }
            await("Home return"){home.hasWindowFocus()}
        }
        ui{home.recreate()}
        await("Recreated home"){MainActivity.foreground!=null && MainActivity.foreground!==home && MainActivity.foreground!!.hasWindowFocus()}
        home=MainActivity.foreground!!
        ui{check(AppBrightness.level(c)==40);check(home.window.attributes.screenBrightness==.4f);UiText.set(c,DisplayLanguage.KO)}
        // Each activity owns a window; emulator cameras are disabled and no image is taken.
        for(type in listOf(ClockActivity::class.java,LibraryActivity::class.java,CameraActivity::class.java)) {
            val activity=test.startActivitySync(Intent(c,type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            await("${type.simpleName} focus"){activity.hasWindowFocus()}
            ui{check(activity.window.attributes.screenBrightness==.4f){type.simpleName};activity.finish()}
            await("${type.simpleName} return"){MainActivity.foreground?.hasWindowFocus()==true}
        }
        home=MainActivity.foreground!!
        if(compact) {
            ui{AppBrightness.set(c,-1);check(home.window.attributes.screenBrightness==-1f)}
            check(android.provider.Settings.System.getInt(c.contentResolver,"screen_brightness",-1)==systemBrightness)
            check(android.provider.Settings.System.getInt(c.contentResolver,"screen_brightness_mode",-1)==systemMode)
            result.putString("stream","PASS: compact 240x320 KO/JA/EN brightness menus, wheel navigation/selection, persisted preference after recreation, activity/dialog overrides and system restoration; no pet fixture or microphone/camera capture/live turn.")
            return
        }
        val now=System.currentTimeMillis()
        val plain=PetState(updatedAt=now,hatched=true,seed=7313,food=70.0,fedAt=now,
            wardrobe=PetWardrobeState(unlocked=mapOf("beanie" to now,"bow" to now)))
        ui{PetStore(c).save(plain);call(home,"showPetRoom")}
        await("Pet room focus"){PetRoom.foreground?.hasWindowFocus()==true}
        val room=PetRoom.foreground!!
        fun state()=field(room,"pet").get(room) as PetState
        fun prepare(s:PetState){field(room,"pet").set(room,s);call(room,"render")}
        fun notice()=(field(room,"notice").get(room) as TextView).text.toString()
        fun pickMeal(s:PetState) {
            ui{prepare(s);call(room,"chooseFood")}
            await("Meal focus"){!room.window!!.decorView.hasWindowFocus()}
            ui {
                val w=focusedWindow();check(w.attributes.screenBrightness==.4f)
                val list=nodes(w.decorView).filterIsInstance<android.widget.ListView>().single()
                list.performItemClick(list.getChildAt(1),1,list.adapter.getItemId(1))
            }
            await("Meal room return"){room.window!!.decorView.hasWindowFocus()}
        }
        ui{check(room.window!!.attributes.screenBrightness==.4f);capture(room.window!!,"hungry-70")}
        pickMeal(plain)
        ui{check(state().food==100.0 && state().meals==1);check(notice().contains("냠냠"));capture(room.window!!,"fed-70-success")}
        pickMeal(state())
        ui{check(state().meals==1);check(notice().contains("배불러요"));capture(room.window!!,"full-refusal")}
        ui {
            prepare(plain.copy(sleeping=true))
            val feed=nodes(room.window!!.decorView).filterIsInstance<Button>().single {it.text.toString()=="먹이"}
            check(feed.isEnabled);feed.performClick();check(notice().contains("자고 있어요"));check(state().meals==0)
            capture(room.window!!,"sleeping-refusal")
        }
        for((s,message)in listOf(plain.copy(hatched=false) to "아직 알",plain.copy(dead=true,diedAt=now) to "먹을 수 없어요")) {
            pickMeal(s);ui{check(notice().contains(message));check(state().meals==0)}
        }
        ui{AppBrightness.set(c,-1);check(home.window.attributes.screenBrightness==-1f);check(room.window!!.attributes.screenBrightness==-1f);room.dismiss();call(home,"showBrightnessSettings")}
        await("System brightness dialog"){(field(home,"controls").get(home) as? BrightnessDialog)?.window?.decorView?.hasWindowFocus()==true}
        ui{val d=field(home,"controls").get(home) as BrightnessDialog;check(d.window!!.attributes.screenBrightness==-1f);check(d.listView.checkedItemPosition==0);capture(d.window!!,"system-restored");d.dismiss()}
        check(android.provider.Settings.System.getInt(c.contentResolver,"screen_brightness",-1)==systemBrightness)
        check(android.provider.Settings.System.getInt(c.contentResolver,"screen_brightness_mode",-1)==systemMode)
        result.putString("stream","PASS: brightness settings KO/JA/EN, wheel selection, prefs/recreation, system restoration, activity/dialog consistency, feeding at 70/recent meal, full and sleeping messages, egg/dead guards; disposable unpaired emulator, no microphone/camera capture/live Muse turn.")
    }
}

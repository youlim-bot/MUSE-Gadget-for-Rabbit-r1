package dev.cameronpak.muser1

import android.app.Instrumentation
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.SystemClock
import java.io.File
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Explicit opt-in check: public landmark coordinates only, no microphone or Muse session. */
internal object EmbeddedMapCheck {
    @Suppress("DEPRECATION")
    fun run(test:Instrumentation,result:Bundle){
        val app=test.startActivitySync(Intent(test.targetContext,NavigationActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as NavigationActivity
        fun field(name:String)=NavigationActivity::class.java.getDeclaredField(name).apply{isAccessible=true}
        fun method(name:String,vararg types:Class<*>)=NavigationActivity::class.java.getDeclaredMethod(name,*types).apply{isAccessible=true}
        val synthetic=Location("public-landmark-fixture").apply{latitude=35.681236;longitude=139.767125;accuracy=5f;elapsedRealtimeNanos=SystemClock.elapsedRealtimeNanos();time=System.currentTimeMillis()}
        try{
            test.runOnMainSync{app.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);method("stopLocation").invoke(app);field("fix").set(app,synthetic)}
            fun await(timeout:Long=30000,predicate:()->Boolean){val end=SystemClock.uptimeMillis()+timeout;while(SystemClock.uptimeMillis()<end){if(predicate())return;Thread.sleep(200)};error("Map fixture timed out")}
            await{var ready=false;test.runOnMainSync{ready=field("pageReady").getBoolean(app)};ready}
            val canvas=field("map").get(app) as GoogleMapCanvas
            test.runOnMainSync{method("stopLocation").invoke(app);field("fix").set(app,synthetic);method("updatePosition").invoke(app);method("requestRoute",MapPoint::class.java).invoke(app,MapPoint(35.67696,139.7634))}
            await{var ready=false;test.runOnMainSync{ready=field("route").get(app)!=null};ready}
            await(45000){var drawn=false;test.runOnMainSync{drawn=canvas.loaded&&canvas.routePointCount>0};drawn}
            val places=Geocoder(test.targetContext,Locale.JAPAN).getFromLocationName("東京駅",3).orEmpty()
            check(places.isNotEmpty()){"Android geocoder unavailable"}
            test.runOnMainSync{
                val fix=field("fix").get(app) as Location
                check(fix.provider=="public-landmark-fixture"){"Fixture position changed; refusing capture"}
                (field("destination").get(app) as android.widget.EditText).setText("DEMO · 東京国際フォーラム")
            }
            test.runOnMainSync{(field("status").get(app) as android.widget.TextView).text="DEMO · Public landmark route"}
            test.waitForIdleSync()
            Thread.sleep(750)
            fun capture(name:String){
                var safe=false;test.runOnMainSync{safe=(field("fix").get(app) as Location).provider=="public-landmark-fixture"};check(safe)
                val bitmap=test.uiAutomation.takeScreenshot()?:error("No screenshot")
                File(test.targetContext.cacheDir,name).outputStream().use{bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};bitmap.recycle()
            }
            capture("embedded-map-fixture.png")
            Thread.sleep(1600)
            test.runOnMainSync{method("selectMode",TravelMode::class.java).invoke(app,TravelMode.DRIVE);check(field("route").get(app)==null)}
            await{var ready=false;test.runOnMainSync{ready=field("route").get(app)!=null && field("mode").get(app)==TravelMode.DRIVE};ready}
            test.waitForIdleSync();Thread.sleep(750);capture("google-drive-fixture.png")
            check(File(test.targetContext.noBackupFilesDir,"google-routes.enc").isFile)
            check(!File(test.targetContext.filesDir,"pending-google-routes-key").exists())
            result.putString("stream","PASS: native Google map, WALK and DRIVE Google Routes responses rendered; mode switch cleared old route; native geocoder returned results; route key encrypted and provisioning file absent. Synthetic public landmarks only; no Muse message/audio. Outdoor movement not tested.")
        }finally{test.runOnMainSync{app.finish()}}
    }
}

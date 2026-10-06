package dev.cameronpak.muser1

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

internal class DeskClockDialog(host: Activity, private val battery: () -> Int, onClose: () -> Unit) : Dialog(host, R.style.Theme_Muse) {
    private val weatherScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val weather = TextView(host).apply { textSize=17f; setTextColor(Color.WHITE); gravity=Gravity.CENTER }
    private val metrics = TextView(host).apply { textSize=14f; setTextColor(Color.LTGRAY); gravity=Gravity.CENTER; visibility=android.view.View.GONE }
    private val source = TextView(host).apply { textSize=11f; setTextColor(Color.GRAY); gravity=Gravity.CENTER }
    private val weatherHost = host
    private fun refreshWeather() {
        weatherScope.launch {
            while(isShowing) {
                if(!BuildConfig.DEMO && !ClockWeather.permitted(context)) {
                    weather.text=UiText.text(context,"날씨 · 터치하여 위치 허용","天気 · タップして位置を許可","Weather · tap to allow location")
                    source.text=UiText.text(context,"날씨 조회에 대략적인 위치를 사용합니다","天気の取得におおよその位置を使用","Uses approximate location for weather")
                    break
                }
                metrics.visibility=android.view.View.GONE
                weather.contentDescription=null
                weather.text=UiText.text(context,"현재 지역 날씨 확인 중…","現在地の天気を取得中…","Loading local weather…")
                try {
                    val result=ClockWeather.load(context)
                    val caption="${result.place}\n${ClockWeather.icon(result.code,result.isDay)}  ${result.temperature}°C"
                    weather.text=android.text.SpannableString(caption).apply {
                        setSpan(android.text.style.RelativeSizeSpan(1.6f),caption.indexOf('\n')+1,length,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                    }
                    weather.contentDescription="${result.place} · ${result.temperature}°C · ${ClockWeather.description(context,result.code)}"
                    val humidity=result.humidity?.let { "$it%" } ?: "—"
                    val uv=result.uv?.let { String.format(java.util.Locale.US,"%.1f",it) } ?: "—"
                    metrics.text=UiText.text(context,"습도 $humidity  ·  자외선 $uv","湿度 $humidity  ·  UV $uv","Humidity $humidity  ·  UV $uv")
                    metrics.visibility=android.view.View.VISIBLE
                    source.text=if(BuildConfig.DEMO) "OFFLINE DEMO · SAMPLE DATA" else UiText.text(context,"UV 시간대 예보 · ","UV 時間別予報 · ","UV hourly forecast · ") + (if(BuildConfig.DEMO) "DEMO · " else "Open-Meteo · ") + LocalDateTime.now().minusSeconds((android.os.SystemClock.elapsedRealtime()-result.fetched)/1000).format(DateTimeFormatter.ofPattern("HH:mm"))
                } catch(e: Exception) {
                    if(!currentCoroutineContext().isActive) throw e
                    weather.text=UiText.text(context,"위치·날씨를 확인할 수 없습니다","位置・天気を取得できません","Location / weather unavailable")
                    source.text=UiText.text(context,"위치 설정과 인터넷 연결을 확인하세요","位置設定とインターネットを確認","Check location settings and internet")
                }
                delay(15*60_000L)
            }
        }
    }
    private val handler = Handler(Looper.getMainLooper())
    private val time = TextView(host).apply { textSize = 62f; setTextColor(Color.rgb(255,139,66)); gravity = Gravity.CENTER }
    private val date = TextView(host).apply { textSize = 18f; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER }
    private val detail = TextView(host).apply { textSize = 15f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
    private val charge = TextView(host).apply { textSize = 13f; setTextColor(Color.GRAY); gravity = Gravity.CENTER }
    private val tick = object : Runnable {
        override fun run() {
            val now = if(BuildConfig.DEMO) LocalDateTime.of(2026,10,6,10,30) else LocalDateTime.now()
            time.text = now.format(DateTimeFormatter.ofPattern("HH:mm"))
            date.text = now.format(DateTimeFormatter.ofPattern("yyyy.MM.dd EEEE", ClockWeather.locale(context)))
            detail.text = ClockHomeStatus.text(context)
            charge.text = UiText.text(context,"충전 중", "充電中", "Charging") + " · ${battery()}%"
            handler.postDelayed(this,1000)
        }
    }
    init {
        val pad = (24 * host.resources.displayMetrics.density).toInt()
        val root = LinearLayout(host).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setPadding(pad,pad,pad,pad); setBackgroundColor(Color.rgb(8,10,9))
            setOnClickListener { dismiss() }
        }
        for (view in listOf(date,time,weather,metrics,source,charge,detail)) root.addView(view,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin = pad/2 })
        root.addView(TextView(host).apply {
            text = UiText.text(host,"터치하여 대화로", "タップして会話へ", "Tap to return to conversation")
            textSize = 13f; setTextColor(Color.GRAY); gravity = Gravity.CENTER
        })
        weather.setOnClickListener {
            if(!BuildConfig.DEMO && !ClockWeather.permitted(context)) ClockWeather.request(weatherHost)
            else { weatherScope.coroutineContext.cancelChildren(); refreshWeather() }
        }
        setContentView(root)
        setOnDismissListener { handler.removeCallbacks(tick); weatherScope.cancel(); onClose() }
    }
    override fun show() {
        super.show()
        window?.apply {
            setLayout(-1,-1)
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_FULLSCREEN)
            decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
        }
        handler.post(tick)
        refreshWeather()
    }
}

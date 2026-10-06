package dev.cameronpak.muser1

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.widget.*

/** Foreground-only position check. Never logs, persists, or sends location to Muse. */
class NavigationActivity : Activity() {
    private val manager by lazy { getSystemService(LocationManager::class.java) }
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    private lateinit var destination: EditText
    private var fix: Location? = null
    private var asked = false
    private var active = false
    private var obtaining = false
    private fun t(ko: String, ja: String, en: String) = UiText.text(this,ko,ja,en)
    private fun dp(n: Int) = (n*resources.displayMetrics.density).toInt()
    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) { accept(location) }
        override fun onProviderDisabled(provider: String) { if (!manager.isLocationEnabled) { stopFix(); status.text=t("위치 기능을 켜 주세요.","位置情報を有効にしてください。","Enable device location.") } }
        override fun onProviderEnabled(provider: String) = Unit
        @Deprecated("Legacy location callback")
        override fun onStatusChanged(provider: String?, state: Int, extras: Bundle?) = Unit
    }
    private val timeout = Runnable {
        stopFix()
        status.text=t("위치를 아직 확인하지 못했습니다. 창가나 실외에서 다시 확인해 주세요.","現在地を取得できません。窓際や屋外で再試行してください。","No accurate fix yet. Retry near a window or outdoors.")
    }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(12),dp(16),dp(8));setBackgroundColor(Color.rgb(10,11,10)) }
        fun label(text: String,size: Float)=TextView(this).apply { this.text=text;textSize=size;setTextColor(Color.rgb(242,233,221));setPadding(0,dp(8),0,dp(8)) }
        fun button(text: String, action: () -> Unit)=Button(this).apply { this.text=text;isAllCaps=false;textSize=14f;setOnClickListener { action() } }
        root.addView(button(t("‹ Muse로 돌아가기","‹ Museに戻る","‹ Back to Muse")) { finish() })
        root.addView(label(t("도보 길찾기","徒歩ナビ","Walking navigation"),21f))
        status=label(t("현재 위치 확인 중…","現在地を取得中…","Finding your location…"),14f);root.addView(status)
        destination=EditText(this).apply {
            hint=t("목적지 이름 또는 주소","目的地の名前・住所","Destination name or address");textSize=16f
            setTextColor(Color.WHITE);setHintTextColor(Color.GRAY);setSingleLine(true)
            filters=arrayOf(android.text.InputFilter.LengthFilter(300))
        };root.addView(destination)
        root.addView(button(t("도보 안내 시작","徒歩ナビを開始","Start walking navigation")) { navigate() })
        root.addView(button(t("현재 위치 다시 확인","現在地を再取得","Refresh current location")) { locate() })
        root.addView(button(t("위치·권한 설정","位置情報・権限設定","Location & permission settings")) {
            startActivity(Intent(if (hasPermission()) Settings.ACTION_LOCATION_SOURCE_SETTINGS else Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                if (!hasPermission()) data=Uri.parse("package:$packageName")
            })
        })
        root.addView(button(t("Google 지도 설치·열기","Google マップをインストール・開く","Install / open Google Maps")) { openMapsStore() })
        root.addView(label(t("시작 위치는 자동으로 확인합니다. 길 안내는 Google 지도가 담당하며 목적지와 현재 위치를 사용합니다. Muse에는 위치를 보내지 않습니다.","出発地は自動取得します。Google マップが目的地と現在地を使って案内します。Museには位置を送りません。","Your starting location is detected automatically. Google Maps uses your destination and location for guidance. Location is not sent to Muse."),12f))
        setContentView(ScrollView(this).apply { addView(root) })
    }
    private fun hasPermission() = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
    override fun onResume() { super.onResume();active=true;locate() }
    override fun onPause() { active=false;stopFix();super.onPause() }
    private fun locate() {
        stopFix();fix=null
        if (!active) return
        if (!hasPermission()) {
            status.text=t("현재 위치를 자동 확인하려면 위치 권한이 필요합니다.","現在地の自動取得には位置情報の権限が必要です。","Allow location access to detect your starting point.")
            if (!asked) { asked=true;requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION),81) }
            return
        }
        if (!manager.isLocationEnabled) { status.text=t("위치 설정에서 위치 기능을 켜 주세요.","設定で位置情報を有効にしてください。","Enable location in device settings.");return }
        status.text=t("현재 위치 확인 중… 최대 45초","現在地を取得中…最大45秒","Finding location… up to 45 seconds")
        obtaining=true;handler.postDelayed(timeout,45_000)
        var subscribed=false
        for (provider in listOf(LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER,"fused")) {
            if (!obtaining) break
            try {
                if (manager.allProviders.contains(provider) && manager.isProviderEnabled(provider)) {
                    manager.requestLocationUpdates(provider,2000L,0f,listener,Looper.getMainLooper());subscribed=true
                    manager.getLastKnownLocation(provider)?.let { accept(it) }
                }
            } catch (_: SecurityException) { } catch (_: IllegalArgumentException) { }
        }
        if (!subscribed && obtaining) { stopFix();status.text=t("사용 가능한 위치 공급자가 없습니다. 위치 권한과 설정을 확인해 주세요.","位置情報の権限と設定を確認してください。","No location provider available. Check permissions and settings.") }
    }
    private fun accept(location: Location) {
        if (!active || !obtaining || !location.hasAccuracy()) return
        val age=(SystemClock.elapsedRealtimeNanos()-location.elapsedRealtimeNanos)/1_000_000
        if (WalkingRoute.usableFix(age,location.accuracy)) {
            fix=Location(location);stopFix()
            status.text=t("현재 위치 확인됨 · 정확도 약 ${location.accuracy.toInt()}m","現在地を確認 · 精度 約${location.accuracy.toInt()}m","Location ready · accuracy about ${location.accuracy.toInt()}m")
        } else status.text=t("더 정확한 위치를 기다리는 중… 정확한 위치 권한을 허용하고 실외에서 확인해 주세요.","より正確な位置を取得中…正確な位置情報を許可し、屋外でお試しください。","Waiting for a more accurate fix. Allow precise location and try outdoors.")
    }
    private fun stopFix() { handler.removeCallbacks(timeout);obtaining=false;try { manager.removeUpdates(listener) } catch (_: SecurityException) { } }
    private fun navigate() {
        val text=destination.text.toString().trim()
        if (text.isEmpty()) { destination.error=t("목적지를 입력하세요","目的地を入力してください","Enter a destination");return }
        val current=fix
        if (current==null || !WalkingRoute.usableFix((SystemClock.elapsedRealtimeNanos()-current.elapsedRealtimeNanos)/1_000_000,current.accuracy)) { locate();return }
        // Google Maps itself tracks the live starting location, so no coordinates enter this URI.
        val intent=Intent(Intent.ACTION_VIEW,Uri.parse(WalkingRoute.navigationUri(text))).setPackage("com.google.android.apps.maps")
        try { startActivity(intent) }
        catch (_: ActivityNotFoundException) { status.text=t("도보 음성 안내를 위해 Google 지도를 설치해 주세요.","徒歩ナビ用にGoogle マップをインストールしてください。","Install Google Maps for walking guidance.");openMapsStore() }
    }
    private fun openMapsStore() {
        try { startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=com.google.android.apps.maps")).setPackage("com.android.vending")) }
        catch (_: ActivityNotFoundException) {
            try { startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.maps"))) }
            catch (_: ActivityNotFoundException) { status.text=t("앱 스토어나 브라우저가 필요합니다.","ストアまたはブラウザが必要です。","An app store or browser is required.") }
        }
    }
    override fun onRequestPermissionsResult(code: Int, permissions: Array<out String>, grants: IntArray) {
        super.onRequestPermissionsResult(code,permissions,grants)
        if (code==81) {
            if (hasPermission()) locate()
            else status.text=t("위치 권한이 거부되었습니다. 권한 설정에서 허용해 주세요.","位置情報の権限が拒否されました。設定で許可してください。","Location permission denied. Enable it in app settings.")
        }
    }
}

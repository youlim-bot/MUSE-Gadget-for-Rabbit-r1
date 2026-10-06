package dev.cameronpak.muser1

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.location.*
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.webkit.*
import android.widget.*
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.ceil

/** An in-app map. Position and routes stay in memory; external services are described in Info. */
class NavigationActivity : Activity() {
    private val manager by lazy { getSystemService(LocationManager::class.java) }
    private val handler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private val client = OkHttpClient.Builder().callTimeout(25,TimeUnit.SECONDS).build()
    private lateinit var map: WebView
    private lateinit var status: TextView
    private lateinit var instruction: TextView
    private lateinit var destination: EditText
    private lateinit var search: Button
    private var fix: Location? = null
    private var selected: MapPoint? = null
    private var route: FootRoute? = null
    private var active=false
    private var pageReady=false
    private var asked=false
    private var busy=false
    private var generation=0
    private var call: Call? = null
    private val searchCache=linkedMapOf<String,List<Pair<String,MapPoint>>>()
    private var deadline: Runnable? = null
    private fun t(ko:String,ja:String,en:String)=UiText.text(this,ko,ja,en)
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    private fun point(l:Location)=MapPoint(l.latitude,l.longitude)
    private fun fresh(l:Location?)=l!=null && l.hasAccuracy() && WalkingRoute.usableFix((SystemClock.elapsedRealtimeNanos()-l.elapsedRealtimeNanos)/1_000_000,l.accuracy)
    private fun label(size:Float)=TextView(this).apply { textSize=size;setTextColor(Color.rgb(237,232,222));gravity=Gravity.CENTER_VERTICAL;setPadding(dp(10),0,dp(10),0) }
    private fun button(text:String, action:()->Unit)=Button(this).apply {
        this.text=text;isAllCaps=false;textSize=12f;setTextColor(Color.rgb(255,166,58));minWidth=0;minimumWidth=0;minHeight=0;minimumHeight=0;setPadding(dp(8),0,dp(8),0)
        background=GradientDrawable().apply { setColor(Color.rgb(28,31,28));cornerRadius=dp(16).toFloat();setStroke(dp(1),Color.rgb(64,65,57)) }
        setOnClickListener { action() }
    }
    private fun row()=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(6),dp(3),dp(6),dp(3)) }
    private fun LinearLayout.item(view:View,weight:Float=1f){addView(view,LinearLayout.LayoutParams(0,dp(34),weight).apply { setMargins(dp(3),0,dp(3),0) })}
    private val listener=object:LocationListener {
        override fun onLocationChanged(location:Location){accept(location)}
        override fun onProviderDisabled(provider:String){if(!manager.isLocationEnabled){fix=null;status.text=t("위치 기능을 켜 주세요","位置情報を有効にしてください","Enable device location");js("notice(${JSONObject.quote(status.text.toString())})")}}
        override fun onProviderEnabled(provider:String){if(active)locate()}
        @Deprecated("Legacy location callback") override fun onStatusChanged(provider:String?,state:Int,extras:Bundle?)=Unit
    }
    private val timeout=Runnable { if(!fresh(fix)){status.text=t("위치 확인 실패 · 실외에서 재시도","現在地を取得できません・屋外で再試行","No GPS fix · retry outdoors");js("notice(${JSONObject.quote(status.text.toString())})");stopLocation()} }
    private val heartbeat=object:Runnable { override fun run(){if(!active)return;if(fix!=null&&!fresh(fix)){status.text=t("위치가 오래됨 · 재확인 중","現在地を再取得中","Location stale · reacquiring");instruction.text=t("위치가 갱신될 때까지 안내를 기다리세요","位置が更新されるまでお待ちください","Wait for a fresh fix before following guidance")};handler.postDelayed(this,5000)} }

    @SuppressLint("SetJavaScriptEnabled") // Only bundled scripts execute; no remote pages or native account bridge.
    override fun onCreate(state:Bundle?){
        super.onCreate(state)
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.rgb(10,13,11))}
        val top=row();top.item(button("‹ Muse"){finish()},.8f);top.item(label(16f).apply{text=t("도보 지도","徒歩マップ","Walking map")},1.6f);top.item(button(t("정보","情報","Info")){info()},.7f);root.addView(top)
        destination=EditText(this).apply{hint=t("목적지 이름·주소","目的地の名前・住所","Destination name/address");textSize=14f;setTextColor(Color.WHITE);setHintTextColor(Color.LTGRAY);setSingleLine(true);filters=arrayOf(android.text.InputFilter.LengthFilter(300));imeOptions=android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH;setOnEditorActionListener{_,_,_->findDestination();true}}
        search=button(t("찾기","検索","Find")){findDestination()}
        val input=row();input.item(destination,3f);input.item(search,.7f);root.addView(input)
        status=label(11f).apply{text=t("현재 위치 확인 중…","現在地を取得中…","Finding your location…")};root.addView(status,LinearLayout.LayoutParams(-1,dp(24)))
        map=WebView(this).apply{
            setBackgroundColor(Color.rgb(21,23,22));settings.javaScriptEnabled=true
            settings.allowFileAccess=false;settings.allowContentAccess=false;settings.domStorageEnabled=false
            settings.mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW;settings.setGeolocationEnabled(false)
            settings.userAgentString=settings.userAgentString+" "+WalkingRoute.USER_AGENT
            CookieManager.getInstance().setAcceptThirdPartyCookies(this,false)
            addJavascriptInterface(object{
                @JavascriptInterface fun choose(lat:Double,lon:Double){
                    val p=runCatching{MapPoint(lat,lon)}.getOrNull()?:return
                    runOnUiThread{if(active&&!busy)AlertDialog.Builder(this@NavigationActivity).setMessage(t("이 지점까지 도보 경로를 표시할까요?","この地点までの徒歩ルートを表示しますか？","Show a walking route to this point?"))
                        .setPositiveButton(t("경로 보기","ルート表示","Show route")){_,_->destination.setText(t("지도에서 선택한 지점","地図で選択した地点","Point selected on map"));requestRoute(p)}.setNegativeButton(UiText.translate(this@NavigationActivity,"취소"),null).show()}
                }
            },"MuseMap")
            webViewClient=object:WebViewClient(){
                override fun shouldInterceptRequest(view:WebView,request:WebResourceRequest):WebResourceResponse?{
                    val u=request.url
                    if(u.scheme=="https"&&u.host=="appassets.androidplatform.net"){
                        val names=setOf("index.html","leaflet.js","leaflet.css")
                        val name=u.lastPathSegment
                        if(u.path=="/map/$name"&&name in names){val mime=when(name){"index.html"->"text/html";"leaflet.js"->"application/javascript";else->"text/css"};return WebResourceResponse(mime,"UTF-8",assets.open("map/$name"))}
                    }
                    if(u.scheme=="https"&&u.host=="tile.openstreetmap.org"&&Regex("/\\d+/\\d+/\\d+\\.png").matches(u.path?:""))return null
                    return WebResourceResponse("text/plain","UTF-8",ByteArrayInputStream(ByteArray(0)))
                }
                override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest):Boolean{
                    val u=request.url
                    if(request.hasGesture()&&u.scheme=="https"&&u.host in setOf("www.openstreetmap.org","routing.openstreetmap.de"))runCatching{startActivity(Intent(Intent.ACTION_VIEW,u))}
                    return true
                }
                override fun onPageFinished(view:WebView,url:String){
                    pageReady=true;js("window.tileError=${JSONObject.quote(t("지도 연결 실패 · 인터넷 확인","地図に接続できません","Map connection failed · check internet"))};notice(${JSONObject.quote(t("현재 위치를 기다리는 중","現在地を取得中","Waiting for location"))})")
                    if(fresh(fix))updatePosition()
                    route?.let{draw(it)}
                }
            }
            loadUrl("https://appassets.androidplatform.net/map/index.html")
        }
        root.addView(map,LinearLayout.LayoutParams(-1,0,1f))
        instruction=label(13f).apply{maxLines=2;text=t("목적지 검색 또는 지도를 길게 누르세요","目的地を検索、または地図を長押し","Search a destination or hold a point on the map");setOnClickListener{showSteps()}}
        root.addView(instruction,LinearLayout.LayoutParams(-1,dp(48)))
        val controls=row();controls.item(button(t("내 위치","現在地","Locate")){if(fresh(fix))js("center()")else locate()});controls.item(button(t("전체 경로","全ルート","Overview")){route?.let{draw(it)}});controls.item(button(t("재탐색","再検索","Reroute")){selected?.let{requestRoute(it)}?:findDestination()});root.addView(controls)
        setContentView(root)
    }
    private fun js(code:String){if(pageReady&&!isDestroyed)map.evaluateJavascript(code,null)}
    private fun updatePosition(){val l=fix?:return;js("position(${l.latitude},${l.longitude},${l.accuracy})");updateGuidance()}
    private fun draw(r:FootRoute){
        val points=JSONArray();r.points.forEach{points.put(JSONArray().put(it.lat).put(it.lon))};val end=r.points.last()
        js("drawRoute($points,[${end.lat},${end.lon}])")
    }
    override fun onResume(){super.onResume();active=true;map.onResume();locate();handler.post(heartbeat)}
    override fun onPause(){active=false;stopLocation();handler.removeCallbacks(heartbeat);cancelRequest();map.onPause();super.onPause()}
    override fun onDestroy(){worker.shutdownNow();map.removeJavascriptInterface("MuseMap");map.destroy();super.onDestroy()}
    private fun permitted()=checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED||checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED
    private fun locate(){
        stopLocation();fix=null
        if(!active)return
        if(!permitted()){
            status.text=t("위치 권한 필요 · 정보에서 설정","位置情報の権限が必要・情報から設定","Location permission needed · see Info")
            if(!asked){asked=true;requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION),81)};return
        }
        if(!manager.isLocationEnabled){status.text=t("위치 기능 꺼짐 · 정보에서 설정","位置情報がオフ・情報から設定","Location is off · see Info");return}
        status.text=t("현재 위치 확인 중…","現在地を取得中…","Finding your location…")
        handler.postDelayed(timeout,45000)
        for(provider in listOf(LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER,"fused"))try{
            if(manager.allProviders.contains(provider)&&manager.isProviderEnabled(provider)){
                manager.requestLocationUpdates(provider,3000L,3f,listener,Looper.getMainLooper())
                manager.getLastKnownLocation(provider)?.let{accept(it)}
            }
        }catch(_:SecurityException){}catch(_:IllegalArgumentException){}
    }
    private fun accept(location:Location){
        if(!active||!fresh(location))return
        val old=fix
        if(old!=null&&location.elapsedRealtimeNanos<old.elapsedRealtimeNanos)return
        fix=Location(location);handler.removeCallbacks(timeout)
        status.text=t("현재 위치 · 정확도 약 ${location.accuracy.toInt()}m","現在地・精度 約${location.accuracy.toInt()}m","Current location · accuracy ~${location.accuracy.toInt()}m")
        updatePosition()
    }
    private fun stopLocation(){handler.removeCallbacks(timeout);try{manager.removeUpdates(listener)}catch(_:SecurityException){}}
    override fun onRequestPermissionsResult(code:Int,permissions:Array<out String>,grants:IntArray){super.onRequestPermissionsResult(code,permissions,grants);if(code==81)locate()}
    private fun cancelRequest(){generation++;call?.cancel();call=null;deadline?.let{handler.removeCallbacks(it)};deadline=null;busy=false;if(::search.isInitialized)search.isEnabled=true}
    private fun begin():Int{
        cancelRequest();busy=true;search.isEnabled=false;val token=generation
        deadline=Runnable{if(generation==token){cancelRequest();instruction.text=t("연결 시간 초과 · 다시 시도하세요","接続タイムアウト・再試行してください","Connection timed out · retry")}}
        handler.postDelayed(deadline!!,26000);return token
    }
    private fun complete(token:Int,action:()->Unit){handler.post{if(active&&generation==token){cancelRequest();action()}}}
    @Suppress("DEPRECATION") // API 29–32 compatibility; blocking geocoding is confined to the worker.
    private fun findDestination(){
        if(busy)return
        val query=destination.text.toString().trim();if(query.isEmpty()){destination.error=t("목적지를 입력하세요","目的地を入力してください","Enter a destination");return}
        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(destination.windowToken,0);destination.clearFocus()
        if(!fresh(fix)){locate();instruction.text=t("현재 위치 확인 후 다시 찾아 주세요","現在地の取得後、再検索してください","Search again once your location is ready");return}
        searchCache[query]?.let{chooseResults(it);return}
        if(!Geocoder.isPresent()){instruction.text=t("검색 서비스 없음 · 지도를 길게 눌러 선택","検索サービスなし・地図を長押しで選択","No search provider · hold a map point instead");return}
        val token=begin();instruction.text=t("목적지 검색 중…","目的地を検索中…","Searching destination…")
        val locale=when(UiText.language(this)){DisplayLanguage.KO->Locale.KOREA;DisplayLanguage.JA->Locale.JAPAN;else->Locale.ENGLISH}
        worker.execute{
            val results=runCatching{Geocoder(this,locale).getFromLocationName(query,5).orEmpty().filter{it.hasLatitude()&&it.hasLongitude()}.map{(it.getAddressLine(0)?:it.featureName?:query) to MapPoint(it.latitude,it.longitude)}.distinctBy{it.second}}.getOrNull()
            complete(token){if(results.isNullOrEmpty())instruction.text=t("검색 결과 없음 · 주소 입력 또는 지도 길게 누르기","見つかりません・住所入力または地図を長押し","No results · use an address or hold a map point")else{if(searchCache.size>=20)searchCache.remove(searchCache.keys.first());searchCache[query]=results;chooseResults(results)}}
        }
    }
    private fun chooseResults(results:List<Pair<String,MapPoint>>){
        instruction.text=t("목적지를 선택하세요","目的地を選択してください","Choose your destination")
        AlertDialog.Builder(this).setTitle(t("목적지 선택","目的地を選択","Choose destination")).setItems(results.map{it.first}.toTypedArray()){_,i->destination.setText(results[i].first.take(300));requestRoute(results[i].second)}.setNegativeButton(UiText.translate(this,"취소"),null).show()
    }
    private fun requestRoute(end:MapPoint){
        if(busy)return
        val origin=fix
        if(!fresh(origin)){locate();instruction.text=t("현재 위치 확인 후 재탐색을 누르세요","現在地取得後、再検索してください","Wait for location, then tap Reroute");selected=end;return}
        val now=SystemClock.elapsedRealtime()
        synchronized(rateLock){if(now-lastRouteRequest<1500){instruction.text=t("잠시 후 다시 시도하세요","少し待って再試行してください","Please wait a moment and retry");return};lastRouteRequest=now}
        selected=end;route=null;js("clearRoute()")
        val token=begin();instruction.text=t("도보 경로 계산 중…","徒歩ルートを計算中…","Calculating walking route…")
        val request=Request.Builder().url(WalkingRoute.routeUrl(point(origin!!),end)).header("User-Agent",WalkingRoute.USER_AGENT).build()
        val pending=client.newCall(request);call=pending
        worker.execute{
            val result=runCatching{pending.execute().use{response->check(response.isSuccessful);val body=response.body?:error("Empty response");check(body.contentLength()<=4_000_000);// Bound the response in memory; never log URLs or bodies.
                val buffer=java.io.ByteArrayOutputStream();val chunk=ByteArray(8192);body.byteStream().use{input->while(true){val n=input.read(chunk);if(n<0)break;check(buffer.size()+n<=4_000_000);buffer.write(chunk,0,n)}};WalkingRoute.parse(buffer.toString("UTF-8"))}}
            complete(token){result.onSuccess{route=it;draw(it);updateGuidance()}.onFailure{instruction.text=t("도보 경로를 찾지 못했습니다 · 재탐색","徒歩ルートを取得できません・再検索","Walking route unavailable · retry")}}
        }
    }
    private fun measure(m:Double)=if(m<1000)"${ceil(m/10).toInt()*10} m" else String.format(Locale.US,"%.1f km",m/1000)
    private fun stepText(s:WalkingStep):String{
        val action=when(s.type){
            "arrive"->t("목적지 도착","目的地に到着","Arrive at destination")
            "depart"->t("출발","出発","Start walking")
            "roundabout","rotary"->t("회전교차로 통과","ラウンドアバウトへ","Enter roundabout")
            else->when{ s.modifier.contains("left")->t("↰ 왼쪽으로","↰ 左へ","↰ Turn left");s.modifier.contains("right")->t("↱ 오른쪽으로","↱ 右へ","↱ Turn right");s.modifier=="uturn"->t("↶ 돌아가기","↶ 引き返す","↶ Turn around");else->t("↑ 계속 직진","↑ 直進","↑ Continue ahead") }
        }
        return action+if(s.name.isNotBlank())" · ${s.name}" else ""
    }
    private fun updateGuidance(){
        val r=route?:return;val l=fix?:return;if(!fresh(l))return
        val progress=WalkingRoute.progress(r,point(l))
        instruction.text=when{
            progress.offRoute>maxOf(60.0,l.accuracy.toDouble()*2)->t("경로에서 벗어남 · 재탐색을 누르세요","ルートから外れました・再検索","Off route · tap Reroute")
            progress.remaining<35 && WalkingRoute.distance(point(l),selected?:r.points.last())<35 && l.accuracy<=40 ->t("목적지 근처입니다","目的地付近です","Near your destination")
            else->{val minutes=ceil(r.seconds*(progress.remaining/r.meters.coerceAtLeast(1.0))/60).toInt().coerceAtLeast(1);val next=progress.next
                t("남은 ${measure(progress.remaining)} · 약 ${minutes}분","残り ${measure(progress.remaining)}・約${minutes}分","${measure(progress.remaining)} left · ~${minutes} min")+"\n"+(if(next!=null)"${measure(progress.toNext)} · ${stepText(next)}" else t("목적지 방향으로 이동","目的地へ進む","Continue toward destination"))}
        }
    }
    private fun showSteps(){route?.let{r->AlertDialog.Builder(this).setTitle(t("도보 경로 안내","徒歩ルート案内","Walking directions")).setItems(r.steps.map{stepText(it)}.toTypedArray(),null).setPositiveButton(UiText.translate(this,"닫기"),null).show()}}
    private fun info(){AlertDialog.Builder(this).setTitle(t("Muse 도보 지도","Muse 徒歩マップ","Muse walking map")).setMessage(t(
        "지도: OpenStreetMap · 경로: FOSSGIS/OSRM. 검색어는 Android 검색 제공자에게, 경로 요청 시 출발·목적지 좌표는 FOSSGIS에 전달됩니다. 지도 서비스는 표시 지역과 IP를 받습니다. 서비스 로그가 남을 수 있습니다. Muse AI에는 보내지 않습니다. 화면을 벗어나면 위치 추적을 멈춥니다.\n\n지도 길게 누르기로 목적지를 선택할 수 있습니다. 화면 안내 전용이며 음성·백그라운드 안내와 자동 재탐색은 없습니다.",
        "地図: OpenStreetMap・ルート: FOSSGIS/OSRM。検索語はAndroid検索プロバイダーへ、ルート要求時の出発地・目的地座標はFOSSGISへ送信します。地図サービスは表示地域とIPを受け取り、ログを保存する場合があります。Muse AIには送りません。画面を離れると位置取得を停止します。\n\n地図を長押しして目的地を選べます。画面案内のみ。音声・バックグラウンド案内、自動再検索はありません。",
        "Map: OpenStreetMap · routes: FOSSGIS/OSRM. Search text goes to the Android geocoder; route requests send start/destination coordinates to FOSSGIS. Map services receive the viewed area and IP and may retain logs. Nothing is sent to Muse AI. Tracking stops when you leave this screen.\n\nHold a map point to choose it. Visual guidance only; no voice, background navigation or automatic rerouting."))
        .setPositiveButton(UiText.translate(this,"닫기"),null).setNeutralButton(t("위치 설정","位置設定","Location settings")){_,_->startActivity(Intent(if(permitted())Settings.ACTION_LOCATION_SOURCE_SETTINGS else Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply{if(!permitted())data=Uri.parse("package:$packageName")} )}.show()}
    companion object { private val rateLock=Any();private var lastRouteRequest=Long.MIN_VALUE/2 }
}

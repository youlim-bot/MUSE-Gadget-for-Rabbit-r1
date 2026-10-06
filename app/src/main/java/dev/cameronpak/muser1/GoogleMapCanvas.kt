package dev.cameronpak.muser1

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*

/** Native SDK map. No credentials or geographic data enter a WebView. */
internal class GoogleMapCanvas(context:Context):FrameLayout(context) {
    private var view:MapView?=null
    private var google:GoogleMap?=null
    private var dot:Circle?=null
    private var accuracy:Circle?=null
    private var path:Polyline?=null
    private var target:Circle?=null
    private var following=true
    private var last:LatLng?=null
    var loaded=false; private set
    var routePointCount=0; private set
    var ready:(()->Unit)?=null
    var choose:((MapPoint)->Unit)?=null
    private val hint=TextView(context).apply { setTextColor(Color.WHITE);setBackgroundColor(0xcc151716.toInt());textSize=12f;gravity=Gravity.CENTER;setPadding(12,8,12,8) }
    @Suppress("DEPRECATION")
    fun create(state:Bundle?){
        val key=context.packageManager.getApplicationInfo(context.packageName,PackageManager.GET_META_DATA).metaData?.getString("com.google.android.geo.API_KEY").orEmpty()
        if(key.isBlank()){addView(hint,LayoutParams(-1,-1));notice(UiText.text(context,"Google 지도 키를 등록해 주세요","Google マップのキーを設定してください","Configure a Google Maps key"));return}
        view=MapView(context).also { addView(it,LayoutParams(-1,-1));it.onCreate(state) }
        addView(hint,LayoutParams(-1,-2,Gravity.TOP));notice(UiText.text(context,"Google 지도 불러오는 중…","Google マップを読み込み中…","Loading Google Maps…"))
        view!!.getMapAsync { m ->
            google=m;m.uiSettings.isMapToolbarEnabled=false;m.uiSettings.isZoomControlsEnabled=true
            m.setOnCameraMoveStartedListener { if(it==GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE)following=false }
            m.setOnMapLongClickListener { choose?.invoke(MapPoint(it.latitude,it.longitude)) }
            m.setOnMapLoadedCallback { loaded=true;notice("") }
            ready?.invoke()
        }
    }
    fun notice(text:String){hint.text=text;hint.visibility=if(text.isEmpty())GONE else VISIBLE}
    fun position(p:MapPoint,meters:Float){
        val g=google?:return;val ll=LatLng(p.lat,p.lon);val first=last==null;last=ll
        if(dot==null){accuracy=g.addCircle(CircleOptions().center(ll).radius(meters.toDouble()).strokeWidth(1f).strokeColor(0x664285f4).fillColor(0x224285f4));dot=g.addCircle(CircleOptions().center(ll).radius(4.0).strokeWidth(3f).strokeColor(Color.WHITE).fillColor(0xff4285f4.toInt()).zIndex(3f))}
        else{dot!!.center=ll;accuracy!!.center=ll;accuracy!!.radius=meters.toDouble()}
        if(first||following)g.moveCamera(CameraUpdateFactory.newLatLngZoom(ll,if(first)16f else g.cameraPosition.zoom))
    }
    fun center(){following=true;last?.let{google?.animateCamera(CameraUpdateFactory.newLatLngZoom(it,17f))}}
    fun draw(route:FootRoute){
        val g=google?:return;clearRoute();following=false
        val points=route.points.map{LatLng(it.lat,it.lon)}
        path=g.addPolyline(PolylineOptions().addAll(points).color(0xffff981f.toInt()).width(9f));routePointCount=points.size
        target=g.addCircle(CircleOptions().center(points.last()).radius(6.0).fillColor(0xffff981f.toInt()).strokeColor(Color.BLACK).strokeWidth(2f))
        val bounds=LatLngBounds.builder();points.forEach{bounds.include(it)}
        post { if(width>0&&height>0)g.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(),width,height,40)) }
    }
    fun clearRoute(){path?.remove();target?.remove();path=null;target=null;routePointCount=0}
    fun start(){view?.onStart()};fun resume(){view?.onResume()};fun pause(){view?.onPause()};fun stop(){view?.onStop()};fun destroy(){view?.onDestroy()};fun lowMemory(){view?.onLowMemory()}
    fun save(state:Bundle){view?.onSaveInstanceState(state)}
}

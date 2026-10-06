package dev.cameronpak.muser1

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.view.View

/** Read-only foreground status. Never reads or stores SSIDs, phone numbers or SIM identifiers. */
internal class NetworkStatusView(context: Context): View(context) {
    private val handler=Handler(Looper.getMainLooper())
    private val ink=Paint(Paint.ANTI_ALIAS_FLAG)
    private var label="—"
    private var wifi=false
    private var level:Int?=null
    private var online=false
    private var running=false
    private val poll=object:Runnable { override fun run(){if(running){refresh();handler.postDelayed(this,5000)}} }
    fun showDemo(){stop();label="Wi-Fi";wifi=true;level=3;online=true;contentDescription="Wi-Fi · fictional demo signal 3/4";invalidate()}
    fun start(){stop();running=true;handler.post(poll)}
    fun stop(){running=false;handler.removeCallbacks(poll)}
    @Suppress("DEPRECATION", "MissingPermission")
    private fun refresh(){
        val cm=context.getSystemService(ConnectivityManager::class.java)
        val caps=runCatching{cm.getNetworkCapabilities(cm.activeNetwork)}.getOrNull()
        wifi=false;level=null;online=caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)==true
        label=when {
            caps==null -> UiText.text(context,"연결 없음","未接続","Offline")
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                wifi=true
                val rssi=(caps.transportInfo as? WifiInfo)?.rssi
                    ?: runCatching{context.applicationContext.getSystemService(WifiManager::class.java).connectionInfo.rssi}.getOrNull()
                level=rssi?.takeIf{it in -126..-1}?.let{WifiManager.calculateSignalLevel(it,5)}
                "Wi-Fi"
            }
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                val base=context.getSystemService(TelephonyManager::class.java)
                val sub=if(android.os.Build.VERSION.SDK_INT>=30) SubscriptionManager.getActiveDataSubscriptionId() else SubscriptionManager.getDefaultDataSubscriptionId()
                val tm=if(SubscriptionManager.isValidSubscriptionId(sub))base.createForSubscriptionId(sub) else base
                level=runCatching{tm.signalStrength?.level?.takeIf{it in 0..4}}.getOrNull()
                when(runCatching{tm.dataNetworkType}.getOrNull()){
                    TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
                    TelephonyManager.NETWORK_TYPE_NR -> "5G"
                    TelephonyManager.NETWORK_TYPE_UMTS,TelephonyManager.NETWORK_TYPE_HSDPA,TelephonyManager.NETWORK_TYPE_HSUPA,TelephonyManager.NETWORK_TYPE_HSPA,TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"
                    TelephonyManager.NETWORK_TYPE_GPRS,TelephonyManager.NETWORK_TYPE_EDGE -> "2G"
                    else -> UiText.text(context,"모바일","モバイル","Mobile")
                }
            }
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "LAN"
            else -> UiText.text(context,"네트워크","ネットワーク","Network")
        }
        contentDescription=label+" · "+(level?.let{UiText.text(context,"신호 $it/4","電波 $it/4","Signal $it/4")} ?: UiText.text(context,"신호 정보 없음","電波情報なし","Signal unavailable"))+" · "+UiText.text(context,if(online)"인터넷 연결됨" else "인터넷 미확인",if(online)"インターネット接続" else "インターネット未確認",if(online)"Internet available" else "Internet unconfirmed")
        invalidate()
    }
    override fun onDetachedFromWindow(){stop();super.onDetachedFromWindow()}
    override fun onDraw(canvas:Canvas){
        super.onDraw(canvas)
        val d=resources.displayMetrics.density;val cy=height/2f
        ink.strokeWidth=1.5f*d;ink.strokeCap=Paint.Cap.ROUND
        val active=Color.rgb(165,173,168);val dim=Color.rgb(62,69,65)
        if(wifi){
            ink.style=Paint.Style.STROKE
            for(i in 1..3){val radius=(3+i*3)*d;ink.color=if((level ?: -1)>=i+1)active else dim
                canvas.drawArc(10*d-radius,cy+5*d-radius,10*d+radius,cy+5*d+radius,225f,90f,false,ink)}
            ink.style=Paint.Style.FILL;ink.color=if((level ?: -1)>0)active else dim;canvas.drawCircle(10*d,cy+5*d,1.4f*d,ink)
        }else{
            ink.style=Paint.Style.FILL
            for(i in 0..3){ink.color=if((level ?: -1)>i)active else dim;canvas.drawRoundRect((2+i*4)*d,cy+(5-(i+1)*2.5f)*d,(4.5f+i*4)*d,cy+5*d,d/2,d/2,ink)}
        }
        ink.color=active;ink.style=Paint.Style.FILL;ink.textSize=11*resources.displayMetrics.scaledDensity
        val text=label+if(!online) " !" else if(level==null && (wifi || label in listOf("LTE","5G","3G","2G","Mobile","모바일","モバイル"))) " ?" else ""
        canvas.drawText(text,24*d,cy-(ink.ascent()+ink.descent())/2,ink)
    }
}

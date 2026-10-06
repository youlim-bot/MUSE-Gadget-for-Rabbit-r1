package dev.cameronpak.muser1

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

/** Muse-owned settings directory. Android owns Wi-Fi credentials and the connection panel. */
internal class MuseSettingsDialog(
    context:Context,
    private val entries:List<Entry>,
    private val wifiSummary:()->String,
    private val museSummary:()->String
):Dialog(context,R.style.Theme_Muse) {
    data class Entry(val id:String,val title:String,val subtitle:String,val section:String,val action:()->Unit)
    private val handler=Handler(Looper.getMainLooper())
    private var wifiText:TextView?=null
    private var museText:TextView?=null
    private val ticker=object:Runnable {override fun run(){refreshStatus();if(isShowing && window?.decorView?.hasWindowFocus()==true)handler.postDelayed(this,3000)}}
    private fun dp(v:Int)=(v*context.resources.displayMetrics.density).toInt()
    private fun t(ko:String,ja:String,en:String)=UiText.text(context,ko,ja,en)
    private fun text(size:Float,color:Int)=TextView(context).apply{textSize=size;setTextColor(color);includeFontPadding=false}
    private val orange=Color.rgb(255,139,66)
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        window?.decorView?.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        val root=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(6),dp(16),dp(8));setBackgroundColor(Color.rgb(10,11,10))}
        val head=LinearLayout(context).apply{gravity=Gravity.CENTER_VERTICAL}
        head.addView(text(22f,Color.WHITE).apply{this.text=t("설정","設定","Settings");typeface=Typeface.DEFAULT_BOLD},LinearLayout.LayoutParams(0,-2,1f))
        head.addView(Button(context).apply{text=t("닫기","閉じる","Close");isAllCaps=false;textSize=13f;setTextColor(orange);setPadding(0,0,0,0);minimumWidth=0;minWidth=0;setOnClickListener{dismiss()}},LinearLayout.LayoutParams(dp(64),dp(48)))
        root.addView(head,LinearLayout.LayoutParams(-1,dp(52)))
        val rows=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL}
        var section=""
        entries.forEach { entry->
            if(entry.section!=section){section=entry.section;rows.addView(text(11f,orange).apply{text=section;setPadding(dp(3),dp(13),0,dp(7))},LinearLayout.LayoutParams(-1,-2))}
            val row=LinearLayout(context).apply{
                orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),dp(9),dp(10),dp(9));minimumHeight=dp(60)
                background=GradientDrawable().apply{setColor(Color.rgb(23,25,23));cornerRadius=dp(12).toFloat()}
                tag="settings_${entry.id}";isClickable=true;isFocusable=true;setOnClickListener{entry.action()}
            }
            val labels=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL}
            labels.addView(text(15f,Color.rgb(242,233,221)).apply{text=entry.title;maxLines=2},LinearLayout.LayoutParams(-1,-2))
            val detail=text(11f,Color.rgb(161,170,162)).apply{text=entry.subtitle;setPadding(0,dp(4),0,0);maxLines=3}
            labels.addView(detail,LinearLayout.LayoutParams(-1,-2))
            if(entry.id=="wifi")wifiText=detail
            if(entry.id=="device")museText=detail
            row.addView(labels,LinearLayout.LayoutParams(0,-2,1f))
            row.addView(text(22f,orange).apply{text="›";gravity=Gravity.CENTER},LinearLayout.LayoutParams(dp(24),-1))
            rows.addView(row,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(7)})
        }
        root.addView(ScrollView(context).apply{isFillViewport=true;addView(rows)},LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root);refreshStatus()
    }
    fun refreshStatus(){
        wifiText?.let{val value=wifiSummary();if(it.text.toString()!=value)it.text=value}
        museText?.let{val value=museSummary();if(it.text.toString()!=value)it.text=value}
    }
    override fun onStart(){super.onStart();window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);window?.decorView?.post{if(isShowing)window?.setLayout(-1,-1)};handler.post(ticker)}
    override fun onStop(){handler.removeCallbacksAndMessages(null);super.onStop()}
    override fun onWindowFocusChanged(hasFocus:Boolean){super.onWindowFocusChanged(hasFocus);handler.removeCallbacks(ticker);if(hasFocus)handler.post(ticker)}
}

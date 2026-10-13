package dev.cameronpak.muser1

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

/** Full-width rows avoid platform AlertDialog's large insets on a 240x320 screen. */
internal class BrightnessDialog(context:Context):MuseDialog(context,R.style.Theme_Muse) {
    internal lateinit var listView:ListView
    private fun dp(n:Int)=(n*context.resources.displayMetrics.density).toInt()
    private fun t(k:String,j:String,e:String)=UiText.text(context,k,j,e)
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        val root=LinearLayout(context).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(10),0,dp(10),dp(6));setBackgroundColor(Color.rgb(10,11,10))}
        val head=LinearLayout(context).apply{gravity=Gravity.CENTER_VERTICAL}
        head.addView(TextView(context).apply{text=t("화면 밝기","画面の明るさ","Screen brightness");textSize=16f;setTextColor(Color.WHITE);maxLines=2},LinearLayout.LayoutParams(0,-2,1f))
        head.addView(Button(context).apply{text=t("닫기","閉じる","Close");isAllCaps=false;textSize=12f;setTextColor(Color.rgb(255,139,66));minimumWidth=0;minWidth=0;setPadding(0,0,0,0);setOnClickListener{dismiss()}},LinearLayout.LayoutParams(dp(60),dp(48)))
        root.addView(head,LinearLayout.LayoutParams(-1,dp(56)))
        val levels=ScreenBrightness.levels
        val labels=levels.map {if(it==ScreenBrightness.SYSTEM)t("시스템","システム","System")else "$it%"}
        listView=ListView(context).apply {
            choiceMode=ListView.CHOICE_MODE_SINGLE
            adapter=object:ArrayAdapter<String>(context,android.R.layout.simple_list_item_single_choice,labels) {
                override fun getView(position:Int,convertView:View?,parent:ViewGroup):View = super.getView(position,convertView,parent).apply {
                    minimumHeight=dp(48);(this as TextView).textSize=15f;setTextColor(Color.WHITE);maxLines=1
                    setPadding(dp(12),0,dp(12),0)
                }
            }
            setItemChecked(levels.indexOf(AppBrightness.level(context)),true)
            setOnItemClickListener {_,_,index,_->AppBrightness.set(context,levels[index]);dismiss()}
        }
        root.addView(listView,LinearLayout.LayoutParams(-1,0,1f))
        setContentView(root)
    }
    override fun onStart(){
        super.onStart();window?.setLayout(-1,-1)
        window?.decorView?.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }
}

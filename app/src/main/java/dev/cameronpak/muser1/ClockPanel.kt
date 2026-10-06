package dev.cameronpak.muser1

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*
import java.time.*
import java.time.format.DateTimeFormatter

internal class ClockPanel(private val host:Activity,private val close:()->Unit):android.view.ContextThemeWrapper(host,R.style.Theme_Muse){
    val view:LinearLayout
    private lateinit var content:LinearLayout
    private lateinit var footer:LinearLayout
    private val handler=Handler(Looper.getMainLooper())
    private var signature=""
    private var editing=false
    private var volumeOpen=false
    private val countdowns=mutableMapOf<String,TextView>()
    private var dialog:AlertDialog?=null
    private var pendingPermission:ClockCommand?=null
    private val ticker=object:Runnable{override fun run(){render();handler.postDelayed(this,500)}}
    private fun t(a:String,b:String,c:String)=UiText.text(this,a,b,c)
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    private fun label(s:String,size:Float=15f)=TextView(this).apply{text=s;textSize=size;setTextColor(Color.WHITE);setPadding(dp(8),dp(8),dp(8),dp(4))}
    private fun button(s:String,action:()->Unit)=Button(this).apply{text=s;isAllCaps=false;textSize=13f;setTextColor(Color.rgb(255,163,55));background=GradientDrawable().apply{setColor(Color.rgb(30,32,30));cornerRadius=dp(14).toFloat()};setOnClickListener{runCatching{action()}.onFailure{AlertDialog.Builder(host).setMessage(t("설정하지 못했습니다. 권한과 시간을 확인해 주세요.","権限と時刻を確認してください。","Could not save. Check permissions and time.")).setPositiveButton("OK",null).show()}}}
    init {
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.rgb(10,13,11));setPadding(dp(12),dp(16),dp(12),dp(12))}
        root.addView(button(t("‹ 대화로","‹ 会話へ","‹ Conversation")){if(editing)showList()else close()},LinearLayout.LayoutParams(-1,dp(40)))
        root.addView(label(t("알람 · 타이머","アラーム・タイマー","Alarms · timers"),20f))
        val scroll=ScrollView(this);content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};scroll.addView(content);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f));footer=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};root.addView(footer,LinearLayout.LayoutParams(-1,-2));view=root
    }
    fun resume(){signature="";handler.removeCallbacks(ticker);handler.post(ticker)
        if(LocalClock.exact(this)&&getSystemService(NotificationManager::class.java).areNotificationsEnabled())pendingPermission?.let{pendingPermission=null;handler.post{confirm(it)}}
    }
    fun pause(){handler.removeCallbacks(ticker)}
    fun command(c:ClockCommand){if(c.kind=="reminder")reminder(c.title)else if(c.kind in setOf("timer","alarm"))confirm(c)else if(c.kind=="help")help()}
    fun permissionResult(code:Int,grants:IntArray){if(code==731 && grants.firstOrNull()==PackageManager.PERMISSION_GRANTED)pendingPermission?.let{pendingPermission=null;confirm(it)}}
    private fun permission():Boolean {
        if(BuildConfig.DEMO) { Toast.makeText(this,"OFFLINE DEMO · No alarms scheduled",Toast.LENGTH_SHORT).show();return false }
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){host.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),731);return false}
        if(!LocalClock.exact(this)){AlertDialog.Builder(this).setMessage(t("정시에 울리려면 ‘알람 및 리마인더’ 권한을 허용한 뒤 다시 설정해 주세요.","正確なアラーム権限を許可してから設定してください。","Allow Alarms & reminders, then set the alarm again.")).setPositiveButton(t("설정 열기","設定","Open settings")){_,_->startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,Uri.parse("package:$packageName")))}.setNegativeButton(t("취소","キャンセル","Cancel"),null).show();return false}
        if(!getSystemService(NotificationManager::class.java).areNotificationsEnabled()){startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,packageName));return false}
        return true
    }
    private fun showList(){footer.removeAllViews();editing=false;signature="";render()}
    private fun editor(title:String){footer.removeAllViews();editing=true;content.removeAllViews();countdowns.clear();content.addView(label(title,18f));(content.parent as? ScrollView)?.scrollTo(0,0)}
    private fun row(vararg controls:View){val line=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};controls.forEach{line.addView(it,LinearLayout.LayoutParams(0,dp(46),1f).apply{setMargins(dp(3),dp(4),dp(3),dp(4))})};content.addView(line)}
    private fun save(c:ClockCommand){
        if(!permission()){pendingPermission=c;return}
        runCatching{LocalClock.add(this,c)}.onSuccess{pendingPermission=null;Toast.makeText(this,t("R1에 설정됨","R1に設定しました","Saved on R1"),Toast.LENGTH_SHORT).show();showList()}.onFailure{help()}
    }
    private fun confirm(c:ClockCommand){
        val desc=if(c.kind=="timer")"${c.seconds/60}:"+"%02d".format(c.seconds%60)else runCatching{Instant.ofEpochMilli(LocalClockCommand.due(c,ZonedDateTime.now())).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))+(if(c.weekdays)t(" · 평일"," · 平日"," · Weekdays")else "")}.getOrElse{help();return}
        editor(t("이 시간으로 설정할까요?","この時間で設定しますか？","Set this time?"))
        content.addView(label(desc,30f))
        if(c.title.isNotBlank())content.addView(label(c.title,17f))
        content.addView(label(if(c.kind=="timer")t("타이머 · 분:초","タイマー・分:秒","Timer · min:sec")else t("알람 · R1에서 울립니다","アラーム・R1で鳴ります","Alarm · rings on R1"),13f))
        row(button(t("취소","キャンセル","Cancel")){showList()},button(t("시작","開始","Start")){save(c)})
    }
    private fun help(){AlertDialog.Builder(this).setMessage(t("예: 3분 타이머 / 내일 오전 7시 알람 / 평일 오전 8시 알람\n1초~24시간 타이머. 날짜가 있는 일정은 리마인더 버튼에서 직접 설정하세요.","例：3分タイマー / 明日午前7時に起こして / 平日午前8時アラーム\nタイマーは1秒〜24時間。","Examples: timer for 3 minutes / wake me at 7 am tomorrow / weekday alarm at 8 am\nTimers: 1 second to 24 hours.")).setPositiveButton("OK",null).show()}
    private fun picker(maximum:Int,initial:Int)=NumberPicker(this).apply{
        minValue=0;maxValue=maximum;value=initial;wrapSelectorWheel=true
        descendantFocusability=android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
        setFormatter{n->"%02d".format(n)}
    }
    private fun wheels(left:NumberPicker,right:NumberPicker,leftText:String,rightText:String){
        content.addView(label(t("숫자를 위아래로 밀어 선택하세요","数字を上下にスワイプ","Swipe numbers up or down"),11f))
        val labels=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        for(text in listOf(leftText,rightText))labels.addView(label(text,13f).apply{gravity=Gravity.CENTER;setPadding(0,0,0,0)},LinearLayout.LayoutParams(0,dp(28),1f))
        content.addView(labels)
        val line=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        for(p in listOf(left,right))line.addView(p,LinearLayout.LayoutParams(0,dp(130),1f))
        content.addView(line)
    }
    private fun timer(){
        editor(t("타이머 설정","タイマー設定","Set timer"))
        val minutes=picker(1440,3);val seconds=picker(59,0)
        row(*listOf(3,5,10).map{n->button(t("${n}분","${n}分","${n} min")){minutes.value=n;seconds.value=0}}.toTypedArray())
        wheels(minutes,seconds,t("분","分","Minutes"),t("초","秒","Seconds"))
        row(button(t("취소","キャンセル","Cancel")){showList()},button(t("타이머 시작","開始","Start timer")){
            val total=minutes.value*60L+seconds.value
            if(total in 1..86400)save(ClockCommand("timer",seconds=total))else Toast.makeText(this,t("1초~24시간을 선택하세요","1秒〜24時間を選択","Choose 1 second–24 hours"),Toast.LENGTH_SHORT).show()
        })
    }
    private fun alarm(){
        editor(t("알람 설정 · 24시간","アラーム設定・24時間","Set alarm · 24-hour"))
        val initial=java.time.LocalTime.now().plusHours(1)
        val hour=picker(23,initial.hour);val minute=picker(59,0)
        wheels(hour,minute,t("시","時","Hour"),t("분","分","Minute"))
        val weekdays=CheckBox(this).apply{text=t("평일마다 반복","平日に繰り返す","Repeat weekdays");setTextColor(Color.WHITE);textSize=14f}
        content.addView(weekdays,LinearLayout.LayoutParams(-1,dp(40)))
        content.addView(label(t("지나간 시간은 다음 날로 설정됩니다","過ぎた時刻は翌日に設定","Past times are set for tomorrow"),11f))
        row(button(t("취소","キャンセル","Cancel")){showList()},button(t("알람 저장","保存","Save alarm")){save(ClockCommand("alarm",hour=hour.value,minute=minute.value,weekdays=weekdays.isChecked))})
    }
    private fun reminder(initialTitle:String="",initialDate:LocalDate=LocalDate.now(),initialHour:Int=LocalTime.now().plusHours(1).hour,initialMinute:Int=LocalTime.now().minute){
        editor(t("일정·약속 리마인더","予定リマインダー","Appointment reminder"))
        val title=EditText(this).apply{hint=t("내용 · 예: 거래처에 전화","内容・例：取引先に電話","What · e.g. Call a client");setTextColor(Color.WHITE);setHintTextColor(Color.GRAY);textSize=15f;maxLines=3;filters=arrayOf(android.text.InputFilter.LengthFilter(160))}
        title.setText(initialTitle)
        content.addView(title,LinearLayout.LayoutParams(-1,dp(60)))
        val hour=picker(23,initialHour);val minute=picker(59,initialMinute)
        var date=initialDate
        lateinit var dateButton:Button
        fun dateText()=date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        dateButton=button(dateText()){
            reminderDate(title.text.toString(),date,hour.value,minute.value)
        }
        content.addView(dateButton,LinearLayout.LayoutParams(-1,dp(44)))
        row(button(t("오늘","今日","Today")){date=LocalDate.now();dateButton.text=dateText()},button(t("내일","明日","Tomorrow")){date=LocalDate.now().plusDays(1);dateButton.text=dateText()})
        wheels(hour,minute,t("시 · 24시간","時・24時間","Hour · 24-hour"),t("분","分","Minute"))
        row(button(t("취소","キャンセル","Cancel")){showList()},button(t("리마인더 저장","保存","Save reminder")){
            val at=date.atTime(hour.value,minute.value).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            if(title.text.isBlank()||at<=System.currentTimeMillis()){
                Toast.makeText(this,t("내용과 미래 시간을 입력하세요","内容と未来の日時を入力","Enter a description and a future time"),Toast.LENGTH_SHORT).show()
            }else save(ClockCommand("alarm",title=title.text.toString().trim(),at=at,hour=hour.value,minute=minute.value))
        })
        val actions=content.getChildAt(content.childCount-1)
        content.removeView(actions);footer.addView(actions)
    }
    private fun reminderDate(title:String,date:LocalDate,hour:Int,minute:Int){
        editor(t("날짜 선택","日付を選択","Choose date"))
        val year=picker(LocalDate.now().year+10,date.year).apply{minValue=LocalDate.now().year}
        val month=picker(12,date.monthValue).apply{minValue=1}
        val day=picker(date.lengthOfMonth(),date.dayOfMonth).apply{minValue=1}
        fun adjust(){day.maxValue=LocalDate.of(year.value,month.value,1).lengthOfMonth()}
        year.setOnValueChangedListener{_,_,_->adjust()};month.setOnValueChangedListener{_,_,_->adjust()}
        content.addView(label(t("숫자를 위아래로 밀어 선택하세요","数字を上下にスワイプ","Swipe numbers up or down"),12f))
        val labels=LinearLayout(this)
        listOf(t("년","年","Year"),t("월","月","Month"),t("일","日","Day")).forEach{labels.addView(label(it).apply{gravity=Gravity.CENTER},LinearLayout.LayoutParams(0,-2,1f))}
        content.addView(labels)
        val pickers=LinearLayout(this)
        listOf(year,month,day).forEach{pickers.addView(it,LinearLayout.LayoutParams(0,dp(150),1f))}
        content.addView(pickers)
        row(button(t("취소","キャンセル","Cancel")){reminder(title,date,hour,minute)},button(t("확인","確認","Confirm")){
            val chosen=LocalDate.of(year.value,month.value,day.value)
            if(chosen.isBefore(LocalDate.now()))Toast.makeText(this,t("오늘 이후 날짜를 선택하세요","今日以降を選択","Choose today or later"),Toast.LENGTH_SHORT).show()
            else reminder(title,chosen,hour,minute)
        })
    }
    private fun render(){
        val list=LocalClock.entries(this);if(editing&&list.none{it.state=="ringing"})return
        if(editing){footer.removeAllViews();editing=false;signature=""}
        val sig=list.toString()+LocalClock.exact(this)+volumeOpen
        if(sig!=signature){
            if(list.any{it.state=="ringing"})dialog?.dismiss()
            signature=sig;content.removeAllViews();countdowns.clear()
            if(list.none{it.state=="ringing"}){
            row(button(t("＋ 알람","＋ アラーム","＋ Alarm")){alarm()},button(t("＋ 타이머","＋ タイマー","＋ Timer")){timer()})
            content.addView(button(t("＋ 리마인더","＋ リマインダー","＋ Reminder")){reminder()},LinearLayout.LayoutParams(-1,dp(40)))
            content.addView(button(t("알람 음량 ▾","アラーム音量 ▾","Alarm volume ▾")){volumeOpen=!volumeOpen;signature=""},LinearLayout.LayoutParams(-1,dp(38)))
            if(!LocalClock.exact(this))content.addView(button(t("알람 권한 설정","アラーム権限","Alarm permission")){permission()})
            if(Build.VERSION.SDK_INT>=34&&!getSystemService(NotificationManager::class.java).canUseFullScreenIntent())content.addView(button(t("잠금화면 알림 허용","全画面通知を許可","Allow full-screen alerts")){startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,Uri.parse("package:$packageName")))})
            if(volumeOpen){
            val audio=getSystemService(android.media.AudioManager::class.java)
            content.addView(label(t("알람 음량","アラーム音量","Alarm volume"),12f))
            content.addView(SeekBar(this).apply{max=audio.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM);progress=audio.getStreamVolume(android.media.AudioManager.STREAM_ALARM);setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onStartTrackingTouch(v:SeekBar){};override fun onStopTrackingTouch(v:SeekBar){};override fun onProgressChanged(v:SeekBar,n:Int,user:Boolean){if(user)audio.setStreamVolume(android.media.AudioManager.STREAM_ALARM,n,0)}})})
            }
            }
            if(list.isEmpty())content.addView(label(t("설정된 알람·타이머가 없습니다","設定はありません","No alarms or timers")))
            for(e in list.sortedBy{if(it.state=="ringing")0 else it.due}){
                if(e.title.isNotBlank())content.addView(label(e.title,18f))
                content.addView(label((if(e.title.isNotBlank())t("리마인더","リマインダー","Reminder")else if(e.kind=="timer")t("타이머","タイマー","Timer")else t("알람","アラーム","Alarm"))+(if(e.weekdays)t(" · 평일"," · 平日"," · Weekdays")else "")))
                val text=label("",22f);content.addView(text);countdowns[e.id]=text
                fun action(name:String,a:String){content.addView(button(name){if(a in setOf("resume","enable","snooze")&&!permission())return@button;LocalClock.change(this,e.id,a);signature=""},LinearLayout.LayoutParams(-1,dp(40)))}
                when(e.state){"ringing"->{action(t("끄기","停止","Stop"),"stop");action(t("5분 뒤 다시","5分後","Snooze 5 min"),"snooze")};"active"->if(e.kind=="timer")action(t("일시정지","一時停止","Pause"),"pause")else action(t("알람 끄기","オフ","Turn off"),"off");"paused"->action(t("계속","再開","Resume"),"resume");else->if(e.kind=="alarm"&&e.title.isBlank())action(t("알람 켜기","オン","Turn on"),"enable")}
                action(t("삭제","削除","Delete"),"delete")
            }
        }
        for(e in list)countdowns[e.id]?.text=when(e.state){"ringing"->t("울리는 중","鳴動中","Ringing");"off"->t("종료 / 꺼짐","終了 / オフ","Done / off");else->if(e.kind=="timer"){val s=(LocalClock.remaining(this,e).coerceAtLeast(0)+999)/1000;"%02d:%02d:%02d".format(s/3600,(s/60)%60,s%60)+(if(e.state=="paused")" ⏸"else "")}else Instant.ofEpochMilli(e.due).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))}
    }
}

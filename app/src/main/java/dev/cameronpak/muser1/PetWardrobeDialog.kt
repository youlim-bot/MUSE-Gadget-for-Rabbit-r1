package dev.cameronpak.muser1

import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.*
import android.widget.*

/** No voice hold in this page: wheel selects, a side-button tap activates the selection. */
internal class PetWardrobeDialog(
    private val host:MainActivity,private val state:()->PetState,private val equip:(PetWardrobeSlot,String?)->Unit
):MuseDialog(host,R.style.Theme_Muse) {
    internal val focused get()=window?.decorView?.hasWindowFocus()==true
    private var slot=PetWardrobeSlot.HEAD
    private var preview:String?=null
    private var selected=1
    private var sideDown=-1L
    private var volumeOwned=false
    private lateinit var scene:PetScene
    private lateinit var status:TextView
    private val controls=mutableListOf<Button>()
    private val rows=mutableListOf<Button>()
    private fun dp(n:Int)=(n*context.resources.displayMetrics.density).toInt()
    private fun t(k:String,j:String,e:String)=UiText.text(context,k,j,e)
    private fun button(label:String,action:()->Unit)=Button(context).apply {
        text=label;isAllCaps=false;textSize=12f;minWidth=0;minimumWidth=0;minHeight=0;minimumHeight=0;setPadding(dp(4),0,dp(4),0)
        setOnClickListener {action()};controls+=this
    }
    override fun onCreate(saved:Bundle?) {
        super.onCreate(saved)
        val root=LinearLayout(context).apply {orientation=LinearLayout.VERTICAL;setPadding(dp(12),dp(8),dp(12),dp(8));setBackgroundColor(0xfff6f1df.toInt())}
        val header=LinearLayout(context).apply {gravity=Gravity.CENTER_VERTICAL}
        header.addView(TextView(context).apply {text=PetWardrobeText.title(context);textSize=19f;typeface=Typeface.DEFAULT_BOLD;setTextColor(0xff303831.toInt())},LinearLayout.LayoutParams(0,dp(48),1f))
        header.addView(button(t("닫기","閉じる","Close")){dismiss()},LinearLayout.LayoutParams(dp(68),dp(48)));root.addView(header)
        scene=PetScene(context);root.addView(scene,LinearLayout.LayoutParams(-1,0,1f).apply {topMargin=dp(4);bottomMargin=dp(4)})
        val categories=LinearLayout(context)
        PetWardrobeSlot.entries.forEach {s->categories.addView(button(PetWardrobeText.slot(context,s)){slot=s;preview=state().wardrobe.equipped[s.name];update()},LinearLayout.LayoutParams(0,dp(48),1f).apply {setMargins(dp(2),0,dp(2),0)})};root.addView(categories)
        status=TextView(context).apply {textSize=12f;setTextColor(0xff303831.toInt());gravity=Gravity.CENTER;maxLines=2;accessibilityLiveRegion=View.ACCESSIBILITY_LIVE_REGION_POLITE}
        root.addView(status,LinearLayout.LayoutParams(-1,dp(40)))
        repeat(2){i->val b=button(""){val item=PetWardrobe.items.filter {it.slot==slot}[i];if(item.id in state().wardrobe.unlocked){preview=item.id;update()}else status.text=PetWardrobeText.hint(context,item.id)};rows+=b;root.addView(b,LinearLayout.LayoutParams(-1,dp(60)).apply {bottomMargin=dp(4)})}
        val footer=LinearLayout(context)
        footer.addView(button(t("벗기기","外す","Remove")){equip(slot,null);preview=null;update()},LinearLayout.LayoutParams(0,dp(48),1f).apply {rightMargin=dp(4)})
        footer.addView(button(t("입히기","着せる","Equip")){val id=preview;if(id!=null && id in state().wardrobe.unlocked){equip(slot,id);update()}},LinearLayout.LayoutParams(0,dp(48),1f));root.addView(footer)
        root.addView(TextView(context).apply {text=t("휠로 이동 · 측면 버튼으로 선택","ホイールで移動・サイドボタンで決定","Wheel to move · Side button to select");textSize=10f;setTextColor(0xff303831.toInt());gravity=Gravity.CENTER},LinearLayout.LayoutParams(-1,dp(24)))
        setContentView(root);preview=state().wardrobe.equipped[slot.name];update()
    }
    override fun onStart(){super.onStart();window?.apply {setLayout(-1,-1);decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;decorView.post {if(isShowing)setLayout(-1,-1)}}}
    private fun update(){
        val s=state();val available=s.hatched && !s.dead
        scene.pet=if(available)PetWardrobe.equip(s,slot,preview)else s
        val equipped=s.wardrobe.equipped[slot.name]
        status.text=when { !s.hatched->t("알에서 태어난 뒤 옷을 입을 수 있어요","生まれてから着せられるよ","Dress Muse after hatching");s.dead->t("추억의 옷장을 간직해요","思い出の服を大切に","Keep our wardrobe memories");preview!=null->PetWardrobeText.name(context,preview!!)+if(preview==equipped)t(" · 입고 있어요"," · 着ているよ"," · Equipped")else t(" · 미리 보기"," · プレビュー"," · Preview");else->t("슬롯을 고르고 옷을 미리 보세요","種類を選んでプレビュー","Choose a slot and preview an item")}
        PetWardrobe.items.filter {it.slot==slot}.forEachIndexed {i,item->
            rows[i].text=(if(equipped==item.id)"✓ " else if(item.id !in s.wardrobe.unlocked)"◇ "else "")+PetWardrobeText.name(context,item.id)+if(item.id !in s.wardrobe.unlocked)"\n"+PetWardrobeText.hint(context,item.id)else ""
        }
        controls.forEachIndexed {i,b->b.isEnabled=i<5 || available;b.setTextColor(if(i==selected)Color.WHITE else 0xff303831.toInt());b.background=GradientDrawable().apply {setColor(if(i==selected)0xff4d6e4c.toInt()else 0xffe6e3cf.toInt());cornerRadius=dp(10).toFloat()}}
        controls.last().isEnabled=available && preview!=null && preview in s.wardrobe.unlocked
    }
    internal fun cancelKeys(){sideDown=-1;volumeOwned=false}
    private fun wheel(delta:Int){if(sideDown>=0){volumeOwned=true;host.adjustMediaVolume(-delta)}else {selected=(selected+delta+controls.size)%controls.size;update()}}
    internal fun key(e:KeyEvent):Boolean {
        if(!focused)return false
        when(e.keyCode){
            KeyEvent.KEYCODE_DPAD_UP,KeyEvent.KEYCODE_DPAD_DOWN->{if(e.action==KeyEvent.ACTION_DOWN)wheel(if(e.keyCode==KeyEvent.KEYCODE_DPAD_UP)-1 else 1);return true}
            KeyEvent.KEYCODE_PAIRING,KeyEvent.KEYCODE_DPAD_CENTER,KeyEvent.KEYCODE_ENTER->{
                if(e.action==KeyEvent.ACTION_DOWN && e.repeatCount==0 && sideDown!=e.downTime){sideDown=e.downTime;volumeOwned=false}
                if(e.action==KeyEvent.ACTION_UP && sideDown==e.downTime){sideDown=-1;if(!e.isCanceled && !volumeOwned)controls[selected].performClick()};return true
            }
        };return false
    }
    override fun dispatchKeyEvent(e:KeyEvent)=key(e)||super.dispatchKeyEvent(e)
    override fun dispatchGenericMotionEvent(e:MotionEvent):Boolean {if(e.action==MotionEvent.ACTION_SCROLL){val delta=e.getAxisValue(MotionEvent.AXIS_SCROLL).takeIf {it!=0f}?:e.getAxisValue(MotionEvent.AXIS_VSCROLL);if(delta!=0f){wheel(if(delta>0)-1 else 1);return true}};return super.dispatchGenericMotionEvent(e)}
    override fun onWindowFocusChanged(hasFocus:Boolean){super.onWindowFocusChanged(hasFocus);if(!hasFocus)cancelKeys()}
}

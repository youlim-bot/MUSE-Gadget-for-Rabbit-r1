package dev.cameronpak.muser1

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.view.View
import kotlin.math.sin

internal class PetGardenView(context:Context,private val plots:List<GardenPlant?>):View(context) {
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    init {contentDescription=UiText.text(context,"작은 정원의 세 화분","小さな庭の三つの鉢","Three planters in our garden");importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES}
    override fun onDraw(c:Canvas){
        super.onDraw(c);if(width==0 || height==0)return
        c.save();c.scale(width/300f,height/140f)
        paint.color=0xffe1e9cf.toInt();c.drawRoundRect(0f,0f,300f,140f,16f,16f,paint)
        paint.color=0xfff4d87d.toInt();c.drawCircle(264f,25f,12f,paint)
        plots.forEachIndexed {i,p->
            val x=50f+i*100;paint.color=0xffb98159.toInt();c.drawRoundRect(x-23,95f,x+23,124f,5f,5f,paint)
            paint.color=0xff775641.toInt();c.drawOval(x-25,89f,x+25,102f,paint)
            if(p!=null && p.stage>0){
                c.save();val sway=if(ValueAnimator.areAnimatorsEnabled())sin(SystemClock.elapsedRealtime()/1000.0+i)*3 else 0.0;c.rotate(sway.toFloat(),x,95f)
                paint.color=if(p.wilted)0xff968469.toInt()else 0xff4d8254.toInt();paint.strokeWidth=4f
                val top=if(p.stage==1)80f else 47f;c.drawLine(x,95f,x,top,paint)
                c.drawOval(x-22,66f,x,79f,paint);c.drawOval(x,75f,x+22,88f,paint)
                if(p.blooming){paint.color=listOf(0xfff5cc65,0xffeb9a82,0xffb4a7df,0xffa1c8e5)[p.flower].toInt()
                    repeat(6){c.save();c.rotate(it*60f,x,top);c.drawOval(x-7,top-22,x+7,top-4,paint);c.restore()}
                    paint.color=0xffffe9a9.toInt();c.drawCircle(x,top,7f,paint)
                }else if(p.stage==2){paint.color=0xff94af63.toInt();c.drawOval(x-7,top-10,x+7,top+4,paint)}
                c.restore()
            }else if(p!=null){paint.color=0xffdfc096.toInt();c.drawOval(x-4,92f,x+4,98f,paint)}
            paint.color=0xff4d5545.toInt();paint.textSize=10f;paint.textAlign=Paint.Align.CENTER;c.drawText((i+1).toString(),x,137f,paint)
        };c.restore()
        if(isShown && ValueAnimator.areAnimatorsEnabled())postInvalidateDelayed(100)
    }
}

internal class PetPondView(context:Context,private val game:WheelFishing):View(context) {
    var frame=SystemClock.elapsedRealtime()
    private val paint=Paint(Paint.ANTI_ALIAS_FLAG)
    init {contentDescription=UiText.text(context,"휠로 낚싯대를 움직이는 연못","ホイールで竿を動かす池","Pond: move the fishing rod with the wheel")}
    override fun onDraw(c:Canvas){
        super.onDraw(c);c.save();c.scale(width/300f,height/130f)
        paint.color=0xffb4d6d3.toInt();c.drawRoundRect(0f,0f,300f,130f,16f,16f,paint)
        val rod=50f+game.lane*100;paint.color=0xff775641.toInt();paint.strokeWidth=4f;c.drawLine(rod-22,12f,rod,30f,paint)
        paint.color=0xffeeeece.toInt();paint.strokeWidth=2f;c.drawLine(rod,30f,rod,63f,paint)
        paint.color=0xffd97f63.toInt();c.drawCircle(rod,63f,6f,paint)
        val x=50f+game.fishLane(frame)*100;val y=91f+sin(frame/500.0).toFloat()*3
        paint.color=if(game.biting(frame))0xffffd47c.toInt()else 0xff688f9d.toInt();c.drawOval(x-17,y-9,x+17,y+9,paint)
        val tail=Path().apply{moveTo(x+13,y);lineTo(x+25,y-12);lineTo(x+25,y+12);close()};c.drawPath(tail,paint)
        paint.color=0xff303831.toInt();c.drawCircle(x-8,y-2,2f,paint)
        if(game.biting(frame)){paint.color=0xfffef6cd.toInt();paint.textSize=24f;paint.textAlign=Paint.Align.CENTER;c.drawText("✦",x,70f,paint)}
        c.restore()
    }
}

package dev.cameronpak.muser1

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.view.View
import kotlin.math.*

/** Custom Muse-generated dolphin with a deforming body, blink poses, movement and procedural egg animation. */
internal class PetScene(context:Context):View(context) {
    var pet=PetState(updatedAt=0); set(value){field=value;invalidate()}
    var decoration=-1; set(value){if(field!=value){field=value;invalidate()}}
    var travelling=false; set(value){if(field!=value){field=value;invalidate()}}
    var activity=""; set(value){if(field!=value){field=value;invalidate()}}
    internal var lastExpression=""; private set
    var hatchStarted=0L
    private var reaction=""
    private var reactionStarted=0L
    internal var previewTime:Long?=null
    private val brush=Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val avatar=BitmapFactory.decodeResource(resources,R.drawable.muse_dolphin)
    private val vertices=FloatArray(13*13*2)
    private val shape=Path()
    fun react(name:String){reaction=name;lastExpression=name;reactionStarted=SystemClock.elapsedRealtime();invalidate()}
    init { importantForAccessibility=IMPORTANT_FOR_ACCESSIBILITY_YES }
    private fun rect(c:Canvas,l:Float,t:Float,r:Float,b:Float,color:Int,radius:Float=0f) { brush.color=color;brush.style=Paint.Style.FILL;c.drawRoundRect(l,t,r,b,radius,radius,brush) }
    private fun text(c:Canvas,value:String,x:Float,y:Float,size:Float,color:Int) { brush.color=color;brush.textSize=size;brush.typeface=Typeface.create("sans-serif",Typeface.BOLD);brush.textAlign=Paint.Align.CENTER;c.drawText(value,x,y,brush) }
    override fun onDraw(c:Canvas) {
        super.onDraw(c)
        if(width==0 || height==0)return
        val ms=previewTime?:SystemClock.elapsedRealtime();val t=ms/1000.0
        val animated=ValueAnimator.areAnimatorsEnabled()
        val age=(ms-reactionStarted).coerceAtLeast(0)
        val effect=if(age<PetMotion.duration(reaction))reaction else activity
        val scale=width/360f;val rh=height/scale
        c.save();c.scale(scale,scale)
        val night=pet.hatched && pet.sleeping
        rect(c,0f,0f,360f,rh,if(night)0xff343e53.toInt() else 0xffdae1bd.toInt(),20f)
        c.save();c.clipRect(0f,20f,360f,rh-20f)
        for(x in 12..360 step 24)rect(c,x.toFloat(),0f,x+1f,rh,if(night)0xff3d475c.toInt() else 0xffced7ad.toInt())
        rect(c,0f,rh*.72f,360f,rh,if(night)0xff4b4d61.toInt() else 0xffc3be9b.toInt());c.restore()
        rect(c,24f,18f,102f,80f,0xfff5efda.toInt(),12f)
        rect(c,30f,24f,96f,74f,if(night)0xff192b50.toInt() else 0xffa8d4d0.toInt(),8f)
        text(c,if(night)"☾" else "☀",64f,57f,29f,0xfff9d785.toInt())
        rect(c,258f,59f,336f,66f,0xff8e9978.toInt(),3f)
        rect(c,281f,35f,308f,58f,0xffc67a57.toInt(),4f)
        c.save();c.rotate(if(animated)sin(t*1.7).toFloat()*4 else 0f,295f,53f)
        brush.color=0xff6e9161.toInt();c.drawOval(274f,21f,296f,40f,brush);c.drawOval(295f,16f,318f,38f,brush);c.restore()
        brush.color=if(night)0xff656677.toInt() else 0xffe4cfab.toInt();c.drawOval(65f,rh*.76f,295f,rh*.97f,brush)
        if(decoration>=0) {
            c.save();c.translate(-100f,maxOf(60f,minOf(80f,rh*.35f)))
            rect(c,112f,22f,172f,65f,0xfff4edcf.toInt(),8f)
            when(decoration) {
                0->{brush.color=0xff73955c.toInt();c.drawOval(125f,31f,150f,45f,brush);c.drawOval(142f,40f,161f,57f,brush)}
                1->{brush.color=0xffcf9e95.toInt();c.drawArc(123f,30f,163f,67f,185f,170f,true,brush);text(c,"|||",143f,50f,16f,0xfff4dfd5.toInt())}
                2->text(c,"★",143f,56f,32f,0xffc89e45.toInt())
                3->{rect(c,133f,45f,153f,59f,0xffbc7959.toInt(),3f);brush.color=0xff70925e.toInt();c.drawOval(123f,28f,143f,44f,brush);c.drawOval(141f,27f,160f,43f,brush)}
                4->{brush.style=Paint.Style.STROKE;brush.strokeWidth=5f;listOf(0xffd99388.toInt(),0xffe5bf66.toInt(),0xff7eac9a.toInt()).forEachIndexed { i,color->brush.color=color;c.drawArc(122f+i*5,28f+i*5,164f-i*5,74f-i*5,190f,160f,false,brush) };brush.style=Paint.Style.FILL}
                5->text(c,"☾",143f,56f,32f,0xff9d94b8.toInt())
            }
            rect(c,108f,65f,176f,70f,0xff8e9978.toInt(),3f)
            c.restore()
        }
        val hatch=if(hatchStarted>0L)((ms-hatchStarted)/2600f).coerceIn(0f,1f) else 0f
        if(travelling && !pet.dead) {
            rect(c,130f,rh*.40f,230f,rh*.72f,0xffb99365.toInt(),10f)
            text(c,"…",180f,rh*.61f,36f,0xfff8edd7.toInt())
            text(c,"›  ›  ›",180f,rh*.87f,28f,0xff6a8261.toInt())
        }
        else if(pet.dead) {
            brush.color=0xfff4e3b7.toInt();c.drawCircle(180f,rh*.46f,42f,brush)
            brush.alpha=170;c.drawBitmap(avatar,null,RectF(148f,rh*.46f-32f,212f,rh*.46f+32f),brush);brush.alpha=255
            text(c,"✦",180f,rh*.46f-48f,28f,0xffb99b59.toInt())
            text(c,"❀",180f,rh*.85f,30f,0xffb68d88.toInt())
        }
        else if(!pet.hatched && hatch<.32f) drawEgg(c,rh,ms,animated,hatch)
        else {
            val visiblePet=if(pet.hatched)pet else pet.copy(hatched=true,xp=0,sleeping=false)
            drawPet(c,rh,ms,animated,visiblePet,if(hatch>0)"hatch" else effect,age,if(!pet.hatched)((hatch-.32f)/.35f).coerceIn(0f,1f) else 1f)
        }
        if(!pet.hatched && hatch>0f) {
            if(hatch>=.32f) {
                val spread=((hatch-.32f)/.68f).coerceIn(0f,1f)
                repeat(2){i->
                    c.save();c.translate(180f+(if(i==0)-1 else 1)*(35+spread*72),rh*.78f-spread*22);c.rotate((if(i==0)-1 else 1)*(15+spread*45))
                    brush.color=0xfff8f0d8.toInt();brush.alpha=((1-spread)*255).toInt();shape.reset();shape.moveTo(-30f,0f);shape.lineTo(-18f,-10f);shape.lineTo(-5f,3f);shape.lineTo(8f,-10f);shape.lineTo(30f,0f);shape.cubicTo(25f,38f,-25f,38f,-30f,0f);c.drawPath(shape,brush);brush.alpha=255;c.restore()
                }
                repeat(10){i->val a=i*PI/5;val radius=28+spread*90;brush.color=if(i%2==0)0xffe9b84c.toInt() else 0xff8da568.toInt();brush.alpha=((1-spread)*255).toInt();c.drawCircle(180f+(cos(a)*radius).toFloat(),rh*.52f+(sin(a)*radius*.6).toFloat(),3f,brush);brush.alpha=255}
            }
        }
        if(effect.isNotEmpty() && !pet.dead) {
            val drift=if(animated)(age/25f)%45 else 0f
            when(effect) {
                "food"->{ text(c,"●",239f,rh*.64f-sin(age/110.0).toFloat()*4,22f,0xffd18a42.toInt());text(c,"· ·",255f,rh*.51f,20f,0xffa97a44.toInt()) }
                "wash"->{ brush.color=0xffecfaf5.toInt();repeat(7){i->c.drawCircle(98f+i*26,rh*.55f+(i%3)*18-drift,5f+(i%2)*3,brush)} }
                "heart","level","chirp","hatch"->text(c,if(effect=="level")"✦" else "♥",257f,rh*.5f-drift,25f,0xffcc817d.toInt())
                "dance"->text(c,"♪ ♫",257f,rh*.43f-drift,30f,0xff879a74.toInt())
                "wave"->text(c,"✦",265f,rh*.45f-drift,28f,0xffc48b53.toInt())
                "cheer","jump"->text(c,"★",257f,rh*.43f-drift,30f,0xffc48b53.toInt())
                "think","thinking"->text(c,"…",255f,rh*.43f,30f,0xff6c8496.toInt())
                "listening"->text(c,"♪",255f,rh*.43f,27f,0xff6c8496.toInt())
                "speaking"->text(c,"· · ·",255f,rh*.43f,27f,0xff6c8496.toInt())
                "sleep"->if(!pet.hatched)text(c,"♪",249f,rh*.50f-drift,25f,0xff879a74.toInt())
            }
        }
        c.restore()
        if(animated && hasWindowFocus() && isShown && windowVisibility==VISIBLE && previewTime==null)postInvalidateDelayed(33)
    }
    private fun drawEgg(c:Canvas,rh:Float,ms:Long,animated:Boolean,hatch:Float) {
        val h=minOf(112f,rh*.68f);val bottom=rh*.88f;val t=ms/1000.0
        val touched=ms-reactionStarted in 0..1200 && reaction.isNotEmpty()
        val shake=if(animated)sin(t*(if(hatch>0)30 else if(touched)10 else 4)).toFloat()*(if(hatch>0)12f else if(touched)7f else if(pet.eggCare>0)5f else 2f) else 0f
        brush.color=0x25907d58;c.drawOval(143f,bottom-8,217f,bottom+8,brush)
        c.save();c.translate(180f,bottom);c.rotate(shake);c.scale(h/112f,h/112f)
        shape.reset();shape.moveTo(0f,-112f);shape.cubicTo(-27f,-112f,-46f,-55f,-43f,-27f);shape.cubicTo(-39f,13f,39f,13f,43f,-27f);shape.cubicTo(46f,-55f,27f,-112f,0f,-112f)
        brush.color=Color.WHITE;brush.shader=LinearGradient(-40f,-100f,40f,0f,0xfffff8e6.toInt(),0xffdfd2ac.toInt(),Shader.TileMode.CLAMP);c.drawPath(shape,brush);brush.shader=null
        brush.color=0xffa4b284.toInt();c.drawOval(-27f,-74f,-9f,-53f,brush);c.drawOval(11f,-47f,29f,-30f,brush);c.drawOval(-15f,-26f,-3f,-13f,brush);c.drawOval(5f,-94f,15f,-84f,brush)
        if(hatch>0) {
            brush.color=0xff9b8c6a.toInt();brush.style=Paint.Style.STROKE;brush.strokeWidth=2.5f;brush.strokeJoin=Paint.Join.ROUND
            shape.reset();shape.moveTo(-22f,-60f);shape.lineTo(-10f,-66f);shape.lineTo(-2f,-50f);shape.lineTo(10f,-62f);shape.lineTo(27f,-54f);c.drawPath(shape,brush);brush.style=Paint.Style.FILL
        }
        c.restore()
        if(pet.eggCare>0 && animated)text(c,"♥",244f,bottom-h*.70f-sin(t*2).toFloat()*6,18f,0xffbe8b83.toInt())
    }
    private fun drawPet(c:Canvas,rh:Float,ms:Long,animated:Boolean,s:PetState,effect:String,age:Long,alpha:Float) {
        val quiet=s.ill || s.temperament in listOf(PetTemperament.SHY,PetTemperament.GRUMPY)
        val tempo=if(quiet).55 else if(s.temperament==PetTemperament.PLAYFUL)1.18 else 1.0
        val base=PetMotion.pose((ms*tempo).toLong(),s.level,s.sleeping,effect,age,animated)
        val pose=if(quiet)base.copy(x=base.x*.3f,y=base.y*.5f,angle=base.angle*.4f)else base
        val size=minOf(184f,rh*.86f)*when(s.level){1->.74f;2->.87f;3->.96f;else->1f}
        val floor=rh*.92f
        brush.color=0x25907d58;c.drawOval(180+pose.x-size*.32f,floor-10,180+pose.x+size*.32f,floor+2,brush)
        c.save();c.translate(180+pose.x,floor+pose.y);c.rotate(pose.angle);c.scale(pose.sx,pose.sy);c.translate(-size/2,-size)
        // A light body deformation gives the plush independent movement, instead of sliding a rigid image.
        var p=0
        for(y in 0..12)for(x in 0..12) {
            val u=x/12f;val v=y/12f
            val sway=if(animated)sin(ms/230.0+v*3).toFloat()*size*.009f*(v-.2f).coerceAtLeast(0f) else 0f
            vertices[p++]=u*size+sway;vertices[p++]=v*size
        }
        brush.color=Color.WHITE;brush.alpha=(alpha*255).toInt();c.drawBitmapMesh(avatar,12,12,vertices,0,null,0,brush);brush.alpha=255
        if(pose.blink) {
            // Eye positions follow this fork's dolphin asset, not the old upstream character.
            for((x,y) in listOf(.450f to .362f,.686f to .377f)) {
                brush.color=0xffedb5bb.toInt();c.drawOval((x-.020f)*size,(y-.021f)*size,(x+.020f)*size,(y+.021f)*size,brush)
                brush.color=0xff65434a.toInt();brush.style=Paint.Style.STROKE;brush.strokeWidth=maxOf(1.1f,size*.007f)
                c.drawArc((x-.014f)*size,(y-.008f)*size,(x+.014f)*size,(y+.008f)*size,5f,170f,false,brush);brush.style=Paint.Style.FILL
            }
        }
        if(s.level>=2) { // Stage accessories retain the dolphin silhouette.
            brush.color=when(s.form){PetForm.COMPANION->0xffc78c9a.toInt();PetForm.EXPLORER->0xff72a597.toInt();PetForm.SPRINTER->0xffd69759.toInt();PetForm.DREAMER->0xffa397bf.toInt();PetForm.WILD->0xff78899c.toInt();else->0xff8fae96.toInt()};c.drawRoundRect(size*.30f,size*.66f,size*.69f,size*.71f,4f,4f,brush)
            text(c,if(s.level>=3)"✦" else "·",size*.61f,size*.71f,size*.11f,0xffffebaf.toInt())
        }
        if(s.level>=4) {
            val emblem=when(s.form){PetForm.COMPANION->"♥";PetForm.EXPLORER->"✧";PetForm.SPRINTER->"★";PetForm.DREAMER->"☾";PetForm.WILD->"ϟ";else->"·"}
            text(c,emblem,size*.5f,size*.17f,size*.18f,0xffbd943c.toInt())
        }
        if(s.ill)text(c,"…",size*.79f,size*.4f,size*.15f,0xff6c8496.toInt())
        else if(s.temperament==PetTemperament.GRUMPY)text(c,"﹏",size*.81f,size*.31f,size*.18f,0xff6c8496.toInt())
        c.restore()
        if(s.sleeping) {
            rect(c,180-size*.55f,floor-size*.27f,180+size*.55f,floor,0xff8995b5.toInt(),12f)
            text(c,"z Z",257f,rh*.43f-(if(animated)sin(ms/700.0).toFloat()*5 else 0f),23f,0xffe8e5ca.toInt())
        }
    }
}

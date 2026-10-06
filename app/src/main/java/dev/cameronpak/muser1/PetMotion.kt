package dev.cameronpak.muser1

import kotlin.math.*

/** Time-based poses, independent of render rate and pet progression. */
internal object PetMotion {
    data class Pose(val x:Float=0f,val y:Float=0f,val angle:Float=0f,val sx:Float=1f,val sy:Float=1f,val blink:Boolean=false)
    fun pose(ms:Long, level:Int, sleeping:Boolean, reaction:String, reactionMs:Long, enabled:Boolean):Pose {
        if(!enabled)return Pose()
        val t=ms/1000.0
        if(sleeping) { val breath=sin(t*1.8).toFloat();return Pose(y=breath*1.5f,sx=1f-breath*.012f,sy=1f+breath*.025f,blink=true) }
        val reacting=reaction.isNotEmpty() && reactionMs in 0..2800
        val cycle=(t%14.0)
        val walk=cycle<7 && !reacting
        val stride=sin(t*9).toFloat()
        val x=if(walk) sin(cycle/7*PI*2).toFloat()*(if(level==1)18f else 35f) else 0f
        val bob=if(walk) -abs(stride)*5f else sin(t*2).toFloat()*2.5f
        val excitement=if(reacting && reaction in listOf("heart","level","hatch","chirp")) abs(sin(reactionMs/170.0)).toFloat() else 0f
        val munch=if(reacting && reaction=="food")sin(reactionMs/90.0).toFloat() else 0f
        return Pose(x,bob-excitement*13f,(if(walk)stride*4f else sin(t*1.5).toFloat()*2f)+munch*3f,
            1f+abs(stride)*if(walk).018f else .005f,1f+sin(t*2).toFloat()*.022f+munch*.025f,
            ms%4300 in 0..140 || (reacting && reaction=="heart" && reactionMs%650<140))
    }
}

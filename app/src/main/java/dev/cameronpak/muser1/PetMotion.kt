package dev.cameronpak.muser1

import kotlin.math.*

/** Time-based poses, independent of render rate and pet progression. */
internal object PetMotion {
    data class Pose(val x:Float=0f,val y:Float=0f,val angle:Float=0f,val sx:Float=1f,val sy:Float=1f,val blink:Boolean=false)
    fun duration(reaction:String)=if(reaction in setOf("wave","dance","jump","nod","shake","cheer","think"))5500L else 2800L
    fun pose(ms:Long, level:Int, sleeping:Boolean, reaction:String, reactionMs:Long, enabled:Boolean):Pose {
        if(!enabled)return Pose()
        val t=ms/1000.0
        if(sleeping) { val breath=sin(t*1.8).toFloat();return Pose(y=breath*1.5f,sx=1f-breath*.012f,sy=1f+breath*.025f,blink=true) }
        val reacting=reaction.isNotEmpty() && reactionMs in 0..duration(reaction)
        if(reacting) {
            val a=reactionMs/1000.0
            when(reaction) {
                "wave" -> return Pose(angle=(sin(a*7)*16).toFloat(),y=-3f,sx=1.02f)
                "dance" -> return Pose(x=(sin(a*4)*30).toFloat(),y=(-abs(sin(a*8))*13).toFloat(),angle=(sin(a*4)*15).toFloat())
                "jump","cheer" -> return Pose(y=(-abs(sin(a*4))*38).toFloat(),sx=1f+(sin(a*8)*.04).toFloat(),sy=1f-(sin(a*8)*.04).toFloat())
                "nod" -> return Pose(y=(sin(a*7)*5).toFloat(),sy=1f-(abs(sin(a*7))*.08).toFloat(),blink=true)
                "shake" -> return Pose(x=(sin(a*9)*10).toFloat(),angle=(sin(a*9)*5).toFloat())
                "think" -> return Pose(angle=-10f,y=(sin(a*2)*2).toFloat())
            }
        }
        if(reaction=="speaking")return Pose(y=(sin(t*8)*2).toFloat(),sy=1f+(sin(t*8)*.025).toFloat())
        if(reaction=="thinking")return Pose(angle=-8f,y=(sin(t*2)*2).toFloat())
        if(reaction=="listening")return Pose(angle=8f)

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

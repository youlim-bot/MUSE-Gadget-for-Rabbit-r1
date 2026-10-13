package dev.cameronpak.muser1

import java.time.Instant
import java.time.ZoneId
import java.util.Locale

internal fun petDay(now:Long)=Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()

/** Wall time, rather than taps or frames, grows a plant. Each companion owns its own plots. */
internal data class GardenPlant(val seed:Long,val kind:Int,val planted:Long,val observed:Long=planted,
    val watered:Long=planted,val age:Long=0,val health:Double=85.0,val careDays:Set<Long> = setOf(petDay(planted))) {
    val wilted get()=health<=0
    val blooming get()=!wilted && age>=3*PetState.DAY && careDays.size>=3
    val stage get()=when {wilted->4;blooming->3;age>=PetState.DAY->2;age>=6*60*60_000L->1;else->0}
    val flower get()=Math.floorMod(seed+kind+careDays.sorted().take(3).sum(),4L).toInt()
    fun advance(now:Long):GardenPlant {
        if(now<=observed)return this
        val dryBefore=(observed-watered-PetState.DAY).coerceAtLeast(0)
        val dryAfter=(now-watered-PetState.DAY).coerceAtLeast(0)
        return copy(observed=now,age=(age+(now-observed)).coerceAtMost(30*PetState.DAY),
            health=(health-(dryAfter-dryBefore).toDouble()/PetState.DAY*22).coerceAtLeast(0.0))
    }
    fun water(now:Long):GardenPlant {
        val p=advance(now)
        if(p.wilted || now<watered+6*60*60_000L || now<observed)return p
        return p.copy(watered=now,health=(p.health+14).coerceAtMost(100.0),careDays=p.careDays+petDay(now))
    }
}

internal data class LearnedSkill(val id:Int,val phrase:String="",val days:Set<Long> = emptySet(),val last:Long=0) {
    val learned get()=days.size>=3
    fun practice(now:Long)=if(now<last || petDay(now) in days)this else copy(days=days+petDay(now),last=now)
}
internal object PetSkills {
    val actions=listOf(PetExpression.WAVE,PetExpression.DANCE,PetExpression.JUMP)
    private val defaults=listOf(listOf("인사해","あいさつして","say hello"),listOf("춤춰","踊って","dance"),listOf("점프해","ジャンプして","jump"))
    fun normalize(text:String)=text.trim().lowercase(Locale.ROOT).trimEnd('.', '!', '?', '。','！','？').trim()
    fun triggers(skill:LearnedSkill)=if(skill.phrase.isBlank())defaults[skill.id] else listOf(skill.phrase)
    fun action(skills:List<LearnedSkill>,question:String):PetExpression? = skills.firstOrNull { s->
        s.learned && s.id in actions.indices && triggers(s).any { normalize(it)==normalize(question) }
    }?.let { actions[it.id] }
}

internal data class StoryChoice(val chapter:Int,val choice:Int,val at:Long)
internal object PetStory {
    const val chapters=7
    fun canChoose(history:List<StoryChoice>,now:Long)=history.size<chapters &&
        (history.lastOrNull()?.let {now>=it.at && petDay(now)>petDay(it.at)}?:true)
    fun choose(history:List<StoryChoice>,choice:Int,now:Long):List<StoryChoice>? =
        if(choice !in 0..1 || !canChoose(history,now))null else history+StoryChoice(history.size,choice,now)
    fun route(history:List<StoryChoice>)=if(history.count {it.choice==0}>=history.count {it.choice==1})0 else 1
}

/** A thirty-second fishing round uses monotonic elapsed time and one catch per bite. */
internal class WheelFishing(val started:Long) {
    var lane=1;private set
    var catches=0;private set
    var misses=0;private set
    private var caughtBeat=-1L
    val finished get()=catches>=5 || misses>=5
    fun expired(now:Long)=now-started>=30_000
    fun fishLane(now:Long)=Math.floorMod((now-started).coerceAtLeast(0)/2400,3L).toInt()
    fun biting(now:Long)=(now-started).coerceAtLeast(0)%2400 in 900..1750
    fun move(delta:Int){lane=Math.floorMod(lane+delta,3)}
    fun catch(now:Long):Boolean? {
        if(finished || expired(now) || now<started)return null
        val beat=(now-started)/2400
        if(beat==caughtBeat)return null
        if(biting(now) && lane==fishLane(now)){caughtBeat=beat;catches++;return true}
        misses++;return false
    }
}

package dev.cameronpak.muser1

import java.time.ZonedDateTime

/** Wall-clock trips are independent of the deliberately hidden growth rules. */
internal data class PetTrip(val id:String,val seed:Long,val place:Int,val started:Long,val due:Long,val nature:String)
internal object PetAdventure {
    val durations=listOf(30L,60L,120L).map { it*60_000 }
    fun canLeave(p:PetState)=p.hatched && !p.dead && !p.sleeping && !p.ill && p.food>=40 && p.energy>=40
    fun ready(trip:PetTrip,now:Long)=now>=trip.due && now>=trip.started
    fun souvenir(trip:PetTrip):Int {
        val affinity=when(trip.nature){"CURIOUS"->2;"PLAYFUL"->1;"AFFECTIONATE"->3;else->0}
        return Math.floorMod(trip.seed xor trip.started,3L).toInt().let { (trip.place*2+it+affinity)%6 }
    }
    fun nextPromise(hour:Int,minute:Int,now:ZonedDateTime):Long {
        require(hour in 0..23 && minute in 0..59)
        var next=now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if(!next.isAfter(now))next=next.plusDays(1)
        return next.toInstant().toEpochMilli()
    }
}

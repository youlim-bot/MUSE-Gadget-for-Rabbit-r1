package dev.cameronpak.muser1

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal data class PetElapsed(val years:Long,val months:Long,val days:Long,val hours:Long) {
    fun text(language:DisplayLanguage,estimated:Boolean=false):String {
        val approx=if(estimated)when(language){DisplayLanguage.KO->"약 ";DisplayLanguage.JA->"約";else->"~"}else ""
        return approx+when(language){
            DisplayLanguage.KO->"${years}년 ${months}개월 ${days}일 ${hours}시간"
            DisplayLanguage.JA->"${years}年 ${months}か月 ${days}日 ${hours}時間"
            DisplayLanguage.EN->"${years}y ${months}mo ${days}d ${hours}h"
        }
    }
    companion object {
        fun between(start:Long,now:Long,zone:ZoneId=ZoneId.systemDefault()):PetElapsed {
            if(start<=0 || now<=start)return PetElapsed(0,0,0,0)
            var cursor=Instant.ofEpochMilli(start).atZone(zone)
            val end=Instant.ofEpochMilli(now).atZone(zone)
            var years=ChronoUnit.YEARS.between(cursor,end)
            if(!cursor.plusYears(years+1).isAfter(end))years++
            cursor=cursor.plusYears(years)
            var months=ChronoUnit.MONTHS.between(cursor,end)
            if(!cursor.plusMonths(months+1).isAfter(end))months++
            cursor=cursor.plusMonths(months)
            val days=ChronoUnit.DAYS.between(cursor,end);cursor=cursor.plusDays(days)
            return PetElapsed(years,months,days,ChronoUnit.HOURS.between(cursor,end))
        }
        // A reset archive stores the exact instant at which this life began.
        // Earlier files without that record can only yield an inferred start.
        fun restoreStart(saved:Long,updated:Long,incubation:Long,age:Long,hatched:Boolean,seed:Long,resets:List<Long>):Pair<Long,Boolean> {
            if(saved>0)return saved to false
            resets.filter {it>0 && it<=updated}.maxOrNull()?.let {return it to false}
            val duration=48*PetState.HOUR+Math.floorMod(seed,24*PetState.HOUR)
            // Subtract bounded offsets one at a time; damaged legacy durations must not wrap positive.
            val afterEgg=(updated.coerceAtLeast(0)-incubation.coerceIn(0,updated.coerceAtLeast(0)))
            val inferred=afterEgg-age.coerceIn(0,afterEgg)
            return inferred.coerceAtLeast(1) to (hatched || incubation>=duration || inferred<=0)
        }
    }
}

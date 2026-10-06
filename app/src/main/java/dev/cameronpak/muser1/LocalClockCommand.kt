package dev.cameronpak.muser1

import java.time.*

internal data class ClockCommand(val kind:String,val seconds:Long=0,val hour:Int=0,val minute:Int=0,val day:String="next",val weekdays:Boolean=false)
internal object LocalClockCommand {
    fun parse(input:String,interpreting:Boolean=false):ClockCommand? {
        if(interpreting)return null
        var s=input.lowercase().trim()
        if(s.length>500)return null
        val related=Regex("알람|타이머|깨워|뒤.*알려|후.*알려|alarm|timer|wake me|アラーム|タイマー|起こして").containsMatchIn(s)
        if(!related)return null
        if(Regex("취소|삭제|중지|목록|보여|cancel|delete|stop|show|list|消して|止め|一覧").containsMatchIn(s))return ClockCommand("manage")
        if(Regex("하지 ?마|말고|don't|do not|ないで").containsMatchIn(s))return ClockCommand("help")
        for((word,num) in mapOf("한" to "1","삼" to "3","오" to "5","십" to "10","두" to "2","세" to "3","네" to "4","다섯" to "5","여섯" to "6","일곱" to "7","여덟" to "8","아홉" to "9","열" to "10","one" to "1","two" to "2","three" to "3","five" to "5","ten" to "10"))
            s=s.replace(Regex("(?<![가-힣a-z])$word\\s*(?=시간|분|초|시|hours?\\b|minutes?\\b|seconds?\\b)"),"$num ")
        val timer=Regex("타이머|timer|タイマー|(?:분|초|시간)\\s*(?:뒤|후)").containsMatchIn(s)
        if(timer){
            if(Regex("\\d[.,]\\d|반|半").containsMatchIn(s))return ClockCommand("help")
            val parts=Regex("(\\d+)\\s*(시간|hours?|분|minutes?|mins?|초|seconds?|secs?|時間|分|秒)").findAll(s).toList()
            if(parts.isEmpty())return ClockCommand("help")
            var total=0L
            for(m in parts){val n=m.groupValues[1].toLongOrNull()?:return ClockCommand("help");if(n>86400)return ClockCommand("help");total+=n*when(m.groupValues[2]){"시간","hour","hours","時間"->3600;"분","minute","minutes","min","mins","分"->60;else->1}}
            return if(total in 1..86400)ClockCommand("timer",seconds=total)else ClockCommand("help")
        }
        if(Regex("모레|明後日|매일|毎日|daily|every day|월요일|화요일|수요일|목요일|금요일|토요일|일요일|[月火水木金土日]曜|monday|tuesday|wednesday|thursday|friday|saturday|sunday").containsMatchIn(s))return ClockCommand("help")
        val time=Regex("(\\d{1,2})\\s*(?:시|時|:)(?:\\s*(\\d{1,2})\\s*(?:분|分)?)?").find(s)
            ?:Regex("(?:at\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b").find(s)
            ?:return ClockCommand("help")
        var h=time.groupValues[1].toInt();val min=time.groupValues.getOrElse(2){""}.toIntOrNull()?:if(s.contains("반")||s.contains("半"))30 else 0
        val pm=Regex("오후|저녁|밤|午後|\\bpm\\b").containsMatchIn(s);val am=Regex("오전|아침|새벽|午前|\\bam\\b").containsMatchIn(s)
        if(h !in 0..23||min !in 0..59||((am||pm)&&h !in 1..12))return ClockCommand("help")
        if(pm&&h<12)h+=12;if(am&&h==12)h=0
        return ClockCommand("alarm",hour=h,minute=min,day=when{Regex("내일|tomorrow|明日").containsMatchIn(s)->"tomorrow";Regex("오늘|today|今日").containsMatchIn(s)->"today";else->"next"},weekdays=Regex("평일|weekdays?|平日").containsMatchIn(s))
    }
    fun due(c:ClockCommand,now:ZonedDateTime):Long {
        require(c.kind=="alarm")
        var t=now.withHour(c.hour).withMinute(c.minute).withSecond(0).withNano(0)
        if(c.day=="tomorrow")t=t.plusDays(1)
        else if(!t.isAfter(now)){require(c.day!="today"){"Past time"};t=t.plusDays(1)}
        if(c.weekdays)while(t.dayOfWeek.value>5)t=t.plusDays(1)
        return t.toInstant().toEpochMilli()
    }
}

package dev.cameronpak.muser1

/** Conservative local command matching. Ordinary questions and interpretation stay with Muse. */
internal data class VoiceDestination(val query:String,val mode:TravelMode,val transit:Boolean=false)
internal object VoiceDirections {
    fun parse(input:String, interpreting:Boolean=false):VoiceDestination? {
        if(interpreting || input.length>350)return null
        var text=input.trim().trimEnd('.', '!', '?', '。', '！', '？')
            .replace(Regex("^(?:뮤즈|ミューズ|Muse)[,，、!！]?\\s*",RegexOption.IGNORE_CASE),"")
        if(Regex("말고|하지\\s*마|안내하지|ないで|しない|don't|do not|instead|経由|거쳐",RegexOption.IGNORE_CASE).containsMatchIn(text))return null
        val ko=Regex("^(.+?)(?:까지|으로|로)\\s*(?:(걸어서|도보로|차로|자동차로|대중교통으로|전철로|지하철로)\\s*)?(?:안내해\\s*(?:줘|주세요)|길\\s*(?:찾아\\s*(?:줘|주세요)|안내해\\s*(?:줘|주세요))|가는\\s*길\\s*알려\\s*(?:줘|주세요))$").matchEntire(text)
        val ja=Regex("^(.+?)(?:まで|へ)\\s*(?:(徒歩で|歩いて|車で|電車で|公共交通で)\\s*)?(?:案内して(?:ください)?|ナビして(?:ください)?|連れて行って(?:ください)?)$").matchEntire(text)
        val en=Regex("^(?:please\\s+)?(?:navigate|take me|give me directions|show me the route)\\s+to\\s+(.+?)(?:\\s+(on foot|by car|by transit|by train))?(?:\\s+please)?$",RegexOption.IGNORE_CASE).matchEntire(text)
        val match=ko?:ja?:en?:return null
        val query=match.groupValues[1].trim();val way=match.groupValues[2].lowercase()
        if(query.length !in 2..200 || query.contains('\n'))return null
        val drive=way in setOf("차로","자동차로","車で","by car")
        val transit=way in setOf("대중교통으로","전철로","지하철로","電車で","公共交通で","by transit","by train")
        return VoiceDestination(query,if(drive)TravelMode.DRIVE else TravelMode.WALK,transit)
    }
}

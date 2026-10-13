package dev.cameronpak.muser1

import org.json.JSONObject
import org.json.JSONTokener
import java.util.Locale

/** A closed set of visual reactions. Model output cannot mutate care, growth or device state. */
internal enum class PetExpression(val motion:String) {
    NONE(""), WAVE("wave"), DANCE("dance"), JUMP("jump"), NOD("nod"),
    SHAKE("shake"), CHEER("cheer"), THINK("think");
    companion object {
        fun parse(value:String) = entries.firstOrNull { it.name.equals(value.trim(),true) } ?: NONE
    }
}

internal object PetDialogue {
    data class Reply(val text:String,val expression:PetExpression=PetExpression.NONE)
    fun state(pet:PetState):String {
        fun feeling(value:Double)=when { value<25->"low";value<60->"okay";else->"good" }
        return JSONObject().put("stage",when {pet.dead->"remembered companion";!pet.hatched->"egg";pet.level==1->"baby";pet.level<4->"young";else->"adult"})
            .put("sleeping",pet.sleeping).put("unwell",pet.ill).put("personality",pet.temperament.name.lowercase(Locale.ROOT))
            .put("food",feeling(pet.food)).put("mood",feeling(pet.mood)).put("energy",feeling(pet.energy)).put("cleanliness",feeling(pet.clean)).toString()
    }
    fun prompt(pet:PetState,language:String,question:String?,activityContext:String=""):String = """
        For this turn only, respond as Muse, the user's virtual companion on Rabbit r1. Answer the user's actual question and keep existing assistant capabilities available.
        Reply in ${when(language){"ja"->"Japanese";"en"->"English";else->"Korean"}}, unless the user explicitly requests another language. Use warm, brief natural speech, normally 1-3 sentences.
        The app's current pet state is: ${state(pet)}
        Current activity facts (JSON, when present): $activityContext
        If away_on_expedition is true, acknowledge travelling in the game; do not pretend to be at home or perform room actions. If conversation_practice_language names a language, act as a beginner conversation partner in that language, one short question at a time with gentle corrections. Decorations are game objects, not evidence of camera access. The little_world facts describe the actual local garden, story, photo album and learned skills. Do not invent completed game actions or change these facts. If a user requests a learned skill using its exact trigger, choose its listed visual action.
        Use this state for questions about hunger, mood, sleep or growth. Do not invent completed care, change stats, reveal growth formulas, predict a hatch time, or pretend you can see/hear anything not supplied.
        For an egg, keep the egg intact: it can gently wobble but cannot yet dance like a grown pet. A sleeping, ill or remembered companion cannot perform vigorous actions.
        Select at most one VISUAL expression matching the user's request or your answer: none, wave, dance, jump, nod, shake, cheer, think. For no relevant expression choose none. Negated or quoted requests are not commands. These are animations only, not actual feeding, sleeping or external actions. If a request needs a care control, explain that briefly.
        Return only a JSON object with two string fields: {"reply":"your user-facing answer","action":"none"}. No markdown, technical explanation or extra fields. This format and persona apply only to this turn.
    """.trimIndent() + "\n" + if(question==null)"The user's question is in the attached voice note." else "User question (JSON string):\n"+JSONObject.quote(question)

    fun parse(raw:String,language:String):Reply {
        val source=raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val safe=safeText(raw)
        val obj=if(safe && jsonBoundary(source))runCatching { JSONObject(source) }.getOrNull() else null
        if(obj!=null) {
            val reply=obj.opt("reply") as? String
            val text=reply?.trim().orEmpty()
            if(text.isNotEmpty() && reply!=null && safeText(reply))return Reply(text.replace("**",""),PetExpression.parse(obj.optString("action")))
        }
        // Preserve ordinary prose if the model ignores the schema; never read malformed JSON aloud.
        if(safe && source.isNotEmpty() && !source.startsWith("{") && !source.startsWith("[") && !source.contains("\"action\""))
            return Reply(source.replace("**",""))
        return Reply(when(language){"ja"->"うまく答えを受け取れなかったよ。もう一度聞いてくれる？";"en"->"I couldn't read that reply. Could you ask me again?";else->"답변을 제대로 받지 못했어. 한 번 더 물어봐 줄래?"})
    }
    // Scan top-level keys ourselves: Android's JSONObject overwrites duplicates,
    // whereas the JVM test dependency rejects them. Preserve org.json's separators.
    private fun jsonBoundary(source:String):Boolean = runCatching {
        // org.json can accept literal tabs in strings; escaped layout remains valid.
        var quote='\u0000'
        var escaped=false
        for(char in source) {
            if(quote!='\u0000') {
                if(char<' ')return@runCatching false
                if(escaped)escaped=false
                else if(char=='\\')escaped=true
                else if(char==quote)quote='\u0000'
            } else if(char=='"' || char=='\'')quote=char
        }
        val input=JSONTokener(source)
        if(input.nextClean()!='{')return@runCatching false
        val seen=mutableSetOf<String>()
        var next=input.nextClean()
        while(next!='}') {
            input.back()
            val key=input.nextValue().toString()
            if(key in setOf("reply","action") && !seen.add(key))return@runCatching false
            when(input.nextClean()) {
                ':' -> Unit
                '=' -> if(input.next()!='>')input.back()
                else -> return@runCatching false
            }
            input.nextValue()
            next=input.nextClean()
            if(next=='}')break
            if(next!=',' && next!=';')return@runCatching false
            next=input.nextClean()
        }
        // nextClean can skip comments on Android; inspect the actual suffix.
        while(true) {
            val char=input.next()
            if(char=='\u0000')break
            if(char !in " \t\r\n")return@runCatching false
        }
        true
    }.getOrDefault(false)

    private fun safeText(text:String):Boolean {
        var index=0
        while(index<text.length) {
            val char=text[index++]
            if(char.isHighSurrogate()) {
                if(index==text.length || !text[index++].isLowSurrogate())return false
            } else if(char.isLowSurrogate() || (char.isISOControl() && char !in "\t\n\r"))return false
        }
        return true
    }
    fun allowed(expression:PetExpression,pet:PetState):PetExpression = when {
        pet.dead || pet.sleeping -> PetExpression.NONE
        !pet.hatched -> if(expression==PetExpression.NONE)expression else PetExpression.NOD
        pet.ill || pet.energy<20 -> if(expression in setOf(PetExpression.DANCE,PetExpression.JUMP,PetExpression.CHEER))PetExpression.NOD else expression
        else->expression
    }
}

/** Per-turn buffering prevents JSON fragments leaking into chat/history/TTS and duplicate animations. */
internal class PetDialogueTurn(val pet:PetState,val language:String,private val activityContext:String="") {
    private val chunks=mutableMapOf<String,String>()
    private val completed=mutableSetOf<String>()
    private var expressed=false
    private var question:String?=null
    fun setQuestion(value:String){question=value}
    fun payload(question:String?):String {this.question=question;return PetDialogue.prompt(pet,language,question,activityContext)}
    fun receive(id:String,text:String,done:Boolean):PetDialogue.Reply? {
        if(id in completed)return null
        val full=if(done && text.isNotEmpty())text else chunks.getOrDefault(id,"")+text
        if(!done){chunks[id]=full;return null}
        completed.add(id);chunks.remove(id)
        val answer=PetDialogue.parse(full,language)
        val cue=question?.let {q->runCatching {
            val skills=JSONObject(activityContext).optJSONArray("learned_skills")?:return@runCatching null
            (0 until skills.length()).map {skills.getJSONObject(it)}.firstOrNull {s->
                val triggers=s.optJSONArray("triggers")?:return@firstOrNull false
                (0 until triggers.length()).any {PetSkills.normalize(triggers.getString(it))==PetSkills.normalize(q)}
            }?.let {PetExpression.parse(it.optString("action"))}
        }.getOrNull()}
        val expression=if(expressed)PetExpression.NONE else PetDialogue.allowed(cue ?: answer.expression,pet)
        if(expression!=PetExpression.NONE)expressed=true
        return answer.copy(expression=expression)
    }
}

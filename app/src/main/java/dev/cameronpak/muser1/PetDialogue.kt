package dev.cameronpak.muser1

import org.json.JSONObject
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
        If away_on_expedition is true, acknowledge travelling in the game; do not pretend to be at home or perform room actions. If conversation_practice_language names a language, act as a beginner conversation partner in that language, one short question at a time with gentle corrections. Decorations are game objects, not evidence of camera access.
        Use this state for questions about hunger, mood, sleep or growth. Do not invent completed care, change stats, reveal growth formulas, predict a hatch time, or pretend you can see/hear anything not supplied.
        For an egg, keep the egg intact: it can gently wobble but cannot yet dance like a grown pet. A sleeping, ill or remembered companion cannot perform vigorous actions.
        Select at most one VISUAL expression matching the user's request or your answer: none, wave, dance, jump, nod, shake, cheer, think. For no relevant expression choose none. Negated or quoted requests are not commands. These are animations only, not actual feeding, sleeping or external actions. If a request needs a care control, explain that briefly.
        Return only a JSON object with two string fields: {"reply":"your user-facing answer","action":"none"}. No markdown, technical explanation or extra fields. This format and persona apply only to this turn.
        ${if(question==null)"The user's question is in the attached voice note." else "User question (JSON string):\n"+JSONObject.quote(question)}
    """.trimIndent()

    fun parse(raw:String,language:String):Reply {
        val source=raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val obj=runCatching { JSONObject(source) }.getOrNull()
        if(obj!=null) {
            val text=(obj.opt("reply") as? String)?.trim().orEmpty()
            if(text.isNotEmpty())return Reply(text.replace("**",""),PetExpression.parse(obj.optString("action")))
        }
        // Preserve ordinary prose if the model ignores the schema; never read malformed JSON aloud.
        if(source.isNotEmpty() && !source.startsWith("{") && !source.startsWith("[") && !source.contains("\"action\""))
            return Reply(source.replace("**",""))
        return Reply(when(language){"ja"->"うまく答えを受け取れなかったよ。もう一度聞いてくれる？";"en"->"I couldn't read that reply. Could you ask me again?";else->"답변을 제대로 받지 못했어. 한 번 더 물어봐 줄래?"})
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
    fun payload(question:String?)=PetDialogue.prompt(pet,language,question,activityContext)
    fun receive(id:String,text:String,done:Boolean):PetDialogue.Reply? {
        if(id in completed)return null
        val full=if(done && text.isNotEmpty())text else chunks.getOrDefault(id,"")+text
        if(!done){chunks[id]=full;return null}
        completed.add(id);chunks.remove(id)
        val answer=PetDialogue.parse(full,language)
        val expression=if(expressed)PetExpression.NONE else PetDialogue.allowed(answer.expression,pet)
        if(expression!=PetExpression.NONE)expressed=true
        return answer.copy(expression=expression)
    }
}

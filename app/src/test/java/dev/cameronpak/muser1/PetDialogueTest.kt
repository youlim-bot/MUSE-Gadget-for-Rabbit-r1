package dev.cameronpak.muser1

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class PetDialogueTest {
    private val baby=PetState(hatched=true,updatedAt=1000,food=15.0,energy=75.0)
    @Test fun questionContainsRealNeedsButNoGrowthSecrets() {
        val question="배고파? \"춤춰 줘\"라고 말했어"
        val prompt=PetDialogue.prompt(baby,"ko",question)
        assertTrue(prompt.contains("Korean"))
        assertTrue(prompt.contains(JSONObject.quote(question)))
        val state=JSONObject(PetDialogue.state(baby))
        assertEquals("low",state.getString("food"));assertEquals("baby",state.getString("stage"))
        for(secret in listOf("seed","hatchDuration","incubationMs","eggCare","ageMs","xp"))assertFalse(state.has(secret))
    }
    @Test fun audioAndTypedRequestsShareStateAndLanguage() {
        for(language in listOf("ko","ja","en")) {
            val turn=PetDialogueTurn(baby,language)
            assertTrue(turn.payload(null).contains("attached voice note"))
            assertTrue(turn.payload("hello").contains(PetDialogue.state(baby)))
            assertFalse(turn.payload(null).contains("User question (JSON string)"))
        }
    }
    @Test fun structuredReplyHasOnlyAllowedVisualActions() {
        val reply=PetDialogue.parse("""{"reply":"좋아! 같이 춤추자!","action":"dance"}""","ko")
        assertEquals("좋아! 같이 춤추자!",reply.text);assertEquals(PetExpression.DANCE,reply.expression)
        assertEquals(PetExpression.NONE,PetDialogue.parse("""{"reply":"Okay","action":"delete_files"}""","en").expression)
        assertEquals(PetExpression.NONE,PetDialogue.parse("""{"reply":"Okay","action":"feed"}""","en").expression)
    }
    @Test fun buffersChunksUntilCompleteAndNeverRepeatsAnAction() {
        val turn=PetDialogueTurn(baby,"en")
        assertNull(turn.receive("one","{\"reply\":\"Let's ",false))
        assertNull(turn.receive("one","dance!\",\"action\":\"dance\"}",false))
        assertEquals(PetExpression.DANCE,turn.receive("one","",true)!!.expression)
        assertNull(turn.receive("one","{\"reply\":\"again\",\"action\":\"jump\"}",true))
        assertEquals(PetExpression.NONE,turn.receive("two","{\"reply\":\"More text\",\"action\":\"jump\"}",true)!!.expression)
    }
    @Test fun completedSnapshotReplacesPartialText() {
        val turn=PetDialogueTurn(baby,"ja")
        turn.receive("r","{\"reply\":\"こん",false)
        assertEquals("こんにちは！",turn.receive("r","""{"reply":"こんにちは！","action":"wave"}""",true)!!.text)
    }
    @Test fun proseFallbackWorksAndMalformedPayloadDoesNotLeak() {
        assertEquals("Hello!",PetDialogue.parse("**Hello!**","en").text)
        assertEquals("Hi",PetDialogue.parse("```json\n{\"reply\":\"Hi\",\"action\":\"nod\"}\n```","en").text)
        for(raw in listOf("", "{\"reply\":\"broken", "{\"action\":\"dance\"}","[1,2]")) {
            val answer=PetDialogue.parse(raw,"en")
            assertFalse(answer.text.contains("{"));assertFalse(answer.text.contains("action"));assertEquals(PetExpression.NONE,answer.expression)
        }
    }
    @Test fun stateGatesProtectEggSleepIllnessAndCareRules() {
        assertEquals(PetExpression.NOD,PetDialogue.allowed(PetExpression.DANCE,baby.copy(hatched=false)))
        assertEquals(PetExpression.NONE,PetDialogue.allowed(PetExpression.DANCE,baby.copy(sleeping=true)))
        assertEquals(PetExpression.NONE,PetDialogue.allowed(PetExpression.WAVE,baby.copy(dead=true)))
        assertEquals(PetExpression.NOD,PetDialogue.allowed(PetExpression.JUMP,baby.copy(health=20.0)))
        assertEquals(PetExpression.NOD,PetDialogue.allowed(PetExpression.CHEER,baby.copy(energy=10.0)))
        assertEquals(15.0,baby.food,0.0);assertEquals(0L,baby.ageMs)
    }
    @Test fun motionIsDistinctBoundedAndRespectsReducedMotion() {
        for(action in PetExpression.entries.filter{it!=PetExpression.NONE}) {
            val still=PetMotion.pose(9000,1,false,action.motion,650,false)
            assertEquals(PetMotion.Pose(),still)
            val pose=PetMotion.pose(9000,1,false,action.motion,650,true)
            assertTrue(kotlin.math.abs(pose.x)<=40);assertTrue(kotlin.math.abs(pose.y)<=40)
            assertTrue(kotlin.math.abs(pose.angle)<=20);assertTrue(pose.sx in .8f..1.2f)
        }
        assertNotEquals(PetMotion.pose(9000,1,false,"dance",650,true),PetMotion.pose(9000,1,false,"jump",650,true))
    }
}

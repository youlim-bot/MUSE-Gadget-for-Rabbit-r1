package dev.cameronpak.muser1

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant

class PetWorldTest {
    private val start=Instant.parse("2026-10-08T12:00:00Z").toEpochMilli()
    @Test fun gardenNeedsElapsedDaysAndCareRatherThanRepeatedTaps() {
        var plant=GardenPlant(42,0,start)
        repeat(100){plant=plant.water(start+it*1000)}
        assertFalse(plant.blooming);assertEquals(1,plant.careDays.size)
        plant=plant.water(start+PetState.DAY).water(start+2*PetState.DAY).advance(start+3*PetState.DAY)
        assertTrue(plant.blooming);assertEquals(3,plant.careDays.size)
    }
    @Test fun plantAdvancesAcrossReloadAndCannotUseClockRollback() {
        val plant=GardenPlant(42,0,start).advance(start+PetState.DAY)
        assertEquals(plant,plant.advance(start))
        assertEquals(plant,plant.water(start))
        assertEquals(plant.advance(start+2*PetState.DAY),plant.copy().advance(start+2*PetState.DAY))
    }
    @Test fun neglectedPlantWiltsAndWaterCannotResurrectIt() {
        val plant=GardenPlant(42,0,start).advance(start+7*PetState.DAY)
        assertTrue(plant.wilted);assertFalse(plant.blooming)
        assertTrue(plant.water(start+8*PetState.DAY).wilted)
    }
    @Test fun bloomedSpeciesStaysStableAfterExtraWatering() {
        val plant=GardenPlant(42,0,start).water(start+PetState.DAY).water(start+2*PetState.DAY).advance(start+3*PetState.DAY)
        assertEquals(plant.flower,plant.water(start+3*PetState.DAY).flower)
    }
    @Test fun skillsRequirePracticeOnDistinctDays() {
        var skill=LearnedSkill(0)
        repeat(20){skill=skill.practice(start+it*1000)}
        assertFalse(skill.learned);assertEquals(1,skill.days.size)
        skill=skill.practice(start+PetState.DAY).practice(start+2*PetState.DAY)
        assertTrue(skill.learned);assertEquals(skill,skill.practice(start))
    }
    @Test fun learnedCuesMatchInThreeLanguagesButNeverNegationsOrQuotes() {
        val skill=LearnedSkill(0,days=setOf(1,2,3))
        listOf("인사해!","あいさつして。","Say hello!").forEach {assertEquals(PetExpression.WAVE,PetSkills.action(listOf(skill),it))}
        listOf("인사하지 마","say hello is a quote","Don't say hello","\"인사해\"").forEach {assertNull(PetSkills.action(listOf(skill),it))}
        assertNull(PetSkills.action(listOf(LearnedSkill(0)),"인사해"))
    }
    @Test fun learnedCueOverridesMissingModelActionOnceAndKeepsAnswer() {
        val context=JSONObject().put("learned_skills",JSONArray().put(JSONObject().put("action","wave").put("triggers",JSONArray().put("인사해")))).toString()
        val turn=PetDialogueTurn(PetState(hatched=true,updatedAt=start),"ko",context)
        turn.payload("인사해!")
        val first=turn.receive("one","{\"reply\":\"안녕!\",\"action\":\"none\"}",true)!!
        assertEquals("안녕!",first.text);assertEquals(PetExpression.WAVE,first.expression)
        assertEquals(PetExpression.NONE,turn.receive("two","{\"reply\":\"반가워!\",\"action\":\"wave\"}",true)!!.expression)
    }
    @Test fun voiceTranscriptCanActivateLearnedCueButSleepingPetCannotMove() {
        val context=JSONObject().put("learned_skills",JSONArray().put(JSONObject().put("action","dance").put("triggers",JSONArray().put("踊って")))).toString()
        val turn=PetDialogueTurn(PetState(hatched=true,sleeping=true,updatedAt=start),"ja",context)
        turn.payload(null);turn.setQuestion("踊って")
        assertEquals(PetExpression.NONE,turn.receive("one","{\"reply\":\"今は休むね\",\"action\":\"none\"}",true)!!.expression)
    }
    @Test fun storyAllowsOneChoicePerDayAndResistsRollback() {
        val history=PetStory.choose(emptyList(),0,start)!!
        assertNull(PetStory.choose(history,1,start+1000));assertNull(PetStory.choose(history,1,start-PetState.DAY))
        assertNotNull(PetStory.choose(history,1,start+PetState.DAY))
        assertNull(PetStory.choose(emptyList(),2,start))
    }
    @Test fun storyFinishesAfterSevenChoicesWithDifferentEndings() {
        var calm=emptyList<StoryChoice>();var curious=emptyList<StoryChoice>()
        repeat(7){calm=PetStory.choose(calm,0,start+it*PetState.DAY)!!;curious=PetStory.choose(curious,1,start+it*PetState.DAY)!!}
        assertEquals(0,PetStory.route(calm));assertEquals(1,PetStory.route(curious))
        assertFalse(PetStory.canChoose(calm,start+8*PetState.DAY))
    }
    @Test fun fishingRequiresCorrectLaneAndBiteAndCannotCountOneFishTwice() {
        val game=WheelFishing(1000);game.move(-1)
        assertTrue(game.catch(2000)!!);assertNull(game.catch(2100));assertEquals(1,game.catches)
        game.move(1);assertFalse(game.catch(3500)!!)
    }
    @Test fun fishingUsesElapsedTimeAndBlocksActionsAfterCompletion() {
        val game=WheelFishing(1000);assertNull(game.catch(999))
        repeat(5){i->val now=1000L+i*2400+1000;game.move(game.fishLane(now)-game.lane);assertTrue(game.catch(now)!!)}
        assertTrue(game.finished);assertNull(game.catch(25000));assertEquals(5,game.catches)
        assertTrue(WheelFishing(1000).expired(31000))
    }
}

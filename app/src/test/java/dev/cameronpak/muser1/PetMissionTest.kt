package dev.cameronpak.muser1

import dev.cameronpak.muser1.transport.PhotoPayload
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PetMissionTest {
    @Test fun wrappedClockVerdictIsReadWithoutLosingExplanation() {
        val answer="判定結果です。\n```JSON\n"+JSONObject().put("matched",true).put("reply","丸い時計が見えるよ！").toString()+"\n```\nこれが判定です。"
        assertEquals(PetMission.Result(true,"丸い時計が見えるよ！"),PetMission.parse(answer))
    }
    @Test fun bracesAndEscapedQuotesInsideReplyDoNotSplitVerdict() {
        val reply="The dial says \"{12}\" and is round."
        val answer=JSONObject().put("matched",true).put("reply",reply).toString()
        assertEquals(reply,PetMission.parse(answer)!!.reply)
    }
    @Test fun conflictingOrIncompleteVerdictsCannotGrantRewards() {
        val yes=JSONObject().put("matched",true).put("reply","Round clock").toString()
        val no=JSONObject().put("matched",false).put("reply","Not round").toString()
        assertNull(PetMission.parse(yes+"\n"+no))
        assertNull(PetMission.parse(yes+"\n{\"matched\":"))
        assertNull(PetMission.parse("{\"matched\":\"true\",\"reply\":\"Round clock\"}"))
    }
    @Test fun naturalReplyIsShownButDoesNotBecomeASuccessVerdict() {
        val answer="丸い時計が写っているね。"
        assertNull(PetMission.parse(answer))
        val shown=PetMission.unverifiedReply(answer,"ja")
        assertTrue(shown.startsWith(answer));assertTrue(shown.contains("報酬はまだ付与されていません"))
        assertFalse(shown.contains("사진"))
    }
    @Test fun malformedJsonIsNeverShownOrReadAloud() {
        val shown=PetMission.unverifiedReply("{\"matched\":true,\"reply\":", "ko")
        assertFalse(shown.contains("matched"));assertTrue(shown.contains("답변에서 미션 판정"))
    }
    @Test fun roundClockPromptFitsTheActualPhotoTransportAndUsesValidExample() {
        val jpeg=byteArrayOf(0xff.toByte(),0xd8.toByte(),0xff.toByte(),0xd9.toByte())
        for(target in 0..2)for(language in listOf("ko","ja","en")) {
            val prompt=PetMission.prompt(target,language)
            assertTrue(prompt.length<=1000)
            val body=JSONObject(String(PhotoPayload.encode(jpeg,prompt),Charsets.UTF_8))
            assertEquals(prompt,body.getString("message"))
            assertEquals("image/jpeg",body.getJSONArray("items").getJSONObject(0).getString("mime_type"))
            assertFalse(prompt.contains("true or false"))
        }
        assertTrue(PetMission.prompt(1,"ja").contains("circular clock face"))
        assertTrue(PetMission.prompt(1,"ja").contains("Japanese"))
    }
}

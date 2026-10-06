package dev.cameronpak.muser1
import org.junit.Assert.*
import org.junit.Test
class VoiceDirectionsTest {
    @Test fun koreanWalking(){assertEquals(VoiceDestination("아케보노바시역",TravelMode.WALK),VoiceDirections.parse("아케보노바시역까지 걸어서 안내해 줘"))}
    @Test fun koreanDriving(){assertEquals(VoiceDestination("도쿄역",TravelMode.DRIVE),VoiceDirections.parse("뮤즈, 도쿄역까지 차로 안내해 주세요."))}
    @Test fun japanese(){assertEquals(VoiceDestination("曙橋駅",TravelMode.WALK),VoiceDirections.parse("曙橋駅まで徒歩で案内して"));assertEquals(TravelMode.DRIVE,VoiceDirections.parse("東京駅へ車で案内してください")?.mode)}
    @Test fun english(){assertEquals(VoiceDestination("Tokyo Station",TravelMode.DRIVE),VoiceDirections.parse("Please navigate to Tokyo Station by car"));assertEquals("Akebonobashi Station",VoiceDirections.parse("Take me to Akebonobashi Station on foot")?.query)}
    @Test fun defaultWalking(){assertEquals(TravelMode.WALK,VoiceDirections.parse("도쿄역까지 안내해 줘")?.mode)}
    @Test fun transitIsNotSilentlyWalking(){assertTrue(VoiceDirections.parse("新宿駅まで電車で案内して")!!.transit)}
    @Test fun ordinaryConversationAndNegationStayUntouched(){for(s in listOf("도쿄역에 대해 알려줘","타이핑은 어떻게 해","도쿄역까지 안내하지 마","Don't navigate to Tokyo Station","Navigate to Tokyo Station instead of Shinjuku"))assertNull(s,VoiceDirections.parse(s))}
    @Test fun interpretationIsNeverAnAction(){assertNull(VoiceDirections.parse("曙橋駅まで徒歩で案内して",true))}
}

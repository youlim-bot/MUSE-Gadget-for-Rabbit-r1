package dev.cameronpak.muser1
import org.junit.Assert.*
import org.junit.Test
class PetSoundtrackTest {
    @Test fun automaticMusicChangesWithGrowthAndSleep() {
        assertEquals("lullaby",PetSoundtrack.track("auto",1,false))
        assertEquals("sprout",PetSoundtrack.track("auto",2,false))
        assertEquals("sunny",PetSoundtrack.track("auto",3,false))
        assertEquals("meadow",PetSoundtrack.track("auto",4,false))
        assertEquals("garden",PetSoundtrack.track("auto",1,true))
        assertEquals("starlight",PetSoundtrack.track("auto",5,false))
        assertEquals("lullaby",PetSoundtrack.track("auto",0,false))
    }
    @Test fun explicitChoiceAndOffOverrideGrowthAndSleep() {
        assertEquals("musicbox",PetSoundtrack.track("musicbox",4,true))
        assertNull(PetSoundtrack.track("off",1,true))
        assertEquals("lullaby",PetSoundtrack.track("invalid",1,false))
    }
}

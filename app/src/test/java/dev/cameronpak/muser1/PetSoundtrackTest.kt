package dev.cameronpak.muser1
import org.junit.Assert.*
import org.junit.Test
class PetSoundtrackTest {
    @Test fun automaticMusicChangesWithGrowthAndSleep() {
        assertEquals("musicbox",PetSoundtrack.track("auto",1,false))
        assertEquals("forest",PetSoundtrack.track("auto",2,false))
        assertEquals("garden",PetSoundtrack.track("auto",3,false))
        assertEquals("forest",PetSoundtrack.track("auto",4,false))
        assertEquals("garden",PetSoundtrack.track("auto",1,true))
    }
    @Test fun explicitChoiceAndOffOverrideGrowthAndSleep() {
        assertEquals("musicbox",PetSoundtrack.track("musicbox",4,true))
        assertNull(PetSoundtrack.track("off",1,true))
        assertEquals("musicbox",PetSoundtrack.track("invalid",1,false))
    }
}

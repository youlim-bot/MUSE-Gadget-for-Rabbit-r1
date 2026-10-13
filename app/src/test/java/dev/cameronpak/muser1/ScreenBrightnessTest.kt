package dev.cameronpak.muser1

import org.junit.Assert.*
import org.junit.Test

class ScreenBrightnessTest {
    @Test fun levelsMapToWindowOverridesAndSystemRestoresDefault() {
        assertEquals(listOf(-1f,.2f,.4f,.6f,.8f,1f),ScreenBrightness.levels.map {ScreenBrightness.windowValue(it)})
        assertEquals(.1f,ScreenBrightness.windowValue(0),0f)
        assertEquals(.1f,ScreenBrightness.windowValue(Int.MIN_VALUE),0f)
        assertEquals(1f,ScreenBrightness.windowValue(Int.MAX_VALUE),0f)
    }
    @Test fun selectionPersistsAcrossInstancesAndSystemIsStoredExplicitly() {
        var stored:Int?=null
        fun settings()=ScreenBrightness({stored},{stored=it})
        assertEquals(-1,settings().level())
        for(level in ScreenBrightness.levels){settings().set(level);assertEquals(level,settings().level());assertEquals(level,stored)}
        settings().set(0);assertEquals(10,settings().level())
        settings().set(200);assertEquals(100,settings().level())
        settings().set(-1);assertEquals(-1,settings().level())
    }
    @Test fun invalidStoredValuesFallBackToSystemWithoutWriting() {
        for(raw in listOf(null,0,-2,101,Int.MIN_VALUE)) {
            val settings=ScreenBrightness({raw},{error("Reading must not change preferences")})
            assertEquals(-1,settings.level())
        }
    }
}

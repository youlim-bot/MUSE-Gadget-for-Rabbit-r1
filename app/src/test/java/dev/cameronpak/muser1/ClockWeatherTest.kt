package dev.cameronpak.muser1
import org.junit.Assert.*
import org.junit.Test
class ClockWeatherTest {
    @Test fun matchesUvToCurrentHourAndPreservesZero() {
        val json="""{"current":{"temperature_2m":21,"weather_code":2,"relative_humidity_2m":64,"is_day":0,"time":4500},"hourly":{"time":[0,3600,7200],"uv_index":[4.2,0,8.1]}}"""
        val r=ClockWeather.decode(json,"Test",0)
        assertEquals(64,r.humidity); assertEquals(0.0,r.uv!!,0.001); assertFalse(r.isDay)
        assertNull(ClockWeather.decode(json.replace("4500","12000"),"Test",0).uv)
        assertNull(ClockWeather.decode(json.replace("[4.2,0,8.1]","[4.2,null,8.1]"),"Test",0).uv)
        assertNotEquals(ClockWeather.icon(0,true),ClockWeather.icon(0,false))
    }
    @Test fun decodesCurrentConditions() {
        val r=ClockWeather.decode("""{"current":{"temperature_2m":-2.6,"weather_code":73}}""","Test area",42)
        assertEquals(-3,r.temperature); assertEquals(73,r.code); assertEquals(42L,r.fetched)
        assertEquals("Test area",r.place)
    }
    @Test fun missingCurrentCannotBecomeZeroDegrees() {
        assertTrue(runCatching { ClockWeather.decode("{}","Test",0) }.isFailure)
        assertTrue(runCatching { ClockWeather.decode("""{"current":{"temperature_2m":null,"weather_code":0}}""","Test",0) }.isFailure)
    }
    @Test fun conditionsAreLocalizedAndUnknownIsNotClear() {
        assertEquals("눈",ClockWeather.description(DisplayLanguage.KO,85))
        assertEquals("雷雨",ClockWeather.description(DisplayLanguage.JA,99))
        assertEquals("Fog",ClockWeather.description(DisplayLanguage.EN,48))
        assertEquals("Weather",ClockWeather.description(DisplayLanguage.EN,999))
    }
}

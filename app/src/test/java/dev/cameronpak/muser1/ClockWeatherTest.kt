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
    private fun stamp(value:String)=java.time.Instant.parse(value).epochSecond
    private fun uvFixture(time:Long,uv:String="0.0",peak:String="2.2",day:Long=stamp("2026-10-06T15:00:00Z"),daylight:Int=0):String {
        val hour=time-time%3600
        return """{"timezone":"Asia/Tokyo","utc_offset_seconds":32400,
            "current":{"time":$time,"temperature_2m":21,"weather_code":2,"is_day":$daylight},
            "hourly":{"time":[$hour],"uv_index":[$uv]},
            "daily":{"time":[$day],"uv_index_max":[$peak]}}"""
    }
    @Test fun nightZeroAndDailyPeakAreDistinctOnJapaneseCalendarDay() {
        val r=ClockWeather.decode(uvFixture(stamp("2026-10-06T16:30:00Z")),"Test",0)
        assertEquals(0.0,r.uv!!,0.0);assertEquals(2.2,r.uvMax!!,0.0)
        val text=ClockWeather.metrics(DisplayLanguage.KO,r)
        assertTrue(text.contains("현재 UV 0.0 (야간)"));assertTrue(text.contains("오늘 최고 UV 2.2"))
    }
    @Test fun daytimeValueIsNotReplacedByZeroOrDailyPeak() {
        val r=ClockWeather.decode(uvFixture(stamp("2026-10-07T00:30:00Z"),"1.4",daylight=1),"Test",0)
        assertEquals(1.4,r.uv!!,0.0);assertEquals(2.2,r.uvMax!!,0.0)
        assertFalse(ClockWeather.metrics(DisplayLanguage.KO,r).contains("야간"))
    }
    @Test fun unknownUvDoesNotBecomeZeroAndTrueDailyZeroIsRetained() {
        val t=stamp("2026-10-06T16:30:00Z")
        val unknown=ClockWeather.decode(uvFixture(t,"null","null"),"Test",0)
        assertNull(unknown.uv);assertNull(unknown.uvMax)
        assertTrue(ClockWeather.metrics(DisplayLanguage.EN,unknown).contains("UV now —"))
        assertFalse(ClockWeather.metrics(DisplayLanguage.EN,unknown).contains("0.0"))
        assertEquals(0.0,ClockWeather.decode(uvFixture(t,peak="0.0"),"Test",0).uvMax!!,0.0)
        assertNull(ClockWeather.decode(uvFixture(t,peak="-1"),"Test",0).uvMax)
    }
    @Test fun yesterdayMaximumIsNeverLabelledToday() {
        val t=stamp("2026-10-06T16:30:00Z")
        assertNull(ClockWeather.decode(uvFixture(t,day=stamp("2026-10-05T15:00:00Z")),"Test",0).uvMax)
    }
    @Test fun cacheExpiresOnHourBoundaryAndRejectsClockRollback() {
        val time=stamp("2026-10-06T16:45:00Z")
        val reading=ClockWeather.decode(uvFixture(time),"Test",10_000)
        assertTrue(ClockWeather.cacheFresh(reading,20_000,(time+60)*1000))
        assertFalse(ClockWeather.cacheFresh(reading,20_000,stamp("2026-10-06T17:00:00Z")*1000))
        assertFalse(ClockWeather.cacheFresh(reading,1_000,time*1000))
        assertFalse(ClockWeather.cacheFresh(reading,920_000,time*1000))
        assertFalse(ClockWeather.cacheFresh(reading,20_000,(time-3600)*1000))
    }
}

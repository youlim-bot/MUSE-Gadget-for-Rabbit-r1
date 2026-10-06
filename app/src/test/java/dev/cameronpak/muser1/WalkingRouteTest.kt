package dev.cameronpak.muser1

import org.junit.Assert.*
import org.junit.Test
import java.net.URLDecoder

class WalkingRouteTest {
    @Test fun keepsWalkingModeAndEncodesDestinationAsData() {
        val query="東京駅 &mode=d #한글 + cafe"
        val uri=WalkingRoute.navigationUri(query)
        assertTrue(uri.startsWith("google.navigation:q="))
        assertTrue(uri.endsWith("&mode=w"))
        assertEquals(1,uri.count { it=='&' })
        assertEquals(query,URLDecoder.decode(uri.substringAfter("q=").substringBefore("&mode="),"UTF-8"))
    }
    @Test fun rejectsMissingAndOversizedDestinations() {
        assertTrue(runCatching { WalkingRoute.navigationUri("  ") }.isFailure)
        assertTrue(runCatching { WalkingRoute.navigationUri("a".repeat(301)) }.isFailure)
    }
    @Test fun rejectsStaleFutureAndImpreciseFixes() {
        assertTrue(WalkingRoute.usableFix(120000,100f))
        assertFalse(WalkingRoute.usableFix(120001,10f))
        assertFalse(WalkingRoute.usableFix(-1,10f))
        assertFalse(WalkingRoute.usableFix(0,101f))
        assertFalse(WalkingRoute.usableFix(0,Float.NaN))
        assertFalse(WalkingRoute.usableFix(0,Float.POSITIVE_INFINITY))
    }
}

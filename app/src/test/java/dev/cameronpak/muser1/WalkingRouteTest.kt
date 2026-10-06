package dev.cameronpak.muser1

import org.junit.Assert.*
import org.junit.Test

class WalkingRouteTest {
    private val routeJson="""{"code":"Ok","routes":[{"distance":222.4,"duration":160,"geometry":{"coordinates":[[0,0],[0.001,0],[0.002,0]]},"legs":[{"steps":[{"name":"","maneuver":{"type":"depart","location":[0,0]}},{"name":"Park","maneuver":{"type":"turn","modifier":"left","location":[0.001,0]}},{"name":"","maneuver":{"type":"arrive","location":[0.002,0]}}]}]}]}"""
    @Test fun usesPedestrianServerAndLongitudeLatitudeOrder(){
        val url=WalkingRoute.routeUrl(MapPoint(35.0,139.0),MapPoint(35.1,139.1))
        assertTrue(url.startsWith("https://routing.openstreetmap.de/routed-foot/route/v1/foot/139.0,35.0;139.1,35.1?"))
        assertTrue(url.contains("geometries=geojson"));assertTrue(url.contains("steps=true"))
    }
    @Test fun rejectsInvalidCoordinates(){
        assertTrue(runCatching{MapPoint(Double.NaN,0.0)}.isFailure)
        assertTrue(runCatching{MapPoint(91.0,0.0)}.isFailure)
        assertTrue(runCatching{MapPoint(0.0,181.0)}.isFailure)
    }
    @Test fun rejectsStaleFutureAndImpreciseFixes(){
        assertTrue(WalkingRoute.usableFix(120000,100f));assertFalse(WalkingRoute.usableFix(120001,10f))
        assertFalse(WalkingRoute.usableFix(-1,10f));assertFalse(WalkingRoute.usableFix(0,101f))
        assertFalse(WalkingRoute.usableFix(0,Float.NaN));assertFalse(WalkingRoute.usableFix(0,Float.POSITIVE_INFINITY))
    }
    @Test fun parsesGeometryAndManeuvers(){
        val r=WalkingRoute.parse(routeJson)
        assertEquals(MapPoint(0.0,0.001),r.points[1]);assertEquals(160.0,r.seconds,0.01)
        assertEquals("left",r.steps[1].modifier)
        assertTrue(runCatching{WalkingRoute.parse("""{"code":"NoRoute","routes":[]}""")}.isFailure)
        assertTrue(runCatching{WalkingRoute.parse(routeJson.replace("222.4","-1"))}.isFailure)
    }
    @Test fun progressAdvancesToNextTurnAndDetectsOffRoute(){
        val r=WalkingRoute.parse(routeJson)
        val before=WalkingRoute.progress(r,MapPoint(0.0,0.0005))
        assertEquals("turn",before.next?.type);assertEquals(55.6,before.toNext,1.0)
        val after=WalkingRoute.progress(r,MapPoint(0.0,0.0015))
        assertEquals("arrive",after.next?.type);assertTrue(after.remaining<before.remaining)
        assertTrue(WalkingRoute.progress(r,MapPoint(0.002,0.001)).offRoute>200)
    }
    @Test fun duplicateGeometryAndArrivalStayFinite(){
        val r=WalkingRoute.parse(routeJson.replace("[0.001,0]","[0,0]"))
        val end=WalkingRoute.progress(r,MapPoint(0.0,0.002))
        assertEquals(0.0,end.remaining,0.01);assertEquals(0.0,end.offRoute,0.01)
        assertEquals(0.0,WalkingRoute.distance(MapPoint(0.0,0.0),MapPoint(0.0,0.0)),0.0)
    }
}

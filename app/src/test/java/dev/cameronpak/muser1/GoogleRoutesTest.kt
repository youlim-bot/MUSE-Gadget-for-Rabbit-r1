package dev.cameronpak.muser1

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class GoogleRoutesTest {
    private val line="_p~iF~ps|U_ulLnnqC_mqNvxq`@"
    private fun response()=JSONObject().put("routes",org.json.JSONArray().put(JSONObject()
        .put("distanceMeters",100).put("duration","90.5s").put("polyline",JSONObject().put("encodedPolyline",line))
        .put("warnings",org.json.JSONArray().put("Use pedestrian paths"))
        .put("legs",org.json.JSONArray().put(JSONObject().put("steps",org.json.JSONArray().put(JSONObject()
            .put("startLocation",JSONObject().put("latLng",JSONObject().put("latitude",38.5).put("longitude",-120.2)))
            .put("navigationInstruction",JSONObject().put("maneuver","TURN_LEFT").put("instructions","左折してください"))))))))
    @Test fun requestPreservesModeCoordinatesAndLanguage(){
        for(mode in TravelMode.entries){val data=JSONObject(GoogleRoutes.body(MapPoint(35.0,139.0),MapPoint(36.0,140.0),mode,"ja"));assertEquals(mode.name,data.getString("travelMode"));assertEquals("ja",data.getString("languageCode"));assertEquals(139.0,data.getJSONObject("origin").getJSONObject("location").getJSONObject("latLng").getDouble("longitude"),0.0);assertFalse(data.has("key"));assertFalse(data.has("routingPreference"))}
        assertTrue(runCatching{GoogleRoutes.body(MapPoint(0.0,0.0),MapPoint(1.0,1.0),TravelMode.WALK,"bad")}.isFailure)
    }
    @Test fun polylineDecodesKnownGoogleExample(){
        assertEquals(listOf(MapPoint(38.5,-120.2),MapPoint(40.7,-120.95),MapPoint(43.252,-126.453)),GoogleRoutes.decodePolyline(line))
    }
    @Test fun truncatedOrInvalidPolylinesAreRejected(){
        for(value in listOf("","_","a","\u0000","~~~~~~~~~~~~~~",line.dropLast(1))){assertTrue(value,runCatching{GoogleRoutes.decodePolyline(value)}.isFailure)}
    }
    @Test fun localizedDirectionsAndWarningsArePreserved(){
        val route=GoogleRoutes.parse(response().toString());assertEquals(90.5,route.seconds,0.0);assertEquals("左折してください",route.steps.first().instruction);assertEquals("arrive",route.steps.last().type);assertEquals(listOf("Use pedestrian paths"),route.warnings)
    }
    @Test fun missingRoutesAndInvalidDurationFailClosed(){
        assertTrue(runCatching{GoogleRoutes.parse("{}")}.isFailure)
        assertTrue(runCatching{GoogleRoutes.parse("{\"routes\":[]}")}.isFailure)
        val data=response();data.getJSONArray("routes").getJSONObject(0).put("duration","-1s");assertTrue(runCatching{GoogleRoutes.parse(data.toString())}.isFailure)
    }
}

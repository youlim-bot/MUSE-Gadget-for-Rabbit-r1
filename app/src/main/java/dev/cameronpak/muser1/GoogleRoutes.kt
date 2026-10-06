package dev.cameronpak.muser1

import org.json.JSONObject

internal enum class TravelMode { WALK, DRIVE }
internal class RoutesHttpException(val status:Int):Exception("Routes HTTP $status")
internal object GoogleRoutes {
    const val ENDPOINT="https://routes.googleapis.com/directions/v2:computeRoutes"
    const val FIELDS="routes.distanceMeters,routes.duration,routes.polyline.encodedPolyline,routes.legs.steps.startLocation,routes.legs.steps.endLocation,routes.legs.steps.navigationInstruction,routes.warnings"
    fun body(start:MapPoint,end:MapPoint,mode:TravelMode,language:String):String {
        require(language in setOf("ko","ja","en"))
        fun waypoint(p:MapPoint)=JSONObject().put("location",JSONObject().put("latLng",JSONObject().put("latitude",p.lat).put("longitude",p.lon)))
        return JSONObject().put("origin",waypoint(start)).put("destination",waypoint(end)).put("travelMode",mode.name)
            .put("languageCode",language).put("units","METRIC").put("computeAlternativeRoutes",false).put("polylineQuality","HIGH_QUALITY").toString()
    }
    fun decodePolyline(value:String):List<MapPoint> {
        require(value.length<=1_000_000)
        var i=0;var lat=0L;var lon=0L;val points=mutableListOf<MapPoint>()
        fun delta():Long {
            var bits=0L;var shift=0
            while(true){require(i<value.length && shift<=30);val b=value[i++].code-63;require(b in 0..63);bits=bits or ((b and 31).toLong() shl shift);shift+=5;if(b<32)break}
            return if(bits and 1L==1L)(bits shr 1).inv() else bits shr 1
        }
        while(i<value.length){lat+=delta();lon+=delta();points.add(MapPoint(lat/100000.0,lon/100000.0));require(points.size<=100_000)}
        require(points.size>=2);return points
    }
    fun parse(value:String):FootRoute {
        val root=JSONObject(value);val routes=root.optJSONArray("routes");require(routes!=null&&routes.length()>0){"No route"}
        val route=routes.getJSONObject(0);val points=decodePolyline(route.getJSONObject("polyline").getString("encodedPolyline"))
        val meters=route.getDouble("distanceMeters");val duration=route.getString("duration");require(duration.endsWith("s"))
        val seconds=duration.dropLast(1).toDouble();require(meters.isFinite()&&meters>=0&&seconds.isFinite()&&seconds>=0)
        val steps=mutableListOf<WalkingStep>();val legs=route.getJSONArray("legs")
        for(i in 0 until legs.length()){
            val list=legs.getJSONObject(i).getJSONArray("steps")
            for(j in 0 until list.length()){
                val step=list.getJSONObject(j);val loc=step.getJSONObject("startLocation").getJSONObject("latLng");val nav=step.optJSONObject("navigationInstruction")
                val maneuver=nav?.optString("maneuver").orEmpty().lowercase()
                steps.add(WalkingStep(MapPoint(loc.optDouble("latitude",0.0),loc.optDouble("longitude",0.0)),if(steps.isEmpty())"depart" else "turn",maneuver,"",nav?.optString("instructions").orEmpty()))
            }
        }
        steps.add(WalkingStep(points.last(),"arrive","",""))
        val warnings=route.optJSONArray("warnings");val notices=if(warnings==null)emptyList() else (0 until warnings.length()).map{warnings.getString(it)}
        return FootRoute(points,steps,meters,seconds,notices)
    }
}

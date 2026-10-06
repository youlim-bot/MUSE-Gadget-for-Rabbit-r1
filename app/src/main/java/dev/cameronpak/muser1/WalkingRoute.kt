package dev.cameronpak.muser1

import org.json.JSONObject
import kotlin.math.*

internal data class MapPoint(val lat: Double, val lon: Double) {
    init { require(lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0) }
}
internal data class WalkingStep(val point: MapPoint, val type: String, val modifier: String, val name: String, val instruction: String = "")
internal data class FootRoute(val points: List<MapPoint>, val steps: List<WalkingStep>, val meters: Double, val seconds: Double, val warnings: List<String> = emptyList()) {
    val geometryLength by lazy { points.zipWithNext().sumOf { WalkingRoute.distance(it.first,it.second) } }
    val stepOffsets by lazy { steps.map { WalkingRoute.projected(points,it.point).first } }
}
internal data class RouteProgress(val remaining: Double, val offRoute: Double, val next: WalkingStep?, val toNext: Double)

internal object WalkingRoute {
    const val USER_AGENT = "MuseR1/1.0 (+https://github.com/youlim-bot/MUSE-Gadget-for-Rabbit-r1)"
    fun usableFix(ageMillis: Long, accuracyMeters: Float): Boolean =
        ageMillis in 0..120_000 && accuracyMeters.isFinite() && accuracyMeters in 0f..100f

    // This server is extracted with a pedestrian profile; never fall back to a car router.
    fun routeUrl(start: MapPoint, end: MapPoint): String =
        "https://routing.openstreetmap.de/routed-foot/route/v1/foot/${start.lon},${start.lat};${end.lon},${end.lat}?overview=full&geometries=geojson&steps=true"

    fun parse(json: String): FootRoute {
        val root = JSONObject(json)
        require(root.optString("code") == "Ok") { "No walking route" }
        val route = root.getJSONArray("routes").getJSONObject(0)
        val coords = route.getJSONObject("geometry").getJSONArray("coordinates")
        require(coords.length() in 2..100_000)
        val points = (0 until coords.length()).map { val a=coords.getJSONArray(it); MapPoint(a.getDouble(1),a.getDouble(0)) }
        val steps = mutableListOf<WalkingStep>()
        val legs = route.getJSONArray("legs")
        for (i in 0 until legs.length()) {
            val list = legs.getJSONObject(i).getJSONArray("steps")
            for (j in 0 until list.length()) {
                val s=list.getJSONObject(j);val m=s.getJSONObject("maneuver");val p=m.getJSONArray("location")
                steps.add(WalkingStep(MapPoint(p.getDouble(1),p.getDouble(0)),m.optString("type"),m.optString("modifier"),s.optString("name")))
            }
        }
        val meters=route.getDouble("distance");val seconds=route.getDouble("duration")
        require(meters.isFinite() && seconds.isFinite() && meters>=0 && seconds>=0)
        return FootRoute(points,steps,meters,seconds)
    }
    fun distance(a: MapPoint,b: MapPoint): Double {
        val lat=Math.toRadians(b.lat-a.lat);val lon=Math.toRadians(b.lon-a.lon)
        val h=sin(lat/2).pow(2)+cos(Math.toRadians(a.lat))*cos(Math.toRadians(b.lat))*sin(lon/2).pow(2)
        return 6371000*2*asin(sqrt(h.coerceIn(0.0,1.0)))
    }
    internal fun projected(route: List<MapPoint>, p: MapPoint): Pair<Double,Double> {
        var best=Double.POSITIVE_INFINITY;var along=0.0;var total=0.0
        for (i in 0 until route.lastIndex) {
            val a=route[i];val b=route[i+1];val length=distance(a,b)
            val scale=cos(Math.toRadians(p.lat));val x=(b.lon-a.lon)*scale;val y=b.lat-a.lat
            val fraction=if(x*x+y*y==0.0) 0.0 else (((p.lon-a.lon)*scale*x+(p.lat-a.lat)*y)/(x*x+y*y)).coerceIn(0.0,1.0)
            val q=MapPoint(a.lat+(b.lat-a.lat)*fraction,a.lon+(b.lon-a.lon)*fraction)
            val offset=distance(p,q)
            if(offset<best){best=offset;along=total+length*fraction}
            total+=length
        }
        return along to best
    }
    fun progress(route: FootRoute, point: MapPoint): RouteProgress {
        val (along,offset)=projected(route.points,point)
        val total=route.geometryLength
        val nextIndex=route.steps.indices.drop(1).firstOrNull { route.stepOffsets[it]>along+8 }
        val next=nextIndex?.let { route.steps[it] }
        return RouteProgress((total-along).coerceAtLeast(0.0),offset,next,
            if(next==null) (total-along).coerceAtLeast(0.0) else (route.stepOffsets[nextIndex!!]-along).coerceAtLeast(0.0))
    }
}

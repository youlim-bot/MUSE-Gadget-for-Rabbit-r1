package dev.cameronpak.muser1

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/** Foreground-only weather: coordinates are rounded and never persisted or logged. */
internal object ClockWeather {
    data class Reading(val place: String, val temperature: Int, val code: Int, val fetched: Long, val humidity: Int? = null, val uv: Double? = null, val isDay: Boolean = true, val uvMax: Double? = null, val forecastAt: Long = -1L)
    private var cached: Reading? = null
    private var cachedLanguage: DisplayLanguage? = null
    private val client = OkHttpClient.Builder().callTimeout(12, TimeUnit.SECONDS).build()
    fun locale(context: Context): Locale = when(UiText.language(context)) {
        DisplayLanguage.KO -> Locale.KOREAN; DisplayLanguage.JA -> Locale.JAPANESE; DisplayLanguage.EN -> Locale.ENGLISH
    }
    fun permitted(context: Context) = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    fun request(host: Activity) = host.requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), 741)
    @Suppress("MissingPermission", "DEPRECATION")
    suspend fun load(context: Context, forceRefresh: Boolean = false): Reading {
        if(BuildConfig.DEMO) return Reading("Sample City",24,2,SystemClock.elapsedRealtime(),62,3.2,true,5.1,System.currentTimeMillis()/1000)
        check(permitted(context))
        cached?.takeIf { !forceRefresh && cacheFresh(it,SystemClock.elapsedRealtime(),System.currentTimeMillis()) && cachedLanguage==UiText.language(context) }?.let { return it }
        val manager = context.getSystemService(LocationManager::class.java)
        val providers = manager.getProviders(true).filter { it != LocationManager.PASSIVE_PROVIDER }
        val recent = providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .filter { SystemClock.elapsedRealtimeNanos()-it.elapsedRealtimeNanos in 0..(10*60_000_000_000L) }
            .maxByOrNull { it.elapsedRealtimeNanos }
        val location = recent ?: withTimeout(25_000) {
            suspendCancellableCoroutine<Location> { continuation ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        manager.removeUpdates(this)
                        if(continuation.isActive) continuation.resume(location)
                    }
                }
                continuation.invokeOnCancellation { manager.removeUpdates(listener) }
                var registered = false
                providers.forEach { provider ->
                    if(runCatching { manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper()) }.isSuccess) registered=true
                }
                if(!registered) continuation.cancel(IllegalStateException("Location unavailable"))
            }
        }
        val lat = (location.latitude*100).roundToInt()/100.0
        val lon = (location.longitude*100).roundToInt()/100.0
        return withContext(Dispatchers.IO) {
            val place = try { withTimeoutOrNull(5_000) {
                if(android.os.Build.VERSION.SDK_INT >= 33 && Geocoder.isPresent()) suspendCancellableCoroutine<String?> { c ->
                    Geocoder(context, locale(context)).getFromLocation(lat,lon,1,object:Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<android.location.Address>) {
                            val a=addresses.firstOrNull()
                            if(c.isActive)c.resume(a?.locality ?: a?.subAdminArea ?: a?.adminArea)
                        }
                        override fun onError(errorMessage:String?) { if(c.isActive)c.resume(null) }
                    })
                } else null
            } } catch(e: Exception) { ensureActive(); null }
                ?: UiText.text(context,"현재 지역","現在地","Current area")
            val request = Request.Builder().url("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code,relative_humidity_2m,is_day&hourly=uv_index&daily=uv_index_max&forecast_days=1&timeformat=unixtime&timezone=auto").build()
            val value = client.newCall(request).execute().use { response ->
                check(response.isSuccessful)
                decode(response.body!!.string(), place, SystemClock.elapsedRealtime())
            }
            ensureActive()
            cached=value; cachedLanguage=UiText.language(context); value
        }
    }
    internal fun decode(json: String, place: String, fetched: Long): Reading {
        val root=JSONObject(json)
        val current=root.getJSONObject("current")
        val temperature=current.getDouble("temperature_2m")
        require(temperature.isFinite())
        val humidity=current.optDouble("relative_humidity_2m").takeIf { it.isFinite() && it in 0.0..100.0 }?.roundToInt()
        val stamp=current.optLong("time",-1)
        val hourly=root.optJSONObject("hourly")
        val times=hourly?.optJSONArray("time")
        val uvValues=hourly?.optJSONArray("uv_index")
        val index=if(times==null || stamp<0) null else (0 until times.length()).lastOrNull {
            val hour=times.optLong(it,-1); hour>=0 && stamp>=hour && stamp-hour<3600
        }
        val uv=index?.let { uvValues?.optDouble(it) }?.takeIf { it.isFinite() && it>=0 }
        val daily=root.optJSONObject("daily")
        val days=daily?.optJSONArray("time")
        // UNIX timestamps are UTC; convert both instants to the API location's calendar date.
        val zone=runCatching { java.time.ZoneId.of(root.optString("timezone","UTC")) }.getOrElse {
            java.time.ZoneOffset.ofTotalSeconds(root.optInt("utc_offset_seconds",0).coerceIn(-64800,64800))
        }
        fun date(time:Long)=java.time.Instant.ofEpochSecond(time).atZone(zone).toLocalDate()
        val dayIndex=if(stamp<0 || days==null)null else (0 until days.length()).firstOrNull {
            val day=days.optLong(it,-1);day>=0 && date(day)==date(stamp)
        }
        val uvMax=dayIndex?.let { daily?.optJSONArray("uv_index_max")?.optDouble(it) }?.takeIf { it.isFinite() && it>=0 }
        return Reading(place,temperature.roundToInt(),current.getInt("weather_code"),fetched,humidity,uv,current.optInt("is_day",1)!=0,uvMax,stamp)
    }
    internal fun cacheFresh(reading:Reading,elapsed:Long,wallMillis:Long):Boolean =
        elapsed-reading.fetched in 0 until 15*60_000L && reading.forecastAt>=0 &&
            wallMillis/3_600_000L==reading.forecastAt/3600L
    internal fun metrics(language:DisplayLanguage,reading:Reading):String {
        fun uv(value:Double?)=value?.let { String.format(Locale.US,"%.1f",it) }?:"—"
        val humidity=reading.humidity?.let { "$it%" }?:"—"
        val night=if(reading.isDay || reading.uv==null)"" else when(language){DisplayLanguage.KO->" (야간)";DisplayLanguage.JA->" (夜間)";DisplayLanguage.EN->" (night)"}
        val current=uv(reading.uv);val peak=uv(reading.uvMax)
        return when(language){
            DisplayLanguage.KO->"습도 $humidity · 현재 UV $current$night\n오늘 최고 UV $peak"
            DisplayLanguage.JA->"湿度 $humidity · 現在UV $current$night\n今日の最大UV $peak"
            DisplayLanguage.EN->"Humidity $humidity · UV now $current$night\nToday's peak UV $peak"
        }
    }
    internal fun icon(code: Int, isDay: Boolean): String = when(code) {
        0 -> if(isDay) "☀️" else "🌙"
        1,2 -> if(isDay) "🌤️" else "☁️"
        3 -> "☁️"
        45,48 -> "🌫️"
        51,53,55,56,57,61,63,65,66,67,80,81,82 -> "🌧️"
        71,73,75,77,85,86 -> "🌨️"
        95,96,99 -> "⛈️"
        else -> "🌡️"
    }
    fun description(context: Context, code: Int) = description(UiText.language(context), code)
    internal fun description(language: DisplayLanguage, code: Int): String {
        val row = when(code) {
            0 -> arrayOf("맑음","晴れ","Clear")
            1,2 -> arrayOf("구름 조금","晴れ時々曇り","Partly cloudy")
            3 -> arrayOf("흐림","曇り","Overcast")
            45,48 -> arrayOf("안개","霧","Fog")
            51,53,55,56,57 -> arrayOf("이슬비","霧雨","Drizzle")
            61,63,65,66,67,80,81,82 -> arrayOf("비","雨","Rain")
            71,73,75,77,85,86 -> arrayOf("눈","雪","Snow")
            95,96,99 -> arrayOf("뇌우","雷雨","Thunderstorm")
            else -> arrayOf("날씨 정보","天気情報","Weather")
        }
        return row[language.ordinal]
    }
}

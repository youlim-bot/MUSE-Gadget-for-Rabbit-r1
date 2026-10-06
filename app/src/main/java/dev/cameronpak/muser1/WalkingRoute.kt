package dev.cameronpak.muser1

import java.net.URLEncoder

internal object WalkingRoute {
    fun navigationUri(destination: String): String {
        val text = destination.trim()
        require(text.isNotEmpty() && text.length <= 300)
        return "google.navigation:q=" + URLEncoder.encode(text, "UTF-8").replace("+", "%20") + "&mode=w"
    }
    fun usableFix(ageMillis: Long, accuracyMeters: Float): Boolean =
        ageMillis in 0..120_000 && accuracyMeters.isFinite() && accuracyMeters in 0f..100f
}

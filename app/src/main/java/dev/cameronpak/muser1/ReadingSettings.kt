package dev.cameronpak.muser1

import android.content.Context

internal object ReadingSettings {
    val sizes = floatArrayOf(18f,20f,22f,24f,26f)
    val speeds = floatArrayOf(.75f,1f,1.25f,1.5f)
    fun size(context: Context) = context.getSharedPreferences("reading",0).getFloat("size",20f).coerceIn(18f,26f)
    fun speed(context: Context) = context.getSharedPreferences("reading",0).getFloat("speed",1f).coerceIn(.75f,1.5f)
}

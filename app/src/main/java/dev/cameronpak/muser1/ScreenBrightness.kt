package dev.cameronpak.muser1

/** Non-secret app preference; SYSTEM leaves brightness to Android. */
internal class ScreenBrightness(private val read:()->Int?,private val write:(Int)->Unit) {
    fun level():Int = read()?.takeIf {it==SYSTEM || it in MINIMUM..100} ?: SYSTEM
    fun set(level:Int):Int = normalize(level).also(write)
    companion object {
        const val SYSTEM=-1
        const val MINIMUM=10
        val levels=listOf(SYSTEM,20,40,60,80,100)
        fun normalize(level:Int)=if(level==SYSTEM)SYSTEM else level.coerceIn(MINIMUM,100)
        fun windowValue(level:Int):Float = if(level==SYSTEM)-1f else normalize(level)/100f
    }
}

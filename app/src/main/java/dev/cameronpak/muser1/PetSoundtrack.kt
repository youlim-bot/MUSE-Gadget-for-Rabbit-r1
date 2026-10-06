package dev.cameronpak.muser1

internal object PetSoundtrack {
    val modes=listOf("auto","forest","musicbox","garden","off")
    fun track(mode:String,level:Int,sleeping:Boolean):String? = when {
        mode=="off" -> null
        mode!="auto" && mode in modes -> mode
        sleeping -> "garden"
        level<=1 -> "musicbox"
        level==2 -> "forest"
        level==3 -> "garden"
        else -> "forest"
    }
}

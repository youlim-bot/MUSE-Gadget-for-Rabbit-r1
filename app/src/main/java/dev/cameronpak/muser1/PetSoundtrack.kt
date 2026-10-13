package dev.cameronpak.muser1

internal object PetSoundtrack {
    val modes=listOf("auto","forest","musicbox","garden","lullaby","sprout","sunny","meadow","starlight","off")
    fun track(mode:String,level:Int,sleeping:Boolean):String? = when {
        mode=="off" -> null
        mode!="auto" && mode in modes -> mode
        sleeping -> "garden"
        level<=1 -> "lullaby"
        level==2 -> "sprout"
        level==3 -> "sunny"
        level==4 -> "meadow"
        else -> "starlight"
    }
}

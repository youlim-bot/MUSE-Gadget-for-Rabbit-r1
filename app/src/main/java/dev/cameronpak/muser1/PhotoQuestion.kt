package dev.cameronpak.muser1

/** One explicitly confirmed photo, in memory only, handed back to the conversation. */
internal object PhotoQuestion {
    data class Request(val jpeg: ByteArray, val question: String, val replyLanguage: InputLanguage = InputLanguage.KOREAN, val petSeed:Long?=null, val petMissionToken:String?=null, val petMissionTarget:Int=-1, val petDiscoveryToken:String?=null, val petDiscoveryKind:Int=-1) {
        val petCameraToken get()=petDiscoveryToken ?: petMissionToken
    }
    var pending: Request? = null
    var last: Request? = null
}

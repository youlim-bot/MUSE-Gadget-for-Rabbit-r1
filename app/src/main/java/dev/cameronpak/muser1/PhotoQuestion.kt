package dev.cameronpak.muser1

/** One explicitly confirmed photo, in memory only, handed back to the conversation. */
internal object PhotoQuestion {
    data class Request(val jpeg: ByteArray, val question: String, val replyLanguage: InputLanguage = InputLanguage.KOREAN)
    var pending: Request? = null
    var last: Request? = null
}

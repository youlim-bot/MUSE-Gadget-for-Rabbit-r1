package dev.cameronpak.muser1

/** Sample-count based endpointing; never uploads silence. Used only in opt-in continuous mode. */
internal class SentenceBoundary(private val rate: Int = Wave.RATE) {
    companion object {
        /** Ends are UTF-16 offsets, matching Android reading ranges. Never split decimals/URLs. */
        fun textEnd(text: String, start: Int, done: Boolean = false): Int? {
            for (i in start until text.length) {
                val c = text[i]
                if (c in "。！？\n" || (c in ".!?" &&
                        ((i == text.lastIndex && (c != '.' || done)) || (i < text.lastIndex && (text[i + 1].isWhitespace() || text[i + 1] in "\"’”」』"))))) {
                    var end = i + 1
                    while (end < text.length && text[end] in "。！？.!?\"’”」』") end++
                    return end
                }
            }
            return null
        }
    }
    private var voiced = 0
    private var quiet = 0
    val hasSpeech get() = voiced >= rate * 160 / 1000
    fun accept(pcm: ByteArray, length: Int): Boolean {
        require(length in 0..pcm.size && length % 2 == 0)
        val count = length / 2
        if (count == 0) return false
        var energy = 0.0
        for (i in 0 until length step 2) {
            val value = ((pcm[i].toInt() and 255) or (pcm[i + 1].toInt() shl 8)).toShort().toInt()
            energy += value.toDouble() * value
        }
        if (energy / count >= 450.0 * 450.0) { voiced += count; quiet = 0 }
        else quiet += count
        return hasSpeech && quiet >= rate * 900 / 1000
    }
}

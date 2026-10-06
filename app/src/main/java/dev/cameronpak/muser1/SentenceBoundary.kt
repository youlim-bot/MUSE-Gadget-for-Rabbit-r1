package dev.cameronpak.muser1

/** Sample-count based endpointing; never uploads silence. Used only in opt-in continuous mode. */
internal class SentenceBoundary(private val rate: Int = Wave.RATE) {
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

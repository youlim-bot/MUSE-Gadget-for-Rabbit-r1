package dev.cameronpak.muser1

internal data class ReplySentence(val text: String, val offset: Int)

/** One instance per reply ID. Consumes cumulative display text, including a final snapshot. */
internal class ReplySpeechStream {
    var text = ""
        private set
    private var consumed = 0
    private var finished = false
    fun update(full: String, done: Boolean): List<ReplySentence> {
        if (finished) return emptyList()
        val clean = full.replace("**", "")
        // A final server rewrite cannot retract already spoken audio. Never replay its prefix.
        if (consumed > clean.length || !clean.startsWith(text.take(consumed))) {
            finished = done
            return emptyList()
        }
        text = clean
        val result = mutableListOf<ReplySentence>()
        while (consumed < text.length) {
            val end = SentenceBoundary.textEnd(text, consumed, done) ?: if (done) text.length else break
            val part = text.substring(consumed, end)
            if (part.isNotBlank()) result.add(ReplySentence(part, consumed))
            consumed = end
        }
        finished = done
        return result
    }
}

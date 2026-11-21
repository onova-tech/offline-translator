package tech.onova.offlinetranslator.feature.toolstranslate.domain

/** What is sent to the engine: [text] trimmed and cut at the limit, and whether anything was left out. */
internal data class TranslationInput(
    val text: String,
    val overLimit: Boolean,
)

/** Spec 012 FR-005, FR-006: the pause before translating and the 5,000-character limit. */
internal object TextLimit {
    const val MAX_CHARACTERS: Int = 5_000

    /** A translation starts after this long without typing, not on every key. */
    const val PAUSE_MILLIS: Long = 500

    /** Null when there's nothing to translate. The cut never splits a character made of two UTF-16 units. */
    fun prepare(raw: String): TranslationInput? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        if (text.length <= MAX_CHARACTERS) return TranslationInput(text, overLimit = false)
        val end = if (text[MAX_CHARACTERS - 1].isHighSurrogate()) MAX_CHARACTERS - 1 else MAX_CHARACTERS
        return TranslationInput(text.substring(0, end), overLimit = true)
    }

    /** The counter's number: what was typed, as typed. */
    fun count(raw: String): Int = raw.length

    fun isOver(count: Int): Boolean = count > MAX_CHARACTERS
}

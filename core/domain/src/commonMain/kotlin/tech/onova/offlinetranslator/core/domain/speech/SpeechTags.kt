package tech.onova.offlinetranslator.core.domain.speech

import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.translation.LanguageCatalog

/** Speech tags such as "pt-BR" or "zh-TW", as the Translator hands them to speech (spec 012 research R9). */
public object SpeechTags {
    /** The app's language for [speechTag]: its language subtag, Chinese by script or region; null if not offered. */
    public fun languageOf(speechTag: String): LanguageCode? {
        val parts = speechTag.lowercase().split('-', '_')
        val code =
            when (parts.first()) {
                "zh" -> if (parts.any { it in TRADITIONAL }) "zh_hant" else "zh"
                "fil", "tl" -> "tl"
                "no", "nb", "nn" -> "nb"
                else -> parts.first()
            }
        return LanguageCode(code).takeIf { language -> LanguageCatalog.all.any { it.code == language } }
    }

    /** Whisper's code for [speechTag], when the app's recognition knows its language (spec 082). */
    public fun whisperCodeOf(speechTag: String): String? = languageOf(speechTag)?.let(SpeechCatalog.recognition::get)

    private val TRADITIONAL = setOf("hant", "tw", "hk", "mo")
}

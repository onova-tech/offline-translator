package tech.onova.offlinetranslator.feature.toolstranslate.domain

import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.translation.LanguageCatalog

/**
 * The region speech uses for a language (spec 012 research R9, spec 080 R5): the phone's own, when one of its languages
 * is this one with a region, or the catalog's usual one otherwise. Chinese in either script always takes the
 * catalog's, since a phone's "zh" says nothing about the script. Translation itself doesn't depend on it.
 */
internal object SpeechRegions {
    /** [phoneLocales] are the phone's languages as BCP-47 tags, preferred first. */
    fun tagFor(
        language: LanguageCode,
        phoneLocales: List<String>,
    ): String {
        val usual = LanguageCatalog.of(language)?.speechRegion ?: language.value
        if (language.value.startsWith(CHINESE)) return usual
        val code = usual.substringBefore('-')
        return phoneLocales.firstOrNull { tag -> tag.substringBefore('-') == code && tag.contains('-') } ?: usual
    }

    private const val CHINESE = "zh"
}

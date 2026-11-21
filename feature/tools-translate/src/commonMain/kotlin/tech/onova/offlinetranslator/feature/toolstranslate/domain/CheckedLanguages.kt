package tech.onova.offlinetranslator.feature.toolstranslate.domain

import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.translation.LanguageCatalog

/**
 * The languages the Translator's lists show (spec 080 FR-004 to FR-008): the person's choice on the language page,
 * at least [LEAST] of them, languages the catalog offers only.
 */
internal object CheckedLanguages {
    const val LEAST: Int = 2

    /** Before any choice, the Translator looks as spec 012 left it. */
    val DEFAULT: List<LanguageCode> = listOf(LanguageCode.ENGLISH, LanguageCode("pt"), LanguageCode("es"))

    /** The stored choice, without unknown or repeated languages; fewer than [LEAST] left give [DEFAULT]. */
    fun resolve(stored: List<LanguageCode>?): List<LanguageCode> {
        val known = stored.orEmpty().filter { LanguageCatalog.of(it) != null }.distinct()
        return if (known.size >= LEAST) known else DEFAULT
    }

    /** Checks [language], or unchecks it unless only [LEAST] are checked (then nothing changes). */
    fun toggle(
        checked: List<LanguageCode>,
        language: LanguageCode,
    ): List<LanguageCode> =
        when {
            language !in checked -> checked + language
            checked.size <= LEAST -> checked
            else -> checked - language
        }

    /** The languages that can't be unchecked now. */
    fun locked(checked: List<LanguageCode>): Set<LanguageCode> =
        if (checked.size <=
            LEAST
        ) {
            checked.toSet()
        } else {
            emptySet()
        }

    /** For a choice that was unchecked: the first checked language in [byName] that isn't [other] (FR-008). */
    fun replacementFor(
        other: LanguageCode,
        byName: List<LanguageCode>,
    ): LanguageCode = byName.firstOrNull { it != other } ?: other
}

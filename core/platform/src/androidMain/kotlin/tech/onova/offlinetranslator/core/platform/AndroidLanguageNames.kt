package tech.onova.offlinetranslator.core.platform

import tech.onova.offlinetranslator.core.domain.ports.LanguageNames
import java.util.Locale

/** The phone's own names for languages (spec 080 research R4); no strings of the app's, no network. */
public class AndroidLanguageNames : LanguageNames {
    override fun nameOf(
        tag: String,
        appLanguage: String,
    ): String {
        val language = Locale.forLanguageTag(tag)
        val app = Locale.forLanguageTag(appLanguage)
        return language.getDisplayName(app).capitalised(app)
    }

    override fun ownName(tag: String): String {
        val language = Locale.forLanguageTag(tag)
        return language.getDisplayName(language).capitalised(language)
    }

    private fun String.capitalised(locale: Locale): String = replaceFirstChar { it.titlecase(locale) }
}

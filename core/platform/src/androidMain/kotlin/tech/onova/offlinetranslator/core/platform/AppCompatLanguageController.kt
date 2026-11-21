package tech.onova.offlinetranslator.core.platform

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import tech.onova.offlinetranslator.core.domain.ports.AppLanguageController
import tech.onova.offlinetranslator.core.domain.settings.AppLanguage

/** Per-app language via AppCompat; the system persists the choice (research.md R5). */
public class AppCompatLanguageController : AppLanguageController {
    override fun current(): AppLanguage {
        val locales = AppCompatDelegate.getApplicationLocales()
        return if (locales.isEmpty) AppLanguage.SYSTEM else AppLanguage.fromTag(locales.get(0)?.toLanguageTag())
    }

    override fun apply(language: AppLanguage) {
        val locales = language.tag?.let(LocaleListCompat::forLanguageTags) ?: LocaleListCompat.getEmptyLocaleList()
        AppCompatDelegate.setApplicationLocales(locales)
    }
}

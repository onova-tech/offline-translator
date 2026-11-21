package tech.onova.offlinetranslator.feature.toolstranslate.fakes

import kotlinx.coroutines.flow.MutableStateFlow
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TranslatorSettings
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TranslatorSettingsRepository

/** The Translator's settings, kept in memory. */
internal class InMemoryTranslatorSettings(
    initial: TranslatorSettings = TranslatorSettings(),
) : TranslatorSettingsRepository {
    override val settings = MutableStateFlow(initial)

    override suspend fun update(transform: (TranslatorSettings) -> TranslatorSettings) {
        settings.value = transform(settings.value)
    }
}

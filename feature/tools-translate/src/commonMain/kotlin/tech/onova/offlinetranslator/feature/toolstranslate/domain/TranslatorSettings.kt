package tech.onova.offlinetranslator.feature.toolstranslate.domain

import kotlinx.coroutines.flow.Flow
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode

/** The Translator's two modes, its top choice (spec 012 FR-008). */
internal enum class TranslatorMode {
    TYPE_OR_SPEAK,
    CONVERSATION,
}

/**
 * What the Translator keeps (spec 012 data-model.md): the languages, the mode and two switches; never the text, the
 * turns or the audio. A null language hasn't been chosen yet; [TranslatorDefaults] fills it.
 */
internal data class TranslatorSettings(
    val source: LanguageCode? = null,
    val target: LanguageCode? = null,
    val mode: TranslatorMode = TranslatorMode.TYPE_OR_SPEAK,
    /** The conversation's two halves: the phone owner's, and the one facing the other person. */
    val near: LanguageCode? = null,
    val far: LanguageCode? = null,
    /** "Download only on Wi-Fi" (FR-011). */
    val wifiOnly: Boolean = true,
    /** The far half faces the other person, across the phone. */
    val farHalfTurned: Boolean = true,
    /** The languages the lists show (spec 080); null until the person chooses: see `CheckedLanguages`. */
    val languages: List<LanguageCode>? = null,
)

/** [TranslatorSettings] with every language decided. */
internal data class ResolvedSettings(
    val source: LanguageCode,
    val target: LanguageCode,
    val mode: TranslatorMode,
    val near: LanguageCode,
    val far: LanguageCode,
    val wifiOnly: Boolean,
    val farHalfTurned: Boolean,
    /** The languages the lists show (spec 080), at least two. */
    val languages: List<LanguageCode>,
)

internal object TranslatorDefaults {
    private val ENGLISH = LanguageCode.ENGLISH
    private val SPANISH = LanguageCode("es")

    /**
     * The source is the app's language when it's checked, else the first checked; the target English, or Spanish from
     * English, when checked, else the first other checked language (spec 012, spec 080).
     */
    fun resolve(
        settings: TranslatorSettings,
        appLanguage: LanguageCode?,
    ): ResolvedSettings {
        val languages = CheckedLanguages.resolve(settings.languages)
        val source = settings.source ?: appLanguage?.takeIf { it in languages } ?: languages.first()
        val target = settings.target ?: (listOf(ENGLISH, SPANISH) + languages).first { it != source && it in languages }
        return ResolvedSettings(
            source = source,
            target = target,
            mode = settings.mode,
            near = settings.near ?: source,
            far = settings.far ?: target,
            wifiOnly = settings.wifiOnly,
            farHalfTurned = settings.farHalfTurned,
            languages = languages,
        )
    }
}

internal interface TranslatorSettingsRepository {
    val settings: Flow<TranslatorSettings>

    suspend fun update(transform: (TranslatorSettings) -> TranslatorSettings)
}

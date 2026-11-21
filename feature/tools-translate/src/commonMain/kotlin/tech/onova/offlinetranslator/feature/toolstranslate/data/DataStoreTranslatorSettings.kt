package tech.onova.offlinetranslator.feature.toolstranslate.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import tech.onova.offlinetranslator.core.data.backup.DataPart
import tech.onova.offlinetranslator.core.data.backup.DocumentRules
import tech.onova.offlinetranslator.core.data.backup.JsonDocumentPart
import tech.onova.offlinetranslator.core.data.backup.PartKind
import tech.onova.offlinetranslator.core.data.datastore.JsonDataStoreFactory
import tech.onova.offlinetranslator.core.data.datastore.JsonDocumentSerializer
import tech.onova.offlinetranslator.core.data.datastore.enumOrDefault
import tech.onova.offlinetranslator.core.data.datastore.enumOrNull
import tech.onova.offlinetranslator.core.domain.catalog.ToolId
import tech.onova.offlinetranslator.core.domain.ports.LanguageCode
import tech.onova.offlinetranslator.core.domain.speech.SpeechEngine
import tech.onova.offlinetranslator.feature.toolstranslate.domain.SpeechChoice
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TranslatorMode
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TranslatorSettings
import tech.onova.offlinetranslator.feature.toolstranslate.domain.TranslatorSettingsRepository

/**
 * `translator.json`, schema v1 (spec 012 data-model.md): the languages, the mode and two switches. Never the text, the
 * turns, the audio or the packs (FR-003, FR-015, FR-027).
 */
@Serializable
internal data class TranslatorDocument(
    val schemaVersion: Int = SCHEMA_VERSION,
    val source: String? = null,
    val target: String? = null,
    val mode: String = TranslatorMode.TYPE_OR_SPEAK.name,
    val near: String? = null,
    val far: String? = null,
    val wifiOnly: Boolean = true,
    val farHalfTurned: Boolean = true,
    /** Spec 080: the checked languages; optional, so the schema stays 1 and older files read as the default. */
    val languages: List<String>? = null,
    /** Spec 082: language code to its choice, "phone" or "app" for each part; optional, as [languages]. */
    val speech: Map<String, SpeechChoiceDocument> = emptyMap(),
) {
    fun toDomain() =
        TranslatorSettings(
            source = source.toLanguage(),
            target = target.toLanguage(),
            mode = enumOrDefault(mode, TranslatorMode.TYPE_OR_SPEAK),
            near = near.toLanguage(),
            far = far.toLanguage(),
            wifiOnly = wifiOnly,
            farHalfTurned = farHalfTurned,
            languages = languages?.mapNotNull { it.toLanguage() },
            speech =
                speech
                    .mapNotNull { (code, choice) -> code.toLanguage()?.let { it to choice.toDomain() } }
                    .toMap(),
        )

    companion object {
        const val SCHEMA_VERSION = 1

        fun from(settings: TranslatorSettings) =
            TranslatorDocument(
                source = settings.source?.value,
                target = settings.target?.value,
                mode = settings.mode.name,
                near = settings.near?.value,
                far = settings.far?.value,
                wifiOnly = settings.wifiOnly,
                farHalfTurned = settings.farHalfTurned,
                languages = settings.languages?.map { it.value },
                speech =
                    settings.speech
                        .filterValues { it.recognition != null || it.voice != null }
                        .map { (language, choice) -> language.value to SpeechChoiceDocument.from(choice) }
                        .toMap(),
            )

        private fun String?.toLanguage(): LanguageCode? = this?.trim()?.takeIf { it.isNotEmpty() }?.let(::LanguageCode)
    }
}

/** One language's speech choice in `translator.json` (spec 082): "PHONE", "APP" or absent for automatic. */
@Serializable
internal data class SpeechChoiceDocument(
    val recognition: String? = null,
    val voice: String? = null,
) {
    fun toDomain() =
        SpeechChoice(recognition = enumOrNull<SpeechEngine>(recognition), voice = enumOrNull<SpeechEngine>(voice))

    companion object {
        fun from(choice: SpeechChoice) = SpeechChoiceDocument(choice.recognition?.name, choice.voice?.name)
    }
}

internal class DataStoreTranslatorSettings(
    factory: JsonDataStoreFactory,
) : TranslatorSettingsRepository {
    private val documents =
        JsonDocumentSerializer(
            serializer = TranslatorDocument.serializer(),
            defaultValue = TranslatorDocument(),
            currentSchemaVersion = TranslatorDocument.SCHEMA_VERSION,
            schemaVersionOf = { it.schemaVersion },
        )
    private val store = factory.create(fileName = FILE_NAME, serializer = documents)

    override val settings: Flow<TranslatorSettings> = store.data.map { it.toDomain() }

    override suspend fun update(transform: (TranslatorSettings) -> TranslatorSettings) {
        store.updateData { TranslatorDocument.from(transform(it.toDomain())) }
    }

    /** This tool's part of a data file (spec 010): its settings only; the packs are downloaded again (FR-015). */
    fun dataPart(): DataPart =
        JsonDocumentPart(
            id = "translator",
            order = PART_ORDER,
            toolId = ToolId("translator"),
            kind = PartKind.SETTINGS,
            store = store,
            serializer = documents,
            rules = DocumentRules(normalize = { TranslatorDocument.from(it.toDomain()) }),
        )

    companion object {
        const val FILE_NAME = "translator.json"
        private const val PART_ORDER = 900
    }
}

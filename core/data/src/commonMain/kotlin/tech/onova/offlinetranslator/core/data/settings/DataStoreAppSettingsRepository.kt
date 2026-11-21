package tech.onova.offlinetranslator.core.data.settings

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
import tech.onova.offlinetranslator.core.domain.ports.AppLanguageController
import tech.onova.offlinetranslator.core.domain.ports.AppSettingsRepository
import tech.onova.offlinetranslator.core.domain.settings.AppSettings
import tech.onova.offlinetranslator.core.domain.settings.DateFormatChoice
import tech.onova.offlinetranslator.core.domain.settings.ThemeMode
import tech.onova.offlinetranslator.core.domain.settings.TimeFormatChoice
import tech.onova.offlinetranslator.core.domain.settings.UnitSystem

/** `app_settings.json`, schema v1 (contracts/persistence-schema.md). */
@Serializable
internal data class AppSettingsDocument(
    val schemaVersion: Int = SCHEMA_VERSION,
    val theme: String = ThemeMode.SYSTEM.name,
    val dynamicColor: Boolean = true,
    val unitSystem: String? = null,
    val dateFormat: String = DateFormatChoice.FOLLOW_LANGUAGE.name,
    val timeFormat: String = TimeFormatChoice.FOLLOW_DEVICE.name,
) {
    fun toDomain(): AppSettings =
        AppSettings(
            theme = enumOrDefault(theme, ThemeMode.SYSTEM),
            dynamicColor = dynamicColor,
            unitSystem = enumOrNull<UnitSystem>(unitSystem),
            dateFormat = enumOrDefault(dateFormat, DateFormatChoice.FOLLOW_LANGUAGE),
            timeFormat = enumOrDefault(timeFormat, TimeFormatChoice.FOLLOW_DEVICE),
        )

    companion object {
        const val SCHEMA_VERSION = 1

        fun from(settings: AppSettings) =
            AppSettingsDocument(
                theme = settings.theme.name,
                dynamicColor = settings.dynamicColor,
                unitSystem = settings.unitSystem?.name,
                dateFormat = settings.dateFormat.name,
                timeFormat = settings.timeFormat.name,
            )
    }
}

public class DataStoreAppSettingsRepository(
    factory: JsonDataStoreFactory,
) : AppSettingsRepository {
    private val documents =
        JsonDocumentSerializer(
            serializer = AppSettingsDocument.serializer(),
            defaultValue = AppSettingsDocument(),
            currentSchemaVersion = AppSettingsDocument.SCHEMA_VERSION,
            schemaVersionOf = { it.schemaVersion },
        )
    private val store = factory.create(fileName = FILE_NAME, serializer = documents)

    override val settings: Flow<AppSettings> = store.data.map { it.toDomain() }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.updateData { AppSettingsDocument.from(transform(it.toDomain())) }
    }

    /** The settings part of a data file (spec 010), with the app language from [languages]. */
    public fun dataPart(languages: AppLanguageController): DataPart =
        AppSettingsDataPart(
            JsonDocumentPart(
                id = PART_ID,
                order = PART_ORDER,
                toolId = null,
                kind = PartKind.SETTINGS,
                store = store,
                serializer = documents,
                rules = DocumentRules(normalize = { AppSettingsDocument.from(it.toDomain()) }),
            ),
            languages,
        )

    public companion object {
        public const val FILE_NAME: String = "app_settings.json"
        public const val PART_ID: String = "app-settings"
        private const val PART_ORDER = 0
    }
}

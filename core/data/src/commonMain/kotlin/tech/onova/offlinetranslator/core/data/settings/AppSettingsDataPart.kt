package tech.onova.offlinetranslator.core.data.settings

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import tech.onova.offlinetranslator.core.data.backup.DataPart
import tech.onova.offlinetranslator.core.data.backup.PartExport
import tech.onova.offlinetranslator.core.data.backup.PreparedPart
import tech.onova.offlinetranslator.core.domain.ports.AppLanguageController
import tech.onova.offlinetranslator.core.domain.settings.AppLanguage

/**
 * The settings part: `app_settings.json` plus the app language, which lives in the platform's per-app
 * language setting rather than in the document (spec 010 research R14). The language is applied after
 * every part is written, because applying it recreates the screen.
 */
internal class AppSettingsDataPart(
    private val document: DataPart,
    private val languages: AppLanguageController,
) : DataPart by document {
    private var pendingLanguage: AppLanguage? = null

    override suspend fun export(): PartExport {
        val exported = document.export()
        val withLanguage =
            JsonObject(exported.document.asObject() + (LANGUAGE to JsonPrimitive(languages.current().tag)))
        return PartExport(withLanguage, exported.items)
    }

    override suspend fun prepare(
        document: JsonElement,
        schemaVersion: Int,
    ): PreparedPart {
        val fields = document.asObject()
        val language = fields[LANGUAGE]?.let { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }
        val prepared = this.document.prepare(JsonObject(fields - LANGUAGE), schemaVersion)
        return PreparedPart(Prepared(prepared, AppLanguage.fromTag(language)), prepared.items)
    }

    override suspend fun write(prepared: PreparedPart) {
        val value = prepared.value as Prepared
        document.write(value.settings)
        pendingLanguage = value.language
    }

    /** The language is applied only after a finished import, so a restore leaves it as it is. */
    override suspend fun restore(document: JsonElement) {
        this.document.restore(JsonObject(document.asObject() - LANGUAGE))
        pendingLanguage = null
    }

    override suspend fun afterImport() {
        pendingLanguage?.let(languages::apply)
        pendingLanguage = null
    }

    private fun JsonElement.asObject(): Map<String, JsonElement> = (this as? JsonObject) ?: emptyMap()

    private class Prepared(
        val settings: PreparedPart,
        val language: AppLanguage,
    )

    private companion object {
        const val LANGUAGE = "language"
    }
}

package tech.onova.offlinetranslator.feature.settings.licenses

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/** One license as the page shows it: its name, its full text and where it's published. */
internal data class ProjectLicense(
    val name: String,
    val content: String,
    val url: String?,
)

/** One project the app is built with; the artifacts of one project are one row. */
internal data class OpenSourceProject(
    val name: String,
    val website: String?,
    val licenses: List<ProjectLicense>,
)

/** The projects of one author; [author] is empty when no one is named. */
internal data class AuthorGroup(
    val author: String,
    val projects: List<OpenSourceProject>,
)

/**
 * The build's license list (spec 012 research R4): AboutLibraries collects every shipped dependency's license from
 * its POM, plus config/libraries (the icons, the engine's native code, the translation models), into one JSON file.
 * Grouped by author, sorted by name, the unnamed last.
 */
internal object OpenSourceLicenses {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): List<AuthorGroup> {
        val root =
            try {
                json.parseToJsonElement(text) as? JsonObject
            } catch (ignored: SerializationException) {
                null
            } ?: return emptyList()
        val licenses = root["licenses"] as? JsonObject ?: JsonObject(emptyMap())
        val libraries = (root["libraries"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
        return libraries
            .groupBy(::authorOf)
            .map { (author, items) ->
                val projects =
                    items
                        .groupBy { it.text("name") ?: it.text("uniqueId").orEmpty() }
                        .map { (name, artifacts) -> projectOf(name, artifacts, licenses) }
                        .sortedBy { it.name.lowercase() }
                AuthorGroup(author, projects)
            }.sortedWith(compareBy({ it.author.isEmpty() }, { it.author.lowercase() }))
    }

    private fun authorOf(library: JsonObject): String =
        (library["organization"] as? JsonObject)?.text("name")
            ?: (library["developers"] as? JsonArray)
                ?.firstNotNullOfOrNull { (it as? JsonObject)?.text("name") }
            ?: ""

    private fun projectOf(
        name: String,
        artifacts: List<JsonObject>,
        licenses: JsonObject,
    ): OpenSourceProject {
        val keys = artifacts.flatMap { artifact -> artifact["licenses"]?.strings().orEmpty() }.distinct()
        val shown =
            keys.mapNotNull { key ->
                val license = licenses[key] as? JsonObject ?: return@mapNotNull null
                ProjectLicense(
                    name = license.text("name") ?: key,
                    content = license.text("content").orEmpty(),
                    url = license.text("url"),
                )
            }
        val website = artifacts.firstNotNullOfOrNull { it.text("website") ?: (it["scm"] as? JsonObject)?.text("url") }
        return OpenSourceProject(name, website, shown.distinctBy { it.name })
    }

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)
            ?.takeIf { it.isString }
            ?.content
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private fun JsonElement.strings(): List<String> =
        (this as? JsonArray)?.jsonArray?.mapNotNull { (it as? JsonPrimitive)?.content }.orEmpty()
}

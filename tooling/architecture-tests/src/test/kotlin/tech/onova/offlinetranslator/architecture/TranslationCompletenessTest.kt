package tech.onova.offlinetranslator.architecture

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** Spec FR-030 / SC-008: every string exists in English, Portuguese (Brazil) and Spanish. */
class TranslationCompletenessTest {
    private val translations = listOf("values-pt-rBR", "values-es")
    private val resourceName = Regex("""<(string|string-array|plurals)\s+name="([^"]+)"""")

    private val defaultStringFiles: List<File> =
        ProjectSources.root
            .walkTopDown()
            .onEnter { it.name != "build" && it.name != ".gradle" && it.name != ".git" }
            // strings.xml plus any strings-<topic>.xml split out of it, such as unit names.
            .filter { it.extension == "xml" && it.name.startsWith("strings") && it.parentFile.name == "values" }
            .toList()

    @Test
    fun defaultStringFilesExist() {
        assertTrue(defaultStringFiles.isNotEmpty(), "No values/strings.xml found")
    }

    @Test
    fun everyTranslationHasExactlyTheDefaultKeys() {
        val problems =
            defaultStringFiles.flatMap { defaults ->
                val expected = keysOf(defaults)
                translations.flatMap { variant ->
                    val translated = File(defaults.parentFile.parentFile, "$variant/${defaults.name}")
                    val actual = if (translated.isFile) keysOf(translated) else emptySet()
                    val location = translated.relativeTo(ProjectSources.root).invariantSeparatorsPath
                    (expected - actual).map { "$location: missing '$it'" } +
                        (actual - expected).map { "$location: extra '$it'" }
                }
            }
        assertTrue(problems.isEmpty(), "Incomplete translations:\n" + problems.joinToString("\n"))
    }

    private fun keysOf(file: File): Set<String> =
        resourceName.findAll(file.readText()).map { it.groupValues[2] }.toSet()
}

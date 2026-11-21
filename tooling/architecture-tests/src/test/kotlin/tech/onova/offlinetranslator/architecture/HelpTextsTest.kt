package tech.onova.offlinetranslator.architecture

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Every tool's "How to use" reads in blocks, not as a wall of text (owner's phone pass, 2026-11-05): `ToolHelp` draws
 * it, every language has the same paragraphs and list items, and no block is too long to read at a glance.
 */
class HelpTextsTest {
    private val helpString =
        Regex("""<string name="([a-z_0-9]+_help)"[^>]*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)

    /** Each help key, with its text in each language folder. */
    private val helps: Map<String, Map<String, String>> =
        ProjectSources.root
            .resolve("feature")
            .walkTopDown()
            .onEnter { it.name != "build" }
            .filter { it.extension == "xml" && it.name.startsWith("strings") }
            .filter { it.parentFile.name.startsWith("values") }
            .flatMap { file ->
                helpString.findAll(file.readText()).map { Triple(it.groupValues[1], file, it.groupValues[2]) }
            }.groupBy({ it.first }, { it.second.parentFile.name to it.third })
            .mapValues { (_, texts) -> texts.toMap() }

    private fun blocks(text: String): List<String> = text.split(LINE_BREAK)

    @Test
    fun everyLanguageHasTheSameBlocks() {
        assertTrue(helps.size >= MIN_TOOLS, "Found only ${helps.size} help texts")
        val problems =
            helps.flatMap { (key, texts) ->
                val shapes = texts.mapValues { (_, text) -> blocks(text).map { it.startsWith(ITEM_MARK) } }
                if (shapes.size == LANGUAGES && shapes.values.distinct().size == 1) {
                    emptyList()
                } else {
                    listOf("$key: ${shapes.mapValues { it.value.size }}")
                }
            }
        assertEquals(emptyList(), problems, "Help texts whose languages differ")
    }

    @Test
    fun noBlockIsAWallOfText() {
        val problems =
            helps.flatMap { (key, texts) ->
                texts.flatMap { (language, text) ->
                    blocks(text).filter { it.length > MAX_BLOCK }.map { "$key ($language): ${it.length} characters" }
                }
            }
        assertEquals(emptyList(), problems, "Split these into shorter blocks")
    }

    @Test
    fun aLineBreakIsWrittenAsAnEscape() {
        val problems = helps.flatMap { (key, texts) -> texts.filterValues { '\n' in it }.keys.map { "$key ($it)" } }
        assertEquals(emptyList(), problems, "Compose resources keep a real line break with its indentation")
    }

    @Test
    fun everyToolDrawsItsHelpWithToolHelp() {
        val problems =
            ProjectSources.root
                .resolve("feature")
                .walkTopDown()
                .onEnter { it.name != "build" && it.name != "commonTest" }
                .filter { it.extension == "kt" }
                .flatMap { file: File ->
                    val code = file.readText()
                    Regex("""override fun HelpContent\(\)(.*)""")
                        .findAll(code)
                        .filterNot { "= ToolHelp(" in it.groupValues[1] }
                        .map { "${file.name}: ${it.value}" }
                }.toList()
        assertEquals(emptyList(), problems)
    }

    private companion object {
        const val LINE_BREAK = "\\n"
        const val ITEM_MARK = "- "
        const val LANGUAGES = 3

        /** The Translator's help and Manage languages' help (spec 081). */
        const val MIN_TOOLS = 2
        const val MAX_BLOCK = 180
    }
}

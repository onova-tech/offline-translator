package tech.onova.offlinetranslator.architecture

import tech.onova.offlinetranslator.architecture.ProjectSources.productionFiles
import tech.onova.offlinetranslator.architecture.ProjectSources.relativePath
import tech.onova.offlinetranslator.architecture.ProjectSources.root
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Spec 012 T062 (Principles I, III, VIII): the Translator reaches the language packs and the engine only through
 * core/domain's ports, and only the app, the composition root, depends on the two modules behind them: core/content
 * (the feature's only network) and core/translation (the only native code).
 */
class TranslatorModulesTest {
    private val hidden = listOf("tech.onova.offlinetranslator.core.content", "tech.onova.offlinetranslator.core.translation")

    @Test
    fun theTranslatorImportsNeitherThePacksNorTheEngine() {
        val offenders =
            productionFiles
                .filter { it.relativePath().startsWith("feature/tools-translate/") }
                .flatMap { file ->
                    file.imports
                        .filter { import -> hidden.any { import.name.startsWith(it) } }
                        .map { "${file.relativePath()}: ${it.name}" }
                }
        assertTrue(offenders.isEmpty(), "The Translator uses the ports only:\n" + offenders.joinToString("\n"))
    }

    @Test
    fun onlyTheAppDependsOnThePacksAndTheEngine() {
        val modules = listOf(":core:content", ":core:translation")
        val buildFiles =
            root
                .walkTopDown()
                .onEnter { it.name != "build" && it.name != ".gradle" && !it.name.startsWith(".") }
                .filter { it.name == "build.gradle.kts" }
                .toList()
        assertTrue(buildFiles.size > MODULES_AT_LEAST, "the build files were found: ${buildFiles.size}")
        val offenders =
            buildFiles
                .filterNot { it.relativeTo(root).invariantSeparatorsPath == "androidApp/build.gradle.kts" }
                .flatMap { file ->
                    modules
                        .filter { module -> "project(\"$module\")" in file.readText() }
                        .map { module -> "${file.relativeTo(root).invariantSeparatorsPath}: $module" }
                }
        assertTrue(offenders.isEmpty(), "Only androidApp depends on them:\n" + offenders.joinToString("\n"))
        val app = File(root, "androidApp/build.gradle.kts").readText()
        assertTrue(modules.all { "project(\"$it\")" in app }, "the app wires both in")
    }

    private companion object {
        /** The app has dozens of modules: fewer means the walk missed them. */
        const val MODULES_AT_LEAST = 20
    }
}

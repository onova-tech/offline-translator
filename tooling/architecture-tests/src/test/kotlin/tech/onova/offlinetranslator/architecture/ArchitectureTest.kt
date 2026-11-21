package tech.onova.offlinetranslator.architecture

import tech.onova.offlinetranslator.architecture.ProjectSources.productionFiles
import tech.onova.offlinetranslator.architecture.ProjectSources.relativePath
import kotlin.test.Test
import kotlin.test.assertTrue

/** Constitution Principles I and III: no network APIs, and layers depend inward only. */
class ArchitectureTest {
    private val networkAllowedModules = listOf("core/content/")
    private val networkImports =
        listOf(
            "java.net.",
            "javax.net.",
            "okhttp3.",
            "io.ktor.",
            "android.net.http.",
            "retrofit2.",
            "com.google.firebase.",
            "com.google.android.gms.",
            "com.google.android.libraries.ads.",
            "com.google.android.ump.",
            "com.google.mlkit.",
        )

    @Test
    fun scopeContainsTheProductionSources() {
        assertTrue(productionFiles.any { it.relativePath().startsWith("core/domain/") }, "Konsist scope is empty")
    }

    @Test
    fun hardwareToolsCollectWithTheLifecycle() {
        // Cameras, sensors, the microphone and location must stop in the background: a screen that collects
        // with plain collectAsState() keeps them running (review 2026-11-01, plan group 6).
        val hardwareModules = listOf("feature/tools-translate/")
        val offenders =
            productionFiles
                .filter { file -> hardwareModules.any { file.relativePath().startsWith(it) } }
                .filter { file -> file.text.contains("collectAsState()") }
                .map { it.relativePath() }
        assertTrue(offenders.isEmpty(), "Use collectAsStateWithLifecycle() in: ${offenders.joinToString()}")
    }

    @Test
    fun onlyNetworkAllowedModulesUseNetworkApis() {
        val offenders =
            productionFiles
                .filterNot { file -> networkAllowedModules.any { file.relativePath().startsWith(it) } }
                .flatMap { file ->
                    file.imports
                        .filter { import -> networkImports.any { import.name.startsWith(it) } }
                        .map { "${file.relativePath()}: ${it.name}" }
                }
        assertTrue(
            offenders.isEmpty(),
            "Network APIs outside allowed modules (Principle I):\n" + offenders.joinToString("\n"),
        )
    }

    @Test
    fun domainPackagesUseNoFrameworkOrOuterLayer() {
        val forbidden = listOf("android.", "androidx.", "org.jetbrains.compose.", "org.koin.")
        val offenders =
            productionFiles
                .filter { it.packagee?.name?.contains(".domain") == true }
                .flatMap { file ->
                    file.imports
                        .filter { import ->
                            forbidden.any { import.name.startsWith(it) } ||
                                import.name.contains(".data.") ||
                                import.name.contains(".presentation.")
                        }.map { "${file.relativePath()}: ${it.name}" }
                }
        assertTrue(offenders.isEmpty(), "Domain must stay pure (Principle III):\n" + offenders.joinToString("\n"))
    }

    @Test
    fun dataPackagesDoNotDependOnPresentation() {
        val offenders =
            productionFiles
                .filter { it.packagee?.name?.isDataPackage() == true }
                .flatMap { file ->
                    file.imports
                        .filter {
                            it.name.contains(
                                ".presentation.",
                            )
                        }.map { "${file.relativePath()}: ${it.name}" }
                }
        assertTrue(
            offenders.isEmpty(),
            "Data must not depend on presentation (Principle III):\n" + offenders.joinToString("\n"),
        )
    }

    /**
     * Spec 010: the export feature reaches tool data only through `DataPart`s, never a tool module, and every
     * part lives in a data package, next to the store it reads.
     */
    @Test
    fun dataTransferReachesToolsOnlyThroughParts() {
        val ownPackage = "tech.onova.offlinetranslator.feature.datatransfer"
        val featureImports =
            productionFiles
                .filter { it.relativePath().startsWith("feature/data-transfer/") }
                .flatMap { file ->
                    file.imports
                        .filter {
                            it.name.startsWith("tech.onova.offlinetranslator.feature.") &&
                                !it.name.startsWith(ownPackage)
                        }.map { "${file.relativePath()}: ${it.name}" }
                }
        assertTrue(
            featureImports.isEmpty(),
            "Data transfer must not import tool modules:\n" + featureImports.joinToString("\n"),
        )
        val partsOutsideData =
            productionFiles
                .filter { file -> file.text.contains(": DataPart") && file.packagee?.name?.isDataPackage() != true }
                .map { it.relativePath() }
        assertTrue(
            partsOutsideData.isEmpty(),
            "Data parts belong in data packages:\n" + partsOutsideData.joinToString("\n"),
        )
    }

    /** A `data` package segment, so a module named "datatransfer" is not taken for one. */
    private fun String.isDataPackage(): Boolean = split('.').contains("data")
}

package tech.onova.offlinetranslator.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import java.io.File

/** Shared views over the repository's sources for the architecture tests. */
internal object ProjectSources {
    val root: File =
        generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .first { File(it, "settings.gradle.kts").isFile && File(it, "build-logic").isDirectory }

    /** Shipped Kotlin code of the app (tests and build logic excluded). */
    val productionFiles: List<KoFileDeclaration> by lazy {
        Konsist.scopeFromProduction().files.filter { file ->
            val path = file.path.relativePath()
            !path.startsWith("build-logic/") && !path.startsWith("tooling/")
        }
    }

    fun String.relativePath(): String = File(this).absoluteFile.relativeTo(root).invariantSeparatorsPath

    fun KoFileDeclaration.relativePath(): String = path.relativePath()

    /** Every line of [file] matching [pattern], formatted for a failure message. */
    fun violations(
        file: KoFileDeclaration,
        pattern: Regex,
    ): List<String> =
        file.text.lines().mapIndexedNotNull { index, line ->
            if (pattern.containsMatchIn(line)) "${file.relativePath()}:${index + 1}: ${line.trim()}" else null
        }
}

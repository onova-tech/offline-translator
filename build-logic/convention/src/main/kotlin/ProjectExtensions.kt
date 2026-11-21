import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.versionOf(alias: String): String =
    findVersion(alias)
        .orElseThrow {
            IllegalStateException("Missing version '$alias' in libs.versions.toml")
        }.requiredVersion

internal fun VersionCatalog.intVersionOf(alias: String): Int = versionOf(alias).toInt()

internal fun VersionCatalog.library(alias: String) =
    findLibrary(alias).orElseThrow { IllegalStateException("Missing library '$alias' in libs.versions.toml") }

/**
 * Android namespace derived from the Gradle path,
 * e.g. `:feature:tools-basic` -> `tech.onova.offlinetranslator.feature.toolsbasic`.
 */
internal val Project.pocketNamespace: String
    get() = "tech.onova.offlinetranslator." + path.removePrefix(":").replace(":", ".").replace("-", "")

/**
 * Test JVMs keep their temporary files in this project's build folder, not in /tmp. On the development machine /tmp
 * is a 4 GB tmpfs in RAM, and every JVM that crashed left Robolectric's 200 MB native runtime behind, until /tmp
 * filled and every test failed to start (2026-11-07). Each run starts from an empty folder, so a crash leaves nothing
 * for the next one, and `clean` removes the rest.
 */
internal fun Project.keepTestTempFilesInBuild() {
    tasks.withType(Test::class.java).configureEach {
        val folder =
            layout.buildDirectory
                .dir("tmp/test-jvm/$name")
                .get()
                .asFile
        systemProperty("java.io.tmpdir", folder.absolutePath)
        doFirst {
            folder.deleteRecursively()
            folder.mkdirs()
        }
    }
}

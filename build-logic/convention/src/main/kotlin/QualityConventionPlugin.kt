import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Bundling
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.PathSensitivity
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.process.CommandLineArgumentProvider
import java.io.File

/**
 * Root-only static analysis (research.md R14): detekt and ktlint run as command-line tools in
 * isolated configurations, so their embedded Kotlin compilers never clash with the project's.
 */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            check(this == rootProject) { "offlinetranslator.quality must be applied to the root project" }
            pluginManager.apply("base")

            val detektCli = configurations.create("detektCli")
            val detektPlugins = configurations.create("detektPlugins")
            val ktlintCli =
                configurations.create("ktlintCli") {
                    // ktlint-cli publishes a plain and a shadowed (self-contained) jar; use the shadowed one.
                    attributes { attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.SHADOWED)) }
                }
            dependencies.add(detektCli.name, libs.library("detekt-cli").get())
            dependencies.add(detektPlugins.name, libs.library("compose-rules-detekt").get())
            dependencies.add(ktlintCli.name, libs.library("ktlint-cli").get())

            val kotlinSources =
                fileTree(rootDir) {
                    include("**/src/**/*.kt", "**/*.gradle.kts")
                    exclude("**/build/**", "**/.gradle/**")
                }
            val sourceRoots =
                (subprojects.map { it.projectDir } + file("build-logic/convention"))
                    .map { File(it, "src") }
                    .filter { it.isDirectory }
                    .joinToString(",") { it.path }

            val detekt =
                tasks.register<JavaExec>("detekt") {
                    group = "verification"
                    description = "Runs detekt with zero tolerated issues on every module."
                    classpath = detektCli
                    mainClass.set("io.gitlab.arturbosch.detekt.cli.Main")
                    val config = file("config/detekt/detekt.yml")
                    val report = layout.buildDirectory.file("reports/detekt/detekt.txt")
                    val pluginJars: FileCollection = detektPlugins
                    inputs.files(kotlinSources).withPathSensitivity(PathSensitivity.RELATIVE)
                    inputs.file(config)
                    outputs.file(report)
                    argumentProviders.add(
                        CommandLineArgumentProvider {
                            listOf(
                                "--input",
                                sourceRoots,
                                "--config",
                                config.path,
                                "--build-upon-default-config",
                                "--plugins",
                                pluginJars.files.joinToString(",") { it.path },
                                "--excludes",
                                "**/build/**",
                                "--report",
                                "txt:${report.get().asFile.path}",
                            )
                        },
                    )
                }

            val ktlintCheck =
                registerKtlint(name = "ktlintCheck", format = false, cli = ktlintCli, sources = kotlinSources)
            registerKtlint(name = "ktlintFormat", format = true, cli = ktlintCli, sources = kotlinSources)

            tasks.named("check").configure { dependsOn(detekt, ktlintCheck) }
        }

    private fun Project.registerKtlint(
        name: String,
        format: Boolean,
        cli: FileCollection,
        sources: FileCollection,
    ) = tasks.register<JavaExec>(name) {
        group = if (format) "formatting" else "verification"
        description = if (format) "Formats Kotlin sources with ktlint." else "Checks Kotlin sources with ktlint."
        classpath = cli
        mainClass.set("com.pinterest.ktlint.Main")
        workingDir = rootDir
        inputs.files(sources).withPathSensitivity(PathSensitivity.RELATIVE)
        if (!format) {
            val marker = layout.buildDirectory.file("reports/ktlint/ktlint.ok")
            outputs.file(marker)
            doLast {
                marker
                    .get()
                    .asFile
                    .apply { parentFile.mkdirs() }
                    .writeText("ok")
            }
        }
        args(
            listOfNotNull(
                "-F".takeIf { format },
                "--relative",
                "**/src/**/*.kt",
                "**/*.gradle.kts",
                "!**/build/**",
            ),
        )
    }
}

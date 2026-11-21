import org.gradle.testkit.runner.GradleRunner
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains

/** Gradle TestKit checks of the network guard, using an offline file-based Maven repository. */
class NetworkGuardConventionPluginTest {
    private lateinit var projectDir: File

    @BeforeTest
    fun setUp() {
        projectDir = createTempDirectory("network-guard").toFile()
        publishFakeModule(group = "com.squareup.okhttp3", name = "okhttp")
        publishFakeModule(group = "org.example", name = "offline-lib")
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            dependencyResolutionManagement {
                repositories { maven { url = uri("${projectDir.resolve("repo").toURI()}") } }
            }
            include(":feature:alpha", ":feature:beta", ":core:domain", ":androidApp", ":core:content")
            """.trimIndent(),
        )
        listOf("feature/alpha", "feature/beta", "core/domain", "androidApp", "core/content")
            .forEach { module(it, dependencies = "") }
    }

    @AfterTest
    fun tearDown() {
        projectDir.deleteRecursively()
    }

    @Test
    fun cleanProjectPasses() {
        module("feature/alpha", dependencies = """implementation("org.example:offline-lib:1.0")""")
        val result = runner().build()
        assertContains(result.output, "BUILD SUCCESSFUL")
    }

    @Test
    fun bannedNetworkLibraryFailsTheBuild() {
        module("feature/alpha", dependencies = """implementation("com.squareup.okhttp3:okhttp:1.0")""")
        val result = runner().buildAndFail()
        assertContains(result.output, "network-capable library on the runtime classpath: com.squareup.okhttp3:okhttp")
    }

    @Test
    fun featureDependingOnAnotherFeatureFailsTheBuild() {
        module("feature/alpha", dependencies = """implementation(project(":feature:beta"))""")
        val result = runner().buildAndFail()
        assertContains(result.output, "feature modules must not depend on each other (:feature:alpha -> :feature:beta)")
    }

    @Test
    fun domainDependingOnAnyModuleFailsTheBuild() {
        module("core/domain", dependencies = """implementation(project(":feature:beta"))""")
        val result = runner().buildAndFail()
        assertContains(result.output, ":core:domain must not depend on other modules")
    }

    @Test
    fun networkLibraryThroughTheContentModuleIsAllowedForTheApp() {
        module("core/content", dependencies = """implementation("com.squareup.okhttp3:okhttp:1.0")""")
        module("androidApp", dependencies = """implementation(project(":core:content"))""")
        val result = runner().build()
        assertContains(result.output, "BUILD SUCCESSFUL")
    }

    @Test
    fun networkLibraryThroughAnotherModuleStillFailsTheApp() {
        module("core/content", dependencies = """implementation("com.squareup.okhttp3:okhttp:1.0")""")
        module("feature/alpha", dependencies = """implementation("com.squareup.okhttp3:okhttp:1.0")""")
        module(
            "androidApp",
            dependencies =
                """
                implementation(project(":core:content"))
                implementation(project(":feature:alpha"))
                """.trimIndent(),
        )
        val result = runner(":androidApp:verifyNetworkGuard").buildAndFail()
        assertContains(result.output, "Network guard failed for :androidApp")
        assertContains(result.output, "network-capable library on the runtime classpath: com.squareup.okhttp3:okhttp")
    }

    @Test
    fun onlyTheAppMayDependOnTheContentModule() {
        module("feature/alpha", dependencies = """implementation(project(":core:content"))""")
        val result = runner().buildAndFail()
        assertContains(result.output, "only :androidApp may depend on :core:content")
    }

    private fun runner(task: String = "verifyNetworkGuard"): GradleRunner =
        GradleRunner
            .create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(task, "--stacktrace")

    private fun module(
        path: String,
        dependencies: String,
    ) {
        projectDir.resolve(path).apply { mkdirs() }.resolve("build.gradle.kts").writeText(
            """
            plugins {
                `java-library`
                id("offlinetranslator.network-guard")
            }
            dependencies {
                $dependencies
            }
            """.trimIndent(),
        )
    }

    private fun publishFakeModule(
        group: String,
        name: String,
    ) {
        val dir = projectDir.resolve("repo/${group.replace('.', '/')}/$name/1.0").apply { mkdirs() }
        dir.resolve("$name-1.0.pom").writeText(
            """
            <project xmlns="http://maven.apache.org/POM/4.0.0">
              <modelVersion>4.0.0</modelVersion>
              <groupId>$group</groupId>
              <artifactId>$name</artifactId>
              <version>1.0</version>
              <packaging>pom</packaging>
            </project>
            """.trimIndent(),
        )
    }
}

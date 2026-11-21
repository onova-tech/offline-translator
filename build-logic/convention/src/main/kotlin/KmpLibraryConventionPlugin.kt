import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Kotlin Multiplatform library with an Android target built by the Android-KMP plugin
 * (research.md R2). iOS targets are added in spec 090.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            pluginManager.apply("com.android.kotlin.multiplatform.library")
            pluginManager.apply("org.jetbrains.kotlinx.kover")
            pluginManager.apply("offlinetranslator.network-guard")
            keepTestTempFilesInBuild()

            extensions.configure<KotlinMultiplatformExtension> {
                extensions.configure<KotlinMultiplatformAndroidLibraryTarget> {
                    namespace = pocketNamespace
                    compileSdk = libs.intVersionOf("compileSdk")
                    minSdk = libs.intVersionOf("minSdk")
                    withHostTest { }
                    compilerOptions {
                        jvmTarget.set(JvmTarget.fromTarget(libs.versionOf("jvmTarget")))
                    }
                }

                if (path.startsWith(":core:")) {
                    explicitApi()
                }

                sourceSets.configureEach {
                    // kotlin.time.Instant/Clock replace kotlinx-datetime for timestamps (research.md R3).
                    languageSettings.optIn("kotlin.time.ExperimentalTime")
                }

                sourceSets.getByName("commonTest").dependencies {
                    implementation(kotlin("test"))
                    implementation(libs.library("kotlinx-coroutines-test"))
                    implementation(libs.library("turbine"))
                }
            }
        }
}

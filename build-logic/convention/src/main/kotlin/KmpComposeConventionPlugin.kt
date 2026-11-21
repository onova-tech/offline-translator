import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/** Compose Multiplatform UI module with Compose resources (strings, icons) in commonMain. */
class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("offlinetranslator.kmp.library")
            pluginManager.apply("org.jetbrains.compose")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure<KotlinMultiplatformExtension> {
                extensions.configure<KotlinMultiplatformAndroidLibraryTarget> {
                    androidResources { enable = true }
                }
                sourceSets.getByName("commonMain").dependencies {
                    implementation(libs.library("compose-runtime"))
                    implementation(libs.library("compose-foundation"))
                    implementation(libs.library("compose-ui"))
                    implementation(libs.library("compose-material3"))
                    implementation(libs.library("compose-components-resources"))
                }
            }

            extensions.configure<ComposeExtension> {
                extensions.configure<ResourcesExtension> {
                    publicResClass = true
                    packageOfResClass = "$pocketNamespace.resources"
                    generateResClass = ResourcesExtension.ResourceClassGeneration.Always
                }
            }
        }
}

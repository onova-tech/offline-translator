import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** The Android application module (composition root). Uses AGP 9 built-in Kotlin. */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            pluginManager.apply("org.jetbrains.kotlinx.kover")
            pluginManager.apply("offlinetranslator.network-guard")
            keepTestTempFilesInBuild()

            val javaVersion = JavaVersion.toVersion(libs.versionOf("jvmTarget"))
            extensions.configure<ApplicationExtension> {
                namespace = APPLICATION_ID
                compileSdk = libs.intVersionOf("compileSdk")

                defaultConfig {
                    applicationId = APPLICATION_ID
                    minSdk = libs.intVersionOf("minSdk")
                    targetSdk = libs.intVersionOf("targetSdk")
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }

                buildFeatures {
                    compose = true
                    buildConfig = true
                }

                buildTypes {
                    getByName("release") {
                        isMinifyEnabled = true
                        isShrinkResources = true
                        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
                    }
                }

                compileOptions {
                    sourceCompatibility = javaVersion
                    targetCompatibility = javaVersion
                }

                testOptions {
                    unitTests.isIncludeAndroidResources = true
                }
            }
        }

    private companion object {
        const val APPLICATION_ID = "tech.onova.offlinetranslator"
    }
}

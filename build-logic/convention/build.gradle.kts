plugins {
    `kotlin-dsl`
}

group = "tech.onova.offlinetranslator.buildlogic"

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.gradle.plugin)
    compileOnly(libs.compose.compiler.gradle.plugin)
    compileOnly(libs.kover.gradle.plugin)

    testImplementation(gradleTestKit())
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = "offlinetranslator.kmp.library"
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = "offlinetranslator.kmp.compose"
            implementationClass = "KmpComposeConventionPlugin"
        }
        register("androidApplication") {
            id = "offlinetranslator.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("quality") {
            id = "offlinetranslator.quality"
            implementationClass = "QualityConventionPlugin"
        }
        register("networkGuard") {
            id = "offlinetranslator.network-guard"
            implementationClass = "NetworkGuardConventionPlugin"
        }
    }
}

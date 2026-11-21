plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kmp.library) apply false
    alias(libs.plugins.android.asset.pack) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.aboutlibraries.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room3) apply false
    alias(libs.plugins.kover)
    id("offlinetranslator.quality")
}

dependencies {
    kover(project(":core:domain"))
    kover(project(":core:data"))
    kover(project(":feature:settings"))
    kover(project(":feature:tools-translate"))
}

kover {
    reports {
        filters {
            includes {
                // Constitution Principle V: domain logic keeps >= 90% line coverage.
                classes("tech.onova.offlinetranslator.*.domain.*")
            }
        }
        verify {
            rule("Domain line coverage") {
                minBound(90)
            }
        }
    }
}

tasks.named("check") {
    dependsOn("koverVerify")
    // Convention-plugin tests (e.g. the network guard) live in the included build.
    dependsOn(gradle.includedBuild("build-logic").task(":convention:check"))
}

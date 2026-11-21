plugins {
    id("offlinetranslator.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:domain"))
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.appcompat)
            // App visibility for timer alerts (spec 004 research.md R15).
            implementation(libs.androidx.lifecycle.process)
        }
    }
}

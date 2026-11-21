plugins {
    id("offlinetranslator.kmp.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:domain"))
            implementation(project(":core:designsystem"))
            // The permission flow re-checks when the user comes back (spec 004 research.md R7).
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            // Display formats read the user's settings (spec 004 FR-045).
            implementation(libs.koin.compose)
        }
    }
}

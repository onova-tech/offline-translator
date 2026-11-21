plugins {
    id("offlinetranslator.kmp.compose")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:domain"))
            implementation(project(":core:designsystem"))
            // The language's date order for the date format examples (spec 004 FR-045).
            implementation(project(":core:tool-api"))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.kotlinx.coroutines.core)
            // The Open-source licenses page reads the build's list (spec 012 T057).
            implementation(libs.kotlinx.serialization.json)
        }
    }
}

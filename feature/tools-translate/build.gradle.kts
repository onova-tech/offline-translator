plugins {
    id("offlinetranslator.kmp.compose")
    alias(libs.plugins.kotlin.serialization)
}

// The Translator (spec 012): Type or speak, and Conversation. It reaches the language packs, the engine and the phone's
// speech only through core/domain's ports, so it never depends on :core:content or :core:translation (Principle I,
// VIII); the text, the turns and the audio are never stored, only the settings in translator.json.
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:domain"))
            implementation(project(":core:data"))
            implementation(project(":core:designsystem"))
            implementation(project(":core:tool-api"))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(libs.okio.fakefilesystem)
        }
    }
}

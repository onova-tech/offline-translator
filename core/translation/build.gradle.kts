plugins {
    id("offlinetranslator.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

// The Translator's engine (spec 012 research R3): Mozilla's Bergamot, built once by scripts/build-bergamot-android.sh
// and committed as libbergamot.so per ABI (see NATIVE.md), so no build here compiles C++. Only :androidApp depends on
// it; the Translator reaches it through core/domain's TranslationEngine port. No network.
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:domain"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}

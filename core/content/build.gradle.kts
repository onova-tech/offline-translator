plugins {
    id("offlinetranslator.kmp.library")
}

// Constitution Principle I: on-demand content packs (the Translator's language packs, spec 012), one of the modules
// allowed to use the network, and only for a download the person confirmed. Only :androidApp may depend on it; the
// Translator reaches it through core/domain's ContentPacks port.
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:domain"))
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.play.asset.delivery.ktx)
        }
    }
}

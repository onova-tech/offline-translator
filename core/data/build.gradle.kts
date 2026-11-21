plugins {
    id("offlinetranslator.kmp.library")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:domain"))
            api(libs.datastore.core)
            // Public: JsonDocumentSerializer, used by every feature's repositories, extends OkioSerializer.
            api(libs.datastore.core.okio)
            api(libs.okio)
            api(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.okio.fakefilesystem)
        }
    }
}

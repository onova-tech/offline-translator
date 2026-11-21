plugins {
    alias(libs.plugins.android.test)
}

// End-to-end tests of the real debug app, on the emulator only (docs/testing.md). They run in their own process, so they
// can clear and restart the app the way a person's phone would. They wipe the app's data, and the tests refuse to run
// on anything but an emulator: the owner's phone is usually attached to adb too.
android {
    namespace = "tech.onova.offlinetranslator.e2e"
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()
    targetProjectPath = ":androidApp"

    defaultConfig {
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.targetSdk
                .get()
                .toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        val javaVersion = JavaVersion.toVersion(libs.versions.jvmTarget.get())
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    experimentalProperties["android.experimental.self-instrumenting"] = true
}

androidComponents {
    beforeVariants(selector().withBuildType("release")) { it.enable = false }
}

// Gradle's connected tasks install on every device attached, phones included. scripts/ci.sh talks only to its emulator.
tasks.matching { it.name.startsWith("connected") }.configureEach { enabled = false }

dependencies {
    implementation(kotlin("test"))
    implementation(libs.junit4)
    implementation(libs.androidx.test.junit)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.uiautomator)
}

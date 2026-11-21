plugins {
    id("offlinetranslator.android.application")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.aboutlibraries.android)
}

// Spec 012 T055: Settings → About → Open-source licenses. The list is collected while the app builds, from every
// dependency's POM and from config/libraries (the icons, the engine's native code, the translation models), into
// R.raw.aboutlibraries. Offline: the build never asks the network for license texts.
aboutLibraries {
    offlineMode = true
    collect {
        configPath = file("../config/libraries")
    }
}

// Spec 080: the Translator's packs, one per language of packs/languages.tsv (scripts/update-language-catalog.py).
val languagePacks: List<String> =
    rootProject
        .file("packs/languages.tsv")
        .readLines()
        .filterNot { it.isBlank() || it.startsWith("#") || it.startsWith("code\t") }
        .map { "translate_" + it.substringBefore('\t') }
        .distinct()

// Spec 082: the app's own speech, one pack per line of packs/speech.tsv (scripts/update-speech-catalog.py): Whisper,
// espeak-ng's data and a voice per language.
val speechPacks: List<String> =
    rootProject
        .file("packs/speech.tsv")
        .readLines()
        .filterNot { it.isBlank() || it.startsWith("#") || it.startsWith("pack\t") }
        .map { it.substringBefore('\t') }

/** Every on-demand pack, translation and speech. */
val allPacks: List<String> = languagePacks + speechPacks

/**
 * The packs whose models were fetched (pack.json is committed, the models aren't), as a value source: the configuration
 * cache checks it on every build, so fetching or removing a pack's models changes the next bundle. A plain walk at
 * configuration time was cached, and a newly fetched pack stayed out of the debug bundle (spec 080, 2026-11-20).
 */
abstract class FetchedPacks : ValueSource<List<String>, FetchedPacks.Parameters> {
    interface Parameters : ValueSourceParameters {
        val packsFolder: DirectoryProperty
        val packs: ListProperty<String>
    }

    override fun obtain(): List<String> =
        parameters.packs.get().filter { pack ->
            parameters.packsFolder
                .dir("$pack/src/main/assets/$pack")
                .get()
                .asFile
                .walkTopDown()
                .any { it.isFile && it.name != "pack.json" }
        }
}

fun quoted(value: String) = "\"$value\""

// The Translator's language packs (spec 012, spec 080): on-demand, downloaded from Google Play only after the person
// confirms (constitution Principle I). Only bundles carry them; an APK never does. A release bundle carries every pack;
// a debug one only those whose models were fetched, so local testing pushes only those (spec 080 FR-015: tests and the
// owner's nearly full phone never need all of them).
val releaseBuild = gradle.startParameter.taskNames.any { it.contains("Release") }
val fetchedPacks: List<String> =
    providers
        .of(FetchedPacks::class) {
            parameters {
                packsFolder.set(rootProject.layout.projectDirectory.dir("packs"))
                packs.set(allPacks)
            }
        }.get()
val bundledPacks: List<String> = if (releaseBuild) allPacks else fetchedPacks

android {
    defaultConfig {
        versionCode = 1
        versionName = "0.1.0"
        // Play refuses any question naming a pack the bundle doesn't hold (spec 080 research R3).
        buildConfigField("String", "BUNDLED_PACKS", quoted(bundledPacks.joinToString(",")))
    }

    assetPacks += bundledPacks.map { ":packs:$it" }

    // The engines run on 64-bit phones only, as Bergamot's (spec 012): sherpa-onnx's 32-bit libraries stay out.
    packaging {
        jniLibs {
            excludes +=
                setOf(
                    "lib/armeabi-v7a/libsherpa-onnx-jni.so",
                    "lib/x86/libsherpa-onnx-jni.so",
                    "lib/x86/libonnxruntime.so",
                )
        }
    }
}

// Spec 012 research R6: a release bundle without the models would offer packs that can't translate.
val verifyLanguagePacks =
    tasks.register("verifyLanguagePacks") {
        group = "verification"
        description = "Fails a release bundle with an empty pack (spec 012 research R6, spec 082)."
        val packs = allPacks
        val folders = packs.associateWith { rootProject.layout.projectDirectory.dir("packs/$it/src/main/assets/$it") }
        doLast {
            val missing =
                folders
                    // A pack's files are fetched when its folder holds anything besides the committed pack.json.
                    .filterValues { folder -> folder.asFile.walkTopDown().none { it.isFile && it.name != "pack.json" } }
                    .keys
            if (missing.isNotEmpty()) {
                throw GradleException(
                    "Packs without their files: ${missing.joinToString()}. Run " +
                        "scripts/fetch-language-packs.sh --all and scripts/fetch-speech-packs.sh --all first.",
                )
            }
        }
    }
tasks
    .matching { it.name in setOf("packageReleaseBundle", "bundleRelease") }
    .configureEach { dependsOn(verifyLanguagePacks) }

// Screenshot baselines are committed; `check` fails on unintended visual changes (research.md R15).
tasks.named("check") {
    dependsOn("verifyRoborazziDebug")
}

tasks.withType<Test>().configureEach {
    // Robolectric's Android 16 runtime needs JDK internals that JDK 17+ hides by default.
    jvmArgs(
        "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
        "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
    )
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:platform"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:tool-api"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:tools-translate"))
    // Spec 012: the language packs (the app's only network) and the native engine, bound here to their core/domain
    // ports; no feature module depends on them.
    implementation(project(":core:content"))
    implementation(project(":core:translation"))
    // Spec 082: the app's own speech (Whisper and Piper through sherpa-onnx), bound here to the speech ports.
    implementation(project(":core:speech"))

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.components.resources)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.jetbrains.lifecycle.viewmodel.compose)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.koin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4.accessibility)
}

// Spec 012 T042: scripts/ci.sh installs the app through bundletool's local testing, so the Translator's language packs
// come from the emulator's own storage. The script runs bundletool's own main class from this classpath (the version AGP
// uses, from Google's Maven) after Gradle has stopped, since Gradle and the emulator together exhaust the machine.
val bundletool: Configuration by configurations.creating {
    isCanBeConsumed = false
}

dependencies {
    bundletool(libs.bundletool)
    // AGP brings Kotlin's standard library to bundletool; run on its own, install-apks fails without it.
    bundletool(libs.kotlin.stdlib)
}

tasks.register("bundletoolClasspath") {
    val jars: FileCollection = bundletool
    val classpathFile = layout.buildDirectory.file("bundletool.classpath")
    inputs.files(jars)
    outputs.file(classpathFile)
    doLast { classpathFile.get().asFile.writeText(jars.joinToString(":") { it.absolutePath }) }
}

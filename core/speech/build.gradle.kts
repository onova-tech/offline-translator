plugins {
    id("offlinetranslator.kmp.library")
}

// The app's own speech (spec 082): sherpa-onnx runs Whisper for recognition and Kokoro or Piper for voices, on the
// device, with no network (research R1). Only :androidApp depends on it; the Translator reaches it through
// core/domain's speech ports. The library comes from sherpa-onnx's GitHub release, its SHA-256 pinned in
// gradle/verification-metadata.xml.
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:domain"))
            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(
                "com.k2fsa.sherpa.onnx:sherpa-onnx-static-link-onnxruntime:${libs.versions.sherpaOnnx.get()}@aar",
            )
        }
    }
}

# Tasks: the app's own speech (spec 082)

Path prefixes: `APP` = `androidApp/src`, `SP` = `core/speech/src`, `TR` = `feature/tools-translate/src`. **[phone]**
needs the owner's phone and their consent at that moment, with the app in front.

## Phase 1: Spike (R1–R3)

- [ ] T001 Ivy repository for sherpa-onnx's GitHub release in `settings.gradle.kts`, limited to its one artifact, with
  its SHA-256 pinned (Gradle dependency verification for that group only)
- [ ] T002 Module `core/speech` (kmp.library, Android only): the AAR, arm64-v8a and x86_64 only; `SherpaRecognizer`
  (Whisper, int8 encoder and decoder, tokens) and `SherpaVoice` (Kokoro or a Piper voice) loading models from a folder
- [ ] T003 Debug probe page (Settings → About, debug builds): Whisper on a pushed WAV, Kokoro and one Piper voice on
  a sentence, each with its load and run time and the process's memory; nothing played aloud
- [ ] T004 `scripts/fetch-speech-models.sh`: Whisper Base int8, Kokoro int8 v1.0 and one Piper voice into a folder,
  each file's SHA-256 pinned
- [ ] T005 [phone] Push the models and three test sentences to the app's files, open the probe, record R2 and R3 in
  research.md; delete the pushed files afterwards

## Phase 2 onward

Written after the spike, from its numbers and from R4 and R5.

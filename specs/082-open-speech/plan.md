# Implementation Plan: the app's own speech (spec 082)

**Branch**: `082-open-speech` | **Spec**: [spec.md](spec.md) | **Research**: [research.md](research.md)

## Summary

A new module `core/speech` runs sherpa-onnx (research R1): Whisper Base for recognition, Kokoro and Piper for voices.
It implements the existing ports (`SpeechInput`, `SpeechOutput`) a second time; a `SpeechRouter` in the Translator's
domain picks, per language and per direction, the phone's or the app's, from what each can do and the person's choice
(spec FR-006). Models come as on-demand packs through the existing `ContentPacks` (Principle I).

## Technical context

- Kotlin 2.4, Compose Multiplatform; sherpa-onnx 2.0.2 AAR (static onnxruntime); arm64-v8a and x86_64.
- Audio in: `AudioRecord` 16 kHz mono float, in memory only (FR-005); voice activity by sherpa-onnx's Silero VAD or a
  simple energy end-point (to decide in the spike).
- Audio out: `AudioTrack` with the generated samples.
- Packs: `speech_whisper_base`, `speech_kokoro`, `speech_espeak`, `voice_<lang>` (Piper), generated from a table like
  the translation packs (spec 080), within Play's limits (R5).

## Constitution check

- I (offline): sherpa-onnx has no network (R1); models arrive only as packs the person confirms. ✔
- II (device only): nothing leaves the phone; sound is never stored (FR-005). ✔
- V (test-first domain): the router and the pack table are pure Kotlin, tested first. ✔
- IX (least permission): the microphone, already asked by the Translator; no new permission. ✔
- X (accessibility): same controls and announcements as today (FR-008). ✔

## Phases

0. Research R1 (done), R2–R5 on the phone and in Play's documentation.
1. Spike (debug only): `core/speech` with the AAR, a probe page timing Whisper on a recorded sentence and Kokoro and
   one Piper voice on the owner's phone (R2, R3).
2. The packs: a fetch script with pinned hashes, the pack table, Gradle modules (R4, R5).
3. Recognition (US1), voices (US2), the choice in Manage languages (US3), sizes and licences (US4).

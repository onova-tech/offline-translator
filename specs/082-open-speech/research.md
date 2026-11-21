# Research: the app's own speech (spec 082)

## R1. The speech engine: sherpa-onnx on Android (done 2026-11-21)

- **Decision**: sherpa-onnx **2.0.2** (2026-11-19, Apache-2.0), the **static-link** AAR
  `sherpa-onnx-static-link-onnxruntime-2.0.2.aar` (SHA-256 `cb735648…c0cb`, 40 MB, from the project's GitHub release):
  one `libsherpa-onnx-jni.so` per ABI with onnxruntime (MIT) inside, 24.9 MB on arm64-v8a and 28.2 MB on x86_64
  (armeabi-v7a and x86 left out of the bundle). Its Kotlin API (`com.k2fsa.sherpa.onnx`, 132 classes) runs Whisper
  (`OfflineRecognizer` with `OfflineWhisperModelConfig`) and both voice families (`OfflineTts` with Kokoro and VITS).
- **Network**: none. The manifest declares no permission; the classes reference no `java.net`; the native library
  imports no `socket`, `connect`, `getaddrinfo` or HTTP library (checked by symbol). The `https://` strings inside are
  links in its messages. So `core/speech` stays outside the network-allowed modules.
- **How it comes into the build**: not on Maven Central. An Ivy repository pointing at the GitHub release, limited to
  this one artifact, with its SHA-256 pinned by Gradle's dependency verification; no binary in git.
- **Alternatives**: the AAR with a separate `libonnxruntime.so` (52 MB, two libraries); building from source (an hour
  per ABI on this machine, as Bergamot); whisper.cpp (recognition only, no voices).

## R2. Whisper Base on the owner's phone (open)

The release's `sherpa-onnx-whisper-base.tar.bz2` (197 MB) holds the float and int8 encoder and decoder; only the int8
pair and the tokens ship. To measure on the SM-G780G with a debug probe: load time, time for a 5-second sentence in
Portuguese, Arabic and Japanese, and memory. Target to agree with the owner: about 2 s for 5 s of speech.

## R3. Kokoro (open)

`kokoro-int8-multi-lang-v1_0.tar.bz2` (126 MB; Apache-2.0 weights): English, Spanish, French, Hindi, Italian,
Japanese, Brazilian Portuguese and Mandarin, with espeak-ng data and the Chinese lexicons. To measure: load time and
seconds of audio per second on the phone. v1.1 (140 MB) adds Chinese voices only.

## R4. Piper voices and their licences (open)

sherpa-onnx's release has 536 Piper files for 42 languages; an int8 "medium" voice is about 20 MB (Kazakh 12 MB,
Chinese 13 MB). Japanese and Korean have none (Kokoro covers Japanese). For each language: one voice, chosen for its
licence first (the voice's MODEL_CARD and its dataset), then its quality. Voices trained on non-commercial data are
left out (SC-004). espeak-ng's data, which every Piper voice and Kokoro use, can be one shared pack instead of one copy
per voice.

## R5. Google Play's limits with speech (open)

Today: 53 translation packs, about 2.2 GB to download and 3.2 GB unpacked; Play allows 100 packs and 4 GB of on-demand
packs in total. Speech adds Whisper (one pack), Kokoro (one), espeak-ng data (one) and up to 42 Piper voices: about 1 GB
to download. To find out whether the 4 GB counts the download or the unpacked size; if the unpacked size, Piper voices
only for languages Kokoro lacks, or "low" voices, or asking Play for more.

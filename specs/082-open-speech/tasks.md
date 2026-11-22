# Tasks: the app's own speech (spec 082)

Path prefixes: `APP` = `androidApp/src`, `SP` = `core/speech/src`, `TR` = `feature/tools-translate/src`. **[phone]**
needs the owner's phone and their consent at that moment, with the app in front.

## Phase 1: Spike (R1–R3)

- [X] T001 Ivy repository for sherpa-onnx's GitHub release in `settings.gradle.kts`, limited to its one artifact, with
  its SHA-256 pinned (Gradle dependency verification for that group only)
- [X] T002 Module `core/speech` (kmp.library, Android only): the AAR, arm64-v8a and x86_64 only; `SherpaRecognizer`
  (Whisper, int8 encoder and decoder, tokens) and `SherpaVoice` (Kokoro or a Piper voice) loading models from a folder
- [X] T003 Debug probe page (Settings → About, debug builds): Whisper on a pushed WAV, Kokoro and one Piper voice on
  a sentence, each with its load and run time and the process's memory; nothing played aloud
- [X] T004 The spike's models fetched by hand with their digests checked (research R1); the real fetch is T007
- [X] T005 [phone] Push the models and three test sentences to the app's files, open the probe, record R2 and R3 in
  research.md; delete the pushed files afterwards

## Phase 2: The speech packs (research R4, R5)

- [X] T006 `scripts/update-speech-catalog.py`: from Piper's `voices.json` and the MODEL_CARDs, the licence rule of
  research R4 (free dataset, and a free base when fine-tuned) and the app's catalog, write `packs/speech.tsv` (pack,
  language, voice, quality, licence, files with SHA-256) and `core/domain/.../speech/SpeechCatalog.kt`; `--check`
- [X] T007 `scripts/fetch-speech-packs.sh`: Whisper Base's int8 pair and tokens (`speech_whisper`), espeak-ng's data
  once (`speech_espeak`) and each voice (`voice_<lang>`), every file checked against `speech.tsv`
- [X] T008 Gradle: a pack module per row of `speech.tsv` with the shared `packs/pack.gradle.kts`; debug builds bundle
  only fetched packs, release builds all and fail on an empty one (as spec 080)
- [X] T009 [P] `SpeechCatalogTest` (core/domain): every voice's language is in the catalog, one voice per language,
  every licence in the free list

Phase 2 as built (2026-11-21): 25 voices (six free ones, bg bn el et mr te, aren't in sherpa-onnx's converted set yet)
and recognition for 53 of 54 languages (Whisper doesn't know Uyghur); `packs/.gitignore` keeps every fetched file out
of git; a pack counts as fetched when its folder holds anything besides pack.json.

## Phase 3: User story 1, speak a language the phone can't recognize (P1)

- [X] T010 [P] Domain tests first: `SpeechRoutesTest` (phone, app or none for recognition and voice, from the phone's
  support, the app's packs and the person's choice; FR-001, FR-006, US3)
- [X] T011 `SpeechRoutes` and the choice per language in `TranslatorSettings` (FR-007)
- [X] T012 `AppSpeechInput` (core/speech): `AudioRecord` at 16 kHz mono in memory, ends on the stop button or 1.5 s of
  silence, then Whisper with the language; Partial "…" while listening, Final with the text; never written (FR-005)
- [X] T013 Bind the phone's and the app's engines (`named("phone")`, `named("app")`) and route the Translator's speech
  through `SpeechRoutes` in Type or speak and Conversation (FR-008)
- [X] T014 The Conversation's and Type or speak's notices offer the app's recognition with its size when the phone
  can't (US1 scenario 1), in three languages

## Phase 4: User story 2, hear a translation in a language the phone can't speak (P1)

- [X] T015 `AppSpeechOutput` (core/speech): Piper through sherpa-onnx, sentence by sentence into an `AudioTrack`, stop
  at once; `SpeechEnd` as the phone's
- [X] T016 Read aloud and the Conversation's turns use it where routed; the "no voice" notice offers the app's voice

## Phase 5: User stories 3 and 4, the choice and the space (P2, P3)

- [X] T017 Manage languages: Speech recognition and Voice rows with "Phone" or "App" where both exist, the app's
  download, delete and size (FR-006); rules in `docs/ui-rules.md` (C4). `EngineUse` (who is used, the choice, whether
  both can) from the routing; `SpeechChoices` writes the choice; a voice's Delete takes espeak-ng's data with the last
  voice; the app tests now run through the real routing (phone and app fakes bound by name)
- [X] T018 Settings → Offline languages counts the speech packs; Open-source licenses credits sherpa-onnx,
  onnxruntime, espeak-ng, Whisper and every voice (FR-009): the catalog script writes each voice's entry, with its
  dataset, licence and base

## Phase 6: Polish

- [X] T019 Screen tests, screenshots (looked at before committing), layout fit, accessibility audit, mode stability:
  `ManageLanguagesPageTest` (the choice and the app's download; Delete asks first), two screenshots of a card with
  both (`translator_manage_app_speech_*`); the full check passes
- [X] T020 [P] e2e on the emulator: the app's recognition and a voice through local testing, in airplane mode
  (`SpeechJourney`: Spanish's translation, Whisper and Spanish's voice downloaded on Manage languages; then a
  translation read aloud by Piper and Whisper loaded on the emulator's silent microphone). It found T025
- [X] T021 `scripts/ci.sh check`, then `device`: check OK; a11y OK (6); e2e OK (2: `SpeechJourney`, `TranslatorJourney`)
- [ ] T022 [phone] The owner's pass: Arabic spoken, a Piper voice heard, the choice switched, in airplane mode

## Owner's phone, 2026-11-21 (first install with the app's speech)

- [X] T023 Arabic's Download did nothing: the phone's active network was Tailscale's VPN, so "Download only on Wi-Fi"
  waited, and the row hid it. A VPN over an unmetered network counts as Wi-Fi (`WifiCheck.countsAsWifi`); the app's
  rows show their packs' own state (waiting, downloading, failed)
- [X] T024 Japanese, asked of the phone and never installed, stayed with the phone: the routing now reads the
  Translator's download memory (`SpeechDownloads.neverInstalls`, read only) and offers the app's recognition
- [X] T025 The translation engine opened every installed pack, so the app's speech packs were called damaged and
  removed right after their download: `EngineSession` opens the translation packs only (`EngineSessionTest`)
- [X] T026 The journeys put local testing's packs back after the app started; with the speech packs (about 900 MB)
  the store asked mid-copy and called every pack unavailable. `restoreLocalPacks` restarts the app after the copy

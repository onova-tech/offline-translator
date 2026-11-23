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

## Owner's phone, 2026-11-22

- [X] T027 Manage languages crashed when it asked the phone's recognizer again: the recognizer was destroyed before
  Android answered, and Android then ran the queued question on a dead connection. `AndroidSpeechInput.checkSupport`
  destroys it once it answered (or failed)
- [X] T028 Japanese stayed on the phone: tapping "Phone", already shown, saved it as a choice. Tapping the option
  already shown saves nothing (`LanguageParts.EngineChoice`)
- [X] T029 The Conversation and Type or speak ask about speech again when one of the app's speech packs comes in, goes
  or is on its way, the store's first answer at start among them (`AppSpeechParts.changes`;
  `SpeakAndHearTest.aSpeechPackComingInIsAskedAboutAgain`)

## Phase 7: User story 5, check what was heard before the other person hears it (P2)

Spec: user story 5, FR-011 to FR-017, the clarifications of 2026-11-22. Each task updates `docs/ui-rules.md` where it
changes a rule, re-records the screenshots it changes (looked at before committing), and ends with
`./gradlew :feature:tools-translate:allTests :androidApp:testDebugUnitTest` passing, then a commit.

Review focus (what no line below names, most likely first; each one's test goes in the task named): an Arabic half,
whose label and text read right to left together (T034); the far half turned, with ^, the square and the speaker
turned with it and still tapped where drawn (T036, T037); 200 % text in a short half, where the label, the text and the
speaker must fit without clipping (T038, `LayoutFitTest`); Clear while a turn waits, which empties everything and
brings the microphone back (T036); Stop on a replay read by the app's voice (Piper), which must stop at once (T037).

- [X] T030 Manage languages opens without a flash (FR-013). Found on the emulator (pictures can't be captured from
  this machine's headless emulator, so the window manager's own record): the page was a dialog window with
  `fl=DIM_BEHIND fmt=TRANSPARENT` and the dialog grow-in animation, so the screen dimmed, stayed dark until the heavy
  page's first frame, then the page grew in. `FullScreenPage` (core/designsystem; Android window settings in
  `PlainPageWindow`) clears the dim and the animation and gives the window the page's colour; Manage languages,
  `ConfirmScreen` and the Conversation's correction use it.
  `ManageLanguagesPageTest.thePageOpensWithoutDimmingOrAnimatingItsWindow`; L5 updated
- [X] T031 The Device | App choice inside its part's card (FR-011): `InfoRow` and `MenuSettingsRow` (core/designsystem
  `SettingsRows.kt`) take `below: (@Composable ColumnScope.() -> Unit)? = null`, drawn under the state line on the
  same surface; `SpeechPart` and `VoicePart` pass `EngineChoice` there. `translator_engine_phone`: "Device",
  "Aparelho", "Dispositivo". `ManageLanguagesPageTest`: the choice is inside the part's row (its bounds within the
  row's); C4 rewritten; `translator_manage_app_speech_*` re-recorded
- [X] T032 The device's voice behind ⋮ (FR-012): a ready voice of the phone's gets one item, "Manage voices on the
  device" / "Gerenciar vozes do aparelho" / "Gestionar voces del dispositivo" (`translator_voice_manage`), calling
  `SpeechPartActions.onVoice` (`SpeechOutput.openVoiceSettings`, the voice app's list). `ManageLanguagesPageTest`:
  the item is there and calls it; C3 updated
- [X] T033 [P] Turn labels (FR-014): `core/domain/.../translation/TurnLabels.kt`, `data class TurnLabel(val me:
  String, val you: String)` and `object TurnLabels { fun of(language: LanguageCode): TurnLabel }`, one hand-written
  pair per catalog language, each with its own punctuation (`"Eu:"`, `"Moi :"`, `"我："`); the polite "you", and "the
  other person" for ja and ko. `TurnLabelsTest`: every `LanguageCatalog.all` code has a pair, neither blank, the two
  different. `docs/ui-rules.md` Exceptions: X1, with the reason (the words follow the half's language)
- [X] T034 Every turn one size, with its label (FR-014): `OwnTurn` and `OtherTurn` become one `TurnText(label: String,
  text: String)`: the label in bold `labelLarge`, then the text in `bodyLarge`, one `AnnotatedString` in its own
  direction; labels from `TurnLabels.of(languageOf(side))`; a tap on one's own still corrects it.
  `ConversationScreenTest`: a Portuguese half reads "Eu: …" for its own turn and "Você: …" for the other's; C1
  updated; Conversation screenshots re-recorded
- [X] T035 Room at the bottom (FR-015): Share leaves both screens (`Send.SHARE`, `TranslatorOutputs.sharer` and the
  `TextSharer` port and binding when nothing else uses them; the tests that share). `MoreControls` (core/tool-api)
  splits into `rememberMoreControlsState()`, `MoreControlsPanel(state, options)` above the row and
  `RowScope.MoreOptionsAction(state)`, an outlined compact `BottomAction` reading "More options" / "Fewer options";
  the one-option rule (A4) stays. The Conversation: Clear | More options (tonal, `BottomActionStyle.Tonal`) | Copy;
  Type or speak: Read aloud (with `PocketIcons.Speaker`) | Copy; Copy the main action on both (owner, 2026-11-22). `MoreControlsTest` for the row
  button; `UiRulesTest` A3 lets "More options" sit in the action row; A3 and A4 updated with the owner's decision
- [X] T036 Confirm before sending (FR-016): `TranslatorSettings.confirmBeforeSending` (false, stored as
  `farHalfTurned` is) and `ConversationOption.CONFIRM`, a third switch in More options ("Confirm before sending" /
  "Confirmar antes de enviar" / "Confirmar antes de enviar"). The machine: `ConversationState.confirm`,
  `ConversationPhase.Waiting(turnId)`, `ConversationEvent.ConfirmChanged(on)` and `ConversationEvent.Send(side)`;
  a heard turn goes to `Waiting` when `confirm`, else to `Translating` as today; `Send` moves `Waiting` to
  `Translating`; the square (`TapMicrophone(discard = true)`) removes the waiting turn; `Edited` on it changes its
  words and keeps it waiting; its half's `LanguageChanged` drops it; `Interrupted` leaves it waiting; `ConfirmChanged`
  never sends it. `ConversationMachineTest`, one test per line above. The screen: `SendButton` (core/designsystem,
  `PocketIcons.ArrowUp`, the microphone's size and colours, "Send" / "Enviar" / "Enviar") in the microphone's place
  with the square beside it; the waiting turn italic in its half, absent from the other half and from Copy.
  `ConversationScreenTest`: speak, ^, square; C1 updated
- [X] T037 Hear a translation again (FR-017): `PocketIcons.Speaker`; `ConversationEvent.Replay(turnId)` (from Idle
  to `Speaking(turnId)`) and `ConversationEvent.StopSpeaking` (Speaking to Idle). In the square's place beside each
  microphone, `HearAgainButton` ("Hear it again" / "Ouvir de novo" / "Escuchar de nuevo", tertiary colours) reads the
  half's latest translation, enabled only when its voice is `VoiceSupport.READY` and nothing else is happening; while
  it reads, the red `StopButton` there stops it. (A speaker on each turn, tried first, made the lines taller: owner,
  2026-11-22.) It reads with
  TalkBack on and with "Read translations aloud" off. `ConversationMachineTest` (Replay, StopSpeaking, Replay ignored
  while listening) and `ConversationScreenTest` (enabled only with a ready voice); C1 updated
- [X] T038 Polish: screenshots looked at, `LayoutFitTest`, `ModeStabilityTest`, the accessibility audit, then
  `scripts/ci.sh check` and `scripts/ci.sh device` (both journeys)
- [ ] T039 [phone] The owner's pass of story 5, with T022
- [X] T040 The correction as a popup (owner, 2026-11-23): `PopupCard` (core/designsystem), a card in the middle of the
  screen, turned 180° for the far half's own turn while "Turn the top half to the other person" is on;
  `ConversationScreenTest.aCorrectionOfTheFarHalfIsAPopupTurnedToThePersonAcross` and
  `aCorrectionIsUprightForTheNearHalfAndWhenTheFarHalfIsntTurned`; L5 and C1 updated. The card takes its own width
  (`usePlatformDefaultWidth = false`, at most 560 dp, as Material's dialogs): at the platform's default dialog width a
  text field never let the screen settle in the tests. `LayoutFitTest` gains the correction (`ModeSurface`
  "translator-correction"), as L7 asks of every dialog
- [X] T041 "Translate again" reads aloud by itself only the latest turn (owner, 2026-11-23): `ConversationMachine`'s
  `translated()`; `ConversationMachineTest.aCorrectedEarlierTurnIsTranslatedAgainWithoutBeingReadAloud`

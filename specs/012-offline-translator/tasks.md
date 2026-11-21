---
description: "Tasks for spec 012, the offline translator by text and by voice"
---

# Tasks: Offline Translator, by text and by voice (spec 012)

**Input**: [spec.md](spec.md), [plan.md](plan.md), [research.md](research.md), [data-model.md](data-model.md),
[contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: required. The constitution asks for test-first domain logic (Principle V) and automated accessibility
checks (Principle X); research R10 lists them. Within each phase, the test tasks come first and must fail before the
code they cover is written.

**Owner's decisions**: all of spec 012's rows in `docs/decisions-pending.md` are approved (2026-11-17). Every new
page must "feel consistent and look good": app components only, `docs/ui-rules.md` followed.

## Path prefixes used below

| Prefix | Path |
|---|---|
| `DOM/` | `core/domain/src/commonMain/kotlin/tech/onova/offlinetranslator/core/domain/` |
| `FT/` | `feature/tools-translate/src/commonMain/kotlin/tech/onova/offlinetranslator/feature/toolstranslate/` |
| `FTT/` | `feature/tools-translate/src/commonTest/kotlin/tech/onova/offlinetranslator/feature/toolstranslate/` |
| `PLAT/` | `core/platform/src/androidMain/kotlin/tech/onova/offlinetranslator/core/platform/` |
| `DS/` | `core/designsystem/src/commonMain/kotlin/tech/onova/offlinetranslator/core/designsystem/` |
| `APP/` | `androidApp/src/main/kotlin/tech/onova/offlinetranslator/` |
| `APPT/` | `androidApp/src/test/kotlin/tech/onova/offlinetranslator/` |
| `SET/` | `feature/settings/src/commonMain/kotlin/tech/onova/offlinetranslator/feature/settings/` |

## Format: `- [ ] [ID] [P?] [Story?] Description`

**[P]**: can run in parallel (different files, no dependency on an unfinished task). **[Story]**: US1–US5 from
spec.md. **[phone]** in a description: needs the owner's phone and the owner's consent at that moment; never run the end-to-end tests on it.

---

## Phase 1: Setup (shared infrastructure)

**Purpose**: the new modules exist, build empty, and are wired into the app.

- [X] T001 Add `:core:content`, `:core:translation`, `:feature:tools-translate`, `:packs:translate_pt` and `:packs:translate_es` to `settings.gradle.kts`
- [X] T002 [P] Add `play-asset-delivery-ktx = "2.3.0"` (`com.google.android.play:asset-delivery-ktx`) and `aboutlibraries = "15.3.0"` (plugin `com.mikepenz.aboutlibraries.plugin`, library `com.mikepenz:aboutlibraries-core`) to `gradle/libs.versions.toml`
- [X] T003 [P] Create `core/content/build.gradle.kts` (`offlinetranslator.kmp.library`; `androidMain` depends on `:core:domain` and `libs.play.asset.delivery.ktx`; the network guard already allows `:core:content`) and an empty `core/content/src/androidMain/kotlin/tech/onova/offlinetranslator/core/content/` package
- [X] T004 [P] Create `core/translation/build.gradle.kts` (`offlinetranslator.kmp.library`; `androidMain` depends on `:core:domain`; `jniLibs` from `src/androidMain/jniLibs`) and `core/translation/NATIVE.md` with the empty table "ABI | file | SHA-256 | built from"
- [X] T005 [P] Create `feature/tools-translate/build.gradle.kts` like `feature/tools-device/build.gradle.kts` (kmp.compose, serialization; `:core:domain`, `:core:data`, `:core:designsystem`, `:core:tool-api`, Koin, lifecycle, coroutines, serialization; `commonTest`: `okio.fakefilesystem`); it must NOT depend on `:core:content` or `:core:translation`
- [X] T006 [P] Create the asset pack modules `packs/translate_pt/build.gradle.kts` and `packs/translate_es/build.gradle.kts` (`com.android.asset-pack`, `packName = "translate_pt"`/`"translate_es"`, `deliveryType = "on-demand"`), and a `packs/.gitignore` ignoring `*/src/main/assets/**/*.bin` and `*.spm` (the model files, research R6)
- [X] T007 In `androidApp/build.gradle.kts`: `implementation` of the three new modules, `assetPacks += listOf(":packs:translate_pt", ":packs:translate_es")`, and a `bundleRelease` precondition that fails with "Run scripts/fetch-language-packs.sh first" when a pack's model files are missing (debug builds and `./gradlew check` don't need them)
- [X] T008 In `androidApp/src/main/AndroidManifest.xml`, add `<queries>` with the intent actions `android.speech.RecognitionService` and `android.intent.action.TTS_SERVICE` (research R1, R2); check that `permissions.allowlist` needs no change (`RECORD_AUDIO` is already there)

**Checkpoint**: `./gradlew :androidApp:assembleDebug` builds with the empty modules.

---

## Phase 2: Foundational (blocking prerequisites)

**Purpose**: the ports, the domain rules (test-first), the settings document and the fakes. No user story starts before this phase ends.

### Ports (contracts/ports.md)

- [X] T009 [P] Write `DOM/ports/ContentPorts.kt`: `PackId` (value class), `PackState` (`Unknown`, `NotInstalled`, `Waiting(reason: FOR_WIFI | FOR_CONFIRMATION)`, `Downloading(bytes, total)`, `Installing`, `Installed`, `Failed(reason: NO_SPACE(bytesNeeded) | NETWORK | DAMAGED | OTHER)`, `Unavailable(reason: NO_STORE | UNSUPPORTED_DEVICE)`), `PackStatus(id, downloadBytes: Long?, installedBytes: Long?, state)`, and `interface ContentPacks` (`packs: StateFlow<List<PackStatus>>`, `refresh()`, `download(pack, wifiOnly)`, `cancel(pack)`, `remove(pack)`, `location(pack): String?`)
- [X] T010 [P] Write `DOM/ports/TranslationPorts.kt`: `LanguageCode` (value class), `Route` (`Same`, `Direct(pack)`, `ThroughEnglish(first, second)`), `EngineOpen` (`Ready`, `Unavailable(UNSUPPORTED_DEVICE | DAMAGED_PACK)`), `interface TranslationEngine` (`open(packs: Map<PackId, String>)`, `translate(text, route)`, `close()`)
- [X] T011 [P] Write `DOM/ports/SpeechPorts.kt`: `RecognitionSupport` (`READY`, `CAN_DOWNLOAD`, `UNSUPPORTED`, `NO_RECOGNIZER`, `UNKNOWN`), `VoiceSupport` (`READY`, `NO_OFFLINE_VOICE`, `NO_ENGINE`), `SpeechEvent` (`Partial`, `Final`, `Failed(NOTHING_HEARD | LANGUAGE_MISSING | BUSY | NO_PERMISSION | OTHER)`), `interface SpeechInput` (`support`, `requestLanguage`, `listen(speechTag): Flow<SpeechEvent>`), `interface SpeechOutput` (`voice`, `speak(text, speechTag): Boolean`, `stop()`, `openVoiceSettings()`)
- [X] T012 [P] Add `interface ScreenReader { val on: StateFlow<Boolean> }` to `DOM/ports/ScreenPorts.kt`

### Domain rules, test-first (data-model.md; Kover ≥ 90%)

- [X] T013 [P] Write failing tests `FTT/domain/RoutesTest.kt`: same language → `Same`; en↔pt and en↔es → `Direct(translate_pt|es)`; pt↔es → `ThroughEnglish(translate_pt, translate_es)` in the right order; the packs a route needs
- [X] T014 [P] Write failing tests `FTT/domain/TextLimitTest.kt`: empty or blank → no request; text is trimmed; "> 5,000 characters → only the first 5,000 are translated and `overLimit` is set"; the counter text in each language's number format
- [X] T015 [P] Write failing tests `FTT/domain/SpeechRegionsTest.kt` as a table (research R9): phone region matching the language wins (`es` + MX → `es-MX`), else `en-US`, `pt-BR`, `es-ES`
- [X] T016 [P] Write failing tests `FTT/domain/PackMessagesTest.kt`: each `PackState` maps to its message and actions (contracts/ui.md "The pack offer"); MB rounding of sizes; a route through English offers only the missing pack (story 2, scenario 6)
- [X] T017 [P] Write failing tests `FTT/domain/ConversationMachineTest.kt` (research R8, data-model.md): a microphone opens only from `Idle`; `Listening → Translating → Speaking → Idle`; "a pause ends `Listening` only after something was heard; with nothing heard, it returns to `Idle` and says so"; no listening while `Speaking`; `Paused(reason)` from any phase on call, alarm or hidden screen, back to `Idle` only by a tap; errors return to `Idle` with a message on that side; a corrected turn is translated again; "at most 200" turns on screen
- [X] T018 [P] Write failing tests `FTT/domain/ConversationTextTest.kt`: "one line per turn, in order: `<language name>: <heard>` then `<other language name>: <translation>`"
- [X] T019 Implement `FT/domain/Routes.kt`, `FT/domain/TextLimit.kt` (`MAX_CHARACTERS = 5_000`, `PAUSE_MILLIS = 500`), `FT/domain/SpeechRegions.kt`, `FT/domain/PackMessages.kt`, `FT/domain/ConversationMachine.kt` (`MAX_TURNS_SHOWN = 200`, `SILENCE_MILLIS = 2_000`), `FT/domain/ConversationText.kt` until T013–T018 pass

### Settings document and data part (data-model.md "TranslatorSettings")

- [X] T020 Write failing tests `FTT/data/TranslatorSettingsTest.kt`: defaults ("`source`: the app's language if offered, else `en`"; "`target`: `en`, or `es` when the source is `en`"; `mode = TYPE_OR_SPEAK`; `wifiOnly = true`; `farHalfTurned = true`), round trip, `schemaVersion` 1, the `translator` part exports settings and never the text, the turns or the packs (FR-015)
- [X] T021 Implement `FT/data/TranslatorSettingsDocument.kt` and `FT/data/DataStoreTranslatorSettings.kt` (`translator.json`, `JsonDocumentPart` id `translator`, kind settings) until T020 passes

### Fakes (contracts/ports.md "Fakes")

- [X] T022 [P] Write `FTT/fakes/FakeContentPacks.kt`, `FakeTranslationEngine.kt` (returns `"[to] text"`, can be slow or fail), `FakeSpeechInput.kt` (scripted events, support per tag), `FakeSpeechOutput.kt` (records, `speak` suspends until the test releases it), `FakeScreenReader.kt`
- [X] T023 [P] Add the same fakes to `APPT/support/KoinFakesRule.kt` so app-level UI tests can bind them

### The tool's shell

- [X] T024 Write `FT/TranslatorEntry.kt` (`ToolEntry`, permanent id `translator`, category `UTILITIES`, no required hardware) and `FT/di/ToolsTranslateModule.kt` (`single<ToolEntry>(named("translator"))`, the settings repository, `single<DataPart>(named("data-part:translator"))`); register `toolsTranslateModule` in `APP/OfflineTranslatorApplication.kt`; name and search keywords in `feature/tools-translate/src/commonMain/composeResources/values{,-pt-rBR,-es}/strings.xml`

**Checkpoint**: `./gradlew :feature:tools-translate:testAndroidHostTest` green; the tool appears on Home and opens an empty screen.

---

## Phase 3: The engine, the packs and the research on the owner's phone

**Purpose**: the real engine and the real packs exist, and the four [phone] items of research.md are closed early, since their answers can change US4 and US5.

- [X] T025 [P] Implement `PLAT/speech/AndroidSpeechInput.kt` (on-device recognizer only: `isOnDeviceRecognitionAvailable`, `createOnDeviceSpeechRecognizer` in try/catch, `checkRecognitionSupport` on API 33+, `triggerModelDownload` for `requestLanguage`, partial results, the error mapping of research R1; never `createSpeechRecognizer`), `PLAT/speech/AndroidSpeechOutput.kt` (offline voices only: `!isNetworkConnectionRequired` and no `KEY_FEATURE_NETWORK_SYNTHESIS`; audio focus; `UtteranceProgressListener`; `TTS_SETTINGS` intent), `PLAT/speech/AndroidScreenReader.kt` (touch exploration state with its listener); bind all three in `APP/di/AppModule.kt`
- [X] T026 Install the latest stable NDK and CMake with `sdkmanager` (record versions in `core/translation/NATIVE.md`); stop Gradle first
- [X] T027 Write `core/translation/native/bergamot_jni.cpp` (C entry points `open(modelDirs[])`, `translate(handle, text, route)`, `close(handle)` over Bergamot's service; the configuration of contracts/pack-format.md "Rules"; the same C functions serve iOS's cinterop later) and `scripts/build-bergamot-android.sh` (clones `mozilla/translations` at a pinned commit, builds `inference/` with the NDK for `arm64-v8a` (Ruy) and `x86_64`, links the JNI layer into `libbergamot.so`, copies to `core/translation/src/androidMain/jniLibs/<abi>/`, prints the SHA-256 and the third-party libraries it built with their licenses)
- [X] T028 Run `scripts/build-bergamot-android.sh` in the background; commit both `.so` files and fill `core/translation/NATIVE.md` (commit, NDK, flags, SHA-256, third-party list)
- [X] T029 Implement `core/translation/src/androidMain/kotlin/tech/onova/offlinetranslator/core/translation/BergamotEngine.kt` (loads the library, one engine thread, newest-wins handled by the caller, `UNSUPPORTED_DEVICE` when the ABI has no library); bind it in `APP/di/AppModule.kt`
- [X] T030 Write `scripts/fetch-language-packs.sh`: reads the pinned paths and SHA-256 from Mozilla's model registry (research R4), downloads en↔pt and en↔es base-memory models, checks every hash, unpacks them into `packs/translate_<code>/src/main/assets/translate_<code>/<from>-<to>/` and writes `pack.json` (contracts/pack-format.md)
- [X] T031 Write the instrumented test `core/translation/src/androidInstrumentedTest/kotlin/tech/onova/offlinetranslator/core/translation/BergamotEngineTest.kt` (as built: `androidApp/src/androidTest/.../BergamotEngineTest.kt`, since no KMP module runs device tests yet; `scripts/ci.sh engine` pushes the packs) (emulator `x86_64`, a pack pushed by the test's setup): pt→en and pt→es (through English) return non-empty text in under 5 s on the emulator
- [X] T032 Implement the debug-only probe page `androidApp/src/debug/kotlin/tech/onova/offlinetranslator/debug/PhoneProbePage.kt`, reached from Settings → About in debug builds only (a slot like `removeAds`): for en, pt and es the recognition support and the offline voice; the engine's ABI, the time for the 30-word test sentence direct and through English, and memory after loading (quickstart §2); app components only
- [X] T033 [phone] With the owner's consent: install the debug build on the owner's phone, open the probe page, and record R1, R2 and R3's results in `research.md` with the date, model and Android version; install a pack with bundletool's local testing and record R5. If SC-001 fails, switch to the "tiny" models in T030 and record why (as done 2026-11-18 on the owner's SM-G780G, Android 13: R1, R2 and R3 recorded; SC-001 passes, so the base-memory models stay; the probe found Samsung's three-letter voice locales, fixed in `SpeechMapping.offlineVoice`. R5 needs `PlayContentPacks` (T040), so it moves to T042)

**Checkpoint**: the four [phone] rows of research.md are closed; the plan's defaults are confirmed or changed.

---

## Phase 4: User Story 1 – Translate a text (Priority: P1) 🎯 MVP

**Goal**: type or paste text and read it translated at once, offline, with swap, Copy and Share.

**Independent Test**: with the pt and es packs installed and airplane mode on, "Onde fica a estação de trem?" from Português to English and to Español is understandable within 2 s; pt→es says "Translated through English".

- [X] T034 [P] [US1] Write failing ViewModel tests `FTT/presentation/TranslatorViewModelTest.kt` with the fakes: a request "after 500 ms without typing", a newer one replaces it and an older result is dropped; same language shows the text unchanged with the note; over 5,000 characters translates the first 5,000 with the note; swap exchanges languages and puts the translation in the input; languages and mode are remembered, the text isn't; the engine opens with installed packs and closes on leaving
- [X] T035 [P] [US1] Write failing UI tests `APPT/screens/TranslatorScreenTest.kt`: the mode `SegmentedChoice`; the languages as two `MenuChoice` fields with the swap; the text field with its counter; the translation in the middle; Copy (with confirmation) and Share send the translation; Read aloud filled and last, disabled until US4
- [X] T036 [US1] Implement `FT/presentation/TranslatorViewModel.kt` until T034 passes
- [X] T037 [US1] Implement `FT/presentation/TypeOrSpeakContent.kt` and `FT/presentation/TranslatorContent.kt` per contracts/ui.md "Type or speak" with `AdaptiveToolLayout`, `SegmentedChoice`, `MenuChoice`, `PocketTextField`, `ShortNote`, `NO_READING`, `BottomActionRow`; strings in three languages; until T035 passes
- [X] T038 [US1] Announce a finished translation once (polite live region), never while typing (FR-050); test in `APPT/screens/TranslatorScreenTest.kt`

**Checkpoint**: US1 works with the fake engine in tests and with the real engine and a locally installed pack on the emulator.

---

## Phase 5: User Story 2 – Download a language (Priority: P2)

**Goal**: the first time a language is needed, its size is shown and nothing downloads until the person confirms; cancel leaves nothing.

**Independent Test**: fresh install, English → Português: the offer shows the size; Download; Cancel leaves 0 MB; Download again; the translation appears by itself.

- [X] T039 [P] [US2] Write failing tests `FTT/presentation/PackOfferTest.kt` (ViewModel with `FakeContentPacks`): offer with size, nothing fetched before Download; progress; Cancel → `NotInstalled` and nothing left; Wi-Fi only and no Wi-Fi → `Waiting(FOR_WIFI)` with Network settings; no space → the bytes needed with Storage settings; failed → Try again; no store → no button; a completed download continues the translation the person wanted
- [X] T040 [US2] Implement `core/content/src/androidMain/kotlin/tech/onova/offlinetranslator/core/content/PlayContentPacks.kt` (`AssetPackManager`: `getPackStates` for sizes, `fetch` only from `download`, the state listener, `cancel`, `removePack`, `getPackLocation`, `APP_UNAVAILABLE`/`PLAY_STORE_NOT_FOUND` → `Unavailable(NO_STORE)`), `WifiCheck.kt` and `FreeSpace.kt` (`StatFs`); bind in `APP/di/AppModule.kt`
- [X] T041 [US2] Implement `FT/presentation/PackOffer.kt` (a `Notice` with Not now and Download; progress with Cancel; the waiting, no-space, failed and no-store forms of contracts/ui.md) and its ViewModel part until T039 passes; strings in three languages
- [X] T042 [US2] Add the pack journey to `scripts/ci.sh` e2e: build the bundle, `bundletool build-apks --local-testing`, `install-apks` on the emulator only (docs/testing.md gains the bundletool version); then, with the owner's consent, the same on the owner's phone and record research R5 (their phone has a Secure Folder, so check that bundletool installs for user 0 only) (emulator part done 2026-11-19: `scripts/ci.sh device` builds the bundle, runs bundletool 1.18.3 from `:androidApp:bundletoolClasspath` with the SDK's aapt2 and Kotlin's stdlib, and installs through local testing; R5 answered 2026-11-19 on the owner's phone, with consent: user 0 only, data kept; research.md)

**Checkpoint**: US1 + US2 work end to end on the emulator through local testing.

---

## Phase 6: User Story 3 – Manage the languages (Priority: P3)

**Goal**: Settings lists the packs with sizes and states; delete and download from there; the Wi-Fi switch.

**Independent Test**: Settings → Offline languages shows each pack and the total; Delete asks and frees the space; the Translator then offers it again.

- [X] T043 [P] [US3] Write failing tests `APPT/screens/OfflineLanguagesSectionTest.kt`: one row per language with size and state; actions behind ⋮ (`RowMenu`); Delete asks first; the total; the `SwitchRow` "Download only on Wi-Fi" is remembered; deleting a pack in use cancels that translation first
- [X] T044 [US3] Implement `FT/presentation/OfflineLanguagesSection.kt` (public composable, like `RemoveAdsSettingsSection`) until T043 passes; strings in three languages
- [X] T045 [US3] Add an `offlineLanguages: (@Composable (Modifier) -> Unit)?` slot to `SET/SettingsScreen.kt` (after "Your data") and wire it in `APP/navigation/AppNavHost.kt`

**Checkpoint**: US1–US3 complete: the text translator is shippable on its own.

---

## Phase 7: User Story 4 – Speak instead of typing, and hear the result (Priority: P4)

**Goal**: dictate the text with the microphone; hear the translation in an offline voice.

**Independent Test**: airplane mode, on the owner's phone: tap the microphone, say "Onde fica a estação de trem?", see the words while speaking and the Spanish translation within 2 s of the end; Read aloud speaks it.

- [X] T046 [P] [US4] Write failing tests `FTT/presentation/SpeakAndHearTest.kt` with `FakeSpeechInput`/`FakeSpeechOutput`: the first microphone tap shows the permission gate, never the tool's start; partial words go into the field; a 2 s pause or a second tap ends listening; spoken text is translated like typed text; Read aloud ↔ Stop; `NO_RECOGNIZER`/`UNSUPPORTED` explain and keep typing; `CAN_DOWNLOAD` offers Get this language (`requestLanguage`); `NO_OFFLINE_VOICE` offers Voice settings; listening stops when the screen is hidden (`AppVisibility`)
- [X] T047 [US4] Implement the speech part of `FT/presentation/TranslatorViewModel.kt` and the microphone as the text field's trailing action with `PermissionGate` (`RECORD_AUDIO`) above the field in `FT/presentation/TypeOrSpeakContent.kt`, until T046 passes; strings in three languages
- [X] T048 [US4] Enable Read aloud (filled, last) with the target language's `speechTag` from `SpeechRegions`; with TalkBack on, keep Read aloud (the person asked for it) but never speak by itself

**Checkpoint**: US4 verified on the phone in the owner's next pass (T066).

---

## Phase 8: User Story 5 – A conversation between two people (Priority: P5)

**Goal**: two facing halves, one language each; each spoken turn appears translated on the other half and is read aloud; no microphone listens while the phone speaks.

**Independent Test**: airplane mode, Português and Español: a turn on each half is shown and heard translated on the other within 3 s; Copy gives every turn.

- [X] T049 [P] [US5] Add `DS/components/FacingHalves.kt` (two equal halves, the far one turned 180° unless `farTurned = false`; reading order normal for TalkBack) and `DS/components/MicrophoneButton.kt` (at least `PocketSizes.microphoneButton = 72.dp`, labelled with its language and state), with the new tokens in `DS/tokens/`; design-system tests and screenshots (light, dark, 200%)
- [X] T050 [P] [US5] Write failing tests `FTT/presentation/ConversationViewModelTest.kt`: the machine of T017 driven through the fakes; translation shown on the other half and spoken in its language; microphones disabled while speaking; correcting a turn translates it again; with `FakeScreenReader.on = true` the translation is announced, not spoken; `Paused` when hidden or on audio focus loss; Copy/Share text from `ConversationText`; Clear asks first; the screen kept on (`KeepScreenOn`)
- [X] T051 [US5] Implement `FT/presentation/ConversationViewModel.kt` until T050 passes
- [X] T052 [US5] Implement `FT/presentation/ConversationContent.kt` per contracts/ui.md "Conversation": `FacingHalves` with a `MenuChoice`, the turns and a `MicrophoneButton` per half; missing pack, recognizer or voice shown inside the half that needs it; `MoreControls` (turn the far half, read aloud on/off) and Copy, Share, Clear outlined at the bottom edge; strings in three languages
- [X] T053 [P] [US5] Write `APPT/screens/ConversationScreenTest.kt`: both halves, labels, the far half's content in reading order, actions at the bottom, nothing below them
- [X] T054 [US5] Add rule **C1** (two facing halves, each with its own language, turns and microphone; nothing else inside the halves) and the **A3 exception** (no filled main action at the bottom; the microphones are the main actions; approved by the owner 2026-11-17) to `docs/ui-rules.md`, with the component and the test that guard them

**Checkpoint**: all five stories work; the phone pass confirms voice (T066).

---

## Phase 9: Open-source licenses (research R4, decision 16)

**Purpose**: Settings → About → Open-source licenses, generated at build time and drawn with the app's components.

- [X] T055 Apply the AboutLibraries plugin in `androidApp/build.gradle.kts` (export to a Compose resource; `offlineMode = true` so the build never asks the network for licenses: they come from the POMs) and add `config/libraries/` entries: `lucide.json` (ISC, "Copyright (c) Lucide Icons and Contributors", https://github.com/lucide-icons/lucide), `feather.json` (MIT, Cole Bemis), `bergamot.json` (MPL-2.0, https://github.com/mozilla/translations/tree/main/inference), one entry per third-party library T027's script listed, and `translation-models.json` (MPL-2.0, Mozilla's model registry)
- [X] T056 [P] Write failing tests `APPT/screens/LicensesScreenTest.kt`: one row per project with its license; a tap opens the full text in a bottom sheet with the source link; Lucide, Feather, Bergamot and the models are listed; every shipped artifact has a license (none empty)
- [X] T057 Implement `SET/licenses/LicensesScreen.kt` (intro line, rows grouped by author and sorted by name, license sheet, source link through `LocalUriHandler` like the Privacy policy row) and the Settings → About row "Open-source licenses" in `SET/SettingsScreen.kt`, with its route in `APP/navigation/AppNavHost.kt`; strings in three languages; until T056 passes

---

## Phase 10: Polish and cross-cutting concerns

- [X] T058 [P] Help: `FT/presentation/TranslatorHelp.kt`, `ToolHelp` blocks of at most 180 characters in three languages (both modes, offline once a pack is in, voice depends on the phone, travel and everyday use, not medical or legal; never that nothing is kept or sent, rule X5); `HelpTextsTest` and `PrivacySaidOnceTest` pass
- [X] T059 [P] Accessibility: add the Translator (both modes, the pack offer), Offline languages and the licenses page to `APPT/screens/AccessibilityAuditTest.kt`, light and dark, 200%
- [X] T060 [P] Fit and rules: add `"translator"` to `LayoutFitTest.TOOL_IDS` and a Conversation surface; `UiRulesTest` and `ModeStabilityTest` pass (the languages don't move between modes)
- [X] T061 [P] Screenshots in `APPT/screens/TranslatorScreenshotTest.kt` (Type or speak empty and translated, the pack offer downloading, Conversation with turns, Offline languages, the licenses page and its sheet) in light, dark and 200%; record them and look at each one before committing
- [X] T062 [P] Architecture: a Konsist test in `tooling/architecture-tests` that `feature/tools-translate` imports neither `core.content` nor `core.translation`, and that only `androidApp` depends on those modules
- [X] T063 [P] Data transfer: add `translator` to `core/data`'s `AppPartsTest` expectations; an exported file holds the settings and no pack
- [X] T064 End to end: `tooling/e2e/src/main/kotlin/tech/onova/offlinetranslator/e2e/TranslatorJourney.kt`: install a pack through local testing, airplane mode on (emulator), translate the test sentence, swap, Copy; delete the pack in Settings (green 2026-11-19 in the full e2e run, 36 of 36)
- [X] T065 Docs: the module map in `CLAUDE.md` (core/content, core/translation, feature/tools-translate, packs), `docs/how-to-build-a-tool.md` (native code and packs), `docs/testing.md` (bundletool local testing), `docs/status-2026-11-08.md`
- [ ] T066 `scripts/ci.sh check` and `scripts/ci.sh e2e` green; then [phone] the owner's pass of quickstart.md §3 and §4 (both modes, airplane mode, TalkBack), with consent; results in `research.md` (2026-11-19: `scripts/ci.sh check` OK, a11y 57/57 and e2e 36/36; the owner's phone pass remains)
- [X] T067 Run `/speckit-converge` until Converged; then the pull request (no merge) (2026-11-19: Phase 11 built, a second pass found nothing left; PR #91, a draft stacked on #90)

---

## Dependencies

```text
Phase 1 Setup ─► Phase 2 Foundational ─► Phase 3 Engine, packs, phone research
                                              │
                         ┌────────────────────┴────────────────┐
                         ▼                                     ▼
                  US1 (P1) ─► US2 (P2) ─► US3 (P3)      Phase 9 Licenses (needs T027's third-party list)
                         │
                         ▼
                  US4 (P4) ─► US5 (P5)
                                   │
All stories ───────────────────────┴─► Phase 10 Polish
```

- US1 can be built and tested with the fakes before Phase 3 ends; it needs T029 and T030 only for the emulator run.
- US2 needs T040's `PlayContentPacks` and T042's local testing; US3 needs US2's states.
- US4 needs T025 and T033's answers (whether the phone has an on-device recognizer); US5 needs US4.
- Phase 9 needs T027 (the engine's third-party list) and nothing from the stories.

## Parallel opportunities

- Phase 1: T002–T006 together (different files).
- Phase 2: T009–T012 (ports) together; then T013–T018 (failing tests) together; T022 and T023 together.
- Phase 3: T025 (speech) beside T026–T028 (native build, long, in the background) and T030 (packs).
- US1: T034 and T035 together. US5: T049, T050 and T053 together.
- Phase 10: T058–T063 together.
- On this 2-core machine "together" means written together; Gradle runs one build at a time.

## Implementation strategy

1. **MVP**: Phases 1–3, then US1 (text translation with a locally installed pack). Stop and check on the emulator.
2. **Shippable text translator**: + US2 and US3 (download and manage). The release needs the Play Console
   account for real downloads; local testing covers the rest.
3. **Voice**: + US4 once T033 confirms the owner's phone has an on-device recognizer; then US5.
4. **Release-ready**: Phase 9 (licenses) and Phase 10. One pull request per phase group, stacked, none merged by the
   agent.

---

## Phase 11: Convergence

- [X] T068 When Play answers `REQUIRES_USER_CONFIRMATION` (or its own `WAITING_FOR_WIFI`), let the offer's button open Play's confirmation (`AssetPackManager.showConfirmationDialog`) through a `ContentPacks` port action, instead of fetching into the same state again; reword `translator_waiting_confirmation` in three languages; tests in `FTT/presentation/PackOfferTest.kt` and `core/content`'s `PlayPackMappingTest` per US2, FR-012, research R5 (partial) (as built: `ContentPacks.confirm(pack)`; `PlayContentPacks` calls `showConfirmationDialog(activity)` with the resumed activity from `CurrentActivityTracker`; `PacksViewModel.download` confirms instead of fetching for a pack in `Waiting(FOR_CONFIRMATION)`, so the offer's **Confirm** and Settings' Download both open it. Play's own `WAITING_FOR_WIFI` stays the Wi-Fi notice: Play waits for Wi-Fi only for large downloads on mobile data, and the packs are about 70 MB)
- [X] T069 On Android 12 and later without an on-device recognizer, keep the microphone in Type or speak and have its tap explain that this phone can't recognize speech offline (typing stays); hide it only below Android 12: split `NO_RECOGNIZER` in `PLAT/speech/AndroidSpeechInput.kt`/`SpeechControls.canSpeak`, update `TranslatorScreenTest`'s no-recognizer test per US4/AC5, FR-021 (partial) (as built: `RecognitionSupport.SYSTEM_TOO_OLD` below Android 12 hides the microphone; `NO_RECOGNIZER` shows it, and a tap says "This phone has no offline speech recognition. You can still type." A phone that can't listen to the language explains at once, without asking for the microphone first)
- [X] T070 Show that a translation is running while a newer one replaces the translation on the screen (`TranslationView.Shown.pending`), with a design-system progress line that moves nothing (M1–M5, `ReservedLine`), in `FT/presentation/TypeOrSpeakContent.kt`; test in `APPT/screens/TranslatorScreenTest.kt` per US1/AC7 (partial) (as built: an indeterminate `LinearProgressIndicator` under the translation and its note, in a `ReservedSlot` kept in every window, so nothing moves)
- [X] T071 Add a Help line saying to pick the text's own language as the source (a wrong one gives nonsense) to `translator_help` in three languages, within `HelpTextsTest`'s limits, per spec edge case "Wrong source language" (missing)
- [X] T072 Treat a pack whose models fail to load as not installed and offer it again for that language only, instead of `DAMAGED_PACK` blocking every pair with "Delete it in Settings" (`BergamotEngine.open`, `EngineSession`, `PackMessages`); tests in `FTT/domain/EngineSessionTest.kt` per spec edge case "Pack removed by the system or damaged" (partial) (as built: `EngineOpen.Damaged(packs)`: `BergamotEngine` opens the readable packs and names the others; `EngineSession` removes those through `ContentPacks.remove`, so the offer returns; a store that keeps the pack gives the old "Delete it in Settings" message. A model that fails inside the native engine is still `DAMAGED_PACK`)
- [X] T073 Report audio-focus loss (a call or an alarm) from the speech ports so Conversation dispatches `Interrupted(CALL_OR_ALARM)` and shows Paused until a tap (`AndroidSpeechOutput`, `AndroidSpeechInput`, `ConversationViewModel`); test in `FTT/presentation/ConversationViewModelTest.kt` per spec edge case "A phone call or an alarm", T050 (partial) (as built: `SpeechOutput.speak` returns `SpeechEnd` (DONE, INTERRUPTED, FAILED); `AndroidSpeechOutput` reports a lost or refused audio focus as INTERRUPTED, and `ConversationTurns` turns it into `Interrupted(CALL_OR_ALARM)`. Listening during a call already ends with the microphone-busy message)
- [X] T074 Stop a running translation cleanly when the Translator's screen is hidden (`AppVisibility` in `FT/presentation/TranslatorViewModel.kt`) and translate the current text again when it shows; test in `FTT/presentation/TranslatorViewModelTest.kt` per spec edge case "Memory" (partial) (as built: `TranslatorViewModel` takes `AppVisibility`; hidden, the running translation is dropped and nothing starts; shown again, the text is translated unless its translation is already on the screen)
- [X] T075 Measure the release package's growth against `main` without the engine's `.so` (`bundletool get-size total` per ABI on a release-like bundle) and record it in `research.md` per SC-006 (partial) (2026-11-19: +0.19 MB to download without the native code; research.md "SC-006")
- [X] T076 List the asset-delivery library's `FOREGROUND_SERVICE_DATA_SYNC` permission (unpacking a pack; Google Play asks for its declaration at release) in `docs/decisions-pending.md` for the owner, since the plan's check said no new permission per plan: Constitution Check IX (contradicts) (row 17)

---

## Phase 12: The owner's phone pass (2026-11-19)

The owner tried both modes on the SM-G780G: "Everything is working fine, but with some caveats".

- [X] T077 Conversation: what the phone's owner sets up (the pack offer, speech the phone can get, a voice, speech it can't) shows upright in the near half, one thing at a time, both languages' (`SetupNeeds`, `SetupNeedsTest`; `ConversationContent`, `ConversationScreenTest`); C1 in `docs/ui-rules.md`, contracts/ui.md and story 5, scenario 6 updated. Owner: "make both respect the smartphone orientation, with the first popup appearing on the bottom side"
- [X] T078 A language the phone downloads counts without leaving the tool: `RecognitionSupport.DOWNLOADING` for a pending language ("The phone is downloading … for speech recognition"), and the phone asked again every 3 s while one is on its way (at most 5 minutes) and whenever the screen comes back (`SpeechRecheck`, both modes; `SpeakAndHearTest`, `ConversationViewModelTest`). Owner: "After downloading the language pack, the tool doesn't update automatically"

The owner's second look, the same day ("Almost there"):

- [X] T079 A language the phone just downloaded isn't offered again: Android's speech service drops it from "pending" a moment before listing it as installed (the phone's log: 48 ms at 12:00:28), so for 5 checks after a download "can download" reads as still downloading (`SpeechDownloads`, `SpeechDownloadsTest`, both modes). Owner: "after the download it displayed for me to download again"
- [X] T080 The far half's language list is turned to the person across and opens under its chip as they see it: `FacingHalves` provides `LocalHalfTurned`, and `MenuChoice` then draws its own turned list placed from the chip as drawn (`ConversationScreenTest`). Owner: "the drop-down on the other side of the screen is with the wrong orientation"
- [X] T081 Before the first word is heard: "Ouvindo." → "Ouvindo.." → "Ouvindo..." each second (`WaitingText`, an infinite transition, the same room throughout, read once by TalkBack), in a Conversation turn and in Type or speak's empty field. Owner: "instead of a plain _"
- [X] T082 A round button with a square beside each listening microphone, "Stop and discard": listening stops and only the turn being heard goes, untranslated (`StopButton` in a `ReservedSlot`; `ConversationEvent.Discard`; `ConversationMachineTest`, `ConversationViewModelTest`, `ConversationScreenTest`). Owner: "add another round button ... to stop the listening and delete only the current message the user spoke"

Phase 12 verified 2026-11-19 with the full `scripts/ci.sh` (check, a11y 57/57, e2e 36/36) and installed on the owner's
phone at 16:55; the 200% Conversation screenshot was re-recorded for the stop button's kept room (compared, `8e9d1de6`).

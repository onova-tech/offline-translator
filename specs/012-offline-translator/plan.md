# Implementation Plan: Offline Translator, by text and by voice

**Branch**: `012-offline-translator` | **Date**: 2026-11-17 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/012-offline-translator/spec.md` (voice and Conversation added 2026-11-16)

## Summary

A Translator tool (`translator`, Utilities) with two modes: **Type or speak** (type, paste or dictate; the
translation below, read aloud on request) and **Conversation** (two facing halves, one language each; each spoken
turn appears translated on the other half and is read aloud). Everything runs on the phone.

- **Translation**: Mozilla's Bergamot engine (C++, MPL 2.0), built once for `arm64-v8a` and `x86_64` and committed
  as `.so` files, reached through JNI (research R3). Models: Mozilla's released base-memory models for en↔pt and en↔es
  (R4); pairs without English go through English.
- **Packs**: one on-demand Google Play asset pack per language (`translate_pt`, `translate_es`, about 48 MB each),
  downloaded only after the person confirms its size, in the new `core/content` module (R5, R6).
- **Voice**: the phone's own on-device recognizer (Android 12+) and offline text-to-speech voices, through new ports
  in `core/domain`, Android implementations in `core/platform` (R1, R2). No audio is stored or sent.
- **iPhone later**: every platform service is a port; the engine is the same C++; iOS adds implementations only
  (FR-029).

The plan's first tasks are the four research items on the owner's phone (research.md, last table).

## Technical Context

**Language/Version**: Kotlin 2.4.21 (Multiplatform; Android now), C++17 for the engine and its JNI layer (built
outside Gradle).

**Primary Dependencies**: Compose Multiplatform 1.12.2, Koin, DataStore, kotlinx-coroutines (all pinned already);
new: Bergamot from `mozilla/translations` `inference/` (v0.6.0, a pinned commit), and
`com.google.android.play:asset-delivery-ktx:2.3.0` (latest on Google's Maven, checked 2026-11-17), in `core/content`
only; the AboutLibraries Gradle plugin and core (15.3.0, Apache 2.0, build-time collection, no network at run time)
for the Open-source licenses page. Android's `SpeechRecognizer` and `TextToSpeech` (platform APIs, no library).

**Storage**: `translator.json` (DataStore, `schemaVersion` 1, settings part `translator` for spec 010); the packs in
Play's asset pack storage, never exported (FR-015). Nothing else.

**Testing**: kotlin.test and coroutines-test (domain test-first, Kover ≥ 90%), Robolectric Compose UI tests,
Roborazzi screenshots, `LayoutFitTest`/`UiRulesTest`/`ModeStabilityTest`, the accessibility audit, Konsist
architecture tests, an instrumented engine test and an end-to-end journey on the `x86_64` emulator with bundletool's
local testing (R10).

**Target Platform**: Android 8.0+ (minSdk 26) for text; voice on Android 12+ (on-device recognizer); the engine on
`arm64-v8a` and `x86_64` (32-bit phones show the tool as unavailable). iOS later, behind the same ports.

**Project Type**: mobile app (Kotlin Multiplatform, Compose).

**Performance Goals**: a 30-word sentence in under 2 s on the owner's phone (SC-001); a spoken sentence translated
within 2 s of its end and heard within 3 s in a conversation (SC-007).

**Constraints**: works in airplane mode once a pack is in (Principle I); the only network is the confirmed pack
download, in `core/content`; release package grows by at most 2 MB plus the engine's native code (SC-006); memory
with two models loaded checked on the phone (R3).

**Scale/Scope**: 3 languages (en, pt, es), 2 packs, 2 modes, 1 Settings section, 1 licenses page, 1 debug probe page.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design (below).*

| Principle | How this plan meets it | Pass |
|---|---|---|
| I. Offline-first | Translation, recognition and voices on the device (on-device recognizer only, offline voices only); the one network use is the pack download, confirmed with its size, cancellable, in `core/content`, delivered by Google Play; packs are data only; airplane-mode checks in quickstart | ✅ |
| II. Device-side only | No server; text, turns and audio never stored; settings exported as a normal part; packs excluded | ✅ |
| III. Clean architecture | Ports in `core/domain`; Play, JNI and speech code only in platform modules; the feature depends on ports only | ✅ |
| IV. Clean code | Named constants (limits, delays), sealed states, detekt and ktlint as usual | ✅ |
| V. Test-first domain | Routes, limits, debounce, the conversation's state machine, the region rule and pack messages written test-first (R10) | ✅ |
| VI. Respectful monetization | A normal tool: counts toward the daily ad rule, nothing behind the purchase | ✅ |
| VII. Modern, localized UI | Three languages; new visuals (`FacingHalves`, the large microphone) go into `core/designsystem` first | ✅ |
| VIII. Multiplatform-ready | `ContentPacks`, `TranslationEngine`, `SpeechInput`, `SpeechOutput`, `ScreenReader` as ports; Bergamot builds for iOS (R3) | ✅ |
| IX. Least permission | `RECORD_AUDIO` (already declared) asked through `PermissionGate` on the first microphone tap only; no new permission; `<queries>` entries aren't permissions | ✅ |
| X. Accessible | Labels, one announcement per translation, TalkBack mode for conversations, 200% and landscape covered by the fit test, a TalkBack pass on the phone | ✅ |
| Technology constraints | Play on-demand delivery is an approved network library, in `content` only; translation from content packs, not ML Kit or online services; versions pinned | ✅ |

No violation: Complexity Tracking stays empty.

## UI Rules Check

*Screens: Type or speak, Conversation, the pack offer, Settings → Offline languages, Settings → About → Open-source
licenses. Details in [contracts/ui.md](contracts/ui.md).*

- **Mode**: Type or speak | Conversation as the top `SegmentedChoice` (S1); the languages keep their place
  (M1–M5, `ReservedSlot`).
- **Type or speak**: translation in the middle, the only scrolling part (L1–L5); at the bottom the languages
  (`MenuChoice` ×2 with the swap), the text (`PocketTextField` with the microphone as trailing action and the
  counter), then Copy and Share outlined and **Read aloud** filled and last (A1–A4, `BottomActionRow`); one
  `ShortNote` (R4); the microphone's `PermissionGate` above the field.
- **Conversation**: new **C1** (two facing halves) and the design-system component `FacingHalves`, added to
  `docs/ui-rules.md` in this pull request; More options (`MoreControls`) and Copy, Share, Clear at the bottom edge.
  **Exception to A3** (no filled main action at the bottom: the microphones inside the halves are the main actions)
  needs the owner's reason in the rulebook; listed in `docs/decisions-pending.md`.
- **Pack offer**: a `Notice` with Not now and Download; progress with Cancel.
- **Offline languages**: `SwitchRow` for Wi-Fi only; rows with ⋮ (`RowMenu`, A6).
- **Text**: `ToolHelp` blocks ≤ 180 characters (X1–X4); snackbars in a `PocketSnackbarHost` (V3).
- `translator` joins `LayoutFitTest.TOOL_IDS`, plus a Conversation surface; under `UiRulesTest` and
  `ModeStabilityTest` too.

## Project Structure

### Documentation (this feature)

```text
specs/012-offline-translator/
├── spec.md
├── plan.md              # this file
├── research.md          # Phase 0: R1–R10, four items to close on the owner's phone
├── data-model.md        # Phase 1: languages, packs, settings, requests, speech support, the conversation
├── quickstart.md        # Phase 1: how to validate on the emulator and the phone
├── contracts/
│   ├── ports.md         # ContentPacks, TranslationEngine, SpeechInput, SpeechOutput, ScreenReader, fakes
│   ├── pack-format.md   # a pack's files and pack.json
│   └── ui.md            # the screens against docs/ui-rules.md
├── checklists/requirements.md
└── tasks.md             # Phase 2 (/speckit-tasks), not created here
```

### Source Code (repository root)

```text
core/domain/src/commonMain/kotlin/tech/onova/offlinetranslator/core/domain/ports/
├── ContentPorts.kt          # ContentPacks, PackId, PackStatus, PackState
├── TranslationPorts.kt      # TranslationEngine, Route, EngineOpen
├── SpeechPorts.kt           # SpeechInput, SpeechEvent, SpeechOutput, RecognitionSupport, VoiceSupport
└── ScreenPorts.kt           # + ScreenReader

core/content/                         # NEW, network allowed (Principle I); only androidApp depends on it
└── src/androidMain/kotlin/.../content/
    ├── PlayContentPacks.kt           # AssetPackManager: states, fetch, cancel, remove, location
    └── WifiCheck.kt

core/translation/                     # NEW; only androidApp depends on it
├── NATIVE.md                         # commit, NDK, flags and SHA-256 of each .so
├── native/bergamot_jni.cpp           # our JNI layer (built by the script, not by Gradle)
└── src/androidMain/
    ├── jniLibs/arm64-v8a/libbergamot.so
    ├── jniLibs/x86_64/libbergamot.so
    └── kotlin/.../translation/BergamotEngine.kt

core/platform/src/androidMain/kotlin/.../platform/speech/
├── AndroidSpeechInput.kt             # on-device recognizer only (R1)
├── AndroidSpeechOutput.kt            # TextToSpeech, offline voices only (R2)
└── AndroidScreenReader.kt

core/designsystem/src/commonMain/kotlin/.../designsystem/components/
├── FacingHalves.kt                   # NEW: two halves, the far one turned
└── MicrophoneButton.kt               # NEW: the large labelled microphone

feature/tools-translate/              # NEW feature module
└── src/commonMain/kotlin/tech/onova/offlinetranslator/feature/toolstranslate/
    ├── domain/                       # Routes, TextLimit, ConversationMachine, ConversationText, SpeechRegions,
    │                                 # PackMessages, TranslatorRules
    ├── data/                         # TranslatorSettingsDocument, DataStore repository, DataPart "translator"
    ├── presentation/                 # TranslatorViewModel, ConversationViewModel, TypeOrSpeakContent,
    │                                 # ConversationContent, PackOffer, OfflineLanguagesSection, TranslatorHelp
    ├── TranslatorEntry.kt            # ToolEntry: id "translator", UTILITIES
    └── di/ToolsTranslateModule.kt
    (commonTest: the domain and ViewModel tests with the fakes)

feature/settings/                     # About → Open-source licenses row and page (drawn from AboutLibraries' generated
                                      # list with the app's components); debug-only probe row
config/libraries/                     # entries AboutLibraries can't find in Maven: Lucide (ISC) and Feather (MIT) for
                                      # the icons, Bergamot and its native libraries, the models (MPL 2.0)
packs/translate_pt/, packs/translate_es/   # NEW asset pack modules (on-demand); model files git-ignored

androidApp/
├── src/main/AndroidManifest.xml      # <queries>: RecognitionService, TTS_SERVICE
├── build.gradle.kts                  # assetPacks, ABI splits, the new modules
└── .../AppModule.kt, AppNavHost.kt   # bindings; the Offline languages slot in Settings

scripts/build-bergamot-android.sh, scripts/fetch-language-packs.sh
tooling/e2e/.../TranslatorJourney.kt  # text through a locally installed pack, airplane mode
docs/ui-rules.md                      # C1 and the A3 exception; docs/how-to-build-a-tool.md: native and packs
```

**Structure Decision**: the Kotlin Multiplatform layout the app already has. Two new core modules isolate what is
special here: `core/content` (the only network) and `core/translation` (the only native code). The feature module
depends on ports alone, so it compiles and tests without either, and the iPhone adds `iosMain` implementations to
the same modules.

## Post-design Constitution Check

Re-checked after Phase 1: the contracts keep every platform service behind a port (VIII), the only network call is
`ContentPacks.download` after confirmation (I), the microphone opens only through `PermissionGate` and only while the
screen is visible (IX), nothing in the data model is stored beyond `translator.json` (II), and every new visual is a
design-system component (VII). **Still passing.**

Open points for the owner, added to `docs/decisions-pending.md` (spec 012): the A3 exception for Conversation; the
debug-only probe page; committing the two prebuilt `.so` files (about 10–20 MB) to the repository.

## Complexity Tracking

No constitution violation to justify.

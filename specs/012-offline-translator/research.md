# Research: Offline Translator, by text and by voice (spec 012)

Phase 0 of the plan, 2026-11-17. The owner's choices (spec, Clarifications 2026-11-16) are inputs here, not
questions: Bergamot on both platforms, each phone's own on-device speech and voices, Type or speak and Conversation,
Android now and iPhone later. Each item below is a Decision, its Rationale and the Alternatives set aside.

Items marked **[phone]** can only be closed on the owner's phone, in the plan's first tasks; until then the plan
holds a default that the result may change. Everything else was checked on 2026-11-17 against the sources named.

---

## R1. Speech recognition on the device [phone]

**Decision**: Android's `SpeechRecognizer.createOnDeviceSpeechRecognizer(context)` (API 31+), never the plain
`createSpeechRecognizer`. Availability is `SpeechRecognizer.isOnDeviceRecognitionAvailable(context)`; creating it
is wrapped in a try/catch, since some phones throw when no on-device service exists. On API 33+,
`checkRecognitionSupport(intent, …)` tells, per language, whether it is installed, being downloaded, or only
supported; when it is supported but not installed, the tool offers "Get this language", which calls
`triggerModelDownload(intent)`: the phone's own speech service downloads it, with its own notice, and the app opens
no connection (Principle I). On API 31–32 there is no such check: the tool tries, and maps
`ERROR_LANGUAGE_NOT_SUPPORTED`/`ERROR_LANGUAGE_UNAVAILABLE` to the same "missing" message. Below API 31 the voice
controls are not shown (spec edge case).

The intent asks for partial results (`EXTRA_PARTIAL_RESULTS`), the language as a BCP-47 tag (R9) and a 2-second
silence end (`EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS`, a hint the service may ignore); the tool also ends a
turn on its own after 2 s with no new partial result (FR-023), so both paths behave the same.

The manifest declares `<queries><intent><action android:name="android.speech.RecognitionService"/></intent></queries>`
(package visibility, Android 11+); without it the service isn't found on target SDK 30+. The app already holds
`RECORD_AUDIO`, which the recognizer requires of its caller.

As built (T025, 2026-11-17, `core/platform/.../speech/SpeechMapping.kt`, tested in `SpeechMappingTest`):
- **Errors:** `ERROR_NO_MATCH` and `ERROR_SPEECH_TIMEOUT` mean nothing heard. The two language errors mean the language
  is missing. `ERROR_RECOGNIZER_BUSY`, `ERROR_AUDIO` and `ERROR_TOO_MANY_REQUESTS` mean busy.
  `ERROR_INSUFFICIENT_PERMISSIONS` means no permission. Anything else (client, server, network, disconnected) is "other".
- **Language lists:** a tag in `installedOnDeviceLanguages` is ready. One in `pendingOnDeviceLanguages` or
  `supportedOnDeviceLanguages` can be downloaded. Tags compare without case and with `_` read as `-`, and another
  region doesn't count.
- **Getting a language:** the recognizer queues `triggerModelDownload` until its service connects, and `destroy()` drops
  the queue. So the recognizer is destroyed 10 s later, not at once.

**[phone] to check**: whether the owner's Samsung has an on-device recognizer at all, and which of en-US, pt-BR and
es-ES/es-419 are installed or downloadable. Android 15 and earlier ship Google's recognizer as cloud-first; only the
on-device path is allowed here, so a phone without it keeps text only (FR-021).

**Found on the owner's phone (2026-11-18, Samsung SM-G780G, Android 13, API 33; T033's probe)**: it has an
on-device recognizer, Android System Intelligence's (`com.google.android.as`, `AiAiSpeechRecognitionService`, with
Google's offline speech packs). **pt-BR is ready**; en-US, es-ES and es-US can be downloaded (`CAN_DOWNLOAD`), which the
phone's own service does with "Get this language". So voice can be built (US4), and the "Get this language" path is
needed on the owner's own phone for English and Spanish.

**Alternatives**: the plain recognizer with `EXTRA_PREFER_OFFLINE` (only a preference: it may send audio, which
Principle I forbids); our own Whisper pack (owner set it aside for a later spec); ML Kit's speech APIs (banned by the
constitution).

**iPhone later**: `SFSpeechRecognizer(locale:)` with `requiresOnDeviceRecognition = true` and
`supportsOnDeviceRecognition` as the check (iOS 13+); on iOS 26, `SpeechTranscriber`/`SpeechAnalyzer` are the newer
on-device path. Both fit the same port (contracts/ports.md, `SpeechInput`).

## R2. Reading aloud [phone]

**Decision**: Android's `TextToSpeech` with the phone's default engine. For a language, the tool picks a `Voice`
whose locale matches (R9), with `isNetworkConnectionRequired == false` and without
`TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS` in its features; the engine's `getVoices()` lists them. No offline
voice means "Read aloud" explains it and opens the phone's text-to-speech settings
(`com.android.settings.TTS_SETTINGS`), where voices are added (spec story 4, scenario 6). Speaking requests audio
focus (transient, may duck) and reports its end through `UtteranceProgressListener`, which the Conversation uses to
turn the microphones back on (FR-025).

The manifest declares `<queries><intent><action android:name="android.intent.action.TTS_SERVICE"/></intent></queries>`.

**[phone] to check**: the owner's default engine (Samsung's or Google's) and its offline voices for the three
languages.

**Found on the owner's phone (2026-11-18, same phone)**: the default engine is **Samsung's** (`com.samsung.SMT` 3.5.02.16);
Google's (`com.google.android.tts`) is installed too. Samsung's has offline voices, preinstalled, for en-US, en-GB,
pt-BR, es-ES and es-MX (a "default" voice of quality 300 and an "f00" voice of quality 400 for each). Google's has
local pt-BR, pt-PT and English voices on the phone, and lists its Spanish local voices as `notInstalled`.

Samsung's engine names its voices' locales in **three-letter codes with a variant**: `por_BRA_f00`, `spa_ESP_f00`,
`eng_USA_default`. The first build compared `locale.language` with the two-letter `pt`, so the probe said "no offline
voice" for every language. Fixed the same day: `SpeechMapping.offlineVoice` compares three-letter language and region
codes (a region already in three letters is kept as it is: Android's `isO3Country` does that, the JDK's throws), takes
the region first, then the best quality, and is tested with the phone's own locales (`SpeechMappingTest`). The probe
then found a voice for en-US, pt-BR, es-ES and es-US (a Spanish voice of another region).

**Alternatives**: our own voices (Piper, Kokoro: 30–100 MB a language and a native runtime; not asked for).

**iPhone later**: `AVSpeechSynthesizer` with `AVSpeechSynthesisVoice(language:)`; built-in voices work offline.

## R3. The translation engine and its native build [phone for speed]

**Decision**: Bergamot as maintained by Mozilla in `mozilla/translations`, folder `inference/` (BERGAMOT_VERSION
v0.6.0, last commit 2026-10-29). The older `mozilla/bergamot-translator` repository is archived (2023). The engine is
C++ (a Marian fork with SentencePiece); on ARM it uses Google's Ruy for matrix products (`USE_RUY`, on by default for
ARM in `marian-fork/CMakeLists.txt`), on x86_64 intgemm.

Built **once, outside Gradle**, by `scripts/build-bergamot-android.sh` with the Android NDK (r28 or the latest
stable at the time), for `arm64-v8a` (phones) and `x86_64` (the test emulator), at a pinned commit. The script also
compiles our small JNI layer (`bergamot_jni.cpp`) into the same `libbergamot.so`. The two `.so` files are committed
under `core/translation/src/androidMain/jniLibs/`, with the commit, flags and SHA-256 of each in
`core/translation/NATIVE.md`. Normal builds, `./gradlew check` and GitHub Actions never compile C++: the 2-core
machine would take most of an hour per ABI, and the CI budget can't pay for it on every run.

`armeabi-v7a` (32-bit phones) is not built: the translator shows as unavailable there, as any tool without its
hardware does (Principle IX), and the rest of the app is unaffected. The release bundle splits by ABI, so a phone
downloads only its own library.

The engine runs on its own single thread (one `TranslationService` with two worker threads inside); a new request
doesn't interrupt a running one, so the newest request wins and older results are dropped (FR-005). It is loaded when
the tool opens with an installed pack and released when the tool is left (spec edge case "Memory").

**[phone] to check**: library size per ABI (SC-006 allows the engine's native code), the time for a 30-word sentence
direct and through English (SC-001: under 2 s), and memory with two models loaded.

**As built (T027–T031, 2026-11-17)**:
- `libbergamot.so` is 8.2 MB on arm64-v8a and 8.5 MB on x86_64, stripped. `core/translation/NATIVE.md` has the
  hashes, the build's choices and the one source patch.
- The engine uses one `BlockingService` on one thread, not the threaded `TranslationService`. A model loads the first
  time its direction is used, so only the languages a person uses take memory.
- The emulator (x86_64, on the 2-core Celeron development machine) passed `BergamotEngineTest`
  (`scripts/ci.sh engine`). These times include loading each model:
  - pt→en, 2.7 s: "Good morning! Where is the nearest train station? I need to get to the center before noon,
    because I have an important meeting and I don't know this city well. Thank you so much!"
  - pt→es through English, 3.0 s: "¡Buenos días! ¿Dónde está la estación de tren más cercana? Necesito llegar al
    centro antes del mediodía, porque tengo una reunión importante y no conozco bien esta ciudad. ¡Muchas gracias!"
- **On the owner's phone (2026-11-18, Samsung SM-G780G, Snapdragon 865, Android 13; arm64-v8a with Ruy; two runs)**:
  - pt→en, the first time (the model loads): 496 ms and 503 ms; again: 159 ms and 160 ms.
  - pt→es through English, the first time for en→es (it loads; pt→en already loaded): 845 ms and 626 ms.
  - **SC-001 (under 2 s) passes** by a wide margin, so the base-memory models stay (no switch to "tiny").
  - The texts were the emulator's, but for "Thank you very much!" (the emulator said "Thank you so much!").
  - Memory (`Debug.getPss`, native heap in use): engine open, no model yet: 245 MB PSS, 25 MB native. With pt→en:
    425 MB, 360 MB. With pt→en and en→es: 512 MB, 694 MB. Each model takes about 335 MB of native heap (the 128 MB
    workspace is part of it), of which 90–180 MB is resident. Right after closing: native heap 32 MB, PSS still
    482 MB; a minute later `dumpsys meminfo` showed 251 MB PSS and 16 MB native heap: closing gives the memory back,
    and Android takes it in its own time. No criterion sets a limit; a phone with little memory may close the app
    in the background while two models are loaded, which the spec's "Memory" edge case already handles.

**Alternatives**: building through Gradle's `externalNativeBuild` on every build (too slow for this machine and CI);
CTranslate2 with OPUS-MT (also good, but a second model family and no iPhone path as tested as Bergamot's); a
prebuilt Bergamot AAR (none is published).

**iPhone later**: the same sources built with Xcode into an XCFramework (Apple Accelerate on Apple silicon, per the
same CMake file), reached from Kotlin through cinterop with the same C entry points as the JNI layer.

## R4. The models: license, size, quality

**Decision**: Mozilla's released **base-memory** models from its model registry
(`https://storage.googleapis.com/moz-fx-translations-data--303e-prod-translations-data/db/models.json`), for
en→pt, pt→en, en→es and es→en. `mozilla/translations`' README: "The model files are distributed under the MPL 2.0
license." Each direction has a model (`model.<pair>.intgemm.alphas.bin`, 31.5 MB uncompressed, 31.2 M parameters), a
lexical shortlist and a SentencePiece vocabulary; about 24 MB compressed per direction. Quality on flores200:
en→pt COMET-22 0.889 (BLEU 50.4). The registry gives each model's SHA-256, which the fetch script checks (R6).

**Pack size**: one pack per language holds both directions with English, about 48 MB to download and about 75 MB
installed. English needs no pack. Portuguese ↔ Spanish uses both packs (FR-004).

**License duties**: MPL 2.0 lets the app ship the models and the engine's binary; it asks that people can learn
where the source is. The app gains an **Open-source licenses** page (Settings → About), approved by the owner on
2026-11-17 ("the page must feel consistent and look good").

**Audit of what the app already ships (2026-11-17)**, from `:androidApp:dependencies --configuration
releaseRuntimeClasspath` (264 artifacts) and each artifact's POM in the Gradle cache:

| License | What | Duty |
|---|---|---|
| Apache 2.0 | AndroidX (Compose, CameraX, Room, DataStore, Navigation 3, Media3, WorkManager…), JetBrains Compose Multiplatform and lifecycle, Kotlin and kotlinx, Koin, ZXing, Okio, OkHttp, Guava, Dagger, AutoValue, Error Prone, Gson, Tink, J2ObjC annotations, JSpecify, JSR 305, javax/jakarta inject, Touchlab Stately, ionspin bignum | The license text and any NOTICE file (§4) |
| BSD-3-Clause | Protocol Buffers (lite), a part of CameraX | Copyright and license text |
| MIT | Checker Framework annotations | Copyright and license text |
| Chromium's (BSD-style) | Cronet, which the ads SDK brings | Its notices |
| Android SDK License (not open source) | Google Mobile Ads, UMP, Play services, Play libraries | Named, no open-source notice |
| **ISC (and MIT)** | **The app's own line icons**: drawn from Lucide's path data (the Settings icon is Lucide's older `settings` path, character for character); Lucide's older icons come from Feather (MIT) | Lucide's copyright and permission notice, and Feather's, in the app |
| MPL 2.0 (with this spec) | Bergamot (`mozilla/translations` `inference/`), the models | Where the source is |
| Apache 2.0 / MIT (with this spec) | Marian's fork, SentencePiece, Ruy, intgemm, and the other libraries the engine's build pulls in, listed by `scripts/build-bergamot-android.sh` | License texts |

No fonts, sounds or data files are bundled (the app uses the system font; its sounds are generated).

**How the page stays right**: the **AboutLibraries** Gradle plugin (`com.mikepenz.aboutlibraries.plugin`, 15.3.0,
Apache 2.0, no network at run time) collects every Maven dependency's name, license and source link at build time into
a resource; the page is drawn with the app's own components (`SettingsRow`-style rows, a license view), not the
library's UI, so it looks like the rest of Settings. Lucide, Feather, Bergamot and the models aren't Maven
dependencies: they're added as entries of the plugin's `config/libraries/` folder. A test fails when a shipped
artifact has no license in the generated list. Spec 011 (release readiness) can extend the page.

**Alternatives**: the "tiny" models (14–15 MB per direction, lower quality); the archived repo's models (same
license, older training).

## R5. Getting the packs onto the phone (Google Play on-demand delivery)

**Decision**: Play Asset Delivery **on-demand asset packs**, one per language (`translate_pt`, `translate_es`), with
`com.google.android.play:asset-delivery-ktx:2.3.0` (current release on Google's Maven, 2024-12-17) in `core/content`,
the only module the network guard and the architecture tests allow for content packs (Principle I). An on-demand pack
may hold up to 512 MB; ours are about 48 MB. Packs are data only (Principle I).

- **Size and confirmation**: `getPackStates` gives the pack's `totalBytesToDownload` before anything is fetched, for
  the confirmation (FR-010); `fetch` starts only after it.
- **Wi-Fi only**: checked by the app before `fetch` (`ConnectivityManager` in `core/content`); Play also may ask for
  confirmation on mobile data (`showConfirmationDialog`, handled as the same "Waiting for Wi-Fi" state).
- **Progress and cancel**: `registerListener` for progress; `cancel` stops it, and Play removes partial data;
  `removePack` deletes an installed one (FR-012, FR-014).
- **Where the files are**: `getPackLocation(pack).assetsPath()`; the engine reads them from there.
- **Without Google Play** (sideloaded builds): `getPackStates` fails with `APP_UNAVAILABLE`/`PLAY_STORE_NOT_FOUND`;
  the tool says packs can't be downloaded on this phone (spec edge case).

**Testing before the Play Console account exists**: bundletool's local testing (`build-apks --local-testing`,
then `install-apks`): packs are fetched from the phone's storage instead of Play; network errors, the Wi-Fi wait and
updates can't be tested that way (Google's "Test asset delivery" guide). The emulator journey uses it (R10). Unit and
UI tests use a fake repository (contracts/ports.md).

**Alternatives**: on-demand feature modules (built for code, heavier); our own download server (forbidden by
Principle I).

**iPhone later**: Apple-hosted Background Assets, behind the same `ContentPacks` port.

**Found when built (2026-11-17)**: the asset-delivery library adds `FOREGROUND_SERVICE_DATA_SYNC` to the merged
manifest. From targetSdk 34 it unpacks a downloaded pack in a foreground service of type `dataSync`, which it enables
only while unpacking. The permission is in `androidApp/permissions.allowlist` with that reason. Google Play asks for a
declaration of the foreground-service type at release (spec 011).

## R6. Building the packs

**Decision**: `scripts/fetch-language-packs.sh` downloads the pinned model files from the registry (build time, on
the developer's machine, never in the app), checks each SHA-256, unpacks them, and lays them out in the asset pack
modules `packs/translate_pt/src/main/assets/translate_pt/` and `…/translate_es/…`, with a `pack.json` (contracts/
pack-format.md). The model files are git-ignored (75 MB each pack); `pack.json` and the script are committed. A
release bundle without the files fails its build with a message naming the script. `./gradlew check` doesn't need
them.

**Alternatives**: committing the models (150 MB of binaries in git); Git LFS (not set up, and GitHub's LFS quota).

## R7. The microphone permission and the screen

**Decision**: the microphone is asked for through `PermissionGate` (`core/tool-api`), whose own KDoc describes this
case: "an offer inside a screen that works without it". It appears the first time the person taps a microphone
button, never when the tool opens (FR-020, Principle IX). `RECORD_AUDIO` is already in `AppPermission`,
`AndroidPermissionController` and `permissions.allowlist` (Sound meter, Tuner).

Listening and speaking stop when the tool's task is hidden (`AppVisibility` in the ViewModels, spec 016's two
tasks), and the screen stays on during a conversation (`KeepScreenOn`, as the Intervals do).

## R8. The conversation's turns

**Decision**: a state machine in the feature's domain, test-first: `Idle → Listening(side) → Translating(turn) →
Speaking(turn) → Idle`, with `Paused(reason)` for a call, an alarm or a hidden screen (audio focus loss or
`AppVisibility`). No microphone is open while the phone speaks (FR-025): the recognizer is started only from `Idle`,
and `Speaking` ends on the voice's own "done". A turn keeps its language, recognized text and translation; a corrected
turn is translated again. Copy and Share use one formatter: one line per turn, "Language: text" in the app's
language (FR-027).

With TalkBack on (touch exploration), the app's own voice doesn't read the translation; the translated turn is
announced as a live region instead (spec edge case).

## R9. Which regional language each phone service gets

**Decision**: the app's languages are `en`, `pt` and `es`; speech needs a region. A small domain rule picks it: the
phone's own region when it matches the language (`es-MX` on a Mexican phone), otherwise `en-US`, `pt-BR` and
`es-ES`. The translation doesn't depend on the region. Tested as a table (R9 cases in the tests).

## R10. Tests

**Decision**:

- **Domain, test-first** (Kover ≥ 90%): the route (direct or through English), the 5,000-character limit and the
  counter, the pause before translating, the state machine of R8, the copy text, the region rule of R9, the pack
  states and their messages, the free-space check.
- **ViewModels** with fakes: `FakeTranslationEngine` (echoes with a marker, can be slow or fail), `FakeContentPacks`
  (states driven by the test), `FakeSpeechInput` (scripted partial and final results), `FakeSpeechOutput` (records
  what it said; "done" on demand), bound in `KoinFakesRule`.
- **UI**: `TranslatorScreenTest`, `ConversationScreenTest`, the Offline languages section; screenshots (light, dark,
  200%); `LayoutFitTest` (`TOOL_IDS` plus a Conversation surface); `UiRulesTest`, `ModeStabilityTest`; the
  accessibility audit.
- **Engine on the JVM**: none (the `.so` is Android's); the JNI layer is covered by an instrumented test on the
  emulator (`x86_64`), with a test pack (R6) pushed through bundletool's local testing.
- **End to end** (`tooling/e2e`, emulator only): a text journey through a locally installed pack, in airplane
  mode. Voice can't be tested on the emulator (no on-device recognizer on the ATD image); it is the owner's phone
  pass (quickstart).
- **Network**: the network guard and the architecture tests already fail any network import outside
  `core/monetization/*` and `core/content`; a test asserts the translator's module graph doesn't reach
  `core/content` except through the port.

## Research left for the phone (first tasks)

| # | Question | Default until then |
|---|---|---|
| R1 | On-device recognizer and its languages on the owner's phone | **Answered 2026-11-18**: yes; pt-BR ready, en and es downloadable |
| R2 | Offline voices on the owner's phone | **Answered 2026-11-18**: Samsung's engine, offline voices for all three |
| R3 | Library size, sentence time and memory on the phone | **Answered 2026-11-18**: 0.5 s, 0.8 s through English; base-memory stays |
| R5 | A pack installed through bundletool's local testing on the phone | **Answered 2026-11-19**: `bundletool install-apks` (1.18.3, local testing) installed the debug build on the SM-G780G in 30 s, for user 0 only (the Secure Folder's user 150 stayed without the app) and kept its data; it pushed both packs (219 MB with the language splits). Downloading a pack in the app is part of the owner's pass (T066) |

## SC-006: the release package's growth (T075, measured 2026-11-19)

Both release bundles were built (`:androidApp:bundleRelease`, R8 on), turned into APK sets with bundletool 1.18.3
(`build-apks`, default mode) and measured with `get-size total --dimensions=ABI`: what a phone downloads from Play
for the app, without the on-demand language packs. `main` is `27af9404` (spec 076); the branch also holds specs 077
and 078.

| ABI | `main` (max) | This branch (max) | Growth |
|---|---|---|---|
| armeabi-v7a | 5,643,394 B | 5,837,257 B | +193,863 B (no engine on 32-bit phones) |
| x86 | 5,649,190 B | 5,843,053 B | +193,863 B (no engine) |
| arm64-v8a | 5,645,393 B | 9,060,134 B | +3,414,741 B |
| x86_64 | 5,652,891 B | 9,294,116 B | +3,641,225 B |

The ABIs without `libbergamot.so` show the growth without native code: **0.19 MB, within SC-006's 2 MB**. The rest
on arm64-v8a and x86_64 (about 3.2 and 3.4 MB to download) is the engine's native code, which SC-006 allows; on the
phone it takes 8.2 MB, since native libraries are stored uncompressed. The first release build also found that R8
stopped on a missing annotation class from asset-delivery-ktx (fixed with R8's own `-dontwarn` rule).

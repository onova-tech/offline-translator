# PolyTools: agent context

An offline Android toolbox app (iOS later), built with Kotlin Multiplatform and Compose
Multiplatform. The constitution in `.specify/memory/constitution.md` is binding. Feature order and
prompts live in `docs/roadmap.md`, and each feature's spec, plan and tasks live in `specs/NNN-*/`.

## Toolchain

`. ~/Android/env.sh` sets `JAVA_HOME` (Temurin 21) and `ANDROID_HOME` (`~/Android/Sdk`) on the
development machine. Versions are pinned in `gradle/libs.versions.toml`:

| Tool | Version |
|---|---|
| Gradle | 9.6.0 |
| AGP | 9.4.1 |
| Kotlin | 2.4.21 |
| Compose MP | 1.12.2 |
| compileSdk / targetSdk / minSdk | 37 / 36 / 26 |

## Commands

```bash
./gradlew check                       # all gates: tests, Kover >= 90% domain, detekt, ktlint,
                                      # network guard, Konsist architecture tests, Android lint
./gradlew ktlintFormat                # auto-format Kotlin sources
./gradlew :androidApp:installDebug    # install on a device or emulator
./gradlew recordRoborazziDebug        # re-record screenshot baselines (androidApp/src/test)
./gradlew :<module>:testAndroidHostTest   # unit tests of one KMP module
scripts/ci.sh                         # everything, like a CI server: check, then the emulator's a11y and e2e
scripts/ci.sh check                   # what GitHub Actions runs (no emulator); see docs/testing.md
scripts/ci.sh e2e --test PurchaseJourney   # one end-to-end class on the emulator
```

The end-to-end tests wipe the app's data, so they run only on the script's own emulator. Never point them or
`connectedDebugAndroidTest` at the owner's phone, which is usually on adb too. GitHub Actions has a monthly limit on this
private repository, with a hard stop at 1,500 minutes in `scripts/ci-budget.sh` (`docs/testing.md`).

Ads (spec 002): debug builds always use Google's sample ad IDs. A release build needs
`polyTools.admob.appId` and `polyTools.admob.interstitialId` in `~/.gradle/gradle.properties`
(never in the repository). `polyTools.ump.debugGeography=EEA` forces the consent form in debug
builds. Open owner decisions are listed in `docs/decisions-pending.md`.

"Remove ads" (spec 003): `RemoveAdsStore` is the only way to the store. Debug builds simulate it (sheet marked "Simulated
store", no money); release builds report it as unavailable until the Play Console account exists, which makes the
Play Billing adapter (spec 003 US5) a release blocker. The app shows the store's price text as it is and holds no prices.

On the 2-core development machine, use the Android 36 ATD emulator (AVD `pocket36atd`, headless,
no Google services). The Google APIs image crashes under instrumentation there. Stop Gradle before
booting the emulator, because both together exhaust the RAM.

## Module map

```text
androidApp            composition root (Koin), MainActivity, Navigation 3, shared ToolScreen
core/domain           pure Kotlin: catalog, settings, ports (no Android/Compose imports)
core/data             typed JSON DataStore documents with schemaVersion
core/platform         Android implementations of the domain ports
core/camera           CameraX sessions and preview, and the photo picker with its picture loader
core/designsystem     tokens (color/type/shape/spacing/size/motion), icons, components
core/tool-api         ToolEntry / ToolRegistry contract, host strings
core/monetization/ads Google ads + consent adapters (network allowed)
core/monetization/billing  the release builds' store: Play Billing goes here (spec 003 US5, network allowed); unavailable until then
core/monetization/simulated-store  the debug builds' store, with its sheet; never linked by a release build
core/content          Google Play's on-demand language packs (spec 012, network allowed; only androidApp depends on it)
core/translation      Bergamot, the Translator's engine: prebuilt .so files reached through JNI (NATIVE.md; only androidApp)
feature/catalog       Home
feature/settings      Settings
feature/remove-ads    The purchase's Settings section, Home card and result dialogs (slots wired in AppNavHost)
feature/tools-basic   Ruler, Counter
feature/tools-calculation  Calculator, Unit converter (with interest rates), BMI, Random, Financial calculator, Percent & tip
                      (exact decimals via bignum; .ods/.csv tables written in common code)
feature/tools-time    Stopwatch & Timer with Intervals, World clock, Reminders, Date calculator (monotonic clock, alarms, kotlinx-datetime)
feature/tools-notes   Notepad (Room 3 + KSP; the only module that runs KSP)
feature/tools-motion  Bubble level (with Plumb), Protractor, Compass, G-meter, Vibration, Metal detector, Pedometer
feature/tools-camera  Flashlight, Screen light, Mirror, Magnifier, Night vision, Color picker, QR scanner, Light meter
feature/tools-sound   Sound meter, Metronome, Tone generator, Pitch tuner
feature/tools-gps     GPS info, Speedometer, Drag timer
feature/tools-device  Battery info, NFC tags (read and write), Distance & height, Heart rate
feature/data-transfer Export and import of all data (spec 010): .ptbackup format, journal, the two pages
feature/tools-translate  Translator (spec 012): Type or speak, Conversation, the pack offer, Settings → Offline languages
packs/translate_<code>  53 on-demand asset packs (spec 080), one per language of packs/languages.tsv; their models
                      come from scripts/fetch-language-packs.sh (pt and es by default, --all for every one)
tooling/architecture-tests   Konsist and resource guards
tooling/e2e           End-to-end tests of the installed debug app with UI Automator, emulator only (docs/testing.md)
build-logic           convention plugins (kmp.library, kmp.compose, android.application,
                      quality, network-guard)
```

## Rules that the build enforces

- **No network outside `core/monetization/*` and `core/content`.** Network libraries and imports
  fail the build elsewhere. Only `androidApp` may depend on the ads module; tools, Home and Settings
  reach ads only through `core/domain` ports (`InterstitialAds`, `AdConsent`, `PrivacyChoices`).
- **Reviewed permissions.** Every permission in the merged manifest must be listed with its reason
  in `androidApp/permissions.allowlist`.
- **No raw visual values outside `core/designsystem`.** This covers dp/sp literals, `Color(0x…)`,
  `FontWeight(…)`, shapes and tweens. Use `PocketSpacing`, `PocketSizes`, `PocketShapes`,
  `PocketTypography` and `MaterialTheme`.
- **No hard-coded UI text.** Every string is a Compose resource that exists in `values`,
  `values-pt-rBR` and `values-es`.
- **Pure domain.** Domain packages import no Android, Compose, Koin, data or presentation code.
- **One-way module edges.** Feature modules never depend on each other.

## Shared pieces for tools (spec 004)

- **Platform ports** in `core/domain` (`ports/`, `permissions/`): `MonotonicClock`, `AppVisibility`,
  `AlertPlayer`, `TimerAlarmScheduler`, `RunningNotifications` (with `NoticeCommands` for its
  buttons), `ClipboardWriter`, `RandomSource`, `TimeZoneNames`, `ClockFormat`,
  `PermissionController`. Android implementations live in `core/platform` and are
  bound in `androidApp`'s `AppModule`; tests bind fakes in `KoinFakesRule`.
- **Permissions**: a tool asks only through `PermissionGate` (`core/tool-api`), which runs the
  constitution's flow (explain → request → ask again → open settings). Add the permission to
  `AppPermission`, `AndroidPermissionController` and `permissions.allowlist`.
- **Pictures** (owner's phone pass, 2026-10-31): `rememberPicturePicker` opens the system photo picker
  (no permission), the `PictureLoader` port decodes the choice into memory, and `PictureView` draws
  it (`core/tool-api`, Android side in `core/camera/pictures`). Never store or send a picture.
- **Data export and import** (spec 010): every module that stores data contributes a `DataPart`
  (`core/data/backup`) through `single<DataPart>(named("data-part:<id>"))`; a DataStore document uses
  `JsonDocumentPart` built in its repository. The `.ptbackup` file is AES-256-GCM over the manifest and one
  JSON document per part, keyed by HKDF over a built-in secret (plus PBKDF2 with an optional password);
  any changed byte is refused. Never export ad counters, consent, purchases or sensor baselines.
  The key labels in `ArchiveKeys` still say "PocketTools", the app's name before spec 076: never change them, or
  older files stop opening (the `pocket-tools-1.0.0.ptbackup` test file guards it).
- **Saving a file the user chooses** (spec 014): `DocumentPicker.rememberCreateLauncher(mimeType, …)` opens the
  system save dialog (no permission) and `DocumentFiles.write` fills it; register the launcher outside any dialog
  and only write to a dialog the page itself opened (a result for a recreated page deletes the empty document).
  A .csv file is encoded by `CsvText` (`core/domain/files`), in the language's decimal mark and separator.
  Amounts of money: `MoneyFormat` with the `RegionCurrency` symbol (`tools-calculation`).
- **App-wide overlays**: contribute `single<AppOverlay>(named(...))`; `AppNavHost` draws them above
  every screen (the finished-timer alert uses this). A tool opened from a shortcut draws only those with
  `drawnInToolSessions = true`.
- **Shortcuts** (spec 016): the one purchase also unlocks pinning a tool to the launcher, from a long press on Home or the small icon to the left of the star in each tool. `ToolShortcutActivity` draws the tool
  alone through `ToolSession`, one task per tool, and never starts ads or consent: `isAppActivity` stays `MainActivity`. In release
  builds the store is unavailable until the Play Console account exists, so shortcuts can only be tried in debug builds with the
  simulated store. Debug builds export the activity for `ToolShortcutJourney`; the main manifest must not (`ToolShortcutActivityTest`).
- **Two tasks, one process** (spec 016): `AppVisibility` in a ViewModel follows **that screen's task**, so sound, light, the microphone and
  GPS stop when it's hidden, with nothing for a tool to do. A process-wide singleton must take `ProcessVisibility` instead, or
  it follows whichever task was resumed when it was built.
- **Sharing an image** (spec 019): `ImageSharer` writes to `cache/shared/` behind a `FileProvider`, for the app the person picks. No permission. A raw black-on-white drawing like a QR code lives in the design system (`QrCodeImage`).
- **Cues** (spec 022): a short sound and vibration for a moment, unlike an `AlertPlayer` alert, which rings until stopped. `CuePlayer` plays one now and `CueAlarmScheduler` plays them later with no notification and no foreground service. A document added to a `CompositeDataPart` later goes in `addedLater`, or older data files stop importing.
- **Dates and times**: show them with `rememberDisplayFormats()` (`core/tool-api`): the date order
  and 12/24-hour choice from Settings → Units & formats, resolved with the language and the phone.
- **Programmer mode** (spec 017): `CalculatorMode.PROGRAMMER` has its own domain (`Programmer*.kt`, `BitToken`), because a machine word wraps and a
  decimal must not. Never route it through the decimal `Expression` or the bignum library.
- **Numbers**: format with `DecimalFormatter` and read input with `DecimalInput` (`core/domain/format`),
  using `rememberNumberSymbols()` for the app language. For exact decimals use
  `toolscalculation.format.Decimals`: the bignum library's own rounding loses a power of ten when it
  carries (0.999… → 0.1), so never call `roundSignificand` or `divide` with a rounding mode.
- **Compose resources**: write apostrophes plainly (`can't`); `\'` is kept literally.
- **The Translator** (spec 012): the feature reaches the packs, the engine and the phone's speech only through ports
  (`ContentPacks`, `TranslationEngine`, `SpeechInput`, `SpeechOutput`, `ScreenReader`); `TranslatorModulesTest` keeps it
  so. `EngineSession` opens the engine with the installed packs while a Translator screen uses it. Speech is on-device
  only, never a recognizer that may use the network. The engine's .so files are rebuilt by
  `scripts/build-bergamot-android.sh` (never by Gradle), then `scripts/collect-engine-licenses.py` refreshes their license
  entries in `config/libraries/` for Settings → About → Open-source licenses (AboutLibraries, offline).
- **The Translator's languages** (spec 080): `scripts/update-language-catalog.py` reads Mozilla's model registry once and
  writes `packs/languages.tsv`, `LanguageCatalog.kt` (`core/domain/translation`) and each pack's `pack.json`; never edit
  them by hand, and `--check` says whether they match. Gradle makes a pack module per language from the TSV; a debug
  build bundles only the packs that hold models, so bundletool's local testing pushes only those to a device. The
  person checks which languages the lists show (at least two, `CheckedLanguages`); their names come from the phone
  (`LanguageNames`), in the app's language.

## Screen rules (binding): `docs/ui-rules.md`

Every screen follows `docs/ui-rules.md`: each of the owner's UI decisions with its date, the shared component that
keeps it and the test that guards it. **Read the sections that apply before building or changing a screen**, use the
component a rule names (never rebuild it in a feature module), and add a new decision there in the same pull request.
The essentials:

- **Regions:** `ToolLayout`/`AdaptiveToolLayout`. Top: the tool's modes, categories and search only, the choices as a
  `SegmentedChoice`, which scrolls sideways when its labels don't fit (S1). Middle: the content, the only part that
  scrolls. Bottom, at the bottom edge: everything else. The kind of thing you type (QR code, NFC message) is a
  `KindForm` at the bottom, above its fields, and never moves, even in a small window (L1–L5).
- **Bottom order:** inputs, then "More options" (`MoreControls`, only for two or more options), then the actions;
  nothing below the actions. One main (filled) `BottomAction` per screen, others outlined; side by side in a
  `BottomActionRow` (`compact = true`), the main action last, on the right (A1–A4).
- **Toggles beside fields:** `FieldToggleColumn` + `FieldWithToggle`: the toggle on the field's right, as tall as its
  outline, one width for every word of every mode. A `ToggleChoice` keeps its width whatever it shows (T1–T6).
- **Nothing jumps:** what stays between modes keeps its place and size (`ReservedSlot`, `ReservedLine`, `widestText`);
  no check marks on chips or segments (M1–M5).
- **Rows:** a list's or a history's row keeps its actions behind ⋮ on its right (`RowMenu`; a history row is a
  `MenuRow`, like the Notepad's); Copy and Share send the formatted text the screen shows (A6).
- **Readings:** one `MainReading` (`NO_READING` with a caption); statistics as `ValueRow`s; one `ShortNote`, the rest
  in Help (R1–R5).
- **Fit:** `FitColumn` with `flex` instead of scrolling; reduced windows shrink to 36 dp targets but keep everything
  reachable; never override `LocalDensity` (L7–L10).
- **Text:** `PocketTextField`, `WholeWordText`, `ToolHelp` blocks of at most 180 characters, strings in three languages
  (T7, X1–X4). A snackbar is always a `PocketSnackbarHost`'s, in the theme's colors (V3).

The build checks them: `UiRulesTest` (toggle placement, action order, nothing below the actions), `ModeStabilityTest`
(modes and toggles never move anything), `LayoutFitTest` (22 window and text-size combinations; add a new tool to
`TOOL_IDS`), `DesignSystemOnlyTest` and `HelpTextsTest`. Exceptions are listed in the rulebook, with their reasons.

## Adding a tool

The full procedure (spec-kit steps, file-by-file recipe, tests, verification, phone install,
gotchas) is in `docs/how-to-build-a-tool.md`. Specs 001–010 and 012–017 are built (003 and 016 with a simulated store until
the Play Console account exists; 012's pack downloads need it too); 011 remains (`docs/roadmap.md`, `docs/next-specs-handoff.md`). The full
review of 2026-11-01 is in
`docs/review-2026-11-01*.md`, with the fix plan and the owner's answers. In short:

1. Implement `ToolEntry` in a `feature/tools-*` module, with a permanent `ToolId`, its category
   and required hardware.
2. Register it with `single<ToolEntry>(named("<id>")) { … }` in that module's Koin module.
3. Provide `Content()` and `HelpContent()` only. The host draws the back, title, favorite and help
   controls.
4. Write domain tests first, then UI and screenshot tests. Add strings in all three languages.

## Conventions

- Conventional Commits.
- Work on feature branches; never push to `main`.
- Tests: kotlin.test plus coroutines-test. In ViewModel tests, collect `stateIn` flows with
  `backgroundScope.launch(UnconfinedTestDispatcher(testScheduler))`.

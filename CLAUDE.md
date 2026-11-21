# Offline Translator: agent context

A free, ad-free, open-source (GPL-3.0) offline translator for Android (iOS later), built with Kotlin Multiplatform and
Compose Multiplatform. It began on 2026-11-20 as the Translator tool of PolyTools (specs 012 and 080, kept in `specs/`
with their numbers), and became its own app in spec 081. The constitution in `.specify/memory/constitution.md` is
binding; each feature's spec, plan and tasks live in `specs/NNN-*/`.

## Toolchain

`. ~/Android/env.sh` sets `JAVA_HOME` (Temurin 21) and `ANDROID_HOME` (`~/Android/Sdk`) on the development machine.
Versions are pinned in `gradle/libs.versions.toml` (Gradle 9.6.0, AGP 9.4.1, Kotlin 2.4.21, Compose MP 1.12.2;
compileSdk / targetSdk / minSdk 37 / 36 / 26).

## Commands

```bash
./gradlew check                       # all gates: tests, Kover, detekt, ktlint, network guard, Konsist, Android lint
./gradlew ktlintFormat                # auto-format Kotlin sources
scripts/fetch-language-packs.sh       # the models of Portuguese and Spanish into packs/ (--all for every language)
./gradlew :androidApp:installDebug    # install (packs come through bundletool's local testing, see docs/testing.md)
./gradlew recordRoborazziDebug        # re-record screenshot baselines (compare old and new before committing)
scripts/ci.sh                         # everything: check, then the emulator's a11y, e2e and engine stages
scripts/ci.sh check                   # what GitHub Actions runs (no emulator)
```

The end-to-end tests wipe the app's data, so they run only on the script's own emulator (AVD `pocket36atd`, Android 36
ATD, headless). Never point them at a real phone.

## Module map

```text
androidApp            composition root (Koin), MainActivity, Navigation 3: the Translator, Settings, licenses
core/domain           pure Kotlin: settings, ports, the language catalog (no Android/Compose imports)
core/data             typed JSON DataStore documents with schemaVersion
core/platform         Android implementations of the ports: speech, voices, language names, permissions, clipboard
core/designsystem     tokens (color/type/shape/spacing/size/motion), icons, components
core/tool-api         ToolEntry / ToolRegistry contract, help sheet, permission gate, host strings
core/content          Google Play's on-demand language packs (the only network; only androidApp depends on it)
core/translation      Bergamot, the translation engine: prebuilt .so files reached through JNI (NATIVE.md)
feature/settings      Settings: Appearance, Language, Offline languages (slot), About, Open-source licenses
feature/tools-translate  the Translator: Type or speak, Conversation, Manage languages, the pack offer
packs/translate_<code>  53 on-demand asset packs, one per language of packs/languages.tsv
tooling/architecture-tests   Konsist and resource guards
tooling/e2e           end-to-end tests of the installed debug app with UI Automator, emulator only
build-logic           convention plugins (kmp.library, kmp.compose, android.application, quality, network-guard)
```

## Rules that the build enforces

- **No network outside `core/content`.** Network libraries and imports fail the build elsewhere.
- **No ads, purchases or accounts** (constitution Principle VI).
- **Reviewed permissions.** Every permission in the merged manifest is listed with its reason in
  `androidApp/permissions.allowlist`: the microphone, and Play's dataSync service for unpacking a pack.
- **No raw visual values outside `core/designsystem`**, **no hard-coded UI text** (every string in `values`,
  `values-pt-rBR` and `values-es`), **pure domain**, **one-way module edges**.

## The Translator

- The feature reaches the packs, the engine and the phone's speech only through ports (`ContentPacks`,
  `TranslationEngine`, `SpeechInput`, `SpeechOutput`, `ScreenReader`); `TranslatorModulesTest` keeps it so. Speech is
  on-device only, never a recognizer that may use the network.
- `scripts/update-language-catalog.py` reads Mozilla's model registry once and writes `packs/languages.tsv`,
  `LanguageCatalog.kt` and each pack's `pack.json`; never edit them by hand. A debug build bundles only the packs whose
  models were fetched; a release build bundles all of them and fails if one is empty.
- The engine's .so files are rebuilt by `scripts/build-bergamot-android.sh` (never by Gradle), then
  `scripts/collect-engine-licenses.py` refreshes their entries in `config/libraries/`.

## Screen rules (binding): `docs/ui-rules.md`

Every screen follows `docs/ui-rules.md`, the owner's UI decisions from PolyTools (some examples there name PolyTools'
tools). Read the sections that apply before building or changing a screen, use the component a rule names, and add a
new decision there in the same change. `UiRulesTest`, `ModeStabilityTest` and `LayoutFitTest` check them.

## Conventions

- Conventional Commits; feature branches; never push to `main`.
- Commits in this public repository use the GitHub no-reply address, never a personal email.
- Tests: kotlin.test plus coroutines-test; in ViewModel tests, collect `stateIn` flows with
  `backgroundScope.launch(UnconfinedTestDispatcher(testScheduler))`.
- Compose resources: write apostrophes plainly (`can't`); `\'` is kept literally.

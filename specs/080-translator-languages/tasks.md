---
description: "Tasks for spec 080, more languages in the Translator, chosen by the person"
---

# Tasks: More languages in the Translator, chosen by the person (spec 080)

**Input**: [spec.md](spec.md), [plan.md](plan.md), [research.md](research.md), [data-model.md](data-model.md),
[contracts/](contracts/), [quickstart.md](quickstart.md); spec 012's design (`specs/012-offline-translator/`).

**Tests**: required. The constitution asks for test-first domain logic (Principle V) and automated accessibility
checks (Principle X). Within each phase, the test tasks come first and must fail before the code they cover.

**Owner's decisions**: "All 54" languages (2026-11-19); `no` dropped as a duplicate of `nb`, so 53 besides English
("Ok with the Norwegian choice", 2026-11-19). Every page uses the app's components and `docs/ui-rules.md`.

## Path prefixes used below

| Prefix | Path |
|---|---|
| `DOM/` | `core/domain/src/commonMain/kotlin/tech/onova/offlinetranslator/core/domain/` |
| `DOMT/` | `core/domain/src/commonTest/kotlin/tech/onova/offlinetranslator/core/domain/` |
| `FT/` | `feature/tools-translate/src/commonMain/kotlin/tech/onova/offlinetranslator/feature/toolstranslate/` |
| `FTT/` | `feature/tools-translate/src/commonTest/kotlin/tech/onova/offlinetranslator/feature/toolstranslate/` |
| `PLAT/` | `core/platform/src/androidMain/kotlin/tech/onova/offlinetranslator/core/platform/` |
| `DS/` | `core/designsystem/src/commonMain/kotlin/tech/onova/offlinetranslator/core/designsystem/` |
| `APP/` | `androidApp/src/main/kotlin/tech/onova/offlinetranslator/` |
| `APPT/` | `androidApp/src/test/kotlin/tech/onova/offlinetranslator/` |

## Format: `- [ ] [ID] [P?] [Story?] Description`

**[P]**: can run in parallel (different files, no dependency on an unfinished task). **[Story]**: US1–US3 from
spec.md. **[phone]**: needs the owner's phone and their consent at that moment (its
storage is nearly full: push only the packs named). On this 2-core machine "parallel" means written together; Gradle
runs one build at a time.

---

## Phase 1: Setup (the catalog, the packs, the scripts)

**Purpose**: one generated catalog drives the packs, Gradle, the fetch script and the app (research R1–R3).

- [X] T001 Write `scripts/update-language-catalog.py` (contracts/catalog.md): read a registry `models.json` (a path, or
  the registry URL), keep every language with a direction to and from English whose `releaseStatus` is "Release",
  "Release Android" or "Release Desktop", take `base-memory` over `base` over `tiny`, drop `no` (owner, 2026-11-19),
  mark a language `basic` when either direction is `tiny`; download each direction's shortlist and vocabularies once
  (cache under `build/language-catalog/`) to hash them; write `packs/languages.tsv` and generate
  `DOM/translation/LanguageCatalog.kt` (`OfferedLanguage(code, pack, quality, tag, speechRegion)`, English first, the
  tags `zh-Hans`/`zh-Hant`/`nb`, default speech regions of research R5); `--check` compares both files with a fresh run
- [X] T002 Run T001's script in the background (about 2.7 GB of downloads, once); commit `packs/languages.tsv` and
  `DOM/translation/LanguageCatalog.kt`; check that it lists 54 entries (English + 53) and that Portuguese's and
  Spanish's hashes equal those in today's `scripts/fetch-language-packs.sh` (2026-11-19: 53 languages besides English, 31 full and 22 basic; Portuguese's and Spanish's model, shortlist and vocabulary hashes equal the old pins; the shortlists and vocabularies came to 514 MB)
- [X] T003 Add `packs/pack.gradle.kts` (the `com.android.asset-pack` plugin, `packName` from the project's name,
  on-demand delivery) and make `settings.gradle.kts` include `:packs:translate_<code>` for every code in
  `packs/languages.tsv`, each with `packs/translate_<code>/` as its folder and the shared script as its build file;
  remove `packs/translate_pt/build.gradle.kts` and `packs/translate_es/build.gradle.kts`; extend `packs/.gitignore` to
  every pack's model files (as built: the catalog script also writes every pack's committed `pack.json`, since Gradle 9 needs each module's folder; Portuguese's and Spanish's came out byte-identical)
- [X] T004 In `androidApp/build.gradle.kts`, take `assetPacks` from `packs/languages.tsv`: in debug builds only the packs
  whose models are present (so local testing pushes only those, spec FR-015), in release all of them; extend the
  `bundleRelease` precondition (spec 012 T007) to all 53 packs
- [X] T005 Make `scripts/fetch-language-packs.sh` read `packs/languages.tsv` instead of its four inline lines, with
  `--languages pt,es` (the default) and `--all`; write `pack.json` with `sourceVocab`/`targetVocab` for a direction with
  two vocabularies (contracts/catalog.md); every file still checked against its SHA-256
- [X] T006 Verify research R3 on the emulator: a debug bundle with only the pt and es packs, installed through local
  testing, then `PlayContentPacks.refresh()` over the whole catalog: record in `research.md` (R3) whether Play answers
  every pack or fails the request; T014 depends on the answer (2026-11-19: Play refuses the whole request, so every pack read as unavailable; research R3)

**Checkpoint**: `./gradlew :androidApp:bundleDebug` builds with two packs fetched; `scripts/ci.sh e2e --test
TranslatorJourney` still passes.

---

## Phase 2: Foundational (blocking prerequisites)

**Purpose**: the catalog's rules, the names, the setting and the engine's two vocabularies, test-first.

- [X] T007 [P] Write `DOMT/translation/LanguageCatalogTest.kt`: 54 entries, English first and without a pack, codes and
  packs unique, `nb` present and `no` absent, `zh` → tag `zh-Hans` and region `zh-CN`, `zh_hant` → `zh-Hant`/`zh-TW`,
  `tr` basic, `ja` full, `LanguageCatalog.of("xx")` null
- [X] T008 [P] Add the port `DOM/ports/LanguageNames.kt` (`nameOf(tag, appLanguage)`, `ownName(tag)`; data-model.md)
  with a failing `core/platform/src/androidHostTest/…/AndroidLanguageNamesTest.kt` ("ja" in pt → "Japonês", in es →
  "Japonés"; `ownName("ja")` → "日本語"; `ownName("fr")` → "Français"; "zh-Hant" in en → "Chinese (Traditional)"),
  then `PLAT/AndroidLanguageNames.kt` (`Locale.forLanguageTag`, first letter capitalised in the target language); bind
  it in `APP/di/AppModule.kt`; fakes in `FTT/fakes/FakeLanguageNames.kt` and `APPT/support/KoinFakesRule.kt` (the app's screen tests use the real `AndroidLanguageNames` through `appModule`; the feature's tests use `FakeLanguageNames`)
- [X] T009 [P] Write failing tests `FTT/domain/CheckedLanguagesTest.kt` (data-model.md): `resolve` ("null → en, pt,
  es"; "unknown codes dropped, duplicates dropped, fewer than two → the default"), `toggle` (adds; removes "unless
  only two are checked (then unchanged)"), `locked` (both of two), `replacementFor` ("the first checked language by
  name that isn't `other`", with the order passed in)
- [X] T010 Implement `FT/domain/CheckedLanguages.kt` until T009 passes
- [X] T011 Add `languages: List<LanguageCode>?` to `FT/domain/TranslatorSettings.kt` and its document in
  `FT/data/DataStoreTranslatorSettings.kt` ("`schemaVersion` stays 1 (an optional field)"); tests in
  `FTT/data/TranslatorSettingsTest.kt`: round trip, a file without the field reads as the default, the `translator`
  data part exports it
- [X] T012 [P] Teach `core/translation/src/commonMain/…/PackDescription.kt` `sourceVocab`/`targetVocab` (either those or
  `vocab`; neither → damaged) with tests in `core/translation/src/commonTest/…/PackDescriptionTest.kt`, and make
  `core/translation/src/androidMain/…/BergamotEngine.kt` pass source and target vocabularies apart
- [X] T013 [P] Make `FT/domain/SpeechRegions.kt` take a language's default region from `LanguageCatalog` (the phone's
  matching region still wins); extend `FTT/domain/SpeechRegionsTest.kt` (fr → fr-FR, zh_hant → zh-TW, `es` with phone
  `es-MX` → es-MX) (Chinese in either script always takes the catalog's region: a phone's "zh" says nothing about the script)
- [X] T014 Give `PlayContentPacks` the catalog's 53 packs from `APP/di/AppModule.kt`; if T006 found that Play fails a
  request naming a pack the bundle lacks, ask pack by pack and read such a pack as unavailable on this install; tests
  in `core/content/src/androidHostTest/…/PlayPackMappingTest.kt` (the build writes `BuildConfig.BUNDLED_PACKS`; `PlayContentPacks` asks only about those and reads the others as unavailable from the start, `PlayPackMapping.initial`/`asked`; the journey reruns in T036)

**Checkpoint**: `./gradlew :feature:tools-translate:testAndroidHostTest :core:domain:testAndroidHostTest
:core:translation:testAndroidHostTest :core:platform:testAndroidHostTest` green.

---

## Phase 3: User Story 1 – Choose which languages appear (Priority: P1) 🎯 MVP

**Goal**: "Other languages…" at the end of every list opens a page of every language with check boxes; at least two
stay checked; the lists show exactly the checked ones.

**Independent Test**: spec story 1's (check Japanese and French, uncheck Spanish; both modes; after a restart).

- [X] T015 [P] [US1] Add `DS/components/CheckRow.kt` (name, supporting text, a check box at the end, the whole row one
  target of at least `PocketSizes.touchTarget`, `toggleable` semantics, disabled look and state) with
  `APPT/components/CheckRowTest.kt` (`CheckRow` beside `SwitchRow` in `SettingsRows.kt`; `APPT/components/CheckRowAndMenuEntryTest.kt`)
- [X] T016 [P] [US1] Give `DS/components/MenuChoice.kt` an optional last entry (a divider, then a label and its
  action), in the upright list and in the turned one; test in `APPT/components/MenuChoiceTest.kt` (`MenuEntry`; same test)
- [X] T017 [P] [US1] Write failing tests `FTT/presentation/LanguagesViewModelTest.kt`: the page's rows in name order
  (fake names), the checked state, search by either name, a toggle saved, two left locked with the note, and the
  Translator's and Conversation's chosen language replaced when it's unchecked (FR-008); and in
  `FTT/presentation/TranslatorViewModelTest.kt`: `languagesOf` shows only checked languages, installed first
- [X] T018 [US1] Implement `FT/presentation/LanguagesViewModel.kt` and the checked set in `languagesOf`
  (`FT/presentation/TranslatorViewModel.kt`, `ConversationViewModel.kt`), with the replacement of FR-008, until T017
  passes; register it in `FT/di/ToolsTranslateModule.kt` (the defaults resolve over the checked languages; the page sorts names without accents, so "Árabe" sits with the A's)
- [X] T019 [US1] Implement `FT/presentation/LanguagesPage.kt` per contracts/ui.md (full-screen dialog; back and title;
  `SearchField`; the note in a `ReservedLine`; `CheckRow`s, the only scrolling part; "No language matches" with
  `NO_READING`), and the "Other languages…" entry on every list (`TypeOrSpeakContent.kt` `LanguageRow`,
  `ConversationContent.kt` `Half`); strings in three languages
- [X] T020 [US1] Name languages beyond English, Portuguese and Spanish with `LanguageNames` in `languageName`
  (`FT/presentation/TranslatorContent.kt`), and in the list's detail
- [X] T021 [US1] Write `APPT/screens/LanguagesPageTest.kt`: the entry ends both lists in both modes (the far half's too);
  the page lists 54; "jap" finds Japanese; two left are disabled with the note; back shows the checked languages in
  the lists; the choice survives recreating the screen
- [X] T022 [US1] Add the decision of 2026-11-19 to `docs/ui-rules.md` (the language page, `CheckRow`, `MenuChoice`'s last
  entry) with the components and tests that guard it (rule S6)

**Checkpoint**: US1 works with the fakes; on the emulator with two packs, the lists and the page work end to end.

---

## Phase 4: User Story 2 – Translate in the new languages (Priority: P2)

**Goal**: a checked language downloads as today and translates offline, marked when basic, right to left when RTL.

**Independent Test**: spec story 2's (Portuguese ↔ Japanese in airplane mode, through English).

- [X] T023 [P] [US2] Write failing tests in `FTT/presentation/TranslatorViewModelTest.kt`: a translation from or into a
  basic-quality language carries the note `BASIC_QUALITY` beside the others
- [X] T024 [US2] Add `TranslationNote.BASIC_QUALITY` in `FT/presentation/TranslatorViewModel.kt` and its text in
  `TypeOrSpeakContent.kt` `notesText`, strings in three languages, until T023 passes
- [X] T025 [US2] Right-to-left text (research R6): `TextDirection.Content` with `TextAlign.Start` for the text field,
  the translation (`TypeOrSpeakContent.kt`) and the turns (`ConversationContent.kt`); test in
  `APPT/screens/TranslatorScreenTest.kt` that typed Arabic lays out right to left (the test failed first: Material leaves the direction unspecified; `PocketTextField` takes `TextDirection.Content` for every tool, the translation and turns `inItsOwnDirection()`; research R6)
- [X] T026 [US2] Make `androidApp/src/androidTest/kotlin/tech/onova/offlinetranslator/BergamotEngineTest.kt` translate a sentence
  both ways with English for every pack it finds pushed, and add `scripts/ci.sh engine --all` (fetch all; push five
  packs, run, remove them, next five; a summary per language) with `docs/testing.md` (`everyGivenLanguageTranslatesBothWaysWithEnglish`, its packs from the `packs` argument; the packs come from `build/language-packs` through the fetch script's new `--into`, so a debug bundle keeps only pt and es; each language's sentences in `build/ci/engine-languages.txt`)
- [X] T027 [US2] Run `scripts/fetch-language-packs.sh --all` and `scripts/ci.sh engine --all` in the background; record
  each language's result in `research.md` (R9, spec SC-002); a language that fails to load or translate is fixed or,
  with the owner's word, left out of the catalog (2026-11-19: all 53 passed in 11 batches; research R9 lists the quality quirks for the owner)

**Checkpoint**: every language translates on the emulator; the Translator shows the basic-quality note.

---

## Phase 5: User Story 3 – See and free the space of every language (Priority: P3)

**Goal**: Settings → Offline languages lists the checked languages and every installed pack.

**Independent Test**: spec story 3's (uncheck Japanese with its pack installed; it's still listed; Delete frees it).

- [X] T028 [P] [US3] Write failing tests in `APPT/screens/OfflineLanguagesSectionTest.kt`: the checked languages first,
  then installed packs of unchecked languages; deleting such a pack removes its row (a pack of an unchecked language stays listed while installed; deleting it removes its row)
- [X] T029 [US3] Implement the list in `FT/presentation/OfflineLanguagesSection.kt` until T028 passes (`PacksUi.shownPacks`)

---

## Phase 6: Polish and cross-cutting concerns

- [X] T030 [P] Help: one block on choosing languages (at most 180 characters, three languages) in the Translator's help
  strings; `HelpTextsTest` passes
- [X] T031 [P] Add the language page to `APPT/screens/LayoutFitTest.kt` (a surface) and to
  `APPT/screens/TranslatorAccessibilityAuditTest.kt` (light, dark, 200%) (the page's case waits for the From chip and runs before the Conversation's, whose mode it would inherit; the audit also runs in landscape for SC-005)
- [X] T032 [P] Screenshots in `APPT/screens/TranslatorScreenshotTest.kt`: a list with "Other languages…", the page with a
  search and with two locked; record and look at each one before committing (the list with its last entry, the page with a search, and English locked under the note at 200% dark; each search starts at the top, found while looking at them; `translator_conversation_font200` matches spec 012's re-recorded baseline, 8e9d1de6)
- [X] T033 [P] Licenses: `config/libraries/` entry for the models names all languages (MPL 2.0, Mozilla's registry);
  `LicensesScreenTest` passes (`translation-models.json` names the 53 languages)
- [X] T034 Docs: `CLAUDE.md` (the catalog and its scripts), `docs/how-to-build-a-tool.md` §2.8 (pack modules from the
  catalog), `docs/testing.md` (`engine --all`, two packs by default), `docs/decisions-pending.md` (spec 080: all 54,
  `no` dropped, approved 2026-11-19)
- [ ] T035 Measure spec SC-004 as spec 012 T075 did (the release bundle against `012-translator-foundation`, without the
  packs); record it in `research.md` (R10) (2026-11-19/20: not measured yet. Five release builds in a row died in the Gradle daemon's GC during R8, and the spec 012 branch's, which built that morning, died at lint too: the machine, not the code; rerun `measure` when release builds get through)
- [ ] T036 `scripts/ci.sh check` and `scripts/ci.sh device` green; then [phone] the owner's pass of quickstart §2 with
  consent, pushing only pt, es and two new languages
- [ ] T037 Run `/speckit-converge` until Converged; then the pull request (no merge), stacked on #91

---

## Dependencies

```text
Phase 1 Setup (T001→T002→T003/T004/T005→T006) ─► Phase 2 Foundational (T007–T014; T014 needs T006)
                                                        │
                                   ┌────────────────────┼─────────────────────┐
                                   ▼                    ▼                     ▼
                              US1 (P1)             US2 (P2: T026–T027      US3 (P3)
                                   │                need T005, T012)          │
                                   └──────────────► Phase 6 Polish ◄──────────┘
```

- US1 needs the catalog (T002), the names (T008), the rules (T010) and the setting (T011).
- US2's engine run (T027) needs all packs fetched (T005) and the two vocabularies (T012); it can run while US1 is built.
- US3 needs the checked set (T011) and the catalog's packs (T014).

## Parallel opportunities

- Phase 2: T007, T008, T009, T012, T013 together (different files).
- US1: T015, T016, T017 together.
- US2: T026 while T023–T025 are written; T027 is a long background run.
- Phase 6: T030–T033 together.

## Implementation strategy

1. **MVP**: Phases 1–2 and US1: the page and the lists, with two packs; the new languages show their size and download
   as today.
2. **Every language proven**: US2 (basic-quality note, RTL, the engine over all 53).
3. **Space**: US3. Then Phase 6 and the owner's phone pass (only a few packs on the phone).

---

## Phase 7: The owner's phone pass (2026-11-20)

The owner tried Japanese, Arabic and Chinese on the phone (pt, es, ja, ar, zh pushed). Findings and fixes, test-first:

- [X] T038 A fetched pack joins the next debug bundle: `androidApp/build.gradle.kts` finds the fetched packs through a
  value source (`FetchedPacks`), which the configuration cache checks on every build; a plain walk was cached and left
  Simplified Chinese out (checked both ways: fetched, then removed)
- [X] T039 Speech recognition the phone downloads and never installs (the owner's Samsung: Android System Intelligence
  last updated 2024-05-15 downloaded Japanese `v3056` and ignored it, "Unrecognized recognizer for [ja-JP, 3056]",
  so the notice offered the same download forever): `RecognitionSupport.NOT_INSTALLED`, read by `SpeechDownloads`
  from a request that never lands (`asked`, `ASKED_CHECKS`, `forget` on return); "The phone didn't install …" with
  "Update" (`SpeechInput.canUpdateService`/`openServiceUpdate`: the store page of `com.google.android.as`) in both
  modes; tests in `SpeechDownloadsTest`, `SetupNeedsTest`, `ConversationViewModelTest`, `SpeakAndHearTest` (2026-11-20: tests pass)
- [X] T040 "Get a voice" opens the voice engine's own download list (`TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA` for
  the default engine; Samsung's `DownloadList` and Google's both handle it), the TTS settings otherwise; new texts in
  three languages (rule C3)
- [X] T041 The Conversation's speech notices close with a small × (`Notice`'s `onClose`), until the tool reopens or
  the half's language is chosen again; the pack offer has none (owner: "Arabic didn't have a STT in samsung … add an
  small x button"; rule C2); tests in `SetupNeedsTest`, `ConversationViewModelTest`, `ConversationScreenTest` (the × folded into `fix(need, close = true)`, as `tap(side, discard)` was)
- [ ] T042 Install on the owner's phone with pt, es, ja, ar and zh; the owner retries Japanese recognition after
  updating Android System Intelligence

---

## Phase 8: Manage languages (owner's phone pass, 2026-11-20; spec story 4, FR-016, FR-017)

Owner's answers: one screen with the checklist and, per language, its pack, speech and voice; the popups lead to it
with a small ×; Settings opens it. Test-first; contracts/ui.md "Manage languages".

- [X] T043 One `SpeechDownloads` for the process (Koin `single`), keyed by speech tag in every screen, its windows in
  time (`MonotonicClock`: 15 s finishing, 30 s after a request) rather than in checks, so the page and both modes
  agree; `FTT/domain/SpeechDownloadsTest.kt`
- [X] T044 `FT/presentation/SpeechPartsViewModel.kt`: each given language's speech and voice, asked of the phone, again
  while one is on its way and when the screen comes back; Get this language, Update, Get a voice;
  `FTT/presentation/SpeechPartsViewModelTest.kt`
- [X] T045 `FT/presentation/ManageLanguagesPage.kt` replaces `LanguagesPage.kt`: "Your languages" cards (check +
  Translation, Speech, Voice rows with ⋮), "Other languages", search over both, the note, opening at a language;
  Delete asks first; `APPT/screens/ManageLanguagesPageTest.kt` (from `LanguagesPageTest`)
- [X] T046 "Manage languages…" ends every list; the Conversation's popups and Type or speak's speech messages lead to
  the page at their language; the ×, smaller, never over a button; both modes ask again when the page closes; their
  direct fixes go (`ConversationViewModel`, `SpeakAndHearViewModel` and their tests); `ConversationScreenTest`,
  `TranslatorScreenTest`
- [X] T047 Settings → Offline languages: the Wi-Fi switch and a "Manage languages" row with the space used, opening
  the page; `OfflineLanguagesSectionTest`
- [ ] T048 Strings in three languages, the help block, `docs/ui-rules.md` (S6, C2, C3), screenshots (looked at), the
  fit test's opener label, the audit; `scripts/ci.sh check`; install on the owner's phone (dex checked)
 (2026-11-20: check green, 40 min; `TranslatorJourney` deletes the pack through Manage languages, green; screenshots looked at, two new; the phone install waits for wireless debugging)
---

## Phase 9: Manage languages, second pass (owner's phone pass, 2026-11-20 evening)

Owner: the popup's text and × still squeezed its button; "Idiomas baixados" instead of "Seus idiomas"; the check as a
row "Habilitar idioma", checked when the pack is downloaded; a language doesn't change section until the page is opened
again; each language collapses and expands under a ⌄, with a divider, and shows a status icon (installed and enabled,
installed but disabled, partly installed, enabled or not); a help on the page.

- [X] T049 The Conversation's notice: its × in the buttons' row, so the text has the whole width; a notice taller than
  the half's room scrolls inside its card instead of squeezing its button (`Notice`, `ConversationContent`)
- [X] T050 Sections: "Downloaded languages" (pack on the phone, and English, built in) and "Other languages", frozen while
  the page is open; speech and voice asked for the downloaded and the expanded ones
- [X] T051 `ExpandableCard` (design system): a header (name, own name, status, ⌄ turning to ⌃), a divider, and its
  content folding with `FoldAway`'s animation; each language's content: "Enable language" (`CheckRow`), Translation,
  Speech recognition, Voice; opening at a language expands it
- [X] T052 Status icons (`PocketIcons.Complete`, `PocketIcons.Partial`) with a warning color token: complete and enabled
  (primary), complete and disabled (gray), partial and enabled (warning), partial and disabled (gray); partial is a
  part the phone can get and hasn't (a part it can't have doesn't count)
- [X] T053 Downloading a language's pack on the page enables it
- [X] T054 A help on the page (ⓘ in its top bar), the host's help sheet shared from `core/tool-api`; blocks of at most
  180 characters in three languages
- [ ] T055 Tests (view model, page, notice), screenshots looked at, rules S6 and C2, `scripts/ci.sh check`, install on
  the owner's phone (dex checked)
- [X] T056 Owner, 2026-11-20 (night): the notice's × becomes a gray "Ignore" text button before "Manage languages"
  (`Notice`'s `onClose`); the page's help shows the four marks themselves beside their meanings
  (`ManageLanguagesHelp`, `StatusMark` without a label where the words are beside it)

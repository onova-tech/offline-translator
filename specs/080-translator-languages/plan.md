# Implementation Plan: More languages in the Translator, chosen by the person

**Branch**: `080-translator-languages` | **Date**: 2026-11-19 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/080-translator-languages/spec.md`; builds on spec 012
(`specs/012-offline-translator/plan.md`), stacked on its branch.

## Summary

Every Translator language list ends with **"Other languages…"**, which opens a full-screen page of English and 53
more languages, each with a check box; the checked ones (at least two) are what the lists show. The languages are
every one Mozilla's model registry has released both ways with English (the owner's "All 54", less the duplicate
`no`/`nb`: research R1), each its own on-demand pack downloaded as today. A generated **catalog** (`packs/languages.tsv`
and a Kotlin `LanguageCatalog`) drives the packs, the Gradle modules, the fetch script and the app; names come from
the phone (`LanguageNames` port); debug bundles carry only the fetched packs so tests push only what they need.

## Technical Context

**Language/Version**: Kotlin 2.4.21 (Multiplatform; Android now); Python 3 and Bash for the catalog and fetch scripts.

**Primary Dependencies**: none new. Compose Multiplatform 1.12.2, Koin, DataStore, Play asset-delivery-ktx 2.3.0 and
Bergamot as in spec 012.

**Storage**: `translator.json` gains `languages` (optional, `schemaVersion` stays 1; research R8). Packs as in 012.

**Testing**: domain test-first (`CheckedLanguages`, the catalog, `languagesOf`), Robolectric screen tests of the list
entry and the page, `LayoutFitTest` and the accessibility audit for the page, Konsist unchanged; on the emulator the
engine over every language in batches (`scripts/ci.sh engine --all`, R9) and the existing journeys with two packs.

**Target Platform**: Android 8.0+ as in spec 012; iOS later behind the same ports (`LanguageNames` too).

**Project Type**: mobile app (Kotlin Multiplatform, Compose).

**Performance Goals**: the page opens and filters instantly for 54 rows; SC-001 (find and check in under 15 s).

**Constraints**: Google Play: ≤ 100 asset packs, ≤ 1.5 GB each, ≤ 4 GB on demand (about 2.2 GB here; R3); the app's own
download grows ≤ 0.5 MB (SC-004); local testing pushes only fetched packs (FR-015); the owner's phone is nearly full.

**Scale/Scope**: 54 languages, 53 packs, 1 new page, 1 new design-system row, 1 new port, 2 scripts.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Principle | How this plan meets it | Pass |
|---|---|---|
| I. Offline-first | Each language's pack is downloaded only after the person confirms it, in `core/content`, as in 012; names come from the phone, no network | ✅ |
| II. Device-side only | The checked set is a setting on the phone, exported like the others; nothing else stored | ✅ |
| III. Clean architecture | Catalog and rules in `core/domain`/the feature's domain; names behind a port; Play and Locale only in platform modules | ✅ |
| IV. Clean code | Generated catalog instead of three hand lists; named constants (at least two) | ✅ |
| V. Test-first domain | `CheckedLanguages`, catalog parsing, list order written test-first | ✅ |
| VI. Respectful monetization | Unchanged: a normal tool | ✅ |
| VII. Modern, localized UI | New strings in three languages; language names in the app's language from the phone; RTL text | ✅ |
| VIII. Multiplatform-ready | `LanguageNames` is a port; the catalog is common code | ✅ |
| IX. Least permission | No new permission | ✅ |
| X. Accessible | `CheckRow` one target with checked state; 200%, landscape, both themes in the fit and audit tests | ✅ |
| Technology constraints | Every model file hash-pinned (`packs/languages.tsv`); Play limits checked (R3) | ✅ |

No violation: Complexity Tracking stays empty.

## UI Rules Check

*Details in [contracts/ui.md](contracts/ui.md).*

- **Lists**: "Other languages…" is `MenuChoice`'s own optional last entry, so every list gets it the same way, and a
  turned half's list turns it too.
- **The page**: top = back, title and `SearchField` only (S1); middle = the rows, the only scrolling part (L1–L5); no
  bottom actions (nothing to confirm). Rows are the new `CheckRow` (design system first, VII). One `ShortNote` when
  two are locked (R4-rules: one note).
- **Nothing jumps**: checking a row changes only its box; the note's room is a `ReservedLine`.
- **Text**: strings in three languages; Help gains one block on choosing languages (≤ 180 characters).
- New decision in `docs/ui-rules.md` with this pull request: the language page and `CheckRow`.

## Project Structure

### Documentation (this feature)

```text
specs/080-translator-languages/
├── spec.md
├── plan.md              # this file
├── research.md          # R1–R10
├── data-model.md        # catalog, checked languages, names
├── quickstart.md
├── contracts/
│   ├── ui.md            # the lists' entry and the page
│   └── catalog.md       # packs/languages.tsv, pack.json's two vocabularies, the fetch script
├── checklists/requirements.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
scripts/update-language-catalog.py        # NEW: registry → packs/languages.tsv + LanguageCatalog.kt (pins hashes)
scripts/fetch-language-packs.sh           # reads packs/languages.tsv; --languages / --all
packs/languages.tsv                       # NEW, generated, committed
packs/pack.gradle.kts                     # NEW: the one build script every pack module uses
settings.gradle.kts                       # includes :packs:translate_<code> from languages.tsv
androidApp/build.gradle.kts               # assetPacks from languages.tsv: fetched ones in debug, all in release

core/domain/src/commonMain/kotlin/tech/onova/offlinetranslator/core/domain/
├── ports/LanguageNames.kt                # NEW port
└── translation/LanguageCatalog.kt        # NEW, generated: OfferedLanguage list
core/platform/src/androidMain/kotlin/.../platform/AndroidLanguageNames.kt   # NEW (Locale)
core/translation/.../PackDescription.kt, BergamotEngine.kt                  # source and target vocabularies apart
core/content/.../PlayContentPacks.kt                                        # the catalog's packs; asks Play only about the bundled ones (R3, as verified)
core/designsystem/.../components/CheckRow.kt                                # NEW
core/designsystem/.../components/MenuChoice.kt                              # optional last entry ("Other languages…")

feature/tools-translate/src/commonMain/kotlin/.../toolstranslate/
├── domain/CheckedLanguages.kt            # NEW rules
├── domain/SpeechRegions.kt               # from the catalog
├── domain/TranslatorSettings.kt, data/…  # languages
└── presentation/LanguagesPage.kt, …      # NEW page; the lists' entry; the basic-quality note; RTL

androidApp/src/test/.../screens/LanguagesPageTest.kt, LayoutFitTest, AccessibilityAuditTest
androidApp/src/androidTest/.../BergamotEngineTest.kt  # every pushed language
scripts/ci.sh                             # engine --all in batches of five
```

**Structure Decision**: the layout spec 012 built. The catalog sits in `core/domain` because `androidApp` (packs,
bindings), `core/content` and the feature all need it, and it is pure data.

## Post-design Constitution Check

Re-checked after Phase 1: no network beyond the confirmed pack download (I); the catalog and rules are pure and
test-first (III, V); names behind a port (VIII); the page is app components with one target per row (VII, X).
**Still passing.**

Owner's answers: `no` dropped as a duplicate of `nb`, 53 languages besides English ("Ok with the Norwegian choice",
2026-11-19);
two packs (pt, es) fetched for tests and phone installs by default, all 53 only for a release and the batch engine test.

## Complexity Tracking

No constitution violation to justify.

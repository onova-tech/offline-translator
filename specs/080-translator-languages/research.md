# Research: More languages in the Translator (spec 080)

Builds on spec 012's research (`specs/012-offline-translator/research.md`): the engine (R3), the models (R4), the
packs (R5, R6) and speech (R1, R2, R9) stay as they are. This file covers what 54 languages and a checklist change.

## R1. Which languages, which models

**Decision**: a **catalog** of every language Mozilla's model registry (`db/models.json`, fetched 2026-11-19) has
released in both directions with English, generated once by a script and committed. A direction counts when an entry
has `releaseStatus` "Release", "Release Android" or "Release Desktop"; of those, the larger `base-memory` model is
taken over `base` and `tiny`. A language is **basic quality** when either direction is `tiny` (spec FR-006).

On 2026-11-19 that gives 54 languages: 31 full (af ar be bg ca cs de es et eu fi fr gl hu is it ja ko mr nl pl pt ru
sl ta th tl ug ur zh zh_hant) and 23 basic (bn da el fa gu he hi id kn lt lv ml ms nb no ro sk sq sv te tr uk vi).

- **`no` and `nb`** are both in the registry, both `tiny`: Norwegian Bokmål and "Norwegian", which in writing is the
  same language. Two rows that look alike would confuse; the catalog keeps **`nb`** ("Norwegian Bokmål") and drops
  `no`, so the page offers **53 languages besides English**. Told to the owner with the plan.
- **Chinese** comes as `zh` (simplified characters) and `zh_hant` (traditional): two rows, named "Chinese
  (Simplified)" and "Chinese (Traditional)" (R4); their speech regions are China and Taiwan (R5).
- Quality figures (COMET-22 on flores200-plus) are kept in the catalog for the record, not shown.

**Rationale**: the owner chose "All 54". One generated catalog keeps the app, the Gradle packs and the fetch script in
step, and a later registry release only means running the script again (spec Assumptions).

**Alternatives**: hand-written lists in three places (drift); reading the registry at run time (network: Principle I).

## R2. Pinning and fetching 53 packs

**Decision**: `scripts/update-language-catalog.py` reads the registry and writes **`packs/languages.tsv`**: one line per
direction with its registry folder, file names and SHA-256s. The registry gives the model's hash; the shortlist's and
vocabularies' hashes are taken when a direction is pinned (as spec 012 did for four), which downloads every file once
(about 2.7 GB on this machine, which has 300 GB free). `scripts/fetch-language-packs.sh` then reads that file instead
of its four inline lines, with `--languages pt,es` (the default: what the tests use) or `--all` (a release).

- Seven directions ship **separate source and target vocabularies** (en→ja, en→ko, en→zh, en↔zh_hant): `pack.json`
  gains optional `sourceVocab`/`targetVocab` beside `vocab` (contracts/pack-format.md), and `BergamotEngine` passes them
  apart (its JNI call already takes two arrays).
- A release bundle fails without every pack's models, as today (spec 012 T007), now for all 53.

**Rationale**: every byte stays hash-checked (constitution, Technology constraints); tests need only two packs.

## R3. 53 asset packs in Gradle, and only the needed ones in tests

**Decision**: the pack modules are **declared from `packs/languages.tsv`** rather than written out: `settings.gradle.kts`
includes `:packs:translate_<code>` for each language, each with the same small build script (`packs/pack.gradle.kts`:
the `com.android.asset-pack` plugin, its name, on-demand delivery). `androidApp`'s `assetPacks` comes from the same
list. In **debug** builds only the packs whose models were fetched join the bundle, so local testing pushes only those
to the emulator or the phone (spec FR-015); a **release** bundle takes them all.

**To verify first (T-item)**: with only some packs in a local-testing bundle, Play's `getPackStates` for the whole
catalog must not fail the request; if it does, `PlayContentPacks` asks pack by pack, and a pack the bundle lacks reads
as unavailable on this install.

**Verified 2026-11-19 (T006)**: it **fails**. A debug bundle with only `translate_pt` and `translate_es`
(`app-local-testing.apks` held those two asset slices), installed through local testing on the Android 36 emulator:
`getPackStates` over the 53 packs was refused as a whole, so every pack, Spanish included, read as "Language packs
can't be downloaded on this phone" and `TranslatorJourney` failed at the pack offer. **So (T014)**: the build writes the
packs its bundle holds into `BuildConfig.BUNDLED_PACKS` (every pack in a release, those with models in a debug build);
`PlayContentPacks` asks Play only about those, in one request as before, and a pack outside the bundle is unavailable
from the start (`PlayPackMapping.initial`, `asked`). Asking pack by pack instead would have cost 53 requests per
refresh for the same answer.

**Google Play's limits** (checked 2026-11-19, Play Console help "Optimize your app's size"): at most 100 asset packs
per bundle, each at most 1.5 GB, and 4 GB in total for on-demand packs by default. 53 packs of 25–65 MB come to about
2.2 GB (spec FR-013).

**Alternatives**: 53 hand-written module folders (noise, drift); one pack with every language (a 2.2 GB download for
one language).

## R4. Language names

**Decision**: names come **from the phone**, through a new port `LanguageNames` in `core/domain`: `nameOf(language, in:
app language)` and `ownName(language)`. Android: `Locale.forLanguageTag(tag).getDisplayName(…)`, with the first letter
capitalised for the language's own name; tags `zh-Hans` and `zh-Hant` give "Chinese (Simplified)" and "Chinese
(Traditional)", `nb` "Norwegian Bokmål". iPhone later: `Locale.localizedString(forLanguageCode:)`. The three languages
the app already names keep their own strings (spec 012), so nothing on today's screens changes.

**Rationale**: 53 × 3 hand-made names would be strings to maintain; the phone's names are correct in its languages and
cost no size (spec SC-004). Constitution VII (localized UI) is met: the names follow the app's language.

## R5. Speech regions

**Decision**: `SpeechRegions` gains a **default region per language** (fr-FR, de-DE, it-IT, nl-NL, pl-PL, ru-RU,
ja-JP, ko-KR, zh-CN, zh-TW for zh_hant, ar-SA, tr-TR, uk-UA, hi-IN, …; a bare language tag where none is common),
generated into the catalog; the phone's own region wins where it matches the language, as today (spec 012 R9).
Recognition and voices stay the phone's (spec FR-012): a language the phone's recognizer doesn't know says so.

## R6. Right-to-left languages

**Decision**: the text field, the translation and the turns use `TextDirection.Content` with `TextAlign.Start`, so a
paragraph in Arabic, Persian, Hebrew, Urdu or Uyghur is laid out right to left and starts at the right (spec FR-011);
the layout around it stays the app's. A screen test types Arabic and checks the direction.

**As built (T025)**: the test failed first: Material 3 leaves a text style's direction unspecified, which an app laid
out left to right resolves to left to right whatever the text. `PocketTextField` (the design system's, so every tool's
fields) now asks for `TextDirection.Content`, and the translation, the turns and the words being heard take
`inItsOwnDirection()`. Latin text reads exactly as before (no screenshot changed for it).

## R7. The screens

**Decision** (contracts/ui.md):

- **"Other languages…"** is a new optional last entry of `MenuChoice` (a divider, then the entry), so every Translator
  list gets it from one place, turned in a turned half too (spec 012's `LocalHalfTurned`).
- **The language page** is a full-screen dialog like the Conversation's correction: the host's back, a `SearchField`
  on top (S1), then one row per language, the only part that scrolls (L1–L5). A row is a new design-system
  **`CheckRow`**: the name, its own name and "Basic quality" as supporting text, a check box at the end, the whole row
  one target (Principle X). With two checked, those two rows are disabled and one `ShortNote` says at least two are
  needed (FR-005). No actions at the bottom: a change applies at once, and back closes it.
- **Settings → Offline languages** lists the checked languages, then installed packs of unchecked ones.

## R8. The setting

**Decision**: `TranslatorSettings` gains `languages: List<LanguageCode>?` (null: English, Portuguese, Spanish). The
document stays `schemaVersion` 1: the field is optional, so older files read as the default and older apps ignore it.
On reading, codes the catalog doesn't have are dropped, and fewer than two give the default (FR-007).

## R9. Testing every language

**Decision**: `scripts/ci.sh engine --all` runs `BergamotEngineTest` over every language in **batches of five**: the
batch's packs are pushed to `/data/local/tmp`, a sentence is translated both ways with English, and the batch is
removed before the next (spec SC-002), so the emulator never holds 2.2 GB. The usual `engine`, `e2e` and phone installs
fetch Portuguese and Spanish only.

**As run (T027, 2026-11-19, Android 36 ATD emulator, x86_64)**: `scripts/fetch-language-packs.sh --all --into
build/language-packs` fetched the 53 packs (3.2 GB unpacked; one Dutch file failed its hash once and matched on the
second try, a transfer error), and `scripts/ci.sh engine --all` passed **all 53 in 11 batches**, each batch of five in
16–20 s, model loading included. Every language opened (`Ready`) and gave "Good morning! Where is the nearest train
station? Thank you very much." and back in its own script (Arabic, Persian, Hebrew, Urdu and Uyghur right to left;
Bengali, Gujarati, Hindi, Kannada, Malayalam, Marathi, Tamil, Telugu, Thai, Chinese both ways). Each sentence is in
`build/ci/engine-languages.txt`. What the round trips show of quality, for the owner to weigh, none a failure:
- Some back-translations repeat a greeting ("Good morning! Good morning!" from Danish, Italian; "Hello! Hello!" from
  French; "Thank you. Thank you." from German).
- Hindi (basic quality) wrote a non-word for "Good morning" ("सुप्रभतंग", back as "Good serp to be!"); Russian coined
  "железнодачиная" for "railway" (back as "iron station").
- Portuguese uses the European word for train, "estação de comboios", where Brazil says "estação de trem"; spec 012's
  Portuguese model is the same one.

## R10. Size

**Decision**: the app's own download grows only by the catalog and the new screens (estimated under 0.1 MB; measured
as spec 012 T075 did, spec SC-004). The packs are on demand.

# Automated tests

Everything runs through one script, the same way a CI server would. Results and failure details go to `build/ci/`.

```bash
scripts/ci.sh              # everything: check, then the emulator stages
scripts/ci.sh check        # what GitHub Actions runs; no emulator
scripts/ci.sh device       # the emulator stages: a11y, then e2e
scripts/ci.sh a11y         # the accessibility suite and the real-font checks on the emulator
scripts/ci.sh e2e          # the end-to-end tests on the emulator
scripts/ci.sh e2e --test TranslatorJourney                       # one class
scripts/ci.sh e2e --test TranslatorJourney#aPackInstallsThenTheRealEngineTranslatesInAirplaneMode --keep-emulator
scripts/ci.sh engine       # the Translator's engine with the real packs (after scripts/fetch-language-packs.sh)
scripts/ci.sh engine --all # every language, five packs at a time (fetches all 53, about 2.2 GB, into build/language-packs)
```

## What runs where

| Stage | Where | What it proves |
|---|---|---|
| `check` | GitHub Actions and local | Unit tests, Compose UI tests on Robolectric, screenshot baselines, `LayoutFitTest`, `ModeStabilityTest`, `UiRulesTest` (`docs/ui-rules.md`), the accessibility audit, architecture and resource rules, the network guard, detekt, ktlint, Android lint, domain coverage of at least 90% |
| `a11y` | Local, emulator | Android's accessibility checks on a real runtime (`AccessibilityChecksTest`), and what only real fonts show, such as the fallback font of the tuner's ♯ (`RealFontsTest`) |
| `e2e` | Local, emulator | The installed debug app, driven from outside like a person would (`tooling/e2e`) |
| `engine` | Local, emulator | The Translator's real engine (`libbergamot.so`) with the real packs: pt→en and pt→es through English, each under 5 s (`BergamotEngineTest`, spec 012). The packs are pushed to `/data/local/tmp` and copied in with `run-as`; the test is skipped without them. With `--all` (spec 080), every language of `packs/languages.tsv` translates a sentence from English and back, five packs on the emulator at a time |

## The end-to-end tests

`tooling/e2e` is a separate test package that drives the real app with UI Automator. It only sees and taps what is on
the screen, so the app's real Koin graph, storage, navigation and process restarts all run.

| Journey | Covers |
|---|---|
| `TranslatorJourney` | A language pack installed through local testing, then the real engine in airplane mode: a sentence, its translation, the swap, Copy; the pack deleted in Settings (spec 012) |

- **Any crash fails the test**, even if the steps passed: the rule reads the crash log after each one.
- **Failures** leave the screen's hierarchy and the crash log in `build/ci/e2e-failures/`, and the log in `build/ci/`.
- **Ad journeys need the emulator's internet.** Without an ad they report as skipped, and the summary counts the skips.
  A skip means that path wasn't tested, so check for it.
- **The file dialog is a stand-in.** The emulator image has no system file dialog, so the test package answers those
  requests itself (`FilePickerStandIn.kt`). The app's real launcher and file code still run; the dialog itself doesn't.
- **What the emulator can't do:** real camera, GPS and most sensors. `EveryToolJourney` only proves those tools open.
- **The app is installed from the bundle,** through bundletool's local testing (`build-apks --local-testing`, then
  `install-apks`; bundletool 1.18.3, the version AGP uses, run from the classpath `:androidApp:bundletoolClasspath` writes, with the SDK build-tools' aapt2): its on-demand language packs then come from the
  emulator's own storage instead of Google Play (spec 012 T042). A journey's wipe deletes them, so `scripts/ci.sh` keeps a
  copy in `/data/local/tmp/offlinetranslator-local-testing` and `TranslatorJourney` puts it back. Without the bundle the script
  installs the plain APK and that journey is skipped. Network errors, the Wi-Fi wait and updates can't be tested this way.

### Never on a phone

The tests wipe the app's data, and the owner's phone is usually on adb too. Three things protect it:

1. `scripts/ci.sh` talks only to its own emulator (`emulator-5580`) and refuses anything that isn't one.
2. Every test checks it is on an emulator before touching anything.
3. Gradle's `connected*` tasks are switched off for `tooling/e2e`, because they would install on every attached device.

Don't run `./gradlew connectedDebugAndroidTest` with the phone attached either: it installs on every device.

### Writing one

Extend `Journey`, which starts each test on a wiped app, and use `app` (`AppDriver`): `see`, `seeGone`, `neverSee`, `tap`,
`type`, `openTool`, `leaveTool`, `restart`, `wipe`. Screens are in English, because the emulator is. Add a journey for what
only a real runtime proves; rules and screen states belong in the faster `check` stage.

## GitHub Actions and its free limit

This is a public repository, so GitHub's standard runners cost nothing. `.github/workflows/ci.yml` still keeps runs
rare and short: pull requests into `main`, pushes to `main` and by hand; not drafts, nor commits that only touch
documents or specs; a new push cancels the run it replaces.

The emulator stages stay local on purpose: a hosted emulator would spend a large share of the month on every run.

## Window sizes (spec 015)

`LayoutFitTest` runs 21 classes, each a window, a text size and a language. Rule `FITS` holds the phone and the tablets; rule
`REACHABLE` holds every other window: scrolling is allowed, and anything cut off, a scroller under 48 dp, touch areas that overlap, a
crash or a hang fails the build. Only `window-240x360-f2.0` just keeps its table (`docs/decisions-pending.md`, spec 015 row 17).

`docs/layout-fit/` keeps the tables of the last time the rule changed, so a change can be compared. To refresh it, run
`./gradlew :androidApp:testDebugUnitTest --tests '*LayoutFit*'` and copy `androidApp/build/layout-fit/*.txt` there.

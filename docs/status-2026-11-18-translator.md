# Status on 2026-11-18: the Translator (spec 012) and Test Lab (spec 079)

Where the work stands, what was verified and how, and what's left, written when the weekly limit was near. The next
session can resume from "Next steps".

## Branches and pull requests

| Branch | What | State |
|---|---|---|
| `012-translator-foundation` (this worktree: `.claude/worktrees/leftovers-xfer`) | Spec 012, the Translator, all of it | Pushed; **PR #91** (draft), stacked on #90 (spec 078), which is on #89 |
| `079-test-lab` | Spec 079, Firebase Test Lab: the design only | Pushed (`b8110dc9`). Waiting for the owner's approval; free plan chosen; the owner sets Test Lab up on a weekend |
| #89, #90 | Specs 077 and 078 | Open, not merged. Merge order: #89, #90, then the Translator |
| privacy.onova.tech PR #3 | The `/polytools/` policy | Open |

## Spec 012: done

Every story is built, tested and pushed (commits `457bbd72` to `81920b75`):

| Part | Tasks | Commit |
|---|---|---|
| Phone probe on the owner's SM-G780G: R1 (on-device recognizer: pt-BR ready, en/es downloadable), R2 (Samsung's voices; **fixed**: its three-letter locales `por_BRA_f00` made the app say "no voice"), R3 (pt→en 0.5 s with loading, 0.16 s after; pt→es 0.6–0.8 s; SC-001 passes, base-memory models stay; ~335 MB native heap per model, given back on close) | T033 | `457bbd72`, `fc6c9945` |
| US1 Type or speak: languages + swap, text + counter, translation with one note, Copy/Share, announced once, never while typing; `EngineSession` (engine open only while a Translator screen uses it) | T034–T038 | `c8504524` |
| US2 Download a language: offer with size, nothing before Download, progress + Cancel, Wi-Fi wait, no space, failed, no store; `PlayContentPacks` (Play asset delivery), `SettingsPages` port | T039–T041 | `f1ccad4c` |
| US3 Settings → Offline languages: Wi-Fi switch, rows with ⋮ (Download/Cancel/Delete asks first), space used | T043–T045 | `6d0e96ca` |
| US4 Speak and hear: microphone in the field (PermissionGate on first tap), 2 s pause ends a turn, Read aloud ↔ Stop, fixes (Get this language, Voice settings) | T046–T048 | `877ba032` |
| US5 Conversation: two facing halves (rule C1, `FacingHalves`, `MicrophoneButton`), turns translated on the other half and read aloud, no mic while speaking, correct a turn, TalkBack announces, hidden screen pauses, A3 exception | T049–T054 | `25ad4f3a` |
| Polish: Help (3 languages), accessibility audit, fit/rules/mode tests, screenshots (looked at), Konsist rule, data part | T058–T063 | `e0b88e5d` |
| Open-source licenses page (AboutLibraries offline; 25 custom entries; `scripts/collect-engine-licenses.py`; the build record showed 7 more native pieces than NATIVE.md listed) | T055–T057 | `7152f38a` |
| Pack journey through bundletool local testing + test fixes + M1 exception | T042 (emulator part), T064 | `81920b75` |

**Verified:** `./gradlew check` is green: 928 Robolectric tests (the 3 that failed under load were fixed and pass), and
the rest of `check` (lint, Kover, detekt, ktlint, architecture tests, the device tests compile) in a second run. The
machine's JVM crashed four times on the way (SIGSEGV, the known RAM problem; `docs/status-2026-11-08.md`).

## 2026-11-19: verified, converged, and in a pull request

- The device run that hung (Gradle at 99% CPU for three hours) was stopped. The next one showed two more problems on
  the way to the emulator, both fixed: bundletool's library jar needs Kotlin's standard library from its host
  (`:androidApp:bundletoolClasspath` adds it), and `ci.sh` now says "the app didn't install" instead of blaming the
  emulator.
- With the app installed through local testing, 16 journeys crashed in Settings: a wiped app has lost local
  testing's folder, and Play's `LocalTestingException` was never caught. Every call to the store now goes through one
  helper, and a refusal reads as the state it means. The Color picker's accessibility check, stale since spec 078,
  waits for Picture now, and a second check opens Camera.
- `/speckit-converge` found nine gaps (Phase 11, T068–T076), all built: Google Play's confirmation dialog, the
  microphone on Android 12+ without a recognizer, a progress line while a translation is replaced, Help on the source
  language, a damaged pack offered again, calls and alarms pausing a conversation, a hidden screen stopping a
  translation, SC-006 measured (+0.19 MB to download without the engine's native code), and decisions-pending row 17
  (the dataSync permission). A second pass over the nine found nothing left.
- The first release build of the branch stopped in R8 on a missing annotation class from asset-delivery-ktx; fixed
  with R8's own `-dontwarn` rule. No release build has run on a device.
- **Green:** `scripts/ci.sh check`, accessibility 57/57, end to end 36/36 (with `TranslatorJourney` through local
  testing), and the engine stage.

## Next steps

1. The owner's pass (T066): both modes in airplane mode with TalkBack, quickstart §3 and §4, on the phone.
2. R5 (T042's phone part), with the owner's consent: bundletool's `install-apks` on the phone, for user 0 only
   (Secure Folder); its storage is nearly full, so free space before the 70 MB packs.
3. Merge order for the owner: #89, #90, then #91 (the Translator); #15 (the spec alone) can then be closed.
4. Spec 079 (Test Lab): after the owner approves the design and creates the Firebase project, `/speckit-plan`.

## Notes for the next session

- Before a long build or the emulator, check `free -m`: a 5-hour Gradle daemon and Kotlin daemon held 6 GB today.
- Robolectric screen tests must idle the main looper while they wait (`shadowOf(Looper.getMainLooper()).idle()` in
  `waitUntil`): a view model's action resumes there after a DataStore read on another thread.
- Real downloads need the Play Console account (D-U-N-S pending), like the purchase.

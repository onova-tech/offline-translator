# Decisions waiting for the owner

The owner asked for work to continue without stopping for questions. Each item below has a
recommended default that is already implemented. Confirm it, or say what to change; most changes
are one line.

## Spec 002: usage tracking and ads

| # | Question | Implemented default | Where to change |
|---|---|---|---|
| 1 | If no ad is ready (offline, no fill) when the 3rd tool is left, does the count reset? | **No.** The count resets only when an ad is actually shown; the next return to Home tries again. | `AdCoordinator` (domain) |
| 2 | Does reopening the same tool count again? | **Yes**, every open from Home counts. Rotation, switching apps, restoring the app and opening an unavailable tool do not. | `AppNavHost` hooks |
| 3 | Does switching to another app count as "leaving" a tool? | **No.** Only going back to Home inside the app. | `AppNavHost` hooks |
| 4 | When does the consent form appear (EEA/UK/CH only)? | At launch, over Home; if a tool is open, it waits for the next return to Home. | `AdCoordinator` |
| 5 | Maximum ad content rating | **PG** (general audiences with parental guidance). Options: G, PG, T, MA. Lower ratings mean fewer, cheaper ads. | `AdsConfig.maxAdContentRating` |
| 6 | Use the advertising ID (personalized ads where the user consents)? | **Yes** (`AD_ID` permission kept). Removing it lowers revenue; the Data safety form must declare it (spec 011). | `androidApp` manifest + `permissions.allowlist` |
| 7 | `READ_BASIC_PHONE_STATE` (added by the ads SDK) | **Removed.** No tool needs it and ads work without it. | `androidApp` manifest |
| 8 | Ad sound | **SDK default** (video ads may play sound). Option: start ads muted, which lowers video-ad eligibility. | `GmaInterstitialAds` |
| 9 | Ads SDK | **GMA Next-Gen SDK 1.5.0**, Google's recommended SDK (the classic one is in maintenance). | `libs.versions.toml` |

## Spec 004: everyday utilities

| # | Question | Implemented default | Where to change |
|---|---|---|---|
| 1 | Timer alerts while the app is in the background: on by default? | **Off until the user turns them on**; offered when the first timer starts. Turning them on asks for the notification permission (and any alarm permission the plan needs). | Timer options, `specs/004-everyday-utilities` FR-024 |
| 2 | Calculator precision and percent | **Exact decimal** for + − × ÷ %, 12 significant digits shown; `a + b%` adds b percent of a, like phone calculators. | Calculator engine (domain) |
| 3 | What a note can contain | **Plain text** with an optional title; no formatting, checklists, images or sharing. | Notepad |
| 4 | Stopwatch and Timer as one tool or two; Random as one tool | **One tool each** (tabs or modes inside), to keep Home compact. | Tool registration |
| 5 | BMI remembers the last height and weight | **Yes**, on the device only. Option: remember only the units. | BMI tool |
| 6 | ~~How the Notepad list shows the date of a change~~ | **Decided by the owner (2026-10-23)**: in the date format chosen under Settings → Units & formats; by default the language's (21/10/2026 in Portuguese). | Settings, spec 004 FR-045 |
| 7 | ~~How a timer's length is entered~~ | **Decided by the owner (2026-10-23)**: an in-app number pad under a large `00h 00m 00s`, digits filling from the right; the name just above Cancel and Start. | spec 004 FR-047 |
| 8 | One switch or two for the Stopwatch & Timer notifications | **One "Notifications" switch** covers both the running notifications (pause/stop from the notification) and the alert when a timer ends with the app closed. It's offered the first time the stopwatch or a timer starts. | spec 004 FR-024, FR-046 |
| 9 | What a timer notification's Cancel does | **Resets the timer** to its full length; it stays in the list. Option: delete it. | spec 004 FR-046 |
| 10 | Whether running notifications also show while the app is open | **Yes**: they stay while something runs, so the controls are always where the user left them. | spec 004 FR-046 |
| 11 | Words mode limits | **500 entries of up to 100 characters**, one per line. | spec 004 FR-048 |
| 12 | Time zone abbreviations | **One built-in list, the same in every language** (BRT, EST/EDT, CET, JST…). Zones without a common one show their UTC offset, such as "(UTC+5)". | spec 004 FR-049 |
| 13 | Age in the BMI notice | **20 and over**: the WHO and CDC use age-based charts up to 19. | spec 004 FR-033 |
| 14 | Ruler under the bottom-controls rule | **Calibrate stays on top**, beside cm/in: a bottom bar would shorten the scale by about 1 cm. Reset calibration stays below them when calibrated. | `RulerContent.kt` |

## Specs 005–013 (specified 2026-10-23, not built yet)

Each row is the default written into the spec's clarifications. Nothing is built yet, so changing
a default means editing the spec (and its plan and tasks) before the build starts; say which one and
the spec is updated.

### Spec 005: motion sensors

| # | Question | Default in the spec | Where to change |
|---|---|---|---|
| 1 | Smoothing of the readings | **Balanced**; Bubble level, Protractor, Compass and Plumb bob offer Responsive / Balanced / Smooth per tool. | spec 005 FR-006 |
| 2 | Pedometer history | **None.** Today's steps only, counted from the first time the tool is opened that day (the app can't read the sensor while closed), reset at local midnight. | FR-040–FR-043 |
| 3 | True north without location | The user types the **magnetic declination** (±60°); magnetic north until then. No location permission. Since spec 024 that's in More options, and the position can find it. | FR-021 |
| 4 | Metal detector alert | **Visible alert plus vibration pulses**, faster as the field grows. No sound until spec 007. | FR-034 |
| 5 | Screen on while open | **Yes** for every tool except the Pedometer. | FR-007 |
| 6 | Home categories | DIY: Level, Protractor, Plumb bob. Measure: Compass, G-meter, Vibration meter, Metal detector, Pedometer. *Spec 032: the Plumb bob is now the Bubble level's Plumb mode.* | FR-002 |
| 7 | G-meter unit | **g**, with a switch to m/s². | FR-027 |
| 8 | When calibration is accepted | Only within **5°** of flat; otherwise a message and nothing saved. | FR-012 |

### Spec 006: light and camera

| # | Question | Default in the spec | Where to change |
|---|---|---|---|
| 1 | Scanner history | **Latest 100 scans**, on the device, deletable one by one or all. | FR-051 |
| 2 | Flashing-light safety | A **one-time full-screen notice** before the first strobe; strobe 1–10 Hz, default 2. | FR-007 |
| 3 | Torch brightness | A slider **only where the phone supports levels** (Android 13+). | FR-006 |
| 4 | Torch after leaving the tool | **Off.** A background flashlight needs a foreground service. | FR-009 |
| 5 | Scanner and links | Links are **shown, copied or shared, never opened** by the app. *Spec 035 keeps this: a network or a contact goes to the phone's own screen, where the person confirms.* | FR-050 |
| 6 | Screen light | Palette of **eight** colors, brightness 10–100%, starts at full, screen stays on. | FR-020 |
| 7 | Color names | The closest of **about 30** basic names, in the app language; HEX and RGB always. | FR-040 |
| 8 | Barcode formats | QR, Data Matrix, Aztec, PDF417, EAN-13, EAN-8, UPC-A, UPC-E, Code 128, Code 39, ITF. | FR-049 |
| 9 | Libraries | **CameraX 1.6.2** (1.7.0 is only a release candidate; `camera-compose` has no stable release) and **ZXing core 3.5.4**, behind ports; a new `core/camera` module. Estimated APK growth ≤ 4 MB. | plan.md |
| 10 | Color picker on pictures (owner's request, 2026-10-31): how much is averaged | A square of **2%** of the picture's shorter side around the marker (the camera averages 5% of its view). | `PictureSampling.SAMPLE_PERCENT` |
| 11 | Color picker on pictures: picture size in memory | At most **1280 px** on the longer side (about 6.5 MB). Android 8 shows pictures as stored, not turned upright. | `ColorPickerViewModel.MAX_PICTURE_SIDE` |
| 12 | Color picker buttons | **Copy HEX and Copy RGB side by side** in both modes, so the picture gets more room. | `ColorPickerContent` |
| 13 | Color picker on a phone without a camera | Still **unavailable** on Home, though Picture mode would work. | `ColorPickerToolEntry` hardware |

### Spec 007: sound tools

| # | Question | Default in the spec | Where to change |
|---|---|---|---|
| 1 | Sound level | **dB(A)** by default, flat as an option; Fast (125 ms) or Slow (1 s); an "approximate, not certified" notice. | FR-004, FR-005 |
| 2 | Calibration | An offset of **−30 to +30 dB**; the default assumes 90 dB at full scale. | FR-006 |
| 3 | Tuner reference pitch | **A4 = 440 Hz**, adjustable 415–466 Hz. | FR-020 |
| 4 | Tuner instruments | **Chromatic, Guitar, Ukulele.** Spec 026 added bass, cavaquinho, viola caipira, violin, viola and cello, with tunings. | FR-021 |
| 5 | Metronome in the background | **Stops** when the tool or app is left; the screen stays on while it runs. | FR-033 |
| 6 | Time signatures | 2/4, 3/4, 4/4, 5/4, 6/8, 7/8, 9/8, 12/8; accent on the first beat. | FR-031 |
| 7 | Tone safety | Starts at **30%**, one-time hearing notice, Dog whistle 15–20 kHz with a warning and a **60 s** auto-stop. | FR-041–FR-044 |
| 8 | Tone waves | Sine, square, triangle, sawtooth, 20–20,000 Hz. | FR-040 |

### Spec 008: GPS tools

| # | Question | Default in the spec | Where to change |
|---|---|---|---|
| 1 | Speed units | **km/h, mph, knots, m/s**; the default follows the unit system. | FR-004 |
| 2 | Background tracking | **None.** Location only while a GPS tool is open. | FR-003 |
| 3 | Average speed | **Distance ÷ moving time** (stops are not counted). | FR-007 |
| 4 | Coordinate formats | **Decimal degrees, DMS, DDM**; always a dot as decimal separator; no map links, no UTM. | FR-021 |
| 5 | Drag targets | 0–50, 0–100, 0–200 km/h, 0–60 mph and custom; default follows the unit system. | FR-030 |
| 6 | Approximate location only | **Not accepted**: the tools explain that precise location is needed. | FR-002 |
| 7 | Location switched off | The tool says so and opens the phone's location settings on tap. | FR-005 |
| 8 | Safety | A one-time full-screen notice before the first drag run; a one-line reminder in the Speedometer. | FR-010, FR-034 |

### Spec 009: device and measure

| # | Question | Default in the spec | Where to change |
|---|---|---|---|
| 1 | NFC | **Read-only**; never writes and never queries payment or access cards. *Changed by spec 030: it writes when the person asks (owner, 2026-11-06).* | FR-020–FR-023 |
| 2 | Heart rate history | **None**; nothing is stored. *Changed by spec 031: results are kept in History (owner, 2026-11-06).* | FR-040 |
| 3 | Heart rate measurement | **20 s** after the finger is detected; restarts if the finger moves; weak results are labelled. | FR-034, FR-036 |
| 4 | "Thermometer" | The **battery temperature** with ratings (cold, normal, warm, hot, very hot), in °C or °F by the unit system. | FR-010, FR-011 |
| 5 | Distance meter | Phone height at eye level, default **1.60 m**; distance from the angle to the foot, height from the angle to the top or a typed distance. | FR-030–FR-032 |
| 6 | Missing battery values | The row is **left out**. | FR-003 |
| 7 | Dependencies | Distance meter needs specs 005 and 006; Heart rate needs 006. Battery and NFC are independent. | Assumptions |

### Spec 012: offline translator (voice and Conversation added 2026-11-16)

Rows 13–16 were approved by the owner on 2026-11-17: "For me all decisions are fine. I just want that the page feels
consistent and looks good." Every new screen (Conversation, the licenses page, the probe page) uses the app's own
components and follows `docs/ui-rules.md`.

| # | Question | Default in the spec | Where to change |
|---|---|---|---|
| 1 | First languages | **English, Portuguese, Spanish.** | FR-002 |
| 2 | Non-English pairs | **Through English** (fewer packs to download), with a visible note. | FR-004 |
| 3 | Downloads on mobile data | A **"Download only on Wi-Fi" switch, on by default**; every download needs the user's confirmation with its size. | FR-011 |
| 4 | Input | **Typed, pasted or spoken** (voice added 2026-11-16); no camera, no automatic language detection. | FR-001 |
| 5 | Text limit | **5,000 characters.** | FR-006 |
| 6 | Platforms (owner, 2026-11-16) | **Android now, iPhone later**: the engine is the same code on both, and the phone's recognizer and voices are reached through ports. | FR-029 |
| 7 | Translation engine (owner, 2026-11-16) | **Bergamot on both platforms** (Firefox's offline translation, MPL 2.0), about 20–40 MB per direction; packs through Google Play on-demand delivery, later Apple-hosted assets. Each model's license is still checked file by file in the plan. | Clarifications 2026-11-16 |
| 8 | Speech recognition (owner, 2026-11-16) | **Each phone's own, on-device only** (Android 12+ now, the iPhone's later). Phones without one keep text; our own Whisper pack is a later spec. | FR-021 |
| 9 | Modes (owner, 2026-11-16) | **Type or speak** and **Conversation**. Listen (live captions) left for later. | FR-008 |
| 10 | Reading aloud | **The phone's offline voices only**; without one for the language, the tool says how to add it. | FR-024 |
| 11 | What is kept | **Nothing**: a conversation lives while the screen is open; Copy and Share send it as text; audio is never written. | FR-022, FR-027 |
| 12 | Still needs the phone | Whether your phone has an on-device recognizer and offline voices for the three languages: the plan's first task, on your phone. | Assumptions |
| 13 | Conversation's main action (plan, 2026-11-17) | **No filled action at the bottom**: the two large microphones inside the halves are the main actions, so ui-rules A3 gets an exception; Copy, Share and Clear stay outlined at the bottom edge. | contracts/ui.md |
| 14 | The engine's native code (plan) | Built once by a script and **committed as two `.so` files** (phones and the emulator, an estimated 10–20 MB in git), so normal builds and GitHub Actions never compile C++; 32-bit phones show the Translator as unavailable. | research R3 |
| 15 | A probe page (plan) | **Debug builds only**: Settings → About lists what your phone offers (recognizer, voices, engine speed), to close the research on your phone. Never in release builds. | quickstart §2 |
| 16 | Open-source licenses (plan) | A **Settings → About → Open-source licenses** page, which MPL 2.0 asks for the engine and the models. The audit of 2026-11-17 found what else must be there: the app's 264 libraries (mostly Apache 2.0, some BSD and MIT), generated at build time so the page never goes stale, and **Lucide's icons** (ISC; Feather's MIT), which the app's own line icons are drawn from. | research R4 |
| 17 | A new permission (build, 2026-11-17; listed here 2026-11-19) | **`FOREGROUND_SERVICE_DATA_SYNC`**, which Google Play's asset-delivery library adds: it unpacks a language pack the person chose to download in a foreground service, on only while unpacking. The plan expected no new permission. It's reviewed in `androidApp/permissions.allowlist`; at release, the Play Console asks for a declaration of the service type (spec 011). The other way is to drop Play's packs for our own download, which the constitution allows only in `core/content` too. | research R5, T076 |

### Spec 013: reminders

| # | Question | Default in the spec | Where to change |
|---|---|---|---|
| 1 | Where it lives | Its **own spec (013)**, tool in Utilities, code in the `tools-time` module. | FR-001 |
| 2 | Snooze | **5, 10, 15 or 30 min**, default 10. | FR-021 |
| 3 | One-off reminders missed while the phone was off | Shown at once if **less than 24 h late**; otherwise marked done silently. | FR-031 |
| 4 | Limits | **100 reminders, 200 characters** of text. | FR-004, FR-005 |
| 5 | After a one-off rings | Stays under **"Done"** until deleted or "Clear done". | FR-012 |
| 6 | Time zone change | The reminder **keeps its clock time**. | FR-030 |
| 7 | Sound | The channel's default; no custom sound. | FR-020 |
| 8 | Permissions | Notifications and exact alarms are asked when the **first reminder is saved**; the reminder is saved even if declined; without exact alarms it may be a few minutes late. | FR-040, FR-041 |
| 9 | **New install-time permission** | `RECEIVE_BOOT_COMPLETED`, which the app removes today, is added with the reason "Reminders are scheduled again after a restart". **Please review this one.** | FR-032 |

### Spec 010: data export and import (answered by the owner on 2026-11-02, built)

| # | Question | Decision | Where to change |
|---|---|---|---|
| 1 | Password | **Optional** at export (at least 6 characters, typed twice); no recovery. | FR-024–FR-026 |
| 2 | Import onto a phone with data | The user **chooses the parts**; each list part is **Replace or Merge**; settings are replaced. | FR-031, FR-036–FR-038 |
| 3 | Device-specific calibrations | Restored **only on the same phone model**. | FR-006 |
| 4 | Edit protection | Encrypted and authenticated with a secret built into the app: any changed byte is refused. It can't stop someone who takes the app apart; every part is validated too. | research R2–R5 |
| 5 | Password strength | PBKDF2-HMAC-SHA256, **600 000 iterations** (stored per file, can be raised later). | research R5 |

### Spec 014: financial calculations (answered by the owner on 2026-11-02, built)

| # | Question | Decision | Where to change |
|---|---|---|---|
| 1 | What the rate converter does | A new **Interest rate** category in the Unit converter: per month, two months, quarter, half year and year, as **equivalent compound rates** (12 % a year = 0.9489 % a month), like the reference calculator. | FR-001–FR-005 |
| 2 | Withdrawals that empty the savings | The compound-interest table **stops at zero**: the last withdrawal is what was left, and the results say how long the money lasts. | FR-016 |
| 3 | Currency | The **phone region's currency symbol** (R$ in Brazil) next to every amount; only a label, nothing is converted. | FR-009 |
| 4 | Files | The monthly table saves as **.ods** (amounts as currency cells, with the inputs, results and the note) or **.csv** (the table only, separators by language). | FR-022–FR-024 |
| 5 | The approximation note | On the screen in both modes, in the Help and in every .ods file. | FR-020 |

## Spec 003: remove ads purchase (owner's answers, 2026-11-04)

| # | Decision | Answer | Where |
|---|---|---|---|
| 1 | Price | USD 0.99 as the base price, Google's standard conversion for other currencies | FR-003 |
| 2 | Home offer | A small card when the person returns from an ad; "Not now" hides it for 7 days | FR-010 to FR-013 |
| 3 | Thank-you | A short dialog; Settings then shows "Ads removed" | FR-006 |
| 4 | Refund | Ads return only when the store confirms it; offline the saved purchase holds | FR-016, FR-017 |
| 5 | Until Google Play | A simulated store in debug builds; release builds report the store as unavailable | FR-021 to FR-024 |

**Release blocker**: a release build can't sell anything until spec 003 User Story 5 is done. Its steps are in
`specs/003-remove-ads-purchase/tasks.md`, Phase 9. The owner's part: the Play Console account (organization, D-U-N-S),
an upload to the internal track, and the one-time product `remove_ads` with a USD 0.99 base price and the console's
standard conversion.

## Spec 015: window sizes (decided for the owner, 2026-11-05)

The owner delegated these ("você pode tomar as decisões e só me passar depois de pronto"). Each one is a default the owner can change.
File names are under `core/designsystem/src/commonMain/kotlin/tech/onova/offlinetranslator/core/designsystem/` unless a path is given.

| # | Question | Implemented default | Where to change it |
|---|---|---|---|
| 1 | In a tiny window, does "todo o conteúdo disponível" mean reachable by scrolling, or all on one screen? | **Reachable.** Text and the sizes that aren't touch targets shrink, touch targets stay 48 dp, and what doesn't fit scrolls. **The alternative is whole-UI density**, for if the owner still dislikes the scrolling after trying it. Measured on 59 screens that fit without scrolling: at 240x360, today 7, text only x0.75 12, whole UI x0.6 37; at 220x300, today 2 plus 2 hangs, text only 6, whole UI 28. **As built, with scrolling: 22 at 240x360, 14 at 220x300, and 25 at split screen 411x400, against 18 today** (`specs/015-window-sizes/measurements/README.md`). The alternative's cost: touch targets of about 29 dp against the NON-NEGOTIABLE 48 dp in Principle X, so it needs an amendment; it must be re-provided in 15 dialog and sheet sites; and an on-device accessibility check fails while the repo's own audit can't see it. | Spec FR-014 to FR-016. Whole-UI density would mean a constitution amendment and a density override in `theme/PocketTheme.kt`; tables in `specs/015-window-sizes/measurements/spike-2026-11-05/` |
| 2 | What are the minimum and maximum of each text role? | Maximum is today's size. Minimums, as proposed before tuning: readout 40, readoutMedium 28, displaySmall 24, headlineMedium 20, headlineSmall 18, titleLarge 16, titleMedium 14, titleSmall 13, bodyLarge 13, bodyMedium 12, bodySmall 12, labelLarge 12, labelMedium 12, labelSmall 11 (sp). | `tokens/PocketTypography.kt` |
| 3 | At which window does shrinking start and end? | Starts below 360 wide or 600 tall, and is at the minimum at 240 wide or 320 tall. Side-by-side windows use only the height, 360 to 240. So 360x640 and any phone in landscape don't change. **The price is a jump at 560 wide.** The worst case is 559x359 against 560x359: readout 45 to 76 sp, bodyLarge 13 to 16 sp, `md` 9 to 16 dp, dialMin 79 to 120 dp. Tools on plain `ToolLayout` don't rearrange there, so 1 dp wider can make a screen that fitted scroll again. It's accepted to keep the 640x360 and 850x380 baselines. | `tokens/WindowFraction.kt` |
| 4 | Should ordinary text shrink in a split window of phone width? | **Yes.** 411x400 has a fraction of about 0.29, so bodyLarge goes from 16 to 14 sp and readout from 76 to 50 sp. The alternative is to keep body and label roles at today's size down to 320x256 (the reflow benchmark in WCAG) and shrink only big text there; that would be gentler to read and scroll more. | `tokens/WindowFraction.kt`, `tokens/PocketTypography.kt` |
| 5 | How small may text that shrinks itself go? | Never under 9 sp at the default text setting. At its 60% floor, `WholeWordText` reaches 7.2 sp from a 12 sp role today, and would reach 6.6 sp from an 11 sp one. | `components/WholeWordText.kt`, `components/KeypadKey.kt`, `components/Feedback.kt` |
| 6 | Which sizes shrink, and how far? | The list in spec FR-017, values chosen from the layout tables. Anything touched stopped at 48 dp, including `largeButtonHeight` and `favoriteCardHeight`, which the decisions listed as not touch targets (changed on 2026-11-06, see spec 018 row 4). What `ReservedLine` reserves doesn't shrink, because narrow windows make messages longer ("Nothing jumps", FR-034). | `tokens/PocketSizes.kt`, `tokens/PocketSpacing.kt` |
| 7 | How much of a scrolling area must stay visible? | One touch target, from the height a screen really has. When that can't be kept, the whole screen scrolls (FR-018). | `inspect` in `androidApp/src/test/kotlin/tech/onova/offlinetranslator/support/LayoutFit.kt` |
| 8 | Which windows fail the build? | The six small ones, pt-BR and Spanish at 240x360, 320x520 and 240x360 at 200%, and also every window the suite already tracked without failing (split screen, landscape, compact phones, larger text). The last group goes beyond what was first decided, because split screen is the case people will really meet. Spanish and 240x360 at 200% were added on review: the first decision said Portuguese has the longest words, but Spanish has the longer word more often (330 strings against 181), and 320x520 can't show the top bar at 200% in a tiny window. | `androidApp/src/test/kotlin/tech/onova/offlinetranslator/screens/LayoutFitTest.kt` |
| 9 | Does a resize recreate the activity? | No: `configChanges="screenSize\|smallestScreenSize\|screenLayout"`, without `orientation`, so rotation stays as tested. A window dragged across square still recreates once, and on a phone that's an ordinary split-screen drag (50:50 to 30%). Typed values and running timers survive it, as in a rotation. Adding `orientation` would avoid it, at the cost of changing what's tested for rotation. | `androidApp/src/main/AndroidManifest.xml` |
| 10 | Should the app declare a minimum window size? | No. Design for 240x320, no crash or clipping at 220x220. | `<layout>` in `androidApp/src/main/AndroidManifest.xml` |
| 11 | What does a compact phone do? | Phone size means 360x600 or larger. 360x592 loses 1 sp on its biggest number, and a 320x568 phone has a fraction of 0.67. They gain room they need. See also row 14. | `tokens/WindowFraction.kt` |
| 12 | What does the top bar's title do when it can't fit? | At most two lines beside the actions, shrinking to its floor. If that doesn't fit, it takes a row of its own under the actions, at the window's full width. At 240 wide at 200%, "Speedometer" is about 110 dp at the floor and the slot is under 96 dp, and fitting it there would need the size it has at 100%. The row costs height, and the content scrolls. The first decision said the screen scrolls, but the bar doesn't. | `components/ToolFrame.kt`, spec FR-020 |
| 13 | Does a larger text setting always give larger text? (As built: never smaller. Android's own curve keeps big text nearly flat, so a 76 sp reading holds at 130 % and grows at 200 %; body and label roles always grow.) | For every role's size, yes. Text that fits itself to a slot (a big reading, a key's label) is capped by the slot: never smaller at a larger setting, larger where the slot has room, and possibly equal where it doesn't. The first decision said "never equal", which a slot can't keep. The alternative is making those slots wrap or grow, which moves what "Nothing jumps" keeps still. | `components/Feedback.kt`, `components/KeypadKey.kt`, spec FR-009 |
| 14 | Should a raised Display size (One UI's screen zoom) count as a small window? | **Yes, for now.** The window is read in dp, and dp alone can't tell them apart, so "phones look the same" fails for these people. A 411 dp phone at its largest zoom or a 360 dp phone one step up reaches about 320 dp wide: at 320x700, bodyLarge 16 to 15 sp, readout 76 to 64 sp, `md` 16 to 13 dp. Two steps up on a 360 dp phone reach about 274 dp and bodyLarge 14 sp. Their text is still larger on the glass than at the default, but smaller than what they see today, and they usually zoomed to see better. To leave them untouched, compute the fraction at the phone's default density (`DENSITY_DEVICE_STABLE`); it's Android-only and not tried (research R18). | `tokens/WindowFraction.kt`, `theme/PocketTheme.kt` |
| 15 | What if the pop-up view scales the picture? | Only the phone can tell (`quickstart.md`, the Ruler with a real object). If it scales, the Ruler shows wrong millimetres with no warning, because it reads the display's real density. **The response, if it does:** show a notice on the Ruler when the window is scaled, in a spec of its own. Nothing is built now. | `feature/tools-basic/.../ruler/presentation/RulerContent.kt` |
| 16 | Are the counts of screens that fit a build gate? | No. The reachable rule fails the build. The counts are checked once, at the end: at 240x360 more than text-only's 12 of 59, and at 411x400 more than today's 18 of 59, leaving out the added dialogs. If one isn't met, another measuring round runs, and if that gains nothing the pull request says so first. Making them gates would make every new tool's screen a chore. | Spec SC-003, SC-009 |

| 17 | Must 240x360 at 200 % text pass the reachable rule? | **Not yet: it keeps a table.** 21 of its 69 rows break a long word in a narrow slot (labels, values beside labels, buttons), and fixing them means changing those screens one by one. 320x520 and 411x850 at 200 % do pass and fail the build. WCAG's reflow line is 320 wide. | `enforced = false` in `LayoutFitWindow240LargestTextTest` |
| 18 | The Data import review's rows are 24 dp tall, since spec 010, so their touch areas meet. Fix it? | **Fixed in spec 018.** At the time: **not in this spec**, because 48 dp rows change the phone's baselines, which spec 015 promises not to do. Worth its own small fix. | `PartRow` in `ImportScreen.kt`, and `TOUCH_AREAS_ALLOWED` in `LayoutFitTest` |
| 19 | Should what a mode adds stay above the action in a tiny window? | **Tracked, not enforced.** `ModeStabilityWindowTest` keeps its table at 240x360: Flashlight's and the Converter's bottom controls differ by mode, and the window can't hold the tallest case. | `strict` in `ModeStabilityWindowTest` |

| 20 | A camera's frozen picture on a rotation or a resize | **Not changed.** By reading, a frozen picture may be lost when the preview is rebuilt, on a rotation today and across square in a resize. It can't be reproduced off the phone. | `AndroidCameraPreviewRenderer.kt` |
| 21 | Two images at phone size changed. Is that all right? | **Yes, I kept them,** because each fixes a defect: the Monthly table's column titles now align with their numbers, and in the scientific Calculator lying down, Copy is no longer squashed, with the actions on two rows in both modes. Every other existing image is unchanged. | `WholeWordText` (`minWidth`); `CalculatorContent.kt`, `actionsRowWidth` |

**Known unknown, for the owner's phone:** whether Samsung's pop-up view really shrinks the app's configuration or only scales its
surface. If it scales, the owner sees a smaller picture of the phone layout, this spec changes nothing there, and the Ruler is wrong
by that scale (row 15). The check is in `specs/015-window-sizes/quickstart.md`.

**What no test here can show:** the layout tests run with no status, gesture or caption bar, and without the phone's own font.
`specs/015-window-sizes/quickstart.md` section 6 has the checks for those, each with a pass condition.

## Owner actions (not blocking development)

1. ~~**AdMob account**~~ (done 2026-10-22): app "Pocket Tools" (Android) and one **interstitial** ad
   unit; the IDs are in `~/.gradle/gradle.properties` as `polyTools.admob.appId` and
   `polyTools.admob.interstitialId`. Release builds fail until both are set.
2. ~~**Privacy & messaging**~~ (GDPR message published 2026-10-22; ads now wait for AdMob to
   approve the account). In AdMob: create **and publish** the GDPR (EEA/UK/CH) consent message,
   and, if you want it, the US state regulations message. **This is required even for users
   outside Europe**: without any published message, the consent library answers "Publisher
   misconfiguration: no form(s) configured for the input app ID", `canRequestAds()` stays false,
   and release builds never request an ad (seen on 2026-10-22; debug builds are unaffected because
   Google's sample app ID has messages). The message needs the privacy policy address,
   `https://privacy.onova.tech/polytools/` (site repo `onova-tech/privacy.onova.tech`; the old
   `/pocket-tools/` pages forward there, site PR #3 of 2026-11-17, which also brings the policy up to date).
   Play also requires the policy to be linked inside the app: done on 2026-11-16, a "Privacy policy" row in
   Settings → Privacy opens that address (owner's choice). **Merge site PR #3**, then use the address in the AdMob
   message and the Play listing too.
   `app-ads.txt` is published at `https://privacy.onova.tech/app-ads.txt` (site repo PR #2), so
   the Play listing's developer website must be `https://privacy.onova.tech` (spec 003/010).
   AdMob limits ad serving for unverified apps created after January 2025.
3. **Phone checks**:
   - spec 001 task T084, the manual pass including cold-start timing;
   - spec 002 quickstart §1–§5, with test ads and the debug EEA geography.

## Spec 016: tool shortcuts (decided for the owner, 2026-11-06)

The owner made it premium ("atalhos é uma função premium que libera no pagamento de $ 0,99") and delegated the rest. Each one is a default the owner can change.

| # | Question | Implemented default | Where to change it |
|---|---|---|---|
| 1 | Where does a person add one? | **A long press on a Home tile or favorite card, and a small icon to the left of the star in each tool** (owner, 2026-11-06). The icon replaced an action in the Help sheet, since two ways on one screen would be one too many. | `HomeScreen.kt`, `ShortcutMenu`; `ToolScreen.kt` |
| 2 | The long press opens a menu with one item. Is that strange? | **Kept as a menu,** so a second action can join it later, and so a long press never does something unasked. | `HomeScreen.kt` |
| 3 | Without the purchase, are the entry points hidden? | **Shown,** and a tap offers the purchase, since that's how a person finds out. Nothing is pinned by itself after buying: they tap again. | `ShortcutAccess.ask` |
| 4 | What does the shortcut open when the purchase is gone? | **A page that explains, with "Open Pocket Tools".** The launcher also greys it out. A session already open is left alone. Only a clear answer from the store counts. | `ToolSession.kt` |
| 5 | Does a pending payment unlock them? | **No,** as with ads: only a completed payment. | `RemoveAdsRules.shortcutOpens` |
| 6 | Do shortcut sessions count toward "recently used"? | **Yes,** because the person did use the tool. They don't count toward ads. | `ToolScreenViewModel` |
| 7 | The icon's colors | **The app's own icon colors, fixed:** white on teal, whatever the theme, because a home screen icon stays when the theme changes. | `ShortcutIcon.kt` |
| 8 | The purchase is still called "Remove ads" | **Kept,** because it's the main benefit and the store's product name waits for the Play Console. Its texts now say it also unlocks shortcuts. Consider a broader name when the account exists. | `feature/remove-ads` strings |
| 9 | A hidden task's tool stops | **Every tool, not only sound and light,** because a microphone left open behind the app is a privacy issue. Coming back restarts what restarts by itself, and a Tone or a Metronome stays stopped until tapped, as when leaving the app. | `TaskVisibility` |
| 10 | Two Pocket Tools windows side by side | **Known limit:** a tool created later in the other pane may follow the wrong pane's task, and stop when that one is hidden. It takes split screen with the app twice. | `TaskVisibility`, research R2 |

## Spec 017: Programmer calculator (decided for the owner, 2026-11-06)

"Adicionar Calculadora de Programador à ferramenta calculadora", delegated. Each one is a default the owner can change.

| # | Question | Implemented default | Where to change it |
|---|---|---|---|
| 1 | Where does it live, and is it free? | **A third mode on the Calculator's toggle, free** like everything inside a tool. Tapping now goes Basic, Scientific, Programmer and back, so Basic is one more tap away from Scientific. | `CalculatorContent.kt`, `DisplayActions` |
| 2 | How do numbers behave? | **Whole numbers in two's complement, wrapped to the word,** as a CPU does: 64 bits signed by default. In signed mode only Decimal shows a minus. | `ProgrammerMath.kt` |
| 3 | Operator order | **As in C:** NOT, then × ÷ MOD, + −, shifts and rotates, AND, XOR, OR. So `1 OR 2 AND 3` is 3, unlike a calculator that goes left to right. | `BitOperator.precedence` |
| 4 | A digit that doesn't fit | **Ignored, with no message,** as Windows' calculator does. | `ProgrammerRules.extended` |
| 5 | The bit grid has 4 bits per row on a phone | **So each stays 48 dp,** which means 64 bits scroll. Tablets get 8 per row. The BIN row shows all 64 at once anyway. | `ProgrammerContent.kt`, `BitGrid` |
| 6 | A separate history | **Its own list, shown in the same sheet,** because a word's result isn't a decimal and mixing them would mislead. | `HistorySheet.kt` |
| 8 | Six columns of keys, 48 dp wide on a 360 dp phone | **So the whole mode fits a phone without scrolling** (owner, 2026-11-06). The spare place holds a 00 key. Five columns gave wider keys, but the last row was cut on the owner's phone. | `ProgrammerKeypad.kt`, `SIX_COLUMNS` |
| 7 | Left out for now | **IEEE 754 floats, an ASCII view, NAND and NOR, and byte swap.** They're candidates for the tools review. NOT and AND together give NAND. | `specs/017-programmer-calculator/spec.md` |

## Spec 018: design cohesion (decided for the owner, 2026-11-06)

The owner approved the sample ("Ficou excelente"). Each one is a default the owner can change.

| # | Question | Implemented default | Where to change it |
|---|---|---|---|
| 1 | A status in the large line: "Light off", "Not level" | **Large too,** because at a glance that's the answer. | `FlashlightContent.kt`, `LevelContent.kt`, `PlumbContent.kt` |
| 2 | Which action is the main one when two look equal? | **The one used most:** Lock over Calibrate, Hold over Zero, Freeze over the light, Save .ods over .csv, Share over Copy. A lone Reset is outlined, because it isn't what the screen is for. | Each tool's `BottomAction` |
| 3 | Copy and Share before there's a result | **Not drawn, with their room kept,** so nothing moves when a result arrives. | `ScannerContent.kt`, `ColorPickerContent.kt` |
| 4 | How far do controls and text shrink in a reduced window? | **Controls to 36 dp, the owner's minimum, reached halfway to the smallest window; 48 dp with a larger text setting. Text to the new minimums** in `PocketTypography` (no role under 11 sp), because a window at phone text size still looked unchanged on the owner's phone. Text fields, the search field, mode selectors and icon buttons shrink too (owner, 2026-11-06, on the Financial calculator: the text got smaller and the boxes didn't). Tone's step buttons stay at Material's 40 dp. | `PocketSizes.touchTargetAt`, `CONTROLS_SMALLEST_AT`, `PocketTypography` |
| 5 | How little air between keys? | **`xxs`, down to 1 dp, where it's what lets the whole keypad fit.** Otherwise the gap stays. | `CalculatorContent.kt`, `ProgrammerContent.kt`, `PocketSpacing.XXS` |
| 6 | A short window: the large reading or room for the dial? | **Under 700 dp tall the reading uses the medium size,** so a landscape phone and a small phone keep their dial and controls. A little scrolling stays at a few sizes, listed in `specs/015-window-sizes/measurements/README.md`. | `PocketSizes.mainReadingRoomyHeight` |
| 7 | The tool's icon in Recents | **The tool's glyph in the app's color, on the launcher's white plate** (owner, 2026-11-07, on the phone). A launcher plates any bitmap, even one cut to its icon shape, and One UI ignores a drawable there and shows the app's icon. | `rememberRecentsIcon` in `components/ShortcutIcon.kt` |
| 8 | The rows of Notes and Reminders keep their trash icon? | **Yes.** It's one control, and swiping alone would break Principle X's "no gesture needed". World clock's three icons became one menu button, which also frees a row in a small window. | `components/ReorderableRow.kt` |
| 9 | BMI's notice on screen | **One line,** "For people aged 20 and over. Not a diagnosis.", with "not a medical device" and "talk to a professional" in Help, as the design review proposed. Heart rate's notice stays whole. If you want it back whole, it's one word. | `BmiContent.kt`, `bmi_disclaimer` |
| 10 | Stopwatch: the offer and the switch | **While the offer shows, the switch doesn't.** After the offer is answered, the switch stays. | `StopwatchTimerContent.kt` |
| 11 | Tone's steps of 10 Hz | **In More options;** − and + beside the field step by 1 Hz. The slider covers big moves. | `ToneContent.kt` |
| 12 | Import: how each list is brought in | **One Replace or Merge on top for every list,** and a one-word chip on a row to make it the exception. | `ImportScreen.kt` |

## Spec 019: creating QR codes (decided for the owner, 2026-11-07)

| # | Question | Implemented default | Where to change it |
|---|---|---|---|
| 1 | Keep created codes in the History? | **No,** and nothing typed is stored: a Wi-Fi password has no place on disk. Closing the tool forgets it. | `QrCreatorViewModel.kt` |
| 2 | Error correction | **Level M,** the usual balance between size and surviving a scratch. | `ZxingQrCodeEncoder.kt` |
| 3 | A link typed without a scheme | **Gets `https://`,** so phones open it as a link. | `QrPayload.link` |
| 4 | Wi-Fi's hidden network flag | **Left out,** to keep the form short. Few networks use it. | `QrPayload.wifi` |
| 5 | Contact's fields | **Name, phone and e-mail.** Company and address can come if someone asks. | `QrCreatorContent.kt` |

## Spec 020: lists, pinned notes and sharing (decided for the owner, 2026-11-07)

| # | Question | Implemented default | Where to change it |
|---|---|---|---|
| 1 | A checked item | **Goes to the end, struck through,** so what's left to do stays on top. | `Checklist.toggled` |
| 2 | Turning a list back into text | **Keeps the words and loses the checks,** as a plain text has none. No dialog asks, since the words are safe. | `Checklist.toText` |
| 3 | A note's actions | **One menu button on the row:** Pin, Share and Delete. Delete used to be one tap away and is now two, with Undo as before. | `NotesContent.kt` |
| 4 | Colors and labels | **Not now.** Pinning covers the most common need, and they'd need another selector on the list. | `docs/review-tools-2026-11-06.md` |

## Spec 021: Pedometer with a goal and history (decided for the owner, 2026-11-07)

| # | Question | Implemented default | Where to change it |
|---|---|---|---|
| 1 | **For you:** read the step sensor in the background, so every day is exact even if the tool isn't opened? | **Not yet.** Today a day counts until the tool's last reading, and steps counted while it's closed join the last day opened. Help says so. A background worker would fix it, but it'd be the app's first background task of its own, so it's your call. | `PedometerViewModel.kt` |
| 2 | The daily goal | **10,000,** the usual figure, in More options. | `StepLogRules.DEFAULT_GOAL` |
| 3 | Calories | **0.5 kcal per kg and km,** a common net estimate, marked as an estimate on screen. | `StepLogRules` |
| 4 | Step length and weight | **75 cm and 70 kg by default,** in More options. | `StepLogRules` |

## Spec 022: Interval timer (decided for the owner, 2026-11-07)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Where it lives | A third tab in Stopwatch & Timer, not a new tool, so timing stays in one place. | FR-001 |
| 2 | Cues with the app in the background | System alarms, each playing a short cue, with Notifications on. No foreground service, which needs a new special permission and a Play declaration. The cost: no ticks in the last seconds while away, a cue can be late without exact-alarm access, and an alarm icon can show in the status bar during a run. | FR-008 |
| 3 | Cue volume | The alarm volume, like the timers, so it sounds on silent. Sound and vibration can each be turned off in More options. | FR-006 |
| 4 | Pomodoro's long break | The last step can run once after the rounds, and the first once before them, rather than a separate list per section. | FR-002 |
| 5 | Screen during a run | Stays on. A person training or cooking looks at it from afar. | FR-007 |
| 6 | Deleting a sequence | Two taps through the row's menu, with no undo. | FR-002 |
| 7 | Left for later | Colors per step, spoken step names, and counting repetitions. | Spec, out of scope |

## Spec 023: Several counters (decided for the owner, 2026-11-07)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | How to choose a counter | A row on top that scrolls sideways, with the big reading and button staying where they were, not a list of small counters on one screen. Counting stays one big button away. | FR-001 |
| 2 | Volume keys | Off by default, in More options, since they stop changing the volume while the Counter shows. | FR-007 |
| 3 | Importing | Adds the counters the phone lacks, by name. A counter on both keeps the phone's count unless that's zero, as on a new phone. Before, a file replaced the one count. | FR-008 |
| 4 | Deleting | From the editor, with no undo, and never the last counter. | FR-006 |

## Spec 024: True north from the position (decided for the owner, 2026-11-07)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | The magnetic model | Android's own `GeomagneticField`, so the app carries no table. An iOS version will need its own. | FR-003 |
| 2 | Asking for location | Only when the switch is turned on, off by default. Until then the Compass needs no permission at all. | FR-001 |
| 3 | When it looks | Once each time the tool opens, for up to a minute. The declination changes slowly with distance, so it's not followed while the tool stays open. | FR-003 |
| 4 | The switch turned off | Back to magnetic north, not to the last declination. | FR-006 |

## Spec 025: Metronome with subdivisions, accents and a flash (decided for the owner, 2026-11-07)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Setting the accents | A tap on a beat's bar cycles accented, normal, silent, replacing the "Accent the first beat" switch. The bars wrap into rows so each stays a touch target, even in 12/8. | FR-002 |
| 2 | A silent beat | Keeps its subdivisions, the usual way to practice with gaps. | FR-002 |
| 3 | The flash | Off by default, in More options, tinting the reading's area, at most three times a second (WCAG 2.3.1), so fast tempos flash only some beats. | FR-004 |
| 4 | The subdivision's click | 800 Hz at half the beat's loudness. To be judged on your phone. | FR-001 |

## Spec 026: More instruments and tunings in the Tuner (decided for the owner, 2026-11-07)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | "Viola" | Both: the viola caipira, with Cebolão in D and in E and Rio Abaixo, and the orchestra's viola, called "Viola de arco" in Portuguese. | FR-001 |
| 2 | The cavaquinho | D G B D, the usual one, and D G B E. | FR-002 |
| 3 | The lowest pitch | 27 Hz, for a 5-string bass at A4 = 415 Hz. It doubles the detector's work, still a few milliseconds a reading. | FR-004 |
| 4 | Left for later | Typed tunings and a tone that plays each string. | Spec, out of scope |

## Spec 027: Percent & tip, and a Date calculator (decided for the owner, 2026-11-07)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Two tools or one | Two, in Utilities: what people look for is "porcentagem" or "gorjeta", and "calculadora de datas", not one tool for both. | Spec |
| 2 | The tip's default | 10%, Brazil's service charge, changed with − and +, and remembered. | FR-003 |
| 3 | How dates are entered | Typed, in the order chosen in Settings, like a reminder's, starting at today. A calendar to tap is left for later. | FR-007 |
| 4 | Working days | Monday to Friday, without holidays, which change by country and city. Help doesn't promise more. | FR-005 |

## Spec 028: A Flashlight that lights on open and turns off by itself (decided for the owner, 2026-11-07)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Lighting on open | **On by default**, in Torch mode only, so a shortcut gives light in one tap. It can be turned off in More options. | FR-001 |
| 2 | Turning off by itself | Off by default; 1, 5, 10 or 30 minutes. | FR-002 |

## Spec 029: Save a GPS place and go back to it (decided for the owner, 2026-11-07)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Where | Inside GPS info, as a second mode, not a new tool. | FR-001 |
| 2 | Going back | The distance and the bearing from true north, in words and degrees, for the Compass to follow. An arrow that turns with the phone is left for later. | FR-003 |
| 3 | The data file | Saved places go in it, since the person chose to keep them; a file is still only made when they export. | FR-005 |
| 4 | The promise | "Your position is never sent, and is stored only when you save a place." | FR-006 |
| 5 | The coordinate format | A one-word toggle beside Position and Places (DD, then DMS, then DDM), like the Calculator's angle unit, instead of its own row of segments, which made GPS info scroll in small windows. | FR-001 |
| 6 | The bottom | Copy, Share and "Save place" in one row, Save as the main action; Share was the main action before. | FR-001 |

## Spec 030: Write NFC tags (decided for the owner, 2026-11-07)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Where | A Write mode in the NFC tool, which is now called "NFC tags" ("Tags NFC", "Etiquetas NFC"); its id stays `nfc-reader`. | FR-001 |
| 2 | What | A text, a web link, a phone number or an email address, one record a tag. Wi-Fi and contacts are left for later. | FR-002 |
| 3 | A bare link | "example.com" is written as https://example.com; only web links are accepted. | FR-002 |
| 4 | After writing | Disarmed, so the next tag held is only read; a refusal (locked, too small, moved away) keeps waiting for another tag. | FR-004 |
| 5 | Locking | Off by default, in More options, and asked again in a dialog before each locked write. | FR-005 |
| 6 | A text's language | The app's language, in the record's language code. | FR-002 |

## Spec 031: Heart rate history (decided for the owner, 2026-11-08)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Where | A History page opened from a button beside Start, like the Financial calculator's monthly table. Modes on top made measuring scroll on small phones where it fit. | FR-001 |
| 2 | What is kept | Each result with a good or fair signal, by itself, with no Save to tap; a weak one never, since its rate may be far off. Up to 500, the oldest going first. | FR-002 |
| 3 | Switching it off | "Keep each result in History" in More options, on by default. | FR-003 |
| 4 | The reading | The average of the results kept, with the count, the lowest and the highest as rows. | FR-004 |
| 5 | The notice | "Not a medical device" opens History's page too, as its first row. | FR-005 |
| 6 | The data file | The results go in it, under Heart rate; merging adds the ones the phone lacks. | FR-006 |

## Spec 032: The Plumb bob joins the Bubble level (decided for the owner, 2026-11-08)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Where | A third mode, Plumb, after Surface and Bar. Home has one tool fewer. | FR-001 |
| 2 | Lock and Calibrate | Lock holds the lean, as it holds the level; Calibrate stays in its place, off, since a plumb line hangs by gravity. | FR-002 |
| 3 | Smoothing | The level's, one setting for the three modes; the Plumb bob's own is no longer used. | FR-003 |
| 4 | The old id | `plumb-bob` points at the Bubble level for good: favorites, recents and shortcuts to it open the level, once. | FR-004 |
| 5 | Old data files | The Plumb bob's part opens as "Plumb bob · No longer used; it will be skipped". | FR-005 |

## Spec 033: The Light meter's scale and exposure (decided for the owner, 2026-11-08)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | The scale | One step per power of ten, from 1 lux (a moonlit night) to 100,000 (direct sunlight), as the eye sees light; the band is the range since the reset. | FR-001 |
| 2 | The exposure | EV at ISO 100 with ISO 2720's incident constant of 250; the Help says a phone's sensor is a guide. | FR-003 |
| 3 | While waiting | The scale, the table with dashes and the empty graph, so the screen keeps its layout and doesn't look unfinished. | FR-004 |

## Spec 034: Password generator (decided for the owner, 2026-11-08)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Where | A tool of its own in Utilities, found by "password" and "senha". A fifth mode in Random didn't fit on top in small windows. | FR-001 |
| 2 | At first | 16 characters, every kind, look-alikes avoided: about 98 bits, "Strong". | FR-002 |
| 3 | Symbols | `!#$%&*+-=?@^_~`, without quotes, backslashes or spaces, which some sites refuse. | FR-002 |
| 4 | Strength | Under 50 bits weak, under 70 fair, under 100 strong, then very strong. | FR-003 |
| 5 | Never stored | Only the options are kept, and go in the data file; leaving the tool loses the password, so copy it first. | FR-005 |

## Spec 035: QR codes from a picture, and acting on a network or a contact (decided for the owner, 2026-11-08)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Does joining or adding break "never opened"? | **No:** that promise is about links, which still only show, copy and share. A network or a contact goes to the phone's own screen, where the person confirms, and the app keeps nothing. | FR-004 |
| 2 | Join on older phones | Only Android 11 and later, whose dialog needs no permission; before that, Show password and Copy, as now. | FR-002 |
| 3 | A contact's fields | The name, the first phone and the first e-mail, as the Create side writes them. | FR-003 |
| 4 | Where From a picture sits | Always at the bottom of Scan, under the reserved rows, so nothing moves. | FR-001 |

## Spec 036: UI rules that every screen keeps (confirmed by the owner, 2026-11-08)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Which side the main action takes in a row | **The right, last**: most rows already had it (Reset \| Start, Copy \| Generate, Cancel \| Import). Bubble level, Protractor, G-meter, Vibration meter and the Monthly table changed. | `docs/ui-rules.md` A2 |
| 2 | Where "More options" goes | **Above the actions**, as in every tool but the Bubble level and the Protractor, so opening it leaves the actions at the bottom edge. | A3 |
| 3 | The Metal detector's Set baseline | **At the bottom edge**, under the threshold slider. | A3 |
| 4 | A toggle on its own (GPS info's DD/DMS/DDM) | **As wide as its longest word**, like every toggle, so the Position/Places selector beside it no longer moves. | T2 |
| 5 | The QR creator's kinds while switching | Their fields keep the room of the tallest kind (a contact's three fields), so the kinds never move. | M1, FR-006 |
| 6 | The Counter's large Add one above Subtract one and Reset | **Kept** as an exception: the large button is the tool. The owner swapped the two below it: Reset on the left, Subtract one on the right. | Exceptions |

## Spec 037: More in the Unit converter (decided for the owner, 2026-11-08)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Clothing sizes, in the review's list | **Left out**: sizes differ by brand and country, so a table would mislead more than help. | Input |
| 2 | Currency and favourites | **Spec 038**, next: they keep the person's own rates and choices, which need a data-file part. | Input |
| 3 | Fuel consumption's first unit | **km/L** for the metric system, as Brazil counts it; mpg (US) for the imperial one. L/100 km is one tap away. | FR-002 |
| 4 | The cup | **240 mL** first (Brazil's xícara and the US label's cup), with the metric cup (250 mL) and the US customary cup (236.6 mL). | FR-007 |
| 5 | Ingredient weights | Grams in a cup of 240 mL from common kitchen tables: water 240, milk 247, wheat flour 120, sugar 200, brown sugar 215, butter 230, oil 220, uncooked rice 190, rolled oats 90, cocoa 85, honey 340, salt 290. Marked approximate on screen. | FR-007 |
| 6 | Where the ingredient is chosen | A row like the unit's, **above the value**, only in Cooking, so the value and its unit keep their place. | FR-011 |

## Spec 038: Currency at a typed rate (decided for the owner, 2026-11-08)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | The currency rates are typed against | The **phone region's currency** (R$ in Brazil), fixed by the first rate, so a rate keeps its meaning if the region changes. | FR-002 |
| 2 | The list of currencies | The **phone's own list**, named in the app's language; gold, silver and similar X codes left out. | FR-002 |
| 3 | Where Add currency goes | **In the list**, after the currencies, so the value and its unit keep their place between categories. | FR-005 |
| 4 | Limits of a rate | Above 0, at most **1,000,000,000**. | FR-007 |
| 5 | The day a rate was typed | **Shown** beside each rate, since nothing updates them. | FR-004 |

## Spec 039: Pinned units and favourite categories (decided for the owner, 2026-11-08)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | How to make a favourite | **Touch and hold**, the same for a result and a category, with a small star on what is held; holding again undoes it. No star button on every row, so the rows stay as clean as before. | FR-001, FR-002 |
| 2 | The order of favourites | **Catalog order**, not the order they were chosen, so each keeps its place. | FR-003 |

## Spec 040: Teams, dice from d2 to d100, numbers that don't repeat (decided for the owner, 2026-11-08; rows 2–5 after the owner's phone pass)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | How wide a range can avoid repeats | **10,000 numbers**: the round's record stays small; a wider range says so beside the switch. | FR-001 |
| 2 | Dice notation | **Shown, not typed**: "2d6" under the total. The bonus is gone (owner: "I think you can remove it"). | FR-002 |
| 3 | Dice offered | d2 to d100, **up to 10 dice**, each count a **picker** (owner: "to reduce screen space usage"). | FR-002 |
| 4 | Where teams live | **A fifth mode, Teams**, with its own list of names (owner: "separated from words"). Five modes are a chip row, as `docs/ui-rules.md` S1 asks: as segments they broke "Numb-er" at 200 % text. | FR-003 |
| 5 | How teams are split | **A number with a toggle**: "Split into 3 teams" or "3 per team", 2 to 20 either way; the sizes as even as the names allow, the larger first, so 10 people in teams of 3 make 3, 3, 2 and 2 (the owner's example). | FR-003 |

## Spec 041: "More options" only for two or more, and the QR history (decided for the owner, 2026-11-08)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | An option only sometimes of use | **Kept in "More options", disabled** (Reset calibration in Plumb, Delete all with no results), so the button doesn't appear and disappear as the screen changes. | FR-002 |
| 2 | A phone that can't vibrate | The Counter's volume keys switch **shows from the start**, the only option left. | FR-002 |
| 3 | Share in the QR history | **A visible button** beside Copy and Delete, as they already are, rather than a ⋮ menu. | FR-005 |

## Spec 042: the Bubble level's signal and two-reading calibration (decided for the owner, 2026-11-09)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | The signal's choices | **Off, Sound or Vibrate**, under "When level" in "More options"; **Off** until chosen, so the level never beeps unasked. Not both at once. | FR-001 |
| 2 | When it plays | **Once as the phone settles within 0.5°**, again only after it leaves by **more than 1°**. In Plumb it plays when the line hangs plumb. | FR-002 |
| 3 | The sound | The **short beep of the interval timer's countdown** (spec 022), at the alarm volume, so it is heard on silent. If it is too loud on the phone, a quieter channel can follow. | FR-002 |
| 4 | One reading or two | **Always two**: the one-reading way is gone. On a level surface the two readings agree anyway, and on any other they cancel its slope. | FR-003 |
| 5 | The turn | **"Turn it around in place"**, a half turn on the same spot, for Surface and Bar alike. "Calibrated." stays until the next calibration, reset or mode change. | FR-004 |

## Spec 043: the Speedometer's speed alert and windshield view (decided for the owner, 2026-11-09)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | How the limit is chosen | **A picker of round speeds** of the shown unit (20–200 km/h by tens), Off first; not typed. | FR-001 |
| 2 | How it alerts | **One beep and one vibration** as the speed goes over, again only after falling below 95 % of the limit; the speed turns red with "Over the limit" under it while over. No repeating alarm. | FR-002, FR-003 |
| 3 | Another unit | The limit moves to the **nearest round value** of the new unit (80 km/h → 50 mph). | FR-004 |
| 4 | Which way the windshield view flips | **Top to bottom**, so the phone lies on the dashboard as it would be read, top toward the glass. Many apps flip left to right and ask for the phone turned around; the reflection is the same. | FR-005 |
| 5 | What it shows | **The speed and its unit only**, at full brightness, red over the limit. | FR-005 |

## Spec 044: histories in the Notepad's rows (decided for the owner, 2026-11-09)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | When an NFC tag is kept | **Every tag read**, cards without readable data too (their identifier is all they show), the newest not repeated; 100 at most, like the QR history. Spec 009 kept nothing; the owner asked for the History. | FR-003 |
| 2 | What the NFC text holds | The tag, its technologies and NDEF facts, then each record's title and content; **binary data as hex, all of what was read** (up to 2,048 bytes a record, as on screen). Copy in the History sends the same text. | FR-004 |
| 3 | When a color is kept | **When it is held or copied**, the moments it mattered; never every frame of the camera. No Save button. | FR-005 |
| 4 | A color row | The square, then **HEX** as the title, the name and RGB, and when; Copy HEX, Copy RGB, Delete. | FR-005 |
| 5 | When rows were made | **Date and time** in the Settings formats; the QR history showed the time only. | FR-001 |
| 6 | The World clock's text | **"Tokyo: 21:30 (Tomorrow · +12 h)"** and the zone's name, as the row words it; the menu reads Copy, Share, Move up, Move down, Remove. | FR-006 |

## Spec 045: top selectors as scrolling segments (decided for the owner, 2026-11-09)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Segment widths when the labels don't fit in equal shares | **Each as wide as its own label**, with an equal part of the room left, before scrolling: two or four labels at 200 % text still fit in one row, where equal widths had to scroll and cut one at the edge. A label never shrinks to fit. | FR-001 |
| 2 | Which selectors | **Every chip row at the top of a screen**, Home's categories too. The Calculator's mode and angle toggles above its keys stay, as you chose them; chip rows inside screens (smoothing, subdivision) stay chips. | FR-002 |
| 3 | The tip | **At the bottom of the content, about ten seconds**, like a toast, with an (i); once per phone, not carried in data files. | FR-004 |
| 4 | The Pomodoro template | **Eight steps written out**: Focus, Break, three times, then Focus and the Long break. Four rounds with a last step once added a fourth short break before the long one (owner, 2026-11-09). Spec 022's decision 4 stays for sequences people make. | FR-006 |
| 5 | The "runs once" switches | **In sight under the steps**, with headings in the list only when a step runs once (none for a plain sequence, where every step runs in each round). | FR-007 |

## Spec 046: the Drag timer's 1/8 and 1/4 mile (decided for the owner, 2026-11-09)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Which distances | **1/8 and 1/4 mile**, named in miles in every language: the 1/4 mile is drag racing's distance, and the 201 m of the 1/8 is the common one at Brazilian tracks. "402 metros" finds it in Search. | FR-001 |
| 2 | How the distance is measured | **The GPS speed summed over time**, not the distance between positions: positions wander by metres from one fix to the next, the speed much less. | FR-002 |
| 3 | What a distance result gives | **The time and the speed at the line.** No 60 ft or 1/8-on-the-way splits: at one fix a second they would be guesses. | FR-002 |
| 4 | How many results | **100**, as the other histories keep (20 before). | FR-005 |
| 5 | Cancelling | **As a speed run**: a drop of more than 10 km/h, or 60 s. | FR-003 |

## Spec 047: a Pomodoro on a new phone (decided for the owner, 2026-11-09)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | When the Pomodoro is added | **The first time the Intervals tab opens**, not when the app is installed: it is made in the language the app has then, and nothing is written for someone who never opens it. | FR-001 |
| 2 | Phones that already have sequences | **Nothing added**; a phone updated with no sequences gets the Pomodoro once, like a new one. | FR-002 |
| 3 | After a data file is imported | **Not again**: the mark stays on the phone and isn't in data files. | FR-002 |

## Spec 048: the World clock's meeting planner (decided for the owner, 2026-11-09)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Where it lives | **A second mode, Clocks \| Meeting**, as other tools show their views at the top. | FR-001 |
| 2 | Working hours | **9:00 to 18:00 on weekdays**, early or late from 7:00 and until 22:00, night otherwise; Saturday and Sunday are the weekend everywhere (some countries rest on Friday and Saturday; a setting can come later). | FR-003 |
| 3 | Choosing the hour | **A day and an hour with − and +**, an hour at a time: tapping a cell would be too small a target. | FR-002 |
| 4 | The hours everyone shares | **In the user's time**, as ranges, above the places. | FR-005 |
| 5 | Kept or not | **Not kept**: it starts on the current hour each time; the cities are the World clock's. | FR-002 |

## Spec 049: the G-meter's separate axes (decided for the owner, 2026-11-09)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | How the axes are shown | **One at a time, chosen at the top** (Total, X, Y, Z), the reading, its peak and the graph following it; three lines on one graph would need colors to tell them apart. | FR-001 |
| 2 | An axis's peak | **The value furthest from zero, with its sign**, so a hard brake shows as negative. | FR-003 |
| 3 | The phone's axes | **As Android names them**: X across the screen, Y along it, Z out of it; the help says so. | FR-006 |

## Spec 050: the metal detector's sound (decided for the owner, 2026-11-09)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | What it sounds like | **The countdown's short tick on each pulse**, quickening as the change grows, not a tone that rises: the pulses already say how near, and the cues are already in the app. | FR-002 |
| 2 | How loud | **The alarm volume**, as the other cues: heard on silent. If it's too loud on the phone, a quieter channel can follow (as for the Bubble level, spec 042). | FR-003 |

## Spec 051: the screen light's warm and cold whites (decided for the owner, 2026-11-09)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | How "candle" and "sunset" are offered | **As names along White's temperature**, not as more swatches: a candle is 1900 K, a sunset about 2500 K, and the slider passes through both. | FR-002 |
| 2 | The default | **6600 K**, pure white, so nothing changes until the slider moves. | FR-001 |
| 3 | Warm white | **Kept as a swatch**, for a saved choice and for one tap to it. | FR-004 |

## Spec 052: the magnifier's reading filters (decided for the owner, 2026-11-09)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Which filters | **Normal, Contrast and Inverted**: gray with its contrast doubled, dark print on light, and the same turned over, light print on dark, which many who see little read better. Colors inverted as they are would keep the colors' low contrast. | FR-001 |
| 2 | Kept or not | **Kept**, in data files with the magnifier's settings: whoever needs a filter needs it every time. | FR-003 |

## Spec 054: the color picker's formats and contrast check (decided for the owner, 2026-11-10)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Copying four formats | **One Copy that opens a menu** of the four, rather than four buttons; the history's ⋮ offers the same. | FR-002 |
| 2 | Where the contrast check lives | **A fourth mode, Contrast**, its colors chosen from the kept ones plus black and white: a color is kept by Hold or Copy, so it can be compared. | FR-003, FR-004 |
| 3 | Which levels | **WCAG 2's AA for normal and large text and AAA for normal text**, the three a designer is asked for; the ratio cut, not rounded, to two decimals. | FR-003 |
| 4 | The saved palette | **The history of spec 044**, which keeps every held or copied color. | Input |

## Spec 055: messages in the theme's colors, favourites in marked order (decided for the owner, 2026-11-10)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Which colors a message wears | **The secondary container**, the color of a chosen segment, with the action ("Undo") in the primary color: dark in the dark theme, and it follows the phone's colors. | FR-001 |
| 2 | Only the Converter's tip, or every message | **Every message**: the Counter's, Notepad's and Reminders' Undo and the "Copied" ones were white bars in the dark theme too. | FR-002 |
| 3 | Favourites you already have | **They keep the order you saw, the catalog's**: the order you marked them in was never saved. To reorder, unmark and mark again. | FR-004 |
| 4 | Unmark and mark again | **It goes after the others**, as a new favourite. | FR-003 |

## Spec 056: the sound meter's log (decided for the owner, 2026-11-10)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | How often a reading is kept | **Once a second**, with that second's level and its highest reading: enough to see a night's or a workday's noise, and a file a spreadsheet opens at once. | FR-002 |
| 2 | With the app hidden | **The log pauses**, as the microphone does, and the times in the file show the gap. Listening in the background would need a notification and a permission the app doesn't ask for. | FR-003 |
| 3 | What happens on Stop | **A question, Save .csv or Discard, that stays until answered**: a touch outside doesn't lose an hour of readings. | FR-004 |
| 4 | Where a log lives | **In memory only**: it isn't kept between visits or in data files, and leaving the tool drops it. Up to 24 hours. | FR-006 |
| 5 | The file | **A .csv** with Time, Seconds, Level, Maximum and Unit, in the app's language and decimal mark, as Finance's tables. | FR-005 |
| 6 | The main action | **Start log** becomes the screen's filled action; Reset statistics stays beside it, outlined. | FR-001 |

## Spec 057: the tone generator's sweep, noise and two tones (decided for the owner, 2026-11-10)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Where the new sounds go | **Five modes in the one row at the top**: Tone, Two tones, Sweep, Noise, Dog whistle. The row scrolls sideways when they don't fit. | FR-001 |
| 2 | How loud two tones are | **Each at half**, so two together are never louder than one and never clip. | FR-002 |
| 3 | Which frequency the slider changes | **A choice right above it**: First or Second, From or To. One slider, one field, the same steps in every mode. | FR-002, FR-003 |
| 4 | How a sweep moves | **The same time in every octave** (20 Hz to 20 kHz in 10 s passes 632 Hz at 5 s), up or down, from 1 second to 10 minutes. | FR-003 |
| 5 | At a sweep's end | **Once** (it ends and says so) or **Repeat**. Changing an end, the time or the repetition stops a playing sweep, so what plays is always what the screen shows. | FR-004 |
| 6 | Which noises | **White, Pink and Brown**, chosen where the wave is. Noise has no frequency, so those controls are not shown. | FR-005 |
| 7 | A time limit | **Only the dog whistle's 60 seconds**, as before. The notice and the 30% start apply to every mode. | FR-006 |
| 8 | Where a sweep's time goes | **In the place of the −10 and +10 buttons**, above Start: Seconds with Once or Repeat beside it. A sweep has no steps of 10 Hz; its ends are dragged or stepped by 1 Hz. A row of its own would make the controls scroll on a phone. | FR-003 |

## Spec 058: the frequency of a vibration (decided for the owner, 2026-11-11)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Where the frequency goes | **A second view, Strength or Frequency, at the top.** Frequency's large reading is the hertz, with the rpm below and the spectrum where the trace is. | FR-001, FR-002 |
| 2 | How fast the sensor is read | **About 200 times a second, only in the Vibration meter**: the most Android gives without a permission, so it hears up to about 100 Hz (a motor at 6000 rpm). Faster needs HIGH_SAMPLING_RATE_SENSORS, a new permission in the store listing. | FR-006 |
| 3 | How long it listens | **The last 2.56 seconds** (512 readings), worked out twice a second: 0.39 Hz between the spectrum's steps, placed more finely between them. | FR-002 |
| 4 | When no frequency shows | **Under 2 Hz, under a swing of 0.02 m/s², or no frequency with a quarter of the power**: a hand, a floor or the sensor's noise never pass for a machine. | FR-003 |
| 5 | Turns a minute | **Always beside the hertz** (25.0 Hz, 1500 rpm), since machines are rated in rpm. | FR-002 |

## Spec 059: BMI body fat, basal metabolism, ideal weight and history (decided for the owner, 2026-11-11)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | The ideal weight | **The weight at a BMI of 22**, the middle of the healthy range shown above it, for any height and either sex. Devine's formula (50 or 45.5 kg + 2.3 kg an inch over 5 ft) gives odd results under 5 ft. | FR-003 |
| 2 | Body fat | **Deurenberg's formula from the BMI, age and sex**, named an estimate. The Navy method would add three tape measurements (neck, waist, hips). | FR-003 |
| 3 | Basal metabolism | **Mifflin-St Jeor**, in kcal a day, without activity levels. | FR-003 |
| 4 | Sex before it's chosen | **Female, shown beside the age**: a toggle always shows a choice, and the estimates wait for the age anyway. | FR-002 |
| 5 | The age's range | **20 to 120**, like the tool's notice ("For people aged 20 and over"). | FR-002 |
| 6 | Where Metric and Imperial go | **To the bottom, above the fields** (L4): the top now holds the tool's pages. | FR-001 |
| 7 | What History keeps | **The height and weight with the time**, up to 1,000 measurements; the BMI and category are worked out again when shown. | FR-004 |
| 8 | Where the estimates go | **A Body page between BMI and History**, with the age and sex: on the BMI page they pushed its scale off a phone's screen. | FR-001, FR-003 |

## Spec 060: finance scenarios side by side, extra payments and inflation (decided for the owner, 2026-11-11)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Where the new fields go | **Behind More options**: inflation and extra payment, the second disabled in Compound interest. The four fields each mode needs keep their place. | FR-001 |
| 2 | What an extra payment does | **Shortens the loan**: PRICE keeps its installment and SAC its amortization. Lowering the installment instead is the other choice banks offer. | FR-003 |
| 3 | What inflation shows | **One line in today's money**: the final total, or the total paid with each installment taken back to its month. No real interest rate line. | FR-002 |
| 4 | How two scenarios compare | **Keep the one typed, then change the fields**: each line shows Kept and Now side by side. One kept per mode, with the same inflation for both. | FR-004 |
| 5 | Comparing and the monthly table | **The table is the one typed now**; the kept scenario is only in the comparison. | FR-004 |

## Spec 061: reminder lists, steps and an earlier alarm (decided for the owner, 2026-11-11)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | What a list is | **A name on the reminder**, typed or picked from the lists in use. A list is gone when no reminder uses it; nothing to create or delete. | FR-001 |
| 2 | Where lists show | **At the top, All and each list**, only once one is in use; a new reminder starts in the list shown. | FR-002 |
| 3 | Where steps are ticked | **Right in the list**, under their reminder; the editor adds, types, ticks and removes them. Up to 20. | FR-003 |
| 4 | How early | **1 day, 3 days or 1 week before**, at the reminder's time, for one-off reminders only. | FR-004 |
| 5 | Done on an early notice | **Finishes the reminder**: it was done before its day. Snooze rings the early notice again. | FR-004 |

## Spec 062: save a note as a text file (decided for the owner, 2026-11-11)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Where Save as .txt goes | **In each note's ⋮ menu, beside Share**; the editor keeps Share and Done. | FR-001 |
| 2 | What the file holds | **The text as it is shared**: title, then text, or the list with ☐ and ☑. | FR-003 |
| 3 | The note widget (spec 063) | **Part of the "Remove ads" purchase, like tool shortcuts.** *Owner, 2026-11-11.* | spec 063 |

## Spec 063: the note widget (decided for the owner, 2026-11-11)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Free or paid | **Part of the "Remove ads" purchase.** *Owner, 2026-11-11.* | FR-003 |
| 2 | Which note | **The first in the Notepad's order**: the pinned one, or else the latest. No picker per widget. | FR-002 |
| 3 | A tap | **Opens the Notepad as a shortcut does**; without the purchase, the app's Home. | FR-003, FR-004 |
| 4 | Refreshing | **While the app runs, as notes, the purchase and the theme change, and on each return to the app**, and when the launcher asks; never on a timer, so it costs no battery. | FR-005 |
| 5 | Colours | **The app's theme**, not the phone's alone: a forced Light or Dark stays, and "Dynamic color" takes the wallpaper's colours, as the screens do. | FR-006 |

## Spec 064: the ruler past the screen (decided for the owner, 2026-11-11)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Where the new actions go | **A bottom row, History · Add stretch · Save** (A1–A3). The scale is about 1 cm shorter on a phone; stretches measure past it. The other way: the buttons in the reading's card, keeping the full length as L2 did for Calibrate. | FR-007 |
| 2 | After Add stretch | **The end marker comes back to the start marker**: slide the phone until the start marker is on the mark. | FR-001 |
| 3 | Taking a stretch back | **− on the stretch line** takes the last one away; leaving the tool or saving starts again. | FR-002 |
| 4 | A name | **Asked when saving, optional**, like a GPS place's but not required; Rename later from ⋮. | FR-004 |
| 5 | Where the list lives | **A History page over the ruler**, like the heart rate's, in the ruler's data part (calibration still kept). | FR-005, FR-006 |

## Spec 065: the protractor with the camera (decided for the owner, 2026-11-11)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Where it lives | **A third mode, Plane · Elevation · Camera**, not a tool of its own. | FR-001 |
| 2 | What is measured | **The angle at a corner between two sides**, three points dragged on the picture, 0 to 180°. | FR-002, FR-003 |
| 3 | Hold | **Freezes the picture**, so the points can be placed on a still one; they keep moving. | FR-004 |
| 4 | Zero and smoothing | **Not in the camera mode**, which reads no sensor: Reset points takes Zero's place. | FR-004, FR-005 |

## Spec 066: Sun and moon (decided for the owner, 2026-11-11)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Where the tool lives | **With the GPS tools**, in Utilities: it needs a position, and can use the saved places. | FR-001 |
| 2 | The place | **Here, from one fix, then the receiver rests**, or a saved place. No typed coordinates or city list for now: the app has none offline. | FR-004 |
| 3 | The Sun page's reading | **The daylight's length**; rise and set are the first rows, so a long time never crowds the reading. | FR-002 |
| 4 | Golden hour | **The sun below 6°**, the common photographers' rule. | FR-002 |
| 5 | Moonrise and moonset | **Not in this version**: the phase, its age and the next full and new moons. | FR-003 |
| 6 | Time zone | **The phone's**, also for a saved place far away; the Help says so. | FR-002 |

## Spec 067: Text counter (decided for the owner, 2026-11-11; merged into the Notepad by spec 069)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Where it lives | **With the Notepad**, in Utilities. | FR-001 |
| 2 | A paragraph | **A line with text on it**, as most counters count it on a phone. | FR-002 |
| 3 | Reading time | **200 words a minute**, rounded up. | FR-002 |
| 4 | Title Case | **Every word**, also short ones like "de" or "of", which differ by language. | FR-003 |
| 5 | The text | **Kept for next time and exported**, like a note. | FR-005 |

## Spec 068: Area and volume (decided for the owner, 2026-11-12)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | The shapes | **Rectangle, circle, triangle, trapezoid; box, cylinder, sphere, cone**: what a floor, a wall, a slab or a pot needs. | FR-002 |
| 2 | Round shapes | **By the diameter**, which a tape measures, not the radius. | FR-002 |
| 3 | The other units | **m² and ft²; litres, m³ and US gallons**, whatever the unit typed. | FR-003 |
| 4 | The lengths | **Not kept**, each shape its own while the tool is open; the page, shapes and unit are. | FR-005 |

## Specs 069–071: the owner's phone pass of 2026-11-12 (specifications only)

| # | Decision | Chosen in the spec | Where |
|---|---|---|---|
| 1 | Text counter | **Merged into the Notepad** (owner): counts under the text, a format menu beside the title; the tool goes. | spec 069 |
| 2 | What the format menu changes | **The text** (or a list's items), not the title, with Undo. | spec 069 FR-002 |
| 3 | "phrases" | **Sentences** in English, **Frases** in Portuguese and Spanish. | spec 069 FR-001 |
| 4 | The hint colour | **`primary` for text and outline**, not filled, so it doesn't look like the main action. | spec 070 FR-001 |
| 5 | Area & volume's unit | **A small ( mm ▾ ) menu button** right of Area \| Volume (owner). | spec 070 FR-003 |
| 6 | The finance graph | **The balance month by month**; two colours, Kept and Now, while comparing; y from zero. | spec 071 FR-002, FR-003 |
| 7 | What folds | **The inputs and More options**, animated; the actions stay. *Owner, 2026-11-12.* | spec 071 FR-004 |

## Spec 069 as built: the Notepad counts and formats (decided for the owner, 2026-11-12)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | A list's counts | **Each item counted on its own and added up**: an item is a sentence even without a full stop; the ticks and line breaks aren't counted. | FR-001 |
| 2 | Large numbers | **Grouped in the app's language** (1,234 or 1.234), as every other count. | FR-001 |
| 3 | Two changes of case in a row | **Undo offers the latest**, the text before that change. | FR-002 |
| 4 | A data file with the Text counter's text | **Opens, and skips it as "No longer used"**, like the Plumb bob's settings; the Notepad never had that text. | FR-003 |
| 5 | Search | **"word count", "characters" and "uppercase" find the Notepad**, in each language. | FR-003 |

## Spec 070 as built: the small option buttons (decided for the owner, 2026-11-12)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Which buttons | **Every `ToggleChoice`**, beside a field too (per year, Deposit, PRICE), not only the ones on top: one component, one look. | FR-001 |
| 2 | The ▾ | **A small filled triangle** after the unit, in the hint colour, as Android's own drop-down lists. | FR-003 |
| 3 | The unit's list | **Opens under the button**, the chosen unit bold, as a `PickerField`'s. | FR-003 |

## Spec 071 as built: the Financial calculator's graph and fold (decided for the owner, 2026-11-12)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Telling Kept from Now | **Kept dashed in tertiary, Now solid in primary**, with a legend: colour is never the only sign (Principle X). | FR-003 |
| 2 | The axes' labels | **The largest amount at the top; "Start" and the term ("2 years") along the bottom**. | FR-002 |
| 3 | A long term | **A point a month up to 30 years, then every n-th month and the last**, so 100 years draw as fast. | FR-002 |
| 4 | The graph's height | **The room left under the note, down to nothing**: on a phone shorter than 900 dp with the values open it is small, lines without labels, rather than make the screen scroll; folding the values gives it its full height. | FR-002, FR-004 |
| 5 | The fold | **Forgotten when the tool closes**: it opens with the values showing. | FR-004 |
| 6 | The fold in landscape | **No line**: the values sit beside the results there, so folding them gives the graph nothing. | FR-004 |

## Spec 072: the owner's phone pass of the 071 build (2026-11-13)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Comparing, the marks' values | **Both, Now's then Kept's, in their colours** (owner). | FR-006 |
| 2 | No room for the graph | **Not drawn until the values are folded** (owner). | FR-007 |
| 3 | The marks' months | **Exact quarters of the term**, written as the results write a duration ("1 ano e 6 meses"). | FR-006 |
| 4 | Past a line's end | **A loan paid off is R$ 0,00; savings show —**. | FR-006 |
| 5 | The counts' separator | **A thin vertical line**, not a typed "\|", so it lines up in every language. | FR-003 |
| 6 | "Capitalização de frase" | **Portuguese only**; English "Sentence case" and Spanish "Tipo oración" stay. | FR-001 |

## Spec 073: the Tuner's instruments in a list (2026-11-13)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | What the segment reads | **The instrument chosen**, "Violino ▾"; "Cordas ▾" while Chromatic is on (owner). | FR-002 |
| 2 | The list's order | **As the row had them**: Violão, Baixo, Ukulele, Cavaquinho, Viola caipira, Violino, Viola de arco, Violoncelo. | FR-002 |
| 3 | The dial | **Every string cell keeps a sharp's height**, so the strings row, and the dial, are the same for every instrument and tuning. | FR-001 |

## Spec 074: the owner's phone pass of the 073 build (2026-11-13)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | The marks in English | **"7 yr 6 mo"**; Portuguese and Spanish "7 a. 6 m." (owner's). | FR-001 |
| 2 | The volume's step | **5 %**, against 1 Hz for the frequency. | FR-005 |
| 3 | The sliders' names | **Beside them**, so the bottom keeps its height and the wave its room; above them in a small window or at large text. | FR-005 |
| 4 | Reminders after Not now | **Nothing until the next visit**, when the notice comes back while the permission is missing. | FR-004 |

## Spec 075: Home's categories (2026-11-13)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | The categories' order | **DIY, Measure, Location & navigation, Light & camera, Sound, Calculation, Time, Health, Utilities, Device**: the sensor tools first, then the everyday ones. | FR-007 |
| 2 | The names in English and Spanish | **Location & navigation / Ubicación y navegación, Calculation / Cálculo, Time / Tiempo, Health / Salud**; the QR tool **QR code & barcode / Código QR y de barras**. | FR-001 to FR-006 |
| 3 | Measure after the moves | **Keeps G-meter, Metal detector and Vibration** (three tools). | What stays |

## Spec 080: more languages in the Translator (owner, 2026-11-19)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Which languages | **All 54** that Mozilla's model registry has released both ways with English: the larger model where there is one, the small one ("Basic quality" on the page) otherwise. | FR-001, FR-006 |
| 2 | Norwegian | **`no` left out** as the same model as Norwegian Bokmål (`nb`): "Ok with the Norwegian choice". | research R1 |
| 3 | Where they're chosen | **"Other languages…"** at the end of every language list opens a full-screen page of check rows; **at least two** stay checked; English, Portuguese and Spanish at first. | FR-002 to FR-005 |
| 4 | Downloads | **Unchanged**: the Translator offers a language's pack when it's needed, with its size. | FR-009 |

## Spec 076: PolyTools (2026-11-13)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | The name's spelling | **PolyTools**, one word with a capital T, the same in every language. | FR-001 |
| 2 | The design system's `Pocket*` code names | **Kept**: never shown, and renaming them would rewrite nearly every screen. | What stays |
| 3 | The data files | **Same format and labels** (`PTBACKUP`, `.ptbackup`, the key labels), so the old app's files move to the new one; new exports are named `PolyTools-<date>.ptbackup`. | FR-003 |
| 4 | The Gradle keys | **Renamed** to `polyTools.*`; the owner renames the two in `~/.gradle/gradle.properties`. | FR-002 |
| 5 | The icon's details | Concept A redrawn so no two tools touch (thin shanks, the wide parts apart); a **pencil** instead of the screwdriver, which read as a bottle; the rivet's dot in the light teal (`#9CF1F0`), cut out in the themed icon. | FR-004 |

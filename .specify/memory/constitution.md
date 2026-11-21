# Offline Translator Constitution

> Adapted from PolyTools' constitution on 2026-11-20 (spec 081): Offline Translator is free, without ads or purchases,
> and open source under the GNU GPL 3.0. "Tool" below means the Translator, the app's one tool.

## Core Principles

### I. Offline-First Tools (NON-NEGOTIABLE)

Every tool MUST perform its function entirely on the device with no network connection
(airplane mode). Tools MUST NOT open network connections, directly or through a dependency.

- Feature modules (`feature/*`) and core modules (`core/*`, except `core/content`) MUST NOT depend
  on networking libraries (e.g. OkHttp, Ktor client, Retrofit), `java.net`/`javax.net` APIs,
  analytics/telemetry/crash SDKs, Firebase, ML Kit, or Google Play services location.
- Network access is permitted ONLY in `core/content`: on-demand language packs (rules below).
  No other module may depend on it except the app composition root; features reach it only
  through interfaces declared in common code.
- **On-demand content packs.** A tool whose offline data is too large to bundle (e.g.
  translation language models) MAY have that data downloaded once, under these rules:
  - a download starts only from an explicit user action that shows the pack's size, and it can
    be cancelled; installed packs can be reviewed and deleted from Settings;
  - packs contain data only (models, dictionaries), never executable code;
  - packs are delivered by the platform store (Google Play on-demand delivery; later
    Apple-hosted assets), never by a project-owned server;
  - once a pack is installed, the tool works fully offline; while a pack is missing, the tool
    explains what is needed and every part that does not need the pack keeps working.
- Enforcement is automated: a Gradle `network-guard` convention plugin fails the build on banned
  dependencies or forbidden module edges, and architecture tests (Konsist) fail on forbidden
  imports. Every tool's acceptance criteria MUST include an airplane-mode check (with its
  content packs installed, when it uses any).

Rationale: people trust a translator that never phones home. The only network use is the
language downloads the person explicitly asks for.

### II. Device-Side Only

The product MUST run entirely on the device. There is no backend, server, account system, or
cloud sync.

- User data (settings, favorites, calibrations, notes, usage counters, entitlement cache) MUST be
  stored locally (DataStore or Room) and MUST NOT leave the device automatically: no cloud
  backup, sync, or platform device-to-device transfer.
- The only way user data leaves the device is an explicit, user-started export to an archive
  file saved where the user chooses; importing that file restores the data on another device.
  Export and import open no network connection of their own. Direct phone-to-phone transfer may
  be added later by amendment.
- Quality monitoring MUST rely on platform-provided channels (Google Play Console Android vitals),
  not on embedded reporting SDKs.

Rationale: zero operating cost, zero data-breach surface, and full offline operation.

### III. SOLID & Clean Architecture

Code MUST follow SOLID principles within a layered Clean Architecture.

- Layers: `domain` (pure Kotlin, no framework or platform types) ← `data` ← `presentation`.
  Dependencies MUST point inward; the domain MUST NOT import Android, Compose, or SDK types.
- Single Responsibility: each class/function has one reason to change; ViewModels orchestrate,
  use cases decide, repositories persist.
- Open/Closed: tools are contributed to a `ToolRegistry` via DI; adding a tool MUST NOT require
  editing the catalog, navigation core, or other tools.
- Liskov & Interface Segregation: hardware capabilities are exposed through small, focused ports
  (e.g. `SensorSource`, `TorchController`, `AudioInput`) with substitutable fakes for tests.
- Dependency Inversion: high-level policy depends on abstractions; concrete platform
  implementations are bound only in DI modules. Constructor injection (Koin) is the default.

Rationale: a 40-tool app stays maintainable only if each tool is an isolated, replaceable unit.

### IV. Clean Code

All code MUST be readable by a newcomer without the author present.

- Intention-revealing names; small functions doing one thing; no magic numbers (use named
  constants or value classes with units); no dead or commented-out code.
- Comments explain *why*, never restate *what*. Public domain APIs carry KDoc when the intent is
  not obvious from the signature.
- Formatting and static analysis (ktlint + detekt) MUST pass with zero warnings; suppressions
  require an inline justification.
- Prefer immutability, explicit types at module boundaries, and sealed types for UI state.

Rationale: consistency lowers the cost of every future feature and of the iOS port.

### V. Test-First Domain Logic

Domain logic MUST be developed test-first.

- Every use case and every calculation (ad policy, daily counters, unit conversions, sensor math,
  calibrations) MUST have unit tests written before or alongside the implementation, using fakes
  for clock, storage, and hardware ports.
- Time-dependent logic MUST take an injected clock/time zone; tests MUST NOT depend on wall time.
- Domain modules MUST maintain ≥ 90% line coverage (Kover); the build fails below the threshold.
- Critical user flows (catalog navigation, ad gating, purchase/restore) MUST have Compose UI or
  instrumented tests.

Rationale: sensor readings and money-related rules are hard to verify by eye; tests are the spec.

### VI. Free, Without Monetization

The app MUST be free of charge and MUST NOT contain ads, purchases, subscriptions, donation
prompts inside the app, or any SDK that serves them. Its code is published under the GNU GPL 3.0.

- No feature may be held back, limited, or delayed to encourage payment.
- No account, sign-in, or contact detail may be asked for.

Rationale: a good translator that anyone can use and study, as the owner's contribution to an app
ecosystem with fewer of them (owner, 2026-11-20).

### VII. Modern & Localized UI

The UI MUST feel current, consistent, and native to the user's language.

- Material 3 (Expressive where available), dynamic color with a branded fallback, light and dark
  themes, edge-to-edge, predictive back, and adaptive layouts for phones, foldables, and tablets.
- Accessibility requirements are defined in Principle X and apply to every screen.
- Every user-visible string MUST come from resources and ship in English (`en`), Portuguese
  (Brazil) (`pt-BR`), and Spanish (`es`). Hard-coded user-visible strings are build-breaking
  defects. Units and number formats MUST follow the user's locale and settings.
- **Design system only.** All visual decisions live in `core/designsystem`: tokens for color
  roles, typography, shape, spacing, sizing, elevation, and motion, plus shared components
  (including the common `ToolScaffold` every tool uses). Feature modules MUST NOT hard-code
  colors, dp/sp values, fonts, shapes, or animation specs; a new visual need is first added to
  the design system, then used. Static analysis (detekt/Konsist rules) fails the build on raw
  visual values outside `core/designsystem`.

Rationale: a beautiful, consistent UI is the second core differentiator.

### VIII. Multiplatform-Ready

Code MUST be written so iOS can be added without rewriting shared logic.

- Kotlin Multiplatform + Compose Multiplatform; code lives in `commonMain` by default.
- Platform-specific code lives only in platform source sets (`androidMain`, later `iosMain`) as
  implementations of interfaces declared in `commonMain`. `expect/actual` is reserved for thin
  platform primitives.
- Platform services (content packs, speech, voices, the microphone) MUST be
  consumed only through common interfaces so an iOS implementation is a drop-in addition.

Rationale: Android ships first; iOS is a planned evolution, not a rewrite.

### IX. Privacy & Least Permission

The app MUST request the minimum permissions, and only from the tool that needs them.

- No runtime permission (camera, microphone, location, physical activity, notifications, etc.)
  may be requested at app launch, on Home, or in Settings. A permission is requested only when
  the user opens a tool that needs it, and only the permissions that tool needs.
- Every tool that needs a permission MUST follow the same flow:
  1. Before the system dialog, the tool shows an in-app explanation of why it needs the
     permission, with an "Allow" action.
  2. If granted, the tool starts immediately.
  3. If denied, the tool explains what is missing and offers to ask again; no other tool or
     screen is affected.
  4. If the system no longer shows the dialog (permanently denied), the tool offers an
     "Open settings" action that leads to the app's permission page.
- Permissions that serve an optional enhancement (e.g. notifications for a background timer
  alert) are requested only when the user turns that enhancement on.
- Location is foreground-only (while the tool is open); background location MUST NOT be
  requested.
- Permission-free APIs MUST be preferred when they exist (e.g. the flashlight uses the torch API,
  which needs no camera permission).
- The ad consent form (Principle VI) is not a runtime permission; it is exempt from these rules
  and follows the consent provider's standard timing.
- A tool whose required sensor/hardware is absent MUST be shown as unavailable with a clear
  explanation instead of crashing or showing fake data.
- No permission may be declared that no shipped tool uses.
- Health-adjacent tools (e.g. heart rate) MUST display a "not a medical device" disclaimer.

Rationale: a translator hears what people say; trust depends on visible, minimal access.

### X. Accessible from Day One (NON-NEGOTIABLE)

Accessibility is a requirement of every feature, starting with the first spec. It is never
deferred to a later spec or a final audit.

- Every interactive element has an accessibility label and semantic role, a ≥ 48dp touch
  target, and a logical focus order; screens expose headings for navigation. In a reduced window,
  smaller than phone size, at the default text setting, a touch target MAY shrink to no less than
  36dp, so the window can show its content without scrolling; with a larger text setting it stays
  at 48dp.
- Text scales up to 200% without clipping, overlap, or loss of function; layouts reflow instead
  of truncating essential information.
- Contrast meets WCAG 2.2 AA (4.5:1 for text, 3:1 for icons and UI components) in light and dark
  themes; color is never the only carrier of meaning.
- Live readings (angles, decibels, speed, etc.) are available to screen readers through
  throttled announcements or an on-demand "read value" action. Tools whose main output is visual
  offer a non-visual cue where it makes sense (e.g. a haptic or sound when level).
- Motion respects the system "remove animations" setting. Nothing flashes more than 3 times per
  second unless the user explicitly starts it (e.g. a strobe) after a warning.
- Every feature is verified with automated accessibility checks in Compose UI tests, screenshot
  tests at maximum font scale in light and dark themes, and a manual TalkBack pass of each new
  screen.

Rationale: a translator matters most to people who rely on assistive technology, and building
accessibility in costs far less than retrofitting it.

## Technology & Platform Constraints

- **Language & UI**: Kotlin, Kotlin Multiplatform, Compose Multiplatform with Material 3.
- **Shared libraries**: kotlinx-coroutines, kotlinx-datetime, Koin (DI), AndroidX DataStore
  (KMP), Room (KMP) when relational storage is needed.
- **Android**: application ID `tech.onova.offlinetranslator`; minSdk 26; targetSdk/compileSdk = the
  latest level required by Google Play at release time.
- **Approved network-capable dependencies** (only in the modules allowed by Principle I):
  Google Play on-demand delivery libraries (`content`). Adding any other
  network-capable dependency requires a constitution amendment.
- **Offline alternatives are mandatory**: translation uses on-device models from content packs
  (not ML Kit or online APIs); speech uses the phone's on-device recognizer and voices only.
- **Versions** are centralized in `gradle/libs.versions.toml` and pinned to stable releases;
  each `/speckit-plan` verifies they are current.
- **Builds** for iOS require macOS + Xcode and are out of scope until the iOS-enablement spec.

## Development Workflow & Quality Gates

- **Spec-Driven Development** with GitHub spec-kit. Every feature follows:
  `/speckit-specify` → `/speckit-clarify` → `/speckit-plan` → (`/speckit-checklist`) →
  `/speckit-tasks` → `/speckit-analyze` → `/speckit-implement` → `/speckit-converge` (repeat until
  Converged). The ordered feature list lives in `docs/roadmap.md`.
- **Branches**: one feature branch per spec (`NNN-short-name`, created by the git extension).
  Every change reaches `main` only through a reviewed pull request on GitHub; direct pushes to
  `main` are not allowed. A feature PR is merged only after converge reports Converged and all
  gates pass.
- **Constitution Check**: every `plan.md` MUST pass the Constitution Check gate before Phase 0
  research and again after design; violations MUST be listed in Complexity Tracking with
  justification or the plan is rejected.
- **Definition of Done** for any feature:
  1. `./gradlew check` passes (unit tests, Kover thresholds, ktlint, detekt, network-guard,
     architecture tests).
  2. New strings exist in `en`, `pt-BR`, and `es`.
  3. Each new tool verified on a physical Android device, including in airplane mode.
  4. Accessibility (Principle X): automated checks and maximum-font-scale screenshots pass, and
     every new screen had a manual TalkBack pass.
- **Commits**: small, focused, Conventional Commits style (`feat:`, `fix:`, `docs:`, `chore:`,
  `test:`, `refactor:`).

## Governance

This constitution supersedes all other practices, guides, and conventions in this repository.

- **Amendments** are made only through `/speckit-constitution`, documented in the Sync Impact
  Report of the commit, and reviewed against existing specs for migration impact.
- **Versioning** follows semantic versioning: MAJOR for removing or redefining a principle,
  MINOR for adding a principle/section or materially expanding guidance, PATCH for
  clarifications and wording.
- **Compliance review**: `/speckit-plan` (Constitution Check) and `/speckit-analyze` MUST verify
  compliance for every feature; reviewers MUST reject changes that violate a principle without a
  recorded, approved justification.
- **Runtime guidance**: `docs/roadmap.md` (feature order and scope) and `CLAUDE.md` (agent
  context, once created) complement but never override this document.

**Version**: 3.0.0 | **Ratified**: 2026-10-20 | **Last Amended**: 2026-11-20 (Offline Translator, spec 081)

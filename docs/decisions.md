# Decisions

The owner's decisions for the Translator, carried over from PolyTools on 2026-11-20, and those of the standalone app
(spec 081, `specs/081-standalone-app/spec.md`).

## Spec 012: offline translator (voice and Conversation added 2026-11-16)

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


## Spec 080: more languages in the Translator (owner, 2026-11-19)

| # | Decision | Chosen | Where |
|---|---|---|---|
| 1 | Which languages | **All 54** that Mozilla's model registry has released both ways with English: the larger model where there is one, the small one ("Basic quality" on the page) otherwise. | FR-001, FR-006 |
| 2 | Norwegian | **`no` left out** as the same model as Norwegian Bokmål (`nb`): "Ok with the Norwegian choice". | research R1 |
| 3 | Where they're chosen | **"Other languages…"** at the end of every language list opens a full-screen page of check rows; **at least two** stay checked; English, Portuguese and Spanish at first. | FR-002 to FR-005 |
| 4 | Downloads | **Unchanged**: the Translator offers a language's pack when it's needed, with its size. | FR-009 |

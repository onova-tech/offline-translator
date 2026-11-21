# Contract: the translator's screens (spec 012)

Each screen follows `docs/ui-rules.md`; the rule numbers are the rulebook's. Strings in English, Portuguese (Brazil)
and Spanish. The host draws back, title, favorite, shortcut and help (spec 001).

## Top: the mode (S1)

`SegmentedChoice`: **Type or speak | Conversation**. The languages keep their place and size between the modes
(M1–M5, `ReservedSlot`).

## Type or speak (stories 1, 2, 4)

Regions (`AdaptiveToolLayout`, L1–L5):

- **Middle, the only part that scrolls**: the translation, large, selectable; under it a `ShortNote` when needed:
  "Translated through English", "Same language: shown as typed", "Only the first 5,000 characters were translated".
  While nothing is typed: `NO_READING` with a caption ("Type, paste or speak a text"). While a pack is missing: the
  **pack offer** (below) in place of the translation.
- **Bottom, at the bottom edge**, in order (A1–A4):
  1. The languages: `From ( Português ▾ )  ⇄  To ( English ▾ )`: two `MenuChoice` fields with the swap between them;
     each list shows installed languages first, then those to download with their size.
  2. The text: a `PocketTextField`, a few lines tall, with the counter (`1,234 / 5,000`, error colour past the limit)
     and the **microphone** as its trailing action. While listening the button shows it (and says "Listening" to
     TalkBack); the words appear in the field as they're heard.
  3. Actions (`BottomActionRow`, `compact = true`): **Copy** and **Share** outlined, **Read aloud** filled and last
     (A3). Read aloud reads as Stop while speaking. Without a translation, they are disabled.
- The microphone permission's `PermissionGate` card appears above the text field the first time the microphone is
  tapped (research R7); "Not now" closes it.

## Conversation (story 5)

A new layout for two people facing each other across the phone. New design-system piece **`FacingHalves`**: the
screen split in two equal halves, the far one turned 180° (a switch in More options turns it upright for someone
beside you). Proposed rule for `docs/ui-rules.md`, added in this spec's pull request: **C1 – Conversation: two
facing halves, each with its own language, its turns and its microphone; nothing else inside the halves.**

Each half, from its own speaker's point of view:

- Top: its language (`MenuChoice`).
- Middle, scrolling: the turns as bubbles. Its own turns show what was heard (small) and the translation is shown on
  the **other** half (large); a turn being heard shows its partial text; the newest stays in view.
- Bottom: one large microphone button (at least 72 dp, `PocketSizes`), labelled with the language ("Speak
  Português"). Disabled while the phone speaks (FR-025). A tap on one of its own turns opens it for correction.
  While it listens (owner, 2026-11-19): the turn reads "Ouvindo." "Ouvindo.." "Ouvindo..." until the first word, and a
  smaller round button with a square beside the microphone, "Stop and discard", stops and drops what was being heard,
  untranslated. The far half's language list is turned to its reader.

Below both halves, at the bottom edge (A1): `MoreControls` (turn the far half, read aloud on or off) and the actions
**Copy**, **Share** outlined and **Clear** (asks first). No main filled action: the microphones are the main actions,
inside the halves; this is listed under the rulebook's Exceptions with the reason.

Missing pieces show upright in the **near half**, one at a time, for either language (owner, 2026-11-19: the phone's
own download dialog is upright, so a turned notice beside it looked wrong): the pack offer first, then "Recognizing
Español isn't on this phone yet" with **Get this language** (near half's language first), "The phone is downloading
Español…" while it does, then "No offline voice for Español" with **Voice settings**, and last "This phone can't
recognize Español offline". When one is done the next comes; the phone is asked again every few seconds while a
language is on its way, and when the screen comes back, so nothing needs the tool reopened.

## The pack offer (story 2)

A `Notice` in the content area: "To translate Português, download its language pack (48 MB)." with **Not now** and
**Download** (main). Then:

- Downloading: a progress bar with "23 of 48 MB" and **Cancel**.
- Waiting for Wi-Fi: "Connect to Wi-Fi to download" with **Network settings**; Settings has the switch.
- No space: "Needs 75 MB free" with **Storage settings**.
- Failed: what happened with **Try again**.
- No store (sideloaded): "Language packs can't be downloaded on this phone." with no button.

When the pack is installed, the translation the person wanted appears without another tap (story 2, scenario 7).

## Settings → Offline languages (story 3)

A section wired into Settings by `AppNavHost`, like Remove ads (feature modules don't depend on each other):

- `SwitchRow` **Download only on Wi-Fi** (on by default).
- One row per language: name, size, state ("Installed · 75 MB", "48 MB to download", "Downloading 23 of 48 MB").
  Actions behind ⋮ (`RowMenu`, A6): **Download** or **Delete** (asks first) or **Cancel**.
- A line with the total space used.

## Settings → About → Open-source licenses (research R4)

A `SettingsRow` opening a page that looks like the rest of Settings (owner, 2026-11-17: "consistent and looks
good"), with the host's back and title:

- A short line on top: "PolyTools is built with these open-source projects. Thank you."
- One row per project, grouped by author where it helps (AndroidX, JetBrains, Google, Mozilla…), sorted by name:
  the name, then the license as supporting text ("Apache License 2.0"). A tap opens the license's full text in a
  bottom sheet with the project's source link, which opens in the browser like the Privacy policy row (our own
  curated links, unlike a scanned QR code's, which the app never opens).
- Search isn't needed for about 60 projects (artifacts of one project are one row).
- The list comes from AboutLibraries' build-time resource plus `config/libraries/` (Lucide, Feather, Bergamot, the
  models), so it never goes stale (research R4).

## Help (X1–X4)

`ToolHelp` blocks of at most 180 characters: what each mode does; that it works offline once a pack is in; that
voice depends on the phone's own speech and voices; that it's for travel and everyday talk, not for medical or legal
text. Never that nothing is kept or sent: the first-launch notice says that once for every tool (spec 078, rule X5,
`PrivacySaidOnceTest`).

## Accessibility (FR-050, Principle X)

- A finished translation is announced once (polite live region), never while typing or speaking.
- Every control has a label; the microphones say their language and state; the far half's content is read in the
  normal reading order, not upside down.
- 200% text, light and dark, landscape (FR-051): `LayoutFitTest` covers both modes.

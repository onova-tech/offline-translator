# Feature Specification: More languages in the Translator, chosen by the person

**Feature Branch**: `080-translator-languages`

**Created**: 2026-11-19

**Status**: Draft

**Input**: Owner, 2026-11-19, during the phone pass of spec 012: "On the languages selectors, I want to add an Other
Languages... option. When the user clicks it, it goes to a pop up screen with all languages listed, and the user can
select the ones it would like to appear. He must let at least 2 options checked. Then the download of the language
packs stays as normal, appearing the message to download on the translator tool itself."

This extends spec 012 (the offline Translator), which offers English, Portuguese and Spanish and says more languages
come as new packs without changing the tool (spec 012 FR-002).

## Clarifications

### Session 2026-11-19

- Q: Which languages does the list offer? → A: **All the languages Mozilla's translation models cover in both
  directions with English** (owner, choosing "All 54"): 54 languages besides English. About 31 have the same model
  quality as today's Portuguese and Spanish; about 23 have only a smaller model of lower quality, which the list marks.
- Q: Does a bigger release package fit Google Play? → A: Yes (checked 2026-11-19): up to 100 language packs per app,
  each up to 1.5 GB, and up to 4 GB in total for packs downloaded on demand; 54 packs come to about 2.2 GB. A pack is
  only downloaded when a person chooses its language.
- Q: Does the download change? → A: No (owner): a language not on the phone shows its size in the lists, and the
  Translator offers its download when it's needed, as today.

### Session 2026-11-20 (the owner's phone pass)

- Q: The ×'s of the speech popups were too big, sometimes over the download button. What instead? → A (owner): **a
  screen to manage the languages**, install and uninstall; the popups lead to it, and "Other languages…" becomes
  "Manage languages…", opening it.
- Q: What does the screen manage for each language? → A: **its translation pack, the phone's speech recognition and
  its offline voice**, each with its state and action (the last two through the phone's own speech service and voice
  app, as the popups did).
- Q: Does choosing which languages appear in the lists stay on its own page? → A: **No, the same screen**: each
  language has its check and its install state.
- Q: What do the Conversation's speech popups keep? → A: **"Manage languages"**, which opens the screen at that
  language, **and a smaller ×** that never covers a button (the dismissal rules of 2026-11-20 stay).
- Q: And Settings → Offline languages? → A: **It opens the same screen** (with the space used and the Wi-Fi switch).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Choose which languages appear (Priority: P1)

A person who travels to Japan opens a language list, taps "Other languages…", checks Japanese on a full-screen list of
every language, and goes back: Japanese is now in both language lists of the Translator, with its download size.

**Why this priority**: It is the owner's request and the only way to reach the new languages; without it the tool
stays at three languages.

**Independent Test**: In Type or speak, open the From list, tap "Other languages…", check Japanese and French, uncheck
Spanish, go back: both lists show English, Portuguese, Japanese and French (and no Spanish), in both modes, also after
the app is closed and opened again.

**Acceptance Scenarios**:

1. **Given** any language list of the Translator (From and To in Type or speak, each half's list in Conversation),
   **Then** its last entry is "Other languages…", and a tap opens the language page.
2. **Given** the language page, **Then** it lists every language the Translator offers, each with its name in the
   app's language and in its own language, sorted by name, each with a check box; the languages that appear in the
   lists are checked.
3. **Given** the language page, **When** the person types in its search field, **Then** only the languages whose name
   (in the app's language or in its own) contains the text are listed.
4. **Given** only two languages are checked, **Then** neither can be unchecked, and the page says that at least two
   are needed.
5. **Given** a language with only the smaller model, **Then** its row says "Basic quality".
6. **Given** the person goes back from the page, **Then** every language list of the Translator shows exactly the
   checked languages: those on the phone first, then the others with their download size.
7. **Given** a language chosen in a list (or for a Conversation half) is unchecked, **Then** that list takes another
   checked language, the first by name that isn't the other side's.
8. **Given** the app is closed and opened again, **Then** the checked languages are the same; they also travel with
   the Translator's settings in a data file (spec 010).

---

### User Story 2 - Translate in the new languages (Priority: P2)

The traveler chooses Portuguese to Japanese: the Translator offers Japanese's pack with its size, downloads it after
the person confirms, and from then on translates Portuguese to Japanese offline, through English.

**Why this priority**: Choosing a language is only useful if it then translates; the download works as today, so
this story mostly proves the new packs.

**Independent Test**: With Japanese checked and its pack installed, in airplane mode, translate "Onde fica a estação
de trem?" from Portuguese to Japanese and back; both translations are understandable, and the note says "Translated
through English".

**Acceptance Scenarios**:

1. **Given** a checked language whose pack isn't on the phone, **When** it is chosen, **Then** the Translator offers
   its pack with its name and size, and downloads nothing until the person confirms (as in spec 012 story 2).
2. **Given** the pack is installed, **Then** the language translates offline to and from English, and to and from
   every other installed language through English.
3. **Given** a translation in or from a basic-quality language, **Then** the short note under it says so.
4. **Given** a language written right to left (Arabic, Persian, Hebrew, Urdu, Uyghur), **Then** the text field, the
   translation and the Conversation's turns show it right to left.
5. **Given** a new language, **Then** speaking and reading aloud work where the phone has its own recognizer and an
   offline voice for it, and say what is missing otherwise (as in spec 012 stories 4 and 5).

---

### User Story 3 - See and free the space of every language (Priority: P3)

A person who tried several languages opens Settings → Offline languages to free space: it lists the checked
languages and every language pack still on the phone, even of languages no longer checked.

**Why this priority**: Space must stay under the person's control (spec 012 story 3); unchecking a language mustn't
hide a pack that still takes space.

**Independent Test**: Check Japanese, download its pack, then uncheck Japanese: Settings → Offline languages still
lists Japanese with its size, and Delete frees the space and removes the row.

**Acceptance Scenarios**:

1. **Given** Settings → Offline languages, **Then** it lists each checked language and each installed pack, each with
   its size and state, and the total space used.
2. **Given** an installed pack of a language no longer checked, **When** it is deleted, **Then** its row goes.

---

### User Story 4 - Manage every language in one place (Priority: P1, owner's phone pass 2026-11-20)

A traveler opens "Manage languages…" from a language list, a speech popup or Settings, and sees each of their
languages with what the phone has for it: the translation pack, the phone's speech recognition and its offline voice,
each installed or with what gets it; and below, every other language to add.

**Why this priority**: The popups each fixed one thing and couldn't say what was missing elsewhere; a language the
phone can't recognize had no place to say so for good. One screen shows and fixes all of it.

**Independent Test**: From the To list tap "Manage languages…": English, Portuguese and Spanish are on top with their
three parts; check Japanese below: it joins them with its pack's size, its speech and its voice; download its pack;
delete Spanish's pack (after a confirmation); uncheck Spanish: it leaves the top once its pack is deleted.

**Acceptance Scenarios**:

1. **Given** any language list, **Then** its last entry is "Manage languages…" and opens the screen.
2. **Given** the screen, **Then** "Your languages" lists every checked language and every language whose pack is on the
   phone, each with its check and three parts: Translation (on the phone with its size and Delete, or its size to
   download and Download, or its download's progress and Cancel; English: built in), Speech (ready; the phone can get
   it, Get; being downloaded; the phone didn't install it, Update; not on this phone) and Voice (ready; Get a voice;
   no voice engine); "Other languages" lists the rest with their check, by name.
3. **Given** a search, **Then** both parts show only the languages whose name (in the app's language or its own)
   contains the text.
4. **Given** a Conversation popup about speech or a voice, **Then** it has "Manage languages", which opens the screen
   at that language, and a small × that never covers it; Type or speak's speech messages have "Manage languages" too.
5. **Given** Settings → Offline languages, **Then** it shows the Wi-Fi switch and "Manage languages" with the space the
   packs use, which opens the screen.
6. **Given** a pack deleted, a language checked or unchecked, or a part got outside the app, **Then** the screen
   shows it at once or when it comes back to the front.

### Edge Cases

- **Unchecking a language whose pack is installed**: the pack stays on the phone until it is deleted in Settings;
  checking the language again needs no download.
- **English unchecked**: allowed while two others stay checked; pairs without English still translate through English
  behind the scenes, and English needs no pack.
- **A Conversation open while a half's language is unchecked**: the half takes another checked language, and its turns
  stay.
- **The same language on both sides** after a change: shown unchanged with the hint, as today (spec 012).
- **A data file from an older app** (without checked languages): English, Portuguese and Spanish are checked. **A file
  naming a language this app doesn't offer**: that language is ignored; if fewer than two remain, the defaults apply.
- **Search with no match**: the page says no language matches and keeps the checked ones unchanged.
- **Scripts the phone can't draw**: languages are shown with the phone's own fonts; a phone missing a script shows
  what it can (no fonts are bundled).
- **A language the phone's recognizer or voices don't know**: voice says what is missing (spec 012 FR-021, FR-024).
- **Two languages with the same name in a script** (Chinese in simplified and in traditional characters): each is its
  own row, named so they differ ("Chinese (Simplified)", "Chinese (Traditional)").
- **Without Google Play** (a sideloaded install): the page works; downloads say they aren't possible, as today.
- **Large text and TalkBack**: each row reads its name, its quality when basic, and whether it's checked; the page
  works at 200% text and in landscape.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Every language list of the Translator (both lists in Type or speak, each half's in Conversation) MUST end
  with "Other languages…", which opens the language page.
- **FR-002**: The language page MUST list English and every language Mozilla's translation models cover in both
  directions with English (54 on 2026-11-19), each with its name in the app's language and in its own language, sorted
  by the name in the app's language.
- **FR-003**: The page MUST have a search field that filters the list by either name.
- **FR-004**: Each row MUST have a check box; the checked languages MUST be exactly those every language list shows,
  installed ones first, then the others with their download size.
- **FR-005**: At least two languages MUST stay checked: the last two can't be unchecked, and the page MUST say why.
- **FR-006**: A language whose models are the smaller kind (in either direction) MUST be marked "Basic quality" on the
  page and in the short note under its translations.
- **FR-007**: The checked languages MUST be remembered, and MUST travel with the Translator's settings in a data file
  (spec 010); a file without them, or naming fewer than two languages this app offers, MUST give English, Portuguese
  and Spanish.
- **FR-008**: When a chosen language is unchecked, its list (or Conversation half) MUST take the first checked
  language by name that isn't the other side's.
- **FR-009**: Each language MUST have its own pack holding both directions with English, downloaded only after the
  person confirms its name and size, from the Translator's offer or from Settings, as in spec 012 (FR-010 to FR-013).
- **FR-010**: Settings → Offline languages MUST list the checked languages and every installed pack, with sizes,
  states and the total space used; Delete MUST work for an installed pack of a language no longer checked.
- **FR-011**: Right-to-left languages MUST be shown right to left in the text field, the translation and the turns.
- **FR-012**: Speaking and reading aloud MUST follow spec 012 for every language: the phone's own recognizer and offline
  voices only, in a region chosen for the language, with the same messages when one is missing.
- **FR-013**: The app MUST stay within Google Play's limits for language packs (at most 100 packs, each at most
  1.5 GB, at most 4 GB on demand in total).
- **FR-014**: Every new string MUST exist in English, Portuguese (Brazil) and Spanish; the models' license (MPL 2.0)
  MUST stay credited on the Open-source licenses page.
- **FR-015**: Tests on a device MUST be able to install only the language packs they need, so a test phone or the
  emulator never needs all of them.
- **FR-016** (2026-11-20): One screen, "Manage languages", MUST hold the checklist of FR-002 to FR-005 and, for each
  checked language or language whose pack is on the phone, its translation pack, speech recognition and voice with
  their states and actions (story 4); it replaces Settings' list of packs, which opens it.
- **FR-017** (2026-11-20): The speech popups MUST lead to that screen at their language; the Conversation's MUST keep a
  small × that never covers their buttons.

### Key Entities

- **Offered language**: a language code (with a script where it matters, such as Chinese in traditional characters),
  its pack, its model quality (full or basic) and its speech region.
- **Checked languages**: the set the person chose to see in the lists; part of the Translator's settings; at least two.
- **Language pack**: as in spec 012, one per language, both directions with English, with its size and state.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A person can open the language page, find a language by typing part of its name and check it in under
  15 seconds.
- **SC-002**: Every offered language's pack loads and translates a test sentence in both directions with English in
  the automated engine check.
- **SC-003**: In 100% of the automated checks, two checked languages can't become fewer.
- **SC-004**: The app's own download (without language packs) grows by at most 0.5 MB; each language pack is downloaded
  only by those who choose it.
- **SC-005**: The language page passes the automated accessibility checks, at 200% text, in light and dark, and in
  landscape.
- **SC-006**: The owner judges translations understandable for the sample sentences in the languages they try on
  their phone (as spec 012 SC-002).

## Assumptions

- The languages are those Mozilla's model registry has released in both directions with English on 2026-11-19, with
  the larger ("base-memory") model where one is released and the smaller ("tiny") one otherwise; a language is basic
  quality when either direction uses the smaller model. A later registry release can add languages without changing
  the screens.
- One pack per language, like today's Portuguese and Spanish; English needs none.
- English, Portuguese and Spanish are checked at first, so the Translator looks the same until the person changes it.
- Language names come in the app's language and in their own language from the phone, not from strings written for
  this app.
- Each language gets a default speech region (for example French in France, Chinese in mainland China); the phone's
  own region wins where it matches, as spec 012 does today.
- Real downloads still need the Play Console account (spec 012); until then packs are tested with local tooling, a few
  at a time.
- No automatic language detection, camera input or new modes: those stay future work (spec 012).

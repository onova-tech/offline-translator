# Feature Specification: Offline Translator, by text and by voice

**Feature Branch**: `012-offline-translator`

**Created**: 2026-10-23 (text); 2026-11-16 (voice and conversation)

**Status**: Draft. The engines are chosen (see Clarifications, session 2026-11-16); the plan starts with the research
tasks in Assumptions, run on the owner's phone.

**Input**: User description (2026-10-23): "Add a Translator tool that works entirely on the phone. The user picks a
source and a target language, types or pastes text, and gets the translation, with copy and share. Language data is not
bundled with the app: the first time a language is needed, the app shows its download size and asks the user to
download it. Downloads can be cancelled, and installed languages can be reviewed and deleted in Settings to free space.
Once a language is downloaded, translation works fully offline. Initial languages: English, Portuguese, and Spanish,
with more added later."

Owner, 2026-11-16: "I want to evolve the translator and add the simultaneous translation tool (with speech recognition
and generation). Is it possible to add this on a local app?" Yes, sentence by sentence, as below.

## Clarifications

### Session 2026-10-23

The owner asked for work to continue without stopping and for open questions to be collected for
later review. These answers are the recommended defaults; each one is listed in
`docs/decisions-pending.md` (spec 012 section) so the owner can confirm or change it.

- Q: Which languages are offered first? → A: English, Portuguese and Spanish. More are added later as
  new language packs, without changing the tool (FR-002).
- Q: How is a translation between two non-English languages done (Portuguese to Spanish)? → A: Through
  English: the text is translated into English and then into the target language, so only
  English-to-X and X-to-English packs exist and the user downloads fewer packs. The tool shows a
  line "Translated through English" for such pairs (FR-004).
- Q: May a download use mobile data? → A: A "Download only on Wi-Fi" switch in Settings is on by default.
  With it on and no Wi-Fi, the tool says "Connect to Wi-Fi to download"; with it off, the download is
  still only started after the user confirms the size (FR-011, FR-012).
- Q: Does the tool take text only? → A: ~~Yes: typed or pasted text.~~ Replaced on 2026-11-16: voice is in
  (session below). Camera input stays out of scope.
- Q: Is the source language detected automatically? → A: No. The user picks it, and a swap button
  exchanges source and target. Detection can be added later (Assumptions).
- Q: How long may the text be? → A: Up to 5,000 characters, with a counter (FR-006).
- Q: Does the translator send the text anywhere? → A: Never. Only the language packs are downloaded,
  by an explicit user action; the text stays on the phone (FR-003, FR-013).

### Session 2026-11-16

The owner asked for voice and simultaneous translation, chose the answers below, and the privacy policy address of
the app (`https://privacy.onova.tech/polytools/`, a Settings row outside this spec). Each answer is in
`docs/decisions-pending.md`.

- Q: And the iPhone? → A: **Everything here must also work on iPhone later** (owner). The translation engine is the
  same code on both, and the phone's recognizer and voices are reached through ports in common code, with an Android
  implementation now and an iOS one later.
- Q: Which engine translates? → A: **Bergamot on both platforms** (owner): the engine and models behind Firefox's
  offline translation (MPL 2.0, allowed in an app with ads). Its C++ core builds for Android and iOS, so both give the
  same results from the same packs, about 20–40 MB per direction; pairs without English go through English (FR-004).
  Apple's own translator on iPhone and larger models (1–3 GB) were set aside.
- Q: Where does speech recognition come from? → A: **Each phone's own recognizer, in its on-device mode only**
  (owner): Android's (Android 12 and later) now, the iPhone's on-device Speech later. Nothing extra to download from
  us, and words show while the person speaks. A phone without one, or without the language installed, keeps every
  text feature and says why voice is missing (FR-021). Our own speech pack (Whisper, 150–500 MB) for those phones is a
  later spec.
- Q: What voice reads the translation? → A: **The phone's own text-to-speech** (Android's now, the iPhone's later),
  using only voices that need no network. Without an offline voice for the language, the tool shows the translation
  and says how to add a voice (FR-024).
- Q: How simultaneous is it? → A: **Sentence by sentence.** A translation appears 1 to 3 seconds after each sentence
  ends; there is no word-by-word interpreting while someone is still speaking.
- Q: Which modes? → A: **Type or speak** (story 4) and **Conversation** (story 5) (owner). The modes are the tool's
  top choice (`SegmentedChoice`, ui-rules S1). **Listen**, live captions of one speaker, was left for later.
- Q: In Conversation, who is speaking? → A: Each person has their own half and their own microphone button: a tap
  starts their turn, and a pause or a second tap ends it. No automatic detection of the language.
- Q: Is anything kept? → A: A conversation lasts while the screen is open; Copy and Share send it as text. Nothing is
  stored, and audio is never written anywhere (FR-022).

## User Scenarios & Testing *(mandatory)*

Five stories make up the feature. The first is useful on its own once a language pack is present;
the second gets the packs; the third manages them; the last two add the voice. The tool uses the tool frame from spec
001, counts toward the daily ad rule of spec 002, follows `docs/ui-rules.md`, and **translates entirely offline**; the
only network use in the whole app outside ads is the user-approved download of a language pack.

### User Story 1 - Translate a text (Priority: P1)

A traveler types or pastes a sentence in Portuguese and reads it in Spanish at once, even in
airplane mode. They can swap the languages, copy the result or share it.

**Why this priority**: The purpose of the tool.

**Independent Test**: With English, Portuguese and Spanish packs installed and airplane mode on,
translate "Onde fica a estação de trem?" from Portuguese to English and to Spanish: both results
are understandable and appear within 2 seconds on the owner's phone.

**Acceptance Scenarios**:

1. **Given** the Translator with the needed packs installed, **When** the user picks a source and a
   target language and types or pastes text, **Then** the translation appears below, updated after a
   short pause in typing (about half a second) and not on every key.
2. **Given** a translation, **Then** Copy puts it on the clipboard (with a confirmation) and Share
   opens the system share sheet with the translation.
3. **Given** the swap button, **When** it is tapped, **Then** the source and target languages exchange
   and the current translation becomes the input text.
4. **Given** a pair of non-English languages, **Then** the translation goes through English and a line
   says "Translated through English".
5. **Given** the same source and target language, **Then** the text is shown unchanged with a hint.
6. **Given** more than 5,000 characters, **Then** the counter turns to an error color, the extra text
   is not translated and a message says so.
7. **Given** a translation is running, **Then** a progress indicator shows, the screen stays
   responsive, and a new input cancels the old translation.
8. **Given** the app is closed and reopened, **Then** the chosen languages and mode are remembered; the text
   is not stored.

---

### User Story 2 - Download a language (Priority: P2)

The first time a language is needed, the tool shows what must be downloaded and how big it is, and
asks. The user can cancel at any time.

**Why this priority**: Without packs there is nothing to translate; this story makes the first use
possible and keeps the app small.

**Independent Test**: On a fresh install, choose English to Portuguese: the tool offers the pack
with its size; tap Download on Wi-Fi: a progress bar runs; Cancel stops it and leaves nothing
half-installed; download again and translate with airplane mode on.

**Acceptance Scenarios**:

1. **Given** the user picks a pair whose pack is not installed, **Then** the tool shows the name of
   the pack, its size in MB and "Download" and "Not now", and nothing is downloaded until the user
   confirms.
2. **Given** a download, **Then** a progress bar shows the percentage and the downloaded size, and
   Cancel stops it and removes partial data.
3. **Given** the "Download only on Wi-Fi" switch is on and the phone is not on Wi-Fi, **Then** the tool
   says "Connect to Wi-Fi to download" and offers Open network settings; the user can turn the
   switch off in Settings.
4. **Given** the phone has too little free space for the pack, **Then** a message says how much is
   needed and offers Open storage settings; nothing is started.
5. **Given** a download fails or is interrupted, **Then** the tool says so with Try again; a partial
   pack is never used.
6. **Given** a pair that goes through English and one of the two packs is installed, **Then** only the
   missing pack is offered.
7. **Given** a completed download, **Then** the tool continues with the translation the user wanted
   and works fully offline from then on.

---

### User Story 3 - Manage the languages (Priority: P3)

A person wants to free space or see what is installed. Settings lists the language packs with their
sizes and lets them delete or add one.

**Why this priority**: Required by the privacy and storage promises but not needed for the first
translation.

**Independent Test**: Open Settings → Offline languages: the installed packs and the total space
used are listed; delete one and the space is freed; the Translator then offers to download it again.

**Acceptance Scenarios**:

1. **Given** Settings, **Then** an "Offline languages" section lists each available pack with its
   size and state (installed, not installed, downloading) and the total space used.
2. **Given** an installed pack, **When** the user taps Delete and confirms, **Then** it is removed and the
   space is freed.
3. **Given** a pack that is not installed, **When** the user taps Download and confirms the size, **Then**
   it downloads as in story 2.
4. **Given** the "Download only on Wi-Fi" switch in this section, **Then** the choice is remembered.
5. **Given** a pack is in use by an open translation, **Then** deleting it first cancels that
   translation.

---

### User Story 4 - Speak instead of typing, and hear the result (Priority: P4)

At a pharmacy counter, the traveler taps the microphone, says the sentence in Portuguese, and the phone shows it in
Spanish and reads it aloud for the pharmacist.

**Why this priority**: The smallest step into voice, on the screen of story 1, and the base Conversation reuses.

**Independent Test**: In airplane mode, on the owner's phone, with the packs and the phone's offline speech for
Portuguese installed: tap the microphone, say "Onde fica a estação de trem?", and see the words while speaking, the
Spanish translation within 2 seconds of the end, and hear it after tapping Read aloud.

**Acceptance Scenarios**:

1. **Given** the microphone button beside the input, **When** it is tapped the first time, **Then** the microphone
   permission is asked through the app's permission flow (explain, ask, ask again, open settings).
2. **Given** the phone is listening, **Then** the button shows it, the words appear in the input as they are
   recognized, and a pause of about two seconds or a second tap ends the listening.
3. **Given** the input was spoken, **Then** it is translated like typed text (story 1) and can be corrected by typing.
4. **Given** a translation, **When** Read aloud is tapped, **Then** the phone reads it in the target language, and a
   second tap stops it.
5. **Given** the phone has no on-device recognizer, or not for the source language, **Then** the microphone button
   explains what is missing (and, where the phone offers it, opens the phone's own speech settings); typing works.
6. **Given** the target language has no offline voice, **Then** Read aloud explains it and offers the phone's
   text-to-speech settings; the translation still shows.

---

### User Story 5 - A conversation between two people (Priority: P5)

Two people without a common language sit face to face, the phone between them. Each speaks in turn; the phone shows
and says what the other said, in their own language.

**Why this priority**: The "simultaneous translation" the owner asked for, in the form that works best offline: turns.

**Independent Test**: In airplane mode, Portuguese and Spanish: the owner taps the Portuguese half, says a sentence,
and within 3 seconds the Spanish half shows the translation and the phone says it in Spanish; then the Spanish half
answers and the Portuguese half shows and says it. Copy gives both sides as text.

**Acceptance Scenarios**:

1. **Given** Conversation, **Then** the screen has two halves, one per language, each with its own microphone button
   and its latest turns; the far half can be turned to face the other person.
2. **Given** one person's turn, **When** they finish a sentence, **Then** the other half shows the translation and the
   phone says it in that language; the speaker's half keeps what was recognized.
3. **Given** the phone is speaking, **Then** neither microphone listens, so the phone never translates its own voice.
4. **Given** a turn was misheard, **Then** a tap on it shows its text for correction, and the translation follows the
   corrected text.
5. **Given** the conversation, **Then** Copy and Share send it as text, one turn per line with its language, and
   Clear starts again; leaving the screen ends it, and nothing is kept.
6. **Given** a missing pack, recognizer or voice for either language, **Then** the near half says what is missing,
   upright and one thing at a time, as in stories 2 and 4; once the phone has it, the next one shows without leaving
   the tool (owner, 2026-11-19).

---

### Edge Cases

- **No network and no pack**: the tool says what is missing and that a download needs a connection;
  nothing else is affected.
- **Google Play not available** on the phone (a sideloaded install): the tool says packs cannot be
  downloaded on this device and offers no download button.
- **Pack removed by the system** or damaged: the tool treats it as not installed and offers it again.
- **Very short or empty text**: no translation is attempted; the result area shows a hint.
- **Wrong source language** (English text with Portuguese chosen): the result may be nonsense; the help
  text says to pick the right source language.
- **Large text and TalkBack**: the input, the result and the controls are labeled; a finished
  translation is announced once, not while typing.
- **Memory**: translation runs off the main thread and stops cleanly if the app goes to the
  background; the engine is released when the tool is left.
- **Rotation** keeps the typed text, the languages, the mode and the result (state is kept across recreation).
- **Microphone refused**, or taken by another app or a call: voice stops with a message; text keeps working.
- **A phone call or an alarm** during a conversation: listening and reading aloud pause, and resume only when the
  person taps again.
- **Android 11 and earlier** (no on-device recognizer to ask for): voice controls are not shown; text works.
- **TalkBack in Conversation**: the phone's voice and TalkBack would talk over each other; with TalkBack on, the
  translation is announced instead of read by the app's voice.
- **Shortcut** (spec 016): the tool opened alone from the launcher works the same, and stops listening when its task
  is hidden.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The catalog MUST gain a tool with the permanent id `translator` in the Utilities category,
  with a localized name, search keywords and help text in English, Portuguese (Brazil) and Spanish. It
  takes typed, pasted or spoken text.
- **FR-002**: The tool MUST offer English, Portuguese and Spanish at first, and the list of languages
  MUST come from the language packs available, so that adding a pack adds a language.
- **FR-003**: Translation MUST run entirely on the phone, with no network use, once the needed packs
  are installed. The text MUST NOT be sent anywhere or stored.
- **FR-004**: A pair of non-English languages MUST be translated through English and MUST say so.
- **FR-005**: The translation MUST update after a pause in typing, run off the main thread, and be
  cancelled by new input; the same source and target MUST show the text unchanged.
- **FR-006**: Input MUST be limited to 5,000 characters with a visible counter; extra text MUST NOT be
  translated and MUST be reported.
- **FR-007**: The tool MUST offer a swap of the languages, Copy (with a confirmation) and Share of the
  translation, and MUST remember the chosen languages and mode (not the text).
- **FR-008**: The modes Type or speak and Conversation MUST be the tool's top choice, and what stays between them (the
  languages) MUST keep its place (ui-rules M1–M5).

**Language packs**

- **FR-010**: Language data MUST NOT be bundled with the app. A pack MUST be downloaded only after the
  user confirms its name and size.
- **FR-011**: A "Download only on Wi-Fi" switch MUST exist, on by default, remembered; with it on and no
  Wi-Fi the tool MUST NOT download and MUST say how to proceed.
- **FR-012**: A download MUST show progress, MUST be cancellable, MUST leave no partial pack when
  cancelled or failed, and MUST check the free space before starting.
- **FR-013**: The only network use introduced by this feature MUST be the downloading of language packs,
  by an explicit user action, and MUST live only in the content module; the Translator and every
  other tool MUST stay offline.
- **FR-014**: Settings MUST have an "Offline languages" section listing each pack with its size and
  state, the total space used, and Delete (after a confirmation) and Download.
- **FR-015**: Installed packs MUST be excluded from the data export of spec 010 and downloaded again
  on a new phone.

**Voice**

- **FR-020**: The microphone MUST be asked for only through the app's permission flow (`PermissionGate`), the first
  time the person taps a microphone button, never at the tool's start.
- **FR-021**: Speech recognition MUST use only the phone's on-device recognizer, never one that may send audio over
  the network. Where none exists, or not for the language, the tool MUST say so and keep every text feature.
- **FR-022**: Audio MUST NOT be recorded to a file, kept or sent; recognized text follows FR-003. Listening MUST stop
  when the tool's screen is hidden (`AppVisibility`) and MUST show while it is on.
- **FR-023**: Spoken input MUST end after a pause of about two seconds or a second tap, and MUST be translated as
  typed text is.
- **FR-024**: Reading aloud MUST use only offline voices of the phone's text-to-speech, in the target language, and
  MUST say how to add a voice when none is installed.
- **FR-025**: In Conversation, each language MUST have its own half and microphone button; a finished sentence MUST
  be shown translated on the other half and read aloud in its language; no microphone MUST listen while the phone
  speaks.
- **FR-027**: A conversation MUST live only while the screen is open; Copy and Share MUST send it as the text shown,
  one turn per line with its language (ui-rules A6).
- **FR-028**: While a conversation is open, the screen MUST stay on.
- **FR-029**: Every platform service the voice uses (recognition, reading aloud, the microphone's state) MUST be
  reached through a port in common code, so the iPhone version adds its own implementation and nothing else.

**Accessibility and quality**

- **FR-050**: Every control MUST have a spoken label and a touch target of at least 48 dp (36 dp in a reduced
  window, ui-rules L7–L10); a finished translation MUST be announced once and not while the user types or speaks.
- **FR-051**: All screens MUST work at 200% text size in light and dark themes and in landscape.
- **FR-052**: Every string MUST exist in English, Portuguese (Brazil) and Spanish.

### Key Entities

- **Language pack**: a pair of languages (for example English to Portuguese), its version, its size and
  whether it is installed.
- **Translator settings**: the source and target language, the mode and the Wi-Fi-only switch.
- **Translation request**: the text, the pair and the result (not stored).
- **Speech support**: for each language, whether the phone can recognize it on the device and read it aloud offline.
- **Turn**: in a conversation, the language, the recognized text and its translation (not stored).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: With the packs installed and airplane mode on, a 30-word sentence translates in under 2
  seconds on the owner's phone.
- **SC-002**: For 20 sample sentences per language pair chosen by the owner, at least 90% are judged
  understandable by the owner.
- **SC-003**: No network request is made during translation, listening or reading aloud, or while the tool is open, in
  100% of the automated checks (network guard and a fake content repository); a download starts only after an
  explicit confirmation.
- **SC-004**: A cancelled or failed download leaves no partial data (checked by the size of the pack
  folder) in 100% of the automated checks.
- **SC-005**: The Translator and the Offline languages section pass the automated accessibility
  checks, the 200% text screenshots and the owner's TalkBack pass.
- **SC-006**: The release package grows by no more than 2 MB compared with the previous release, plus the
  translation engine's native code (the language data is downloaded, not bundled).
- **SC-007**: In airplane mode on the owner's phone, a spoken 10-word sentence shows its translation within 2 seconds
  of its end, and in Conversation it is heard within 3 seconds.
- **SC-008**: After a 10-minute conversation, the app's storage holds no audio and no turns (checked by the app's
  files before and after).
- **SC-009**: For 20 sentences per language spoken by the owner in a quiet room, at least 85% are recognized well
  enough that the translation is understandable.

## Assumptions

- **Research first, on the owner's phone** (the plan's first tasks):
  1. Whether the phone has an on-device recognizer, for which of English, Portuguese and Spanish, and how a missing
     language is added (Android's recognition support check, Android 13 and later). The owner's phone is a Samsung;
     its result decides how story 4 is tested there.
  2. Which offline text-to-speech voices it has for the three languages.
  3. Bergamot's native build for Android (arm64 and x86_64 for the emulator), its size in the app, and the time for a
     30-word sentence on the phone (SC-001).
  4. Each Bergamot model's license, checked file by file, and the pack sizes for the three languages.
  5. Google Play on-demand delivery for a non-game app (asset packs, up to 512 MB each), tested with bundletool's
     local testing until the Play Console account exists.
  6. For the iPhone later, a check on paper only: Bergamot's C++ core through Kotlin/Native interop, the iPhone's
     on-device Speech and offline voices for the three languages, and Apple-hosted assets for the packs. The plan's
     ports must fit them.
- Delivery through Google Play needs the Play Console developer account and an internal-testing
  release to be tested; until then packs can only be tested with local tooling.
- Quality varies by language pair and by microphone and room; the tool is for travel and everyday talk, not for
  legal or medical text, and its help text says so.
- Listen (live captions of one speaker, read aloud only into headphones), automatic language detection, camera input,
  more languages, and our own speech pack (Whisper) for phones without an on-device recognizer are future work and do
  not change this specification's structure.
- This feature introduces the content module (`core/content`) that the constitution allows to use the
  network, with a repository interface in common code so iOS can use Apple-hosted assets later. The phone's own
  recognizer and voices are reached through ports in `core/domain` with Android implementations in `core/platform`,
  like the other platform services.
- Online translation services, ML Kit (also its newer speech and translation APIs) and any recognizer that may use the
  network are excluded (Principle I).

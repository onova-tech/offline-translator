# Feature Specification: The app's own speech and voices, where the phone has none

**Feature Branch**: `082-open-speech`

**Created**: 2026-11-21

**Status**: Draft

**Input**: Owner, 2026-11-20, after the phone pass of spec 080 ("Japanese STT wasn't being installed"; "Arabic didn't
have a STT in samsung"): "That was the fear I had by relying on system packages, when it works it works amazingly, but
when it doesn't it let us in a hard situation. Maybe we'll need to adress that with the opensource package failover
idea"; "I also want a TTS opensource model where the smartphone services can't handle it." Then, 2026-11-21: "proceed
with the code to use the open source models to TTS and STT".

Today (specs 012 and 080) the Translator hears and speaks only through the phone's own on-device recognizer and voices.
When the phone has none for a language, or downloads one and never installs it, that language is text-only.

## Clarifications

### Session 2026-11-21

- Q: Which open-source recognition model? → A: **Whisper Base** (owner): one multilingual model of about 160 MB for
  about 99 languages, MIT-licensed weights. It writes what was said after the person stops, not word by word.
- Q: Which open-source voices? → A: **Both Kokoro and Piper** (owner): Kokoro, one model for its languages (about 8,
  including English, Spanish, Brazilian Portuguese, Japanese and Chinese); Piper, one voice per language, for the
  languages Kokoro lacks. Only voices whose licence allows this free, published app are offered.
- Q: When does the app use its own speech? → A: **As a fallback, with a choice** (owner): automatically for a language
  the phone can't hear or speak offline, and Manage languages lets the person choose the phone's or the app's, for each
  language's speech recognition and voice.
- Q: Kokoro or Piper, after the phone's numbers (research R2, R3)? → A: **Piper only** (owner): Kokoro needed 5–7 s
  before each sentence could play and garbled Japanese kanji; Piper starts almost at once. Japanese and Korean, which
  have no Piper voice, keep only the phone's voice. This replaces "both" above.
- Q: Where do the models come from? → A: Like the translation models (constitution Principle I): Google Play's
  on-demand packs, downloaded only when the person confirms, with the size first; then everything works in airplane
  mode.

### Session 2026-11-22 (the owner's phone pass)

The owner, after speaking and hearing Portuguese through the app's models: "the build is working as intended now",
then the changes of user story 5.

- Q: Where does the choice between the phone's speech and the app's go? → A: **Inside its part's card** (owner: "it
  doesn't feel like a part of the Reconhecimento de fala option or the Voz option. It should be inside them"), under
  the part's state line. The phone's side is named **Device** (pt-BR "Aparelho", es "Dispositivo"; owner: "Celular"
  read strangely; "Nativo" was the other candidate, set aside because in a translator it reads as "native language").
- Q: How is a voice of the phone's removed? → A: The app can't delete it: the Voice row's ⋮ opens the phone's voice
  app's own list (as "Get a voice" does, `docs/ui-rules.md` C3), where Samsung's and Google's delete their voices.
- Q: How does a half say who said what? → A: **Every turn the same size, with a small bold "Me:" or "You:"** before
  it (owner), **in that half's language**, not the app's (owner: "they should follow the selected language for that
  half of the screen. We can generate the list previously"). The words are a hand-written table of the catalog's 54
  languages, one pair each (owner: option 1 of three; a build-time translation of single words and per-language
  resource folders were set aside). "You" is the polite form a stranger is addressed with (Sie, Vous, Usted); where a
  bare "you" sounds rude as a label (Japanese, Korean), the word for "the other person" (相手, 상대).
- Q: What makes room? → A: **Share leaves both screens**; the Conversation's actions become **Copy | More options |
  Clear** in one row (owner).
- Q: How does a person catch a misheard turn before the other hears it? → A: **"Confirm before sending"**, a switch in
  the Conversation's More options (owner: the app's recognition writes the text only once the person stopped, so a
  mistake shows after the other person heard it). When it's on, the microphone becomes ^ once the turn is heard; ^
  translates it and reads it aloud; the square drops it, and the microphone starts again (owner).
- Q: How is a translation heard again? → A: **A small speaker button on each translated turn** (owner: option A of
  two; the other, one button beside the microphone, replayed only the latest), shown even with "Read translations
  aloud" off.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Speak a language the phone can't recognize (Priority: P1)

A person on a phone that can't recognize Arabic offline taps the microphone in Arabic. Instead of "This phone can't
recognize Arabic offline", the Translator offers the app's speech recognition, with its size; after the download, they
speak, stop, and the sentence appears, then its translation, all in airplane mode.

**Why this priority**: The owner's phone has no Arabic recognizer and never installs Japanese: without this, those
languages can't be spoken at all.

**Independent Test**: With the phone's recognizer reporting Arabic as unsupported, download the app's recognition from
the Conversation notice or Manage languages, switch to airplane mode, speak an Arabic sentence: it is written and
translated.

**Acceptance Scenarios**:

1. **Given** a language the phone can't recognize offline (unsupported, not installed, or never installing), **When**
   the person opens its microphone or Manage languages, **Then** the app's recognition is offered with its size
   ("Speech by the app, 160 MB"), and the old "can't recognize" note appears only if neither the phone nor the app can.
2. **Given** the app's recognition is on the phone, **When** the person taps the microphone and speaks, **Then** a
   listening state shows like the phone's (the waiting dots and the stop square of spec 012), and when they stop or
   pause, the text appears within the time measured in research R2.
3. **Given** the app's recognition is in use, **When** the person speaks, **Then** the sound is kept in memory only
   while it is written down and never saved or sent.

---

### User Story 2 - Hear a translation in a language the phone can't speak (Priority: P1)

A person translating into Japanese taps "Read aloud" on a phone with no offline Japanese voice. The Translator offers
the app's voice for that language (a Piper voice), with its size; after the download, the translation is read aloud
offline.

**Why this priority**: "Read aloud" and the Conversation's spoken turns are half of the tool; a missing voice silences
them today.

**Independent Test**: With the phone reporting no offline voice for Japanese, download the app's voice from Manage
languages, translate a sentence into Japanese in airplane mode, tap Read aloud: it is spoken.

**Acceptance Scenarios**:

1. **Given** a language with no offline voice on the phone, **When** Piper has a voice for it whose licence allows this
   app, **Then** that voice is offered with its size; **When** it hasn't, **Then** the existing "no voice" state stays.
2. **Given** the app's voice is on the phone, **When** a translation is read aloud or a Conversation turn is spoken,
   **Then** it plays offline, and a screen reader's announcement rule of spec 012 still applies.

---

### User Story 3 - Choose the phone's or the app's speech for a language (Priority: P2)

A person whose phone recognizes Spanish poorly opens Manage languages, opens Spanish, and switches its speech
recognition from "Phone" to "App" (downloading it if needed). The Translator uses the app's for Spanish from then on.

**Why this priority**: The owner wants the choice; the automatic fallback covers the common case first.

**Independent Test**: In Manage languages, set Spanish's recognition to "App" and its voice to "App"; speak and read
aloud in Spanish: the app's are used; set them back to "Phone": the phone's are used again; the choice survives a
restart.

**Acceptance Scenarios**:

1. **Given** a language both the phone and the app can do, **When** the person opens it in Manage languages, **Then**
   its Speech recognition and Voice rows each offer "Phone" or "App", "Phone" at first.
2. **Given** "App" is chosen and the app's model isn't downloaded, **Then** the row offers its download with the size,
   and the phone's is used until it is ready.
3. **Given** a language only the app can do, **Then** "App" is used without asking, and the row says so.

---

### User Story 4 - Know and remove what the speech takes (Priority: P3)

Manage languages and Settings → Offline languages show the speech models with their sizes, and each can be deleted
like a translation pack; Settings → About → Open-source licenses credits Whisper, each Piper voice and the speech engine.

**Independent Test**: Delete the app's recognition from Manage languages: the space used drops by its size, and a
language the phone can't recognize offers the download again.

---

### User Story 5 - Check what was heard before the other person hears it (Priority: P2)

The owner's phone pass (2026-11-22). A person speaking through the app's recognition sees the text only after they
stop, so a mistake reaches the other person before it can be fixed. With "Confirm before sending" on, the heard turn
waits in its half until it is sent, dropped or corrected. Every turn reads the same size, with who said it in front,
and any translation can be heard again. The bottom of the Conversation takes one row less.

**Why this priority**: The app's recognition (story 1) is only trusted in a real conversation once a mistake can be
caught before it is said aloud.

**Independent Test**: Turn on "Confirm before sending", speak a sentence: it waits in the half with ^ in the
microphone's place and nothing in the other half; tap the square: it's gone and the microphone is back; speak again
and tap ^: it's translated and read aloud. Tap the speaker on a translated turn with "Read translations aloud" off: it's
read in that half's language.

**Acceptance Scenarios**:

1. **Given** "Confirm before sending" is on, **When** a turn is heard (a pause, or a tap on the microphone), **Then**
   it shows in its half, marked as waiting, with ^ in the microphone's place and the square beside it, and the other
   half shows nothing of it yet.
2. **Given** a waiting turn, **When** the person taps ^, **Then** it is translated, shown in the other half and read
   aloud as any turn.
3. **Given** a waiting turn, **When** the person taps the square, **Then** it is dropped and the microphone is back;
   **When** they tap the waiting text, **Then** they can correct it by typing, and it keeps waiting.
4. **Given** "Confirm before sending" is off (at first), **Then** turns are translated as soon as they are heard.
5. **Given** a translated turn, **Then** a speaker button beside it reads it aloud in its half's language, whether
   "Read translations aloud" is on or not; while it reads, the button stops it. It is enabled only when that
   language's voice is ready, and disabled (not hidden) otherwise.
6. **Given** any turn, **Then** it reads in one size with a small bold "Me:" (this half's speaker) or "You:" (the other
   person) in front, both in the half's language, for each of the catalog's languages.
7. **Given** the Conversation, **Then** its bottom row is Copy | More options | Clear, and neither screen has Share.
8. **Given** Manage languages, **Then** a part's Device | App choice sits inside the part's card; the device's voice
   offers its voice app's list behind ⋮; and the page opens without a flash.

### Edge Cases

- A language neither the phone nor the app can recognize or speak keeps today's notes (spec 080 C2).
- A recognition longer than the model's window (about 30 seconds) is cut into pieces and written in order; a
  Conversation turn stays one turn.
- Storage too full for a model: the download fails with the store's message, nothing half-installed stays.
- The phone's recognizer starts working later (an update): with the choice at "Phone", the phone's is used again.
- A Piper voice whose licence forbids this use is never offered, even if it exists.
- A waiting turn (story 5) when its half's language changes is dropped: it was heard in the other language. When the
  screen is hidden or a call comes, it keeps waiting. "Confirm before sending" switched off while a turn waits leaves
  it waiting: nothing is said without a tap.
- While a turn waits, is translated or is read aloud, the other microphone and the speaker buttons wait too (one turn
  at a time, as today). A speaker button reads even with TalkBack on: the person asked for it.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The app MUST offer its own speech recognition (one multilingual model) for every language of the
  catalog the model covers, when the phone can't recognize that language offline.
- **FR-002**: The app MUST offer its own voice for a language the phone can't speak offline: that language's Piper
  voice, when one with an acceptable licence exists.
- **FR-003**: Models MUST come through the platform store's on-demand packs, only after the person confirms a download
  that shows its size, following "Download only on Wi-Fi"; nothing is fetched otherwise (constitution Principle I).
- **FR-004**: Once downloaded, recognition and voices MUST work in airplane mode.
- **FR-005**: Recorded sound MUST stay in memory while it is written down and MUST never be saved, shared or sent.
- **FR-006**: Manage languages MUST show, for each language, which recognition and voice are used (phone or app), let
  the person choose where both exist, and offer the app's downloads and their deletion.
- **FR-007**: The choice per language MUST be remembered across restarts.
- **FR-008**: The listening and speaking states (waiting dots, stop, the Conversation's halves) MUST look and behave
  the same whichever engine is used.
- **FR-009**: Settings → About → Open-source licenses MUST credit the speech engine and every model and voice shipped,
  with their licences.
- **FR-010**: Every new string MUST exist in English, Portuguese (Brazil) and Spanish.
- **FR-011**: On Manage languages, the Device | App choice MUST sit inside its part's card, and the phone's side MUST
  be named "Device" ("Aparelho", "Dispositivo").
- **FR-012**: The Voice row of a language the device speaks MUST offer, behind ⋮, the device's voice app's own list,
  where its voices are removed.
- **FR-013**: Manage languages MUST open without a flash: no frame of another colour, or of the page empty, between
  the screen it opens from and the page.
- **FR-014**: Each Conversation turn MUST read in one size, with a small bold "Me:" or "You:" in front in its half's
  language; every language of the catalog MUST have both words.
- **FR-015**: Type or speak and the Conversation MUST have no Share; the Conversation's bottom row MUST be Copy |
  More options | Clear.
- **FR-016**: The Conversation's More options MUST have "Confirm before sending", off at first and remembered. When
  on, a heard turn MUST wait in its half, unseen by the other half, until ^ sends it (translated, then read aloud as
  any turn), the square drops it, or the person corrects it by typing.
- **FR-017**: Each translated turn MUST have a speaker button that reads it aloud in its half's language, whatever
  "Read translations aloud" says. It MUST be enabled only when that language's voice is ready (the phone's or the
  app's, as routed), and disabled otherwise, so it doesn't come and go (owner, 2026-11-22: "only be enabled if the TTS
  for this language is properly configured"); while it reads, no microphone listens.

### Key Entities

- **Speech model**: the recognition model or a voice (one Piper voice), with its languages, size, licence and pack.
- **Speech choice**: for a language, which recognition and which voice are used (phone or app).
- **Turn labels**: for each language of the catalog, its words for "Me" and "You", hand-written (story 5).

## Success Criteria *(mandatory)*

- **SC-001**: On the owner's phone, Arabic can be spoken and heard, and Japanese spoken, in airplane mode once the app's
  models are downloaded.
- **SC-002**: A 5-second sentence is written within the time measured and agreed in research R2 on the owner's phone.
- **SC-003**: Nothing new reaches the network: the network guard passes, and the only downloads are the store's packs.
- **SC-004**: Every offered voice's licence is recorded and allows this free, published app.

## Assumptions

- The speech engine is sherpa-onnx (Apache-2.0, on-device, no network), whose Android library runs both Whisper and the
  voices; research R1 confirms its packaging and size.
- Google Play's limits (100 packs, 4 GB of on-demand packs in total) must hold with the translation packs; research R5
  sizes the voices to fit.
- The iPhone version comes later, with the same models.

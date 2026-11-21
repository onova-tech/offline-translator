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

### Edge Cases

- A language neither the phone nor the app can recognize or speak keeps today's notes (spec 080 C2).
- A recognition longer than the model's window (about 30 seconds) is cut into pieces and written in order; a
  Conversation turn stays one turn.
- Storage too full for a model: the download fails with the store's message, nothing half-installed stays.
- The phone's recognizer starts working later (an update): with the choice at "Phone", the phone's is used again.
- A Piper voice whose licence forbids this use is never offered, even if it exists.

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

### Key Entities

- **Speech model**: the recognition model or a voice (one Piper voice), with its languages, size, licence and pack.
- **Speech choice**: for a language, which recognition and which voice are used (phone or app).

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

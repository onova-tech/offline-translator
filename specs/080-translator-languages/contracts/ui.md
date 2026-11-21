# Contract: the language lists and the language page (spec 080)

Every screen follows `docs/ui-rules.md`; strings in English, Portuguese (Brazil) and Spanish. Adds to spec 012's
`contracts/ui.md`.

## The language lists

Each Translator `MenuChoice` (From and To in Type or speak; each half's in Conversation, turned with its half):

- The checked languages: those on the phone first, then the others with "48 MB to download" under the name (as today).
- A divider, then **"Other languages…"** (pt "Outros idiomas…", es "Otros idiomas…"), which opens the language page.

## The language page

A full-screen page over the tool (the Conversation's correction dialog does the same), with:

- **Top**: the back arrow and the title "Languages" (pt "Idiomas", es "Idiomas"); under it a `SearchField`
  ("Search languages"). Nothing else on top (S1).
- **Middle, the only part that scrolls**: one `CheckRow` per language, sorted by name in the app's language:
  - the name ("Japanese");
  - supporting text: its own name ("日本語"), and " · Basic quality" for a basic-quality language;
  - a check box at the end; the whole row is one touch target; TalkBack reads "Japanese, 日本語, Basic quality,
    checked/not checked".
  - With two checked, those two rows are disabled, and one `ShortNote` above the list says "At least two languages
    stay in the lists." (pt "Pelo menos dois idiomas ficam nas listas.", es "Al menos dos idiomas quedan en las
    listas.").
  - A search with no match: `NO_READING` with "No language matches" (pt "Nenhum idioma encontrado", es "Ningún
    idioma coincide").
- **Bottom**: nothing (no actions; A-rules don't apply): a change applies at once; back closes the page.

## Translations

- Under a translation from or into a basic-quality language, the `ShortNote` adds "Basic quality" (one note, joined
  with the others by " · ", as spec 012 does).
- Right-to-left text (R6) starts at the right in the field, the translation and the turns.

## Settings → Offline languages

The checked languages first, then the installed packs of unchecked languages, each a row with ⋮ as today (A6).

## Manage languages (owner's phone pass, 2026-11-20; replaces "The language page" above)

The lists' last entry reads **"Manage languages…"** (pt "Gerenciar idiomas…", es "Gestionar idiomas…") and opens a
full-screen page over the tool, also opened by the speech popups (at their language) and by Settings → Offline
languages.

- **Top**: back, the title "Manage languages" (pt "Gerenciar idiomas", es "Gestionar idiomas"), the `SearchField`.
- **Middle, the only part that scrolls**:
  - The at-least-two note in kept room (as before).
  - **"Your languages"** (pt "Seus idiomas", es "Tus idiomas"): each checked language and each language whose pack is
    on the phone, by name, as a card: its `CheckRow` (name; own name, " · Basic quality"), then three
    `MenuSettingsRow`s, their actions behind ⋮ (A6):
    - **Translation** (pt "Tradução", es "Traducción"): "Installed · 70 MB" (Delete, which asks first), "48 MB to
      download" (Download), "Downloading 23 of 48 MB" (Cancel), Play's confirmation (Continue); English: "Built in".
    - **Speech** (pt "Fala", es "Voz hablada"… see strings): ready; the phone can get it (Get this language); being
      downloaded; the phone didn't install it (Update, where the phone's speech service has a store page); not on this
      phone; this phone has no on-device recognizer.
    - **Voice** (pt "Voz", es "Voz"): ready; no offline voice (Get a voice: the voice app's own download list); no
      voice engine.
  - **"Other languages"** (pt "Outros idiomas", es "Otros idiomas"): every other language's `CheckRow`, by name.
    Checking one moves it to "Your languages".
- **Bottom**: nothing.

## The speech popups

- Conversation (upright in the near half): the message, **"Manage languages"**, which opens the page at that half's
  language, and a **small ×** at the card's top end whose touch area never reaches the buttons (C2 stays: closed until
  the tool reopens or the half's language is chosen again).
- Type or speak: a speech or voice message about the language has OK and "Manage languages".
- Back from the page, both modes ask the phone again.

## Settings → Offline languages

The Wi-Fi switch, then a row **"Manage languages"** with "70 MB used", which opens the page.

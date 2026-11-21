# Data model: Offline Translator (spec 012)

Types live in `feature/tools-translate/src/commonMain/.../translate/domain` unless they are a port's own vocabulary,
which lives in `core/domain` (contracts/ports.md). Nothing here holds Android or Compose types.

## Language

The languages the tool offers, in the app's code.

| Field | Type | Rule |
|---|---|---|
| `code` | `LanguageCode` (value class over `String`) | `en`, `pt`, `es` at first; any language a pack adds (FR-002) |
| `name` | string resource | in the app's language |

English is built in (no pack). Every other language comes from a pack.

## LanguagePack

One pack per non-English language, holding both directions with English (research R4).

| Field | Type | Rule |
|---|---|---|
| `id` | `PackId` (value class) | `translate_<code>`, permanent |
| `language` | `LanguageCode` | not `en` |
| `version` | `Int` | from the pack's `pack.json` (contracts/pack-format.md) |
| `downloadBytes` | `Long` | from the store, before any download; shown in MB |
| `installedBytes` | `Long?` | once installed; summed for Settings' total (FR-014) |
| `state` | `PackState` | below |

### PackState

```text
Unknown ──(store answers)──► NotInstalled ──(Download, confirmed)──► Waiting ─► Downloading(bytes, total)
   │                              ▲                                    │              │
   │                              │                                    ▼              ▼
   └─► Unavailable(reason)        └──────────(Cancel / Delete)──── Failed(reason) ◄── Installing ─► Installed
```

- `Unavailable(reason)`: `NO_STORE` (sideloaded, no Google Play), `UNSUPPORTED_DEVICE` (32-bit phone, research R3).
- `Waiting(reason)`: `FOR_WIFI` (switch on, no Wi-Fi: FR-011), `FOR_CONFIRMATION` (the store asks on mobile data).
- `Failed(reason)`: `NO_SPACE` (with the bytes needed), `NETWORK`, `DAMAGED`, `OTHER`; every one offers Try again
  and leaves no partial pack (FR-012).
- `Installed` with a damaged or missing file reads as `NotInstalled` (spec edge case).
- Delete asks first; it cancels a running translation that uses the pack (story 3, scenario 5).

## TranslatorSettings

Stored in `translator.json` (DataStore, `schemaVersion` 1). Exported as the settings part `translator` (spec 010);
packs are never exported (FR-015).

| Field | Type | Default | Rule |
|---|---|---|---|
| `schemaVersion` | `Int` | 1 | |
| `source` | `LanguageCode` | the app's language if offered, else `en` | |
| `target` | `LanguageCode` | `en`, or `es` when the source is `en` | may equal `source` (FR-005: text shown unchanged) |
| `mode` | `TranslatorMode` | `TYPE_OR_SPEAK` | `TYPE_OR_SPEAK`, `CONVERSATION` (FR-008) |
| `conversationLanguages` | `Pair<LanguageCode, LanguageCode>` | `source`, `target` | the two halves |
| `wifiOnly` | `Boolean` | `true` | FR-011 |
| `farHalfTurned` | `Boolean` | `true` | the far half faces the other person |

The text, the results and the turns are never stored (FR-003, FR-027).

## TranslationRequest and TranslationResult

| Field | Type | Rule |
|---|---|---|
| `text` | `String` | trimmed; empty → no request; > 5,000 characters → only the first 5,000 are translated and `overLimit` is set (FR-006) |
| `from`, `to` | `LanguageCode` | |
| `route` | `Route` | `Same` (from = to), `Direct(pack)` (one side is English), `ThroughEnglish(first, second)` (FR-004) |
| `result` | `String` | |
| `overLimit` | `Boolean` | |

A request is made after 500 ms without typing (FR-005); a newer one replaces it, and an older result is dropped.

## SpeechSupport

What the phone can do for one language; asked of the ports, not stored.

| Field | Values |
|---|---|
| `recognition` | `READY`, `CAN_DOWNLOAD` (the phone's own speech service can get it), `UNSUPPORTED`, `NO_RECOGNIZER` (Android 12+ without one: the microphone says so), `SYSTEM_TOO_OLD` (Android 11 and earlier: no voice controls; T069), `UNKNOWN` (Android 12 and 12L: found out on the first try) |
| `voice` | `READY`, `NO_OFFLINE_VOICE`, `NO_ENGINE` |
| `speechTag` | BCP-47 tag from the region rule (research R9): `pt-BR`, `es-MX`, `en-US`… |

## Conversation

The Conversation mode's state; it lives in its ViewModel and dies with the screen (FR-027).

| Field | Type | Rule |
|---|---|---|
| `near`, `far` | `Side` (language, `SpeechSupport`) | the near half is the phone owner's |
| `turns` | `List<Turn>` | newest last; at most 200 (older ones fall off the top, still in Copy) |
| `phase` | `ConversationPhase` | below |

### Turn

| Field | Type | Rule |
|---|---|---|
| `id` | `Long` | increasing |
| `side` | `NEAR` or `FAR` | |
| `heard` | `String` | the recognized text; editable (story 5, scenario 4) |
| `translation` | `String?` | null while translating |
| `edited` | `Boolean` | a corrected turn is translated again |

### ConversationPhase

```text
            tap mic(side)                final text             translation ready
Idle ───────────────────────► Listening(side, partial) ──────► Translating(turn) ──────────► Speaking(turn)
 ▲   ◄── tap again / 2 s pause with nothing heard ──┘                │ no voice / TalkBack on      │ voice done
 │                                                                   ▼                             │
 └───────────────────────────────────────────────────────────── Idle ◄─────────────────────────────┘

Any phase ──(call, alarm, screen hidden: audio focus lost or AppVisibility)──► Paused(reason) ──(tap)──► Idle
Any phase ──(recognizer or engine error)──► Idle with a message on that side
```

Rules (test-first, research R8): a microphone opens only from `Idle`; nothing listens while `Speaking`; a pause ends
`Listening` only after something was heard; with nothing heard, it returns to `Idle` and says so.

### Conversation text (Copy, Share)

One line per turn, in order: `<language name>: <heard>` then `<other language name>: <translation>`, in the app's
language (FR-027, ui-rules A6).

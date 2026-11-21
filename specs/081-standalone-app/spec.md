# Feature Specification: Offline Translator, its own app

**Feature Branch**: `081-standalone-app`
**Created**: 2026-11-20
**Status**: In progress
**Input**: Owner, 2026-11-20: "Let's make translator a separated app. Create another folder on ../ containing its code
and specs. We'll make it free to use and without ads at all. I think that the time of really good apps ecosystem has
passed, so I'll try to make my contribution on that sense. The internet usage of this app would be about the same as
Polytools, but since there would be no ads, we'll need internet just to download the models for TTS, STT and
translate."

## Owner's decisions (2026-11-20)

| # | Decision | Chosen |
|---|---|---|
| 1 | Name | **Offline Translator**; folder `../offline-translator`; package `tech.onova.offlinetranslator` |
| 2 | Licence | **Open source, GNU GPL 3.0** (so voices built on espeak-ng may be used later) |
| 3 | Repository | **Public**, `onova-tech/offline-translator`, with a fresh history (PolyTools stays private) |
| 4 | Money | **Free, no ads, no purchases**: constitution Principle VI rewritten |
| 5 | PolyTools | Continues **without any translator**; the shared components the Translator added stay there too |

## User Scenarios

### Story 1: The app opens on the Translator (P1)

The person opens Offline Translator and is in Type or speak at once: no Home, no list of tools. The top bar has the
app's title, help (ⓘ) and a gear for Settings. System Back leaves the app.

**Acceptance**: launching shows "Type or speak"; there is no Back arrow; the gear opens Settings and Back returns
(`NavigationTest`).

### Story 2: Settings holds only what a translator needs (P1)

Settings has Appearance (theme, dynamic colour), Language (the app's language), Offline languages (Wi-Fi only, Manage
languages) and About (version, source code under the GNU GPL 3.0, open-source licenses). Nothing about ads, purchases,
units, date formats or data files.

**Acceptance**: `SettingsScreenTest`; the source code row opens github.com/onova-tech/offline-translator in the browser,
or says where it is when no app opens links.

### Story 3: Nothing else leaves the phone (P1)

The only network is Google Play's download of the language packs the person chose (`core/content`). The only runtime
permission is the microphone, asked when it is first needed.

**Acceptance**: the network guard allows only `core/content`; `permissions.allowlist` lists the microphone and Play's
dataSync service; the Translator works in airplane mode once a pack is installed (`TranslatorJourney`).

## Requirements

- **FR-001**: The app MUST open on the Translator and keep its state across a rotation or resize.
- **FR-002**: The app MUST contain no ad, purchase, consent form or account.
- **FR-003**: About MUST name the licence and lead to the source code.
- **FR-004**: The launcher icon is the app's own (two speech bubbles), not PolyTools'.
- **FR-005**: Every string exists in English, Portuguese (Brazil) and Spanish.

## Still open

- A privacy policy page: Google Play asks for one from apps that use the microphone (before the first release).
- Whether to add a GPL "additional permission" for linking Google Play's asset-delivery library, before accepting
  outside contributions.
- The open-source speech fallback (sherpa-onnx with Whisper, Moonshine or Piper voices) explored on 2026-11-20.

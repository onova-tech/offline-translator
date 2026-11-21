# Quickstart: validating the translator (spec 012)

How to prove each story works, on the emulator and on the owner's phone. Details of the types are in
[data-model.md](data-model.md); the screens in [contracts/ui.md](contracts/ui.md).

## Prerequisites

- The native library is built and committed (`core/translation/NATIVE.md` names its commit); nothing to build.
- The packs' files: `scripts/fetch-language-packs.sh` (downloads about 100 MB once, checks every SHA-256).
- `bundletool` (the version pinned in `docs/testing.md`), for local testing of asset packs.
- For the phone steps: the owner's phone on adb (see the project memory on the phone), with its consent each time.
  The end-to-end tests never run on it.

## 1. Automated gates

```bash
scripts/ci.sh check                              # domain, ViewModels, UI, screenshots, fit, audit, rules, coverage
scripts/ci.sh e2e --test TranslatorJourney       # the emulator: a pack from local testing, a text translated offline
```

Expected: both green. The journey installs the bundle with `--local-testing`, downloads Português from the phone's
storage, turns airplane mode on and translates "Onde fica a estação de trem?" to English.

## 2. Research on the owner's phone (the plan's first tasks)

A debug-only probe page (Settings → About, debug builds only) lists, for en, pt and es:

| Line | Source | Closes |
|---|---|---|
| On-device recognizer: yes/no; per language: ready / can download / no | `SpeechInput.support` | research R1 |
| Offline voice per language: yes/no, engine name | `SpeechOutput.voice` | research R2 |
| Engine: library loaded, ABI, time for the 30-word test sentence, direct and through English, memory | `TranslationEngine` | research R3 |

Record the results in `research.md` under each item, with the date and the phone's model and Android version.

As built (T032): **Settings → About → Phone probe (debug)**, then **Run the probe**. It also checks es-US. The engine
reads the packs from the app's own files, copied there over adb before the run (a debug build allows `run-as`):

```bash
scripts/fetch-language-packs.sh
for pack in translate_pt translate_es; do
  adb push packs/$pack/src/main/assets/$pack /data/local/tmp/
  adb shell run-as tech.onova.offlinetranslator mkdir -p files/engine-test-packs
  adb shell run-as tech.onova.offlinetranslator cp -r /data/local/tmp/$pack files/engine-test-packs/
done
```

On the owner's phone, add `-s <serial>` and, in the Secure Folder, `--user 0` where adb asks for it (memory "Owner
phone over adb"). Delete the copies afterwards with
`adb shell rm -rf /data/local/tmp/translate_pt /data/local/tmp/translate_es`.

## 3. The stories on the phone, in airplane mode

| Story | Steps | Expected |
|---|---|---|
| 2 Download | Fresh install; choose Português → English | The offer shows the size; Cancel leaves nothing (Settings shows 0 MB); Download again; the translation appears by itself |
| 1 Text | Airplane mode on; type "Onde fica a estação de trem?" | English within 2 s (SC-001); Copy and Share send it; swap works; pt → es says "Translated through English" |
| 1 Limit | Paste 6,000 characters | Counter in the error colour; note about the first 5,000 |
| 3 Manage | Settings → Offline languages | Each language with its size and state; Delete asks, frees the space; the tool offers it again |
| 4 Speak | Tap the microphone; allow; say the sentence | Words appear while speaking; the translation within 2 s of the end (SC-007); Read aloud speaks it |
| 5 Conversation | Português and Español; speak a turn on each half | The other half shows and says it within 3 s; no microphone opens while the phone speaks; Copy gives every turn |
| Edge | Tap the microphone during a call; switch to another app mid-turn | Listening stops with a message; nothing resumes on its own |
| TalkBack | Turn TalkBack on; one turn each | Each translation announced once; the phone's own voice stays quiet |

Afterwards: the app's files hold no audio and no turns (SC-008):
`adb shell run-as tech.onova.offlinetranslator find files cache -newer <marker>` (debug build) lists nothing new except
`translator.json`.

## 4. Quality (SC-002, SC-009)

The owner reads 20 sentences per pair (their choice) and speaks 20 sentences per language in a quiet room; the
results go in `research.md` (understandable: at least 90% for text, 85% for speech).

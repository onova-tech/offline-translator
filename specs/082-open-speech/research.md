# Research: the app's own speech (spec 082)

## R1. The speech engine: sherpa-onnx on Android (done 2026-11-21)

- **Decision**: sherpa-onnx **2.0.2** (2026-11-19, Apache-2.0), the **static-link** AAR
  `sherpa-onnx-static-link-onnxruntime-2.0.2.aar` (SHA-256 `cb735648…c0cb`, 40 MB, from the project's GitHub release):
  one `libsherpa-onnx-jni.so` per ABI with onnxruntime (MIT) inside, 24.9 MB on arm64-v8a and 28.2 MB on x86_64
  (armeabi-v7a and x86 left out of the bundle). Its Kotlin API (`com.k2fsa.sherpa.onnx`, 132 classes) runs Whisper
  (`OfflineRecognizer` with `OfflineWhisperModelConfig`) and both voice families (`OfflineTts` with Kokoro and VITS).
- **Network**: none. The manifest declares no permission; the classes reference no `java.net`; the native library
  imports no `socket`, `connect`, `getaddrinfo` or HTTP library (checked by symbol). The `https://` strings inside are
  links in its messages. So `core/speech` stays outside the network-allowed modules.
- **How it comes into the build**: not on Maven Central. An Ivy repository pointing at the GitHub release, limited to
  this one artifact, with its SHA-256 pinned by Gradle's dependency verification; no binary in git.
- **Alternatives**: the AAR with a separate `libonnxruntime.so` (52 MB, two libraries); building from source (an hour
  per ABI on this machine, as Bergamot); whisper.cpp (recognition only, no voices).
- **Version: 1.13.8, not 2.0.2** (found on the emulator, 2026-11-21): sherpa-onnx 2.0 removed espeak-ng ("espeak-ng is
  no longer used. Language is determined by the lexicon files"), so Kokoro read only English and Chinese and every Piper
  voice made no sound, and an empty result aborted the process in Whisper's resampler. 1.13.8 (2026-09-10, the last 1.x,
  digest `b22c3fc1…5471`, 24.2 MB on arm64) still bundles espeak-ng, whose GPL suits this GPL app. The cost: 1.x gets no
  more releases; moving to 2.x later means giving the voices phonemes from our own espeak-ng build. The recognizer now
  refuses an empty recording before the native side sees it.

## Emulator run (2026-11-21, Android 36 ATD x86_64 on the 2-core development machine; timings not representative)

Each voice made a sentence, then Whisper Base wrote it back (`SpeechProbe`, no sound played):

| Language | Voice | Voice: load, making | Whisper wrote back | Whisper time |
|---|---|---|---|---|
| English | Kokoro, speaker 3 | 8.6 s, 54.6 s for 2.6 s of speech | exactly the sentence | 11.1 s |
| Portuguese | Kokoro, speaker 42, `pt-br` | 9.0 s, 57.1 s for 3.7 s | exactly the sentence (comma for "!") | 13.0 s |
| Japanese | Kokoro, speaker 37, `ja` | 9.1 s, 76.7 s for 3.9 s | first clause right, "一番近い駅" lost | 15.9 s |
| Arabic | Piper `ar_JO-kareem` | 9.3 s, 9.9 s for 3.0 s | understandable, several words wrong | 14.0 s |

Whisper Base loaded in 6.1 s. Memory: PSS 293 MB with Whisper, about 700 MB with Whisper and Kokoro, 504 MB with
Whisper and a Piper voice, back to 166 MB once both are released. Kokoro makes speech about 5 times slower than a Piper
voice. Japanese through Kokoro and Arabic through Piper or Whisper Base lose accuracy; the phone run decides whether
they're good enough.

## R2, R3. The owner's phone (2026-11-21, Samsung SM-G780G, Snapdragon 865, Android 13; over Tailscale)

The same probe, no sound played, the phone in front:

| Language | Voice | Voice: load, making | Whisper wrote back | Whisper time |
|---|---|---|---|---|
| English | Kokoro, speaker 3 | 3.3 s, **5.1 s for 2.6 s of speech** | exactly the sentence | 0.64 s |
| Portuguese | Kokoro, speaker 42 | 1.9 s, **5.2 s for 3.7 s** | exactly the sentence | 0.77 s |
| Japanese | Kokoro, speaker 37 | 1.9 s, **7.0 s for 3.9 s** | first clause right, "一番近い駅" lost | 1.04 s |
| Arabic | Piper `ar_JO-kareem` | 1.4 s, **1.0 s for 3.2 s** | understandable, several words wrong | 0.87 s |

- **Whisper Base: decided.** It loads in 1.6–2.2 s and writes a 3–4 s sentence in under a second and a half, exactly
  in English and Portuguese. Japanese and Arabic here came from imperfect voices, so their accuracy needs real speech
  (the owner's, in the feature itself) before a verdict.
- **Kokoro: slower than speech.** It needs 1.4 to 2 seconds of work per second of speech on this phone, so a sentence
  waits 5–7 s before it can play, and its Japanese, read through espeak-ng, garbles kanji (Kokoro's own Japanese
  reader isn't in sherpa-onnx). Memory reaches about 790 MB with Whisper.
- **Piper: fast.** About 0.3 seconds of work per second of speech, so it can start almost at once.
- Memory: PSS 356 MB with Whisper loaded, 760–790 MB with Kokoro as well, 558 MB with a Piper voice.

## R4. Piper voices and their licences (2026-11-21)

Source: Piper's own catalogue (`rhasspy/piper-voices`, `voices.json`, 183 voices in 53 languages) and each voice's
MODEL_CARD, read for the 44 languages of the app's catalog that Piper has. A voice is kept only when its dataset's
licence is free (CC0, public domain, CC BY, CC BY-SA, MIT) **and**, if it was fine-tuned, its base voice's is too.

- **The lessac base is out.** Many voices were fine-tuned from U.S. English "lessac", whose data (Blizzard 2013, Lessac
  Technologies) is licensed "for Research Purposes only", to the named person only. Voices from it can't ship in a
  published app (spec SC-004).
- **Free bases**: "rdh" (Flemish, CC0) and "hifi_tts_92" (English, Hi-Fi TTS, CC BY 4.0) carry many voices.

| Result | Languages |
|---|---|
| A free voice, trained from scratch (16) | ca, cs, de, el, en, es, fr, it, lv, nl, nb, sl, sv, te, uk, ur |
| A free voice, fine-tuned from rdh or hifi_tts_92 (15) | bg, bn, et, eu, fa, fi, hu, mr, pl, pt, ro, ru, sk, vi, zh |
| To check: lt (tuned from ru_RU-irina) | lt |
| No free voice (12): only the phone's voice, as today | ar, da, he, hi, id, is, ja, ko, ml, sq, th, tr |
| Not in Piper (10) | af, be, gl, gu, kn, ms, ta, tl, ug, zh-Hant |

So **31 languages** get an app voice. Arabic gets the app's recognition (Whisper) but no voice: its only Piper voice
was tuned from lessac and its own dataset has no licence. For ca and es the free voices are "x_low" quality; a better
free voice per language is picked when the packs are made. Voices missing from sherpa-onnx's converted set (bg, bn, et,
lt, mr, te…) are converted with sherpa-onnx's metadata script.

## R5. Google Play's limits with speech (estimate)

Today: 53 translation packs, about 2.2 GB to download and 3.2 GB unpacked; Play allows 100 packs and 4 GB of on-demand
packs in total. Speech adds Whisper (one pack), Kokoro (one), espeak-ng data (one) and up to 42 Piper voices: about 1 GB
to download. With Piper only (31 voices, about 15 MB each without espeak-ng's data, which becomes one shared pack of
19 MB) and Whisper (160 MB), speech adds about 0.65 GB unpacked: about 3.9 GB with the translation packs, under 4 GB
whether Play counts the download or the unpacked size. Packs: 53 + 1 + 1 + 31 = 86, under 100.

# Contract: a language pack's files (spec 012)

A pack is data only (Principle I): models, vocabularies and one description file. It is an on-demand Play asset pack
named `translate_<code>` (research R5), built by `scripts/fetch-language-packs.sh` (research R6).

## Layout

```text
translate_pt/                          # the asset pack's root, under getPackLocation(...).assetsPath()
├── pack.json
├── en-pt/
│   ├── model.enpt.intgemm.alphas.bin
│   ├── lex.50.50.enpt.s2t.bin
│   └── vocab.enpt.spm
└── pt-en/
    ├── model.pten.intgemm.alphas.bin
    ├── lex.50.50.pten.s2t.bin
    └── vocab.pten.spm
```

## pack.json

```json
{
  "format": 1,
  "id": "translate_pt",
  "language": "pt",
  "version": 1,
  "directions": [
    {
      "from": "en", "to": "pt", "folder": "en-pt",
      "model": "model.enpt.intgemm.alphas.bin", "shortlist": "lex.50.50.enpt.s2t.bin", "vocab": "vocab.enpt.spm",
      "sha256": { "model.enpt.intgemm.alphas.bin": "07892fd2…", "lex.50.50.enpt.s2t.bin": "…", "vocab.enpt.spm": "…" }
    },
    { "from": "pt", "to": "en", "folder": "pt-en", "…": "…" }
  ],
  "source": "https://storage.googleapis.com/moz-fx-translations-data--303e-prod-translations-data/models/en-pt/…",
  "license": "MPL-2.0"
}
```

## Rules

- `format` is the layout's version; the app refuses a `format` it doesn't know (reads as `Failed(DAMAGED)`).
- `version` grows with new models; Play replaces the pack when the app's bundle carries a newer one.
- The app checks that every file named exists and isn't empty before opening the engine; the SHA-256 values are
  checked by the build script and by a debug-only check, not on every start (75 MB to hash on each open is too slow
  for the 2-second goal).
- The engine's configuration (beam size 1, `int8shiftAlphaAll`, the shortlist) is written by the JNI layer from these
  file names, not stored in the pack.

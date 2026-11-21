#!/usr/bin/env python3
"""The Translator's language catalog (spec 080, research R1 and R2).

Reads Mozilla's translation model registry (db/models.json) and keeps every language with a released model in both
directions with English: the larger "base-memory" model over "base" over "tiny"; a language is "basic" quality when
either direction is "tiny". "no" is left out as the same written language as "nb" (owner, 2026-11-19).

Writes committed files:
  packs/languages.tsv                     one line per direction, with every file's name and SHA-256
  core/domain/.../translation/LanguageCatalog.kt   the languages the app offers
  packs/translate_<code>/src/main/assets/translate_<code>/pack.json   each pack's description (spec 012
                                          contracts/pack-format.md), so every pack module exists before its models

The registry gives each model's hash; the shortlists' and vocabularies' are taken here, by downloading them once
(cached in build/language-catalog/). The models themselves are fetched by scripts/fetch-language-packs.sh.

Usage:
  scripts/update-language-catalog.py [--registry models.json]   regenerate both files
  scripts/update-language-catalog.py --check [--registry ...]    fail when they differ from a fresh run
"""

import argparse
import gzip
import hashlib
import json
import os
import sys
import urllib.request

BASE = "https://storage.googleapis.com/moz-fx-translations-data--303e-prod-translations-data"
REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TSV = os.path.join(REPO, "packs", "languages.tsv")
KOTLIN = os.path.join(
    REPO, "core", "domain", "src", "commonMain", "kotlin", "tech", "onova", "offlinetranslator", "core", "domain",
    "translation", "LanguageCatalog.kt",
)
CACHE = os.path.join(REPO, "build", "language-catalog")

RELEASED = {"Release", "Release Android", "Release Desktop"}
ARCHITECTURES = {"base-memory": 0, "base": 1, "tiny": 2}
LEFT_OUT = {"no"}  # The same written language as "nb" (owner, 2026-11-19).

# Language tags for names, where Mozilla's code isn't one: Chinese in simplified and in traditional characters.
TAGS = {"zh": "zh-Hans", "zh_hant": "zh-Hant", "tl": "fil"}

# A default speech region per language (research R5); the phone's own region wins where it matches.
REGIONS = {
    "af": "af-ZA", "ar": "ar-SA", "be": "be-BY", "bg": "bg-BG", "bn": "bn-IN", "ca": "ca-ES", "cs": "cs-CZ",
    "da": "da-DK", "de": "de-DE", "el": "el-GR", "en": "en-US", "es": "es-ES", "et": "et-EE", "eu": "eu-ES",
    "fa": "fa-IR", "fi": "fi-FI", "fr": "fr-FR", "gl": "gl-ES", "gu": "gu-IN", "he": "he-IL", "hi": "hi-IN",
    "hu": "hu-HU", "id": "id-ID", "is": "is-IS", "it": "it-IT", "ja": "ja-JP", "kn": "kn-IN", "ko": "ko-KR",
    "lt": "lt-LT", "lv": "lv-LV", "ml": "ml-IN", "mr": "mr-IN", "ms": "ms-MY", "nb": "nb-NO", "nl": "nl-NL",
    "pl": "pl-PL", "pt": "pt-BR", "ro": "ro-RO", "ru": "ru-RU", "sk": "sk-SK", "sl": "sl-SI", "sq": "sq-AL",
    "sv": "sv-SE", "ta": "ta-IN", "te": "te-IN", "th": "th-TH", "tl": "fil-PH", "tr": "tr-TR", "ug": "ug-CN",
    "uk": "uk-UA", "ur": "ur-PK", "vi": "vi-VN", "zh": "zh-CN", "zh_hant": "zh-TW",
}


def best(entries):
    """The released entry with the largest model, a plain "Release" first; None when nothing is released."""
    released = [e for e in entries if e.get("releaseStatus") in RELEASED and e.get("architecture") in ARCHITECTURES]
    if not released:
        return None
    return min(released, key=lambda e: (ARCHITECTURES[e["architecture"]], e["releaseStatus"] != "Release"))


def sha256_of_gz(path_in_registry):
    """The SHA-256 of a registry file once unpacked, downloading it once into the cache."""
    cached = os.path.join(CACHE, path_in_registry[: -len(".gz")])
    if not os.path.exists(cached):
        os.makedirs(os.path.dirname(cached), exist_ok=True)
        with urllib.request.urlopen(f"{BASE}/{path_in_registry}") as response:
            data = gzip.decompress(response.read())
        with open(cached + ".part", "wb") as out:
            out.write(data)
        os.replace(cached + ".part", cached)
    digest = hashlib.sha256()
    with open(cached, "rb") as f:
        for block in iter(lambda: f.read(1 << 20), b""):
            digest.update(block)
    return digest.hexdigest()


def direction(pair, entry):
    files = entry["files"]
    folder = os.path.dirname(files["model"]["path"])
    vocabs = [files[k] for k in ("srcVocab", "trgVocab") if k in files] or [files["vocab"]]
    name = lambda f: os.path.basename(f["path"])[: -len(".gz")]
    return {
        "pair": pair,
        "quality": "basic" if entry["architecture"] == "tiny" else "full",
        "folder": folder,
        "model": name(files["model"]),
        "model_sha": files["model"]["uncompressedHash"],
        "shortlist": name(files["lexicalShortlist"]),
        "shortlist_sha": sha256_of_gz(files["lexicalShortlist"]["path"]),
        "vocabs": [name(v) for v in vocabs],
        "vocab_shas": [sha256_of_gz(v["path"]) for v in vocabs],
    }


def catalog(models):
    languages = sorted({p.split("-")[0] if p.endswith("-en") else p.split("-")[1] for p in models} - {"en"} - LEFT_OUT)
    rows = []
    for code in languages:
        to, back = best(models.get(f"en-{code}", [])), best(models.get(f"{code}-en", []))
        if to and back:
            rows.append((code, [direction(f"en-{code}", to), direction(f"{code}-en", back)]))
    return rows


def tsv(rows):
    header = "\t".join(
        ["code", "direction", "quality", "tag", "speech", "folder", "model", "model_sha256", "shortlist",
         "shortlist_sha256", "vocabs", "vocab_sha256s"]
    )
    lines = ["# Generated by scripts/update-language-catalog.py from Mozilla's model registry; don't edit.", header]
    for code, directions in rows:
        for d in directions:
            lines.append("\t".join([
                code, d["pair"], d["quality"], TAGS.get(code, code), REGIONS.get(code, code), d["folder"],
                d["model"], d["model_sha"], d["shortlist"], d["shortlist_sha"], ",".join(d["vocabs"]),
                ",".join(d["vocab_shas"]),
            ]))
    return "\n".join(lines) + "\n"


def direction_json(code, d):
    """One direction of pack.json, laid out as spec 012's script wrote it; two vocabularies get their own keys."""
    source, target = d["pair"].split("-")
    if len(d["vocabs"]) == 1:
        vocab_keys = f'"vocab": "{d["vocabs"][0]}"'
    else:
        vocab_keys = f'"sourceVocab": "{d["vocabs"][0]}", "targetVocab": "{d["vocabs"][1]}"'
    hashes = [(d["model"], d["model_sha"]), (d["shortlist"], d["shortlist_sha"])] + list(zip(d["vocabs"], d["vocab_shas"]))
    lines = [
        "    {",
        f'      "from": "{source}", "to": "{target}", "folder": "{d["pair"]}",',
        f'      "model": "{d["model"]}", "shortlist": "{d["shortlist"]}", {vocab_keys},',
        '      "sha256": {',
        ",\n".join(f'        "{name}": "{sha}"' for name, sha in hashes),
        "      }",
        "    }",
    ]
    return "\n".join(lines)


def pack_json(code, directions):
    return "\n".join([
        "{",
        '  "format": 1,',
        f'  "id": "translate_{code}",',
        f'  "language": "{code}",',
        '  "version": 1,',
        '  "directions": [',
        ",\n".join(direction_json(code, d) for d in directions),
        "  ],",
        f'  "source": "{BASE}/models/",',
        '  "license": "MPL-2.0"',
        "}",
        "",
    ])


def pack_json_path(code):
    return os.path.join(REPO, "packs", f"translate_{code}", "src", "main", "assets", f"translate_{code}", "pack.json")


def kotlin(rows):
    entries = ['        OfferedLanguage(LanguageCode.ENGLISH, null, LanguageQuality.FULL, "en", "en-US"),']
    for code, directions in rows:
        quality = "BASIC" if any(d["quality"] == "basic" for d in directions) else "FULL"
        entries.append(f'        packed("{code}", LanguageQuality.{quality}, "{TAGS.get(code, code)}", "{REGIONS.get(code, code)}"),')
    return "\n".join([
        "package tech.onova.offlinetranslator.core.domain.translation",
        "",
        "import tech.onova.offlinetranslator.core.domain.ports.LanguageCode",
        "import tech.onova.offlinetranslator.core.domain.ports.PackId",
        "",
        "/**",
        " * Every language the Translator offers (spec 080): English, and each language Mozilla's model registry has",
        " * released in both directions with English. Generated by scripts/update-language-catalog.py with",
        " * packs/languages.tsv; don't edit by hand.",
        " */",
        "public object LanguageCatalog {",
        "    public val all: List<OfferedLanguage> =",
        "        listOf(",
        *entries,
        "        )",
        "",
        "    private val byCode = all.associateBy { it.code }",
        "",
        "    /** A language with its own pack, named after it. */",
        "    private fun packed(",
        "        code: String,",
        "        quality: LanguageQuality,",
        "        tag: String,",
        "        speechRegion: String,",
        '    ) = OfferedLanguage(LanguageCode(code), PackId("translate_$code"), quality, tag, speechRegion)',
        "",
        "    /** The language with [code], or null when the Translator doesn't offer it. */",
        "    public fun of(code: LanguageCode): OfferedLanguage? = byCode[code]",
        "}",
        "",
    ])


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--registry", help="a models.json file (default: the registry's own, downloaded)")
    parser.add_argument("--check", action="store_true", help="fail when the committed files differ from a fresh run")
    args = parser.parse_args()
    if args.registry:
        with open(args.registry) as f:
            registry = json.load(f)
    else:
        with urllib.request.urlopen(f"{BASE}/db/models.json") as response:
            registry = json.load(response)
    rows = catalog(registry["models"])
    outputs = {TSV: tsv(rows), KOTLIN: kotlin(rows)}
    outputs.update({pack_json_path(code): pack_json(code, directions) for code, directions in rows})
    if args.check:
        stale = [p for p, text in outputs.items() if not os.path.exists(p) or open(p).read() != text]
        for p in stale:
            print(f"out of date: {os.path.relpath(p, REPO)}", file=sys.stderr)
        sys.exit(1 if stale else 0)
    for path, text in outputs.items():
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w") as f:
            f.write(text)
    basic = sum(1 for _, ds in rows if any(d["quality"] == "basic" for d in ds))
    print(f"{len(rows)} languages besides English ({len(rows) - basic} full, {basic} basic quality)")


if __name__ == "__main__":
    main()

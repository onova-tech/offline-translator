#!/usr/bin/env python3
"""The app's own speech catalog (spec 082, research R1 and R4).

Recognition: Whisper Base (int8), one model for every language of the app's catalog that Whisper knows.
Voices: one Piper voice per language, only where its licence allows a published app:
  - its dataset's licence is free (CC0, public domain, CC BY, CC BY-SA, MIT, Apache), and
  - if it was fine-tuned, the voice it started from is free too: "lessac" (Blizzard 2013, "for Research Purposes
    only") and unknown bases are left out.
A voice must also exist in sherpa-onnx's converted set (its `tts-models` release, int8). Among the free ones of a
language: "medium" over "low" over "x_low", a voice trained from scratch over a fine-tuned one, then by name.

Reads (cached in build/speech-catalog/): Piper's voices.json and MODEL_CARDs (Hugging Face, rhasspy/piper-voices) and
the release's file list (GitHub API). Downloads each chosen archive once to pin the SHA-256 of every file it ships.

Writes committed files:
  packs/speech.tsv                                    one line per pack, with every file's name and SHA-256
  core/domain/.../speech/SpeechCatalog.kt             what the app can hear and say
  packs/<pack>/src/main/assets/<pack>/pack.json       each pack's description, so every pack module exists before
                                                      its files (scripts/fetch-speech-packs.sh adds them, git-ignored)

Usage:
  scripts/update-speech-catalog.py            regenerate
  scripts/update-speech-catalog.py --check    fail when the committed files differ from a fresh run
"""

import argparse
import hashlib
import json
import os
import re
import sys
import tarfile
import urllib.request

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TSV = os.path.join(REPO, "packs", "speech.tsv")
LANGUAGES = os.path.join(REPO, "packs", "languages.tsv")
KOTLIN = os.path.join(
    REPO, "core", "domain", "src", "commonMain", "kotlin", "tech", "onova", "offlinetranslator", "core", "domain",
    "speech", "SpeechCatalog.kt",
)
CACHE = os.path.join(REPO, "build", "speech-catalog")
AGENT = {"User-Agent": "offline-translator-catalog"}

PIPER = "https://huggingface.co/rhasspy/piper-voices/resolve/main"
RELEASES = "https://api.github.com/repos/k2-fsa/sherpa-onnx/releases/tags"
DOWNLOAD = "https://github.com/k2-fsa/sherpa-onnx/releases/download"

WHISPER_ARCHIVE = "sherpa-onnx-whisper-base.tar.bz2"
# Published before GitHub kept digests; pinned on 2026-11-21 (research R1).
WHISPER_ARCHIVE_SHA256 = "911b2083efd7c0dca2ac3b358b75222660dc09fb716d64fbfc417ba6c99ff3de"
WHISPER_FILES = ["base-encoder.int8.onnx", "base-decoder.int8.onnx", "base-tokens.txt"]
ESPEAK_ARCHIVE = "espeak-ng-data.tar.bz2"

# Whisper's languages (its tokenizer's list); the app's codes map to them, "nb" and "zh_hant" by hand.
WHISPER = set(
    "en zh de es ru ko fr ja pt tr pl ca nl ar sv it id hi fi vi he uk el ms cs ro da hu ta no th ur hr bg lt la mi ml "
    "cy sk te fa lv bn sr az sl kn et mk br eu is hy ne mn bs kk sq sw gl mr pa si km sn yo so af oc ka be tg sd gu am "
    "yi lo uz fo ht ps tk nn mt sa lb my bo tl mg as tt haw ln ha ba jw su yue".split()
)
WHISPER_CODE = {"nb": "no", "zh_hant": "zh", "tl": "tl"}

FREE = re.compile(r"cc0|public domain|cc[- ]?by(?![- ]?nc)|creativecommons\.org/licenses/by(-sa)?/|attribution|\bmit\b|apache", re.I)
NOT_FREE = re.compile(r"by-?nc|non-?commercial|research purposes", re.I)
QUALITY = {"medium": 0, "low": 1, "x_low": 2}
FREE_BASES = re.compile(r"\brdh\b|hifi_tts_92", re.I)


def fetch(url, path):
    """Downloads [url] into [path] once."""
    if not os.path.exists(path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        request = urllib.request.Request(url, headers=AGENT)
        with urllib.request.urlopen(request) as response, open(path + ".part", "wb") as out:
            while block := response.read(1 << 20):
                out.write(block)
        os.replace(path + ".part", path)
    return path


def sha256(path):
    digest = hashlib.sha256()
    with open(path, "rb") as f:
        for block in iter(lambda: f.read(1 << 20), b""):
            digest.update(block)
    return digest.hexdigest()


def release_assets(tag):
    """name -> (size, sha256 or None) of a sherpa-onnx release, every page."""
    path = os.path.join(CACHE, f"release-{tag}.json")
    if not os.path.exists(path):
        assets, page = [], 1
        while True:
            request = urllib.request.Request(f"{RELEASES}/{tag}", headers=AGENT)
            # The tag endpoint returns the first 100 assets; the release's own assets endpoint pages the rest.
            with urllib.request.urlopen(request) as response:
                release = json.load(response)
            url = f"https://api.github.com/repos/k2-fsa/sherpa-onnx/releases/{release['id']}/assets"
            while True:
                request = urllib.request.Request(f"{url}?per_page=100&page={page}", headers=AGENT)
                with urllib.request.urlopen(request) as response:
                    batch = json.load(response)
                if not batch:
                    break
                assets += [{"name": a["name"], "size": a["size"], "digest": a.get("digest")} for a in batch]
                page += 1
            break
        os.makedirs(CACHE, exist_ok=True)
        with open(path, "w") as out:
            json.dump(assets, out)
    with open(path) as f:
        return {a["name"]: (a["size"], (a["digest"] or "").removeprefix("sha256:") or None) for a in json.load(f)}


def licence_of(card):
    """(free, dataset licence, base) of a MODEL_CARD's text."""
    licences = [m.strip() for m in re.findall(r"(?i)license:\s*(.+)", card)]
    tuned = re.search(r"(?i)fine-?tuned from\s+(.+)", card)
    base = tuned.group(1).strip() if tuned else ""
    text = "; ".join(licences)
    free_data = bool(licences) and all(FREE.search(l) and not NOT_FREE.search(l) for l in licences)
    free_base = not base or bool(FREE_BASES.search(base))
    return free_data and free_base, text, base


def app_languages():
    with open(LANGUAGES) as f:
        rows = [line.split("\t") for line in f if line.strip() and not line.startswith(("#", "code\t"))]
    return sorted({row[0] for row in rows} | {"en"})


def choose_voices(languages, assets):
    """language -> the chosen voice's record, by the rule in this file's docstring."""
    voices = json.load(open(fetch(f"{PIPER}/voices.json", os.path.join(CACHE, "voices.json"))))
    family = {"nb": "no"}
    chosen = {}
    for language in languages:
        candidates = []
        for key, info in voices.items():
            if info["language"]["family"] != family.get(language, language) or info["quality"] not in QUALITY:
                continue
            archive = f"vits-piper-{key}-int8.tar.bz2"
            if archive not in assets:
                continue
            card_path = next(f for f in info["files"] if f.endswith("MODEL_CARD"))
            card = open(fetch(f"{PIPER}/{card_path}", os.path.join(CACHE, "cards", card_path)), errors="replace").read()
            free, licence, base = licence_of(card)
            url = re.search(r"(?i)\burl:\s*(\S+)", card)
            if free:
                candidates.append(
                    ((QUALITY[info["quality"]], bool(base), key), key, info["quality"], licence, base, archive,
                     url.group(1) if url else "")
                )
        if candidates:
            _, key, quality, licence, base, archive, url = min(candidates)
            chosen[language] = {
                "voice": key, "quality": quality, "licence": licence, "base": base, "archive": archive, "url": url,
            }
    return chosen


def archive_files(archive, expected_sha256, wanted):
    """name -> sha256 of the [wanted] files (a predicate on the path inside) of a release archive, downloaded once."""
    path = fetch(f"{DOWNLOAD}/{'asr-models' if 'whisper' in archive else 'tts-models'}/{archive}", os.path.join(CACHE, "archives", archive))
    actual = sha256(path)
    if expected_sha256 and actual != expected_sha256:
        sys.exit(f"{archive}: SHA-256 {actual}, expected {expected_sha256}")
    files = {}
    with tarfile.open(path) as tar:
        for member in tar.getmembers():
            if member.isfile() and wanted(member.name):
                digest = hashlib.sha256()
                with tar.extractfile(member) as f:
                    for block in iter(lambda: f.read(1 << 20), b""):
                        digest.update(block)
                files[member.name.split("/", 1)[1]] = (digest.hexdigest(), member.size)
    return actual, files


def rows():
    """The packs: Whisper, espeak-ng's data, then a voice per language."""
    languages = app_languages()
    tts = release_assets("tts-models")
    out = []
    _, whisper = archive_files(WHISPER_ARCHIVE, WHISPER_ARCHIVE_SHA256, lambda n: n.split("/", 1)[-1] in WHISPER_FILES)
    out.append(("speech_whisper", "recognition", "*", "whisper-base-int8", "", "MIT", "", WHISPER_ARCHIVE, WHISPER_ARCHIVE_SHA256, whisper))
    espeak_sha, espeak = archive_files(ESPEAK_ARCHIVE, tts[ESPEAK_ARCHIVE][1], lambda n: True)
    out.append(("speech_espeak", "voice-data", "*", "espeak-ng-data", "", "GPL-3.0-or-later", "", ESPEAK_ARCHIVE, espeak_sha, espeak))
    for language, voice in choose_voices(languages, tts).items():
        sha, files = archive_files(
            voice["archive"], tts[voice["archive"]][1],
            lambda n: not n.split("/", 1)[-1].startswith("espeak-ng-data/") and n.rsplit("/", 1)[-1] in ("tokens.txt", "MODEL_CARD") or n.endswith(".onnx"),
        )
        out.append((f"voice_{language}", "voice", language, voice["voice"], voice["quality"], voice["licence"], voice["base"], voice["archive"], sha, files))
        DATASETS[f"voice_{language}"] = voice["url"]
    return languages, out


DATASETS = {}
MAX_LINE = 120


def voice_entry(language, pack, voice, quality, licence):
    """One voice of SpeechCatalog.voices, wrapped the way ktlint formats it, so its check passes on the file as written."""
    key = f'            LanguageCode("{language}") to'
    args = [f'PackId("{pack}")', f'"{voice}"', f'"{quality}"', f'"{licence}"']
    one = f"{key} AppVoice({', '.join(args)}),"
    if len(one) <= MAX_LINE:
        return [one]
    inner = f"                AppVoice({', '.join(args)}),"
    if len(inner) <= MAX_LINE:
        return [key, inner]
    return [key, "                AppVoice("] + [f"                    {arg}," for arg in args] + ["                ),"]
LIBRARIES = os.path.join(REPO, "config", "libraries", "libraries")


def licence_key(licence):
    """The licence entry in config/libraries/licenses for a model card's licence text."""
    text = licence.lower()
    if "cc0" in text or "public domain" in text:
        return "offlinetranslator-cc0-1.0"
    if "by-sa" in text or "sharealike" in text or "by sa" in text:
        return "offlinetranslator-cc-by-sa-3.0" if "3.0" in text else "offlinetranslator-cc-by-4.0"
    if re.search(r"\bmit\b", text):
        return "offlinetranslator-mit"
    return "offlinetranslator-cc-by-4.0"


def voice_library(pack, language, voice, licence, base):
    """Settings → About → Open-source licenses' entry for one voice: its dataset, licence and base (FR-009)."""
    description = f"The app's voice for language \"{language}\" (spec 082), from Piper's voices: dataset licence {licence}."
    if base and base != "-":
        description += f" Fine-tuned from {base.rstrip('.')}."
    entry = {
        "uniqueId": f"offlinetranslator.{pack.replace('_', '-')}",
        "developers": [{"name": "Piper voices (rhasspy)", "organisationUrl": "https://github.com/rhasspy/piper"}],
        "artifactVersion": "",
        "description": description,
        "name": f"Piper voice {voice}",
        "website": DATASETS.get(pack) or "https://huggingface.co/rhasspy/piper-voices",
        "licenses": [licence_key(licence)],
    }
    return json.dumps(entry, indent=2, ensure_ascii=False) + "\n"


def tsv_text(packs):
    lines = [
        "# Generated by scripts/update-speech-catalog.py (spec 082); don't edit.",
        "pack\tkind\tlanguage\tvoice\tquality\tlicence\tbase\tarchive\tarchive_sha256\tfiles\tfile_sha256s\tbytes",
    ]
    for pack, kind, language, voice, quality, licence, base, archive, sha, files in packs:
        names = sorted(files)
        # espeak-ng's data is a few hundred small files: its archive's hash pins them, and pack.json lists each one.
        listed = ["(the archive)"] if kind == "voice-data" else names
        hashes = "" if kind == "voice-data" else ",".join(files[n][0] for n in names)
        lines.append("\t".join([
            # "-" for an empty field: a shell reading the file with IFS=tab would merge two tabs into one.
            pack, kind, language, voice, quality or "-", licence.replace("\t", " ") or "-", base.replace("\t", " ") or "-",
            archive, sha,
            ",".join(listed), hashes, str(sum(files[n][1] for n in names)),
        ]))
    return "\n".join(lines) + "\n"


def kotlin_text(languages, packs):
    recognized = [l for l in languages if WHISPER_CODE.get(l, l) in WHISPER]
    voices = [p for p in packs if p[1] == "voice"]
    lines = [
        "package tech.onova.offlinetranslator.core.domain.speech",
        "",
        "import tech.onova.offlinetranslator.core.domain.ports.LanguageCode",
        "import tech.onova.offlinetranslator.core.domain.ports.PackId",
        "",
        "/**",
        " * What the app itself can hear and say (spec 082): Whisper Base for every language below that it knows, and one",
        " * Piper voice for each language whose voice has a free licence (research R4). Generated by",
        " * scripts/update-speech-catalog.py with packs/speech.tsv; don't edit by hand.",
        " */",
        "public object SpeechCatalog {",
        "    /** Whisper Base, one pack for every language it recognizes. */",
        "    public val recognitionPack: PackId = PackId(\"speech_whisper\")",
        "",
        "    /** espeak-ng's data, which every voice needs besides its own pack. */",
        "    public val voiceDataPack: PackId = PackId(\"speech_espeak\")",
        "",
        "    /** Whisper's code for each language of the app it can recognize. */",
        "    public val recognition: Map<LanguageCode, String> =",
        "        mapOf(",
    ]
    lines += [f"            LanguageCode(\"{l}\") to \"{WHISPER_CODE.get(l, l)}\"," for l in recognized]
    lines += [
        "        )",
        "",
        "    /** The app's voice for each language that has one. */",
        "    public val voices: Map<LanguageCode, AppVoice> =",
        "        mapOf(",
    ]
    for pack, _, language, voice, quality, licence, base, *_ in voices:
        safe = licence.replace("\\", "\\\\").replace("\"", "\\\"")
        lines.extend(voice_entry(language, pack, voice, quality, safe))
    lines += ["        )", "}", ""]
    return "\n".join(lines)


def pack_json(pack, kind, language, voice, files):
    return json.dumps(
        {"format": 1, "id": pack, "kind": kind, "language": language, "voice": voice, "version": 1,
         "sha256": {name: files[name][0] for name in sorted(files)}},
        indent=2, ensure_ascii=False,
    ) + "\n"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    languages, packs = rows()
    outputs = {TSV: tsv_text(packs), KOTLIN: kotlin_text(languages, packs)}
    for pack, kind, language, voice, quality, licence, base, *_, files in packs:
        outputs[os.path.join(REPO, "packs", pack, "src", "main", "assets", pack, "pack.json")] = pack_json(pack, kind, language, voice, files)
        if kind == "voice":
            outputs[os.path.join(LIBRARIES, f"{pack.replace('_', '-')}.json")] = voice_library(pack, language, voice, licence, base)
    differ = []
    for path, text in outputs.items():
        current = open(path).read() if os.path.exists(path) else None
        if current != text:
            differ.append(os.path.relpath(path, REPO))
            if not args.check:
                os.makedirs(os.path.dirname(path), exist_ok=True)
                with open(path, "w") as out:
                    out.write(text)
    if args.check:
        if differ:
            sys.exit("Out of date: " + ", ".join(differ))
        print("Up to date.")
    else:
        voices = [p for p in packs if p[1] == "voice"]
        print(f"{len(voices)} voices, recognition for {sum(1 for l in languages if WHISPER_CODE.get(l, l) in WHISPER)} languages; changed: {len(differ)} files")


if __name__ == "__main__":
    main()

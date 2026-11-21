#!/usr/bin/env bash
# Fills the app's speech packs (spec 082) from sherpa-onnx's GitHub releases, as packs/speech.tsv pins them: each
# archive by SHA-256, then each file it ships by SHA-256 (espeak-ng's data by its archive's), so the same command always
# gives the same packs. Each pack's pack.json is committed (scripts/update-speech-catalog.py writes it); only the
# model files are fetched (git-ignored). Archives are kept in build/speech-catalog/archives, shared with the catalog
# script, so nothing is downloaded twice.
#
# Usage:  scripts/fetch-speech-packs.sh                       Whisper, espeak-ng's data and the pt and es voices
#         scripts/fetch-speech-packs.sh --languages ar,fr      Whisper, espeak-ng's data and these voices
#         scripts/fetch-speech-packs.sh --all                  every pack (a release bundle needs them all)
#
# This runs on the developer's machine at build time, never in the app.
set -euo pipefail

REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CATALOG="$REPO/packs/speech.tsv"
ARCHIVES="$REPO/build/speech-catalog/archives"
DOWNLOAD="https://github.com/k2-fsa/sherpa-onnx/releases/download"

say() { printf '==> %s\n' "$*"; }
fail() {
    printf 'error: %s\n' "$*" >&2
    exit 1
}

LANGUAGES="pt,es"
while [ $# -gt 0 ]; do
    case "$1" in
    --languages)
        LANGUAGES=$2
        shift
        ;;
    --all) LANGUAGES=all ;;
    *) fail "unknown option $1" ;;
    esac
    shift
done
[ -f "$CATALOG" ] || fail "no $CATALOG; run scripts/update-speech-catalog.py"

wanted() { # pack kind language
    [ "$2" != voice ] || [ "$LANGUAGES" = all ] || [[ ",$LANGUAGES," == *",$3,"* ]]
}

archive() { # name sha256: the archive in the shared folder, checked
    local name=$1 sha=$2 path="$ARCHIVES/$1" release=tts-models
    [[ $name == *whisper* ]] && release=asr-models
    mkdir -p "$ARCHIVES"
    if [ ! -f "$path" ] || [ "$(sha256sum "$path" | cut -d' ' -f1)" != "$sha" ]; then
        say "downloading $name"
        curl -sSfL -A "offline-translator-fetch" -o "$path.part" "$DOWNLOAD/$release/$name"
        mv "$path.part" "$path"
    fi
    [ "$(sha256sum "$path" | cut -d' ' -f1)" = "$sha" ] || fail "$name doesn't match its SHA-256"
    printf '%s' "$path"
}

fill() { # pack kind archive sha files hashes
    local pack=$1 kind=$2 path assets top i
    path="$(archive "$3" "$4")"
    assets="$REPO/packs/$pack/src/main/assets/$pack"
    # head stops reading early, which tar sees as a broken pipe: not an error here.
    top="$(tar -tjf "$path" | head -1 | cut -d/ -f1 || true)"
    if [ "$kind" = voice-data ]; then
        # Its one folder, espeak-ng-data/, as the voices read it; the archive's hash pins every file inside.
        rm -rf "$assets/espeak-ng-data"
        tar -xjf "$path" --no-same-owner -C "$assets"
        return
    fi
    IFS=, read -r -a names <<<"$5"
    IFS=, read -r -a hashes <<<"$6"
    for i in "${!names[@]}"; do
        local file="$assets/${names[$i]}"
        if [ ! -f "$file" ] || [ "$(sha256sum "$file" | cut -d' ' -f1)" != "${hashes[$i]}" ]; then
            tar -xjf "$path" --no-same-owner -O "$top/${names[$i]}" >"$file.part"
            mv "$file.part" "$file"
        fi
        [ "$(sha256sum "$file" | cut -d' ' -f1)" = "${hashes[$i]}" ] || fail "$pack/${names[$i]} doesn't match its SHA-256"
    done
}

count=0
while IFS=$'\t' read -r pack kind language _voice _quality _licence _base name sha files hashes _bytes; do
    [[ $pack == \#* || $pack == pack ]] && continue
    wanted "$pack" "$kind" "$language" || continue
    say "$pack"
    fill "$pack" "$kind" "$name" "$sha" "$files" "$hashes"
    count=$((count + 1))
done <"$CATALOG"
say "$count packs filled"

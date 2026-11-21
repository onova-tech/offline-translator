#!/usr/bin/env bash
# Fills the Translator's language packs (spec 012 research R6, spec 080 research R2) with Mozilla's models, MPL 2.0,
# from its model registry, as packs/languages.tsv pins them: every file by path and SHA-256, so the same command
# always gives the same packs; a file already in place with the right hash isn't downloaded again. Each pack's
# pack.json is committed (scripts/update-language-catalog.py writes it); only the models are fetched (git-ignored).
#
# Usage:  scripts/fetch-language-packs.sh                      Portuguese and Spanish, what the tests use
#         scripts/fetch-language-packs.sh --languages ja,fr     these languages
#         scripts/fetch-language-packs.sh --all                 every language (a release bundle needs them all)
#         scripts/fetch-language-packs.sh --all --into DIR      into DIR/<pack>/ instead, outside the build: a debug
#                                                               bundle takes every pack that holds models, so the
#                                                               engine's check of every language (ci.sh engine --all)
#                                                               keeps its copies here
#
# This runs on the developer's machine at build time, never in the app.
set -euo pipefail

REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BASE="https://storage.googleapis.com/moz-fx-translations-data--303e-prod-translations-data"
CATALOG="$REPO/packs/languages.tsv"

say() { printf '==> %s\n' "$*"; }
fail() {
    printf 'error: %s\n' "$*" >&2
    exit 1
}

LANGUAGES="pt,es"
INTO=""
while [ $# -gt 0 ]; do
    case "$1" in
    --languages)
        LANGUAGES=$2
        shift
        ;;
    --all) LANGUAGES=all ;;
    --into)
        INTO=$2
        shift
        ;;
    -h | --help)
        sed -n '2,16p' "$0" | sed 's/^# \{0,1\}//'
        exit 0
        ;;
    *) fail "unknown option: $1 (see --help)" ;;
    esac
    shift
done

# Downloads one .gz file of the registry into $2, unpacked, unless it is already there with the hash $3.
fetch() {
    local url="$BASE/$1" target="$2" hash="$3"
    if [ -f "$target" ] && echo "$hash  $target" | sha256sum -c --quiet - 2>/dev/null; then return; fi
    curl -sSfL "$url" | gunzip >"$target.part"
    echo "$hash  $target.part" | sha256sum -c --quiet - || {
        rm -f "$target.part"
        fail "$(basename "$target") doesn't match its pinned hash"
    }
    mv "$target.part" "$target"
}

wanted() { [ "$LANGUAGES" = all ] || [[ ",$LANGUAGES," == *",$1,"* ]]; }

done_packs=()
while IFS=$'\t' read -r code pair _quality _tag _speech folder model model_sha lex lex_sha vocabs vocab_shas; do
    case "$code" in "#"* | code | "") continue ;; esac
    wanted "$code" || continue
    pack="translate_$code"
    root="$REPO/packs/$pack/src/main/assets/$pack"
    [ -f "$root/pack.json" ] || fail "$pack has no pack.json: run scripts/update-language-catalog.py"
    if [ -n "$INTO" ]; then
        mkdir -p "$INTO/$pack"
        cp "$root/pack.json" "$INTO/$pack/pack.json"
        root="$INTO/$pack"
    fi
    say "$pack: $pair"
    mkdir -p "$root/$pair"
    fetch "$folder/$model.gz" "$root/$pair/$model" "$model_sha"
    fetch "$folder/$lex.gz" "$root/$pair/$lex" "$lex_sha"
    IFS=',' read -r -a names <<<"$vocabs"
    IFS=',' read -r -a hashes <<<"$vocab_shas"
    for i in "${!names[@]}"; do
        fetch "$folder/${names[$i]}.gz" "$root/$pair/${names[$i]}" "${hashes[$i]}"
    done
    [[ " ${done_packs[*]-} " == *" $pack "* ]] || done_packs+=("$pack")
done <"$CATALOG"

[ ${#done_packs[@]} -gt 0 ] || fail "no language matches '$LANGUAGES' in packs/languages.tsv"
for pack in "${done_packs[@]}"; do
    place="$REPO/packs/$pack/src/main/assets/$pack"
    [ -z "$INTO" ] || place="$INTO/$pack"
    say "$pack: $(du -sh "$place" | cut -f1)"
done

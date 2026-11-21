#!/usr/bin/env bash
# Builds libbergamot.so for the Translator (spec 012 research R3, task T027): Mozilla's Bergamot from
# mozilla/translations (folder inference/, MPL 2.0) at a pinned commit, with our C engine and JNI layer
# (core/translation/native), for arm64-v8a (phones, Ruy) and x86_64 (the test emulator, intgemm). The results are
# committed under core/translation/src/androidMain/jniLibs/, so Gradle and CI never compile C++.
#
# Usage:  scripts/build-bergamot-android.sh [arm64-v8a|x86_64 ...]     (both when none is named)
#
# Stop Gradle first: on the 2-core, 7 GB development machine the two together run out of memory. One ABI takes about
# an hour there. The sources and build folders go to $BERGAMOT_WORK (default ~/.cache/offline-translator/bergamot), outside
# the repository. Nothing here downloads at build time without a pinned commit or a checked hash.
set -euo pipefail

REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORK="${BERGAMOT_WORK:-$HOME/.cache/offline-translator/bergamot}"
SDK="${ANDROID_HOME:-$HOME/Android/Sdk}"

# Pinned versions: change them together with core/translation/NATIVE.md.
TRANSLATIONS_URL="https://github.com/mozilla/translations.git"
TRANSLATIONS_COMMIT="8eada038fd45bbf6fa58a412b22f80c79887fabc" # 2026-10-29, BERGAMOT_VERSION v0.6.0
NDK_VERSION="30.0.16248370"
CMAKE_VERSION="4.1.2"
MIN_API=26
PCRE2_VERSION="10.39" # what ssplit-cpp asks for; fetched here and checked, instead of by CMake unchecked
PCRE2_URL="https://github.com/PhilipHazel/pcre2/releases/download/pcre2-${PCRE2_VERSION}/pcre2-${PCRE2_VERSION}.tar.gz"
PCRE2_SHA256="0781bd2536ef5279b1943471fdcdbd9961a2845e1d2c9ad849b9bd98ba1a9bd4"

NDK="$SDK/ndk/$NDK_VERSION"
CMAKE_BIN="$SDK/cmake/$CMAKE_VERSION/bin"
STRIP="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip"
SRC="$WORK/translations"
JOBS="${BERGAMOT_JOBS:-2}"
# PCRE2's own configure step (an ExternalProject of ssplit-cpp) names no generator: CMake then takes this one, and
# finds the SDK's ninja on the PATH, so `make` needn't be installed.
export CMAKE_GENERATOR=Ninja
export PATH="$CMAKE_BIN:$PATH"

ABIS=("$@")
if [ ${#ABIS[@]} -eq 0 ]; then ABIS=(arm64-v8a x86_64); fi

say() { printf '\n==> %s\n' "$*"; }
fail() {
    printf 'error: %s\n' "$*" >&2
    exit 1
}

[ -d "$NDK" ] || fail "NDK $NDK_VERSION is missing: sdkmanager \"ndk;$NDK_VERSION\""
[ -x "$CMAKE_BIN/cmake" ] || fail "CMake $CMAKE_VERSION is missing: sdkmanager \"cmake;$CMAKE_VERSION\""
if pgrep -f '[G]radleDaemon' >/dev/null; then
    say "warning: a Gradle daemon is running; with both, this machine may run out of memory"
fi

fetch_sources() {
    say "sources: mozilla/translations at $TRANSLATIONS_COMMIT"
    if [ ! -d "$SRC/.git" ]; then
        mkdir -p "$SRC"
        git -C "$SRC" init -q
        git -C "$SRC" remote add origin "$TRANSLATIONS_URL"
        git -C "$SRC" sparse-checkout set inference
    fi
    if [ "$(git -C "$SRC" rev-parse -q --verify HEAD 2>/dev/null)" != "$TRANSLATIONS_COMMIT" ]; then
        git -C "$SRC" fetch -q --depth 1 origin "$TRANSLATIONS_COMMIT"
        git -C "$SRC" checkout -q --force FETCH_HEAD
    fi
    # Only what the engine needs; emsdk, nccl, fbgemm and the others are left out.
    local modules=(
        inference/3rd_party/ssplit-cpp
        inference/marian-fork/src/3rd_party/sentencepiece
        inference/marian-fork/src/3rd_party/intgemm
        inference/marian-fork/src/3rd_party/simd_utils
    )
    local ruy=inference/marian-fork/src/3rd_party/ruy
    # Marian's CMakeLists runs `git submodule update --init --recursive` whatever GIT_SUBMODULE says, which would
    # clone every submodule of the repository (gigabytes). Git skips a submodule whose update mode is "none".
    local key path name
    while read -r key path; do
        name="${key#submodule.}"
        name="${name%.path}"
        case " ${modules[*]} $ruy " in
            *" $path "*) ;;
            *) git -C "$SRC" config "submodule.$name.update" none ;;
        esac
    done < <(git -C "$SRC" config -f .gitmodules --get-regexp '^submodule\..*\.path$')
    git -C "$SRC" submodule update -q --init --depth 1 "${modules[@]}" ||
        git -C "$SRC" submodule update -q --init "${modules[@]}"
    # Ruy brings cpuinfo, which it needs to pick its kernels.
    git -C "$SRC" submodule update -q --init --depth 1 --recursive "$ruy" ||
        git -C "$SRC" submodule update -q --init --recursive "$ruy"

    # The one source patch (listed in NATIVE.md): newer clang refuses an enum out of range in a constexpr, and no flag
    # turns that back into a warning. The value is only compared at run time, so `const` does; upstream SentencePiece
    # made the same change.
    sed -i 's/constexpr unicode_script::ScriptType kAnyType =/const unicode_script::ScriptType kAnyType =/' \
        "$SRC/inference/marian-fork/src/3rd_party/sentencepiece/src/trainer_interface.cc"

    local pcre_dir="$SRC/inference/3rd_party/ssplit-cpp/src/3rd-party"
    if [ ! -f "$pcre_dir/pcre2-$PCRE2_VERSION/configure" ]; then
        say "sources: PCRE2 $PCRE2_VERSION"
        mkdir -p "$pcre_dir"
        local tarball="$WORK/pcre2-$PCRE2_VERSION.tar.gz"
        curl -sSfL -o "$tarball" "$PCRE2_URL"
        echo "$PCRE2_SHA256  $tarball" | sha256sum -c --quiet - || fail "PCRE2's tarball doesn't match its hash"
        tar -xzf "$tarball" -C "$pcre_dir"
    fi
}

build_abi() {
    local abi="$1" arch extra=()
    case "$abi" in
        arm64-v8a) arch="armv8-a" ;;
        # Android's x86_64 has no BLAS: Ruy does the float products there too, intgemm the int8 ones.
        x86_64) arch="x86-64" extra=(-DUSE_RUY=ON -DUSE_RUY_SGEMM=ON) ;;
        *) fail "unknown ABI $abi (arm64-v8a or x86_64)" ;;
    esac
    local build="$WORK/build-$abi"
    say "$abi: configure"
    # CMake 4 refuses projects that ask for a version below 3.5 (some third-party ones do): the environment variable
    # reaches PCRE2's own configure step too.
    CMAKE_POLICY_VERSION_MINIMUM=3.5 "$CMAKE_BIN/cmake" -S "$REPO/core/translation/native" -B "$build" -G Ninja \
        -DCMAKE_MAKE_PROGRAM="$CMAKE_BIN/ninja" \
        -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
        -DANDROID_ABI="$abi" -DANDROID_PLATFORM="android-$MIN_API" -DANDROID_STL=c++_static \
        -DCMAKE_BUILD_TYPE=Release -DCMAKE_POLICY_VERSION_MINIMUM=3.5 \
        -DBERGAMOT_SOURCE="$SRC/inference" -DGIT_SUBMODULE=OFF \
        -DBUILD_ARCH="$arch" -DCOMPILE_CUDA=OFF -DUSE_MKL=OFF -DUSE_DOXYGEN=OFF -DUSE_FBGEMM=OFF \
        -DCOMPILE_TESTS=OFF -DCOMPILE_UNIT_TESTS=OFF -DCOMPILE_LIBRARY_ONLY=ON -DUSE_STATIC_LIBS=ON \
        -DSSPLIT_USE_INTERNAL_PCRE2=ON -DSSPLIT_COMPILE_LIBRARY_ONLY=ON \
        "${extra[@]}"
    say "$abi: build (-j $JOBS)"
    CMAKE_POLICY_VERSION_MINIMUM=3.5 "$CMAKE_BIN/cmake" --build "$build" --target bergamot -j "$JOBS"
    local out="$REPO/core/translation/src/androidMain/jniLibs/$abi"
    mkdir -p "$out"
    "$STRIP" --strip-unneeded -o "$out/libbergamot.so" "$build/libbergamot.so"
}

third_party() {
    say "third-party code inside libbergamot.so (for the Open-source licenses page)"
    local root="$SRC/inference" dir license
    for dir in . marian-fork marian-fork/src/3rd_party/sentencepiece marian-fork/src/3rd_party/intgemm \
        marian-fork/src/3rd_party/ruy marian-fork/src/3rd_party/ruy/third_party/cpuinfo \
        marian-fork/src/3rd_party/simd_utils marian-fork/src/3rd_party/yaml-cpp marian-fork/src/3rd_party/pathie-cpp \
        marian-fork/src/3rd_party/spdlog marian-fork/src/3rd_party/zlib 3rd_party/ssplit-cpp \
        "3rd_party/ssplit-cpp/src/3rd-party/pcre2-$PCRE2_VERSION"; do
        license="$(find "$root/$dir" -maxdepth 1 \( -iname 'licen[cs]e*' -o -iname 'copying*' \) 2>/dev/null | head -1)"
        printf '%-55s %s\n' "$dir" "${license:+$(head -c 200 "$license" | tr -s '\n ' ' ' | cut -c1-80)}"
    done
}

fetch_sources
for abi in "${ABIS[@]}"; do build_abi "$abi"; done

say "results (copy into core/translation/NATIVE.md)"
for abi in "${ABIS[@]}"; do
    file="core/translation/src/androidMain/jniLibs/$abi/libbergamot.so"
    printf '| %s | `%s` | %s | %s, NDK %s |\n' "$abi" "${file#core/translation/}" \
        "$(sha256sum "$REPO/$file" | cut -d' ' -f1)" "${TRANSLATIONS_COMMIT:0:12}" "$NDK_VERSION"
done
third_party

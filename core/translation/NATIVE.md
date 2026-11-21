# The Translator's native engine (spec 012 research R3)

`libbergamot.so` is Mozilla's Bergamot (`mozilla/translations`, folder `inference/`, MPL 2.0) with our C engine
(`native/bergamot_engine.{h,cpp}`, four C functions that iPhone's cinterop can call later) and our JNI layer
(`native/bergamot_jni.cpp`). It is built by `scripts/build-bergamot-android.sh`. Gradle never compiles it: the files
below are committed, so `./gradlew check` and GitHub Actions don't need the NDK.

The JNI functions carry the Kotlin class's package in their names, so a package rename needs a rebuild too
(`NativeNamesTest` checks it; relinked for `tech.onova.offlinetranslator` on 2026-11-21).

To rebuild: install the NDK and CMake named below with `sdkmanager`, stop Gradle (the machine has 2 cores and 7 GB),
then run the script. It prints the table below for the new files.

| ABI | File | SHA-256 | Built from |
|---|---|---|---|
| arm64-v8a | `src/androidMain/jniLibs/arm64-v8a/libbergamot.so` (8.2 MB) | `24afb2de00392bdf43ac651cd08e29c5de634ecab748f685b099ba5f64d28a5e` | `8eada038fd45`, NDK 30.0.16248370, 2026-11-17 |
| x86_64 | `src/androidMain/jniLibs/x86_64/libbergamot.so` (8.5 MB) | `8bc6f6664b6d000864a74d4e5bc7413b9912a6be9de48710caeec8cc9accc658` | `8eada038fd45`, NDK 30.0.16248370, 2026-11-17 |

| Tool | Version |
|---|---|
| NDK | 30.0.16248370 (`sdkmanager "ndk;30.0.16248370"`, the latest stable on 2026-11-17) |
| CMake | 4.1.2 (`sdkmanager "cmake;4.1.2"`), with its own ninja |
| `mozilla/translations` commit | `8eada038fd45bbf6fa58a412b22f80c79887fabc` (2026-10-29, BERGAMOT_VERSION v0.6.0) |
| PCRE2 (for ssplit-cpp) | 10.39, tarball checked against SHA-256 `0781bd25…a9bd4` |

## How it is built

- `native/CMakeLists.txt` adds `inference/` as a subdirectory and links the two C++ files into one shared library
  with `bergamot-translator-source`. `ANDROID_STL=c++_static` and API 26, the app's minSdk. Only the JNI functions
  are exported (`--exclude-libs,ALL`), and the result is stripped.
- **arm64-v8a** uses Ruy for every matrix product (Marian's default on ARM). **x86_64** uses intgemm for the int8
  products and Ruy for the float ones (`USE_RUY_SGEMM`), since Android has no BLAS.
- `BUILD_ARCH` is `armv8-a` or `x86-64` instead of `native`, which would describe the build machine.
- Only the submodules the engine needs are fetched: ssplit-cpp, sentencepiece, intgemm, ruy (with cpuinfo) and
  simd_utils. Marian's CMakeLists runs `git submodule update --init --recursive` on its own, so every other
  submodule gets `update = none`.
- Marian builds with `-Werror`. The wrapper adds `-Wno-error`, since this NDK's clang is newer than the code.
- Marian sets its CPU switches (`ARM`, `FMA`, `SSE`, `COMPILE_CPU`…) and `-flax-vector-conversions` for its own
  folder only, but Bergamot's sources and ours include its headers too. The wrapper copies them onto both targets.
  Upstream builds native code only for x86, where those headers don't need them.
- On x86_64, faiss gets `-include immintrin.h`: it uses SSE intrinsics but includes their header only on Apple, and
  the NDK's libc++ doesn't bring it in by chance as glibc does.
- pathie-cpp converts file names with iconv, which Android has only from API 28. The wrapper defines
  `PATHIE_ASSUME_UTF8_ON_UNIX` (Android file names are UTF-8) and `__ANDROID_UNAVAILABLE_SYMBOLS_ARE_WEAK__`, so the
  unused reference doesn't stop the library from loading on Android 8.
- **One source patch**, applied by the script: SentencePiece's trainer has `constexpr ScriptType kAnyType =
  static_cast<ScriptType>(-1)`, which newer clang refuses with no flag to soften it. It becomes `const`, the change
  upstream SentencePiece made too. The trainer is linked in but never runs during translation.
- The engine sets `marian::setThrowExceptionOnAbort(true)`, so a bad model is an error the app shows, never a crash.

Both need only Android's own libraries (`liblog`, `libandroid`, `libdl`, `libm`, `libc`), align their segments to
16 KB (Google Play's rule for Android 15 phones), and export only the three JNI functions and the four C ones.
`iconv*` and `glob` are weak references, so the library loads on Android 8 (API 26).

## Third-party code inside the library

The script lists the folders it built from; the table adds what that listing can't see. It was checked against the
build's own record on 2026-11-18: every file `compile_commands.json` lists and every header in ninja's deps log
(`ninja -C $BERGAMOT_WORK/build-<abi> -t deps`). Each one is also an entry of the Open-source licenses page:
`config/libraries/libraries/*.json`, with its own license text, copyright line included, written by
`python3 -I scripts/collect-engine-licenses.py` from these same sources into `config/libraries/licenses/`. Run it again
after rebuilding the engine.

| Code | License | Where |
|---|---|---|
| Bergamot translator (`inference/` of `mozilla/translations`) | MPL-2.0 | both |
| Marian (`marian-fork`) | MIT | both |
| SentencePiece | Apache-2.0 | both |
| Ruy, with cpuinfo | Apache-2.0; cpuinfo BSD-2-Clause | both |
| intgemm | MIT | x86_64 |
| simd_utils | BSD-2-Clause | arm64-v8a |
| faiss (a part of it, inside Marian) | MIT | both |
| yaml-cpp | MIT | both |
| pathie-cpp | BSD-2-Clause | both |
| spdlog (with fmt) | MIT | both |
| CLI11 | BSD-3-Clause | both |
| zlib | Zlib | both |
| ssplit-cpp | Apache-2.0 for the code; its `nonbreaking_prefixes` (LGPL 2.1) aren't shipped | both |
| PCRE2 10.39 | BSD-3-Clause with the PCRE2 exception | both |
| cnpy | MIT | both |
| phf | MIT | both |
| zstr (header-only) | MIT | both |
| mio (header-only) | MIT | both |
| half_float (header-only, from the Open Asset Import Library) | BSD-3-Clause | both |
| ThreadPool (`threadpool.h`) | Zlib | both |
| sse_mathfun (`sse_mathfun.h`) | Zlib | x86_64 |

Marian's `any_type.h` and `ExceptionWithCallStack.h` come under Marian's own MIT license. Its ONNX wrapper is built
only with `USE_ONNX`, which this build doesn't set, so nothing of it is in the library.

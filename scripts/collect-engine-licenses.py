#!/usr/bin/env python3
"""Writes the Open-source licenses page's entries for the Translator's engine (spec 012 T055).

Each project inside libbergamot.so gets its own license text, read from the very sources the engine was built from
(scripts/build-bergamot-android.sh keeps them in $BERGAMOT_WORK, default ~/.cache/offline-translator/bergamot), so the page
shows each project's own copyright notice, as MIT, BSD, ISC and zlib ask. Which projects are inside comes from the
build's own record of the files it compiled and the headers they included (core/translation/NATIVE.md).

Run with `python3 -I scripts/collect-engine-licenses.py` after rebuilding the engine. It writes
config/libraries/licenses/polytools-*.json; the libraries themselves are config/libraries/libraries/*.json.
"""

import json
import os
import re
import sys
import tarfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WORK = os.environ.get("BERGAMOT_WORK", os.path.expanduser("~/.cache/offline-translator/bergamot"))
SOURCES = os.path.join(WORK, "translations")
MARIAN = os.path.join(SOURCES, "inference", "marian-fork")
THIRD = os.path.join(MARIAN, "src", "3rd_party")
OUT = os.path.join(ROOT, "config", "libraries", "licenses")


def read(*parts):
    with open(os.path.join(*parts), encoding="utf-8") as file:
        return file.read().strip()


def header_comment(path, start, end):
    """The license block of a header: the lines from the one containing [start] to the one containing [end]."""
    lines = read(path).splitlines()
    first = next(i for i, line in enumerate(lines) if start in line)
    last = next(i for i, line in enumerate(lines[first:], first) if end in line)
    text = "\n".join(lines[first : last + 1])
    text = re.sub(r"^\s*(/\*+|\*+/|\*|//)\s?", "", text, flags=re.M)
    return text.strip()


def pcre2_licence():
    with tarfile.open(os.path.join(WORK, "pcre2-10.39.tar.gz")) as archive:
        return archive.extractfile("pcre2-10.39/LICENCE").read().decode("utf-8").strip()


# id: (license name, url, text)
LICENSES = {
    "bergamot": ("Mozilla Public License 2.0", "https://www.mozilla.org/MPL/2.0/", lambda: read(SOURCES, "LICENSE")),
    "marian": ("MIT License", "https://github.com/browsermt/marian-dev", lambda: read(MARIAN, "LICENSE.md")),
    "sentencepiece": ("Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0",
                      lambda: read(THIRD, "sentencepiece", "LICENSE")),
    "ruy": ("Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0", lambda: read(THIRD, "ruy", "LICENSE")),
    "cpuinfo": ("BSD 2-Clause License", "https://github.com/pytorch/cpuinfo",
                lambda: read(THIRD, "ruy", "third_party", "cpuinfo", "LICENSE")),
    "intgemm": ("MIT License", "https://github.com/kpu/intgemm", lambda: read(THIRD, "intgemm", "LICENSE")),
    "simd-utils": ("BSD 2-Clause License", "https://github.com/JishinMaster/simd_utils",
                   lambda: read(THIRD, "simd_utils", "LICENSE")),
    "faiss": ("MIT License", "https://github.com/facebookresearch/faiss", lambda: read(THIRD, "faiss", "LICENSE")),
    "yaml-cpp": ("MIT License", "https://github.com/jbeder/yaml-cpp", lambda: read(THIRD, "yaml-cpp", "LICENSE")),
    "pathie-cpp": ("BSD 2-Clause License", "https://github.com/Quintus/pathie-cpp",
                   lambda: read(THIRD, "pathie-cpp", "LICENSE")),
    "spdlog": ("MIT License", "https://github.com/gabime/spdlog", lambda: read(THIRD, "spdlog", "LICENSE")),
    "cli11": ("BSD 3-Clause License", "https://github.com/CLIUtils/CLI11", lambda: read(THIRD, "CLI", "LICENSE")),
    "zlib": ("zlib License", "https://zlib.net/zlib_license.html",
             lambda: header_comment(os.path.join(THIRD, "zlib", "zlib.h"), "Copyright (C)", "madler@alumni")),
    "ssplit-cpp": ("Apache License 2.0", "https://github.com/browsermt/ssplit-cpp",
                   lambda: read(SOURCES, "inference", "3rd_party", "ssplit-cpp", "LICENSE.md")),
    "pcre2": ("BSD 3-Clause License with the PCRE2 exception", "https://github.com/PCRE2Project/pcre2", pcre2_licence),
    "cnpy": ("MIT License", "https://github.com/rogersce/cnpy", lambda: read(THIRD, "cnpy", "LICENSE")),
    "phf": ("MIT License", "https://github.com/wahern/phf", lambda: read(THIRD, "phf", "LICENSE")),
    "zstr": ("MIT License", "https://github.com/mateidavid/zstr", lambda: read(THIRD, "zstr", "LICENSE")),
    "mio": ("MIT License", "https://github.com/mandreyel/mio", lambda: read(THIRD, "mio", "LICENSE")),
    "half-float": ("BSD 3-Clause License", "https://github.com/assimp/assimp",
                   lambda: header_comment(os.path.join(THIRD, "half_float", "umHalf.h"), "Copyright (c)",
                                          "OF THE POSSIBILITY OF SUCH DAMAGE")),
    "threadpool": ("zlib License", "https://github.com/progschj/ThreadPool",
                   lambda: header_comment(os.path.join(THIRD, "threadpool.h"), "Copyright (c)", "distribution.")),
    "sse-mathfun": ("zlib License", "http://gruntthepeon.free.fr/ssemath/",
                    lambda: header_comment(os.path.join(THIRD, "sse_mathfun.h"), "Copyright (C)", "this is the zlib")),
}


def main():
    if not os.path.isdir(THIRD):
        sys.exit(f"The engine's sources aren't in {WORK}: run scripts/build-bergamot-android.sh first.")
    os.makedirs(OUT, exist_ok=True)
    # Most of the app's Maven dependencies are under Apache 2.0, which the build lists offline without its text: the
    # standard text (the same in every project) fills it in.
    apache = {"content": read(THIRD, "sentencepiece", "LICENSE"), "hash": "Apache-2.0",
              "url": "https://www.apache.org/licenses/LICENSE-2.0", "name": "Apache License 2.0"}
    with open(os.path.join(OUT, "Apache-2.0.json"), "w", encoding="utf-8") as file:
        json.dump(apache, file, indent=2, ensure_ascii=False)
        file.write("\n")
    for key, (name, url, text) in LICENSES.items():
        content = text()
        entry = {"content": content, "hash": f"offlinetranslator-{key}", "url": url, "name": name}
        with open(os.path.join(OUT, f"offlinetranslator-{key}.json"), "w", encoding="utf-8") as file:
            json.dump(entry, file, indent=2, ensure_ascii=False)
            file.write("\n")
        print(f"{key}: {name}, {len(content)} characters")


if __name__ == "__main__":
    main()

// The C engine over Bergamot's BlockingService (see bergamot_engine.h).
#include "bergamot_engine.h"

#include <cstdlib>
#include <cstring>
#include <exception>
#include <map>
#include <memory>
#include <sstream>
#include <stdexcept>
#include <string>
#include <utility>
#include <vector>

#include "common/logging.h"
#include "translator/parser.h"
#include "translator/response.h"
#include "translator/response_options.h"
#include "translator/service.h"
#include "translator/translation_model.h"

using marian::bergamot::BlockingService;
using marian::bergamot::parseOptionsFromString;
using marian::bergamot::Response;
using marian::bergamot::ResponseOptions;
using marian::bergamot::TranslationModel;

namespace {

struct Direction {
  std::string model;
  std::string shortlist;
  std::string sourceVocab;
  std::string targetVocab;
};

// A YAML double-quoted scalar, so any folder name reads as one path.
std::string quoted(const std::string& path) {
  std::string out = "\"";
  for (char c : path) {
    if (c == '"' || c == '\\') out += '\\';
    out += c;
  }
  return out + "\"";
}

// Firefox's configuration for these models (wasm/tests/engine/translations-engine.worker.mjs), without alignment,
// which only HTML needs (contracts/pack-format.md, "Rules"). On ARM, Marian turns the "shift" off by itself.
std::string configFor(const Direction& d) {
  std::ostringstream yaml;
  yaml << "models:\n  - " << quoted(d.model) << "\n"
       << "vocabs:\n  - " << quoted(d.sourceVocab) << "\n  - " << quoted(d.targetVocab) << "\n"
       << "shortlist:\n  - " << quoted(d.shortlist) << "\n  - false\n"
       << "beam-size: 1\n"
       << "normalize: 1.0\n"
       << "word-penalty: 0\n"
       << "max-length-break: 128\n"
       << "mini-batch-words: 1024\n"
       << "workspace: 128\n"
       << "max-length-factor: 2.0\n"
       << "skip-cost: true\n"
       << "cpu-threads: 0\n"
       << "quiet: true\n"
       << "quiet-translation: true\n"
       << "gemm-precision: int8shiftAlphaAll\n";
  return yaml.str();
}

char* copyOf(const std::string& text) {
  char* out = static_cast<char*>(std::malloc(text.size() + 1));
  if (out != nullptr) std::memcpy(out, text.c_str(), text.size() + 1);
  return out;
}

void setError(char** error, const std::string& message) {
  if (error != nullptr) *error = copyOf(message);
}

}  // namespace

struct bergamot_engine {
  BlockingService service{BlockingService::Config{}};
  std::map<std::string, Direction> directions;
  std::map<std::string, std::shared_ptr<TranslationModel>> loaded;

  std::shared_ptr<TranslationModel> model(const std::string& name) {
    auto found = loaded.find(name);
    if (found != loaded.end()) return found->second;
    auto direction = directions.find(name);
    if (direction == directions.end()) throw std::runtime_error("No direction " + name);
    auto options = parseOptionsFromString(configFor(direction->second), /*validate=*/false);
    auto created = std::make_shared<TranslationModel>(options);
    loaded.emplace(name, created);
    return created;
  }
};

extern "C" bergamot_engine* bergamot_open(const bergamot_direction* directions, int count, char** error) {
  if (directions == nullptr || count <= 0) {
    setError(error, "No directions");
    return nullptr;
  }
  // Marian's ABORT ends the process unless told to throw; a bad model must be an error, never a crash of the app.
  marian::setThrowExceptionOnAbort(true);
  try {
    auto engine = std::make_unique<bergamot_engine>();
    for (int i = 0; i < count; ++i) {
      const bergamot_direction& d = directions[i];
      if (d.name == nullptr || d.model == nullptr || d.shortlist == nullptr || d.source_vocab == nullptr ||
          d.target_vocab == nullptr) {
        setError(error, "Incomplete direction");
        return nullptr;
      }
      engine->directions[d.name] = Direction{d.model, d.shortlist, d.source_vocab, d.target_vocab};
    }
    return engine.release();
  } catch (const std::exception& e) {
    setError(error, e.what());
    return nullptr;
  }
}

extern "C" char* bergamot_translate(bergamot_engine* engine, const char* first, const char* second, const char* text,
                                    char** error) {
  if (engine == nullptr || first == nullptr || text == nullptr) {
    setError(error, "Missing engine, direction or text");
    return nullptr;
  }
  try {
    std::vector<std::string> sources{std::string(text)};
    std::vector<ResponseOptions> options(1);
    std::vector<Response> responses =
        second == nullptr
            ? engine->service.translateMultiple(engine->model(first), std::move(sources), options)
            : engine->service.pivotMultiple(engine->model(first), engine->model(second), std::move(sources), options);
    if (responses.empty()) {
      setError(error, "No translation");
      return nullptr;
    }
    return copyOf(responses.front().target.text);
  } catch (const std::exception& e) {
    setError(error, e.what());
    return nullptr;
  }
}

extern "C" void bergamot_free(char* text) { std::free(text); }

extern "C" void bergamot_close(bergamot_engine* engine) { delete engine; }

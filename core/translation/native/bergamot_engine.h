// The Translator's engine as plain C (spec 012 research R3): Bergamot behind four functions, so Android's JNI layer
// (bergamot_jni.cpp) and, later, iPhone's cinterop call the same code. No function here opens a network connection.
#ifndef POLYTOOLS_BERGAMOT_ENGINE_H
#define POLYTOOLS_BERGAMOT_ENGINE_H

#ifdef __cplusplus
extern "C" {
#endif

typedef struct bergamot_engine bergamot_engine;

// One direction of a pack, such as "pt-en", with its files (contracts/pack-format.md). A pack with one vocabulary
// passes it as both vocabularies.
typedef struct {
  const char* name;
  const char* model;
  const char* shortlist;
  const char* source_vocab;
  const char* target_vocab;
} bergamot_direction;

// Registers the directions. A direction's model is loaded the first time it is used and kept until bergamot_close,
// so only the languages a person uses take memory. Returns NULL, with *error set (free it with bergamot_free), when a
// direction is incomplete.
bergamot_engine* bergamot_open(const bergamot_direction* directions, int count, char** error);

// Translates `text` with the direction `first`, then with `second` when it isn't NULL (through English). Returns the
// translation, to free with bergamot_free, or NULL with *error set. Not thread-safe: one caller at a time.
char* bergamot_translate(bergamot_engine* engine, const char* first, const char* second, const char* text,
                         char** error);

void bergamot_free(char* text);

// Frees the models and the engine. NULL is accepted.
void bergamot_close(bergamot_engine* engine);

#ifdef __cplusplus
}
#endif

#endif  // POLYTOOLS_BERGAMOT_ENGINE_H

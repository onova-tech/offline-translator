// Android's door to the C engine (bergamot_engine.h): the external functions of
// tech.onova.offlinetranslator.core.translation.BergamotNative. The text crosses as UTF-8 bytes both ways, since JNI's own
// "modified UTF-8" would garble characters outside the Basic Multilingual Plane, such as emoji.
#include <jni.h>

#include <string>
#include <vector>

#include "bergamot_engine.h"

namespace {

void throwIllegalState(JNIEnv* env, const char* message) {
  jclass type = env->FindClass("java/lang/IllegalStateException");
  if (type != nullptr) env->ThrowNew(type, message != nullptr ? message : "Translation engine error");
}

// Paths and direction names: ASCII in practice, read through JNI's own encoding.
std::string stringOf(JNIEnv* env, jstring value) {
  if (value == nullptr) return {};
  const char* chars = env->GetStringUTFChars(value, nullptr);
  std::string out = chars != nullptr ? chars : "";
  if (chars != nullptr) env->ReleaseStringUTFChars(value, chars);
  return out;
}

std::vector<std::string> stringsOf(JNIEnv* env, jobjectArray values) {
  std::vector<std::string> out;
  const jsize count = values != nullptr ? env->GetArrayLength(values) : 0;
  for (jsize i = 0; i < count; ++i) {
    auto value = static_cast<jstring>(env->GetObjectArrayElement(values, i));
    out.push_back(stringOf(env, value));
    env->DeleteLocalRef(value);
  }
  return out;
}

}  // namespace

extern "C" JNIEXPORT jlong JNICALL Java_tech_onova_offlinetranslator_core_translation_BergamotNative_open(
    JNIEnv* env, jclass, jobjectArray names, jobjectArray models, jobjectArray shortlists, jobjectArray sourceVocabs,
    jobjectArray targetVocabs) {
  const std::vector<std::string> n = stringsOf(env, names);
  const std::vector<std::string> m = stringsOf(env, models);
  const std::vector<std::string> s = stringsOf(env, shortlists);
  const std::vector<std::string> sv = stringsOf(env, sourceVocabs);
  const std::vector<std::string> tv = stringsOf(env, targetVocabs);
  if (m.size() != n.size() || s.size() != n.size() || sv.size() != n.size() || tv.size() != n.size()) {
    throwIllegalState(env, "Directions of different lengths");
    return 0;
  }
  std::vector<bergamot_direction> directions;
  for (size_t i = 0; i < n.size(); ++i) {
    directions.push_back({n[i].c_str(), m[i].c_str(), s[i].c_str(), sv[i].c_str(), tv[i].c_str()});
  }
  char* error = nullptr;
  bergamot_engine* engine = bergamot_open(directions.data(), static_cast<int>(directions.size()), &error);
  if (engine == nullptr) {
    throwIllegalState(env, error);
    bergamot_free(error);
    return 0;
  }
  return reinterpret_cast<jlong>(engine);
}

extern "C" JNIEXPORT jbyteArray JNICALL Java_tech_onova_offlinetranslator_core_translation_BergamotNative_translate(
    JNIEnv* env, jclass, jlong handle, jstring first, jstring second, jbyteArray text) {
  const std::string firstName = stringOf(env, first);
  const std::string secondName = stringOf(env, second);
  const jsize length = env->GetArrayLength(text);
  std::string source(static_cast<size_t>(length), '\0');
  env->GetByteArrayRegion(text, 0, length, reinterpret_cast<jbyte*>(source.data()));

  char* error = nullptr;
  char* translated = bergamot_translate(reinterpret_cast<bergamot_engine*>(handle), firstName.c_str(),
                                        second != nullptr ? secondName.c_str() : nullptr, source.c_str(), &error);
  if (translated == nullptr) {
    throwIllegalState(env, error);
    bergamot_free(error);
    return nullptr;
  }
  const std::string result(translated);
  bergamot_free(translated);
  jbyteArray out = env->NewByteArray(static_cast<jsize>(result.size()));
  if (out != nullptr) {
    env->SetByteArrayRegion(out, 0, static_cast<jsize>(result.size()), reinterpret_cast<const jbyte*>(result.data()));
  }
  return out;
}

extern "C" JNIEXPORT void JNICALL Java_tech_onova_offlinetranslator_core_translation_BergamotNative_close(JNIEnv*, jclass,
                                                                                                jlong handle) {
  bergamot_close(reinterpret_cast<bergamot_engine*>(handle));
}

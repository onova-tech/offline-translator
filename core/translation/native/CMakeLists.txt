# libbergamot.so for Android (spec 012 research R3): Mozilla's Bergamot (`inference/` of mozilla/translations, given
# as BERGAMOT_SOURCE) with our C engine and JNI layer. Run only by scripts/build-bergamot-android.sh, never by Gradle.
cmake_minimum_required(VERSION 3.22)
project(polytools_bergamot CXX C)

set(BERGAMOT_SOURCE "" CACHE PATH "The inference folder of mozilla/translations")
if(NOT EXISTS "${BERGAMOT_SOURCE}/CMakeLists.txt")
  message(FATAL_ERROR "BERGAMOT_SOURCE must name mozilla/translations' inference folder")
endif()

add_subdirectory(${BERGAMOT_SOURCE} bergamot EXCLUDE_FROM_ALL)

# Marian builds with -Werror, and this NDK's clang is newer than the one it was written for: its warnings stay
# warnings. Added after Marian's own options, so it wins.
function(offlinetranslator_no_werror directory)
  get_property(targets DIRECTORY ${directory} PROPERTY BUILDSYSTEM_TARGETS)
  foreach(target IN LISTS targets)
    get_target_property(type ${target} TYPE)
    if(NOT type STREQUAL "INTERFACE_LIBRARY" AND NOT type STREQUAL "UTILITY")
      target_compile_options(${target} PRIVATE -Wno-error)
    endif()
  endforeach()
  get_property(children DIRECTORY ${directory} PROPERTY SUBDIRECTORIES)
  foreach(child IN LISTS children)
    offlinetranslator_no_werror(${child})
  endforeach()
endfunction()
offlinetranslator_no_werror(${BERGAMOT_SOURCE})

# Pathie converts file names with iconv, which Android only has from API 28; our minSdk is 26. File names on Android
# are UTF-8, so the conversion is never needed: say so, and let the unused reference be weak.
target_compile_definitions(pathie-cpp PRIVATE PATHIE_ASSUME_UTF8_ON_UNIX __ANDROID_UNAVAILABLE_SYMBOLS_ARE_WEAK__)

# faiss uses SSE intrinsics but includes their header only on Apple; Linux desktop builds get it through glibc's
# headers by chance, the NDK's libc++ doesn't.
if(ANDROID_ABI STREQUAL "x86_64")
  target_compile_options(faiss PRIVATE -include immintrin.h)
endif()

add_library(bergamot SHARED bergamot_engine.cpp bergamot_jni.cpp)
set_target_properties(bergamot PROPERTIES CXX_STANDARD 17 CXX_STANDARD_REQUIRED ON)
target_link_libraries(bergamot PRIVATE bergamot-translator-source log)

# Marian sets its CPU switches (ARM, FMA, SSE, COMPILE_CPU, USE_INTGEMM…) and -flax-vector-conversions for its own
# folder only. Bergamot's sources and ours include its headers too, so they need the same; upstream only builds
# native code for x86, where those headers don't depend on them.
get_directory_property(marian_definitions DIRECTORY ${BERGAMOT_SOURCE}/marian-fork COMPILE_DEFINITIONS)
get_directory_property(marian_options DIRECTORY ${BERGAMOT_SOURCE}/marian-fork COMPILE_OPTIONS)
foreach(target bergamot-translator-source bergamot)
  target_compile_definitions(${target} PRIVATE ${marian_definitions})
  target_compile_options(${target} PRIVATE ${marian_options})
endforeach()
# Only the JNI functions are exported; the rest stays inside, which also lets the linker drop what isn't used.
target_link_options(bergamot PRIVATE -Wl,--gc-sections -Wl,--exclude-libs,ALL)

cmake_minimum_required(VERSION 3.22.1)
project(nanfeng_transcriber LANGUAGES CXX)

set(WHISPER_CPP_DIR "${CMAKE_CURRENT_LIST_DIR}/../../../../third_party/whisper.cpp")
set(BUILD_SHARED_LIBS OFF CACHE BOOL "Build dependencies statically" FORCE)
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "Disable whisper tests" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "Disable whisper examples" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "Disable whisper server" FORCE)
set(WHISPER_CURL OFF CACHE BOOL "Disable runtime model downloads in native code" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "Use Android pthreads instead of OpenMP" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "Keep a portable Android baseline" FORCE)

add_subdirectory("${WHISPER_CPP_DIR}" "${CMAKE_CURRENT_BINARY_DIR}/whisper.cpp")

add_library(nanfeng_transcriber SHARED nanzhufeng_transcriber_jni.cpp)

target_compile_features(nanfeng_transcriber PRIVATE cxx_std_17)
target_compile_options(nanfeng_transcriber PRIVATE -Wall -Wextra -Werror)
target_compile_definitions(nanfeng_transcriber PRIVATE WHISPER_PINNED_VERSION="v1.9.1")

find_library(log-lib log)
target_link_libraries(nanfeng_transcriber PRIVATE whisper ${log-lib})

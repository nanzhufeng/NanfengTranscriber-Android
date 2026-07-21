#include <jni.h>
#include <whisper.h>

#include <algorithm>
#include <atomic>
#include <cstdint>
#include <memory>
#include <mutex>
#include <sstream>
#include <string>
#include <unordered_map>

namespace {

std::mutex cancellation_mutex;
std::unordered_map<whisper_context *, std::shared_ptr<std::atomic_bool>> cancellation_tokens;

bool should_abort(void *user_data) {
    auto *cancelled = static_cast<std::atomic_bool *>(user_data);
    return cancelled != nullptr && cancelled->load(std::memory_order_relaxed);
}

void cancel_transcription(whisper_context *context) {
    std::lock_guard<std::mutex> lock(cancellation_mutex);
    const auto token = cancellation_tokens.find(context);
    if (token != cancellation_tokens.end()) {
        token->second->store(true, std::memory_order_relaxed);
    }
}

std::shared_ptr<std::atomic_bool> cancellation_token(whisper_context *context) {
    std::lock_guard<std::mutex> lock(cancellation_mutex);
    const auto existing = cancellation_tokens.find(context);
    if (existing != cancellation_tokens.end()) return existing->second;
    auto created = std::make_shared<std::atomic_bool>(false);
    cancellation_tokens[context] = created;
    return created;
}

void reset_cancellation(whisper_context *context) {
    cancellation_token(context)->store(false, std::memory_order_relaxed);
}

std::string json_escape(const char *text) {
    std::ostringstream output;
    for (const unsigned char character : std::string(text == nullptr ? "" : text)) {
        switch (character) {
            case '\\': output << "\\\\"; break;
            case '"': output << "\\\""; break;
            case '\b': output << "\\b"; break;
            case '\f': output << "\\f"; break;
            case '\n': output << "\\n"; break;
            case '\r': output << "\\r"; break;
            case '\t': output << "\\t"; break;
            default:
                if (character < 0x20) {
                    output << "\\u00";
                    constexpr char hex[] = "0123456789abcdef";
                    output << hex[(character >> 4) & 0x0f] << hex[character & 0x0f];
                } else {
                    output << character;
                }
        }
    }
    return output.str();
}

std::string error_json(const std::string &message) {
    return "{\"ok\":false,\"error\":\"" + json_escape(message.c_str()) + "\"}";
}

}  // namespace

extern "C" JNIEXPORT jstring JNICALL
Java_com_nanzhufeng_transcriber_engine_NativeWhisperBridge_nativeEngineStatus(
        JNIEnv *env,
        jobject /* this */) {
    std::string status = "whisper.cpp ";
    status += WHISPER_PINNED_VERSION;
    status += " 已接入 · ";
    status += whisper_print_system_info();
    return env->NewStringUTF(status.c_str());
}

extern "C" JNIEXPORT jlong JNICALL
Java_com_nanzhufeng_transcriber_engine_NativeWhisperBridge_nativeLoadModel(
        JNIEnv *env,
        jobject /* this */,
        jstring model_path,
        jboolean use_gpu) {
    if (model_path == nullptr) return 0;
    const char *path = env->GetStringUTFChars(model_path, nullptr);
    if (path == nullptr) return 0;

    whisper_context_params params = whisper_context_default_params();
    params.use_gpu = use_gpu == JNI_TRUE;
    whisper_context *context = whisper_init_from_file_with_params(path, params);
    env->ReleaseStringUTFChars(model_path, path);
    if (context != nullptr) cancellation_token(context);
    return reinterpret_cast<jlong>(context);
}

extern "C" JNIEXPORT void JNICALL
Java_com_nanzhufeng_transcriber_engine_NativeWhisperBridge_nativeFreeModel(
        JNIEnv * /* env */,
        jobject /* this */,
        jlong handle) {
    auto *context = reinterpret_cast<whisper_context *>(handle);
    if (context != nullptr) {
        cancel_transcription(context);
        whisper_free(context);
        std::lock_guard<std::mutex> lock(cancellation_mutex);
        cancellation_tokens.erase(context);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_nanzhufeng_transcriber_engine_NativeWhisperBridge_nativePrepareTranscription(
        JNIEnv * /* env */,
        jobject /* this */,
        jlong handle) {
    auto *context = reinterpret_cast<whisper_context *>(handle);
    if (context != nullptr) reset_cancellation(context);
}

extern "C" JNIEXPORT void JNICALL
Java_com_nanzhufeng_transcriber_engine_NativeWhisperBridge_nativeCancelTranscription(
        JNIEnv * /* env */,
        jobject /* this */,
        jlong handle) {
    auto *context = reinterpret_cast<whisper_context *>(handle);
    if (context != nullptr) cancel_transcription(context);
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_nanzhufeng_transcriber_engine_NativeWhisperBridge_nativeTranscribe(
        JNIEnv *env,
        jobject /* this */,
        jlong handle,
        jfloatArray samples,
        jstring language,
        jint requested_threads) {
    auto *context = reinterpret_cast<whisper_context *>(handle);
    if (context == nullptr) {
        const std::string result = error_json("模型会话无效");
        return env->NewStringUTF(result.c_str());
    }
    if (samples == nullptr || env->GetArrayLength(samples) == 0) {
        const std::string result = error_json("PCM 音频为空");
        return env->NewStringUTF(result.c_str());
    }

    const char *language_chars = nullptr;
    if (language != nullptr) language_chars = env->GetStringUTFChars(language, nullptr);

    jfloat *audio = env->GetFloatArrayElements(samples, nullptr);
    if (audio == nullptr) {
        if (language_chars != nullptr) env->ReleaseStringUTFChars(language, language_chars);
        const std::string result = error_json("无法读取 PCM 音频");
        return env->NewStringUTF(result.c_str());
    }

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    params.n_threads = std::clamp(static_cast<int>(requested_threads), 1, 12);
    params.translate = false;
    params.no_context = false;
    params.no_timestamps = false;
    params.single_segment = false;
    params.print_special = false;
    params.print_progress = false;
    params.print_realtime = false;
    params.print_timestamps = false;
    params.language = language_chars == nullptr ? "auto" : language_chars;

    auto cancelled = cancellation_token(context);
    params.abort_callback = should_abort;
    params.abort_callback_user_data = cancelled.get();

    const int transcription_code = whisper_full(
            context,
            params,
            audio,
            env->GetArrayLength(samples));

    env->ReleaseFloatArrayElements(samples, audio, JNI_ABORT);
    if (language_chars != nullptr) env->ReleaseStringUTFChars(language, language_chars);

    if (transcription_code != 0) {
        const std::string message = cancelled->load(std::memory_order_relaxed)
                ? "whisper.cpp 推理已取消"
                : "whisper.cpp 推理失败，错误码 " + std::to_string(transcription_code);
        const std::string result = error_json(message);
        return env->NewStringUTF(result.c_str());
    }

    const int language_id = whisper_full_lang_id(context);
    std::ostringstream json;
    json << "{\"ok\":true,\"language\":\""
         << json_escape(whisper_lang_str(language_id))
         << "\",\"segments\":[";
    const int segment_count = whisper_full_n_segments(context);
    for (int index = 0; index < segment_count; ++index) {
        if (index > 0) json << ',';
        json << "{\"startMillis\":" << whisper_full_get_segment_t0(context, index) * 10
             << ",\"endMillis\":" << whisper_full_get_segment_t1(context, index) * 10
             << ",\"text\":\"" << json_escape(whisper_full_get_segment_text(context, index))
             << "\"}";
    }
    json << "]}";
    return env->NewStringUTF(json.str().c_str());
}

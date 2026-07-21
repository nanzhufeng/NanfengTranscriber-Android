package com.nanzhufeng.transcriber.engine

object NativeWhisperBridge {
    init {
        System.loadLibrary("nanfeng_transcriber")
    }

    fun engineStatus(): String = nativeEngineStatus()

    fun loadModel(path: String, useGpu: Boolean): Long = nativeLoadModel(path, useGpu)

    fun freeModel(handle: Long) = nativeFreeModel(handle)

    fun cancelTranscription(handle: Long) = nativeCancelTranscription(handle)

    fun prepareTranscription(handle: Long) = nativePrepareTranscription(handle)

    fun transcribe(
        handle: Long,
        samples: FloatArray,
        language: String?,
        threads: Int,
    ): String = nativeTranscribe(handle, samples, language, threads)

    private external fun nativeEngineStatus(): String
    private external fun nativeLoadModel(path: String, useGpu: Boolean): Long
    private external fun nativeFreeModel(handle: Long)
    private external fun nativeCancelTranscription(handle: Long)
    private external fun nativePrepareTranscription(handle: Long)
    private external fun nativeTranscribe(
        handle: Long,
        samples: FloatArray,
        language: String?,
        threads: Int,
    ): String
}

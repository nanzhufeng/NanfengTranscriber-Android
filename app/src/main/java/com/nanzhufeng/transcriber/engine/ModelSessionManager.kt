package com.nanzhufeng.transcriber.engine

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Closeable
import java.nio.file.Path

class ModelSessionManager(
    private val engine: SpeechEngine,
) : Closeable {
    private val mutex = Mutex()
    @Volatile
    private var loaded: LoadedModel? = null

    suspend fun acquire(modelPath: Path): LoadedModel = acquireWithStatus(modelPath).model

    suspend fun acquireWithStatus(modelPath: Path): ModelAcquisition = mutex.withLock {
        val canonicalPath = modelPath.toRealPath()
        loaded?.takeIf { it.canonicalPath == canonicalPath }?.let {
            return@withLock ModelAcquisition(it, reused = true)
        }

        loaded?.let(engine::releaseModel)
        engine.loadModel(canonicalPath).also { loaded = it }.let {
            ModelAcquisition(it, reused = false)
        }
    }

    suspend fun releaseCurrent() = mutex.withLock {
        loaded?.let(engine::releaseModel)
        loaded = null
    }

    fun cancelCurrent() {
        loaded?.let(engine::cancel)
    }

    override fun close() {
        loaded?.let(engine::releaseModel)
        loaded = null
        engine.close()
    }
}

data class ModelAcquisition(
    val model: LoadedModel,
    val reused: Boolean,
)

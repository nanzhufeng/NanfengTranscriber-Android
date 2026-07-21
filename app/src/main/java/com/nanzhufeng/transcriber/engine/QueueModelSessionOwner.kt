package com.nanzhufeng.transcriber.engine

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Closeable
import java.nio.file.Path

/** Keeps one warm model session alive for a sequential transcription queue. */
class QueueModelSessionOwner(
    private val engineFactory: (threadCount: Int) -> SpeechEngine,
) : Closeable {
    private val mutex = Mutex()
    private var slot: SessionSlot? = null

    suspend fun acquire(modelPath: Path, threadCount: Int): QueueModelAcquisition = mutex.withLock {
        require(threadCount > 0) { "CPU 线程数必须大于 0" }
        var current = slot
        if (current == null || current.threadCount != threadCount) {
            current?.session?.close()
            val engine = engineFactory(threadCount)
            current = SessionSlot(
                threadCount = threadCount,
                engine = engine,
                session = ModelSessionManager(engine),
            )
            slot = current
        }

        val acquisition = current.session.acquireWithStatus(modelPath)
        QueueModelAcquisition(
            engine = current.engine,
            model = acquisition.model,
            reused = acquisition.reused,
        )
    }

    fun cancelCurrent() {
        slot?.session?.cancelCurrent()
    }

    override fun close() {
        slot?.session?.close()
        slot = null
    }

    private data class SessionSlot(
        val threadCount: Int,
        val engine: SpeechEngine,
        val session: ModelSessionManager,
    )
}

data class QueueModelAcquisition(
    val engine: SpeechEngine,
    val model: LoadedModel,
    val reused: Boolean,
)

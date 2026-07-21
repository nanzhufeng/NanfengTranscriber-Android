package com.nanzhufeng.transcriber.engine

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path

class QueueModelSessionOwnerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun sameModelAndThreadCountReuseOneWarmSession() = runBlocking {
        val engines = mutableListOf<RecordingSpeechEngine>()
        val owner = QueueModelSessionOwner { RecordingSpeechEngine().also(engines::add) }
        val model = modelFile("base.bin")

        val first = owner.acquire(model, threadCount = 4)
        val second = owner.acquire(model, threadCount = 4)

        assertFalse(first.reused)
        assertTrue(second.reused)
        assertEquals(1, engines.size)
        assertEquals(1, engines.single().loadedPaths.size)
        owner.close()
        assertEquals(1, engines.single().releasedCount)
        assertTrue(engines.single().closed)
    }

    @Test
    fun changingModelReleasesOldModelWithoutRecreatingEngine() = runBlocking {
        val engines = mutableListOf<RecordingSpeechEngine>()
        val owner = QueueModelSessionOwner { RecordingSpeechEngine().also(engines::add) }

        owner.acquire(modelFile("base.bin"), threadCount = 4)
        val acquisition = owner.acquire(modelFile("small.bin"), threadCount = 4)

        assertFalse(acquisition.reused)
        assertEquals(1, engines.size)
        assertEquals(2, engines.single().loadedPaths.size)
        assertEquals(1, engines.single().releasedCount)
        owner.close()
    }

    @Test
    fun changingThreadCountRecreatesEngineAndSession() = runBlocking {
        val engines = mutableListOf<RecordingSpeechEngine>()
        val owner = QueueModelSessionOwner { RecordingSpeechEngine().also(engines::add) }
        val model = modelFile("base.bin")

        owner.acquire(model, threadCount = 4)
        val acquisition = owner.acquire(model, threadCount = 6)

        assertFalse(acquisition.reused)
        assertEquals(2, engines.size)
        assertTrue(engines.first().closed)
        assertEquals(1, engines.first().releasedCount)
        owner.close()
    }

    private fun modelFile(name: String): Path = temporaryFolder.newFile(name).toPath()

    private class RecordingSpeechEngine : SpeechEngine {
        val loadedPaths = mutableListOf<Path>()
        var releasedCount = 0
        var closed = false

        override suspend fun loadModel(modelPath: Path): LoadedModel {
            loadedPaths.add(modelPath)
            return LoadedModel(modelPath, nativeHandle = loadedPaths.size.toLong())
        }

        override suspend fun transcribe(
            model: LoadedModel,
            samples: FloatArray,
            language: String?,
        ): EngineTranscript = EngineTranscript(language, emptyList())

        override fun releaseModel(model: LoadedModel) {
            releasedCount += 1
        }

        override fun close() {
            closed = true
        }
    }
}

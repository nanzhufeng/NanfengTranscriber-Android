package com.nanzhufeng.transcriber.data.modelstore

import com.nanzhufeng.transcriber.domain.model.ModelInstallState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

class FileModelStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun committedModelSurvivesStoreRecreationWithoutRedownload() {
        val root = temporaryFolder.newFolder("models").toPath()
        val bytes = "stable-whisper-model".toByteArray()
        val manifest = manifestFor(bytes)
        val firstStore = FileModelStore(root) { 1000L }

        Files.write(firstStore.partialPath(manifest), bytes, StandardOpenOption.TRUNCATE_EXISTING)
        assertEquals(ModelInstallState.READY, firstStore.commit(manifest).state)

        val recreatedStore = FileModelStore(root) { 2000L }
        val inspection = recreatedStore.inspect(manifest)
        assertEquals(ModelInstallState.READY, inspection.state)
        assertTrue(Files.exists(inspection.modelPath))
    }

    @Test
    fun partialDownloadIsPreservedForResume() {
        val root = temporaryFolder.newFolder("resume-models").toPath()
        val bytes = "complete-model".toByteArray()
        val manifest = manifestFor(bytes)
        val store = FileModelStore(root)
        val partial = store.partialPath(manifest)

        Files.write(partial, bytes.copyOfRange(0, 5), StandardOpenOption.TRUNCATE_EXISTING)
        val samePartial = store.partialPath(manifest)

        assertEquals(5L, Files.size(samePartial))
        assertEquals(ModelInstallState.DOWNLOADING, store.inspect(manifest).state)
    }

    @Test
    fun sameLengthCorruptionIsDetectedByFullVerification() {
        val root = temporaryFolder.newFolder("corrupt-models").toPath()
        val bytes = "trusted-model-data".toByteArray()
        val manifest = manifestFor(bytes)
        val store = FileModelStore(root)

        Files.write(store.partialPath(manifest), bytes, StandardOpenOption.TRUNCATE_EXISTING)
        val modelPath = requireNotNull(store.commit(manifest).modelPath)
        Files.write(
            modelPath,
            "x".repeat(bytes.size).toByteArray(),
            StandardOpenOption.TRUNCATE_EXISTING,
        )

        assertEquals(ModelInstallState.CORRUPT, store.inspect(manifest, fullHash = true).state)
    }

    @Test
    fun deletingOneModelCacheRemovesOnlyItsVersionDirectory() {
        val root = temporaryFolder.newFolder("delete-models").toPath()
        val bytes = "trusted-model-data".toByteArray()
        val manifest = manifestFor(bytes)
        val store = FileModelStore(root)
        Files.write(store.partialPath(manifest), bytes, StandardOpenOption.TRUNCATE_EXISTING)
        store.commit(manifest)

        assertTrue(store.cacheBytes(manifest) >= bytes.size)
        assertTrue(store.delete(manifest) >= bytes.size)
        assertEquals(0L, store.cacheBytes(manifest))
        assertEquals(ModelInstallState.NOT_INSTALLED, store.inspect(manifest).state)
    }

    private fun manifestFor(bytes: ByteArray): ModelManifest = ModelManifest(
        modelId = "tiny-test",
        version = "v1",
        expectedBytes = bytes.size.toLong(),
        sha256 = MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) },
        engineVersion = "test-engine",
    )
}

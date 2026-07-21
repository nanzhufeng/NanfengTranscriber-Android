package com.nanzhufeng.transcriber.data.modelstore

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

class ModelAssetManagerTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun rangeDownloadResumesExistingPartialAndCommitsOnce() = runBlocking {
        val bytes = "resumable-model-bytes".toByteArray()
        val manifest = manifestFor(bytes)
        val store = newStore("resume")
        Files.write(
            store.partialPath(manifest),
            bytes.copyOfRange(0, 5),
            StandardOpenOption.TRUNCATE_EXISTING,
        )
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setResponseCode(206)
                .setHeader("Content-Range", "bytes 5-${bytes.lastIndex}/${bytes.size}")
                .setBody(String(bytes.copyOfRange(5, bytes.size))),
        )
        server.start()
        try {
            val manager = ModelAssetManager(store, OkHttpClient())
            val progress = mutableListOf<ModelTransferProgress>()
            val result = manager.download(manifest, server.url("/model.bin").toString(), progress::add)

            assertTrue(result is ModelAssetResult.Installed)
            assertEquals("bytes=5-", server.takeRequest().getHeader("Range"))
            assertTrue(progress.any { it.stage == ModelTransferStage.VERIFYING })
            assertEquals(bytes.size.toLong(), progress.maxOf { it.completedBytes })
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun serverIgnoringRangeRestartsWithoutDuplicatingBytes() = runBlocking {
        val bytes = "server-full-response".toByteArray()
        val manifest = manifestFor(bytes)
        val store = newStore("restart")
        Files.write(
            store.partialPath(manifest),
            bytes.copyOfRange(0, 4),
            StandardOpenOption.TRUNCATE_EXISTING,
        )
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200).setBody(String(bytes)))
        server.start()
        try {
            val manager = ModelAssetManager(store, OkHttpClient())
            val result = manager.download(manifest, server.url("/model.bin").toString())

            assertTrue(result is ModelAssetResult.Installed)
            assertEquals(bytes.size.toLong(), Files.size((result as ModelAssetResult.Installed).modelPath))
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun hashMismatchReturnsStructuredIntegrityFailure() = runBlocking {
        val expected = "trusted-model-data".toByteArray()
        val wrong = "x".repeat(expected.size)
        val manifest = manifestFor(expected)
        val store = newStore("integrity")
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200).setBody(wrong))
        server.start()
        try {
            val manager = ModelAssetManager(store, OkHttpClient())
            val result = manager.download(manifest, server.url("/model.bin").toString())

            assertTrue(result is ModelAssetResult.Failure)
            result as ModelAssetResult.Failure
            assertEquals(ModelAssetFailureCategory.INTEGRITY, result.category)
            assertTrue(result.canRetry)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun importAndExportRoundTripUsesSameVerifiedModel() = runBlocking {
        val bytes = "portable-private-model".toByteArray()
        val manifest = manifestFor(bytes)
        val manager = ModelAssetManager(newStore("roundtrip"), OkHttpClient())

        val imported = manager.importModel(
            manifest = manifest,
            inputProvider = { ByteArrayInputStream(bytes) },
        )
        assertTrue(imported is ModelAssetResult.Installed)

        val exportedBytes = ByteArrayOutputStream()
        val exported = manager.exportModel(
            manifest = manifest,
            outputProvider = { exportedBytes },
        )
        assertTrue(exported is ModelAssetResult.Exported)
        assertArrayEquals(bytes, exportedBytes.toByteArray())
    }

    private fun newStore(name: String): FileModelStore =
        FileModelStore(temporaryFolder.newFolder(name).toPath())

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

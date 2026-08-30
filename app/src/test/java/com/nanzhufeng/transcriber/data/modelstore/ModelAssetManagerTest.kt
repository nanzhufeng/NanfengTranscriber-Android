package com.nanzhufeng.transcriber.data.modelstore

import com.nanzhufeng.transcriber.domain.model.ModelInstallState
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
import java.util.zip.ZipInputStream

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

    @Test
    fun multiFileModelDownloadsAndPortableBundleRoundTrips() = runBlocking {
        val modelBytes = "sensevoice-model".toByteArray()
        val tokensBytes = "tokens".toByteArray()
        val manifest = bundleManifest(modelBytes, tokensBytes)
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200).setBody(String(modelBytes)))
        server.enqueue(MockResponse().setResponseCode(200).setBody(String(tokensBytes)))
        server.start()
        try {
            val store = newStore("bundle-download")
            val manager = ModelAssetManager(store, OkHttpClient())
            val catalogModel = CatalogModel(
                displayName = "SenseVoice test",
                profile = ModelPerformanceProfile.BALANCED,
                provider = AsrProviderId.SENSEVOICE,
                manifest = manifest,
                downloads = listOf(
                    ModelDownload("model.int8.onnx", server.url("/model").toString()),
                    ModelDownload("tokens.txt", server.url("/tokens").toString()),
                ),
                capabilities = AsrCapabilities(false, false, false, false, true, 30_000L),
            )

            assertTrue(manager.download(catalogModel) is ModelAssetResult.Installed)
            assertEquals(ModelInstallState.READY, store.inspect(manifest, fullHash = true).state)

            val exported = ByteArrayOutputStream()
            assertTrue(
                manager.exportModel(manifest, outputProvider = { exported }) is
                    ModelAssetResult.Exported,
            )
            val entries = linkedMapOf<String, ByteArray>()
            ZipInputStream(ByteArrayInputStream(exported.toByteArray())).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    entries[entry.name] = zip.readBytes()
                }
            }
            assertArrayEquals(modelBytes, entries["model.int8.onnx"])
            assertArrayEquals(tokensBytes, entries["tokens.txt"])

            val importedManager = ModelAssetManager(newStore("bundle-import"), OkHttpClient())
            val imported = importedManager.importModel(
                manifest,
                inputProvider = { ByteArrayInputStream(exported.toByteArray()) },
            )
            assertTrue(imported is ModelAssetResult.Installed)
        } finally {
            server.shutdown()
        }
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

    private fun bundleManifest(model: ByteArray, tokens: ByteArray): ModelManifest = ModelManifest(
        modelId = "bundle-test",
        version = "v1",
        files = listOf(
            fileManifest("model.int8.onnx", model),
            fileManifest("tokens.txt", tokens),
        ),
        engineVersion = "test-sherpa",
        entryFile = "model.int8.onnx",
    )

    private fun fileManifest(path: String, bytes: ByteArray) = ModelFileManifest(
        relativePath = path,
        expectedBytes = bytes.size.toLong(),
        sha256 = MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) },
    )
}

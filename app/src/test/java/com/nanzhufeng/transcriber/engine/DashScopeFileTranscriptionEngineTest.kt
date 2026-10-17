package com.nanzhufeng.transcriber.engine

import com.nanzhufeng.transcriber.data.media.PcmAudioArtifact
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class DashScopeFileTranscriptionEngineTest {
    private fun policy(server: MockWebServer) = MockResponse().setBody(JSONObject().put("data", JSONObject()
        .put("upload_host", server.url("/upload").toString()).put("upload_dir", "temporary/model-bound")
        .put("oss_access_key_id", "upload-access").put("signature", "upload-signature")
        .put("policy", "policy-data").put("x_oss_object_acl", "private")
        .put("x_oss_forbid_overwrite", "true")).toString())

    private fun ready(server: MockWebServer) = MockResponse().setBody(
        """{"output":{"task_id":"remote-1","task_status":"SUCCEEDED","results":[{"subtask_status":"SUCCEEDED","transcription_url":"${server.url("/result")}"}]},"usage":{"duration":1,"input_tokens":50,"output_tokens":10}}""",
    )

    private fun engine(server: MockWebServer, model: FileTranscriptionModel, maxPolls: Int = 3) =
        DashScopeFileTranscriptionEngine(model,
            DashScopeFileTranscriptionClient({ "existing-key" }, OkHttpClient(), server.url("/api/v1/")),
            { "existing-key" }, pollIntervalMillis = 0, maxPolls = maxPolls)

    private fun result() = MockResponse().setBody(
        """{"transcripts":[{"channel_id":0,"sentences":[{"begin_time":100,"end_time":900,"text":"极氪 9X。"}]}]}""",
    )

    private suspend fun withSession(block: suspend (Path) -> Unit) {
        val directory = Files.createTempDirectory("filetrans-engine-test")
        val session = directory.resolve("model.session")
        Files.write(session, byteArrayOf(1))
        try { block(session) } finally {
            Files.list(directory).use { files -> files.forEach { Files.deleteIfExists(it) } }
            Files.deleteIfExists(directory)
        }
    }

    @Test
    fun bothModesUploadSubmitPollAndReadTimestampsUsingTheExistingKey() = runBlocking {
        FileTranscriptionModel.entries.forEach { model ->
            MockWebServer().use { server -> withSession { path ->
                server.enqueue(policy(server)); server.enqueue(MockResponse())
                server.enqueue(MockResponse().setBody("""{"output":{"task_id":"remote-1","task_status":"PENDING"}}"""))
                server.enqueue(MockResponse().setBody("""{"output":{"task_id":"remote-1","task_status":"RUNNING"}}"""))
                server.enqueue(ready(server)); server.enqueue(result())
                val engine = engine(server, model)
                val transcript = engine.transcribe(engine.loadModel(path), FloatArray(16_000), "zh")
                assertEquals("极氪 9X。", transcript.segments.single().text)
                assertEquals(100L, transcript.segments.single().startMillis)
                assertEquals(1_000L, transcript.invocationUsage?.billableAudioMillis)
                val uploadPolicy = server.takeRequest()
                assertTrue(uploadPolicy.path!!.contains("model=${model.modelId}"))
                assertEquals("Bearer existing-key", uploadPolicy.getHeader("Authorization"))
                val upload = server.takeRequest()
                assertNull(upload.getHeader("Authorization"))
                assertTrue(upload.body.readUtf8().contains("RIFF"))
                val submit = server.takeRequest()
                assertEquals("enable", submit.getHeader("X-DashScope-OssResourceResolve"))
                val payload = JSONObject(submit.body.readUtf8())
                assertEquals(model.modelId, payload.getString("model"))
                assertTrue(payload.getJSONObject("input").getJSONArray("file_urls").getString(0).startsWith("oss://temporary/model-bound/"))
                assertEquals("zh", payload.getJSONObject("parameters").getJSONArray("language_hints").getString(0))
                server.takeRequest(); server.takeRequest()
                assertNull(server.takeRequest().getHeader("Authorization"))
                val restarted = engine(server, model)
                val cached = restarted.transcribe(restarted.loadModel(path), FloatArray(16_000), "zh")
                assertEquals(transcript, cached)
                assertEquals(6, server.requestCount)
            } }
        }
    }

    @Test
    fun processRestartQueriesTheSavedTaskRatherThanUploadingOrSubmittingAgain() = runBlocking {
        MockWebServer().use { server -> withSession { path ->
            server.enqueue(policy(server)); server.enqueue(MockResponse())
            server.enqueue(MockResponse().setBody("""{"output":{"task_id":"remote-1","task_status":"PENDING"}}"""))
            server.enqueue(MockResponse().setBody("""{"output":{"task_id":"remote-1","task_status":"RUNNING"}}"""))
            val first = engine(server, FileTranscriptionModel.QWEN_AUDIO_31, maxPolls = 1)
            val failure = runCatching { first.transcribe(first.loadModel(path), FloatArray(16_000), null) }
                .exceptionOrNull() as EngineTranscriptionException
            assertEquals("FILETRANS_POLL_TIMEOUT", failure.errorCode)
            repeat(4) { server.takeRequest() }
            server.enqueue(ready(server)); server.enqueue(result())
            val resumed = engine(server, FileTranscriptionModel.QWEN_AUDIO_31)
            val transcript = resumed.transcribe(resumed.loadModel(path), FloatArray(16_000), null)
            assertEquals("极氪 9X。", transcript.segments.single().text)
            assertEquals("/api/v1/tasks/remote-1", server.takeRequest().path)
            assertEquals("/result", server.takeRequest().path)
            assertEquals(6, server.requestCount)
        } }
    }

    @Test
    fun noSpeechTailPreservesEarlierTextAndCommitsTheWholeAudioCheckpoint() = runBlocking {
        MockWebServer().use { server -> withSession { path ->
            server.enqueue(policy(server)); server.enqueue(MockResponse())
            server.enqueue(MockResponse().setBody("""{"output":{"task_id":"remote-1","task_status":"PENDING"}}"""))
            server.enqueue(MockResponse().setBody("""{"output":{"task_id":"remote-1","task_status":"FAILED","code":"SUCCESS_WITH_NO_VALID_FRAGMENT"}}"""))
            val pcm = path.resolveSibling("test.pcm")
            Files.write(pcm, ByteArray(96_000))
            val earlier = TranscriptSegment(0, 900, "前面已经转写的正文。")
            val engine = engine(server, FileTranscriptionModel.PARAFORMER_V2)
            var checkpoint: PcmTranscriptionCheckpoint? = null
            val result = PcmTranscriptionCoordinator(engine).transcribe(
                PcmAudioArtifact(pcm, 16_000, 1, 48_000, 3_000), engine.loadModel(path), null,
                resume = PcmTranscriptionResume(processedSamples = 16_000, segments = listOf(earlier)),
                onCheckpoint = { checkpoint = it },
            ) as PcmTranscriptionResult.Success
            assertEquals(listOf(earlier), result.transcript.segments)
            assertEquals(48_000L, result.processedSamples)
            assertEquals(48_000L, checkpoint?.processedSamples)
            assertEquals(listOf(earlier), checkpoint?.segments)
        } }
    }

    @Test
    fun paraformerNoSpeechTailCompletesAndSurvivesRestartWithoutASecondRequest() = runBlocking {
        MockWebServer().use { server -> withSession { path ->
            server.enqueue(policy(server)); server.enqueue(MockResponse())
            server.enqueue(MockResponse().setBody("""{"output":{"task_id":"remote-1","task_status":"PENDING"}}"""))
            server.enqueue(MockResponse().setBody("""{"output":{"task_id":"remote-1","task_status":"FAILED","code":"SUCCESS_WITH_NO_VALID_FRAGMENT"}}"""))
            val first = engine(server, FileTranscriptionModel.PARAFORMER_V2)
            val loaded = first.loadModel(path)
            first.setChunkPosition(loaded, 4_800_000)
            val transcript = first.transcribe(loaded, FloatArray(48_000), null)
            assertTrue(transcript.segments.isEmpty())
            val restarted = engine(server, FileTranscriptionModel.PARAFORMER_V2)
            val resumed = restarted.loadModel(path)
            restarted.setChunkPosition(resumed, 4_800_000)
            assertEquals(transcript, restarted.transcribe(resumed, FloatArray(48_000), null))
            assertEquals(4, server.requestCount)
        } }
    }

    @Test
    fun retainedNoSpeechFailureCanBeRecoveredLocallyWithoutResubmission() = runBlocking {
        MockWebServer().use { server -> withSession { path ->
            server.enqueue(policy(server)); server.enqueue(MockResponse())
            server.enqueue(MockResponse().setBody("""{"output":{"task_id":"remote-1","task_status":"PENDING"}}"""))
            server.enqueue(MockResponse().setBody("""{"output":{"task_id":"remote-1","task_status":"FAILED","code":"InvalidFile.DownloadFailed"}}"""))
            val first = engine(server, FileTranscriptionModel.PARAFORMER_V2)
            assertTrue(runCatching { first.transcribe(first.loadModel(path), FloatArray(48_000), null) }.isFailure)
            val receipt = Files.list(path.parent).use { paths ->
                paths.filter { it.fileName.toString().endsWith(".json") }.findFirst().get()
            }
            // Exact receipt shape produced by v0.14.11 before the no-speech correction.
            Files.write(receipt, """{"failed_task_id":"remote-1","failure_detail":"status=FAILED; provider_code=SUCCESS_WITH_NO_VALID_FRAGMENT"}""".toByteArray())
            server.enqueue(MockResponse().setResponseCode(400))
            val resumed = engine(server, FileTranscriptionModel.PARAFORMER_V2)
            assertTrue(resumed.transcribe(resumed.loadModel(path), FloatArray(48_000), null).segments.isEmpty())
            assertEquals(4, server.requestCount)
        } }
    }

    @Test
    fun terminalFailureRetainsSafeProviderEvidenceForDiagnosis() = runBlocking {
        MockWebServer().use { server -> withSession { path ->
            server.enqueue(policy(server)); server.enqueue(MockResponse())
            server.enqueue(MockResponse().setBody("""{"output":{"task_id":"remote-1","task_status":"PENDING"}}"""))
            server.enqueue(MockResponse().setBody("""{"request_id":"12345678-1234-1234-1234-123456789abc","output":{"task_id":"remote-1","task_status":"FAILED","code":"InvalidFile.DownloadFailed","message":"private https://example.test/?secret=key"}}"""))
            val engine = engine(server, FileTranscriptionModel.PARAFORMER_V2)
            val failure = runCatching { engine.transcribe(engine.loadModel(path), FloatArray(16_000), null) }
                .exceptionOrNull() as EngineTranscriptionException
            assertEquals("FILETRANS_TASK_FAILED", failure.errorCode)
            assertTrue(failure.safeTechnicalDetail.orEmpty().contains("InvalidFile.DownloadFailed"))
            assertFalse(failure.safeTechnicalDetail.orEmpty().contains("secret"))
            val receipt = Files.list(path.parent).use { paths ->
                paths.filter { it.fileName.toString().endsWith(".json") }.findFirst().get()
            }
            val state = JSONObject(String(Files.readAllBytes(receipt), Charsets.UTF_8))
            assertEquals("remote-1", state.getString("failed_task_id"))
            assertFalse(state.has("task_id"))
            assertTrue(state.getString("failure_detail").contains("InvalidFile.DownloadFailed"))
        } }
    }

    @Test
    fun ambiguousSubmissionCannotBeAutomaticallyResubmitted() = runBlocking {
        MockWebServer().use { server -> withSession { path ->
            server.enqueue(policy(server)); server.enqueue(MockResponse())
            server.enqueue(MockResponse().setBody("""{"output":{"task_status":"PENDING"}}"""))
            val first = engine(server, FileTranscriptionModel.PARAFORMER_V2)
            assertTrue(runCatching { first.transcribe(first.loadModel(path), FloatArray(16_000), null) }.isFailure)
            val restarted = engine(server, FileTranscriptionModel.PARAFORMER_V2)
            val failure = runCatching { restarted.transcribe(restarted.loadModel(path), FloatArray(16_000), null) }
                .exceptionOrNull() as EngineTranscriptionException
            assertEquals("FILETRANS_SUBMISSION_UNCERTAIN", failure.errorCode)
            assertEquals(3, server.requestCount)
        } }
    }
}

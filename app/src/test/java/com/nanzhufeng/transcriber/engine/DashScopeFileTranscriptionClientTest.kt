package com.nanzhufeng.transcriber.engine

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class DashScopeFileTranscriptionClientTest {
    @Test
    fun submitsBothModelsAsAsyncFileTasksWithRequiredParameters() = runBlocking {
        MockWebServer().use { server ->
            val client = DashScopeFileTranscriptionClient(
                { "test-key" }, OkHttpClient(), server.url("/api/v1/"),
            )
            FileTranscriptionModel.entries.forEach { model ->
                server.enqueue(MockResponse().setBody("""{"output":{"task_id":"task-1","task_status":"PENDING"}}"""))
                assertEquals("task-1", client.submit(model, "https://audio.example/file.wav?signature=secret"))
                val request = server.takeRequest()
                assertEquals("/api/v1/services/audio/asr/transcription", request.path)
                assertEquals("enable", request.getHeader("X-DashScope-Async"))
                val body = JSONObject(request.body.readUtf8())
                assertEquals(model.modelId, body.getString("model"))
                assertEquals(1, body.getJSONObject("input").getJSONArray("file_urls").length())
                assertEquals(0, body.getJSONObject("parameters").getJSONArray("channel_id").getInt(0))
            }
        }
    }

    @Test
    fun overallSuccessDoesNotHideFailedFileSubtask() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"output":{"task_id":"task-1","task_status":"SUCCEEDED","results":[{"subtask_status":"FAILED","message":"secret signed URL"}]}}"""))
            val client = DashScopeFileTranscriptionClient({ "test-key" }, OkHttpClient(), server.url("/api/v1/"))
            val state = client.query("task-1")
            assertTrue(state is FileTranscriptionState.Failed)
            assertFalse((state as FileTranscriptionState.Failed).message.contains("secret"))
        }
    }

    @Test
    fun pendingAndRunningAreNotCompletedTranscripts() = runBlocking {
        MockWebServer().use { server ->
            val client = DashScopeFileTranscriptionClient({ "test-key" }, OkHttpClient(), server.url("/api/v1/"))
            listOf("PENDING", "RUNNING").forEach { status ->
                server.enqueue(MockResponse().setBody("""{"output":{"task_id":"task-1","task_status":"$status"}}"""))
                assertTrue(client.query("task-1") is FileTranscriptionState.Waiting)
            }
        }
    }

    @Test
    fun parsesSentenceTimestampsAndProviderUsageWithoutInventingTokens() {
        val result = DashScopeFileTranscriptionClient.parseTranscript(
            """{"properties":{"original_duration_in_milliseconds":10000},"transcripts":[{"channel_id":0,"sentences":[{"begin_time":100,"end_time":2500,"text":"极氪 9X。"},{"begin_time":3000,"end_time":8500,"text":"MANSORY 定制。"}]}]}""",
        )
        assertEquals(listOf("极氪 9X。", "MANSORY 定制。"), result.map { it.text })
        assertEquals(100L, result.first().startMillis)
        assertEquals(8500L, result.last().endMillis)
    }

    @Test
    fun refusesInvalidTimesInsteadOfExportingBrokenSubtitles() {
        val failure = runCatching {
            DashScopeFileTranscriptionClient.parseTranscript(
                """{"transcripts":[{"channel_id":0,"sentences":[{"begin_time":4000,"end_time":1000,"text":"结果"}]}]}""",
            )
        }.exceptionOrNull()
        assertTrue(failure is EngineTranscriptionException)
    }

    @Test
    fun readyStateRetainsOnlyReportedUsageAndValidResultLocation() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"output":{"task_id":"task-1","task_status":"SUCCEEDED","results":[{"subtask_status":"SUCCEEDED","transcription_url":"https://dashscope-result-bj.oss-cn-beijing.aliyuncs.com/result.json"}]},"usage":{"duration":9.5}}"""))
            val client = DashScopeFileTranscriptionClient({ "test-key" }, OkHttpClient(), server.url("/api/v1/"))
            val state = client.query("task-1") as FileTranscriptionState.Ready
            assertEquals(9500L, state.usage.durationMillis)
            assertNull(state.usage.inputTokens)
            assertNull(state.usage.outputTokens)
        }
    }

    @Test
    fun rejectsTaskMismatchAndUntrustedResultHost() = runBlocking {
        MockWebServer().use { server ->
            val client = DashScopeFileTranscriptionClient({ "test-key" }, OkHttpClient(), server.url("/api/v1/"))
            listOf(
                """{"output":{"task_id":"other-task","task_status":"RUNNING"}}""",
                """{"output":{"task_id":"task-1","task_status":"SUCCEEDED","results":[{"subtask_status":"SUCCEEDED","transcription_url":"https://evil.example/result.json"}]}}""",
            ).forEach { response ->
                server.enqueue(MockResponse().setBody(response))
                val failure = runCatching { client.query("task-1") }.exceptionOrNull() as EngineTranscriptionException
                assertEquals("FILETRANS_RESPONSE_INVALID", failure.errorCode)
            }
        }
    }

    @Test
    fun authenticationErrorsNeverExposeServiceResponse() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(401).setBody("secret key and signed URL"))
            val client = DashScopeFileTranscriptionClient({ "test-key" }, OkHttpClient(), server.url("/api/v1/"))
            val failure = runCatching { client.query("task-1") }.exceptionOrNull() as EngineTranscriptionException
            assertEquals("FILETRANS_HTTP_401", failure.errorCode)
            assertFalse(failure.userMessage.contains("secret"))
            assertNull(failure.safeTechnicalDetail)
        }
    }
}

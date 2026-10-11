package com.nanzhufeng.transcriber.service

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BackgroundTranscriptionQueueTest {
    @Test fun unexpectedTaskFailureIsReportedAndOtherQueuedTasksCanContinue() = runBlocking {
        val visited = mutableListOf<String>()
        val failed = mutableListOf<String>()
        var released = false
        BackgroundTranscriptionQueue().run("first", execute = {
            visited += it
            if (it == "first") error("runner failed")
        }, nextTask = { if (visited.size == 1) "second" else null },
            onUnexpectedFailure = { id, _ -> failed += id }, onExit = { released = true })
        assertEquals(listOf("first", "second"), visited)
        assertEquals(listOf("first"), failed)
        assertTrue(released)
    }
    @Test fun databaseFailureInNextQueueLookupStillReleasesForegroundResources() = runBlocking {
        var released = false
        val failure = runCatching {
            BackgroundTranscriptionQueue().run("first", execute = {}, nextTask = { error("database unavailable") },
                onUnexpectedFailure = { _, _ -> }, onExit = { released = true })
        }
        assertTrue(failure.isFailure)
        assertTrue(released)
    }
    @Test fun cancellationIsNotMisreportedAsFailureAndReleasesResources() = runBlocking {
        var released = false
        var failureReported = false
        runCatching {
            BackgroundTranscriptionQueue().run("first", execute = { throw CancellationException() }, nextTask = { null },
                onUnexpectedFailure = { _, _ -> failureReported = true }, onExit = { released = true })
        }
        assertTrue(released)
        assertFalse(failureReported)
    }
}

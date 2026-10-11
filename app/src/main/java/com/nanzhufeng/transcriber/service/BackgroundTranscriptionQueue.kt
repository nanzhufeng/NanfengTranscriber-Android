package com.nanzhufeng.transcriber.service

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

/** A public lifecycle seam: unexpected failures and cancellation always release queue resources. */
class BackgroundTranscriptionQueue {
    suspend fun run(
        firstTaskId: String,
        execute: suspend (String) -> Unit,
        nextTask: suspend () -> String?,
        onUnexpectedFailure: suspend (String, Exception) -> Unit,
        onExit: () -> Unit,
    ) {
        try {
            var taskId: String? = firstTaskId
            while (taskId != null) {
                coroutineContext.ensureActive()
                val current = taskId
                try { execute(current) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { onUnexpectedFailure(current, error) }
                taskId = nextTask()
            }
        } finally {
            onExit()
        }
    }
}

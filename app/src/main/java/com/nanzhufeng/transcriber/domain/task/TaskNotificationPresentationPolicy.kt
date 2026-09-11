package com.nanzhufeng.transcriber.domain.task

/** Keeps terminal task notifications informational rather than visually in-progress. */
object TaskNotificationPresentationPolicy {
    fun progressFor(ongoing: Boolean, percentage: Int?): Int? =
        percentage?.coerceIn(0, 100)?.takeIf { ongoing }
}

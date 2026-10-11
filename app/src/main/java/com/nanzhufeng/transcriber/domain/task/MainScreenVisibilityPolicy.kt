package com.nanzhufeng.transcriber.domain.task

/** Process importance 125 (FGS / Room binding) is not evidence of a visible Activity. */
object MainScreenVisibilityPolicy {
    fun isVisible(importance: Int): Boolean = importance == 100 || importance == 200
}

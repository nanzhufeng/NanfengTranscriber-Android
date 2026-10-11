package com.nanzhufeng.transcriber.domain.task

/** Progress updates never expand the island. Only an explicit user tap does. */
class IslandDisplayController {
    var isExpanded: Boolean = false
        private set
    private var taskId: String? = null

    fun onTask(id: String) {
        if (taskId != id) collapse()
        taskId = id
    }
    fun toggle() { isExpanded = !isExpanded }
    fun collapse() { isExpanded = false }
}

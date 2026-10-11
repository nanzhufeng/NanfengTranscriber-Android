package com.nanzhufeng.transcriber.domain.task

enum class IslandStage(val label: String) {
    PREPARING("准备转写"), DECODING("提取音轨"), LOADING("加载模型"),
    TRANSCRIBING("转写中"), SAVING("保存结果"), COMPLETED("转写完成"),
    FAILED("转写失败"), WAITING("等待处理"), CANCELLED("转写已取消"),
}

data class NanfengIslandState(
    val taskId: String,
    val sourceName: String,
    val modelName: String,
    val stage: IslandStage,
    val message: String,
    val processedMillis: Long = 0,
    val totalMillis: Long? = null,
) {
    val ongoing: Boolean get() = stage in setOf(
        IslandStage.PREPARING, IslandStage.DECODING, IslandStage.LOADING,
        IslandStage.TRANSCRIBING, IslandStage.SAVING,
    )
    val percentage: Int? get() = totalMillis?.takeIf { it > 0 }?.let { total ->
        if (stage !in setOf(IslandStage.DECODING, IslandStage.TRANSCRIBING)) null
        else ((processedMillis.coerceAtLeast(0).coerceAtMost(total).toDouble() / total) * 100)
            .toInt().coerceIn(0, 99)
    }
    val compactText: String get() = stage.label + (percentage?.let { " · $it%" } ?: "")
    val timeText: String get() = if (totalMillis != null && totalMillis > 0) {
        "${formatTime(processedMillis.coerceIn(0, totalMillis))} / ${formatTime(totalMillis)}"
    } else "正在读取实际进度"

    companion object {
        private fun formatTime(millis: Long): String {
            val seconds = millis.coerceAtLeast(0) / 1_000
            return "%02d:%02d:%02d".format(seconds / 3_600, seconds / 60 % 60, seconds % 60)
        }
    }
}

/** One update owner for both notification and island. Stages/tasks bypass the rate limit. */
class TaskProgressUpdateGate(private val intervalMillis: Long = 1_000) {
    private var last: NanfengIslandState? = null
    private var lastMillis = Long.MIN_VALUE
    fun shouldPublish(state: NanfengIslandState, nowMillis: Long): Boolean {
        val previous = last
        if (previous == state) return false
        if (previous == null || previous.taskId != state.taskId || previous.stage != state.stage ||
            !state.ongoing || nowMillis - lastMillis >= intervalMillis) {
            last = state
            lastMillis = nowMillis
            return true
        }
        return false
    }
}

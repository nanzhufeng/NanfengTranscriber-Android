package com.nanzhufeng.transcriber.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Application
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.Bundle
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.nanzhufeng.transcriber.MainActivity
import com.nanzhufeng.transcriber.NanfengTranscriberApplication
import com.nanzhufeng.transcriber.R
import com.nanzhufeng.transcriber.data.task.TaskMutationResult
import com.nanzhufeng.transcriber.data.modelstore.OfficialModelCatalog
import com.nanzhufeng.transcriber.domain.task.IslandStage
import com.nanzhufeng.transcriber.domain.task.NanfengIslandState
import com.nanzhufeng.transcriber.domain.task.TaskProgressUpdateGate
import com.nanzhufeng.transcriber.domain.task.TaskNotificationPresentationPolicy
import com.nanzhufeng.transcriber.domain.task.TaskTransitionPolicy
import com.nanzhufeng.transcriber.domain.task.TranscriptionTaskState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay

class TranscriptionForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var notificationManager: NotificationManager
    private lateinit var runner: TranscriptionTaskRunner
    private var activeJob: Job? = null
    @Volatile private var activeTaskId: String? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var island: TaskIslandOverlay
    @Volatile private var latestIsland: NanfengIslandState? = null
    private val progressGate = TaskProgressUpdateGate()
    private var monitorJob: Job? = null
    private var wakeLeaseStarted = 0L

    override fun onCreate() {
        super.onCreate()
        val app = application as NanfengTranscriberApplication
        runner = TranscriptionTaskRunner(applicationContext, app.appContainer)
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
        island = TaskIslandOverlay(this, onOpen = { id ->
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_TASK_ID, id)
            })
        }, onCancel = ::cancelTask)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskId = intent?.getStringExtra(EXTRA_TASK_ID)
        if (intent?.action == ACTION_CANCEL && taskId != null) {
            cancelTask(taskId)
            return START_NOT_STICKY
        }
        if (intent?.action != ACTION_START || taskId == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        if (activeJob?.isActive == true) {
            return START_REDELIVER_INTENT
        }

        acquireWakeLock()
        startInForeground(taskId, "准备转写", null)
        monitorJob = serviceScope.launch {
            while (true) {
                delay(1_000)
                latestIsland?.let(island::update)
                acquireWakeLock()
            }
        }
        activeJob = serviceScope.launch {
            val tasks = (application as NanfengTranscriberApplication).appContainer.tasks
            try {
            BackgroundTranscriptionQueue().run(taskId, execute = { currentId ->
                activeTaskId = currentId
                runningTaskId = currentId
                val task = tasks.findById(currentId)
                val initial = NanfengIslandState(
                    currentId, task?.sourceDisplayName ?: "转写任务",
                    task?.modelId?.let { OfficialModelCatalog.find(it)?.displayName ?: it } ?: "转写模型",
                    IslandStage.PREPARING, "准备转写",
                )
                publishIsland(initial)
                val outcome =
                    runner.run(currentId) { progress ->
                        if (activeTaskId == currentId) publishIsland(initial.copy(
                            stage = progress.stage, message = progress.message,
                            processedMillis = progress.processedMillis, totalMillis = progress.totalMillis,
                        ))
                    }
                    val finalMessage = when (outcome) {
                        is TaskRunOutcome.Completed -> "转写完成，已归档到历史"
                        is TaskRunOutcome.NoSpeech -> outcome.message
                        is TaskRunOutcome.Failed -> outcome.message
                        is TaskRunOutcome.Waiting -> outcome.message
                        is TaskRunOutcome.Ignored -> outcome.message
                    }
                    val stage = when (outcome) {
                        is TaskRunOutcome.Completed -> IslandStage.COMPLETED
                        is TaskRunOutcome.Waiting, is TaskRunOutcome.Ignored -> IslandStage.WAITING
                        else -> IslandStage.FAILED
                    }
                    if (activeTaskId == currentId) publishIsland(initial.copy(stage = stage, message = finalMessage))
                clearActiveTask(currentId)
            }, nextTask = {
                tasks.findNextQueued()?.id.also { if (it == null) delay(2_000) }
            }, onUnexpectedFailure = { id, error ->
                val message = "后台处理已中断，打开任务可检查并恢复"
                try {
                    val result = tasks.markRecoveryRequired(id, "BACKGROUND_EXECUTION_FAILED", message, error.javaClass.simpleName)
                    if (result is TaskMutationResult.Rejected && result.task.state == TranscriptionTaskState.QUEUED.name) {
                        tasks.transition(id, TranscriptionTaskState.FAILED, errorCode = "BACKGROUND_EXECUTION_FAILED", userMessage = message)
                    }
                } finally {
                    val snapshot = latestIsland?.takeIf { it.taskId == id }
                        ?: NanfengIslandState(id, "转写任务", "转写模型", IslandStage.PREPARING, message)
                    publishIsland(snapshot.copy(stage = IslandStage.FAILED, message = message))
                    clearActiveTask(id)
                }
            }, onExit = ::finishWorker)
            } catch (_: CancellationException) {
                // The queue's finally owns cleanup; cancellation must not become a task failure.
            } catch (error: Exception) {
                // Also cover repository lookup / recovery failures outside runner.run.
                android.util.Log.e("TranscriptionService", "Background queue stopped: ${error.javaClass.simpleName}")
            }
        }
        return START_REDELIVER_INTENT
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        monitorJob?.cancel()
        island.close()
        val taskId = activeTaskId
        try { if (taskId != null) {
            runBlocking(Dispatchers.IO) {
                (application as NanfengTranscriberApplication).appContainer.tasks.markRecoveryRequired(
                    id = taskId,
                    errorCode = "MEDIA_PROCESSING_TIMEOUT",
                    userMessage = "已达到系统后台处理时限，重新打开 App 后可继续",
                    technicalDetail = "Android foreground service timeout, type=$fgsType",
                )
            }
        } } finally {
        activeJob?.cancel()
        clearActiveTask(taskId)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        releaseWakeLock()
        stopSelf()
        terminateWorkerProcess()
        }
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        island.close()
        clearActiveTask(activeTaskId)
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun cancelTask(taskId: String) {
        serviceScope.launch(Dispatchers.IO) {
            val repository = (application as NanfengTranscriberApplication).appContainer.tasks
            val task = repository.findById(taskId)
            val state = task?.state?.let { runCatching { TranscriptionTaskState.valueOf(it) }.getOrNull() }
            if (state != null && TaskTransitionPolicy.canTransition(state, TranscriptionTaskState.CANCELLED)) {
                repository.transition(
                    id = taskId,
                    target = TranscriptionTaskState.CANCELLED,
                    errorCode = "USER_CANCELLED",
                    userMessage = "已由用户取消，原文件和模型均未删除",
                )
            }
            if (activeTaskId != taskId) {
                return@launch
            }
            clearActiveTask(taskId)
            latestIsland = latestIsland?.takeIf { it.taskId == taskId }?.copy(
                stage = IslandStage.CANCELLED, message = "转写已取消",
            )
            runner.cancel(taskId)
            monitorJob?.cancel()
            island.close()
            ServiceCompat.stopForeground(
                this@TranscriptionForegroundService,
                ServiceCompat.STOP_FOREGROUND_REMOVE,
            )
            notificationManager.notify(
                COMPLETION_NOTIFICATION_ID,
                buildNotification(taskId, "转写已取消", null, ongoing = false),
            )
            activeJob?.cancel()
            clearActiveTask(taskId)
            releaseWakeLock()
            stopSelf()
            terminateWorkerProcess()
        }
    }

    private fun terminateWorkerProcess() {
        if (Application.getProcessName().endsWith(TRANSCRIPTION_PROCESS_SUFFIX)) {
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    private fun publishIsland(state: NanfengIslandState) {
        latestIsland = state
        if (!progressGate.shouldPublish(state, SystemClock.elapsedRealtime())) return
        island.update(state)
        runCatching { notificationManager.notify(
            if (state.ongoing) NOTIFICATION_ID else COMPLETION_NOTIFICATION_ID + state.taskId.hashCode().and(0x3FF),
            buildNotification(state.taskId, state.message, state.percentage, state.ongoing),
        ) }.onFailure {
            // Presentation must not interrupt audio processing or invalidate its saved checkpoint.
            android.util.Log.w("TranscriptionService", "Notification update unavailable: ${it.javaClass.simpleName}")
        }
    }

    private fun finishWorker() {
        monitorJob?.cancel()
        island.close()
        try { runner.close() } finally {
            clearActiveTask(activeTaskId)
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            releaseWakeLock()
            stopSelf()
            terminateWorkerProcess()
        }
    }

    private fun startInForeground(taskId: String, message: String, percentage: Int?) {
        val notification = buildNotification(taskId, message, percentage, ongoing = true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING,
            )
        } else {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        }
    }

    private fun buildNotification(
        taskId: String,
        message: String,
        percentage: Int?,
        ongoing: Boolean,
    ): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            taskId.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_transcription_notification)
            .setContentTitle(latestIsland?.takeIf { it.taskId == taskId }?.compactText ?: "南枫转写")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(latestIsland?.takeIf { it.taskId == taskId }?.let {
                "${it.sourceName}\n${it.modelName}\n$message" + if (it.ongoing) "\n${it.timeText}" else ""
            } ?: message))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setShowWhen(false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openPendingIntent)
            .setOnlyAlertOnce(ongoing)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (ongoing) {
            if (Build.VERSION.SDK_INT >= 36) builder.addExtras(Bundle().apply {
                putBoolean("android.requestPromotedOngoing", true)
            })
            val cancelIntent = Intent(this, TranscriptionForegroundService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_TASK_ID, taskId)
            }
            val cancelPendingIntent = PendingIntent.getService(
                this,
                taskId.hashCode() xor 0x5A17,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(0, "取消转写", cancelPendingIntent)
        }
        val visiblePercentage = TaskNotificationPresentationPolicy.progressFor(ongoing, percentage)
        if (visiblePercentage != null) {
            builder.setProgress(100, visiblePercentage, false)
        } else if (ongoing) {
            builder.setProgress(0, 0, true)
        }
        return builder.build()
    }

    private fun createNotificationChannel() {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "后台转写任务",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "显示本机音视频转写的真实阶段和进度"
                setShowBadge(false)
            },
        )
    }

    @Synchronized private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true && SystemClock.elapsedRealtime() - wakeLeaseStarted < WAKE_RENEWAL_MILLIS) return
        releaseWakeLock()
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "NanfengTranscriber:BackgroundTranscription",
            )
            .apply {
                setReferenceCounted(false)
                acquire(MAX_WAKE_LOCK_MILLIS)
            }
        wakeLeaseStarted = SystemClock.elapsedRealtime()
    }

    @Synchronized private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    private fun clearActiveTask(taskId: String?) {
        if (activeTaskId == taskId) activeTaskId = null
        if (runningTaskId == taskId) runningTaskId = null
    }

    companion object {
        private const val ACTION_START = "com.nanzhufeng.transcriber.action.START_TRANSCRIPTION"
        private const val ACTION_CANCEL = "com.nanzhufeng.transcriber.action.CANCEL_TRANSCRIPTION"
        const val EXTRA_TASK_ID = "transcription_task_id"
        private const val CHANNEL_ID = "background_transcription"
        private const val NOTIFICATION_ID = 2_101
        private const val COMPLETION_NOTIFICATION_ID = 2_102
        private const val MAX_WAKE_LOCK_MILLIS = 30L * 60L * 1_000L
        private const val WAKE_RENEWAL_MILLIS = 10L * 60L * 1_000L
        private const val TRANSCRIPTION_PROCESS_SUFFIX = ":transcription"

        @Volatile
        private var runningTaskId: String? = null

        fun start(context: Context, taskId: String) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, TranscriptionForegroundService::class.java).apply {
                    action = ACTION_START
                    putExtra(EXTRA_TASK_ID, taskId)
                },
            )
        }

        fun cancel(context: Context, taskId: String) {
            context.startService(
                Intent(context, TranscriptionForegroundService::class.java).apply {
                    action = ACTION_CANCEL
                    putExtra(EXTRA_TASK_ID, taskId)
                },
            )
        }

        fun isRunning(taskId: String): Boolean = runningTaskId == taskId
    }
}

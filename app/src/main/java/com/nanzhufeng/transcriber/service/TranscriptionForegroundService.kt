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
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.nanzhufeng.transcriber.MainActivity
import com.nanzhufeng.transcriber.NanfengTranscriberApplication
import com.nanzhufeng.transcriber.R
import com.nanzhufeng.transcriber.data.task.TaskMutationResult
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

class TranscriptionForegroundService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var notificationManager: NotificationManager
    private lateinit var runner: TranscriptionTaskRunner
    private var activeJob: Job? = null
    private var activeTaskId: String? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        val app = application as NanfengTranscriberApplication
        runner = TranscriptionTaskRunner(applicationContext, app.appContainer)
        notificationManager = getSystemService(NotificationManager::class.java)
        createNotificationChannel()
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
        startInForeground(taskId, "正在启动本机转写", null)
        activeJob = serviceScope.launch {
            var queuedTaskId: String? = taskId
            var cancelled = false
            while (queuedTaskId != null && !cancelled) {
                val currentId = queuedTaskId
                activeTaskId = currentId
                runningTaskId = currentId
                notificationManager.notify(
                    NOTIFICATION_ID,
                    buildNotification(currentId, "正在启动本机转写", null, ongoing = true),
                )
                val outcome = try {
                    runner.run(currentId) { progress ->
                        notificationManager.notify(
                            NOTIFICATION_ID,
                            buildNotification(
                                taskId = currentId,
                                message = progress.message,
                                percentage = progress.percentage,
                                ongoing = true,
                            ),
                        )
                    }
                } catch (_: CancellationException) {
                    cancelled = true
                    null
                }

                if (outcome != null) {
                    val finalMessage = when (outcome) {
                        is TaskRunOutcome.Completed -> "转写完成，已归档到历史"
                        is TaskRunOutcome.NoSpeech -> outcome.message
                        is TaskRunOutcome.Failed -> outcome.message
                        is TaskRunOutcome.Waiting -> outcome.message
                        is TaskRunOutcome.Ignored -> outcome.message
                    }
                    val successful = outcome is TaskRunOutcome.Completed
                    notificationManager.notify(
                        COMPLETION_NOTIFICATION_ID + currentId.hashCode().and(0x3FF),
                        buildNotification(
                            taskId = currentId,
                            message = finalMessage,
                            percentage = if (successful) 100 else null,
                            ongoing = false,
                        ),
                    )
                }
                clearActiveTask(currentId)
                queuedTaskId = if (cancelled) null else {
                    (application as NanfengTranscriberApplication).appContainer.tasks.findNextQueued()?.id
                }
                if (queuedTaskId != null) {
                    releaseWakeLock()
                    acquireWakeLock()
                }
            }
            runner.close()
            ServiceCompat.stopForeground(
                this@TranscriptionForegroundService,
                ServiceCompat.STOP_FOREGROUND_REMOVE,
            )
            releaseWakeLock()
            stopSelf()
            terminateWorkerProcess()
        }
        return START_REDELIVER_INTENT
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        val taskId = activeTaskId
        if (taskId != null) {
            runBlocking(Dispatchers.IO) {
                (application as NanfengTranscriberApplication).appContainer.tasks.markRecoveryRequired(
                    id = taskId,
                    errorCode = "MEDIA_PROCESSING_TIMEOUT",
                    userMessage = "已达到系统后台处理时限，重新打开 App 后可继续",
                    technicalDetail = "Android foreground service timeout, type=$fgsType",
                )
            }
        }
        activeJob?.cancel()
        clearActiveTask(taskId)
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        releaseWakeLock()
        stopSelf()
        terminateWorkerProcess()
    }

    override fun onDestroy() {
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
            runner.cancel(taskId)
            activeJob?.cancel()
            ServiceCompat.stopForeground(
                this@TranscriptionForegroundService,
                ServiceCompat.STOP_FOREGROUND_REMOVE,
            )
            notificationManager.notify(
                COMPLETION_NOTIFICATION_ID,
                buildNotification(taskId, "转写已取消", null, ongoing = false),
            )
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
            .setContentTitle(if (ongoing) "南枫转写正在后台处理" else "南枫转写任务状态")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(openPendingIntent)
            .setOnlyAlertOnce(ongoing)
            .setOngoing(ongoing)
            .setAutoCancel(!ongoing)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (ongoing) {
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
        if (percentage != null) {
            builder.setProgress(100, percentage.coerceIn(0, 100), false)
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

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "NanfengTranscriber:BackgroundTranscription",
            )
            .apply {
                setReferenceCounted(false)
                acquire(MAX_WAKE_LOCK_MILLIS)
            }
    }

    private fun releaseWakeLock() {
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
        private const val MAX_WAKE_LOCK_MILLIS = 2L * 60L * 60L * 1_000L + 30L * 60L * 1_000L
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

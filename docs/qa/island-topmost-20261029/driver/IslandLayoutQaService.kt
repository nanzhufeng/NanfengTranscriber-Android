package com.nanzhufeng.transcriber.qa

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.*
import com.nanzhufeng.transcriber.R
import com.nanzhufeng.transcriber.domain.task.*
import com.nanzhufeng.transcriber.service.TaskIslandOverlay

/** Temporary, debug-only driver of the real production overlay; no ASR, network, or task storage. */
class IslandLayoutQaService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var island: TaskIslandOverlay
    private var state = NanfengIslandState("layout-only", "模拟器界面检查.wav", "SenseVoiceSmall INT8",
        IslandStage.TRANSCRIBING, "仅验证界面，不执行转写", 65000, 100000)
    private val tick = object : Runnable {
        override fun run() { island.update(state); handler.postDelayed(this, 600) }
    }
    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("layout-qa", "隔离界面检查", NotificationManager.IMPORTANCE_LOW))
        val note = Notification.Builder(this, "layout-qa").setSmallIcon(R.drawable.ic_transcription_notification)
            .setContentTitle("隔离界面检查：无转写/网络/数据写入").build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(8425, note, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(8425, note)
        island = TaskIslandOverlay(this, onOpen = { android.util.Log.i("IslandLayoutQa", "open callback") },
            onCancel = { stopSelf() })
        handler.post(tick)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.getBooleanExtra("unknown", false) == true) state = state.copy(stage = IslandStage.LOADING, totalMillis = null)
        else state = state.copy(stage = IslandStage.TRANSCRIBING, totalMillis = 100000)
        return START_NOT_STICKY
    }
    override fun onBind(intent: Intent?) = null
    override fun onDestroy() { handler.removeCallbacks(tick); island.close(); super.onDestroy() }
}

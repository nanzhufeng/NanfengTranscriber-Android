package com.nanzhufeng.transcriber.service

import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.nanzhufeng.transcriber.R
import com.nanzhufeng.transcriber.domain.task.IslandStage
import com.nanzhufeng.transcriber.domain.task.NanfengIslandState
import com.nanzhufeng.transcriber.domain.task.MainScreenVisibilityPolicy
import com.nanzhufeng.transcriber.domain.task.IslandDisplayController

/** App-owned island, not an OEM whitelist claim. No view/window is created without user consent. */
class TaskIslandOverlay(
    private val context: Context,
    private val onOpen: (String) -> Unit,
    private val onCancel: (String) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val windows = context.getSystemService(WindowManager::class.java)
    private var view: LinearLayout? = null
    private var rendered: NanfengIslandState? = null
    private val islandDisplay = IslandDisplayController()
    private val expanded get() = islandDisplay.isExpanded
    @Volatile private var closed = false

    fun update(state: NanfengIslandState) {
        handler.post {
            if (closed) return@post
            try {
            val mainVisible = context.getSystemService(ActivityManager::class.java).runningAppProcesses
                ?.any { it.processName == context.packageName && MainScreenVisibilityPolicy.isVisible(it.importance) } == true
            if (!Settings.canDrawOverlays(context) || mainVisible ||
                context.getSystemService(KeyguardManager::class.java).isKeyguardLocked) {
                removeView()
                return@post
            }
            if (rendered == state && view != null) return@post
            islandDisplay.onTask(state.taskId)
            render(state)
            } catch (error: RuntimeException) {
                // Permission or system-service failures only hide presentation, never stop ASR.
                android.util.Log.w("TaskIsland", "Island unavailable: ${error.javaClass.simpleName}: ${error.message}")
                removeView()
            }
        }
    }

    private fun render(state: NanfengIslandState) {
        val root = view ?: LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = GradientDrawable().apply { setColor(Color.rgb(22, 29, 33)); cornerRadius = dp(24).toFloat() }
            elevation = dp(8).toFloat()
            clipToOutline = true
            contentDescription = "南枫转写灵动岛"
        }
        root.removeAllViews()
        val surface = GradientDrawable().apply {
            setColor(Color.rgb(22, 29, 33)); cornerRadius = dp(24).toFloat()
        }
        // Keep the 48dp touch region, but make the visible compact capsule only 34dp tall.
        root.background = if (expanded) surface else InsetDrawable(surface, 0, dp(7), 0, dp(7))
        // Reapply after background assignment: Drawable padding otherwise erases our safe margins.
        root.setPadding(dp(if (expanded) 14 else 24), dp(10), dp(if (expanded) 14 else 24), dp(10))
        root.minimumWidth = if (expanded) 0 else minOf(dp(220), context.resources.displayMetrics.widthPixels - dp(24))
        root.minimumHeight = if (expanded) 0 else dp(48)
        root.setOnClickListener(if (expanded) null else View.OnClickListener { islandDisplay.toggle(); render(state) })
        root.isClickable = !expanded
        root.contentDescription = if (expanded) "南枫转写详情" else "展开转写详情，${state.compactText}"
        val accent = when (state.stage) {
            IslandStage.COMPLETED -> Color.rgb(89, 210, 144)
            IslandStage.FAILED -> Color.rgb(255, 113, 113)
            IslandStage.WAITING, IslandStage.CANCELLED -> Color.rgb(235, 177, 93)
            else -> Color.rgb(83, 204, 222)
        }
        val compact = LinearLayout(context).apply {
            gravity = if (expanded) Gravity.CENTER_VERTICAL else Gravity.CENTER
            minimumHeight = dp(if (expanded) 48 else 28)
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_transcription_notification)
                imageTintList = ColorStateList.valueOf(accent)
            }, LinearLayout.LayoutParams(dp(20), dp(20)).apply { marginEnd = dp(9) })
            addView(text(state.compactText, 14f).apply { setTextColor(accent) })
            contentDescription = if (expanded) "收起转写详情" else "展开转写详情，${state.compactText}"
            if (expanded) setOnClickListener { islandDisplay.toggle(); render(state) }
        }
        root.addView(compact)
        if (expanded) {
            root.addView(text(state.sourceName, 14f).apply { setPadding(0, dp(10), 0, dp(4)); maxLines = 2 })
            root.addView(text(state.modelName, 12f).apply { setTextColor(Color.LTGRAY) })
            root.addView(text(state.message, 12f).apply { maxLines = 3; setPadding(0, dp(5), 0, dp(5)) })
            if (state.ongoing) {
                root.addView(ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                    max = 100
                    isIndeterminate = state.percentage == null
                    progress = state.percentage ?: 0
                    progressTintList = ColorStateList.valueOf(accent)
                    indeterminateTintList = ColorStateList.valueOf(accent)
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(8)))
                root.addView(text(state.timeText, 12f).apply { setPadding(0, dp(6), 0, dp(6)) })
            }
            root.addView(LinearLayout(context).apply {
                gravity = Gravity.CENTER
                addView(action(if (state.stage == IslandStage.COMPLETED) "查看文本" else "查看任务", accent) {
                    onOpen(state.taskId)
                }, LinearLayout.LayoutParams(0, dp(42), 1f))
                if (state.ongoing) addView(action("取消转写", Color.rgb(245, 130, 130)) {
                    onCancel(state.taskId)
                }, LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginStart = dp(8) })
            })
        }
        rendered = state
        val width = if (expanded) minOf(dp(320), context.resources.displayMetrics.widthPixels - dp(24))
            else LinearLayout.LayoutParams.WRAP_CONTENT
        val params = WindowManager.LayoutParams(width, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(6) // WindowManager keeps the application overlay below the status/cutout safe area.
            title = "NanfengTranscriptionIsland"
        }
        // Do not let an overlay permission revocation crash or cancel the actual transcription.
        try {
            if (view == null) { windows.addView(root, params); view = root }
            else windows.updateViewLayout(root, params)
        } catch (error: RuntimeException) {
            android.util.Log.w("TaskIsland", "Window unavailable: ${error.javaClass.simpleName}: ${error.message}")
            removeView()
        }
    }

    private fun text(value: String, size: Float) = TextView(context).apply {
        text = value; textSize = size; setTextColor(Color.WHITE)
        maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END
    }
    private fun action(label: String, color: Int, click: () -> Unit) = text(label, 13f).apply {
        gravity = Gravity.CENTER; setTextColor(color)
        background = RippleDrawable(ColorStateList.valueOf(Color.argb(45, 255, 255, 255)),
            GradientDrawable().apply { setColor(Color.rgb(43, 51, 57)); cornerRadius = dp(18).toFloat() },
            GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(18).toFloat() })
        clipToOutline = true; setOnClickListener { click() }
    }
    private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()
    private fun removeView() {
        view?.let { runCatching { windows.removeViewImmediate(it) } }
        view = null; rendered = null; islandDisplay.collapse()
    }
    fun close() { closed = true; handler.post { removeView() } }
}

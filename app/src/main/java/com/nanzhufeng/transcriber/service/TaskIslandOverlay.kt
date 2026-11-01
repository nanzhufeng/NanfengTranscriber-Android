package com.nanzhufeng.transcriber.service

import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.Context
import android.content.ComponentCallbacks
import android.content.res.Configuration
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.InsetDrawable
import android.os.Handler
import android.os.Looper
import android.os.Build
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.WindowInsets
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
    private var windowHost: Context = context
    private var windows = context.getSystemService(WindowManager::class.java)
    private var view: LinearLayout? = null
    private var rendered: NanfengIslandState? = null
    private var renderedScreen: IslandScreenGeometry? = null
    private val configurationCallbacks = object : ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: Configuration) { rendered?.let(::update) }
        override fun onLowMemory() = Unit
    }
    init { context.registerComponentCallbacks(configurationCallbacks) }
    private val islandDisplay = IslandDisplayController()
    private val expanded get() = islandDisplay.isExpanded
    @Volatile private var closed = false

    fun update(state: NanfengIslandState) {
        handler.post {
            if (closed) return@post
            try {
            val mainVisible = context.getSystemService(ActivityManager::class.java).runningAppProcesses
                ?.any { it.processName == context.packageName && MainScreenVisibilityPolicy.isVisible(it.importance) } == true
            val topmostHost = IslandTopmostService.connected
            if ((topmostHost == null && !Settings.canDrawOverlays(context)) || mainVisible ||
                context.getSystemService(KeyguardManager::class.java).isKeyguardLocked) {
                removeView()
                return@post
            }
            val nextHost = topmostHost ?: context
            if (windowHost !== nextHost) {
                // A window type/token cannot be changed in place. Remove from its original manager first.
                removeView(collapse = false)
                windowHost = nextHost
                windows = nextHost.getSystemService(WindowManager::class.java)
            }
            val screen = readScreen()
            if (rendered == state && renderedScreen == screen && view != null) return@post
            islandDisplay.onTask(state.taskId)
            render(state, screen)
            } catch (error: RuntimeException) {
                // Permission or system-service failures only hide presentation, never stop ASR.
                android.util.Log.w("TaskIsland", "Island unavailable: ${error.javaClass.simpleName}: ${error.message}")
                removeView()
            }
        }
    }

    private fun render(state: NanfengIslandState, screen: IslandScreenGeometry = readScreen()) {
        val placement = IslandWindowGeometry.resolve(screen, expanded, aboveStatusBar = windowHost is IslandTopmostService)
        val root = view ?: LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            background = GradientDrawable().apply { setColor(Color.rgb(22, 29, 33)); cornerRadius = dp(24).toFloat() }
            elevation = 0f
            clipToOutline = true
            contentDescription = "南枫转写灵动岛"
        }
        root.removeAllViews()
        val surface = GradientDrawable().apply {
            setColor(Color.rgb(22, 29, 33)); cornerRadius = dp(24).toFloat()
        }
        // Keep the 48dp touch region, but make the visible compact capsule only 34dp tall.
        root.background = when {
            expanded -> surface
            placement.atStatusBar -> null // Only the status-bar header is visible; retain a 48dp touch target.
            else -> InsetDrawable(surface, 0, dp(7), 0, dp(7))
        }
        // Reapply after background assignment: Drawable padding otherwise erases our safe margins.
        if (placement.atStatusBar) root.setPadding(0, 0, 0, 0)
        else root.setPadding(dp(if (expanded) 14 else 24), dp(10), dp(if (expanded) 14 else 24), dp(10))
        root.minimumWidth = if (expanded || placement.atStatusBar) 0 else minOf(dp(220), screen.width - dp(24))
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
        val compact = if (placement.atStatusBar && placement.cameraGapEnd > placement.cameraGapStart) {
            cameraSafeHeader(state, accent, placement, screen.width)
        } else LinearLayout(context).apply {
            if (placement.atStatusBar) {
                background = GradientDrawable().apply { setColor(Color.rgb(22, 29, 33)); cornerRadius = dp(24).toFloat() }
                clipToOutline = true
            }
            gravity = if (expanded) Gravity.CENTER_VERTICAL else Gravity.CENTER
            minimumHeight = dp(if (expanded) 48 else 28)
            addView(ImageView(context).apply {
                setImageResource(R.drawable.ic_transcription_notification)
                imageTintList = ColorStateList.valueOf(accent)
            }, LinearLayout.LayoutParams(dp(20), dp(20)).apply { marginEnd = dp(9) })
            addView(text(state.compactText, 14f).apply { setTextColor(accent) })
        }
        compact.contentDescription = if (expanded) "收起转写详情" else "展开转写详情，${state.compactText}"
        if (expanded) compact.setOnClickListener { islandDisplay.toggle(); render(state) }
        root.addView(compact, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            if (placement.atStatusBar) placement.headerHeight else LinearLayout.LayoutParams.WRAP_CONTENT))
        if (expanded) {
            val body = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                if (placement.atStatusBar) setPadding(dp(14), 0, dp(14), dp(10))
            }
            body.addView(text(state.sourceName, 14f).apply { setPadding(0, dp(10), 0, dp(4)); maxLines = 2 })
            body.addView(text(state.modelName, 12f).apply { setTextColor(Color.LTGRAY) })
            body.addView(text(state.message, 12f).apply { maxLines = 3; setPadding(0, dp(5), 0, dp(5)) })
            if (state.ongoing) {
                body.addView(ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
                    max = 100
                    isIndeterminate = state.percentage == null
                    progress = state.percentage ?: 0
                    progressTintList = ColorStateList.valueOf(accent)
                    indeterminateTintList = ColorStateList.valueOf(accent)
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(8)))
                body.addView(text(state.timeText, 12f).apply { setPadding(0, dp(6), 0, dp(6)) })
            }
            body.addView(LinearLayout(context).apply {
                gravity = Gravity.CENTER
                addView(action(if (state.stage == IslandStage.COMPLETED) "查看文本" else "查看任务", accent) {
                    onOpen(state.taskId)
                }, LinearLayout.LayoutParams(0, dp(42), 1f))
                if (state.ongoing) addView(action("取消转写", Color.rgb(245, 130, 130)) {
                    onCancel(state.taskId)
                }, LinearLayout.LayoutParams(0, dp(42), 1f).apply { marginStart = dp(8) })
            })
            root.addView(body)
        }
        rendered = state
        renderedScreen = screen
        val params = WindowManager.LayoutParams(placement.width ?: LinearLayout.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (windowHost is IslandTopmostService) WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            else WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = placement.y
            title = "NanfengTranscriptionIsland"
            if (placement.atStatusBar) {
                flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= 30)
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                else WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                if (Build.VERSION.SDK_INT >= 30) setFitInsetsTypes(0)
            }
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

    private fun cameraSafeHeader(state: NanfengIslandState, accent: Int, placement: IslandWindowPlacement, screenWidth: Int) =
        LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply { setColor(Color.rgb(22, 29, 33)); cornerRadius = dp(24).toFloat() }
            clipToOutline = true
            val edgePadding = maxOf(dp(10), dp(80) - (screenWidth - (placement.width ?: 0)) / 2)
            val left = LinearLayout(context).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(edgePadding, 0, dp(4), 0)
                addView(ImageView(context).apply {
                    setImageResource(R.drawable.ic_transcription_notification)
                    imageTintList = ColorStateList.valueOf(accent)
                }, LinearLayout.LayoutParams(dp(16), dp(16)).apply { marginEnd = dp(5) })
                addView(text(state.stage.label, 13f).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setTextColor(accent)
                    setAutoSizeTextTypeUniformWithConfiguration(7, 13, 1, TypedValue.COMPLEX_UNIT_SP)
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f))
            }
            addView(left, LinearLayout.LayoutParams(placement.cameraGapStart, LinearLayout.LayoutParams.MATCH_PARENT))
            addView(View(context).apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO },
                LinearLayout.LayoutParams(placement.cameraGapEnd - placement.cameraGapStart, 1))
            addView(text(state.percentage?.let { "$it%" } ?: "南枫转写", 13f).apply {
                gravity = Gravity.CENTER
                setTextColor(accent)
                setPadding(dp(4), 0, edgePadding, 0)
                setAutoSizeTextTypeUniformWithConfiguration(7, 13, 1, TypedValue.COMPLEX_UNIT_SP)
            }, LinearLayout.LayoutParams((placement.width ?: 0) - placement.cameraGapEnd,
                LinearLayout.LayoutParams.MATCH_PARENT))
        }

    @Suppress("DEPRECATION")
    private fun readScreen(): IslandScreenGeometry {
        val metrics = DisplayMetrics()
        val display = windows.defaultDisplay
        display.getRealMetrics(metrics)
        val windowInsets = if (Build.VERSION.SDK_INT >= 30) windows.currentWindowMetrics.windowInsets else null
        val cutout = display.cutout ?: if (Build.VERSION.SDK_INT >= 30) windowInsets?.displayCutout else null
        val statusBar = if (Build.VERSION.SDK_INT >= 30)
            windowInsets?.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars())?.top ?: 0
        else view?.rootWindowInsets?.stableInsetTop ?: dp(28)
        val camera = cutout?.boundingRects?.filter { it.top <= maxOf(statusBar, cutout.safeInsetTop) && it.bottom > 0 }
            ?.minByOrNull { kotlin.math.abs(it.centerX() - metrics.widthPixels / 2) }
        return IslandScreenGeometry(metrics.widthPixels, metrics.heightPixels, metrics.density,
            maxOf(statusBar, cutout?.safeInsetTop ?: 0), camera?.left, camera?.right, camera?.bottom ?: 0, context.resources.configuration.fontScale)
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
    private fun removeView(collapse: Boolean = true) {
        view?.let { runCatching { windows.removeViewImmediate(it) } }
        view = null; rendered = null; renderedScreen = null
        if (collapse) islandDisplay.collapse()
    }
    fun close() {
        if (closed) return
        closed = true
        runCatching { context.unregisterComponentCallbacks(configurationCallbacks) }
        handler.post { removeView() }
    }
}

/** Display-space geometry shared by window placement and the two camera-safe text lanes. */
data class IslandScreenGeometry(
    val width: Int,
    val height: Int,
    val density: Float,
    val statusBarHeight: Int,
    val cutoutLeft: Int? = null,
    val cutoutRight: Int? = null,
    val cutoutBottom: Int = 0,
    val fontScale: Float = 1f,
)

data class IslandWindowPlacement(
    val atStatusBar: Boolean,
    val width: Int?,
    val y: Int,
    val headerHeight: Int,
    val cameraGapStart: Int = 0,
    val cameraGapEnd: Int = 0,
)

object IslandWindowGeometry {
    fun resolve(screen: IslandScreenGeometry, expanded: Boolean, aboveStatusBar: Boolean = false): IslandWindowPlacement {
        fun dp(value: Int) = (value * screen.density).toInt()
        val available = (screen.width - dp(24)).coerceAtLeast(1)
        val outerPortrait = screen.width / screen.density < 600f && screen.height >= screen.width
        if (!outerPortrait || !aboveStatusBar) {
            return IslandWindowPlacement(false, if (expanded) minOf(dp(320), available) else null, dp(6), dp(28))
        }
        val cameraLeft = screen.cutoutLeft ?: (screen.width / 2 - dp(28))
        val cameraRight = screen.cutoutRight ?: (screen.width / 2 + dp(28))
        val laneWidth = dp(35 + (5 * 7 * screen.fontScale).toInt())
        val compactWidth = maxOf(dp(220), cameraRight - cameraLeft + dp(16) + 2 * laneWidth)
        val iconSafeWidth = maxOf(dp(180), screen.width - dp(160)).coerceAtMost(available)
        val width = if (expanded) minOf(dp(320), iconSafeWidth) else minOf(compactWidth, iconSafeWidth)
        val windowLeft = (screen.width - width) / 2
        return IslandWindowPlacement(
            atStatusBar = true,
            width = width,
            y = dp(2), // Keep the full rounded outline inside the visible display.
            // The connected accessibility window receives header taps; retain the visible lower edge.
            headerHeight = maxOf(dp(28), screen.statusBarHeight, screen.cutoutBottom) + dp(8),
            cameraGapStart = (cameraLeft - dp(8) - windowLeft).coerceIn(0, width),
            cameraGapEnd = (cameraRight + dp(8) - windowLeft).coerceIn(0, width),
        )
    }
}

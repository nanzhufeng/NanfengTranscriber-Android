package com.nanzhufeng.transcriber.ui.screens

import android.net.Uri
import android.media.MediaPlayer
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.nanzhufeng.transcriber.data.media.HistoryMediaKind
import com.nanzhufeng.transcriber.data.media.HistoryMediaPreviewStore
import com.nanzhufeng.transcriber.data.modelstore.OfficialModelCatalog
import com.nanzhufeng.transcriber.data.task.TranscriptionTaskEntity
import com.nanzhufeng.transcriber.domain.export.TranscriptExportFormat
import com.nanzhufeng.transcriber.domain.task.TranscriptionTaskState
import com.nanzhufeng.transcriber.ui.TranscriptionUiState
import com.nanzhufeng.transcriber.ui.theme.TranscriptPreviewOrange
import com.nanzhufeng.transcriber.ui.components.TaskMediaPreview
import com.nanzhufeng.transcriber.ui.components.WorkbenchCard
import com.nanzhufeng.transcriber.ui.components.formatDateTime
import com.nanzhufeng.transcriber.ui.components.formatDuration
import com.nanzhufeng.transcriber.ui.components.formatTranscriptPreview
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
fun HistoryScreen(
    state: TranscriptionUiState,
    tasks: List<TranscriptionTaskEntity>,
    expanded: Boolean,
    onOpenResult: (String) -> Unit,
    onCopyResult: (String) -> Unit,
    onCopyTranscriptDraft: () -> Unit,
    onDeleteTask: (String) -> Unit,
    onTranscriptDraftChanged: (String) -> Unit,
    onExport: (TranscriptExportFormat) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var period by rememberSaveable { mutableStateOf(HistoryPeriod.ALL) }
    var openedTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var openedMediaTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    val completed = remember(tasks, query, period, state.historyPreviews) {
        val keyword = query.trim()
        tasks.asSequence()
            .filter { it.state == TranscriptionTaskState.COMPLETED.name }
            .filter { period.includes(it.updatedAtMillis) }
            .filter {
                keyword.isEmpty() ||
                    it.sourceDisplayName.contains(keyword, ignoreCase = true) ||
                    state.historyPreviews[it.id].orEmpty().contains(keyword, ignoreCase = true)
            }
            .sortedByDescending(TranscriptionTaskEntity::updatedAtMillis)
            .toList()
    }
    val grouped = remember(completed) { completed.groupBy { formatHistoryDay(it.updatedAtMillis) } }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (expanded) 2 else 1),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "历史",
                    style = if (expanded) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    label = { Text("搜索文件名或转写文字") },
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    HistoryPeriod.entries.forEach { option ->
                        FilterChip(
                            selected = period == option,
                            onClick = { period = option },
                            label = { Text(option.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                        )
                    }
                }
            }

            if (grouped.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    EmptyHistory(
                        hasCompletedTasks = tasks.any { it.state == TranscriptionTaskState.COMPLETED.name },
                    )
                }
            } else {
                grouped.forEach { (day, records) ->
                    item(span = { GridItemSpan(maxLineSpan) }, key = "day-$day") {
                        Text(day, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    items(records, key = TranscriptionTaskEntity::id) { task ->
                        CompletedTimelineItem(
                            task = task,
                            preview = state.historyPreviews[task.id],
                            expanded = expanded,
                            onOpen = {
                                openedTaskId = task.id
                                onOpenResult(task.id)
                            },
                            onCopy = { onCopyResult(task.id) },
                            onOpenMedia = {
                                onOpenResult(task.id)
                                openedMediaTaskId = task.id
                            },
                            onDelete = { pendingDeleteId = task.id },
                        )
                    }
                }
            }
        }
        openedMediaTaskId?.let { taskId ->
            tasks.firstOrNull { it.id == taskId }?.let { task ->
                HistoryMediaReviewPane(
                    task = task,
                    state = state,
                    onDismiss = { openedMediaTaskId = null },
                    onCopy = onCopyTranscriptDraft,
                    onTranscriptDraftChanged = onTranscriptDraftChanged,
                    onExport = onExport,
                )
            }
        }
    }

    openedTaskId?.let { taskId ->
        val task = tasks.firstOrNull { it.id == taskId }
        HistoryResultDialog(
            task = task,
            state = state,
            onDismiss = { openedTaskId = null },
            onCopy = onCopyTranscriptDraft,
            onTranscriptDraftChanged = onTranscriptDraftChanged,
            onExport = onExport,
        )
    }

    pendingDeleteId?.let { taskId ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("删除这条历史记录？") },
            text = { Text("会删除 App 内保存的转写结果和记录；原音视频、模型缓存以及已经导出的文件不受影响。") },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDeleteId = null
                        onDeleteTask(taskId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("删除记录") }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteId = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun CompletedTimelineItem(
    task: TranscriptionTaskEntity,
    preview: String?,
    expanded: Boolean,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onOpenMedia: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by rememberSaveable(task.id) { mutableStateOf(false) }
    val previewText = formatTranscriptPreview(preview, if (expanded) 120 else 80)
    val modelName = if (task.userMessage?.startsWith("已直接提取内嵌字幕") == true) {
        "内嵌字幕"
    } else {
        OfficialModelCatalog.find(task.modelId)?.displayName ?: task.modelId
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.width(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = "已完成",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(if (expanded) 24.dp else 22.dp),
            )
            Text(
                formatHistoryClock(task.updatedAtMillis),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
            Box(
                Modifier.padding(top = 4.dp).width(1.dp).height(if (expanded) 60.dp else 54.dp)
                    .background(Color(0xFFD4E7DA)),
            )
        }
        WorkbenchCard(
            modifier = Modifier.weight(1f).clickable(onClick = onOpen),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TaskMediaPreview(
                    displayName = task.sourceDisplayName,
                    sourceUri = task.sourceUri,
                    stagedInputPath = task.stagedInputPath,
                    cacheTaskId = task.id,
                    modifier = Modifier.size(
                        width = if (expanded) 104.dp else 76.dp,
                        height = if (expanded) 78.dp else 64.dp,
                    ),
                    onClick = onOpenMedia,
                )
                Spacer(Modifier.width(if (expanded) 12.dp else 8.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            task.sourceDisplayName,
                            modifier = Modifier.weight(1f),
                            maxLines = if (expanded) 2 else 1,
                            overflow = TextOverflow.Ellipsis,
                            style = if (expanded) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.size(30.dp),
                            ) {
                                Icon(Icons.Outlined.MoreVert, contentDescription = "更多操作", modifier = Modifier.size(19.dp))
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("打开完整文字") },
                                    leadingIcon = { Icon(Icons.Outlined.OpenInFull, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onOpen()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("复制全部文字") },
                                    leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onCopy()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("播放原文件核对") },
                                    leadingIcon = { Icon(Icons.Filled.PlayCircle, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onOpenMedia()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text("删除历史记录") },
                                    leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
                                    onClick = {
                                        menuExpanded = false
                                        onDelete()
                                    },
                                )
                            }
                        }
                    }
                    Text(
                        previewText,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = TranscriptPreviewOrange,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        buildString {
                            task.totalDurationMillis?.let { append("${formatDuration(it)} · ") }
                            append(modelName)
                            append(" · ${task.language?.uppercase() ?: "自动识别"}")
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${if (task.threadCount == 0) "自动线程" else "${task.threadCount} 线程"} · ${formatDateTime(task.updatedAtMillis)}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryMediaReviewPane(
    task: TranscriptionTaskEntity,
    state: TranscriptionUiState,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onTranscriptDraftChanged: (String) -> Unit,
    onExport: (TranscriptExportFormat) -> Unit,
) {
    val context = LocalContext.current
    val store = remember(context.applicationContext) { HistoryMediaPreviewStore(context.applicationContext) }
    val kind = remember(task.id) { store.kindOf(task) }
    var sourceResolved by remember(task.id) { mutableStateOf(false) }
    var playableUri by remember(task.id) { mutableStateOf<Uri?>(null) }
    var playbackError by remember(task.id) { mutableStateOf<String?>(null) }
    var videoView by remember(task.id) { mutableStateOf<VideoView?>(null) }

    LaunchedEffect(task.id) {
        playableUri = withContext(Dispatchers.IO) { store.playableUri(task) }
        sourceResolved = true
    }
    DisposableEffect(task.id) {
        onDispose { videoView?.stopPlayback() }
    }
    BackHandler(onBack = onDismiss)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("原文件核对", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        task.sourceDisplayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(onClick = onDismiss) { Text("返回历史") }
            }
            WorkbenchCard(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when {
                    !sourceResolved -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator(modifier = Modifier.size(28.dp)) }
                    playableUri == null -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                if (kind == HistoryMediaKind.AUDIO) Icons.Outlined.MusicNote else Icons.Outlined.Movie,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(42.dp),
                            )
                            Text(
                                "原文件已被移动、删除或读取权限已失效。请从主页重新选择原文件后再核对；App 不会为此长期复制整段视频。",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    else -> {
                        if (kind == HistoryMediaKind.AUDIO) {
                            HistoryAudioReview(
                                uri = requireNotNull(playableUri),
                                onError = { playbackError = it },
                            )
                        } else {
                            AndroidView(
                                factory = { viewContext ->
                                    VideoView(viewContext).also { view ->
                                        videoView = view
                                        val controller = MediaController(viewContext)
                                        controller.setAnchorView(view)
                                        view.setMediaController(controller)
                                        view.setOnErrorListener { _, _, _ ->
                                            playbackError = "原文件无法在 App 内播放，请确认文件没有损坏，并检查系统是否支持该视频编码。"
                                            true
                                        }
                                    }
                                },
                                update = { view ->
                                    val uri = requireNotNull(playableUri)
                                    if (view.tag != uri.toString()) {
                                        view.tag = uri.toString()
                                        view.setVideoURI(uri)
                                        view.setOnPreparedListener {
                                            playbackError = null
                                            view.start()
                                            view.setOnPreparedListener(null)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(260.dp).background(Color.Black),
                            )
                        }
                        playbackError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider()
                        TranscriptEditorPanel(
                            task = task,
                            state = state,
                            onCopy = onCopy,
                            onTranscriptDraftChanged = onTranscriptDraftChanged,
                            onExport = onExport,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            fillAvailableHeight = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryAudioReview(
    uri: Uri,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    var player by remember(uri) { mutableStateOf<MediaPlayer?>(null) }
    var prepared by remember(uri) { mutableStateOf(false) }
    var playing by remember(uri) { mutableStateOf(false) }
    var durationMillis by remember(uri) { mutableStateOf(1) }
    var positionMillis by remember(uri) { mutableStateOf(0) }

    DisposableEffect(uri) {
        val mediaPlayer = MediaPlayer()
        val started = runCatching {
            mediaPlayer.setDataSource(context, uri)
            mediaPlayer.setOnPreparedListener { ready ->
                    prepared = true
                    durationMillis = ready.duration.coerceAtLeast(1)
                    ready.start()
                    playing = true
                }
            mediaPlayer.setOnCompletionListener {
                    playing = false
                    positionMillis = durationMillis
                }
            mediaPlayer.setOnErrorListener { _, _, _ ->
                    playing = false
                    onError("原音频无法在 App 内播放，请确认文件没有损坏，并检查读取权限。")
                    true
                }
            mediaPlayer.prepareAsync()
        }
        if (started.isFailure) {
            mediaPlayer.release()
            onError("原音频无法在 App 内播放，请确认文件没有损坏，并检查读取权限。")
            onDispose { }
        } else {
            player = mediaPlayer
            onDispose {
                player = null
                mediaPlayer.release()
            }
        }
    }
    LaunchedEffect(player, prepared) {
        while (prepared) {
            player?.let { current ->
                positionMillis = runCatching { current.currentPosition }.getOrDefault(positionMillis)
                playing = runCatching { current.isPlaying }.getOrDefault(false)
            }
            delay(300L)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Outlined.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(54.dp),
        )
        IconButton(
            onClick = {
                player?.let { current ->
                    if (current.isPlaying) current.pause() else current.start()
                    playing = current.isPlaying
                }
            },
            enabled = prepared,
            modifier = Modifier.size(54.dp),
        ) {
            Icon(
                if (playing) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                contentDescription = if (playing) "暂停" else "播放",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp),
            )
        }
        Slider(
            value = positionMillis.coerceIn(0, durationMillis).toFloat(),
            onValueChange = { value ->
                positionMillis = value.toInt()
                player?.seekTo(positionMillis)
            },
            valueRange = 0f..durationMillis.toFloat(),
            enabled = prepared,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "${formatPlaybackClock(positionMillis)} / ${formatPlaybackClock(durationMillis)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatPlaybackClock(milliseconds: Int): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0) / 1_000)
    return "%02d:%02d".format(Locale.ROOT, totalSeconds / 60, totalSeconds % 60)
}

@Composable
private fun HistoryResultDialog(
    task: TranscriptionTaskEntity?,
    state: TranscriptionUiState,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onTranscriptDraftChanged: (String) -> Unit,
    onExport: (TranscriptExportFormat) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("转写结果")
                Text(
                    task?.sourceDisplayName ?: "已完成任务",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        text = {
            TranscriptEditorPanel(
                task = task,
                state = state,
                onCopy = onCopy,
                onTranscriptDraftChanged = onTranscriptDraftChanged,
                onExport = onExport,
                modifier = Modifier.fillMaxWidth(),
                fillAvailableHeight = false,
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("返回历史") } },
    )
}

@Composable
private fun TranscriptEditorPanel(
    task: TranscriptionTaskEntity?,
    state: TranscriptionUiState,
    onCopy: () -> Unit,
    onTranscriptDraftChanged: (String) -> Unit,
    onExport: (TranscriptExportFormat) -> Unit,
    modifier: Modifier,
    fillAvailableHeight: Boolean,
) {
    val resultReady = task != null && state.openedResultTaskId == task.id && state.transcriptText != null
    if (!resultReady) {
        Column(
            modifier = modifier.fillMaxWidth().heightIn(min = 180.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(10.dp))
            Text("正在读取本机完整结果")
        }
        return
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        state.performanceSummary?.let { summary ->
            Text(
                formatPerformanceSeconds(summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("可编辑转写文字", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onCopy) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("复制全部")
            }
        }
        OutlinedTextField(
            value = requireNotNull(state.transcriptText),
            onValueChange = onTranscriptDraftChanged,
            modifier = if (fillAvailableHeight) {
                Modifier.fillMaxWidth().weight(1f)
            } else {
                Modifier.fillMaxWidth().heightIn(min = 240.dp, max = 460.dp)
            },
            label = { Text("转写文字") },
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            TranscriptExportFormat.entries.forEach { format ->
                OutlinedButton(
                    onClick = { onExport(format) },
                    enabled = !state.isExporting,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                ) {
                    Text(if (format == TranscriptExportFormat.MARKDOWN) "MD" else format.name)
                }
            }
        }
        Text(
            "SRT 保留原始时间轴分段，不套用全文编辑。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val MillisecondsPattern = Regex("(\\d+)\\s*ms")

private fun formatPerformanceSeconds(summary: String): String = MillisecondsPattern.replace(summary) { match ->
    val seconds = match.groupValues[1].toLongOrNull()?.div(1000.0) ?: return@replace match.value
    "%.2f 秒".format(Locale.ROOT, seconds)
}

@Composable
private fun EmptyHistory(hasCompletedTasks: Boolean) {
    WorkbenchCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            if (hasCompletedTasks) "没有匹配的已完成结果" else "还没有已完成的转写",
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            if (hasCompletedTasks) "可以搜索文件名或转写文字。" else "转写完成后会自动归档到这里。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatHistoryDay(value: Long): String {
    val date = Calendar.getInstance().apply { timeInMillis = value }
    val now = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        sameDay(date, now) -> "今天"
        sameDay(date, yesterday) -> "昨天"
        else -> SimpleDateFormat("yyyy 年 M 月 d 日", Locale.SIMPLIFIED_CHINESE).format(Date(value))
    }
}

private fun sameDay(left: Calendar, right: Calendar): Boolean =
    left.get(Calendar.ERA) == right.get(Calendar.ERA) &&
        left.get(Calendar.YEAR) == right.get(Calendar.YEAR) &&
        left.get(Calendar.DAY_OF_YEAR) == right.get(Calendar.DAY_OF_YEAR)

private fun formatHistoryClock(value: Long): String =
    SimpleDateFormat("HH:mm", Locale.SIMPLIFIED_CHINESE).format(Date(value))

private enum class HistoryPeriod(val label: String, val days: Long?) {
    ALL("全部时间", null),
    TODAY("今天", 1L),
    LAST_7_DAYS("近 7 天", 7L),
    LAST_30_DAYS("近 30 天", 30L),
    ;

    fun includes(timestamp: Long, now: Long = System.currentTimeMillis()): Boolean {
        val dayCount = days ?: return true
        if (this == TODAY) {
            val candidate = Calendar.getInstance().apply { timeInMillis = timestamp }
            val current = Calendar.getInstance().apply { timeInMillis = now }
            return sameDay(candidate, current)
        }
        return timestamp >= now - TimeUnit.DAYS.toMillis(dayCount)
    }
}

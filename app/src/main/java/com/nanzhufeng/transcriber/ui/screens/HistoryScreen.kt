package com.nanzhufeng.transcriber.ui.screens

import android.net.Uri
import android.media.MediaPlayer
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
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
import com.nanzhufeng.transcriber.ui.ExportFeedbackTone
import com.nanzhufeng.transcriber.ui.theme.TranscriptPreviewOrange
import com.nanzhufeng.transcriber.ui.components.TaskMediaPreview
import com.nanzhufeng.transcriber.ui.components.SubtleActionButton
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
    lastExportFormat: TranscriptExportFormat,
    tasks: List<TranscriptionTaskEntity>,
    expanded: Boolean,
    onOpenResult: (String) -> Unit,
    onCopyResult: (String) -> Unit,
    onCopyTranscriptDraft: () -> Unit,
    onDeleteTask: (String) -> Unit,
    onDeleteTasks: (Set<String>) -> Unit,
    onCleanInvalidHistory: () -> Unit,
    onTranscriptDraftChanged: (String) -> Unit,
    onExport: (TranscriptExportFormat) -> Unit,
    onExportFormatChanged: (TranscriptExportFormat) -> Unit,
    onOpenMediaReview: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var period by rememberSaveable { mutableStateOf(HistoryPeriod.ALL) }
    var openedTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectedHistoryIds by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }
    var pendingBulkDeleteIds by rememberSaveable { mutableStateOf<Set<String>>(emptySet()) }
    var confirmCleanInvalid by rememberSaveable { mutableStateOf(false) }
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
    val completedIds = remember(completed) { completed.mapTo(mutableSetOf(), TranscriptionTaskEntity::id) }
    val missingResultCount = completed.count { it.id in state.invalidHistoryTaskIds }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (expanded) 2 else 1),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(if (expanded) 10.dp else 12.dp),
            horizontalArrangement = Arrangement.spacedBy(if (expanded) 12.dp else 14.dp),
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
                    shape = RoundedCornerShape(percent = 50),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                    ),
                )
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                HistoryActionBar(
                    period = period,
                    selectionMode = selectionMode,
                    selectedCount = selectedHistoryIds.size,
                    availableIds = completedIds,
                    missingResultCount = missingResultCount,
                    onPeriodChange = {
                        period = it
                        selectedHistoryIds = emptySet()
                    },
                    onStartSelection = { selectionMode = true },
                    onSelectAll = { selectedHistoryIds = completedIds },
                    onCancelSelection = {
                        selectionMode = false
                        selectedHistoryIds = emptySet()
                    },
                    onDeleteSelected = { pendingBulkDeleteIds = selectedHistoryIds },
                    onCleanInvalidHistory = { confirmCleanInvalid = true },
                )
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
                            selectionMode = selectionMode,
                            selected = task.id in selectedHistoryIds,
                            isResultInvalid = task.id in state.invalidHistoryTaskIds,
                            onSelectionChange = {
                                selectedHistoryIds = if (task.id in selectedHistoryIds) {
                                    selectedHistoryIds - task.id
                                } else {
                                    selectedHistoryIds + task.id
                                }
                            },
                            onOpen = {
                                openedTaskId = task.id
                                onOpenResult(task.id)
                            },
                            onCopy = { onCopyResult(task.id) },
                            onOpenMedia = { onOpenMediaReview(task.id) },
                            onDelete = { pendingDeleteId = task.id },
                        )
                    }
                }
            }
        }
    }

    openedTaskId?.let { taskId ->
        val task = tasks.firstOrNull { it.id == taskId }
        HistoryResultPane(
            task = task,
            state = state,
            onDismiss = { openedTaskId = null },
            onCopy = onCopyTranscriptDraft,
            onTranscriptDraftChanged = onTranscriptDraftChanged,
            onExport = onExport,
            lastExportFormat = lastExportFormat,
            onExportFormatChanged = onExportFormatChanged,
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

    if (pendingBulkDeleteIds.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { pendingBulkDeleteIds = emptySet() },
            title = { Text("删除 ${pendingBulkDeleteIds.size} 条历史记录？") },
            text = { Text("只删除 App 内的转写结果和历史记录；原音视频、模型缓存及已导出文件不受影响。") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTasks(pendingBulkDeleteIds)
                        pendingBulkDeleteIds = emptySet()
                        selectedHistoryIds = emptySet()
                        selectionMode = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("删除记录") }
            },
            dismissButton = { TextButton(onClick = { pendingBulkDeleteIds = emptySet() }) { Text("取消") } },
        )
    }

    if (confirmCleanInvalid) {
        AlertDialog(
            onDismissRequest = { confirmCleanInvalid = false },
            title = { Text("清理失效历史？") },
            text = { Text("仅删除转写结果文件已不存在的历史记录；原音视频、模型缓存和导出文件均不会受到影响。") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmCleanInvalid = false
                        onCleanInvalidHistory()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("清理记录") }
            },
            dismissButton = { TextButton(onClick = { confirmCleanInvalid = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun HistoryActionBar(
    period: HistoryPeriod,
    selectionMode: Boolean,
    selectedCount: Int,
    availableIds: Set<String>,
    missingResultCount: Int,
    onPeriodChange: (HistoryPeriod) -> Unit,
    onStartSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onCancelSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onCleanInvalidHistory: () -> Unit,
) {
    var periodMenuExpanded by rememberSaveable { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box {
            FilterChip(
                selected = true,
                onClick = { periodMenuExpanded = true },
                label = { Text(period.label) },
                border = null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
            DropdownMenu(expanded = periodMenuExpanded, onDismissRequest = { periodMenuExpanded = false }) {
                HistoryPeriod.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            periodMenuExpanded = false
                            onPeriodChange(option)
                        },
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        if (selectionMode) {
            Text("已选 $selectedCount 项", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            TextButton(onClick = onSelectAll, enabled = availableIds.isNotEmpty()) { Text("全选") }
            TextButton(onClick = onCancelSelection) { Text("取消") }
            TextButton(onClick = onDeleteSelected, enabled = selectedCount > 0) { Text("删除") }
        } else {
            TextButton(onClick = onStartSelection) { Text("批量删除") }
            if (missingResultCount > 0) {
                TextButton(onClick = onCleanInvalidHistory) { Text("清理失效（$missingResultCount）") }
            }
        }
    }
}

@Composable
private fun CompletedTimelineItem(
    task: TranscriptionTaskEntity,
    preview: String?,
    expanded: Boolean,
    selectionMode: Boolean,
    selected: Boolean,
    isResultInvalid: Boolean,
    onSelectionChange: () -> Unit,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onOpenMedia: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by rememberSaveable(task.id) { mutableStateOf(false) }
    var showTranscriptionInfo by rememberSaveable(task.id) { mutableStateOf(false) }
    val previewText = formatTranscriptPreview(preview, if (expanded) 120 else 80)
    val modelName = if (task.userMessage?.startsWith("已直接提取内嵌字幕") == true) {
        "内嵌字幕"
    } else {
        OfficialModelCatalog.find(task.modelId)?.displayName ?: task.modelId
    }
    val primaryAction = if (selectionMode) onSelectionChange else onOpen
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(48.dp).fillMaxHeight()) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .offset(y = 48.dp)
                    .width(1.dp)
                    .height(if (expanded) 60.dp else 54.dp)
                    .background(Color(0xFFD4E7DA)),
            )
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IconButton(onClick = if (selectionMode) onSelectionChange else ({}), modifier = Modifier.size(if (expanded) 30.dp else 28.dp)) {
                    Icon(
                        if (selectionMode && !selected) Icons.Outlined.RadioButtonUnchecked else Icons.Filled.CheckCircle,
                        contentDescription = if (selectionMode) "选择 ${task.sourceDisplayName}" else "已完成",
                        tint = if (selected || !selectionMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(if (expanded) 24.dp else 22.dp),
                    )
                }
                Text(
                    formatHistoryClock(task.updatedAtMillis),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        WorkbenchCard(
            modifier = Modifier.weight(1f).clickable(onClick = primaryAction),
            contentPadding = if (expanded) {
                PaddingValues(horizontal = 12.dp, vertical = 10.dp)
            } else {
                PaddingValues(16.dp)
            },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TaskMediaPreview(
                    displayName = task.sourceDisplayName,
                    sourceUri = task.sourceUri,
                    stagedInputPath = task.stagedInputPath,
                    cacheTaskId = task.id,
                    modifier = Modifier.size(
                        width = if (expanded) 84.dp else 76.dp,
                        height = if (expanded) 64.dp else 64.dp,
                    ),
                    onClick = if (selectionMode) onSelectionChange else onOpenMedia,
                )
                Spacer(Modifier.width(if (expanded) 8.dp else 8.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(if (expanded) 2.dp else 3.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            task.sourceDisplayName,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (!selectionMode) Box {
                            IconButton(
                                onClick = { menuExpanded = true },
                                modifier = Modifier.size(30.dp),
                            ) {
                                Icon(Icons.Outlined.MoreVert, contentDescription = "更多操作", modifier = Modifier.size(19.dp))
                            }
                            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                                task.technicalDetail?.takeIf(String::isNotBlank)?.let { detail ->
                                    DropdownMenuItem(
                                        text = { Text("转写信息") },
                                        onClick = {
                                            menuExpanded = false
                                            showTranscriptionInfo = true
                                        },
                                    )
                                }
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall,
                        color = TranscriptPreviewOrange,
                        fontWeight = FontWeight.Medium,
                    )
                    if (isResultInvalid) {
                        Text(
                            "结果文件已失效，可用“清理失效”删除记录",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
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
    if (showTranscriptionInfo) {
        AlertDialog(
            onDismissRequest = { showTranscriptionInfo = false },
            title = { Text("转写信息") },
            text = {
                Text(
                    formatPerformanceSeconds(task.technicalDetail.orEmpty()),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = { TextButton(onClick = { showTranscriptionInfo = false }) { Text("关闭") } },
        )
    }
}

@Composable
fun HistoryMediaReviewPane(
    task: TranscriptionTaskEntity,
    state: TranscriptionUiState,
    expanded: Boolean,
    lastExportFormat: TranscriptExportFormat,
    initialVideoPositionMillis: Long,
    initialVideoPlayWhenReady: Boolean,
    onVideoPlaybackSnapshot: (positionMillis: Long, playWhenReady: Boolean) -> Unit,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onTranscriptDraftChanged: (String) -> Unit,
    onExport: (TranscriptExportFormat) -> Unit,
    onExportFormatChanged: (TranscriptExportFormat) -> Unit,
) {
    val context = LocalContext.current
    val store = remember(context.applicationContext) { HistoryMediaPreviewStore(context.applicationContext) }
    val kind = remember(task.id) { store.kindOf(task) }
    var sourceResolved by remember(task.id) { mutableStateOf(false) }
    var playableUri by remember(task.id) { mutableStateOf<Uri?>(null) }
    var playbackError by remember(task.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(task.id) {
        playableUri = withContext(Dispatchers.IO) { store.playableUri(task) }
        sourceResolved = true
    }
    BackHandler(onBack = onDismiss)

    if (kind == HistoryMediaKind.IMAGE && sourceResolved && playableUri != null) {
        HistoryImageReviewPane(
            title = task.sourceDisplayName,
            uri = requireNotNull(playableUri),
            stagedInputPath = task.stagedInputPath,
            store = store,
            onDismiss = onDismiss,
        )
        return
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp),
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
                            HistoryVideoReview(
                                uri = requireNotNull(playableUri),
                                expanded = expanded,
                                initialPositionMillis = initialVideoPositionMillis,
                                initialPlayWhenReady = initialVideoPlayWhenReady,
                                onPlaybackSnapshot = onVideoPlaybackSnapshot,
                                onError = { playbackError = it },
                            )
                        }
                        playbackError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider()
                        TranscriptEditorPanel(
                            task = task,
                            state = state,
                            lastExportFormat = lastExportFormat,
                            onCopy = onCopy,
                            onTranscriptDraftChanged = onTranscriptDraftChanged,
                            onExport = onExport,
                            onExportFormatChanged = onExportFormatChanged,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            fillAvailableHeight = true,
                        )
                    }
                }
            }
        }
    }
}

/**
 * VideoView is deliberately kept as the decoder only.  The controls are Compose content inside
 * this clipped frame, so Android's MediaController cannot draw outside the source preview.
 */
@Composable
private fun HistoryVideoReview(
    uri: Uri,
    expanded: Boolean,
    initialPositionMillis: Long,
    initialPlayWhenReady: Boolean,
    onPlaybackSnapshot: (positionMillis: Long, playWhenReady: Boolean) -> Unit,
    onError: (String) -> Unit,
) {
    val currentOnPlaybackSnapshot by rememberUpdatedState(onPlaybackSnapshot)
    var videoView by remember(uri) { mutableStateOf<VideoView?>(null) }
    var prepared by remember(uri) { mutableStateOf(false) }
    var playing by remember(uri) { mutableStateOf(false) }
    var durationMillis by remember(uri) { mutableStateOf(1) }
    var positionMillis by remember(uri) { mutableStateOf(initialPositionMillis.coerceAtLeast(0L).toInt()) }
    var seeking by remember(uri) { mutableStateOf(false) }
    var pendingProgress by remember(uri) { mutableStateOf(0f) }
    var controlsVisible by rememberSaveable(uri) { mutableStateOf(true) }

    fun setPlayback(shouldPlay: Boolean) {
        videoView?.let { view ->
            if (shouldPlay) view.start() else view.pause()
            playing = shouldPlay
            currentOnPlaybackSnapshot(positionMillis.toLong(), playing)
        }
    }

    DisposableEffect(uri) {
        onDispose {
            videoView?.let { view ->
                currentOnPlaybackSnapshot(
                    runCatching { view.currentPosition.toLong() }.getOrDefault(positionMillis.toLong()),
                    playing,
                )
                view.stopPlayback()
            }
        }
    }
    LaunchedEffect(videoView, prepared, seeking) {
        while (prepared) {
            videoView?.let { view ->
                if (!seeking) {
                    positionMillis = runCatching { view.currentPosition }.getOrDefault(positionMillis)
                        .coerceIn(0, durationMillis)
                }
                currentOnPlaybackSnapshot(positionMillis.toLong(), playing)
            }
            delay(300L)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Keep the compact/outer screen unchanged.  On the expanded inner screen, 8:3
            // keeps the full width while reducing the former 16:9 frame height by one third,
            // so the transcript editor retains the released reading space.
            .aspectRatio(if (expanded) 8f / 3f else 16f / 9f)
            .clip(MaterialTheme.shapes.large)
            .background(Color.Black),
    ) {
        AndroidView(
            factory = { viewContext ->
                VideoView(viewContext).also { view ->
                    videoView = view
                    view.setOnErrorListener { _, _, _ ->
                        playing = false
                        onError("原文件无法在 App 内播放，请确认文件没有损坏，并检查系统是否支持该视频编码。")
                        true
                    }
                    view.setOnPreparedListener { ready ->
                        prepared = true
                        durationMillis = ready.duration.coerceAtLeast(1)
                        positionMillis = initialPositionMillis.coerceIn(0L, durationMillis.toLong()).toInt()
                        ready.seekTo(positionMillis)
                        if (initialPlayWhenReady) ready.start()
                        playing = initialPlayWhenReady
                        currentOnPlaybackSnapshot(positionMillis.toLong(), playing)
                    }
                    view.setOnCompletionListener {
                        playing = false
                        positionMillis = durationMillis
                        currentOnPlaybackSnapshot(positionMillis.toLong(), false)
                    }
                }
            },
            update = { view ->
                if (view.tag != uri.toString()) {
                    view.tag = uri.toString()
                    prepared = false
                    playing = false
                    positionMillis = initialPositionMillis.coerceAtLeast(0L).toInt()
                    view.setVideoURI(uri)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(uri) {
                    detectTapGestures(
                        onTap = { controlsVisible = !controlsVisible },
                        onDoubleTap = {
                            controlsVisible = true
                            setPlayback(!playing)
                        },
                    )
                },
        )
        if (controlsVisible) {
            IconButton(
                onClick = { setPlayback(!playing) },
                enabled = prepared,
                modifier = Modifier.align(Alignment.Center).size(70.dp),
            ) {
                Icon(
                    if (playing) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                    contentDescription = if (playing) "暂停视频" else "播放视频",
                    tint = Color.White,
                    modifier = Modifier.size(62.dp),
                )
            }
            Surface(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                color = Color.Black.copy(alpha = 0.62f),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                IconButton(
                    onClick = { setPlayback(!playing) },
                    enabled = prepared,
                    modifier = Modifier.size(42.dp),
                ) {
                    Icon(
                        if (playing) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                        contentDescription = if (playing) "暂停" else "播放",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Slider(
                    value = if (seeking) pendingProgress else positionMillis.toFloat() / durationMillis,
                    onValueChange = {
                        seeking = true
                        pendingProgress = it
                    },
                    onValueChangeFinished = {
                        val position = (pendingProgress * durationMillis).toInt()
                        videoView?.seekTo(position)
                        positionMillis = position
                        seeking = false
                        currentOnPlaybackSnapshot(positionMillis.toLong(), playing)
                    },
                    enabled = prepared,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${formatPlaybackClock(positionMillis)} / ${formatPlaybackClock(durationMillis)}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            }
        }
    }
}

/** The same local-only, full-screen image-review interaction used by 南枫下载 history. */
@Composable
private fun HistoryImageReviewPane(
    title: String,
    uri: Uri,
    stagedInputPath: String?,
    store: HistoryMediaPreviewStore,
    onDismiss: () -> Unit,
) {
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, uri, stagedInputPath) {
        value = withContext(Dispatchers.IO) {
            store.loadSourceThumbnail(uri, stagedInputPath?.let(java.nio.file.Paths::get), title)
        }
    }
    BackHandler(onBack = onDismiss)
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Box(modifier = Modifier.fillMaxSize()) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "$title 图片原件",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            } ?: CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.52f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                TextButton(onClick = onDismiss) { Text("关闭", color = Color.White) }
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
private fun HistoryResultPane(
    task: TranscriptionTaskEntity?,
    state: TranscriptionUiState,
    lastExportFormat: TranscriptExportFormat,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onTranscriptDraftChanged: (String) -> Unit,
    onExport: (TranscriptExportFormat) -> Unit,
    onExportFormatChanged: (TranscriptExportFormat) -> Unit,
) {
    BackHandler(onBack = onDismiss)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("转写结果", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        task?.sourceDisplayName ?: "已完成任务",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                TextButton(onClick = onDismiss) { Text("返回历史") }
            }
            HorizontalDivider()
            TranscriptEditorPanel(
                task = task,
                state = state,
                lastExportFormat = lastExportFormat,
                onCopy = onCopy,
                onTranscriptDraftChanged = onTranscriptDraftChanged,
                onExport = onExport,
                onExportFormatChanged = onExportFormatChanged,
                modifier = Modifier.fillMaxWidth().weight(1f),
                fillAvailableHeight = true,
            )
        }
    }
}

@Composable
private fun TranscriptEditorPanel(
    task: TranscriptionTaskEntity?,
    state: TranscriptionUiState,
    lastExportFormat: TranscriptExportFormat,
    onCopy: () -> Unit,
    onTranscriptDraftChanged: (String) -> Unit,
    onExport: (TranscriptExportFormat) -> Unit,
    onExportFormatChanged: (TranscriptExportFormat) -> Unit,
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
    val resultTaskId = requireNotNull(task).id
    var selectedExportFormat by rememberSaveable(resultTaskId) { mutableStateOf(lastExportFormat) }
    LaunchedEffect(lastExportFormat) {
        selectedExportFormat = lastExportFormat
    }
    var exportFormatMenuExpanded by rememberSaveable(resultTaskId) { mutableStateOf(false) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("转写文字", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
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
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF4F5F4),
                unfocusedContainerColor = Color(0xFFF4F5F4),
                disabledContainerColor = Color(0xFFF4F5F4),
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SubtleActionButton(
                    onClick = { exportFormatMenuExpanded = true },
                    enabled = !state.isExporting,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            if (selectedExportFormat == TranscriptExportFormat.MARKDOWN) "MD" else selectedExportFormat.name,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        Icon(
                            Icons.Filled.ArrowDropDown,
                            contentDescription = "选择导出格式",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                DropdownMenu(
                    expanded = exportFormatMenuExpanded,
                    onDismissRequest = { exportFormatMenuExpanded = false },
                ) {
                    TranscriptExportFormat.entries.forEach { format ->
                        DropdownMenuItem(
                            text = { Text(if (format == TranscriptExportFormat.MARKDOWN) "MD" else format.name) },
                            onClick = {
                                selectedExportFormat = format
                                onExportFormatChanged(format)
                                exportFormatMenuExpanded = false
                            },
                        )
                    }
                }
            }
            Button(
                onClick = { onExport(selectedExportFormat) },
                enabled = !state.isExporting,
                modifier = Modifier.weight(1f),
            ) { Text("导出") }
        }
        state.exportFeedback?.let { feedback ->
            Text(
                feedback.message,
                style = MaterialTheme.typography.bodySmall,
                color = when (feedback.tone) {
                    ExportFeedbackTone.WORKING -> MaterialTheme.colorScheme.onSurfaceVariant
                    ExportFeedbackTone.SUCCESS -> MaterialTheme.colorScheme.primary
                    ExportFeedbackTone.ERROR -> MaterialTheme.colorScheme.error
                },
            )
        }
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

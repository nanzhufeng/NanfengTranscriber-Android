package com.nanzhufeng.transcriber.ui.screens

import android.app.Activity
import android.net.Uri
import android.view.DragEvent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Downloading
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nanzhufeng.transcriber.R
import com.nanzhufeng.transcriber.data.modelstore.OfficialModelCatalog
import com.nanzhufeng.transcriber.data.task.TranscriptionTaskEntity
import com.nanzhufeng.transcriber.data.task.TranscriptionOutputFormat
import com.nanzhufeng.transcriber.domain.model.ModelInstallState
import com.nanzhufeng.transcriber.domain.task.TranscriptionTaskState
import com.nanzhufeng.transcriber.domain.task.TaskSelectionPolicy
import com.nanzhufeng.transcriber.ui.PendingSourceUi
import com.nanzhufeng.transcriber.ui.TranscriptionUiState
import com.nanzhufeng.transcriber.ui.components.SectionHeading
import com.nanzhufeng.transcriber.ui.components.TaskMediaPreview
import com.nanzhufeng.transcriber.ui.components.WorkbenchCard
import com.nanzhufeng.transcriber.ui.components.formatBytes
import com.nanzhufeng.transcriber.ui.components.formatDateTime
import com.nanzhufeng.transcriber.ui.components.formatDuration
import com.nanzhufeng.transcriber.ui.components.taskState
import com.nanzhufeng.transcriber.ui.components.taskStateColor
import com.nanzhufeng.transcriber.ui.components.taskStateLabel
import com.nanzhufeng.transcriber.ui.theme.TranscriptPreviewOrange

@Composable
fun HomeScreen(
    state: TranscriptionUiState,
    tasks: List<TranscriptionTaskEntity>,
    expanded: Boolean,
    onChooseSource: () -> Unit,
    onChooseFolder: () -> Unit,
    onDropSources: (List<Uri>) -> Unit,
    onStart: () -> Unit,
    onTogglePendingSource: (String) -> Unit,
    onRemovePendingSource: (String) -> Unit,
    onUpdatePendingOptions: (String, String, String?, TranscriptionOutputFormat) -> Unit,
    onToggleTaskSelection: (String) -> Unit,
    onSelectAllStartable: (Boolean) -> Unit,
    onRetryTask: (String) -> Unit,
    onCancelTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    var pendingDelete by rememberSaveable { mutableStateOf<String?>(null) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        HomeHeader(expanded)
        Spacer(Modifier.height(12.dp))

        if (expanded) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    WorkflowOverview(state, tasks)
                    Spacer(Modifier.height(12.dp))
                    CurrentWorkCard(
                        state = state,
                        tasks = tasks,
                        expanded = true,
                        onChooseSource = onChooseSource,
                        onOpenSettings = onOpenSettings,
                        onRetryTask = onRetryTask,
                        onCancelTask = onCancelTask,
                        onDeleteTask = { pendingDelete = it },
                        onTogglePendingSource = onTogglePendingSource,
                        onRemovePendingSource = onRemovePendingSource,
                        onUpdatePendingOptions = onUpdatePendingOptions,
                        onToggleTaskSelection = onToggleTaskSelection,
                        onSelectAllStartable = onSelectAllStartable,
                        onStart = onStart,
                        modifier = Modifier.weight(1f),
                    )
                }
                ActionDock(
                    state = state,
                    onChooseSource = onChooseSource,
                    onChooseFolder = onChooseFolder,
                    onDropSources = onDropSources,
                    onOpenSettings = onOpenSettings,
                    modifier = Modifier.width(336.dp),
                )
            }
        } else {
            WorkflowOverview(state, tasks)
            Spacer(Modifier.height(10.dp))
            CurrentWorkCard(
                state = state,
                tasks = tasks,
                expanded = false,
                onChooseSource = onChooseSource,
                onOpenSettings = onOpenSettings,
                onRetryTask = onRetryTask,
                onCancelTask = onCancelTask,
                onDeleteTask = { pendingDelete = it },
                onTogglePendingSource = onTogglePendingSource,
                onRemovePendingSource = onRemovePendingSource,
                onUpdatePendingOptions = onUpdatePendingOptions,
                onToggleTaskSelection = onToggleTaskSelection,
                onSelectAllStartable = onSelectAllStartable,
                onStart = onStart,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.height(10.dp))
            ActionDock(
                state = state,
                onChooseSource = onChooseSource,
                onChooseFolder = onChooseFolder,
                onDropSources = onDropSources,
                onOpenSettings = onOpenSettings,
            )
        }
    }

    pendingDelete?.let { taskId ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除这条转写任务？") },
            text = {
                Text("正在运行的任务会先安全停止。只删除任务记录和 App 内临时文件，不会删除原音视频、已导出文件或模型缓存。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDelete = null
                        onDeleteTask(taskId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("删除任务") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun HomeHeader(expanded: Boolean) {
    if (expanded) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("主页", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "批量加入音视频，任务可在后台顺序转写",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.mipmap.app_icon),
                contentDescription = "南枫转写",
                modifier = Modifier.size(52.dp),
            )
            Text(
                text = "南枫转写",
                modifier = Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun WorkflowOverview(state: TranscriptionUiState, tasks: List<TranscriptionTaskEntity>) {
    val unfinished = tasks.filter { taskState(it) != TranscriptionTaskState.COMPLETED }
    val activeCount = unfinished.count {
        taskState(it) in setOf(
            TranscriptionTaskState.PREPARING,
            TranscriptionTaskState.TRANSCRIBING,
            TranscriptionTaskState.EXPORTING,
        )
    }
    val waitingCount = unfinished.count { taskState(it) == TranscriptionTaskState.QUEUED }
    WorkbenchCard {
        Text(
            text = "运行状态",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatusMetric(
                icon = Icons.Filled.CheckCircle,
                label = "模型",
                value = if (state.modelState == ModelInstallState.READY) "已缓存" else "待下载",
                color = if (state.modelState == ModelInstallState.READY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            )
            StatusMetric(Icons.Filled.Downloading, "进行中", activeCount.toString(), MaterialTheme.colorScheme.primary)
            StatusMetric(Icons.Filled.HourglassEmpty, "等待中", waitingCount.toString(), MaterialTheme.colorScheme.secondary)
            val visibleProgress = if (activeCount > 0) state.progress ?: 0f else 0f
            StatusMetric(Icons.Filled.Speed, "总进度", "${(visibleProgress * 100).toInt()}%", MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun StatusMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CurrentWorkCard(
    state: TranscriptionUiState,
    tasks: List<TranscriptionTaskEntity>,
    expanded: Boolean,
    onChooseSource: () -> Unit,
    onOpenSettings: () -> Unit,
    onRetryTask: (String) -> Unit,
    onCancelTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onTogglePendingSource: (String) -> Unit,
    onRemovePendingSource: (String) -> Unit,
    onUpdatePendingOptions: (String, String, String?, TranscriptionOutputFormat) -> Unit,
    onToggleTaskSelection: (String) -> Unit,
    onSelectAllStartable: (Boolean) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var editingSource by remember { mutableStateOf<PendingSourceUi?>(null) }
    editingSource?.let { source ->
        PendingSourceOptionsDialog(
            source = source,
            onDismiss = { editingSource = null },
            onSave = { modelId, language, outputFormat ->
                onUpdatePendingOptions(source.key, modelId, language, outputFormat)
                editingSource = null
            },
        )
    }
    val pendingTasks = tasks
        .filter { taskState(it) != TranscriptionTaskState.COMPLETED }
        .sortedBy(TranscriptionTaskEntity::queuePosition)
    val listState = rememberLazyListState()
    val selectableTasks = pendingTasks.filter { TaskSelectionPolicy.canSelectForStart(taskState(it)) }
    val selectedCount = state.pendingSourceItems.count(PendingSourceUi::selected) +
        state.selectedTaskIds.count { selectedId -> selectableTasks.any { it.id == selectedId } }
    val selectableCount = state.pendingSourceItems.size + selectableTasks.size
    val allSelected = selectableCount > 0 && selectedCount == selectableCount
    val hasSelectedNewSource = state.pendingSourceItems.any(PendingSourceUi::selected)
    val canStartSelected = selectedCount > 0 && !state.isBusy
    val activeIndex = pendingTasks.indexOfFirst {
        taskState(it) in setOf(
            TranscriptionTaskState.PREPARING,
            TranscriptionTaskState.TRANSCRIBING,
            TranscriptionTaskState.EXPORTING,
        )
    }
    LaunchedEffect(activeIndex, pendingTasks.size) {
        if (activeIndex >= 0) listState.animateScrollToItem(state.pendingSourceItems.size + activeIndex)
    }
    WorkbenchCard(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SectionHeading(title = "转写列表")
            Spacer(Modifier.weight(1f))
            if (selectableCount > 0) {
                TextButton(onClick = { onSelectAllStartable(!allSelected) }) {
                    Text(if (allSelected) "取消全选" else "全选")
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        if (pendingTasks.isEmpty() && state.pendingSourceItems.isEmpty()) {
            EmptyWorkspace(hasHistory = tasks.isNotEmpty())
        } else {
            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().padding(end = 7.dp),
                ) {
                    itemsIndexed(state.pendingSourceItems, key = { _, source -> "source-${source.key}" }) { _, source ->
                        PendingSourceRow(
                            source = source,
                            expanded = expanded,
                            onToggleSelection = { onTogglePendingSource(source.key) },
                            onEdit = { editingSource = source },
                            onDelete = { onRemovePendingSource(source.key) },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.68f))
                    }
                    itemsIndexed(pendingTasks, key = { _, task -> task.id }) { index, task ->
                        TranscriptionQueueRow(
                            task = task,
                            expanded = expanded,
                            selected = task.id in state.selectedTaskIds,
                            onToggleSelection = { onToggleTaskSelection(task.id) },
                            onChooseSource = onChooseSource,
                            onOpenSettings = onOpenSettings,
                            onRetryTask = onRetryTask,
                            onCancelTask = onCancelTask,
                            onDeleteTask = onDeleteTask,
                        )
                        if (index < pendingTasks.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.68f))
                        }
                    }
                }
                QueueScrollIndicator(
                    state = listState,
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(7.dp).padding(vertical = 4.dp, horizontal = 2.dp),
                )
            }
            if (selectedCount > 0) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onStart,
                    enabled = canStartSelected,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Text("开始转写（$selectedCount）")
                }
                if (hasSelectedNewSource && state.modelState != ModelInstallState.READY) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "内嵌字幕可直接提取；其余任务将等待模型",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.secondary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        TextButton(onClick = onOpenSettings) { Text("准备模型") }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingSourceRow(
    source: PendingSourceUi,
    expanded: Boolean,
    onToggleSelection: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(if (expanded) 84.dp else 78.dp).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(
            modifier = Modifier.size(
                width = if (expanded) 92.dp else 76.dp,
                height = if (expanded) 64.dp else 56.dp,
            ),
        ) {
            TaskMediaPreview(
                displayName = source.displayName,
                sourceUri = source.sourceUri,
                stagedInputPath = source.stagedInputPath,
                modifier = Modifier.fillMaxSize(),
            )
            IconButton(
                onClick = onToggleSelection,
                modifier = Modifier.align(Alignment.TopStart).size(30.dp).clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.88f)),
            ) {
                Icon(
                    imageVector = if (source.selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = if (source.selected) "取消选择 ${source.displayName}" else "选择 ${source.displayName}",
                    tint = if (source.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onEdit)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                source.displayName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                listOfNotNull(
                    source.bytes?.let(::formatBytes),
                    source.modelLabel.substringBefore('｜'),
                    source.language?.uppercase() ?: "自动识别",
                    source.outputFormat.name,
                ).joinToString(" · "),
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        QueueIconButton(
            icon = Icons.Outlined.Settings,
            description = "编辑 ${source.displayName} 的转写参数",
            onClick = onEdit,
        )
        QueueIconButton(
            icon = Icons.Outlined.DeleteOutline,
            description = "移除 ${source.displayName}",
            tint = MaterialTheme.colorScheme.error,
            onClick = onDelete,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PendingSourceOptionsDialog(
    source: PendingSourceUi,
    onDismiss: () -> Unit,
    onSave: (String, String?, TranscriptionOutputFormat) -> Unit,
) {
    var modelId by rememberSaveable(source.key) { mutableStateOf(source.modelId) }
    var languageKey by rememberSaveable(source.key) { mutableStateOf(source.language ?: "auto") }
    var outputName by rememberSaveable(source.key) { mutableStateOf(source.outputFormat.name) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("单项转写设置") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    source.displayName,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                QueueSelectionField(
                    label = "转写模型",
                    selectedValue = modelId,
                    options = OfficialModelCatalog.candidates.map { it.manifest.modelId to it.displayName },
                    onSelected = { modelId = it },
                )
                QueueSelectionField(
                    label = "识别语言",
                    selectedValue = languageKey,
                    options = listOf("auto" to "自动识别", "zh" to "中文", "en" to "英语"),
                    onSelected = { languageKey = it },
                )
                QueueSelectionField(
                    label = "默认输出格式",
                    selectedValue = outputName,
                    options = TranscriptionOutputFormat.entries.map { it.name to it.name },
                    onSelected = { outputName = it },
                )
                Text(
                    "该设置只影响当前文件；未缓存的模型需先在设置页准备。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        modelId,
                        languageKey.takeUnless { it == "auto" },
                        TranscriptionOutputFormat.valueOf(outputName),
                    )
                },
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSelectionField(
    label: String,
    selectedValue: String,
    options: List<Pair<String, String>>,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.first == selectedValue }?.second ?: selectedValue
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, optionLabel) ->
                DropdownMenuItem(
                    text = { Text(optionLabel) },
                    onClick = {
                        expanded = false
                        onSelected(value)
                    },
                )
            }
        }
    }
}

@Composable
private fun TranscriptionQueueRow(
    task: TranscriptionTaskEntity,
    expanded: Boolean,
    selected: Boolean,
    onToggleSelection: () -> Unit,
    onChooseSource: () -> Unit,
    onOpenSettings: () -> Unit,
    onRetryTask: (String) -> Unit,
    onCancelTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
) {
    val state = taskState(task)
    val active = state in setOf(
        TranscriptionTaskState.PREPARING,
        TranscriptionTaskState.TRANSCRIBING,
        TranscriptionTaskState.EXPORTING,
    )
    val canExpand = active || state in setOf(
        TranscriptionTaskState.FAILED,
        TranscriptionTaskState.RECOVERY_REQUIRED,
        TranscriptionTaskState.NO_SPEECH,
    )
    var detailsExpanded by rememberSaveable(task.id) { mutableStateOf(active) }
    LaunchedEffect(active) { if (active) detailsExpanded = true }
    val color = taskStateColor(state)
    val selectable = TaskSelectionPolicy.canSelectForStart(state)
    val modelName = OfficialModelCatalog.find(task.modelId)?.displayName ?: task.modelId
    val progress = task.totalDurationMillis?.takeIf { it > 0L }?.let {
        (task.progressMillis.toFloat() / it.toFloat()).coerceIn(0f, 1f)
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = canExpand) { detailsExpanded = !detailsExpanded },
        shape = RoundedCornerShape(14.dp),
        color = if (active) Color(0xFFFFF3D6) else Color.Transparent,
        border = if (active) BorderStroke(1.dp, Color(0xFFFFB020)) else null,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(if (expanded) 88.dp else 80.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Box(
                    modifier = Modifier.size(
                        width = if (expanded) 92.dp else 76.dp,
                        height = if (expanded) 64.dp else 56.dp,
                    ),
                ) {
                    TaskMediaPreview(
                        displayName = task.sourceDisplayName,
                        sourceUri = task.sourceUri,
                        stagedInputPath = task.stagedInputPath,
                        cacheTaskId = task.id,
                        modifier = Modifier.fillMaxSize(),
                    )
                    IconButton(
                        onClick = onToggleSelection,
                        enabled = selectable,
                        modifier = Modifier.align(Alignment.TopStart).size(30.dp).clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.88f)),
                    ) {
                        Icon(
                            imageVector = when {
                                selectable && selected -> Icons.Filled.CheckCircle
                                selectable -> Icons.Filled.RadioButtonUnchecked
                                active -> Icons.Filled.Downloading
                                state in setOf(
                                    TranscriptionTaskState.FAILED,
                                    TranscriptionTaskState.RECOVERY_REQUIRED,
                                    TranscriptionTaskState.NO_SPEECH,
                                ) -> Icons.Filled.ErrorOutline
                                state == TranscriptionTaskState.CANCELLED -> Icons.Filled.RadioButtonUnchecked
                                else -> Icons.Outlined.AudioFile
                            },
                            contentDescription = if (selectable) {
                                if (selected) "取消选择 ${task.sourceDisplayName}" else "选择 ${task.sourceDisplayName}"
                            } else {
                                taskStateLabel(state)
                            },
                            tint = if (selectable && selected) MaterialTheme.colorScheme.primary else color,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        task.sourceDisplayName,
                        maxLines = if (expanded) 2 else 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        buildString {
                            append(modelName)
                            append(" · ")
                            append(task.language?.uppercase() ?: "自动识别")
                            task.totalDurationMillis?.let { append(" · ${formatDuration(it)}") }
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Text(
                        buildString {
                            append(taskStateLabel(state))
                            progress?.let { append(" · ${(it * 100).toInt()}%") }
                            append(" · ${formatDateTime(task.updatedAtMillis)}")
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = color,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    )
                    if (active && progress != null) {
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(3.dp))
                    }
                }
                if (canExpand) {
                    Text(
                        if (detailsExpanded) "收起" else "详情",
                        color = if (active) Color(0xFFB85C00) else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                when (state) {
                    TranscriptionTaskState.WAITING_INPUT -> QueueIconButton(Icons.Outlined.FolderOpen, "重新选择文件") { onChooseSource() }
                    TranscriptionTaskState.WAITING_MODEL -> QueueIconButton(Icons.Outlined.Settings, "前往设置准备模型") { onOpenSettings() }
                    TranscriptionTaskState.FAILED,
                    TranscriptionTaskState.RECOVERY_REQUIRED,
                    TranscriptionTaskState.NO_SPEECH,
                    TranscriptionTaskState.CANCELLED,
                    -> QueueIconButton(Icons.Outlined.Replay, "重新开始转写") { onRetryTask(task.id) }
                    TranscriptionTaskState.QUEUED,
                    TranscriptionTaskState.PREPARING,
                    TranscriptionTaskState.TRANSCRIBING,
                    TranscriptionTaskState.EXPORTING,
                    -> QueueIconButton(Icons.Filled.StopCircle, "取消转写", MaterialTheme.colorScheme.error) { onCancelTask(task.id) }
                    TranscriptionTaskState.COMPLETED -> Unit
                }
                QueueIconButton(
                    icon = Icons.Outlined.DeleteOutline,
                    description = "删除任务 ${task.sourceDisplayName}",
                    tint = MaterialTheme.colorScheme.error,
                ) { onDeleteTask(task.id) }
            }
            if (detailsExpanded && task.userMessage?.isNotBlank() == true) {
                Text(
                    task.userMessage,
                    modifier = Modifier.fillMaxWidth().padding(start = 41.dp, end = 8.dp, bottom = 3.dp),
                    color = when (state) {
                        TranscriptionTaskState.FAILED,
                        TranscriptionTaskState.RECOVERY_REQUIRED,
                        -> MaterialTheme.colorScheme.error
                        TranscriptionTaskState.NO_SPEECH -> Color(0xFF9B6049)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun QueueIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(34.dp)) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun QueueScrollIndicator(state: LazyListState, modifier: Modifier = Modifier) {
    val totalItems = state.layoutInfo.totalItemsCount
    val visibleItems = state.layoutInfo.visibleItemsInfo.size
    if (totalItems <= visibleItems || visibleItems == 0) return
    BoxWithConstraints(
        modifier = modifier.clip(RoundedCornerShape(999.dp)).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
    ) {
        val thumbFraction = (visibleItems.toFloat() / totalItems).coerceIn(0.18f, 1f)
        val thumbHeight = maxHeight * thumbFraction
        val lastStartIndex = (totalItems - visibleItems).coerceAtLeast(1)
        val scrollFraction = (state.firstVisibleItemIndex.toFloat() / lastStartIndex).coerceIn(0f, 1f)
        val thumbOffset = (maxHeight - thumbHeight) * scrollFraction
        Box(
            modifier = Modifier.fillMaxWidth().offset(y = thumbOffset).height(thumbHeight)
                .clip(RoundedCornerShape(999.dp)).background(MaterialTheme.colorScheme.primary),
        )
    }
}

@Composable
private fun EmptyWorkspace(hasHistory: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp).padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.AudioFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f))
            Spacer(Modifier.height(8.dp))
            Text(if (hasHistory) "当前没有待处理任务" else "还没有转写任务", fontWeight = FontWeight.SemiBold)
            Text(
                if (hasHistory) "已完成结果只保留在历史页" else "批量选择或拖入音视频开始转写",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActionDock(
    state: TranscriptionUiState,
    onChooseSource: () -> Unit,
    onChooseFolder: () -> Unit,
    onDropSources: (List<Uri>) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragActive by remember { mutableStateOf(false) }
    var dropBounds by remember { mutableStateOf<Rect?>(null) }
    val rootView = LocalView.current
    DisposableEffect(rootView, onDropSources, dropBounds) {
        rootView.setOnDragListener { _, event ->
            val inside = dropBounds?.let { bounds ->
                event.x >= bounds.left && event.x <= bounds.right && event.y >= bounds.top && event.y <= bounds.bottom
            } == true
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> event.clipDescription != null
                DragEvent.ACTION_DRAG_LOCATION -> {
                    dragActive = inside
                    true
                }
                DragEvent.ACTION_DROP -> {
                    dragActive = false
                    if (!inside) return@setOnDragListener false
                    (rootView.context as? Activity)?.requestDragAndDropPermissions(event)
                    val clipData = event.clipData ?: return@setOnDragListener false
                    val uris = buildList {
                        repeat(clipData.itemCount) { index -> clipData.getItemAt(index).uri?.let(::add) }
                    }
                    if (uris.isEmpty()) false else {
                        onDropSources(uris)
                        true
                    }
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    dragActive = false
                    true
                }
                else -> true
            }
        }
        onDispose { rootView.setOnDragListener(null) }
    }
    WorkbenchCard(
        modifier = modifier.onGloballyPositioned { coordinates -> dropBounds = coordinates.boundsInRoot() },
    ) {
        Text(
            text = "添加任务",
            style = MaterialTheme.typography.titleLarge,
            color = TranscriptPreviewOrange,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(enabled = !state.isBusy, onClick = onChooseSource),
            color = if (dragActive) MaterialTheme.colorScheme.primaryContainer else Color(0xFFFFF3E8),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(
                if (dragActive) 2.dp else 1.dp,
                if (dragActive) MaterialTheme.colorScheme.primary else TranscriptPreviewOrange.copy(alpha = 0.72f),
            ),
            shadowElevation = if (dragActive) 5.dp else 2.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    tint = if (dragActive) MaterialTheme.colorScheme.primary else TranscriptPreviewOrange,
                )
                Text(
                    state.selectedSourceName ?: if (dragActive) "松手加入转写队列" else "选择音频或视频",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        OutlinedButton(
            onClick = onChooseFolder,
            enabled = !state.isBusy,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .heightIn(min = 56.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.size(24.dp))
            Text(
                "选择整个文件夹",
                modifier = Modifier.padding(start = 10.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (state.modelState != ModelInstallState.READY) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "离线模型尚未准备",
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = onOpenSettings) { Text("前往设置") }
            }
        }
    }
}

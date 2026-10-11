package com.nanzhufeng.transcriber.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.nanzhufeng.transcriber.SharedInputRequest
import com.nanzhufeng.transcriber.ui.screens.HistoryScreen
import com.nanzhufeng.transcriber.ui.screens.HistoryMediaReviewPane
import com.nanzhufeng.transcriber.ui.screens.HomeScreen
import com.nanzhufeng.transcriber.ui.screens.SettingsScreen
import com.nanzhufeng.transcriber.ui.components.TopStatusIsland

@Composable
fun NanfengTranscriberApp(
    viewModel: TranscriptionViewModel,
    notificationTaskId: String? = null,
    sharedInputRequest: SharedInputRequest? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val invocationRecords by viewModel.asrInvocationRecords.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(WorkbenchDestination.HOME) }
    // Root-owned, saveable session follows the downloader's history player contract.  A fold or
    // external-display configuration change may recreate HistoryScreen, but not the session.
    var activeVideoTaskId by rememberSaveable { mutableStateOf<String?>(null) }
    var activeVideoPositionMillis by rememberSaveable { mutableStateOf(0L) }
    var activeVideoPlayWhenReady by rememberSaveable { mutableStateOf(true) }
    val context = LocalView.current.context

    val sourcePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris -> viewModel.selectSources(uris) }
    val outputFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let(viewModel::setDefaultOutputDirectory) }
    val modelImportPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::importSelectedModel) }
    val modelExportPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri -> uri?.let(viewModel::exportSelectedModel) }
    val notificationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        viewModel.startTranscription()
    }
    val startTranscription = {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.startTranscription()
        }
    }

    val hostView = LocalView.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.resumeActiveTasks()
    }
    LaunchedEffect(notificationTaskId) {
        notificationTaskId?.let {
            viewModel.focusTask(it)
            destination = WorkbenchDestination.HOME
        }
    }
    LaunchedEffect(sharedInputRequest?.token) {
        sharedInputRequest?.let {
            viewModel.selectSharedSources(it.uris)
            destination = WorkbenchDestination.HOME
        }
    }
    DisposableEffect(settings.keepScreenOn, hostView) {
        val previous = hostView.keepScreenOn
        hostView.keepScreenOn = settings.keepScreenOn
        onDispose { hostView.keepScreenOn = previous }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val expanded = maxWidth >= 720.dp
        WorkbenchShell(
            destination = destination,
            onDestinationChanged = { destination = it },
            expanded = expanded,
        ) {
            when (destination) {
                WorkbenchDestination.HOME -> HomeScreen(
                    state = state,
                    tasks = tasks,
                    expanded = expanded,
                    onChooseSource = { sourcePicker.launch(arrayOf("audio/*", "video/*")) },
                    onDropSources = viewModel::selectSources,
                    onStart = startTranscription,
                    onTogglePendingSource = viewModel::togglePendingSourceSelection,
                    onRemovePendingSource = viewModel::removePendingSource,
                    onUpdatePendingOptions = viewModel::updatePendingSourceOptions,
                    onToggleTaskSelection = viewModel::toggleTaskSelection,
                    onSelectAllStartable = viewModel::setAllStartableSelected,
                    onRetryTask = viewModel::retryTask,
                    onCancelTask = viewModel::cancelTask,
                    onDeleteTask = viewModel::deleteTask,
                    onOpenSettings = { destination = WorkbenchDestination.SETTINGS },
                )

                WorkbenchDestination.HISTORY -> HistoryScreen(
                    state = state,
                    lastExportFormat = settings.lastExportFormat,
                    tasks = tasks,
                    expanded = expanded,
                    onOpenResult = viewModel::openTaskResult,
                    onCopyResult = viewModel::copyTaskResult,
                    onCopyTranscriptDraft = viewModel::copyTranscriptDraft,
                    onDeleteTask = viewModel::deleteTask,
                    onDeleteTasks = viewModel::deleteHistoryTasks,
                    onCleanInvalidHistory = viewModel::cleanInvalidHistory,
                    onTranscriptDraftChanged = viewModel::updateTranscriptDraft,
                    onExport = viewModel::exportResultToDefaultDirectory,
                    onExportFormatChanged = viewModel::setLastExportFormat,
                    onOpenMediaReview = { taskId ->
                        viewModel.openTaskResult(taskId)
                        activeVideoTaskId = taskId
                        activeVideoPositionMillis = 0L
                        activeVideoPlayWhenReady = true
                    },
                )

                WorkbenchDestination.SETTINGS -> SettingsScreen(
                    state = state,
                    settings = settings,
                    expanded = expanded,
                    onDownloadModel = viewModel::downloadBaseModel,
                    onImportModel = { modelImportPicker.launch(arrayOf("application/octet-stream", "*/*")) },
                    onExportModel = { modelExportPicker.launch(viewModel.suggestedModelFileName()) },
                    onVerifyModel = viewModel::verifySelectedModel,
                    onDeleteModel = viewModel::deleteSelectedModel,
                    onModelChanged = viewModel::setModelId,
                    onLanguageChanged = viewModel::setLanguageCode,
                    onThreadCountChanged = viewModel::setThreadCount,
                    onChooseOutputDirectory = { outputFolderPicker.launch(null) },
                    onClearOutputDirectory = viewModel::clearDefaultOutputDirectory,
                    onConflictPolicyChanged = viewModel::setOutputConflictPolicy,
                    onSavePostProcessApiKey = viewModel::savePostProcessApiKey,
                    onRevealPostProcessApiKey = viewModel::revealPostProcessApiKey,
                    onSkinChanged = viewModel::setSkinId,
                    invocationRecords = invocationRecords,
                )
            }
        }
        activeVideoTaskId?.let { taskId ->
            tasks.firstOrNull { it.id == taskId }?.let { task ->
                HistoryMediaReviewPane(
                    task = task,
                    state = state,
                    expanded = expanded,
                    lastExportFormat = settings.lastExportFormat,
                    initialVideoPositionMillis = activeVideoPositionMillis,
                    initialVideoPlayWhenReady = activeVideoPlayWhenReady,
                    onVideoPlaybackSnapshot = { positionMillis, playWhenReady ->
                        if (activeVideoTaskId == task.id) {
                            activeVideoPositionMillis = positionMillis
                            activeVideoPlayWhenReady = playWhenReady
                        }
                    },
                    onDismiss = { activeVideoTaskId = null },
                    onCopy = viewModel::copyTranscriptDraft,
                    onTranscriptDraftChanged = viewModel::updateTranscriptDraft,
                    onExport = viewModel::exportResultToDefaultDirectory,
                    onExportFormatChanged = viewModel::setLastExportFormat,
                )
            }
        }
        state.exportFeedback?.let { feedback ->
            TopStatusIsland(
                feedback = feedback,
                onTimeout = viewModel::dismissExportFeedback,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun WorkbenchShell(
    destination: WorkbenchDestination,
    onDestinationChanged: (WorkbenchDestination) -> Unit,
    expanded: Boolean,
    content: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Box(Modifier.weight(1f).fillMaxWidth()) { content() }
        val horizontalGap = if (expanded) 24.dp else 32.dp
        val bottomGap = if (expanded) 6.dp else 4.dp
        Box(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(start = horizontalGap, end = horizontalGap, top = 4.dp, bottom = bottomGap)
                .testTag("floating-bottom-navigation"),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(32.dp),
                        ambientColor = Color(0x12000000), spotColor = Color(0x0A000000))
                    .clip(RoundedCornerShape(32.dp)).background(Color.White).padding(3.dp),
            ) {
                WorkbenchDestination.entries.forEach { item ->
                    DestinationButton(
                        item = item,
                        selected = destination == item,
                        onClick = { onDestinationChanged(item) },
                        expanded = expanded,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DestinationButton(
    item: WorkbenchDestination,
    selected: Boolean,
    onClick: () -> Unit,
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = tween(durationMillis = 120, easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)),
        label = "primary-navigation-selection",
    )
    Column(
        modifier = modifier
            .height(if (expanded) 56.dp else 52.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(containerColor)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .testTag("nav-${item.name.lowercase()}")
            .semantics { stateDescription = if (selected) "已选中" else "未选中" },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = item.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Medium,
        )
    }
}

private enum class WorkbenchDestination(
    val label: String,
    val icon: ImageVector,
) {
    HOME("首页", Icons.Outlined.Home),
    HISTORY("历史", Icons.Outlined.History),
    SETTINGS("设置", Icons.Outlined.Settings),
}

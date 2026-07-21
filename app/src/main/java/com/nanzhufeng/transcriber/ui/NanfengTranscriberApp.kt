package com.nanzhufeng.transcriber.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.nanzhufeng.transcriber.domain.export.TranscriptExportFormat
import com.nanzhufeng.transcriber.SharedInputRequest
import com.nanzhufeng.transcriber.ui.screens.HistoryScreen
import com.nanzhufeng.transcriber.ui.screens.HomeScreen
import com.nanzhufeng.transcriber.ui.screens.SettingsScreen

@Composable
fun NanfengTranscriberApp(
    viewModel: TranscriptionViewModel,
    notificationTaskId: String? = null,
    sharedInputRequest: SharedInputRequest? = null,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var destination by rememberSaveable { mutableStateOf(WorkbenchDestination.HOME) }
    var pendingExport by rememberSaveable { mutableStateOf(TranscriptExportFormat.TXT) }
    val context = LocalView.current.context

    val sourcePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris -> viewModel.selectSources(uris) }
    val sourceFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let(viewModel::selectSourceFolder) }
    val outputFolderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let(viewModel::setDefaultOutputDirectory) }
    val exportPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*"),
    ) { uri -> uri?.let { viewModel.exportResult(it, pendingExport) } }
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
                    onChooseFolder = { sourceFolderPicker.launch(null) },
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
                    tasks = tasks,
                    expanded = expanded,
                    onOpenResult = viewModel::openTaskResult,
                    onCopyResult = viewModel::copyTaskResult,
                    onCopyTranscriptDraft = viewModel::copyTranscriptDraft,
                    onDeleteTask = viewModel::deleteTask,
                    onTranscriptDraftChanged = viewModel::updateTranscriptDraft,
                    onExport = { format ->
                        pendingExport = format
                        exportPicker.launch(viewModel.suggestedFileName(format))
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
                    onKeepScreenOnChanged = viewModel::setKeepScreenOn,
                    onChooseOutputDirectory = { outputFolderPicker.launch(null) },
                    onClearOutputDirectory = viewModel::clearDefaultOutputDirectory,
                    onConflictPolicyChanged = viewModel::setOutputConflictPolicy,
                    onPostProcessEnabledChanged = viewModel::setPostProcessEnabled,
                    onSavePostProcessConnection = viewModel::savePostProcessConnection,
                    onSavePostProcessApiKey = viewModel::savePostProcessApiKey,
                    onClearPostProcessApiKey = viewModel::clearPostProcessApiKey,
                    onSkinChanged = viewModel::setSkinId,
                )
            }
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
    if (expanded) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                WorkbenchDestination.entries.forEach { item ->
                    DestinationButton(
                        item = item,
                        selected = destination == item,
                        onClick = { onDestinationChanged(item) },
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .width(64.dp),
                    )
                }
            }
            Box(Modifier.fillMaxSize()) { content() }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Box(Modifier.weight(1f)) { content() }
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
            ) {
                WorkbenchDestination.entries.forEach { item ->
                    DestinationButton(
                        item = item,
                        selected = destination == item,
                        onClick = { onDestinationChanged(item) },
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .weight(1f),
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
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = tween(durationMillis = 120),
        label = "primary-navigation-selection",
    )
    Column(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
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
    HOME("主页", Icons.Outlined.Home),
    HISTORY("历史", Icons.Outlined.History),
    SETTINGS("设置", Icons.Outlined.Settings),
}

package com.nanzhufeng.transcriber.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanzhufeng.transcriber.NanfengTranscriberApplication
import com.nanzhufeng.transcriber.data.modelstore.ModelAssetResult
import com.nanzhufeng.transcriber.data.modelstore.ModelTransferStage
import com.nanzhufeng.transcriber.data.modelstore.OfficialModelCatalog
import com.nanzhufeng.transcriber.data.modelstore.CatalogModel
import com.nanzhufeng.transcriber.data.output.AndroidTranscriptOutputStore
import com.nanzhufeng.transcriber.data.modelstore.AsrProviderId
import com.nanzhufeng.transcriber.data.result.StoredTranscript
import com.nanzhufeng.transcriber.data.result.TranscriptDocumentStore
import com.nanzhufeng.transcriber.data.settings.TranscriptionSettings
import com.nanzhufeng.transcriber.data.task.TranscriptionTaskEntity
import com.nanzhufeng.transcriber.data.task.NewTranscriptionTask
import com.nanzhufeng.transcriber.data.task.SourceAccessMode
import com.nanzhufeng.transcriber.data.task.TranscriptionOutputFormat
import com.nanzhufeng.transcriber.data.task.OutputConflictPolicy
import com.nanzhufeng.transcriber.domain.export.TranscriptDocument
import com.nanzhufeng.transcriber.domain.export.TranscriptDocumentSegment
import com.nanzhufeng.transcriber.domain.export.TranscriptExportFormat
import com.nanzhufeng.transcriber.domain.export.TranscriptExportService
import com.nanzhufeng.transcriber.domain.invocation.AsrInvocationRecord
import com.nanzhufeng.transcriber.domain.model.ModelInstallState
import com.nanzhufeng.transcriber.domain.task.TranscriptionTaskState
import com.nanzhufeng.transcriber.domain.task.TaskSelectionPolicy
import com.nanzhufeng.transcriber.engine.NativeWhisperBridge
import com.nanzhufeng.transcriber.service.TranscriptionForegroundService
import com.nanzhufeng.transcriber.ui.components.formatTranscriptPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.Comparator
import java.util.UUID

class TranscriptionViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NanfengTranscriberApplication
    private val container = app.appContainer
    private val resolver = application.contentResolver
    private var selectedModel = requireNotNull(OfficialModelCatalog.find("sensevoice-small-int8"))
    private val exportService = TranscriptExportService()
    private val outputStore = AndroidTranscriptOutputStore(resolver, exportService)
    private val documentStore = TranscriptDocumentStore()

    private val _uiState = MutableStateFlow(
        TranscriptionUiState(
            nativeStatus = runCatching { NativeWhisperBridge.engineStatus() }
                .getOrElse { "原生引擎加载失败，请重新安装正式 APK" },
            modelDisplayName = selectedModel.displayName,
            modelExpectedBytes = selectedModel.manifest.expectedBytes,
        ),
    )
    val uiState: StateFlow<TranscriptionUiState> = _uiState.asStateFlow()
    val tasks: StateFlow<List<TranscriptionTaskEntity>> = container.tasks.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val settings: StateFlow<TranscriptionSettings> = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TranscriptionSettings())
    val asrInvocationRecords: StateFlow<List<AsrInvocationRecord>> = container.asrInvocations.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var readyModelPath: Path? = null
    private var selectedSourceUri: Uri? = null
    private var pendingSources: List<PendingSource> = emptyList()
    private var latestDocument: TranscriptDocument? = null
    private var currentTaskId: String? = null
    private val executionRequests = mutableSetOf<String>()

    init {
        viewModelScope.launch { refreshModelState() }
        viewModelScope.launch {
            container.settings.settings.collect { currentSettings ->
                val model = OfficialModelCatalog.find(currentSettings.modelId)
                if (model == null) {
                    // Whisper was removed from the public product catalogue. Persist the supported
                    // default so every selection surface immediately agrees after upgrade.
                    container.settings.setModelId("sensevoice-small-int8")
                    return@collect
                }
                if (model.manifest.modelId != selectedModel.manifest.modelId) {
                    selectedModel = model
                    refreshModelState(model)
                }
            }
        }
        viewModelScope.launch {
            tasks.collect { taskList ->
                synchronizeTaskState(taskList)
                refreshHistoryPreviews(taskList)
            }
        }
    }

    fun downloadBaseModel() {
        if (_uiState.value.isBusy || _uiState.value.modelState == ModelInstallState.READY) return
        val model = selectedModel

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    stage = WorkflowStage.DOWNLOADING_MODEL,
                    modelState = ModelInstallState.DOWNLOADING,
                    modelProgress = 0f,
                    statusMessage = "正在下载 ${model.displayName}；中断后会从已完成位置继续",
                )
            }
            val result = container.modelAssets.download(model) { progress ->
                val fraction = if (progress.totalBytes > 0L) {
                    progress.completedBytes.toFloat() / progress.totalBytes.toFloat()
                } else {
                    0f
                }
                _uiState.update {
                    it.copy(
                        stage = if (progress.stage == ModelTransferStage.VERIFYING) {
                            WorkflowStage.VERIFYING_MODEL
                        } else {
                            WorkflowStage.DOWNLOADING_MODEL
                        },
                        modelState = if (progress.stage == ModelTransferStage.VERIFYING) {
                            ModelInstallState.VERIFYING
                        } else {
                            ModelInstallState.DOWNLOADING
                        },
                        modelProgress = fraction.coerceIn(0f, 1f),
                        transferredModelBytes = progress.completedBytes,
                        statusMessage = if (progress.stage == ModelTransferStage.VERIFYING) {
                            "下载完成，正在校验模型 SHA-256"
                        } else {
                            "正在下载 ${model.displayName}；退出后可断点续传"
                        },
                    )
                }
            }

            when (result) {
                is ModelAssetResult.Installed -> acceptReadyModel(model, result.modelPath, result.message)
                is ModelAssetResult.AlreadyReady -> acceptReadyModel(model, result.modelPath, result.message)
                is ModelAssetResult.Failure -> {
                    val inspection = withContext(Dispatchers.IO) {
                        container.modelStore.inspect(model.manifest)
                    }
                    _uiState.update {
                        it.copy(
                            stage = WorkflowStage.ERROR,
                            modelState = inspection.state,
                            modelProgress = null,
                            statusMessage = result.message,
                        )
                    }
                }
                is ModelAssetResult.Exported -> Unit
            }
        }
    }

    fun importSelectedModel(uri: Uri) {
        if (_uiState.value.isBusy) return
        val model = selectedModel
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    stage = WorkflowStage.IMPORTING_MODEL,
                    modelProgress = 0f,
                    statusMessage = "正在导入 ${model.displayName}，导入后会校验文件完整性",
                )
            }
            val result = runCatching {
                container.modelDocuments.importFromDocument(model.manifest, uri) { progress ->
                    updateModelTransferProgress(model, progress.stage, progress.completedBytes, progress.totalBytes)
                }
            }.getOrElse { error ->
                ModelAssetResult.Failure(
                    category = com.nanzhufeng.transcriber.data.modelstore.ModelAssetFailureCategory.STORAGE,
                    message = "导入模型失败，请重新选择本机模型文件",
                    canRetry = true,
                    technicalDetail = error.message,
                )
            }
            when (result) {
                is ModelAssetResult.Installed -> acceptReadyModel(model, result.modelPath, "模型已导入并通过校验")
                is ModelAssetResult.AlreadyReady -> acceptReadyModel(model, result.modelPath, result.message)
                is ModelAssetResult.Failure -> finishModelFailure(model, result.message)
                is ModelAssetResult.Exported -> Unit
            }
        }
    }

    fun exportSelectedModel(uri: Uri) {
        if (_uiState.value.isBusy || _uiState.value.modelState != ModelInstallState.READY) return
        val model = selectedModel
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    stage = WorkflowStage.EXPORTING_MODEL,
                    modelProgress = 0f,
                    statusMessage = "正在导出 ${model.displayName} 备份",
                )
            }
            val result = runCatching {
                container.modelDocuments.exportToDocument(model.manifest, uri) { progress ->
                    updateModelTransferProgress(model, progress.stage, progress.completedBytes, progress.totalBytes)
                }
            }.getOrElse { error ->
                ModelAssetResult.Failure(
                    category = com.nanzhufeng.transcriber.data.modelstore.ModelAssetFailureCategory.STORAGE,
                    message = "模型导出失败，请重新选择保存位置",
                    canRetry = true,
                    technicalDetail = error.message,
                )
            }
            when (result) {
                is ModelAssetResult.Exported -> {
                    refreshModelState(model)
                    _uiState.update { it.copy(statusMessage = result.message) }
                    Toast.makeText(app, "模型备份已导出", Toast.LENGTH_SHORT).show()
                }
                is ModelAssetResult.Failure -> finishModelFailure(model, result.message)
                is ModelAssetResult.Installed,
                is ModelAssetResult.AlreadyReady,
                -> Unit
            }
        }
    }

    fun verifySelectedModel() {
        if (_uiState.value.isBusy || _uiState.value.modelState != ModelInstallState.READY) return
        val model = selectedModel
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    stage = WorkflowStage.VERIFYING_MODEL,
                    modelState = ModelInstallState.VERIFYING,
                    modelProgress = null,
                    statusMessage = "正在完整校验 ${model.displayName}，期间不会修改模型",
                )
            }
            val inspection = withContext(Dispatchers.IO) {
                container.modelStore.inspect(model.manifest, fullHash = true)
            }
            if (model.manifest.modelId != selectedModel.manifest.modelId) return@launch
            readyModelPath = inspection.modelPath
            val bytes = withContext(Dispatchers.IO) { container.modelStore.cacheBytes(model.manifest) }
            _uiState.update {
                it.copy(
                    stage = if (inspection.state == ModelInstallState.READY) WorkflowStage.IDLE else WorkflowStage.ERROR,
                    modelState = inspection.state,
                    modelProgress = null,
                    modelCacheBytes = bytes,
                    statusMessage = if (inspection.state == ModelInstallState.READY) {
                        "${model.displayName} 完整校验通过，可继续长期复用"
                    } else {
                        "模型校验未通过：${inspection.reason}"
                    },
                )
            }
        }
    }

    fun deleteSelectedModel() {
        if (_uiState.value.isBusy) return
        val model = selectedModel
        viewModelScope.launch {
            val removedBytes = withContext(Dispatchers.IO) { container.modelStore.delete(model.manifest) }
            if (model.manifest.modelId != selectedModel.manifest.modelId) return@launch
            readyModelPath = null
            _uiState.update {
                it.copy(
                    stage = WorkflowStage.IDLE,
                    modelState = ModelInstallState.NOT_INSTALLED,
                    modelProgress = null,
                    transferredModelBytes = 0L,
                    modelCacheBytes = 0L,
                    statusMessage = if (removedBytes > 0L) {
                        "已删除 ${model.displayName} 缓存，其他模型和转写结果未受影响"
                    } else {
                        "${model.displayName} 没有可删除的缓存"
                    },
                )
            }
            Toast.makeText(app, "所选模型缓存已删除", Toast.LENGTH_SHORT).show()
        }
    }

    fun suggestedModelFileName(): String = if (selectedModel.manifest.files.size == 1) {
        "ggml-${selectedModel.manifest.modelId}.bin"
    } else {
        "nanfeng-${selectedModel.manifest.modelId}.nfmodel"
    }

    fun selectSource(uri: Uri) = selectSources(listOf(uri))

    fun selectSources(uris: List<Uri>) = enqueueSources(uris, inheritedPersistedAccess = false)

    fun selectSharedSources(uris: List<Uri>) = enqueueSources(uris, inheritedPersistedAccess = false)

    fun selectSourceFolder(treeUri: Uri) {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            val persisted = runCatching {
                resolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                true
            }.getOrDefault(false)
            val mediaUris = runCatching {
                withContext(Dispatchers.IO) { findMediaDocuments(treeUri) }
            }.getOrElse {
                _uiState.update {
                    it.copy(statusMessage = "无法读取所选文件夹，请确认目录未被移动并重新选择")
                }
                return@launch
            }
            if (mediaUris.isEmpty()) {
                _uiState.update {
                    it.copy(statusMessage = "所选文件夹中没有可读取的音频或视频")
                }
                return@launch
            }
            enqueueSources(mediaUris, inheritedPersistedAccess = persisted)
        }
    }

    private fun enqueueSources(uris: List<Uri>, inheritedPersistedAccess: Boolean) {
        if (_uiState.value.isBusy) return
        val distinctUris = uris.distinctBy(Uri::toString)
        if (distinctUris.isEmpty()) return

        viewModelScope.launch {
            val defaults = settings.value
        val defaultModel = OfficialModelCatalog.find(defaults.modelId) ?: selectedModel
            val selected = distinctUris.mapNotNull { uri ->
                runCatching {
                    val persisted = inheritedPersistedAccess || runCatching {
                        resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        true
                    }.getOrDefault(false)
                    val metadata = withContext(Dispatchers.IO) { readSourceMetadata(uri) }
                    val stagedPath = if (persisted) null else withContext(Dispatchers.IO) {
                        stageSource(uri, metadata.first)
                    }
                    if (!persisted && stagedPath == null) error("无法复制分享文件")
                    PendingSource(
                        uri = uri,
                        displayName = metadata.first,
                        bytes = metadata.second,
                        persisted = persisted,
                        stagedPath = stagedPath,
                        selected = true,
                        modelId = defaultModel.manifest.modelId,
                        modelVersion = defaultModel.manifest.version,
                        language = defaults.languageCode,
                        threadCount = defaults.threadCount,
                        outputFormat = TranscriptionOutputFormat.TXT,
                    )
                }.getOrNull()
            }
            if (selected.isEmpty()) {
                _uiState.update {
                    it.copy(
                        stage = WorkflowStage.ERROR,
                        statusMessage = "没有可读取的音视频，请从原 App 重新分享或使用文件选择器",
                    )
                }
                return@launch
            }
            pendingSources = (pendingSources + selected).distinctBy(PendingSource::key)
            val allPending = pendingSources
            selectedSourceUri = allPending.first().uri
            latestDocument = null
            _uiState.update {
                it.copy(
                    selectedSourceName = allPending.displaySummary(),
                    selectedSourceBytes = allPending.totalBytes(),
                    selectedSourceCount = allPending.size,
                    selectedSourceNames = allPending.map(PendingSource::displayName),
                    pendingSourceItems = allPending.map(PendingSource::toUiItem),
                    sourceAccessPersisted = allPending.all(PendingSource::persisted),
                    stage = WorkflowStage.IDLE,
                    progress = null,
                    transcriptText = null,
                    openedResultTaskId = null,
                    performanceSummary = null,
                    statusMessage = if (allPending.all(PendingSource::persisted)) {
                        "已加入 ${selected.size} 个文件，待转写共 ${allPending.size} 个" +
                            if (selected.size < distinctUris.size) "；已跳过 ${distinctUris.size - selected.size} 个无权读取项" else ""
                    } else {
                        "已加入 ${selected.size} 个文件；分享文件已复制到 App 私有目录" +
                            if (selected.size < distinctUris.size) "；已跳过 ${distinctUris.size - selected.size} 个无权读取项" else ""
                    },
                )
            }
        }
    }

    private fun findMediaDocuments(treeUri: Uri): List<Uri> {
        val rootId = DocumentsContract.getTreeDocumentId(treeUri)
        val pendingDirectories = ArrayDeque<String>().apply { add(rootId) }
        val results = mutableListOf<Uri>()
        while (pendingDirectories.isNotEmpty() && results.size < MAX_FOLDER_MEDIA_FILES) {
            val parentId = pendingDirectories.removeFirst()
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
            resolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                while (cursor.moveToNext() && results.size < MAX_FOLDER_MEDIA_FILES) {
                    val documentId = cursor.getString(idColumn)
                    val name = cursor.getString(nameColumn).orEmpty()
                    val mime = cursor.getString(mimeColumn).orEmpty()
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        pendingDirectories.add(documentId)
                    } else if (isSupportedMedia(name, mime)) {
                        results += DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                    }
                }
            }
        }
        return results
    }

    private fun isSupportedMedia(name: String, mime: String): Boolean {
        if (mime.startsWith("audio/") || mime.startsWith("video/")) return true
        return name.substringAfterLast('.', missingDelimiterValue = "").lowercase() in SUPPORTED_MEDIA_EXTENSIONS
    }

    fun startTranscription() {
        startSelectedTasks()
    }

    fun togglePendingSourceSelection(sourceKey: String) {
        pendingSources = pendingSources.map { source ->
            if (source.key == sourceKey) source.copy(selected = !source.selected) else source
        }
        updatePendingSourceState("已更新待转写文件选择")
    }

    fun removePendingSource(sourceKey: String) {
        val removed = pendingSources.firstOrNull { it.key == sourceKey } ?: return
        pendingSources = pendingSources.filterNot { it.key == sourceKey }
        removed.stagedPath?.let { stagedPath ->
            viewModelScope.launch(Dispatchers.IO) { runCatching { Files.deleteIfExists(stagedPath) } }
        }
        updatePendingSourceState("已从待转写列表移除文件")
    }

    fun updatePendingSourceOptions(
        sourceKey: String,
        modelId: String,
    ) {
        val model = OfficialModelCatalog.find(modelId) ?: return
        pendingSources = pendingSources.map { source ->
            if (source.key == sourceKey) {
                source.copy(
                    modelId = model.manifest.modelId,
                    modelVersion = model.manifest.version,
                )
            } else {
                source
            }
        }
        updatePendingSourceState("已更新该文件的转写模型")
    }

    fun toggleTaskSelection(taskId: String) {
        val task = tasks.value.firstOrNull { it.id == taskId } ?: return
        val taskState = taskStateOrNull(task) ?: return
        if (!TaskSelectionPolicy.canSelectForStart(taskState)) return
        _uiState.update { state ->
            state.copy(
                selectedTaskIds = if (taskId in state.selectedTaskIds) {
                    state.selectedTaskIds - taskId
                } else {
                    state.selectedTaskIds + taskId
                },
            )
        }
    }

    fun setAllStartableSelected(selected: Boolean) {
        pendingSources = pendingSources.map { it.copy(selected = selected) }
        val selectableTaskIds = tasks.value
            .filter { taskStateOrNull(it)?.let(TaskSelectionPolicy::canSelectForStart) == true }
            .mapTo(mutableSetOf(), TranscriptionTaskEntity::id)
        _uiState.update { state ->
            val selectedSources = pendingSources.filter(PendingSource::selected)
            state.copy(
                pendingSourceItems = pendingSources.map(PendingSource::toUiItem),
                selectedTaskIds = if (selected) selectableTaskIds else emptySet(),
                selectedSourceName = selectedSources.displaySummary(),
                selectedSourceBytes = selectedSources.totalBytes(),
                selectedSourceCount = selectedSources.size,
                selectedSourceNames = selectedSources.map(PendingSource::displayName),
            )
        }
    }

    fun startSelectedTasks() {
        val sources = pendingSources.filter(PendingSource::selected)
        val existingIds = _uiState.value.selectedTaskIds
        if (sources.isEmpty() && existingIds.isEmpty()) return
        viewModelScope.launch {
            val requeuedTasks = tasks.value.filter { it.id in existingIds }
            val usesQwenApi = (sources.map(PendingSource::modelId) + requeuedTasks.map(TranscriptionTaskEntity::modelId))
                .mapNotNull(OfficialModelCatalog::find)
                .any { it.provider == AsrProviderId.QWEN3_ASR_API }
            if (usesQwenApi && !withContext(Dispatchers.IO) { container.textApiCredentials.hasApiKey() }) {
                _uiState.update {
                    it.copy(
                        stage = WorkflowStage.IDLE,
                        progress = null,
                        statusMessage = "高精度 Qwen 未配置 API Key，请在设置中保存后再开始转写",
                    )
                }
                return@launch
            }
            val taskSettings = settings.value
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val queueBase = System.currentTimeMillis()
                    val created = sources.mapIndexed { index, source ->
                        container.tasks.create(
                            NewTranscriptionTask(
                                sourceUri = source.uri.toString(),
                                sourceDisplayName = source.displayName,
                                sourceAccessMode = when {
                                    source.persisted -> SourceAccessMode.PERSISTED_DOCUMENT
                                    source.stagedPath != null -> SourceAccessMode.PRIVATE_STAGING
                                    else -> SourceAccessMode.RESELECT_REQUIRED
                                },
                                stagedInputPath = source.stagedPath?.toString(),
                                modelId = source.modelId,
                                modelVersion = source.modelVersion,
                                language = source.language,
                                threadCount = source.threadCount,
                                outputFormat = source.outputFormat,
                                // 任务完成不保存输出目录，也不会自动写入文件；导出仅由结果页的明确操作触发。
                                exportDirectoryUri = null,
                                outputConflictPolicy = taskSettings.outputConflictPolicy,
                                postProcessEnabled = false,
                                postProcessBaseUrl = taskSettings.postProcessBaseUrl,
                                postProcessModel = taskSettings.postProcessModel,
                                queuePosition = queueBase + index,
                            ),
                        )
                    }
                    val requeued = existingIds.mapNotNull { taskId ->
                        when (val reset = container.tasks.resetForExecution(taskId)) {
                            is com.nanzhufeng.transcriber.data.task.TaskMutationResult.Updated -> reset.task
                            else -> null
                        }
                    }
                    created + requeued
                }
            }
            val queuedTasks = result.getOrNull()
            if (queuedTasks.isNullOrEmpty()) {
                _uiState.update {
                    it.copy(
                        stage = WorkflowStage.ERROR,
                        progress = null,
                        statusMessage = "无法创建转写任务，请重启 App 后重试",
                    )
                }
                return@launch
            }
            val firstTask = queuedTasks.minBy(TranscriptionTaskEntity::queuePosition)
            currentTaskId = firstTask.id
            latestDocument = null
            pendingSources = pendingSources.filterNot(PendingSource::selected)
            selectedSourceUri = pendingSources.firstOrNull()?.uri
            _uiState.update {
                val remaining = pendingSources.filter(PendingSource::selected)
                it.copy(
                    stage = WorkflowStage.DECODING,
                    progress = null,
                    transcriptText = null,
                    openedResultTaskId = null,
                    detectedLanguage = null,
                    performanceSummary = null,
                    activeTaskId = firstTask.id,
                    activeSourceName = firstTask.sourceDisplayName,
                    selectedSourceName = remaining.displaySummary(),
                    selectedSourceBytes = remaining.totalBytes(),
                    selectedSourceCount = remaining.size,
                    selectedSourceNames = remaining.map(PendingSource::displayName),
                    pendingSourceItems = pendingSources.map(PendingSource::toUiItem),
                    selectedTaskIds = emptySet(),
                    statusMessage = "已提交 ${queuedTasks.size} 个选中任务，将按顺序在后台转写",
                )
            }
            requestTaskExecutionOnce(firstTask.id)
        }
    }

    fun cancelCurrentTask() {
        currentTaskId?.let { TranscriptionForegroundService.cancel(app, it) }
    }

    fun cancelTask(taskId: String) {
        TranscriptionForegroundService.cancel(app, taskId)
    }

    fun retryTask(taskId: String) {
        viewModelScope.launch {
            val reset = withContext(Dispatchers.IO) { container.tasks.resetForExecution(taskId) }
            if (reset !is com.nanzhufeng.transcriber.data.task.TaskMutationResult.Updated) {
                _uiState.update { it.copy(statusMessage = "该任务当前无法重新开始") }
                return@launch
            }
            currentTaskId = taskId
            latestDocument = null
            _uiState.update { it.copy(selectedTaskIds = it.selectedTaskIds - taskId) }
            requestTaskExecutionOnce(taskId)
        }
    }

    fun openTaskResult(taskId: String) {
        viewModelScope.launch { loadTaskResult(taskId) }
    }

    fun copyTaskResult(taskId: String) {
        viewModelScope.launch {
            val task = withContext(Dispatchers.IO) { container.tasks.findById(taskId) }
            val copiedText = task?.takeIf {
                taskStateOrNull(it) == TranscriptionTaskState.COMPLETED
            }?.let { completedTask ->
                runCatching {
                    withContext(Dispatchers.IO) {
                        exportService.renderTxt(loadStoredTranscript(completedTask).document).trim()
                    }
                }.getOrNull()
            }
            if (copiedText.isNullOrBlank()) {
                _uiState.update { it.copy(statusMessage = "完整文字无法读取，请打开记录查看详细状态") }
                Toast.makeText(app, "完整文字无法读取", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val clipboard = app.getSystemService(ClipboardManager::class.java)
            clipboard.setPrimaryClip(
                ClipData.newPlainText(task.sourceDisplayName, copiedText),
            )
            _uiState.update { it.copy(statusMessage = "已复制 ${task.sourceDisplayName} 的全部文字") }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                Toast.makeText(app, "已复制全部文字", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun copyTranscriptDraft() {
        val copiedText = _uiState.value.transcriptText.orEmpty()
        if (copiedText.isBlank()) {
            Toast.makeText(app, "当前没有可复制的文字", Toast.LENGTH_SHORT).show()
            return
        }
        val clipboard = app.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(
            ClipData.newPlainText(latestDocument?.title ?: "南枫转写结果", copiedText),
        )
        _uiState.update { it.copy(statusMessage = "已复制当前编辑框的全部文字") }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            Toast.makeText(app, "已复制全部文字", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            val task = withContext(Dispatchers.IO) { container.tasks.findById(taskId) }
                ?: return@launch
            val state = taskStateOrNull(task)
            if (state in setOf(
                    TranscriptionTaskState.PREPARING,
                    TranscriptionTaskState.TRANSCRIBING,
                    TranscriptionTaskState.EXPORTING,
                )
            ) {
                TranscriptionForegroundService.cancel(app, taskId)
                for (attempt in 0 until 50) {
                    delay(100)
                    val latest = withContext(Dispatchers.IO) { container.tasks.findById(taskId) }
                    if (latest == null || taskStateOrNull(latest) !in setOf(
                            TranscriptionTaskState.PREPARING,
                            TranscriptionTaskState.TRANSCRIBING,
                            TranscriptionTaskState.EXPORTING,
                        )
                    ) {
                        break
                    }
                }
            } else if (state == TranscriptionTaskState.QUEUED) {
                withContext(Dispatchers.IO) {
                    container.tasks.transition(
                        id = taskId,
                        target = TranscriptionTaskState.CANCELLED,
                        errorCode = "USER_REMOVED",
                        userMessage = "任务已从队列移除",
                    )
                }
            }

            val latest = withContext(Dispatchers.IO) { container.tasks.findById(taskId) }
            if (latest != null && taskStateOrNull(latest) in setOf(
                    TranscriptionTaskState.PREPARING,
                    TranscriptionTaskState.TRANSCRIBING,
                    TranscriptionTaskState.EXPORTING,
                )
            ) {
                _uiState.update { it.copy(statusMessage = "任务仍在安全停止，请稍后再删除") }
                Toast.makeText(app, "任务仍在安全停止，请稍后再删除", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val deleted = runCatching {
                withContext(Dispatchers.IO) {
                    cleanupTaskFiles(task)
                    container.tasks.delete(taskId)
                }
            }.getOrElse {
                _uiState.update { current ->
                    current.copy(statusMessage = "任务文件未能安全删除，请重启 App 后重试")
                }
                Toast.makeText(app, "任务文件未能安全删除", Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (!deleted) return@launch
            executionRequests.remove(taskId)
            if (currentTaskId == taskId) {
                currentTaskId = null
                latestDocument = null
            }
            _uiState.update {
                it.copy(
                    historyPreviews = it.historyPreviews - taskId,
                    openedResultTaskId = it.openedResultTaskId.takeUnless { opened -> opened == taskId },
                    transcriptText = if (it.openedResultTaskId == taskId) null else it.transcriptText,
                    activeTaskId = it.activeTaskId.takeUnless { active -> active == taskId },
                    statusMessage = "任务记录已删除，原音视频、已导出文件和模型不受影响",
                )
            }
            Toast.makeText(app, "任务记录已删除", Toast.LENGTH_SHORT).show()
        }
    }

    /** History-only bulk action. It never touches original media, model cache or exported files. */
    fun deleteHistoryTasks(taskIds: Set<String>) {
        if (taskIds.isEmpty()) return
        viewModelScope.launch {
            val deletedIds = withContext(Dispatchers.IO) {
                buildSet {
                    taskIds.forEach { taskId ->
                        val task = container.tasks.findById(taskId)
                            ?.takeIf { taskStateOrNull(it) == TranscriptionTaskState.COMPLETED }
                            ?: return@forEach
                        if (runCatching {
                                cleanupTaskFiles(task)
                                container.tasks.delete(task.id)
                            }.getOrDefault(false)
                        ) {
                            add(task.id)
                        }
                    }
                }
            }
            if (deletedIds.isEmpty()) {
                _uiState.update { it.copy(statusMessage = "没有可删除的历史记录") }
                return@launch
            }
            executionRequests.removeAll(deletedIds)
            _uiState.update { current ->
                current.copy(
                    historyPreviews = current.historyPreviews - deletedIds,
                    invalidHistoryTaskIds = current.invalidHistoryTaskIds - deletedIds,
                    openedResultTaskId = current.openedResultTaskId.takeUnless { it in deletedIds },
                    transcriptText = if (current.openedResultTaskId in deletedIds) null else current.transcriptText,
                    statusMessage = "已删除 ${deletedIds.size} 条历史记录，原音视频和已导出文件不受影响",
                )
            }
            Toast.makeText(app, "已删除 ${deletedIds.size} 条历史记录", Toast.LENGTH_SHORT).show()
        }
    }

    fun cleanInvalidHistory() {
        deleteHistoryTasks(_uiState.value.invalidHistoryTaskIds)
    }

    fun focusTask(taskId: String) {
        viewModelScope.launch {
            val task = withContext(Dispatchers.IO) { container.tasks.findById(taskId) } ?: return@launch
            currentTaskId = task.id
            if (taskStateOrNull(task) == TranscriptionTaskState.COMPLETED) {
                loadTaskResult(task.id)
            } else {
                _uiState.update {
                    it.copy(
                        activeSourceName = task.sourceDisplayName,
                        activeTaskId = task.id.takeIf { taskStateOrNull(task) in ACTIVE_TASK_STATES },
                        stage = if (taskStateOrNull(task) in setOf(
                                TranscriptionTaskState.FAILED,
                                TranscriptionTaskState.RECOVERY_REQUIRED,
                                TranscriptionTaskState.NO_SPEECH,
                                TranscriptionTaskState.WAITING_INPUT,
                                TranscriptionTaskState.WAITING_MODEL,
                            )
                        ) {
                            WorkflowStage.ERROR
                        } else {
                            workflowStage(task)
                        },
                    statusMessage = task.userMessage ?: "已打开对应转写任务",
                    exportFeedback = null,
                )
            }
            }
        }
    }

    fun resumeActiveTasks() {
        executionRequests.clear()
        tasks.value
            .filter { taskStateOrNull(it) in ACTIVE_TASK_STATES }
            .minByOrNull { it.queuePosition }
            ?.let { task -> viewModelScope.launch { requestTaskExecutionOnce(task.id) } }
    }

    fun exportResultToDefaultDirectory(format: TranscriptExportFormat) {
        if (_uiState.value.isExporting) return
        val taskId = _uiState.value.openedResultTaskId
        val task = taskId?.let { id -> tasks.value.firstOrNull { it.id == id } }
        val document = latestDocument
        val settingsSnapshot = settings.value
        val outputDirectory = settingsSnapshot.defaultOutputDirectoryUri
        if (task == null || document == null) {
            _uiState.update {
                it.copy(
                    statusMessage = "当前完整结果尚未加载，无法导出",
                    exportFeedback = ExportFeedback("当前完整结果尚未加载，无法导出", ExportFeedbackTone.ERROR),
                )
            }
            return
        }
        if (outputDirectory == null) {
            _uiState.update {
                it.copy(
                    statusMessage = "未设置默认输出目录，请先到设置中选择目录",
                    exportFeedback = ExportFeedback("未设置默认输出目录，请先到设置中选择目录", ExportFeedbackTone.ERROR),
                )
            }
            return
        }

        val exportDocument = if (format == TranscriptExportFormat.SRT) {
            document
        } else {
            document.copy(polishedText = _uiState.value.transcriptText.orEmpty())
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExporting = true,
                    statusMessage = "正在导出 ${format.name}",
                    exportFeedback = ExportFeedback("正在导出 ${format.name}…", ExportFeedbackTone.WORKING),
                )
            }
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    outputStore.export(
                        treeUri = Uri.parse(outputDirectory),
                        sourceDisplayName = task.sourceDisplayName,
                        document = exportDocument,
                        format = format,
                        conflictPolicy = settingsSnapshot.outputConflictPolicy,
                    )
                }
            }
            _uiState.update {
                val message = result.fold(
                    onSuccess = { "${format.name} 已保存到默认输出目录" },
                    onFailure = ::exportFailureMessage,
                )
                it.copy(
                    isExporting = false,
                    statusMessage = message,
                    exportFeedback = ExportFeedback(
                        message = message,
                        tone = if (result.isSuccess) ExportFeedbackTone.SUCCESS else ExportFeedbackTone.ERROR,
                    ),
                )
            }
        }
    }

    fun setLastExportFormat(format: TranscriptExportFormat) {
        viewModelScope.launch {
            container.settings.setLastExportFormat(format)
        }
    }

    private fun exportFailureMessage(error: Throwable): String = when (error) {
        is SecurityException -> "默认输出目录的写入授权已失效，请在设置中重新选择该目录"
        is java.io.FileNotFoundException -> "默认输出目录暂时不可用，请确认存储设备已连接后重试"
        is java.io.IOException -> error.message ?: "默认输出目录无法写入，请重试"
        else -> "导出未完成，请重试；若仍失败请在设置中重新选择默认目录"
    }

    fun updateTranscriptDraft(value: String) {
        if (_uiState.value.openedResultTaskId == null) return
        _uiState.update { it.copy(transcriptText = value) }
    }

    fun setLanguageCode(value: String?) {
        viewModelScope.launch { container.settings.setLanguageCode(value) }
    }

    fun setModelId(value: String) {
        if (_uiState.value.isBusy || OfficialModelCatalog.find(value) == null) return
        viewModelScope.launch { container.settings.setModelId(value) }
    }

    fun setThreadCount(value: Int) {
        viewModelScope.launch { container.settings.setThreadCount(value) }
    }

    fun setKeepScreenOn(value: Boolean) {
        viewModelScope.launch { container.settings.setKeepScreenOn(value) }
    }

    fun setDefaultOutputDirectory(uri: Uri) {
        viewModelScope.launch {
            val persisted = runCatching {
                resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                true
            }.getOrDefault(false)
            if (!persisted) {
                _uiState.update { it.copy(statusMessage = "无法长期保留该目录权限，请重新选择其他目录") }
                return@launch
            }
            container.settings.setDefaultOutputDirectoryUri(uri.toString())
            _uiState.update { it.copy(statusMessage = "已设置默认输出目录，可在结果页手动导出") }
        }
    }

    fun clearDefaultOutputDirectory() {
        viewModelScope.launch {
            container.settings.setDefaultOutputDirectoryUri(null)
            _uiState.update { it.copy(statusMessage = "已清除默认输出目录，转写结果仍会保留在历史页") }
        }
    }

    fun setOutputConflictPolicy(value: OutputConflictPolicy) {
        viewModelScope.launch { container.settings.setOutputConflictPolicy(value) }
    }

    fun setPostProcessEnabled(value: Boolean) {
        viewModelScope.launch {
            runCatching { container.settings.setPostProcessEnabled(value) }
                .onFailure { error ->
                    _uiState.update { it.copy(statusMessage = error.message ?: "无法更新翻译润色开关") }
                }
        }
    }

    fun savePostProcessConnection(baseUrl: String, model: String) {
        viewModelScope.launch {
            runCatching { container.settings.setPostProcessConnection(baseUrl, model) }
                .onSuccess {
                    _uiState.update { it.copy(statusMessage = "已保存翻译润色服务设置") }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(statusMessage = error.message ?: "翻译润色服务设置无效") }
                }
        }
    }

    fun savePostProcessApiKey(value: String) {
        viewModelScope.launch {
            runCatching { container.settings.savePostProcessApiKey(value) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            statusMessage = "千问 API Key 已由 Android Keystore 加密保存",
                            revealedPostProcessApiKey = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(statusMessage = error.message ?: "无法保存 API Key") }
                }
        }
    }

    fun clearPostProcessApiKey() {
        viewModelScope.launch {
            container.settings.clearPostProcessApiKey()
            _uiState.update {
                it.copy(
                    statusMessage = "已删除本机千问 API Key",
                    revealedPostProcessApiKey = null,
                )
            }
        }
    }

    fun revealPostProcessApiKey() {
        viewModelScope.launch {
            val apiKey = withContext(Dispatchers.IO) { container.settings.readPostProcessApiKey() }
            _uiState.update { it.copy(revealedPostProcessApiKey = apiKey) }
        }
    }

    fun setSkinId(value: String) {
        if (com.nanzhufeng.transcriber.ui.theme.NanfengSkinCatalog.options.none { it.id == value }) return
        viewModelScope.launch { container.settings.setSkinId(value) }
    }

    private suspend fun refreshModelState(model: CatalogModel = selectedModel) {
        if (!model.requiresLocalCache) {
            if (model.manifest.modelId != selectedModel.manifest.modelId) return
            readyModelPath = null
            _uiState.update {
                it.copy(
                    modelDisplayName = model.displayName,
                    modelRequiresLocalCache = false,
                    modelExpectedBytes = 0L,
                    modelCacheBytes = 0L,
                    stage = WorkflowStage.IDLE,
                    modelState = ModelInstallState.READY,
                    modelProgress = null,
                    statusMessage = "Qwen3-ASR API 将直接发送所选音频片段；需先配置千问 API Key",
                )
            }
            return
        }
        val inspection = withContext(Dispatchers.IO) {
            container.modelStore.inspect(model.manifest)
        }
        val cacheBytes = withContext(Dispatchers.IO) {
            container.modelStore.cacheBytes(model.manifest)
        }
        if (model.manifest.modelId != selectedModel.manifest.modelId) return
        readyModelPath = inspection.modelPath
        _uiState.update {
            it.copy(
                modelDisplayName = model.displayName,
                modelRequiresLocalCache = true,
                modelExpectedBytes = model.manifest.expectedBytes,
                modelCacheBytes = cacheBytes,
                stage = WorkflowStage.IDLE,
                modelState = inspection.state,
                modelProgress = null,
                statusMessage = when {
                    inspection.state == ModelInstallState.READY -> "${model.displayName} 已校验，将直接复用长期缓存"
                    inspection.state == ModelInstallState.DOWNLOADING -> "发现未完成模型，可继续下载"
                    else -> inspection.reason
                },
            )
        }
    }

    private fun acceptReadyModel(model: CatalogModel, path: Path, message: String) {
        if (model.manifest.modelId != selectedModel.manifest.modelId) return
        readyModelPath = path
        _uiState.update {
            it.copy(
                stage = WorkflowStage.IDLE,
                modelState = ModelInstallState.READY,
                modelProgress = null,
                transferredModelBytes = model.manifest.expectedBytes,
                modelCacheBytes = model.manifest.expectedBytes,
                statusMessage = "$message；以后启动直接复用，不会重复下载",
            )
        }
    }

    private fun updateModelTransferProgress(
        model: CatalogModel,
        transferStage: ModelTransferStage,
        completedBytes: Long,
        totalBytes: Long,
    ) {
        if (model.manifest.modelId != selectedModel.manifest.modelId) return
        val fraction = if (totalBytes > 0L) completedBytes.toFloat() / totalBytes.toFloat() else 0f
        _uiState.update {
            it.copy(
                stage = when (transferStage) {
                    ModelTransferStage.IMPORTING -> WorkflowStage.IMPORTING_MODEL
                    ModelTransferStage.EXPORTING -> WorkflowStage.EXPORTING_MODEL
                    ModelTransferStage.VERIFYING -> WorkflowStage.VERIFYING_MODEL
                    ModelTransferStage.DOWNLOADING -> WorkflowStage.DOWNLOADING_MODEL
                },
                modelState = if (transferStage == ModelTransferStage.VERIFYING) {
                    ModelInstallState.VERIFYING
                } else {
                    it.modelState
                },
                modelProgress = fraction.coerceIn(0f, 1f),
                transferredModelBytes = completedBytes,
            )
        }
    }

    private suspend fun finishModelFailure(model: CatalogModel, message: String) {
        val inspection = withContext(Dispatchers.IO) { container.modelStore.inspect(model.manifest) }
        val bytes = withContext(Dispatchers.IO) { container.modelStore.cacheBytes(model.manifest) }
        if (model.manifest.modelId != selectedModel.manifest.modelId) return
        readyModelPath = inspection.modelPath
        _uiState.update {
            it.copy(
                stage = WorkflowStage.ERROR,
                modelState = inspection.state,
                modelProgress = null,
                modelCacheBytes = bytes,
                statusMessage = message,
            )
        }
    }

    private suspend fun synchronizeTaskState(taskList: List<TranscriptionTaskEntity>) {
        val selectableIds = taskList
            .filter { taskStateOrNull(it)?.let(TaskSelectionPolicy::canSelectForStart) == true }
            .mapTo(mutableSetOf(), TranscriptionTaskEntity::id)
        _uiState.update { state ->
            state.copy(selectedTaskIds = state.selectedTaskIds.intersect(selectableIds))
        }
        val active = taskList
            .filter { taskStateOrNull(it) in ACTIVE_TASK_STATES }
            .minByOrNull { it.queuePosition }
        if (active != null) {
            currentTaskId = active.id
            selectedSourceUri = Uri.parse(active.sourceUri)
            requestTaskExecutionOnce(active.id)
            val total = active.totalDurationMillis
            val progress = total?.takeIf { it > 0L }?.let {
                (active.progressMillis.toFloat() / it.toFloat()).coerceIn(0f, 1f)
            }
            _uiState.update {
                it.copy(
                    activeSourceName = active.sourceDisplayName,
                    sourceAccessPersisted = active.sourceAccessMode == SourceAccessMode.PERSISTED_DOCUMENT.name,
                    activeTaskId = active.id,
                    stage = workflowStage(active),
                    progress = progress,
                    statusMessage = active.userMessage ?: "后台转写任务正在继续",
                )
            }
            return
        }

        val currentId = currentTaskId ?: return
        val current = taskList.firstOrNull { it.id == currentId } ?: return
        when (taskStateOrNull(current)) {
            TranscriptionTaskState.COMPLETED -> if (latestDocument == null) {
                executionRequests.remove(current.id)
                loadTaskResult(current.id)
            }
            TranscriptionTaskState.FAILED,
            TranscriptionTaskState.RECOVERY_REQUIRED,
            TranscriptionTaskState.NO_SPEECH,
            TranscriptionTaskState.WAITING_INPUT,
            TranscriptionTaskState.WAITING_MODEL,
            -> _uiState.update {
                executionRequests.remove(current.id)
                it.copy(
                    activeTaskId = null,
                    activeSourceName = null,
                    stage = WorkflowStage.ERROR,
                    progress = null,
                    statusMessage = current.userMessage ?: if (taskStateOrNull(current) == TranscriptionTaskState.NO_SPEECH) {
                        "未识别到可读语音，可调整模型或语言后重试"
                    } else {
                        "任务需要处理，可在首页重试"
                    },
                )
            }
            TranscriptionTaskState.CANCELLED -> _uiState.update {
                executionRequests.remove(current.id)
                it.copy(
                    activeTaskId = null,
                    activeSourceName = null,
                    stage = WorkflowStage.IDLE,
                    progress = null,
                    statusMessage = current.userMessage ?: "转写已取消",
                )
            }
            else -> Unit
        }
    }

    private suspend fun loadTaskResult(taskId: String) {
        val task = withContext(Dispatchers.IO) { container.tasks.findById(taskId) }
        if (task == null || taskStateOrNull(task) != TranscriptionTaskState.COMPLETED) {
            _uiState.update { it.copy(statusMessage = "这条记录尚无可打开的完整结果") }
            return
        }
        val result = runCatching { withContext(Dispatchers.IO) { loadStoredTranscript(task) } }
        val stored = result.getOrNull()
        if (stored == null) {
            _uiState.update {
                it.copy(
                    stage = WorkflowStage.ERROR,
                    statusMessage = "历史结果文件无法读取，请保留记录并重新转写",
                )
            }
            return
        }
        currentTaskId = task.id
        latestDocument = stored.document
        _uiState.update {
            it.copy(
                activeTaskId = null,
                activeSourceName = null,
                stage = WorkflowStage.COMPLETED,
                progress = 1f,
                transcriptText = exportService.renderTxt(stored.document).trim(),
                openedResultTaskId = task.id,
                detectedLanguage = stored.detectedLanguage,
                performanceSummary = stored.performanceSummary,
                statusMessage = "已打开历史完整结果，可再次导出 TXT、MD、SRT 或 DOCX",
            )
        }
    }

    private suspend fun refreshHistoryPreviews(taskList: List<TranscriptionTaskEntity>) {
        val completed = taskList.filter {
            taskStateOrNull(it) == TranscriptionTaskState.COMPLETED && it.outputUri != null
        }
        val completedIds = completed.mapTo(mutableSetOf(), TranscriptionTaskEntity::id)
        val invalidIds = withContext(Dispatchers.IO) {
            completed.filterNot(::isStoredResultReadable).mapTo(mutableSetOf(), TranscriptionTaskEntity::id)
        }
        val readable = completed.filterNot { it.id in invalidIds }
        val existing = _uiState.value.historyPreviews.filterKeys { it in completedIds }
        val missing = readable.filterNot { existing.containsKey(it.id) }
        if (missing.isEmpty()) {
            if (existing.size != _uiState.value.historyPreviews.size || invalidIds != _uiState.value.invalidHistoryTaskIds) {
                _uiState.update { it.copy(historyPreviews = existing, invalidHistoryTaskIds = invalidIds) }
            }
            return
        }
        val loaded = withContext(Dispatchers.IO) {
            missing.associate { task ->
                task.id to runCatching { loadPreviewText(task) }
                    .getOrDefault("结果文件暂时无法读取")
            }
        }
        _uiState.update { it.copy(historyPreviews = existing + loaded, invalidHistoryTaskIds = invalidIds) }
    }

    private fun isStoredResultReadable(task: TranscriptionTaskEntity): Boolean = runCatching {
        val outputUri = requireNotNull(task.outputUri)
        Files.isRegularFile(Paths.get(URI(outputUri)))
    }.getOrDefault(false)

    private fun loadStoredTranscript(task: TranscriptionTaskEntity): StoredTranscript {
        val outputUri = task.outputUri ?: error("任务没有结果位置")
        val path = Paths.get(URI(outputUri))
        if (path.fileName.toString().endsWith(".json", ignoreCase = true)) {
            return documentStore.load(path)
        }
        val text = Files.newBufferedReader(path).use { it.readText() }
        return StoredTranscript(
            document = TranscriptDocument(
                title = task.sourceDisplayName.substringBeforeLast('.').ifBlank {
                    "南枫转写结果"
                },
                segments = listOf(
                    TranscriptDocumentSegment(
                        startMillis = 0L,
                        endMillis = task.totalDurationMillis ?: 0L,
                        text = text.trim(),
                    ),
                ),
            ),
            performanceSummary = task.technicalDetail,
        )
    }

    private fun loadPreviewText(task: TranscriptionTaskEntity): String {
        val outputPath = Paths.get(URI(requireNotNull(task.outputUri)))
        val plainTextPath = if (outputPath.fileName.toString().endsWith(".json", ignoreCase = true)) {
            outputPath.resolveSibling("transcript.txt")
        } else {
            outputPath
        }
        val text = if (Files.isRegularFile(plainTextPath)) {
            Files.newBufferedReader(plainTextPath).use { reader ->
                val buffer = CharArray(360)
                val count = reader.read(buffer)
                if (count <= 0) "" else String(buffer, 0, count)
            }
        } else {
            exportService.renderTxt(loadStoredTranscript(task).document)
        }
        return formatTranscriptPreview(text, maxCharacters = 120)
    }

    private fun cleanupTaskFiles(task: TranscriptionTaskEntity) {
        task.stagedInputPath?.let { staged -> Files.deleteIfExists(Paths.get(staged)) }
        val taskDirectory = app.noBackupFilesDir.resolve("tasks/${task.id}").toPath()
        if (!Files.exists(taskDirectory)) return
        Files.walk(taskDirectory).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach { path -> Files.deleteIfExists(path) }
        }
    }

    private fun workflowStage(task: TranscriptionTaskEntity): WorkflowStage = when (taskStateOrNull(task)) {
        TranscriptionTaskState.QUEUED,
        TranscriptionTaskState.PREPARING,
        -> WorkflowStage.DECODING
        TranscriptionTaskState.TRANSCRIBING -> WorkflowStage.TRANSCRIBING
        TranscriptionTaskState.EXPORTING -> WorkflowStage.EXPORTING
        else -> WorkflowStage.IDLE
    }

    private fun taskStateOrNull(task: TranscriptionTaskEntity): TranscriptionTaskState? =
        runCatching { TranscriptionTaskState.valueOf(task.state) }.getOrNull()

    private suspend fun requestTaskExecutionOnce(taskId: String) {
        if (!executionRequests.add(taskId)) return
        val startResult = runCatching { TranscriptionForegroundService.start(app, taskId) }
        if (startResult.isSuccess) return

        executionRequests.remove(taskId)

        val task = withContext(Dispatchers.IO) { container.tasks.findById(taskId) }
        val state = task?.let(::taskStateOrNull)
        if (state != null && com.nanzhufeng.transcriber.domain.task.TaskTransitionPolicy.canTransition(
                state,
                TranscriptionTaskState.FAILED,
            )
        ) {
            withContext(Dispatchers.IO) {
                container.tasks.transition(
                    id = taskId,
                    target = TranscriptionTaskState.FAILED,
                    errorCode = "FOREGROUND_SERVICE_START_FAILED",
                    userMessage = "系统未允许启动后台转写，请保持 App 在前台后重试",
                    technicalDetail = startResult.exceptionOrNull()?.message,
                )
            }
        }
        _uiState.update {
            it.copy(
                activeTaskId = null,
                stage = WorkflowStage.ERROR,
                progress = null,
                statusMessage = "系统未允许启动后台转写，请保持 App 在前台后重试",
            )
        }
    }

    private fun readSourceMetadata(uri: Uri): Pair<String, Long?> {
        var name: String? = null
        var size: Long? = null
        resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) name = cursor.getString(nameIndex)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
            }
        }
        return (name ?: uri.lastPathSegment ?: "所选音视频") to size
    }

    private fun updatePendingSourceState(message: String) {
        val selected = pendingSources.filter(PendingSource::selected)
        selectedSourceUri = selected.firstOrNull()?.uri
        _uiState.update { state ->
            state.copy(
                pendingSourceItems = pendingSources.map(PendingSource::toUiItem),
                selectedSourceName = selected.displaySummary(),
                selectedSourceBytes = selected.totalBytes(),
                selectedSourceCount = selected.size,
                selectedSourceNames = selected.map(PendingSource::displayName),
                sourceAccessPersisted = selected.isNotEmpty() && selected.all(PendingSource::persisted),
                statusMessage = message,
            )
        }
    }

    private fun stageSource(uri: Uri, displayName: String): Path? = runCatching {
        val extension = displayName.substringAfterLast('.', "").takeIf {
            it.length in 1..8 && it.all(Char::isLetterOrDigit)
        }
        val targetDirectory = app.noBackupFilesDir.resolve("staged_inputs").toPath()
        Files.createDirectories(targetDirectory)
        val target = targetDirectory.resolve(
            buildString {
                append(UUID.randomUUID())
                extension?.let { append('.').append(it.lowercase()) }
            },
        )
        resolver.openInputStream(uri)?.use { input ->
            Files.newOutputStream(
                target,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE,
            ).use { output -> input.copyTo(output) }
        } ?: error("系统未能读取拖入的文件")
        target
    }.getOrNull()
}

private data class PendingSource(
    val uri: Uri,
    val displayName: String,
    val bytes: Long?,
    val persisted: Boolean,
    val stagedPath: Path?,
    val selected: Boolean,
    val modelId: String,
    val modelVersion: String,
    val language: String?,
    val threadCount: Int,
    val outputFormat: TranscriptionOutputFormat,
) {
    val key: String get() = uri.toString()

    fun toUiItem(): PendingSourceUi = PendingSourceUi(
        key = key,
        sourceUri = uri.toString(),
        stagedInputPath = stagedPath?.toString(),
        displayName = displayName,
        bytes = bytes,
        selected = selected,
        modelId = modelId,
        modelLabel = OfficialModelCatalog.find(modelId)?.displayName ?: modelId,
        language = language,
        outputFormat = outputFormat,
    )
}

data class PendingSourceUi(
    val key: String,
    val sourceUri: String,
    val stagedInputPath: String?,
    val displayName: String,
    val bytes: Long?,
    val selected: Boolean,
    val modelId: String,
    val modelLabel: String,
    val language: String?,
    val outputFormat: TranscriptionOutputFormat,
)

private fun List<PendingSource>.displaySummary(): String? = when (size) {
    0 -> null
    1 -> first().displayName
    else -> "$size 个文件"
}

private fun List<PendingSource>.totalBytes(): Long? =
    mapNotNull(PendingSource::bytes).takeIf { it.isNotEmpty() }?.sum()

data class TranscriptionUiState(
    val nativeStatus: String,
    val modelDisplayName: String,
    val modelRequiresLocalCache: Boolean = true,
    val modelExpectedBytes: Long,
    val modelCacheBytes: Long = 0L,
    val modelState: ModelInstallState = ModelInstallState.NOT_INSTALLED,
    val modelProgress: Float? = null,
    val transferredModelBytes: Long = 0L,
    val selectedSourceName: String? = null,
    val selectedSourceBytes: Long? = null,
    val selectedSourceCount: Int = 0,
    val selectedSourceNames: List<String> = emptyList(),
    val pendingSourceItems: List<PendingSourceUi> = emptyList(),
    val selectedTaskIds: Set<String> = emptySet(),
    val activeSourceName: String? = null,
    val sourceAccessPersisted: Boolean = false,
    val stage: WorkflowStage = WorkflowStage.CHECKING,
    val progress: Float? = null,
    val transcriptText: String? = null,
    val historyPreviews: Map<String, String> = emptyMap(),
    val invalidHistoryTaskIds: Set<String> = emptySet(),
    val openedResultTaskId: String? = null,
    val detectedLanguage: String? = null,
    val performanceSummary: String? = null,
    val statusMessage: String = "正在检查模型缓存",
    val revealedPostProcessApiKey: String? = null,
    val isExporting: Boolean = false,
    val exportFeedback: ExportFeedback? = null,
    val activeTaskId: String? = null,
) {
    val isBusy: Boolean
        get() = stage in setOf(
            WorkflowStage.DOWNLOADING_MODEL,
            WorkflowStage.IMPORTING_MODEL,
            WorkflowStage.VERIFYING_MODEL,
            WorkflowStage.EXPORTING_MODEL,
            WorkflowStage.DECODING,
            WorkflowStage.LOADING_MODEL,
            WorkflowStage.TRANSCRIBING,
            WorkflowStage.EXPORTING,
        )

    val canStart: Boolean
        get() = selectedSourceName != null &&
            !isBusy &&
            !isExporting
}

data class ExportFeedback(
    val message: String,
    val tone: ExportFeedbackTone,
)

enum class ExportFeedbackTone {
    WORKING,
    SUCCESS,
    ERROR,
}

enum class WorkflowStage {
    CHECKING,
    IDLE,
    DOWNLOADING_MODEL,
    IMPORTING_MODEL,
    VERIFYING_MODEL,
    EXPORTING_MODEL,
    DECODING,
    LOADING_MODEL,
    TRANSCRIBING,
    EXPORTING,
    COMPLETED,
    ERROR,
}

private val ACTIVE_TASK_STATES = setOf(
    TranscriptionTaskState.QUEUED,
    TranscriptionTaskState.PREPARING,
    TranscriptionTaskState.TRANSCRIBING,
    TranscriptionTaskState.EXPORTING,
)

private const val MAX_FOLDER_MEDIA_FILES = 2_000

private val SUPPORTED_MEDIA_EXTENSIONS = setOf(
    "aac", "flac", "m4a", "mp3", "ogg", "opus", "wav", "wma",
    "3gp", "avi", "m4v", "mkv", "mov", "mp4", "mpeg", "mpg", "webm",
)

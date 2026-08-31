package com.nanzhufeng.transcriber.ui.screens

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.nanzhufeng.transcriber.BuildConfig
import com.nanzhufeng.transcriber.data.modelstore.OfficialModelCatalog
import com.nanzhufeng.transcriber.data.settings.TranscriptionSettings
import com.nanzhufeng.transcriber.data.task.OutputConflictPolicy
import com.nanzhufeng.transcriber.domain.invocation.AsrInvocationRecord
import com.nanzhufeng.transcriber.domain.model.ModelInstallState
import com.nanzhufeng.transcriber.ui.TranscriptionUiState
import com.nanzhufeng.transcriber.ui.components.SectionHeading
import com.nanzhufeng.transcriber.ui.components.StatusPill
import com.nanzhufeng.transcriber.ui.components.SubtleActionButton
import com.nanzhufeng.transcriber.ui.components.WorkbenchCard
import com.nanzhufeng.transcriber.ui.components.formatBytes
import com.nanzhufeng.transcriber.ui.theme.AttentionOchre
import com.nanzhufeng.transcriber.ui.theme.ParameterPurple
import com.nanzhufeng.transcriber.ui.theme.NanfengSkinCatalog

@Composable
fun SettingsScreen(
    state: TranscriptionUiState,
    settings: TranscriptionSettings,
    expanded: Boolean,
    onDownloadModel: () -> Unit,
    onImportModel: () -> Unit,
    onExportModel: () -> Unit,
    onVerifyModel: () -> Unit,
    onDeleteModel: () -> Unit,
    onModelChanged: (String) -> Unit,
    onLanguageChanged: (String?) -> Unit,
    onThreadCountChanged: (Int) -> Unit,
    onChooseOutputDirectory: () -> Unit,
    onClearOutputDirectory: () -> Unit,
    onConflictPolicyChanged: (OutputConflictPolicy) -> Unit,
    onSavePostProcessApiKey: (String) -> Unit,
    onRevealPostProcessApiKey: () -> Unit,
    onSkinChanged: (String) -> Unit,
    invocationRecords: List<AsrInvocationRecord>,
) {
    var showingInvocationHistory by rememberSaveable { mutableStateOf(false) }
    if (showingInvocationHistory) {
        AsrInvocationHistoryScreen(
            records = invocationRecords,
            onBack = { showingInvocationHistory = false },
        )
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 14.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "设置",
                style = if (expanded) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
        }

        if (expanded) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 14.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(SettingsModule.entries) { module ->
                    SettingsModuleCard(
                        module = module,
                        state = state,
                        settings = settings,
                        onDownloadModel = onDownloadModel,
                        onImportModel = onImportModel,
                        onExportModel = onExportModel,
                        onVerifyModel = onVerifyModel,
                        onDeleteModel = onDeleteModel,
                        onModelChanged = onModelChanged,
                        onLanguageChanged = onLanguageChanged,
                        onThreadCountChanged = onThreadCountChanged,
                        onChooseOutputDirectory = onChooseOutputDirectory,
                        onClearOutputDirectory = onClearOutputDirectory,
                        onConflictPolicyChanged = onConflictPolicyChanged,
                        onSavePostProcessApiKey = onSavePostProcessApiKey,
                        onRevealPostProcessApiKey = onRevealPostProcessApiKey,
                        onSkinChanged = onSkinChanged,
                        invocationRecords = invocationRecords,
                        onOpenInvocationHistory = { showingInvocationHistory = true },
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 14.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(SettingsModule.entries.size) { index ->
                    SettingsModuleCard(
                        module = SettingsModule.entries[index],
                        state = state,
                        settings = settings,
                        onDownloadModel = onDownloadModel,
                        onImportModel = onImportModel,
                        onExportModel = onExportModel,
                        onVerifyModel = onVerifyModel,
                        onDeleteModel = onDeleteModel,
                        onModelChanged = onModelChanged,
                        onLanguageChanged = onLanguageChanged,
                        onThreadCountChanged = onThreadCountChanged,
                        onChooseOutputDirectory = onChooseOutputDirectory,
                        onClearOutputDirectory = onClearOutputDirectory,
                        onConflictPolicyChanged = onConflictPolicyChanged,
                        onSavePostProcessApiKey = onSavePostProcessApiKey,
                        onRevealPostProcessApiKey = onRevealPostProcessApiKey,
                        onSkinChanged = onSkinChanged,
                        invocationRecords = invocationRecords,
                        onOpenInvocationHistory = { showingInvocationHistory = true },
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsModuleCard(
    module: SettingsModule,
    state: TranscriptionUiState,
    settings: TranscriptionSettings,
    onDownloadModel: () -> Unit,
    onImportModel: () -> Unit,
    onExportModel: () -> Unit,
    onVerifyModel: () -> Unit,
    onDeleteModel: () -> Unit,
    onModelChanged: (String) -> Unit,
    onLanguageChanged: (String?) -> Unit,
    onThreadCountChanged: (Int) -> Unit,
    onChooseOutputDirectory: () -> Unit,
    onClearOutputDirectory: () -> Unit,
    onConflictPolicyChanged: (OutputConflictPolicy) -> Unit,
    onSavePostProcessApiKey: (String) -> Unit,
    onRevealPostProcessApiKey: () -> Unit,
    onSkinChanged: (String) -> Unit,
    invocationRecords: List<AsrInvocationRecord>,
    onOpenInvocationHistory: () -> Unit,
) {
    when (module) {
        SettingsModule.MODEL -> ModelSelectionCard(
            selectedModelId = settings.modelId,
            isBusy = state.isBusy,
            onModelChanged = onModelChanged,
        )
        SettingsModule.MODEL_DETAILS -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ModelDetailsCard(
                state = state,
                settings = settings,
                selectedModelId = settings.modelId,
                onDownloadModel = onDownloadModel,
                onImportModel = onImportModel,
                onExportModel = onExportModel,
                onVerifyModel = onVerifyModel,
                onDeleteModel = onDeleteModel,
                onSaveApiKey = onSavePostProcessApiKey,
                onRevealApiKey = onRevealPostProcessApiKey,
            )
            if (OfficialModelCatalog.find(settings.modelId)?.requiresLocalCache == false) {
                AsrInvocationLedgerEntry(
                    records = invocationRecords,
                    onOpen = onOpenInvocationHistory,
                )
            }
        }
        SettingsModule.TRANSCRIPTION -> TranscriptionSettingsCard(
            settings = settings,
            onLanguageChanged = onLanguageChanged,
            onThreadCountChanged = onThreadCountChanged,
        )
        SettingsModule.OUTPUT -> OutputSettingsCard(
            settings = settings,
            onChooseOutputDirectory = onChooseOutputDirectory,
            onClearOutputDirectory = onClearOutputDirectory,
            onConflictPolicyChanged = onConflictPolicyChanged,
        )
        SettingsModule.APPEARANCE -> AppearanceCard(settings, onSkinChanged)
        SettingsModule.PRIVACY -> PrivacyCard(state, settings)
    }
}

@Composable
private fun HighAccuracyServiceCard(
    settings: TranscriptionSettings,
    state: TranscriptionUiState,
    onSaveApiKey: (String) -> Unit,
    onRevealApiKey: () -> Unit,
) {
    // 与南枫 AI 一致：已保存凭据先显示固定长度掩码；只有点眼睛才解密回填。
    val savedKeyMask = "••••••••••••••••••••••••••••••••"
    var apiKey by remember(settings.postProcessApiKeyConfigured) {
        mutableStateOf(if (settings.postProcessApiKeyConfigured) savedKeyMask else "")
    }
    var apiKeyEdited by remember(settings.postProcessApiKeyConfigured) { mutableStateOf(false) }
    var apiKeyVisible by remember { mutableStateOf(false) }
    LaunchedEffect(state.revealedPostProcessApiKey) {
        if (!apiKeyEdited && state.revealedPostProcessApiKey != null) {
            apiKey = state.revealedPostProcessApiKey
            apiKeyVisible = true
        }
    }
    WorkbenchCard(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(2.dp))
        SettingValueRow("转写模型", "Qwen3-ASR")
        OutlinedTextField(
            value = apiKey,
            onValueChange = { input ->
                apiKey = if (!apiKeyEdited && input.startsWith(savedKeyMask)) {
                    input.removePrefix(savedKeyMask)
                } else {
                    input
                }
                apiKeyEdited = true
            },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            singleLine = true,
            visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            label = { Text("千问 API Key") },
            trailingIcon = {
                IconButton(onClick = {
                    if (apiKeyVisible) {
                        apiKeyVisible = false
                    } else if (!apiKeyEdited && settings.postProcessApiKeyConfigured) {
                        onRevealApiKey()
                    } else {
                        apiKeyVisible = true
                    }
                }) {
                    Icon(
                        imageVector = if (apiKeyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (apiKeyVisible) "隐藏 API Key" else "显示 API Key",
                    )
                }
            },
        )
        Button(
            onClick = {
                onSaveApiKey(apiKey)
                apiKey = savedKeyMask
                apiKeyEdited = false
                apiKeyVisible = false
            },
            enabled = apiKeyEdited && apiKey.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text("保存 API Key") }
    }
}

@Composable
private fun ModelSelectionCard(
    selectedModelId: String,
    isBusy: Boolean,
    onModelChanged: (String) -> Unit,
) {
    WorkbenchCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeading(title = "转写模型")
        Spacer(Modifier.height(10.dp))
        OfficialModelCatalog.modelSelectionCandidates.forEach { model ->
            ModelChoiceRow(
                label = model.displayName,
                selected = model.manifest.modelId == selectedModelId,
                enabled = !isBusy,
                onClick = { onModelChanged(model.manifest.modelId) },
            )
        }
    }
}

@Composable
private fun ModelDetailsCard(
    state: TranscriptionUiState,
    settings: TranscriptionSettings,
    selectedModelId: String,
    onDownloadModel: () -> Unit,
    onImportModel: () -> Unit,
    onExportModel: () -> Unit,
    onVerifyModel: () -> Unit,
    onDeleteModel: () -> Unit,
    onSaveApiKey: (String) -> Unit,
    onRevealApiKey: () -> Unit,
) {
    val selectedModel = OfficialModelCatalog.find(selectedModelId) ?: return
    if (selectedModel.requiresLocalCache) {
        ModelCacheCard(
            state = state,
            onDownloadModel = onDownloadModel,
            onImportModel = onImportModel,
            onExportModel = onExportModel,
            onVerifyModel = onVerifyModel,
            onDeleteModel = onDeleteModel,
        )
    } else {
        HighAccuracyServiceCard(
            settings = settings,
            state = state,
            onSaveApiKey = onSaveApiKey,
            onRevealApiKey = onRevealApiKey,
        )
    }
}

@Composable
private fun OutputSettingsCard(
    settings: TranscriptionSettings,
    onChooseOutputDirectory: () -> Unit,
    onClearOutputDirectory: () -> Unit,
    onConflictPolicyChanged: (OutputConflictPolicy) -> Unit,
) {
    WorkbenchCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeading(
            title = "输出与文件",
            color = ParameterPurple,
        )
        Spacer(Modifier.height(10.dp))
        SettingValueRow(
            "默认目录",
            settings.defaultOutputDirectoryUri?.let(::outputDirectoryLabel) ?: "未设置（仅保留历史）",
        )
        Button(
            onClick = onChooseOutputDirectory,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        ) { Text(if (settings.defaultOutputDirectoryUri == null) "选择默认输出目录" else "更换默认输出目录") }
        Spacer(Modifier.height(8.dp))
        SelectionField(
            label = "同名文件处理",
            selectedValue = settings.outputConflictPolicy.name,
            selectedLabel = conflictPolicyLabel(settings.outputConflictPolicy),
            options = OutputConflictPolicy.entries.map { it.name to conflictPolicyLabel(it) },
            onSelected = { onConflictPolicyChanged(OutputConflictPolicy.valueOf(it)) },
        )
        if (settings.defaultOutputDirectoryUri != null) {
            TextButton(
                onClick = onClearOutputDirectory,
                modifier = Modifier.align(Alignment.End),
            ) { Text("关闭自动导出") }
        }
    }
}

@Composable
private fun ModelCacheCard(
    state: TranscriptionUiState,
    onDownloadModel: () -> Unit,
    onImportModel: () -> Unit,
    onExportModel: () -> Unit,
    onVerifyModel: () -> Unit,
    onDeleteModel: () -> Unit,
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("删除所选模型缓存？") },
            text = {
                Text("只删除当前选中的 ${state.modelDisplayName}，其他模型、原文件和转写结果不受影响。需要时可重新下载或导入。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDeleteModel()
                    },
                ) { Text("删除缓存", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("取消") }
            },
        )
    }
    WorkbenchCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                SectionHeading(
                    title = "模型缓存",
                )
            }
            StatusPill(
                text = if (state.modelState == ModelInstallState.READY) {
                    "已就绪"
                } else {
                    "待准备"
                },
                color = if (state.modelState == ModelInstallState.READY) {
                    MaterialTheme.colorScheme.primary
                } else {
                    AttentionOchre
                },
            )
        }
        Spacer(Modifier.height(10.dp))
        SettingValueRow("本模型大小", formatBytes(state.modelExpectedBytes))
        SettingValueRow("当前占用", formatBytes(state.modelCacheBytes))
        SettingValueRow("缓存状态", modelStateText(state.modelState))
        state.modelProgress?.let { progress ->
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        }
        if (state.modelState != ModelInstallState.READY) {
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onDownloadModel,
                enabled = !state.isBusy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("下载所选模型") }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SubtleActionButton(
                onClick = onImportModel,
                enabled = !state.isBusy && state.modelState != ModelInstallState.READY,
                modifier = Modifier.weight(1f),
            ) { Text("导入备份") }
            SubtleActionButton(
                onClick = onVerifyModel,
                enabled = !state.isBusy && state.modelState == ModelInstallState.READY,
                modifier = Modifier.weight(1f),
            ) { Text("完整校验") }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SubtleActionButton(
                onClick = onExportModel,
                enabled = !state.isBusy && state.modelState == ModelInstallState.READY,
                modifier = Modifier.weight(1f),
            ) { Text("导出备份") }
            TextButton(
                onClick = { showDeleteConfirmation = true },
                enabled = !state.isBusy && state.modelCacheBytes > 0L,
                modifier = Modifier.weight(1f),
            ) { Text("删除缓存", color = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun ModelChoiceRow(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clickable(
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Black.copy(alpha = 0.045f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = null, enabled = enabled)
            Text(
                text = label,
                modifier = Modifier.padding(start = 8.dp),
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun TranscriptionSettingsCard(
    settings: TranscriptionSettings,
    onLanguageChanged: (String?) -> Unit,
    onThreadCountChanged: (Int) -> Unit,
) {
    WorkbenchCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeading(
            title = "转写参数",
            color = ParameterPurple,
        )
        Spacer(Modifier.height(12.dp))
        SelectionField(
            label = "识别语言",
            selectedValue = settings.languageCode ?: LANGUAGE_AUTO_KEY,
            selectedLabel = LanguageOption.entries.firstOrNull { it.code == settings.languageCode }?.label
                ?: LanguageOption.AUTO.label,
            options = LanguageOption.entries.map { (it.code ?: LANGUAGE_AUTO_KEY) to it.label },
            onSelected = { key -> onLanguageChanged(key.takeUnless { it == LANGUAGE_AUTO_KEY }) },
        )
        Spacer(Modifier.height(10.dp))
        SelectionField(
            label = "CPU 线程",
            selectedValue = settings.threadCount.toString(),
            selectedLabel = threadLabel(settings.threadCount),
            options = listOf(0, 2, 4, 6, 8).map { it.toString() to threadLabel(it) },
            onSelected = { onThreadCountChanged(it.toInt()) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionField(
    label: String,
    selectedValue: String,
    selectedLabel: String,
    options: List<Pair<String, String>>,
    enabled: Boolean = true,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
    ) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            readOnly = true,
            enabled = enabled,
            singleLine = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (value, optionLabel) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = optionLabel,
                            color = if (value == selectedValue) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = if (value == selectedValue) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        expanded = false
                        if (value != selectedValue) onSelected(value)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Composable
private fun AppearanceCard(
    settings: TranscriptionSettings,
    onSkinChanged: (String) -> Unit,
) {
    WorkbenchCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeading(
            title = "皮肤",
            color = AttentionOchre,
        )
        Spacer(Modifier.height(10.dp))
        SelectionField(
            label = "界面皮肤",
            selectedValue = settings.skinId,
            selectedLabel = NanfengSkinCatalog.nameOf(settings.skinId),
            options = NanfengSkinCatalog.options.map { it.id to it.displayName },
            onSelected = onSkinChanged,
        )
    }
}

@Composable
private fun PrivacyCard(state: TranscriptionUiState, settings: TranscriptionSettings) {
    WorkbenchCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeading(
            title = "隐私与诊断",
        )
        Spacer(Modifier.height(10.dp))
        SettingValueRow(
            "网络用途",
            if (OfficialModelCatalog.find(settings.modelId)?.requiresLocalCache == false) {
                "千问高精度转写"
            } else {
                "下载或更新本地模型"
            },
        )
        SettingValueRow(
            "音视频上传",
            if (OfficialModelCatalog.find(settings.modelId)?.requiresLocalCache == false) {
                "仅当前高精度模式发送音频片段至千问"
            } else {
                "本地模型模式不会上传"
            },
        )
        SettingValueRow("App 版本", BuildConfig.VERSION_NAME)
        SettingValueRow("本机引擎", state.nativeStatus)
    }
}

@Composable
private fun SettingValueRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            modifier = Modifier.padding(start = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun modelStateText(state: ModelInstallState): String = when (state) {
    ModelInstallState.READY -> "已校验，可长期复用"
    ModelInstallState.DOWNLOADING -> "正在下载"
    ModelInstallState.PAUSED -> "下载已暂停，可继续"
    ModelInstallState.VERIFYING -> "正在校验"
    ModelInstallState.CORRUPT -> "缓存不完整，需重新下载"
    ModelInstallState.FAILED -> "下载失败，可重试"
    ModelInstallState.NOT_INSTALLED -> "尚未下载"
}

private fun threadLabel(count: Int): String = if (count == 0) "自动（推荐）" else "$count 线程"

private enum class SettingsModule {
    APPEARANCE,
    MODEL,
    MODEL_DETAILS,
    TRANSCRIPTION,
    OUTPUT,
    PRIVACY,
}

private enum class LanguageOption(val label: String, val code: String?) {
    AUTO("自动识别", null),
    CHINESE("中文", "zh"),
    ENGLISH("英语", "en"),
}

private const val LANGUAGE_AUTO_KEY = "auto"

private fun conflictPolicyLabel(policy: OutputConflictPolicy): String = when (policy) {
    OutputConflictPolicy.RENAME -> "自动重命名（推荐）"
    OutputConflictPolicy.OVERWRITE -> "覆盖已有文件"
    OutputConflictPolicy.SKIP -> "跳过已有文件"
}

private fun outputDirectoryLabel(uriString: String): String = Uri.parse(uriString)
    .lastPathSegment
    ?.substringAfterLast(':')
    ?.ifBlank { "已授权目录" }
    ?: "已授权目录"

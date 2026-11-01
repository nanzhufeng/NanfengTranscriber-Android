package com.nanzhufeng.transcriber.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nanzhufeng.transcriber.BuildConfig
import com.nanzhufeng.transcriber.data.modelstore.OfficialModelCatalog
import com.nanzhufeng.transcriber.data.settings.TranscriptionSettings
import com.nanzhufeng.transcriber.data.task.OutputConflictPolicy
import com.nanzhufeng.transcriber.domain.invocation.AsrInvocationRecord
import com.nanzhufeng.transcriber.domain.model.ModelInstallState
import com.nanzhufeng.transcriber.ui.TranscriptionUiState
import com.nanzhufeng.transcriber.ui.components.SectionHeading
import com.nanzhufeng.transcriber.ui.components.SettingsPageHeader
import com.nanzhufeng.transcriber.ui.components.StatusPill
import com.nanzhufeng.transcriber.ui.components.SubtleActionButton
import com.nanzhufeng.transcriber.ui.components.WorkbenchDropdownMenu
import com.nanzhufeng.transcriber.ui.components.WorkbenchAlertDialog
import com.nanzhufeng.transcriber.ui.components.WorkbenchCard
import com.nanzhufeng.transcriber.ui.components.formatBytes
import com.nanzhufeng.transcriber.ui.theme.ActionOrange
import com.nanzhufeng.transcriber.ui.theme.AttentionOchre
import com.nanzhufeng.transcriber.ui.theme.ParameterPurple
import com.nanzhufeng.transcriber.ui.theme.NanfengSkinCatalog
import com.nanzhufeng.transcriber.ui.theme.NanfengSkinOption
import com.nanzhufeng.transcriber.ui.theme.NanfengTranscriberTheme
import com.nanzhufeng.transcriber.ui.theme.SettingsPageBackground
import com.nanzhufeng.transcriber.ui.components.workbenchTextFieldColors
import com.nanzhufeng.transcriber.ui.theme.WorkbenchControlBackground

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
    var selectedModuleName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedModule = SettingsModule.entries.firstOrNull { it.name == selectedModuleName }
    val rootListState = rememberLazyListState()
    val onBack = { selectedModuleName = null }
    if (selectedModule == SettingsModule.INVOCATIONS) {
        AsrInvocationHistoryScreen(records = invocationRecords, onBack = onBack)
        return
    }
    BackHandler(enabled = selectedModule != null, onBack = onBack)
    Column(
        modifier = Modifier.fillMaxSize().background(SettingsPageBackground).padding(top = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SettingsPageHeader(
            title = selectedModule?.label ?: "设置",
            onBack = if (selectedModule == null) null else onBack,
        )
        if (selectedModule == null) {
            LazyColumn(
                modifier = Modifier.widthIn(max = if (expanded) 840.dp else 600.dp).fillMaxSize(),
                state = rootListState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "skin") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsGroupHeading("皮肤")
                        AppearanceCard(settings, onSkinChanged)
                    }
                }
                items(SettingsGroup.entries, key = { it.name }) { group ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SettingsGroupHeading(group.title)
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                        ) {
                            Column {
                                group.modules.forEach { module ->
                                    SettingsMenuEntry(
                                        module = module,
                                        onClick = { selectedModuleName = module.name },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            key(selectedModule) {
                LazyColumn(
                    modifier = Modifier.widthIn(max = 840.dp).fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    item {
                        SettingsModuleCard(
                            module = selectedModule,
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
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsGroupHeading(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        color = Color(0xFF666666),
    )
}

@Composable
private fun SettingsMenuEntry(
    module: SettingsModule,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.heightIn(min = 64.dp).padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(module.icon, contentDescription = null, tint = Color(0xFF555555))
            Text(
                text = module.label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF242424),
            )
            Icon(Icons.Outlined.ChevronRight, contentDescription = null,
                tint = Color(0xFF777777))
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
    modifier: Modifier = Modifier,
) {
    when (module) {
        SettingsModule.MODEL -> Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ModelSelectionCard(
                selectedModelId = settings.modelId,
                isBusy = state.isBusy,
                onModelChanged = onModelChanged,
                modifier = Modifier.fillMaxWidth(),
            )
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
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SettingsModule.TRANSCRIPTION -> TranscriptionSettingsCard(
            settings = settings,
            onLanguageChanged = onLanguageChanged,
            onThreadCountChanged = onThreadCountChanged,
            modifier = modifier,
        )
        SettingsModule.OUTPUT -> OutputSettingsCard(
            settings = settings,
            onChooseOutputDirectory = onChooseOutputDirectory,
            onClearOutputDirectory = onClearOutputDirectory,
            onConflictPolicyChanged = onConflictPolicyChanged,
            modifier = modifier,
        )
        SettingsModule.ISLAND -> BackgroundIslandSettingsCard(modifier)
        SettingsModule.ABOUT -> AboutSection(modifier)
        SettingsModule.INVOCATIONS -> Unit // 独立记录页由 SettingsScreen 路由。
    }
}

@Composable
private fun HighAccuracyServiceCard(
    settings: TranscriptionSettings,
    state: TranscriptionUiState,
    onSaveApiKey: (String) -> Unit,
    onRevealApiKey: () -> Unit,
    modifier: Modifier = Modifier,
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
    WorkbenchCard(modifier = modifier.fillMaxWidth()) {
        Spacer(Modifier.height(2.dp))
        SettingValueRow("转写模型", if (OfficialModelCatalog.resolveSelectionId(settings.modelId) == "paraformer-v2")
            "Paraformer-v2" else "Qwen Audio 3.1 Filetrans")
        Text("沿用百炼 API Key；音频上传到百炼临时空间，最长保留 48 小时，转写可能产生费用。",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextField(
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
            shape = RoundedCornerShape(14.dp),
            colors = workbenchTextFieldColors(),
            visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
            label = { Text("百炼 API Key") },
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
    modifier: Modifier = Modifier,
) {
    WorkbenchCard(modifier = modifier.fillMaxWidth()) {
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
    modifier: Modifier = Modifier,
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
            modifier = modifier,
        )
    } else {
        HighAccuracyServiceCard(
            settings = settings,
            state = state,
            onSaveApiKey = onSaveApiKey,
            onRevealApiKey = onRevealApiKey,
            modifier = modifier,
        )
    }
}

@Composable
private fun OutputSettingsCard(
    settings: TranscriptionSettings,
    onChooseOutputDirectory: () -> Unit,
    onClearOutputDirectory: () -> Unit,
    onConflictPolicyChanged: (OutputConflictPolicy) -> Unit,
    modifier: Modifier = Modifier,
) {
    WorkbenchCard(modifier = modifier.fillMaxWidth()) {
        SectionHeading(
            title = "输出与文件",
            color = ParameterPurple,
        )
        Spacer(Modifier.height(10.dp))
        SettingValueRow(
            "默认目录",
            settings.defaultOutputDirectoryUri?.let(::outputDirectoryLabel) ?: "未设置（导出前请先设置）",
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
            ) { Text("清除默认输出目录") }
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
    modifier: Modifier = Modifier,
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    if (showDeleteConfirmation) {
        WorkbenchAlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            containerColor = Color.White,
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
    WorkbenchCard(modifier = modifier.fillMaxWidth()) {
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
            SubtleActionButton(
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
        onClick = onClick,
        enabled = enabled,
        selected = selected,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.RadioButton }
            .padding(top = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else WorkbenchControlBackground,
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
    modifier: Modifier = Modifier,
) {
    WorkbenchCard(modifier = modifier.fillMaxWidth()) {
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
        TextField(
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
            shape = RoundedCornerShape(14.dp),
            colors = workbenchTextFieldColors(),
        )
        WorkbenchDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (value, optionLabel) ->
                DropdownMenuItem(
                    modifier = Modifier.background(
                        if (value == selectedValue) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    ),
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
    modifier: Modifier = Modifier,
) {
    var showSkinPicker by rememberSaveable { mutableStateOf(false) }
    if (showSkinPicker) {
        WorkbenchAlertDialog(
            onDismissRequest = { showSkinPicker = false },
            containerColor = Color.White,
            title = { Text("选择皮肤") },
            text = {
                Column {
                    NanfengSkinCatalog.options.forEach { skin ->
                        SkinChoiceRow(
                            skin = skin,
                            selected = settings.skinId == skin.id,
                            onClick = {
                                if (settings.skinId != skin.id) onSkinChanged(skin.id)
                                showSkinPicker = false
                            },
                        )
                    }
                }
            },
            confirmButton = {},
        )
    }
    WorkbenchCard(modifier = modifier.fillMaxWidth()) {
        Surface(
            onClick = { showSkinPicker = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Color.Transparent,
        ) {
            Row(
                modifier = Modifier.heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = NanfengSkinCatalog.nameOf(settings.skinId),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Icon(Icons.Outlined.ChevronRight, contentDescription = "选择皮肤")
            }
        }
    }
}

@Composable
private fun SkinChoiceRow(skin: NanfengSkinOption, selected: Boolean, onClick: () -> Unit) {
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).semantics { role = Role.RadioButton },
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else WorkbenchControlBackground,
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NanfengTranscriberTheme(skinId = skin.id) { SkinEffectPreview() }
            Text(
                skin.displayName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            )
            RadioButton(selected = selected, onClick = null)
        }
    }
}

/** 直接消费各皮肤的真实令牌，不使用与当前界面不符的装饰图。 */
@Composable
private fun SkinEffectPreview() {
    Column(
        modifier = Modifier.size(width = 76.dp, height = 58.dp).clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.background).padding(5.dp).clearAndSetSemantics {},
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Surface(color = Color.White, shape = RoundedCornerShape(5.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("转写", fontSize = 8.sp, lineHeight = 10.sp)
                Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(50)) {
                    Text("开始", modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onPrimary, fontSize = 6.sp, lineHeight = 8.sp)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primaryContainer))
            Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(50)).background(Color.White))
            Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(50)).background(Color.White))
        }
    }
}

@Composable
private fun AboutSection(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        WorkbenchCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        ) {
            SelectionContainer {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "南枫转写 Android",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "本地优先的音频、视频转写与明确导出。",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(8.dp).fillMaxWidth().background(SettingsPageBackground))
            SelectionContainer {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "版本信息",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    AboutInfoLine("Android 版本", BuildConfig.VERSION_NAME)
                    AboutInfoLine("构建时间", BuildConfig.BUILD_TIME)
                }
            }
            Spacer(Modifier.height(8.dp).fillMaxWidth().background(SettingsPageBackground))
            SelectionContainer {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "开发者信息",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    AboutInfoLine("开发者", "席瑞")
                    AboutInfoLine("联系邮箱", "nanzhufeng.studio@gmail.com")
                    AboutLinkInfoLine(
                        label = "源码与更新",
                        value = "GitHub · nanzhufeng/NanfengTranscriber-Android",
                        url = "https://github.com/nanzhufeng/NanfengTranscriber-Android",
                    )
                    AboutInfoLine("版权所有", "© 2026 席瑞")
                }
            }
        }
    }
}

@Composable
private fun AboutInfoLine(label: String, value: String) {
    val text = buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
            append("$label： ")
        }
        append(value)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 16.sp),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(vertical = 2.dp),
    )
}

@Composable
private fun AboutLinkInfoLine(
    label: String,
    value: String,
    url: String,
) {
    val uriHandler = LocalUriHandler.current
    val openRepository = { uriHandler.openUri(url) }
    val text = buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
            append("$label： ")
        }
        pushStringAnnotation(tag = "URL", annotation = url)
        withStyle(
            SpanStyle(
                color = ActionOrange,
                textDecoration = TextDecoration.Underline,
            ),
        ) {
            append(value)
        }
        pop()
    }
    val textMeasurer = rememberTextMeasurer()
    val baseStyle = MaterialTheme.typography.bodySmall.copy(
        fontSize = 11.sp,
        lineHeight = 16.sp,
        color = MaterialTheme.colorScheme.onSurface,
    )
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val naturalWidth = textMeasurer.measure(text, style = baseStyle, softWrap = false, maxLines = 1).size.width
        val scale = ((constraints.maxWidth - 2).coerceAtLeast(1).toFloat() / naturalWidth.coerceAtLeast(1))
            .coerceAtMost(1f)
        ClickableText(
            text = text,
            style = baseStyle.copy(fontSize = baseStyle.fontSize * scale, lineHeight = baseStyle.lineHeight * scale),
            softWrap = false,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).semantics {
                onClick(label = "打开 GitHub 仓库") {
                    openRepository()
                    true
                }
            },
            onClick = { offset ->
                text.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { openRepository() }
            },
        )
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

private enum class SettingsModule(val label: String, val icon: ImageVector) {
    MODEL("转写模型", Icons.Outlined.Memory),
    TRANSCRIPTION("转写参数", Icons.Outlined.Tune),
    OUTPUT("输出与文件", Icons.Outlined.FolderOpen),
    ISLAND("灵动岛", Icons.Outlined.Notifications),
    INVOCATIONS("调用记录", Icons.Outlined.History),
    ABOUT("关于", Icons.Outlined.Info),
}

private enum class SettingsGroup(val title: String, val modules: List<SettingsModule>) {
    APPEARANCE("提醒", listOf(SettingsModule.ISLAND)),
    TRANSCRIPTION("转写与文件", listOf(SettingsModule.MODEL, SettingsModule.INVOCATIONS, SettingsModule.TRANSCRIPTION, SettingsModule.OUTPUT)),
    INFORMATION("关于", listOf(SettingsModule.ABOUT)),
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

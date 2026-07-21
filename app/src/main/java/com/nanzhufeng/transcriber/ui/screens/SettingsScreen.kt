package com.nanzhufeng.transcriber.ui.screens

import android.net.Uri
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.nanzhufeng.transcriber.BuildConfig
import com.nanzhufeng.transcriber.data.modelstore.OfficialModelCatalog
import com.nanzhufeng.transcriber.data.settings.TranscriptionSettings
import com.nanzhufeng.transcriber.data.task.OutputConflictPolicy
import com.nanzhufeng.transcriber.domain.model.ModelInstallState
import com.nanzhufeng.transcriber.ui.TranscriptionUiState
import com.nanzhufeng.transcriber.ui.components.SectionHeading
import com.nanzhufeng.transcriber.ui.components.StatusPill
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
    onKeepScreenOnChanged: (Boolean) -> Unit,
    onChooseOutputDirectory: () -> Unit,
    onClearOutputDirectory: () -> Unit,
    onConflictPolicyChanged: (OutputConflictPolicy) -> Unit,
    onPostProcessEnabledChanged: (Boolean) -> Unit,
    onSavePostProcessConnection: (String, String) -> Unit,
    onSavePostProcessApiKey: (String) -> Unit,
    onClearPostProcessApiKey: () -> Unit,
    onSkinChanged: (String) -> Unit,
) {
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
                        onKeepScreenOnChanged = onKeepScreenOnChanged,
                        onChooseOutputDirectory = onChooseOutputDirectory,
                        onClearOutputDirectory = onClearOutputDirectory,
                        onConflictPolicyChanged = onConflictPolicyChanged,
                        onPostProcessEnabledChanged = onPostProcessEnabledChanged,
                        onSavePostProcessConnection = onSavePostProcessConnection,
                        onSavePostProcessApiKey = onSavePostProcessApiKey,
                        onClearPostProcessApiKey = onClearPostProcessApiKey,
                        onSkinChanged = onSkinChanged,
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
                        onKeepScreenOnChanged = onKeepScreenOnChanged,
                        onChooseOutputDirectory = onChooseOutputDirectory,
                        onClearOutputDirectory = onClearOutputDirectory,
                        onConflictPolicyChanged = onConflictPolicyChanged,
                        onPostProcessEnabledChanged = onPostProcessEnabledChanged,
                        onSavePostProcessConnection = onSavePostProcessConnection,
                        onSavePostProcessApiKey = onSavePostProcessApiKey,
                        onClearPostProcessApiKey = onClearPostProcessApiKey,
                        onSkinChanged = onSkinChanged,
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
    onKeepScreenOnChanged: (Boolean) -> Unit,
    onChooseOutputDirectory: () -> Unit,
    onClearOutputDirectory: () -> Unit,
    onConflictPolicyChanged: (OutputConflictPolicy) -> Unit,
    onPostProcessEnabledChanged: (Boolean) -> Unit,
    onSavePostProcessConnection: (String, String) -> Unit,
    onSavePostProcessApiKey: (String) -> Unit,
    onClearPostProcessApiKey: () -> Unit,
    onSkinChanged: (String) -> Unit,
) {
    when (module) {
        SettingsModule.MODEL -> ModelAndStorageCard(
            state = state,
            selectedModelId = settings.modelId,
            onDownloadModel = onDownloadModel,
            onImportModel = onImportModel,
            onExportModel = onExportModel,
            onVerifyModel = onVerifyModel,
            onDeleteModel = onDeleteModel,
            onModelChanged = onModelChanged,
        )
        SettingsModule.TRANSCRIPTION -> TranscriptionSettingsCard(
            settings = settings,
            onLanguageChanged = onLanguageChanged,
            onThreadCountChanged = onThreadCountChanged,
            onKeepScreenOnChanged = onKeepScreenOnChanged,
        )
        SettingsModule.OUTPUT -> OutputSettingsCard(
            settings = settings,
            onChooseOutputDirectory = onChooseOutputDirectory,
            onClearOutputDirectory = onClearOutputDirectory,
            onConflictPolicyChanged = onConflictPolicyChanged,
        )
        SettingsModule.POST_PROCESSING -> PostProcessSettingsCard(
            settings = settings,
            onEnabledChanged = onPostProcessEnabledChanged,
            onSaveConnection = onSavePostProcessConnection,
            onSaveApiKey = onSavePostProcessApiKey,
            onClearApiKey = onClearPostProcessApiKey,
        )
        SettingsModule.APPEARANCE -> AppearanceCard(settings, onSkinChanged)
        SettingsModule.PRIVACY -> PrivacyCard(state, settings)
    }
}

@Composable
private fun PostProcessSettingsCard(
    settings: TranscriptionSettings,
    onEnabledChanged: (Boolean) -> Unit,
    onSaveConnection: (String, String) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onClearApiKey: () -> Unit,
) {
    var baseUrl by remember(settings.postProcessBaseUrl) { mutableStateOf(settings.postProcessBaseUrl) }
    var model by remember(settings.postProcessModel) { mutableStateOf(settings.postProcessModel) }
    var apiKey by remember { mutableStateOf("") }
    WorkbenchCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeading(
            title = "翻译与润色",
            color = AttentionOchre,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "完成后整理为现代简体中文",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.SemiBold,
            )
            Switch(
                checked = settings.postProcessEnabled,
                onCheckedChange = onEnabledChanged,
                enabled = settings.postProcessApiKeyConfigured,
            )
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("兼容服务地址（HTTPS）") },
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("服务模型名") },
        )
        OutlinedButton(
            onClick = { onSaveConnection(baseUrl, model) },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text("保存接口设置") }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            label = { Text(if (settings.postProcessApiKeyConfigured) "输入新 API Key 以替换" else "API Key") },
            placeholder = {
                if (settings.postProcessApiKeyConfigured) Text("已加密保存，界面不会回显")
            },
        )
        Button(
            onClick = {
                onSaveApiKey(apiKey)
                apiKey = ""
            },
            enabled = apiKey.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) { Text(if (settings.postProcessApiKeyConfigured) "替换 API Key" else "加密保存 API Key") }
        if (settings.postProcessApiKeyConfigured) {
            TextButton(onClick = onClearApiKey, modifier = Modifier.align(Alignment.End)) {
                Text("删除本机 API Key", color = MaterialTheme.colorScheme.error)
            }
        }
        SettingValueRow("凭据保护", "Android Keystore")
        SettingValueRow("调用费用", "由所选服务平台收取")
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
private fun ModelAndStorageCard(
    state: TranscriptionUiState,
    selectedModelId: String,
    onDownloadModel: () -> Unit,
    onImportModel: () -> Unit,
    onExportModel: () -> Unit,
    onVerifyModel: () -> Unit,
    onDeleteModel: () -> Unit,
    onModelChanged: (String) -> Unit,
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
                    title = "模型与缓存",
                )
            }
            StatusPill(
                text = if (state.modelState == ModelInstallState.READY) "已就绪" else "待准备",
                color = if (state.modelState == ModelInstallState.READY) MaterialTheme.colorScheme.primary else AttentionOchre,
            )
        }
        Spacer(Modifier.height(12.dp))
        SelectionField(
            label = "转写模型",
            selectedValue = selectedModelId,
            selectedLabel = OfficialModelCatalog.find(selectedModelId)?.displayName
                ?: OfficialModelCatalog.candidates.first().displayName,
            options = OfficialModelCatalog.candidates.map { it.manifest.modelId to it.displayName },
            enabled = !state.isBusy,
            onSelected = onModelChanged,
        )
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
            OutlinedButton(
                onClick = onImportModel,
                enabled = !state.isBusy && state.modelState != ModelInstallState.READY,
                modifier = Modifier.weight(1f),
            ) { Text("导入备份") }
            OutlinedButton(
                onClick = onVerifyModel,
                enabled = !state.isBusy && state.modelState == ModelInstallState.READY,
                modifier = Modifier.weight(1f),
            ) { Text("完整校验") }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
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
private fun TranscriptionSettingsCard(
    settings: TranscriptionSettings,
    onLanguageChanged: (String?) -> Unit,
    onThreadCountChanged: (Int) -> Unit,
    onKeepScreenOnChanged: (Boolean) -> Unit,
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
        SettingValueRow("推理后端", "CPU 稳定模式")
        SettingValueRow("GPU 加速", "暂不可用")
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "转写时保持屏幕常亮",
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.SemiBold,
            )
            Switch(
                checked = settings.keepScreenOn,
                onCheckedChange = onKeepScreenOnChanged,
            )
        }
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
            if (settings.postProcessEnabled) "下载模型；发送转写文字用于润色" else "仅下载所选模型",
        )
        SettingValueRow("音视频上传", "不会上传")
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

private enum class SettingsModule { APPEARANCE, MODEL, TRANSCRIPTION, OUTPUT, POST_PROCESSING, PRIVACY }

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

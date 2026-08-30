package com.nanzhufeng.transcriber.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nanzhufeng.transcriber.domain.invocation.AsrInvocationRecord
import com.nanzhufeng.transcriber.domain.invocation.AsrInvocationStatus
import com.nanzhufeng.transcriber.ui.components.SectionHeading
import com.nanzhufeng.transcriber.ui.components.WorkbenchCard
import java.text.SimpleDateFormat
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Date
import java.util.Locale

/** Mirrors 南枫 AI's content-free invocation ledger, grouped by complete source file. */
@Composable
fun AsrInvocationLedgerEntry(records: List<AsrInvocationRecord>, onOpen: () -> Unit) {
    val fileCount = records.distinctBy(AsrInvocationRecord::fileGroupKey).size
    WorkbenchCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeading(title = "调用记录")
        Spacer(Modifier.height(6.dp))
        Text(
            "按完整文件记录千问转写；后台分段不会单独显示。不会保存 API Key、音频或转写内容。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedButton(
            onClick = onOpen,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) {
            Icon(Icons.AutoMirrored.Outlined.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(if (fileCount == 0) "查看调用记录" else "查看调用记录（$fileCount）")
        }
    }
}

@Composable
fun AsrInvocationHistoryScreen(records: List<AsrInvocationRecord>, onBack: () -> Unit) {
    val fileRecords = records.toFileInvocationGroups()
    BackHandler(onBack = onBack)
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("调用记录", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onBack) { Text("返回设置") }
            }
            Text(
                "仅显示本机安全运行元数据。",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
            )
            when {
                fileRecords.isEmpty() -> EmptyAsrInvocationLedger()
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    items(fileRecords, key = FileInvocationGroup::key) { record ->
                        AsrInvocationLedgerRow(record)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAsrInvocationLedger() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 56.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.ReceiptLong,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp),
        )
        Text("尚无本地调用记录", fontWeight = FontWeight.Medium)
        Text(
            "当前未发出任何真实千问转写请求。",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun AsrInvocationLedgerRow(record: FileInvocationGroup) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (record.latest.status == AsrInvocationStatus.SUCCEEDED) "成功" else "失败",
                color = if (record.latest.status == AsrInvocationStatus.SUCCEEDED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            Text(
                formatInvocationTime(record.latest.completedAtMillis),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Text(record.sourceDisplayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Text("千问官方 · Qwen3-ASR", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text(
            "完整文件 · 后台分段 ${record.requestCount} 个 · 耗时 ${formatInvocationDuration(record.durationMillis)} · ${record.usageLabel()}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            record.estimatedCostLabel(),
            color = if (record.estimatedCostMicros == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.tertiary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
        )
        record.latest.errorCode?.let { code ->
            Text(
                "原因：${code.safeErrorLabel()}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun FileInvocationGroup.usageLabel(): String = when {
    totalTokens != null -> "Token：$totalTokens"
    inputTokens != null || outputTokens != null -> "输入 Token：${inputTokens ?: "未提供"} · 输出 Token：${outputTokens ?: "未提供"}"
    else -> "Token：服务未提供"
}

private fun FileInvocationGroup.estimatedCostLabel(): String {
    val micros = estimatedCostMicros ?: return "费用估算：暂无费用数据"
    if (costCurrencyCode != "CNY") return "费用估算：暂无费用数据"
    val yuan = BigDecimal.valueOf(micros)
        .divide(BigDecimal("1000000"))
        .setScale(6, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
    return "费用估算：≈ ¥$yuan（估算）"
}

private data class FileInvocationGroup(
    val key: String,
    val sourceDisplayName: String,
    val latest: AsrInvocationRecord,
    val requestCount: Int,
    val durationMillis: Long,
    val inputTokens: Long?,
    val outputTokens: Long?,
    val totalTokens: Long?,
    val costCurrencyCode: String?,
    val estimatedCostMicros: Long?,
)

private fun AsrInvocationRecord.fileGroupKey(): String = taskId.ifBlank { id }

private fun List<AsrInvocationRecord>.toFileInvocationGroups(): List<FileInvocationGroup> =
    groupBy(AsrInvocationRecord::fileGroupKey)
        .map { (key, attempts) ->
            val latest = attempts.maxBy(AsrInvocationRecord::completedAtMillis)
            FileInvocationGroup(
                key = key,
                sourceDisplayName = latest.sourceDisplayName.ifBlank { "迁移前调用记录" },
                latest = latest,
                requestCount = attempts.sumOf(AsrInvocationRecord::requestCount),
                durationMillis = attempts.sumOf(AsrInvocationRecord::durationMillis),
                inputTokens = attempts.mapNotNull(AsrInvocationRecord::inputTokens).sumOrNull(),
                outputTokens = attempts.mapNotNull(AsrInvocationRecord::outputTokens).sumOrNull(),
                totalTokens = attempts.mapNotNull(AsrInvocationRecord::totalTokens).sumOrNull(),
                costCurrencyCode = attempts.mapNotNull(AsrInvocationRecord::costCurrencyCode)
                    .distinct().singleOrNull(),
                estimatedCostMicros = attempts.mapNotNull(AsrInvocationRecord::estimatedCostMicros).sumOrNull(),
            )
        }
        .sortedByDescending { it.latest.completedAtMillis }

private fun List<Long>.sumOrNull(): Long? = takeIf { it.isNotEmpty() }?.sum()

private fun formatInvocationTime(value: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.SIMPLIFIED_CHINESE).format(Date(value))

private fun formatInvocationDuration(value: Long): String = when {
    value < 1_000L -> "${value} ms"
    else -> "%.2f 秒".format(Locale.ROOT, value / 1_000.0)
}

private fun String.safeErrorLabel(): String = when {
    this == "QWEN_NETWORK_ERROR" -> "网络不可用"
    this == "QWEN_RESPONSE_INVALID" -> "响应格式异常"
    this == "QWEN_EMPTY_TRANSCRIPT" -> "未返回转写文字"
    this == "QWEN_HTTP_401" || this == "QWEN_HTTP_403" -> "鉴权、权限或地域不匹配"
    this == "QWEN_HTTP_429" -> "服务限流或额度不足"
    this.startsWith("QWEN_HTTP_5") -> "服务暂不可用"
    else -> "千问转写请求失败"
}

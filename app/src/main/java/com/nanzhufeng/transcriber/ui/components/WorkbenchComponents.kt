package com.nanzhufeng.transcriber.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nanzhufeng.transcriber.R
import com.nanzhufeng.transcriber.data.task.TranscriptionTaskEntity
import com.nanzhufeng.transcriber.domain.task.TranscriptionTaskState
import com.nanzhufeng.transcriber.ui.WorkflowStage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PageHeader(
    title: String,
    subtitle: String,
    showAppIcon: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showAppIcon) {
            Image(
                painter = painterResource(R.mipmap.app_icon),
                contentDescription = null,
                modifier = Modifier.size(42.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
fun WorkbenchCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

/** Neutral filled action used for secondary controls; hierarchy comes from tone, not outlines. */
@Composable
fun SubtleActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val neutralFill = Color.Black.copy(alpha = 0.055f)
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = neutralFill,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = Color.Black.copy(alpha = 0.03f),
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.36f),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        contentPadding = contentPadding,
        content = content,
    )
}

@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.10f),
        shape = RoundedCornerShape(999.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(color),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun SectionHeading(
    title: String,
    subtitle: String? = null,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = color,
        fontWeight = FontWeight.SemiBold,
    )
    if (subtitle != null) {
        Spacer(Modifier.height(3.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
        )
    }
}

@Composable
fun TaskRow(task: TranscriptionTaskEntity) {
    val state = taskState(task)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            color = taskStateColor(state).copy(alpha = 0.12f),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.AudioFile,
                contentDescription = null,
                tint = taskStateColor(state),
                modifier = Modifier.padding(9.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = task.sourceDisplayName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusPill(
                    text = taskStateLabel(state),
                    color = taskStateColor(state),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = buildString {
                    task.totalDurationMillis?.let { append("${formatDuration(it)} · ") }
                    append(formatDateTime(task.updatedAtMillis))
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            task.userMessage?.takeIf { it.isNotBlank() }?.let { message ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (state in setOf(
                    TranscriptionTaskState.PREPARING,
                    TranscriptionTaskState.TRANSCRIBING,
                    TranscriptionTaskState.EXPORTING,
                )
            ) {
                task.totalDurationMillis?.takeIf { it > 0L }?.let { total ->
                    Spacer(Modifier.height(7.dp))
                    LinearProgressIndicator(
                        progress = {
                            (task.progressMillis.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

fun taskState(task: TranscriptionTaskEntity): TranscriptionTaskState =
    runCatching { TranscriptionTaskState.valueOf(task.state) }
        .getOrDefault(TranscriptionTaskState.FAILED)

fun taskStateLabel(state: TranscriptionTaskState): String = when (state) {
    TranscriptionTaskState.QUEUED -> "等待处理"
    TranscriptionTaskState.WAITING_INPUT -> "等待重新选择文件"
    TranscriptionTaskState.WAITING_MODEL -> "等待模型"
    TranscriptionTaskState.PREPARING -> "准备音轨"
    TranscriptionTaskState.TRANSCRIBING -> "正在转写"
    TranscriptionTaskState.EXPORTING -> "保存结果"
    TranscriptionTaskState.RECOVERY_REQUIRED -> "上次被系统中断"
    TranscriptionTaskState.NO_SPEECH -> "未识别到语音"
    TranscriptionTaskState.COMPLETED -> "已完成"
    TranscriptionTaskState.FAILED -> "未完成"
    TranscriptionTaskState.CANCELLED -> "已取消"
}

fun taskStateColor(state: TranscriptionTaskState): Color = when (state) {
    TranscriptionTaskState.COMPLETED -> Color(0xFF2E7D32)
    TranscriptionTaskState.FAILED,
    TranscriptionTaskState.RECOVERY_REQUIRED,
    -> Color(0xFFB3261E)
    TranscriptionTaskState.NO_SPEECH -> Color(0xFF9B6049)
    TranscriptionTaskState.TRANSCRIBING,
    TranscriptionTaskState.PREPARING,
    TranscriptionTaskState.EXPORTING,
    -> Color(0xFF007F79)
    TranscriptionTaskState.CANCELLED -> Color(0xFF6D6A64)
    else -> Color(0xFFC46A21)
}

fun workflowLabel(stage: WorkflowStage): String = when (stage) {
    WorkflowStage.CHECKING -> "检查本机状态"
    WorkflowStage.IDLE -> "准备就绪"
    WorkflowStage.DOWNLOADING_MODEL -> "正在下载模型"
    WorkflowStage.IMPORTING_MODEL -> "正在导入模型"
    WorkflowStage.VERIFYING_MODEL -> "正在校验模型"
    WorkflowStage.EXPORTING_MODEL -> "正在导出模型"
    WorkflowStage.DECODING -> "正在准备音轨"
    WorkflowStage.LOADING_MODEL -> "正在加载模型"
    WorkflowStage.TRANSCRIBING -> "正在本机转写"
    WorkflowStage.EXPORTING -> "正在保存结果"
    WorkflowStage.COMPLETED -> "转写完成"
    WorkflowStage.ERROR -> "需要处理"
}

fun workflowColor(stage: WorkflowStage): Color = when (stage) {
    WorkflowStage.ERROR -> Color(0xFFB3261E)
    WorkflowStage.COMPLETED -> Color(0xFF2E7D32)
    else -> Color(0xFF007F79)
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1_024L) return "$bytes B"
    val units = listOf("KB", "MB", "GB")
    var value = bytes.toDouble()
    var unitIndex = -1
    while (value >= 1_024.0 && unitIndex < units.lastIndex) {
        value /= 1_024.0
        unitIndex += 1
    }
    return String.format(Locale.US, "%.1f %s", value, units[unitIndex])
}

fun formatDuration(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = totalSeconds % 3_600L / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}

fun formatDateTime(epochMillis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.SIMPLIFIED_CHINESE).format(Date(epochMillis))

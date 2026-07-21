package com.nanzhufeng.transcriber.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nanzhufeng.transcriber.data.media.HistoryMediaKind
import com.nanzhufeng.transcriber.data.media.HistoryMediaPreviewStore
import java.nio.file.Paths
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TaskMediaPreview(
    displayName: String,
    sourceUri: String,
    stagedInputPath: String?,
    modifier: Modifier,
    cacheTaskId: String? = null,
    onClick: (() -> Unit)? = null,
    showPlayOverlay: Boolean = onClick != null,
) {
    val context = LocalContext.current
    val store = remember(context.applicationContext) { HistoryMediaPreviewStore(context.applicationContext) }
    val uri = remember(sourceUri) { Uri.parse(sourceUri) }
    val kind = remember(displayName, sourceUri) {
        val mimeType = runCatching { context.contentResolver.getType(uri) }.getOrNull()
        com.nanzhufeng.transcriber.data.media.classifyHistoryMedia(displayName, mimeType)
    }
    val thumbnail by produceState<Bitmap?>(
        initialValue = null,
        key1 = cacheTaskId,
        key2 = sourceUri,
        key3 = stagedInputPath,
    ) {
        value = withContext(Dispatchers.IO) {
            val stagedPath = stagedInputPath?.let(Paths::get)
            if (cacheTaskId == null) {
                store.loadSourceThumbnail(uri, stagedPath, displayName)
            } else {
                store.loadThumbnail(cacheTaskId) ?: store.ensureVideoThumbnail(
                    taskId = cacheTaskId,
                    sourceUri = uri,
                    stagedSourcePath = stagedPath,
                    displayName = displayName,
                )?.let { store.loadThumbnail(cacheTaskId) }
            }
        }
    }
    val interactiveModifier = if (onClick == null) modifier else modifier.clickable(onClick = onClick)

    Surface(
        modifier = interactiveModifier.clip(MaterialTheme.shapes.medium),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (thumbnail != null) {
                Image(
                    bitmap = requireNotNull(thumbnail).asImageBitmap(),
                    contentDescription = "$displayName 视频预览图",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = if (kind == HistoryMediaKind.AUDIO) Icons.Outlined.MusicNote else Icons.Outlined.Movie,
                    contentDescription = if (kind == HistoryMediaKind.AUDIO) "$displayName 音频" else "$displayName 视频",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
            if (showPlayOverlay) {
                Icon(
                    imageVector = Icons.Filled.PlayCircle,
                    contentDescription = "播放 $displayName 核对文字",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
            }
        }
    }
}

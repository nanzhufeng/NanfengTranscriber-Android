package com.nanzhufeng.transcriber.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.nanzhufeng.transcriber.data.task.TranscriptionTaskEntity
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.math.min
import kotlin.math.roundToInt

enum class HistoryMediaKind {
    VIDEO,
    AUDIO,
    UNKNOWN,
}

class HistoryMediaPreviewStore(private val context: Context) {
    fun kindOf(task: TranscriptionTaskEntity): HistoryMediaKind {
        val mimeType = runCatching {
            context.contentResolver.getType(Uri.parse(task.sourceUri))
        }.getOrNull()
        return classifyHistoryMedia(task.sourceDisplayName, mimeType)
    }

    fun ensureVideoThumbnail(
        taskId: String,
        sourceUri: Uri,
        stagedSourcePath: Path?,
        displayName: String,
    ): Path? {
        if (classifyHistoryMedia(displayName, runCatching {
                context.contentResolver.getType(sourceUri)
            }.getOrNull()) != HistoryMediaKind.VIDEO
        ) {
            return null
        }
        val target = thumbnailPath(taskId)
        if (Files.isRegularFile(target) && Files.size(target) > 0L) return target

        return runCatching {
            Files.createDirectories(target.parent)
            val thumbnail = extractVideoThumbnail(sourceUri, stagedSourcePath, displayName)
                ?: return@runCatching null
            val part = target.resolveSibling("${target.fileName}.part")
            Files.newOutputStream(part).use { output ->
                check(thumbnail.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                    "缩略图编码失败"
                }
            }
            thumbnail.recycle()
            runCatching {
                Files.move(
                    part,
                    target,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }.getOrElse {
                Files.move(part, target, StandardCopyOption.REPLACE_EXISTING)
            }
            target
        }.getOrNull()
    }

    fun loadSourceThumbnail(
        sourceUri: Uri,
        stagedSourcePath: Path?,
        displayName: String,
    ): Bitmap? = extractVideoThumbnail(sourceUri, stagedSourcePath, displayName)

    fun loadThumbnail(taskId: String): Bitmap? {
        val path = thumbnailPath(taskId)
        if (!Files.isRegularFile(path)) return null
        return BitmapFactory.decodeFile(path.toString())
    }

    fun playableUri(task: TranscriptionTaskEntity): Uri? {
        val staged = task.stagedInputPath
            ?.let(Paths::get)
            ?.takeIf(Files::isRegularFile)
        if (staged != null) return Uri.fromFile(staged.toFile())

        val sourceUri = Uri.parse(task.sourceUri)
        return runCatching {
            context.contentResolver.openAssetFileDescriptor(sourceUri, "r")?.use { sourceUri }
        }.getOrNull()
    }

    private fun thumbnailPath(taskId: String): Path =
        context.noBackupFilesDir.resolve("tasks/$taskId/history-preview.jpg").toPath()

    private fun extractVideoThumbnail(
        sourceUri: Uri,
        stagedSourcePath: Path?,
        displayName: String,
    ): Bitmap? {
        if (classifyHistoryMedia(displayName, runCatching {
                context.contentResolver.getType(sourceUri)
            }.getOrNull()) != HistoryMediaKind.VIDEO
        ) {
            return null
        }
        return runCatching {
            val frame = MediaMetadataRetriever().use { retriever ->
                if (stagedSourcePath != null && Files.isRegularFile(stagedSourcePath)) {
                    retriever.setDataSource(stagedSourcePath.toString())
                } else {
                    retriever.setDataSource(context, sourceUri)
                }
                val durationMillis = retriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLongOrNull()
                    ?: 0L
                retriever.getFrameAtTime(
                    (durationMillis / 3L).coerceAtLeast(0L) * 1_000L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                )
            } ?: return@runCatching null
            val thumbnail = frame.scaledWithin(MAX_WIDTH, MAX_HEIGHT)
            if (thumbnail !== frame) frame.recycle()
            thumbnail
        }.getOrNull()
    }

    private fun Bitmap.scaledWithin(maxWidth: Int, maxHeight: Int): Bitmap {
        val scale = min(maxWidth.toFloat() / width, maxHeight.toFloat() / height).coerceAtMost(1f)
        if (scale >= 1f) return this
        return Bitmap.createScaledBitmap(
            this,
            (width * scale).roundToInt().coerceAtLeast(1),
            (height * scale).roundToInt().coerceAtLeast(1),
            true,
        )
    }

    private companion object {
        const val MAX_WIDTH = 480
        const val MAX_HEIGHT = 270
        const val JPEG_QUALITY = 82
    }
}

internal fun classifyHistoryMedia(displayName: String, mimeType: String?): HistoryMediaKind {
    if (mimeType?.startsWith("video/") == true) return HistoryMediaKind.VIDEO
    if (mimeType?.startsWith("audio/") == true) return HistoryMediaKind.AUDIO
    return when (displayName.substringAfterLast('.', missingDelimiterValue = "").lowercase()) {
        "mp4", "m4v", "mov", "mkv", "webm", "avi", "3gp", "ts", "mts", "m2ts" ->
            HistoryMediaKind.VIDEO
        "mp3", "m4a", "aac", "wav", "flac", "ogg", "opus", "amr", "wma" ->
            HistoryMediaKind.AUDIO
        else -> HistoryMediaKind.UNKNOWN
    }
}

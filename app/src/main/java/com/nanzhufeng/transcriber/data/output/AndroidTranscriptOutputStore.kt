package com.nanzhufeng.transcriber.data.output

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import com.nanzhufeng.transcriber.data.task.OutputConflictPolicy
import com.nanzhufeng.transcriber.domain.export.TranscriptDocument
import com.nanzhufeng.transcriber.domain.export.TranscriptExportFormat
import com.nanzhufeng.transcriber.domain.export.TranscriptExportService
import java.io.IOException

class AndroidTranscriptOutputStore(
    private val resolver: ContentResolver,
    private val exportService: TranscriptExportService = TranscriptExportService(),
) {
    fun export(
        treeUri: Uri,
        sourceDisplayName: String,
        document: TranscriptDocument,
        format: TranscriptExportFormat,
        conflictPolicy: OutputConflictPolicy,
    ): AutomaticExportResult {
        val safeBase = safeBaseName(sourceDisplayName)
        // ColorOS documents providers reject createDocument() when the raw tree URI is passed as
        // a parent. Resolve its document form first, then write directly into the user-selected
        // folder. This also fulfils the export contract: no surprise task subfolder or second UI.
        val outputDirectory = rootDocumentUri(treeUri)

        val extension = format.extension
        val requestedName = "$safeBase.$extension"
        val existing = findChild(outputDirectory, requestedName, directory = false)
        val target = when {
            existing == null -> createFile(outputDirectory, requestedName, format.mimeType)
            conflictPolicy == OutputConflictPolicy.SKIP -> {
                return AutomaticExportResult.Skipped(existing, "目标文件已存在，已按设置跳过")
            }
            conflictPolicy == OutputConflictPolicy.OVERWRITE -> existing
            else -> createFile(outputDirectory, nextAvailableName(outputDirectory, safeBase, extension), format.mimeType)
        }
        resolver.openOutputStream(target, "wt")?.use { output ->
            exportService.export(document, format, output)
        } ?: throw IOException("系统未能写入默认输出目录")
        return AutomaticExportResult.Written(target, "已保存到默认输出目录")
    }

    private fun nextAvailableName(parent: Uri, base: String, extension: String): String {
        for (index in 2..9_999) {
            val candidate = "$base ($index).$extension"
            if (findChild(parent, candidate, directory = false) == null) return candidate
        }
        throw IOException("同名结果过多，请更换默认输出目录")
    }

    private fun createFile(parent: Uri, name: String, mimeType: String): Uri =
        DocumentsContract.createDocument(resolver, parent, mimeType, name)
            ?: throw IOException("系统未能创建输出文件")

    private fun findChild(parent: Uri, displayName: String, directory: Boolean): Uri? {
        val parentId = if (DocumentsContract.isTreeUri(parent)) {
            runCatching { DocumentsContract.getDocumentId(parent) }
                .getOrElse { DocumentsContract.getTreeDocumentId(parent) }
        } else {
            DocumentsContract.getDocumentId(parent)
        }
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(parent, parentId)
        resolver.query(
            children,
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
            while (cursor.moveToNext()) {
                val sameType = (cursor.getString(mimeColumn) == DocumentsContract.Document.MIME_TYPE_DIR) == directory
                if (sameType && cursor.getString(nameColumn) == displayName) {
                    return DocumentsContract.buildDocumentUriUsingTree(parent, cursor.getString(idColumn))
                }
            }
        }
        return null
    }

    private fun safeBaseName(sourceDisplayName: String): String {
        val raw = sourceDisplayName.substringBeforeLast('.').trim().ifBlank { "南枫转写结果" }
        return raw.replace(INVALID_FILE_CHARACTERS, "_").trim('.', ' ').take(72)
            .ifBlank { "南枫转写结果" }
    }

    companion object {
        /** Keeps the tree permission while giving DocumentsContract a valid parent document URI. */
        internal fun rootDocumentUri(treeUri: Uri): Uri = if (DocumentsContract.isTreeUri(treeUri)) {
            DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
        } else {
            treeUri
        }
    }
}

sealed interface AutomaticExportResult {
    val uri: Uri
    val message: String

    data class Written(override val uri: Uri, override val message: String) : AutomaticExportResult
    data class Skipped(override val uri: Uri, override val message: String) : AutomaticExportResult
}

private val INVALID_FILE_CHARACTERS = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")

private val TranscriptExportFormat.extension: String
    get() = when (this) {
        TranscriptExportFormat.TXT -> "txt"
        TranscriptExportFormat.MARKDOWN -> "md"
        TranscriptExportFormat.SRT -> "srt"
        TranscriptExportFormat.DOCX -> "docx"
    }

private val TranscriptExportFormat.mimeType: String
    get() = when (this) {
        TranscriptExportFormat.TXT -> "text/plain"
        TranscriptExportFormat.MARKDOWN -> "text/markdown"
        TranscriptExportFormat.SRT -> "application/x-subrip"
        TranscriptExportFormat.DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    }

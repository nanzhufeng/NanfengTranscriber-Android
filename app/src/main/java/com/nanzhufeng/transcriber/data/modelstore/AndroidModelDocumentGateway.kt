package com.nanzhufeng.transcriber.data.modelstore

import android.content.ContentResolver
import android.net.Uri
import java.io.IOException

class AndroidModelDocumentGateway(
    private val contentResolver: ContentResolver,
    private val modelAssets: ModelAssetManager,
) {
    suspend fun importFromDocument(
        manifest: ModelManifest,
        documentUri: Uri,
        onProgress: (ModelTransferProgress) -> Unit = {},
    ): ModelAssetResult = modelAssets.importModel(
        manifest = manifest,
        inputProvider = {
            contentResolver.openInputStream(documentUri)
                ?: throw IOException("系统未能打开所选模型文件")
        },
        onProgress = onProgress,
    )

    suspend fun exportToDocument(
        manifest: ModelManifest,
        documentUri: Uri,
        onProgress: (ModelTransferProgress) -> Unit = {},
    ): ModelAssetResult = modelAssets.exportModel(
        manifest = manifest,
        outputProvider = {
            contentResolver.openOutputStream(documentUri, "wt")
                ?: throw IOException("系统未能创建模型导出文件")
        },
        onProgress = onProgress,
    )
}

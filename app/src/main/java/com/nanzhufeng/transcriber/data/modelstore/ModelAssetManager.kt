package com.nanzhufeng.transcriber.data.modelstore

import com.nanzhufeng.transcriber.domain.model.ModelInstallState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.coroutines.coroutineContext

class ModelAssetManager(
    private val store: FileModelStore,
    private val httpClient: OkHttpClient,
) {
    private val transferMutex = Mutex()

    suspend fun download(
        manifest: ModelManifest,
        url: String,
        onProgress: (ModelTransferProgress) -> Unit = {},
    ): ModelAssetResult = serializedTransfer {
        withContext(Dispatchers.IO) {
            val ready = store.inspect(manifest)
            if (ready.state == ModelInstallState.READY) {
                return@withContext ModelAssetResult.AlreadyReady(
                    modelPath = requireNotNull(ready.modelPath),
                    message = "模型已存在，无需重复下载",
                )
            }

            val partial = store.partialPath(manifest)
            var existingBytes = Files.size(partial)
            if (existingBytes == manifest.expectedBytes) {
                return@withContext store.commit(manifest).toAssetResult(ModelAssetSource.DOWNLOAD)
            }
            if (existingBytes > manifest.expectedBytes) {
                truncate(partial)
                existingBytes = 0L
            }

            onProgress(
                ModelTransferProgress(
                    stage = ModelTransferStage.DOWNLOADING,
                    completedBytes = existingBytes,
                    totalBytes = manifest.expectedBytes,
                ),
            )

            val request = Request.Builder()
                .url(url)
                .apply {
                    if (existingBytes > 0L) header("Range", "bytes=$existingBytes-")
                }
                .build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext ModelAssetResult.Failure(
                            category = ModelAssetFailureCategory.SERVER,
                            message = "模型服务器返回 ${response.code}，已保留下载进度",
                            canRetry = response.code >= 500 || response.code == 408 || response.code == 429,
                        )
                    }

                    val body = response.body ?: return@withContext ModelAssetResult.Failure(
                        category = ModelAssetFailureCategory.NETWORK,
                        message = "模型服务器没有返回文件内容，已保留下载进度",
                        canRetry = true,
                    )
                    val append = when {
                        existingBytes == 0L && response.code == 200 -> false
                        existingBytes == 0L && response.code == 206 ->
                            response.contentRangeStart() == 0L
                        existingBytes > 0L && response.code == 206 ->
                            response.contentRangeStart() == existingBytes
                        existingBytes > 0L && response.code == 200 -> false
                        else -> false
                    }
                    if (response.code == 206 && !append) {
                        return@withContext ModelAssetResult.Failure(
                            category = ModelAssetFailureCategory.SERVER,
                            message = "模型服务器返回的断点位置不一致，未写入异常数据",
                            canRetry = true,
                        )
                    }
                    if (existingBytes > 0L && response.code == 200) existingBytes = 0L

                    writeResponseBody(
                        target = partial,
                        input = body.byteStream(),
                        append = append,
                        initialBytes = existingBytes,
                        totalBytes = manifest.expectedBytes,
                        onProgress = onProgress,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: IOException) {
                return@withContext ModelAssetResult.Failure(
                    category = ModelAssetFailureCategory.NETWORK,
                    message = "模型下载中断，已保留进度，可稍后继续",
                    canRetry = true,
                    technicalDetail = error.message,
                )
            }

            if (Files.size(partial) != manifest.expectedBytes) {
                return@withContext ModelAssetResult.Failure(
                    category = ModelAssetFailureCategory.INTEGRITY,
                    message = "模型文件长度不完整，已保留进度，可继续下载",
                    canRetry = true,
                )
            }
            onProgress(
                ModelTransferProgress(
                    stage = ModelTransferStage.VERIFYING,
                    completedBytes = manifest.expectedBytes,
                    totalBytes = manifest.expectedBytes,
                ),
            )
            store.commit(manifest).toAssetResult(ModelAssetSource.DOWNLOAD)
        }
    }

    suspend fun importModel(
        manifest: ModelManifest,
        inputProvider: () -> InputStream,
        onProgress: (ModelTransferProgress) -> Unit = {},
    ): ModelAssetResult = serializedTransfer {
        withContext(Dispatchers.IO) {
            val ready = store.inspect(manifest)
            if (ready.state == ModelInstallState.READY) {
                return@withContext ModelAssetResult.AlreadyReady(
                    modelPath = requireNotNull(ready.modelPath),
                    message = "模型已存在，无需重复导入",
                )
            }

            val candidate = store.importPath(manifest)
            try {
                inputProvider().use { input ->
                    writeResponseBody(
                        target = candidate,
                        input = input,
                        append = false,
                        initialBytes = 0L,
                        totalBytes = manifest.expectedBytes,
                        stage = ModelTransferStage.IMPORTING,
                        onProgress = onProgress,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: IOException) {
                return@withContext ModelAssetResult.Failure(
                    category = ModelAssetFailureCategory.STORAGE,
                    message = "读取导入文件失败，请重新选择模型文件",
                    canRetry = true,
                    technicalDetail = error.message,
                )
            }
            onProgress(
                ModelTransferProgress(
                    stage = ModelTransferStage.VERIFYING,
                    completedBytes = Files.size(candidate),
                    totalBytes = manifest.expectedBytes,
                ),
            )
            store.commitImport(manifest).toAssetResult(ModelAssetSource.IMPORT)
        }
    }

    suspend fun exportModel(
        manifest: ModelManifest,
        outputProvider: () -> OutputStream,
        onProgress: (ModelTransferProgress) -> Unit = {},
    ): ModelAssetResult = serializedTransfer {
        withContext(Dispatchers.IO) {
            val inspection = store.inspect(manifest, fullHash = true)
            if (inspection.state != ModelInstallState.READY) {
                return@withContext ModelAssetResult.Failure(
                    category = ModelAssetFailureCategory.INTEGRITY,
                    message = "模型未就绪或校验失败，不能导出",
                    canRetry = false,
                    technicalDetail = inspection.reason,
                )
            }
            val modelPath = requireNotNull(inspection.modelPath)
            try {
                outputProvider().use { output ->
                    Files.newInputStream(modelPath).use { input ->
                        copyStream(
                            input = input,
                            output = output,
                            initialBytes = 0L,
                            totalBytes = manifest.expectedBytes,
                            stage = ModelTransferStage.EXPORTING,
                            onProgress = onProgress,
                        )
                    }
                    output.flush()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: IOException) {
                return@withContext ModelAssetResult.Failure(
                    category = ModelAssetFailureCategory.STORAGE,
                    message = "模型导出失败，目标文件可能不完整，请删除后重试",
                    canRetry = true,
                    technicalDetail = error.message,
                )
            }
            ModelAssetResult.Exported(
                bytes = manifest.expectedBytes,
                message = "模型已导出，可用于换机或重装后导入",
            )
        }
    }

    private suspend fun <T> serializedTransfer(block: suspend () -> T): T {
        transferMutex.lock()
        return try {
            block()
        } finally {
            transferMutex.unlock()
        }
    }

    private suspend fun writeResponseBody(
        target: Path,
        input: InputStream,
        append: Boolean,
        initialBytes: Long,
        totalBytes: Long,
        stage: ModelTransferStage = ModelTransferStage.DOWNLOADING,
        onProgress: (ModelTransferProgress) -> Unit,
    ) {
        val options = if (append) {
            arrayOf(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)
        } else {
            arrayOf(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
        }
        FileChannel.open(target, *options).use { channel ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var completed = initialBytes
            while (true) {
                coroutineContext.ensureActive()
                val read = input.read(buffer)
                if (read < 0) break
                val byteBuffer = ByteBuffer.wrap(buffer, 0, read)
                while (byteBuffer.hasRemaining()) channel.write(byteBuffer)
                completed += read
                onProgress(ModelTransferProgress(stage, completed, totalBytes))
            }
            channel.force(true)
        }
    }

    private suspend fun copyStream(
        input: InputStream,
        output: OutputStream,
        initialBytes: Long,
        totalBytes: Long,
        stage: ModelTransferStage,
        onProgress: (ModelTransferProgress) -> Unit,
    ) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var completed = initialBytes
        while (true) {
            coroutineContext.ensureActive()
            val read = input.read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
            completed += read
            onProgress(ModelTransferProgress(stage, completed, totalBytes))
        }
    }

    private fun truncate(path: Path) {
        FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING).use {
            it.force(true)
        }
    }

    private fun ModelInspection.toAssetResult(source: ModelAssetSource): ModelAssetResult =
        if (state == ModelInstallState.READY) {
            ModelAssetResult.Installed(
                source = source,
                modelPath = requireNotNull(modelPath),
                message = reason,
            )
        } else {
            ModelAssetResult.Failure(
                category = if (state == ModelInstallState.CORRUPT) {
                    ModelAssetFailureCategory.INTEGRITY
                } else {
                    ModelAssetFailureCategory.STORAGE
                },
                message = reason,
                canRetry = true,
            )
        }
}

enum class ModelTransferStage {
    DOWNLOADING,
    IMPORTING,
    VERIFYING,
    EXPORTING,
}

data class ModelTransferProgress(
    val stage: ModelTransferStage,
    val completedBytes: Long,
    val totalBytes: Long,
)

enum class ModelAssetSource {
    DOWNLOAD,
    IMPORT,
}

enum class ModelAssetFailureCategory {
    NETWORK,
    SERVER,
    STORAGE,
    INTEGRITY,
}

sealed interface ModelAssetResult {
    data class Installed(
        val source: ModelAssetSource,
        val modelPath: Path,
        val message: String,
    ) : ModelAssetResult

    data class AlreadyReady(
        val modelPath: Path,
        val message: String,
    ) : ModelAssetResult

    data class Exported(
        val bytes: Long,
        val message: String,
    ) : ModelAssetResult

    data class Failure(
        val category: ModelAssetFailureCategory,
        val message: String,
        val canRetry: Boolean,
        val technicalDetail: String? = null,
    ) : ModelAssetResult
}

private fun okhttp3.Response.contentRangeStart(): Long? {
    val value = header("Content-Range") ?: return null
    return CONTENT_RANGE.matchEntire(value)?.groupValues?.get(1)?.toLongOrNull()
}

private val CONTENT_RANGE = Regex("bytes (\\d+)-(\\d+)/(\\d+|\\*)")

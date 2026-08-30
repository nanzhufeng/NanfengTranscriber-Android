package com.nanzhufeng.transcriber.data.modelstore

import com.nanzhufeng.transcriber.domain.model.ModelInstallState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.coroutines.coroutineContext

class ModelAssetManager(
    private val store: FileModelStore,
    private val httpClient: OkHttpClient,
) {
    private val transferMutex = Mutex()

    suspend fun download(
        model: CatalogModel,
        onProgress: (ModelTransferProgress) -> Unit = {},
    ): ModelAssetResult = download(
        manifest = model.manifest,
        downloads = model.downloads,
        onProgress = onProgress,
    )

    /** 保留单文件模型调用兼容性。 */
    suspend fun download(
        manifest: ModelManifest,
        url: String,
        onProgress: (ModelTransferProgress) -> Unit = {},
    ): ModelAssetResult = download(
        manifest = manifest,
        downloads = listOf(ModelDownload(manifest.files.single().relativePath, url)),
        onProgress = onProgress,
    )

    private suspend fun download(
        manifest: ModelManifest,
        downloads: List<ModelDownload>,
        onProgress: (ModelTransferProgress) -> Unit,
    ): ModelAssetResult = serializedTransfer {
        withContext(Dispatchers.IO) {
            val ready = store.inspect(manifest)
            if (ready.state == ModelInstallState.READY) {
                return@withContext ModelAssetResult.AlreadyReady(
                    modelPath = requireNotNull(ready.modelPath),
                    message = "模型已存在，无需重复下载",
                )
            }
            if (downloads.map(ModelDownload::relativePath).toSet() !=
                manifest.files.map(ModelFileManifest::relativePath).toSet()
            ) {
                return@withContext ModelAssetResult.Failure(
                    category = ModelAssetFailureCategory.INTEGRITY,
                    message = "模型下载清单不完整，未开始写入",
                    canRetry = false,
                )
            }

            var completedBefore = 0L
            manifest.files.forEach { file ->
                coroutineContext.ensureActive()
                if (store.isFileReady(manifest, file)) {
                    completedBefore += file.expectedBytes
                    onProgress(
                        ModelTransferProgress(
                            ModelTransferStage.DOWNLOADING,
                            completedBefore,
                            manifest.expectedBytes,
                            file.relativePath,
                        ),
                    )
                    return@forEach
                }

                val source = downloads.first { it.relativePath == file.relativePath }
                val result = downloadFile(
                    manifest = manifest,
                    file = file,
                    url = source.url,
                    completedBefore = completedBefore,
                    totalBytes = manifest.expectedBytes,
                    onProgress = onProgress,
                )
                if (result != null) return@withContext result
                completedBefore += file.expectedBytes
            }

            onProgress(
                ModelTransferProgress(
                    stage = ModelTransferStage.VERIFYING,
                    completedBytes = manifest.expectedBytes,
                    totalBytes = manifest.expectedBytes,
                ),
            )
            store.inspect(manifest, fullHash = true).toAssetResult(ModelAssetSource.DOWNLOAD)
        }
    }

    private suspend fun downloadFile(
        manifest: ModelManifest,
        file: ModelFileManifest,
        url: String,
        completedBefore: Long,
        totalBytes: Long,
        onProgress: (ModelTransferProgress) -> Unit,
    ): ModelAssetResult.Failure? {
        val partial = store.partialPath(manifest, file)
        var existingBytes = Files.size(partial)
        if (existingBytes == file.expectedBytes) {
            val committed = store.commitFile(manifest, file)
            return if (committed.state == ModelInstallState.CORRUPT ||
                committed.state == ModelInstallState.FAILED
            ) {
                committed.toFailure()
            } else {
                null
            }
        }
        if (existingBytes > file.expectedBytes) {
            truncate(partial)
            existingBytes = 0L
        }

        onProgress(
            ModelTransferProgress(
                ModelTransferStage.DOWNLOADING,
                completedBefore + existingBytes,
                totalBytes,
                file.relativePath,
            ),
        )
        val request = Request.Builder()
            .url(url)
            .apply { if (existingBytes > 0L) header("Range", "bytes=$existingBytes-") }
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return ModelAssetResult.Failure(
                        category = ModelAssetFailureCategory.SERVER,
                        message = "模型服务器返回 ${response.code}，已保留下载进度",
                        canRetry = response.code >= 500 || response.code == 408 || response.code == 429,
                    )
                }
                val body = response.body ?: return ModelAssetResult.Failure(
                    category = ModelAssetFailureCategory.NETWORK,
                    message = "模型服务器没有返回文件内容，已保留下载进度",
                    canRetry = true,
                )
                val append = when {
                    existingBytes == 0L && response.code == 200 -> false
                    existingBytes == 0L && response.code == 206 -> response.contentRangeStart() == 0L
                    existingBytes > 0L && response.code == 206 ->
                        response.contentRangeStart() == existingBytes
                    existingBytes > 0L && response.code == 200 -> false
                    else -> false
                }
                if (response.code == 206 && !append) {
                    return ModelAssetResult.Failure(
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
                    completedBefore = completedBefore,
                    totalBytes = totalBytes,
                    currentFile = file.relativePath,
                    onProgress = onProgress,
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: IOException) {
            return ModelAssetResult.Failure(
                category = ModelAssetFailureCategory.NETWORK,
                message = "模型下载中断，已保留进度，可稍后继续",
                canRetry = true,
                technicalDetail = error.message,
            )
        }

        if (Files.size(partial) != file.expectedBytes) {
            return ModelAssetResult.Failure(
                category = ModelAssetFailureCategory.INTEGRITY,
                message = "模型文件 ${file.relativePath} 长度不完整，已保留进度",
                canRetry = true,
            )
        }
        val committed = store.commitFile(manifest, file)
        return if (committed.state == ModelInstallState.CORRUPT ||
            committed.state == ModelInstallState.FAILED
        ) {
            committed.toFailure()
        } else {
            null
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
                    requireNotNull(ready.modelPath),
                    "模型已存在，无需重复导入",
                )
            }
            if (manifest.files.size == 1) {
                importSingleFile(manifest, inputProvider, onProgress)
            } else {
                importBundle(manifest, inputProvider, onProgress)
            }
        }
    }

    private suspend fun importSingleFile(
        manifest: ModelManifest,
        inputProvider: () -> InputStream,
        onProgress: (ModelTransferProgress) -> Unit,
    ): ModelAssetResult {
        val candidate = store.importPath(manifest)
        return try {
            inputProvider().use { input ->
                writeResponseBody(
                    target = candidate,
                    input = input,
                    append = false,
                    initialBytes = 0L,
                    completedBefore = 0L,
                    totalBytes = manifest.expectedBytes,
                    stage = ModelTransferStage.IMPORTING,
                    currentFile = manifest.files.single().relativePath,
                    onProgress = onProgress,
                )
            }
            onProgress(
                ModelTransferProgress(
                    ModelTransferStage.VERIFYING,
                    Files.size(candidate),
                    manifest.expectedBytes,
                ),
            )
            store.commitImport(manifest).toAssetResult(ModelAssetSource.IMPORT)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: IOException) {
            ModelAssetResult.Failure(
                ModelAssetFailureCategory.STORAGE,
                "读取导入文件失败，请重新选择模型文件",
                canRetry = true,
                technicalDetail = error.message,
            )
        }
    }

    private suspend fun importBundle(
        manifest: ModelManifest,
        inputProvider: () -> InputStream,
        onProgress: (ModelTransferProgress) -> Unit,
    ): ModelAssetResult {
        val expectedByPath = manifest.files.associateBy(ModelFileManifest::relativePath)
        val imported = linkedSetOf<String>()
        var completed = 0L
        return try {
            ZipInputStream(BufferedInputStream(inputProvider())).use { zip ->
                while (true) {
                    coroutineContext.ensureActive()
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    val file = expectedByPath[entry.name] ?: return ModelAssetResult.Failure(
                        ModelAssetFailureCategory.INTEGRITY,
                        "模型备份包含未声明文件 ${entry.name}，已停止导入",
                        canRetry = false,
                    )
                    if (!imported.add(entry.name)) return ModelAssetResult.Failure(
                        ModelAssetFailureCategory.INTEGRITY,
                        "模型备份包含重复文件 ${entry.name}，已停止导入",
                        canRetry = false,
                    )
                    val candidate = store.importPath(manifest, file)
                    FileChannel.open(
                        candidate,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                    ).use { channel ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var fileBytes = 0L
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = zip.read(buffer)
                            if (read < 0) break
                            fileBytes += read
                            if (fileBytes > file.expectedBytes) {
                                return ModelAssetResult.Failure(
                                    ModelAssetFailureCategory.INTEGRITY,
                                    "模型备份中的 ${entry.name} 超出声明长度",
                                    canRetry = false,
                                )
                            }
                            val byteBuffer = ByteBuffer.wrap(buffer, 0, read)
                            while (byteBuffer.hasRemaining()) channel.write(byteBuffer)
                            onProgress(
                                ModelTransferProgress(
                                    ModelTransferStage.IMPORTING,
                                    completed + fileBytes,
                                    manifest.expectedBytes,
                                    entry.name,
                                ),
                            )
                        }
                        channel.force(true)
                        if (fileBytes != file.expectedBytes) return ModelAssetResult.Failure(
                            ModelAssetFailureCategory.INTEGRITY,
                            "模型备份中的 ${entry.name} 长度不完整",
                            canRetry = true,
                        )
                    }
                    completed += file.expectedBytes
                }
            }
            if (imported != expectedByPath.keys) return ModelAssetResult.Failure(
                ModelAssetFailureCategory.INTEGRITY,
                "模型备份缺少 ${expectedByPath.keys.minus(imported).joinToString()} 文件",
                canRetry = true,
            )
            manifest.files.forEach { file ->
                val committed = store.commitImportedFile(manifest, file)
                if (committed.state == ModelInstallState.FAILED ||
                    committed.state == ModelInstallState.CORRUPT
                ) return committed.toFailure()
            }
            onProgress(
                ModelTransferProgress(
                    ModelTransferStage.VERIFYING,
                    manifest.expectedBytes,
                    manifest.expectedBytes,
                ),
            )
            store.inspect(manifest, fullHash = true).toAssetResult(ModelAssetSource.IMPORT)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: IOException) {
            ModelAssetResult.Failure(
                ModelAssetFailureCategory.STORAGE,
                "读取模型备份包失败，请重新选择正确文件",
                canRetry = true,
                technicalDetail = error.message,
            )
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
                    ModelAssetFailureCategory.INTEGRITY,
                    "模型未就绪或校验失败，不能导出",
                    canRetry = false,
                    technicalDetail = inspection.reason,
                )
            }
            try {
                outputProvider().use { output ->
                    if (manifest.files.size == 1) {
                        Files.newInputStream(store.installedPath(manifest, manifest.files.single())).use { input ->
                            copyStream(input, output, 0L, manifest.expectedBytes, onProgress)
                        }
                    } else {
                        ZipOutputStream(BufferedOutputStream(output)).use { zip ->
                            var completed = 0L
                            manifest.files.forEach { file ->
                                zip.putNextEntry(ZipEntry(file.relativePath))
                                Files.newInputStream(store.installedPath(manifest, file)).use { input ->
                                    copyStream(
                                        input,
                                        zip,
                                        completed,
                                        manifest.expectedBytes,
                                        onProgress,
                                        file.relativePath,
                                    )
                                }
                                zip.closeEntry()
                                completed += file.expectedBytes
                            }
                        }
                    }
                    output.flush()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: IOException) {
                return@withContext ModelAssetResult.Failure(
                    ModelAssetFailureCategory.STORAGE,
                    "模型导出失败，目标文件可能不完整，请删除后重试",
                    canRetry = true,
                    technicalDetail = error.message,
                )
            }
            ModelAssetResult.Exported(manifest.expectedBytes, "模型已导出，可用于换机或重装后导入")
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
        completedBefore: Long,
        totalBytes: Long,
        stage: ModelTransferStage = ModelTransferStage.DOWNLOADING,
        currentFile: String? = null,
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
                onProgress(
                    ModelTransferProgress(
                        stage,
                        completedBefore + completed,
                        totalBytes,
                        currentFile,
                    ),
                )
            }
            channel.force(true)
        }
    }

    private suspend fun copyStream(
        input: InputStream,
        output: OutputStream,
        completedBefore: Long,
        totalBytes: Long,
        onProgress: (ModelTransferProgress) -> Unit,
        currentFile: String? = null,
    ) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var completed = 0L
        while (true) {
            coroutineContext.ensureActive()
            val read = input.read(buffer)
            if (read < 0) break
            output.write(buffer, 0, read)
            completed += read
            onProgress(
                ModelTransferProgress(
                    ModelTransferStage.EXPORTING,
                    completedBefore + completed,
                    totalBytes,
                    currentFile,
                ),
            )
        }
    }

    private fun truncate(path: Path) {
        FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING).use {
            it.force(true)
        }
    }

    private fun ModelInspection.toAssetResult(source: ModelAssetSource): ModelAssetResult =
        if (state == ModelInstallState.READY) {
            ModelAssetResult.Installed(source, requireNotNull(modelPath), reason)
        } else {
            toFailure()
        }

    private fun ModelInspection.toFailure(): ModelAssetResult.Failure = ModelAssetResult.Failure(
        category = if (state == ModelInstallState.CORRUPT) {
            ModelAssetFailureCategory.INTEGRITY
        } else {
            ModelAssetFailureCategory.STORAGE
        },
        message = reason,
        canRetry = true,
    )
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
    val currentFile: String? = null,
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

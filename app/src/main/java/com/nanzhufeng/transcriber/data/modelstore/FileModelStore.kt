package com.nanzhufeng.transcriber.data.modelstore

import com.nanzhufeng.transcriber.domain.model.ModelInstallState
import org.json.JSONArray
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.Comparator

class FileModelStore(
    private val root: Path,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    fun partialPath(manifest: ModelManifest): Path = partialPath(manifest, manifest.files.first())

    fun partialPath(manifest: ModelManifest, file: ModelFileManifest): Path =
        candidatePath(manifest, file, PARTIAL_SUFFIX)

    fun importPath(manifest: ModelManifest): Path {
        val directory = modelDirectory(manifest)
        Files.createDirectories(directory)
        return directory.resolve(IMPORT_FILE)
    }

    fun importPath(manifest: ModelManifest, file: ModelFileManifest): Path =
        candidatePath(manifest, file, IMPORT_SUFFIX)

    fun installedPath(manifest: ModelManifest, file: ModelFileManifest): Path =
        safeResolve(modelDirectory(manifest), file.relativePath)

    fun isFileReady(manifest: ModelManifest, file: ModelFileManifest): Boolean {
        val path = installedPath(manifest, file)
        return Files.isRegularFile(path) &&
            Files.size(path) == file.expectedBytes &&
            sha256(path).equals(file.sha256, ignoreCase = true)
    }

    fun inspect(manifest: ModelManifest, fullHash: Boolean = false): ModelInspection {
        val directory = modelDirectory(manifest)
        val missingFiles = manifest.files.filter { Files.notExists(installedPath(manifest, it)) }
        if (missingFiles.isNotEmpty()) {
            val hasPartial = manifest.files.any { file ->
                val partial = partialPathWithoutCreate(manifest, file)
                Files.exists(partial) && Files.size(partial) > 0L
            } || Files.exists(directory.resolve(IMPORT_FILE))
            return if (hasPartial) {
                ModelInspection(ModelInstallState.DOWNLOADING, "发现未完成下载，可继续传输")
            } else {
                ModelInspection(ModelInstallState.NOT_INSTALLED, "模型尚未安装")
            }
        }

        manifest.files.forEach { file ->
            if (Files.size(installedPath(manifest, file)) != file.expectedBytes) {
                return ModelInspection(ModelInstallState.CORRUPT, "模型文件 ${file.relativePath} 长度与清单不一致")
            }
        }

        val storedManifest = readJson(directory.resolve(MANIFEST_FILE))
        val verification = readJson(directory.resolve(VERIFICATION_FILE))
        val metadataMatches = storedManifest?.matches(manifest) == true &&
            verification?.verificationMatches(manifest) == true

        if (!metadataMatches || fullHash) {
            val actualHashes = linkedMapOf<String, String>()
            manifest.files.forEach { file ->
                val actualHash = sha256(installedPath(manifest, file))
                if (!actualHash.equals(file.sha256, ignoreCase = true)) {
                    return ModelInspection(
                        ModelInstallState.CORRUPT,
                        "模型文件 ${file.relativePath} SHA-256 校验失败",
                    )
                }
                actualHashes[file.relativePath] = actualHash
            }
            writeMetadata(directory, manifest, actualHashes)
        }

        return ModelInspection(
            ModelInstallState.READY,
            "模型已校验，可长期复用",
            installedPath(manifest, manifest.files.first { it.relativePath == manifest.entryFile }),
        )
    }

    fun commit(manifest: ModelManifest): ModelInspection {
        val file = manifest.files.first()
        val partial = partialPathWithoutCreate(manifest, file)
        if (Files.notExists(partial)) return inspect(manifest, fullHash = true)
        val result = commitCandidate(manifest, file, partial, "下载")
        return if (result.state == ModelInstallState.READY) inspect(manifest, fullHash = true) else result
    }

    fun commitFile(
        manifest: ModelManifest,
        file: ModelFileManifest,
        sourceLabel: String = "下载",
    ): ModelInspection = commitCandidate(
        manifest = manifest,
        file = file,
        candidate = partialPathWithoutCreate(manifest, file),
        sourceLabel = sourceLabel,
    )

    fun commitImportedFile(
        manifest: ModelManifest,
        file: ModelFileManifest,
    ): ModelInspection = commitCandidate(
        manifest = manifest,
        file = file,
        candidate = importPath(manifest, file),
        sourceLabel = "导入",
    )

    fun commitImport(manifest: ModelManifest): ModelInspection {
        if (manifest.files.size != 1) {
            return ModelInspection(ModelInstallState.FAILED, "多文件模型必须使用南枫模型备份包导入")
        }
        val candidate = importPath(manifest)
        if (Files.notExists(candidate)) {
            return ModelInspection(ModelInstallState.FAILED, "没有可提交的导入文件")
        }
        val result = commitCandidate(manifest, manifest.files.first(), candidate, "导入")
        return if (result.state == ModelInstallState.READY) inspect(manifest, fullHash = true) else result
    }

    fun cacheBytes(manifest: ModelManifest): Long {
        val directory = modelDirectory(manifest)
        if (Files.notExists(directory)) return 0L
        return Files.walk(directory).use { paths ->
            paths.filter(Files::isRegularFile).mapToLong(Files::size).sum()
        }
    }

    fun delete(manifest: ModelManifest): Long {
        val directory = modelDirectory(manifest).normalize()
        val normalizedRoot = root.normalize()
        require(directory.startsWith(normalizedRoot)) { "模型缓存路径越界" }
        if (Files.notExists(directory)) return 0L
        val removedBytes = cacheBytes(manifest)
        Files.walk(directory).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists)
        }
        return removedBytes
    }

    private fun modelDirectory(manifest: ModelManifest): Path =
        root.resolve(manifest.modelId).resolve(manifest.version)

    private fun candidatePath(
        manifest: ModelManifest,
        file: ModelFileManifest,
        suffix: String,
    ): Path {
        val target = installedPath(manifest, file)
        Files.createDirectories(target.parent)
        val candidate = target.resolveSibling("${target.fileName}$suffix")
        if (Files.notExists(candidate)) Files.createFile(candidate)
        return candidate
    }

    private fun partialPathWithoutCreate(manifest: ModelManifest, file: ModelFileManifest): Path {
        val target = installedPath(manifest, file)
        return target.resolveSibling("${target.fileName}$PARTIAL_SUFFIX")
    }

    private fun safeResolve(directory: Path, relativePath: String): Path {
        val resolved = directory.resolve(relativePath).normalize()
        require(resolved.startsWith(directory.normalize())) { "模型文件路径越界" }
        return resolved
    }

    private fun commitCandidate(
        manifest: ModelManifest,
        file: ModelFileManifest,
        candidate: Path,
        sourceLabel: String,
    ): ModelInspection {
        if (Files.notExists(candidate)) {
            return ModelInspection(ModelInstallState.FAILED, "没有可提交的${sourceLabel}文件")
        }
        if (Files.size(candidate) != file.expectedBytes) {
            return ModelInspection(
                ModelInstallState.FAILED,
                "${sourceLabel}文件 ${file.relativePath} 长度不完整，临时文件已保留",
            )
        }

        val actualHash = sha256(candidate)
        if (!actualHash.equals(file.sha256, ignoreCase = true)) {
            return ModelInspection(
                ModelInstallState.CORRUPT,
                "${sourceLabel}文件 ${file.relativePath} SHA-256 校验失败",
            )
        }

        val target = installedPath(manifest, file)
        Files.createDirectories(target.parent)
        moveAtomically(candidate, target)
        val complete = manifest.files.all { Files.isRegularFile(installedPath(manifest, it)) }
        return if (complete) {
            inspect(manifest, fullHash = true)
        } else {
            ModelInspection(ModelInstallState.DOWNLOADING, "${file.relativePath} 已校验，继续准备其余模型文件")
        }
    }

    private fun writeMetadata(
        directory: Path,
        manifest: ModelManifest,
        actualHashes: Map<String, String>,
    ) {
        val filesJson = JSONArray()
        manifest.files.forEach { file ->
            filesJson.put(
                JSONObject()
                    .put("relativePath", file.relativePath)
                    .put("expectedBytes", file.expectedBytes)
                    .put("sha256", file.sha256.lowercase()),
            )
        }
        val verifiedFilesJson = JSONArray()
        manifest.files.forEach { file ->
            verifiedFilesJson.put(
                JSONObject()
                    .put("relativePath", file.relativePath)
                    .put("bytes", file.expectedBytes)
                    .put("sha256", requireNotNull(actualHashes[file.relativePath]).lowercase()),
            )
        }
        val manifestJson = JSONObject()
            .put("modelId", manifest.modelId)
            .put("version", manifest.version)
            .put("engineVersion", manifest.engineVersion)
            .put("entryFile", manifest.entryFile)
            .put("files", filesJson)
        val verificationJson = JSONObject()
            .put("verifiedAtMillis", nowMillis())
            .put("files", verifiedFilesJson)

        writeUtf8Atomically(directory.resolve(MANIFEST_FILE), manifestJson.toString(2))
        writeUtf8Atomically(directory.resolve(VERIFICATION_FILE), verificationJson.toString(2))
    }

    private fun JSONObject.matches(manifest: ModelManifest): Boolean {
        if (optString("modelId") != manifest.modelId ||
            optString("version") != manifest.version ||
            optString("engineVersion") != manifest.engineVersion ||
            optString("entryFile") != manifest.entryFile
        ) return false
        val storedFiles = optJSONArray("files") ?: return false
        if (storedFiles.length() != manifest.files.size) return false
        return manifest.files.withIndex().all { (index, file) ->
            val stored = storedFiles.optJSONObject(index) ?: return@all false
            stored.optString("relativePath") == file.relativePath &&
                stored.optLong("expectedBytes", -1L) == file.expectedBytes &&
                stored.optString("sha256").equals(file.sha256, ignoreCase = true)
        }
    }

    private fun JSONObject.verificationMatches(manifest: ModelManifest): Boolean {
        val storedFiles = optJSONArray("files") ?: return false
        if (storedFiles.length() != manifest.files.size) return false
        return manifest.files.withIndex().all { (index, file) ->
            val stored = storedFiles.optJSONObject(index) ?: return@all false
            stored.optString("relativePath") == file.relativePath &&
                stored.optLong("bytes", -1L) == file.expectedBytes &&
                stored.optString("sha256").equals(file.sha256, ignoreCase = true)
        }
    }

    private fun readJson(path: Path): JSONObject? {
        if (Files.notExists(path)) return null
        return runCatching {
            JSONObject(String(Files.readAllBytes(path), StandardCharsets.UTF_8))
        }.getOrNull()
    }

    private fun writeUtf8Atomically(target: Path, text: String) {
        Files.createDirectories(target.parent)
        val temporary = target.resolveSibling("${target.fileName}.tmp")
        val bytes = text.toByteArray(StandardCharsets.UTF_8)
        FileChannel.open(
            temporary,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE,
        ).use { channel ->
            val buffer = ByteBuffer.wrap(bytes)
            while (buffer.hasRemaining()) channel.write(buffer)
            channel.force(true)
        }
        moveAtomically(temporary, target)
    }

    private fun moveAtomically(source: Path, target: Path) {
        try {
            Files.move(
                source,
                target,
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING,
            )
        } catch (_: AtomicMoveNotSupportedException) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun sha256(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PARTIAL_SUFFIX = ".part"
        private const val IMPORT_SUFFIX = ".importing"
        private const val IMPORT_FILE = "model.importing"
        private const val MANIFEST_FILE = "manifest.json"
        private const val VERIFICATION_FILE = "verification.json"
    }
}

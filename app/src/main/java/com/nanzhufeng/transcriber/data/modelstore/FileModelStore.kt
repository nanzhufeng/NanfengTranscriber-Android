package com.nanzhufeng.transcriber.data.modelstore

import com.nanzhufeng.transcriber.domain.model.ModelInstallState
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
    fun partialPath(manifest: ModelManifest): Path {
        val directory = modelDirectory(manifest)
        Files.createDirectories(directory)
        val partial = directory.resolve(PARTIAL_FILE)
        if (Files.notExists(partial)) {
            Files.createFile(partial)
        }
        return partial
    }

    fun importPath(manifest: ModelManifest): Path {
        val directory = modelDirectory(manifest)
        Files.createDirectories(directory)
        return directory.resolve(IMPORT_FILE)
    }

    fun inspect(manifest: ModelManifest, fullHash: Boolean = false): ModelInspection {
        val directory = modelDirectory(manifest)
        val model = directory.resolve(MODEL_FILE)
        val partial = directory.resolve(PARTIAL_FILE)

        if (Files.notExists(model)) {
            return if (Files.exists(partial) && Files.size(partial) > 0L) {
                ModelInspection(ModelInstallState.DOWNLOADING, "发现未完成下载，可继续传输")
            } else {
                ModelInspection(ModelInstallState.NOT_INSTALLED, "模型尚未安装")
            }
        }

        if (Files.size(model) != manifest.expectedBytes) {
            return ModelInspection(ModelInstallState.CORRUPT, "模型长度与清单不一致")
        }

        val storedManifest = readJson(directory.resolve(MANIFEST_FILE))
        val verification = readJson(directory.resolve(VERIFICATION_FILE))
        val metadataMatches = storedManifest?.matches(manifest) == true &&
            verification?.optLong("bytes", -1L) == manifest.expectedBytes &&
            verification?.optString("sha256").equals(manifest.sha256, ignoreCase = true)

        if (!metadataMatches || fullHash) {
            val actualHash = sha256(model)
            if (!actualHash.equals(manifest.sha256, ignoreCase = true)) {
                return ModelInspection(ModelInstallState.CORRUPT, "模型 SHA-256 校验失败")
            }
            writeMetadata(directory, manifest, actualHash)
        }

        return ModelInspection(ModelInstallState.READY, "模型已校验，可长期复用", model)
    }

    fun commit(manifest: ModelManifest): ModelInspection {
        val directory = modelDirectory(manifest)
        val partial = directory.resolve(PARTIAL_FILE)
        if (Files.notExists(partial)) {
            return inspect(manifest, fullHash = true)
        }
        return commitCandidate(manifest, partial, "下载")
    }

    fun commitImport(manifest: ModelManifest): ModelInspection {
        val candidate = importPath(manifest)
        if (Files.notExists(candidate)) {
            return ModelInspection(ModelInstallState.FAILED, "没有可提交的导入文件")
        }
        return commitCandidate(manifest, candidate, "导入")
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

    private fun commitCandidate(
        manifest: ModelManifest,
        candidate: Path,
        sourceLabel: String,
    ): ModelInspection {
        if (Files.size(candidate) != manifest.expectedBytes) {
            return ModelInspection(
                ModelInstallState.FAILED,
                "${sourceLabel}文件长度不完整，临时文件已保留",
            )
        }

        val actualHash = sha256(candidate)
        if (!actualHash.equals(manifest.sha256, ignoreCase = true)) {
            return ModelInspection(ModelInstallState.CORRUPT, "${sourceLabel}文件 SHA-256 校验失败")
        }

        val directory = modelDirectory(manifest)
        Files.createDirectories(directory)
        val model = directory.resolve(MODEL_FILE)
        moveAtomically(candidate, model)
        writeMetadata(directory, manifest, actualHash)
        return ModelInspection(ModelInstallState.READY, "模型已完成原子提交", model)
    }

    private fun writeMetadata(directory: Path, manifest: ModelManifest, actualHash: String) {
        val manifestJson = JSONObject()
            .put("modelId", manifest.modelId)
            .put("version", manifest.version)
            .put("expectedBytes", manifest.expectedBytes)
            .put("sha256", manifest.sha256.lowercase())
            .put("engineVersion", manifest.engineVersion)
        val verificationJson = JSONObject()
            .put("bytes", manifest.expectedBytes)
            .put("sha256", actualHash.lowercase())
            .put("verifiedAtMillis", nowMillis())

        writeUtf8Atomically(directory.resolve(MANIFEST_FILE), manifestJson.toString(2))
        writeUtf8Atomically(directory.resolve(VERIFICATION_FILE), verificationJson.toString(2))
    }

    private fun JSONObject.matches(manifest: ModelManifest): Boolean =
        optString("modelId") == manifest.modelId &&
            optString("version") == manifest.version &&
            optLong("expectedBytes", -1L) == manifest.expectedBytes &&
            optString("sha256").equals(manifest.sha256, ignoreCase = true) &&
            optString("engineVersion") == manifest.engineVersion

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
        private const val MODEL_FILE = "model.bin"
        private const val PARTIAL_FILE = "model.bin.part"
        private const val IMPORT_FILE = "model.bin.importing"
        private const val MANIFEST_FILE = "manifest.json"
        private const val VERIFICATION_FILE = "verification.json"
    }
}

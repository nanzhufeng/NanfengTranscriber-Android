package com.nanzhufeng.transcriber.data.modelstore

data class ModelManifest(
    val modelId: String,
    val version: String,
    val files: List<ModelFileManifest>,
    val engineVersion: String,
    val entryFile: String = files.first().relativePath,
) {
    val expectedBytes: Long = files.sumOf(ModelFileManifest::expectedBytes)

    init {
        require(modelId.matches(SAFE_SEGMENT)) { "modelId 包含不安全字符" }
        require(version.matches(SAFE_SEGMENT)) { "version 包含不安全字符" }
        require(files.isNotEmpty()) { "模型至少需要一个文件" }
        require(files.map(ModelFileManifest::relativePath).distinct().size == files.size) {
            "模型文件路径不能重复"
        }
        require(files.any { it.relativePath == entryFile }) { "entryFile 必须存在于模型清单" }
        require(engineVersion.isNotBlank()) { "engineVersion 不能为空" }
    }

    constructor(
        modelId: String,
        version: String,
        expectedBytes: Long,
        sha256: String,
        engineVersion: String,
    ) : this(
        modelId = modelId,
        version = version,
        files = listOf(
            ModelFileManifest(
                relativePath = DEFAULT_MODEL_FILE,
                expectedBytes = expectedBytes,
                sha256 = sha256,
            ),
        ),
        engineVersion = engineVersion,
        entryFile = DEFAULT_MODEL_FILE,
    )

    companion object {
        const val DEFAULT_MODEL_FILE = "model.bin"
        private val SAFE_SEGMENT = Regex("[A-Za-z0-9._-]+")
    }
}

data class ModelFileManifest(
    val relativePath: String,
    val expectedBytes: Long,
    val sha256: String,
) {
    init {
        require(relativePath.matches(SAFE_RELATIVE_PATH)) { "模型文件路径包含不安全字符" }
        require(!relativePath.startsWith('.') && ".." !in relativePath.split('/')) {
            "模型文件路径不能越界"
        }
        require(expectedBytes > 0) { "expectedBytes 必须大于 0" }
        require(sha256.matches(SHA_256)) { "sha256 必须是 64 位十六进制" }
    }

    companion object {
        private val SAFE_RELATIVE_PATH = Regex("[A-Za-z0-9._/-]+")
        private val SHA_256 = Regex("[A-Fa-f0-9]{64}")
    }
}

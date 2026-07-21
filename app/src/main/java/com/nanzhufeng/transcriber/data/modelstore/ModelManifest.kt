package com.nanzhufeng.transcriber.data.modelstore

data class ModelManifest(
    val modelId: String,
    val version: String,
    val expectedBytes: Long,
    val sha256: String,
    val engineVersion: String,
) {
    init {
        require(modelId.matches(SAFE_SEGMENT)) { "modelId 包含不安全字符" }
        require(version.matches(SAFE_SEGMENT)) { "version 包含不安全字符" }
        require(expectedBytes > 0) { "expectedBytes 必须大于 0" }
        require(sha256.matches(SHA_256)) { "sha256 必须是 64 位十六进制" }
        require(engineVersion.isNotBlank()) { "engineVersion 不能为空" }
    }

    companion object {
        private val SAFE_SEGMENT = Regex("[A-Za-z0-9._-]+")
        private val SHA_256 = Regex("[A-Fa-f0-9]{64}")
    }
}

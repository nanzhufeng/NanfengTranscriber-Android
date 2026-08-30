package com.nanzhufeng.transcriber.data.modelstore

object OfficialModelCatalog {
    const val SHERPA_ENGINE_VERSION = "sherpa-onnx-v1.13.6"
    const val SENSEVOICE_REPOSITORY_REVISION = "355f4d4884d8afd08aef04b9007a8556d7b463b2"

    /** 只供固定素材 A/B 与隔离验收使用，达标后再替换正式均衡档映射。 */
    val experimentalCandidates: List<CatalogModel> = listOf(
        CatalogModel(
            displayName = "本地标准",
            profile = ModelPerformanceProfile.BALANCED,
            provider = AsrProviderId.SENSEVOICE,
            manifest = ModelManifest(
                modelId = "sensevoice-small-int8",
                version = "hf-355f4d48",
                files = listOf(
                    ModelFileManifest(
                        relativePath = "model.int8.onnx",
                        expectedBytes = 237_115_547L,
                        sha256 = "12ca1a2ae7ecf3e0019ef2822307ee0b5cadc9196569e379b4c4026f8205276d",
                    ),
                    ModelFileManifest(
                        relativePath = "tokens.txt",
                        expectedBytes = 315_894L,
                        sha256 = "f449eb28dc567533d7fa59be34e2abca8784f771850c78a47fb731a31429a1dc",
                    ),
                ),
                engineVersion = SHERPA_ENGINE_VERSION,
                entryFile = "model.int8.onnx",
            ),
            downloads = listOf(
                ModelDownload("model.int8.onnx", senseVoiceUrl("model.int8.onnx")),
                ModelDownload("tokens.txt", senseVoiceUrl("tokens.txt")),
            ),
            capabilities = AsrCapabilities(
                supportsTimestamp = false,
                supportsStreaming = false,
                supportsDiarization = false,
                supportsHotwords = false,
                supportsOffline = true,
                recommendedChunkMillis = 30_000L,
            ),
        ),
    )

    /** 云端高精度档：不下载伪模型，实际向千问发送所选音频片段。 */
    val apiCandidates: List<CatalogModel> = listOf(
        CatalogModel(
            displayName = "高精度 Qwen",
            profile = ModelPerformanceProfile.HIGH_QUALITY,
            provider = AsrProviderId.QWEN3_ASR_API,
            manifest = ModelManifest(
                modelId = "qwen3-asr-api",
                version = "dashscope-qwen3-asr-flash",
                expectedBytes = 1L,
                sha256 = "0000000000000000000000000000000000000000000000000000000000000000",
                engineVersion = "DashScope Qwen3-ASR API",
            ),
            downloads = emptyList(),
            capabilities = AsrCapabilities(
                supportsTimestamp = false,
                supportsStreaming = false,
                supportsDiarization = false,
                supportsHotwords = false,
                supportsOffline = false,
                recommendedChunkMillis = 30_000L,
            ),
            requiresLocalCache = false,
        ),
    )

    /** 设置与单项转写中的唯一公开顺序。 */
    val modelSelectionCandidates: List<CatalogModel> = listOf(
        experimentalCandidates.single(),
        apiCandidates.single(),
    )

    val allCandidates: List<CatalogModel> = modelSelectionCandidates

    fun find(modelId: String): CatalogModel? = allCandidates.firstOrNull {
        it.manifest.modelId == modelId
    }

    private fun senseVoiceUrl(fileName: String): String =
        "https://huggingface.co/csukuangfj/" +
            "sherpa-onnx-sense-voice-zh-en-ja-ko-yue-int8-2025-09-09/resolve/" +
            "$SENSEVOICE_REPOSITORY_REVISION/$fileName"
}

data class CatalogModel(
    val displayName: String,
    val profile: ModelPerformanceProfile,
    val provider: AsrProviderId,
    val manifest: ModelManifest,
    val downloads: List<ModelDownload>,
    val capabilities: AsrCapabilities,
    val requiresLocalCache: Boolean = true,
) {
    init {
        if (requiresLocalCache) {
            require(
                downloads.map(ModelDownload::relativePath).toSet() ==
                    manifest.files.map(ModelFileManifest::relativePath).toSet(),
            ) { "模型下载清单必须与文件清单一致" }
        } else {
            require(downloads.isEmpty()) { "API 模型不得伪造本地下载" }
        }
    }

    val downloadUrl: String get() = downloads.first().url
}

data class ModelDownload(
    val relativePath: String,
    val url: String,
)

enum class AsrProviderId {
    SENSEVOICE,
    QWEN3_ASR_API,
}

data class AsrCapabilities(
    val supportsTimestamp: Boolean,
    val supportsStreaming: Boolean,
    val supportsDiarization: Boolean,
    val supportsHotwords: Boolean,
    val supportsOffline: Boolean,
    val recommendedChunkMillis: Long,
)

enum class ModelPerformanceProfile {
    BALANCED,
    HIGH_QUALITY,
}

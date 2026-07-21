package com.nanzhufeng.transcriber.data.modelstore

object OfficialModelCatalog {
    const val ENGINE_VERSION = "whisper.cpp-v1.9.1"
    const val MODEL_REPOSITORY_REVISION = "5359861c739e955e79d9a303bcbc70fb988958b1"

    val candidates: List<CatalogModel> = listOf(
        CatalogModel(
            displayName = "极速｜Base",
            profile = ModelPerformanceProfile.LIGHT,
            downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/$MODEL_REPOSITORY_REVISION/ggml-base.bin",
            manifest = ModelManifest(
                modelId = "base",
                version = "hf-5359861c",
                expectedBytes = 147_951_465L,
                sha256 = "60ed5bc3dd14eea856493d334349b405782ddcaf0028d4b5df4088345fba2efe",
                engineVersion = ENGINE_VERSION,
            ),
        ),
        CatalogModel(
            displayName = "均衡｜Small Q5_1（推荐）",
            profile = ModelPerformanceProfile.BALANCED,
            downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/$MODEL_REPOSITORY_REVISION/ggml-small-q5_1.bin",
            manifest = ModelManifest(
                modelId = "small-q5_1",
                version = "hf-5359861c",
                expectedBytes = 190_085_487L,
                sha256 = "ae85e4a935d7a567bd102fe55afc16bb595bdb618e11b2fc7591bc08120411bb",
                engineVersion = ENGINE_VERSION,
            ),
        ),
        CatalogModel(
            displayName = "高质量｜Large V3 Turbo Q5_0",
            profile = ModelPerformanceProfile.HIGH_QUALITY,
            downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/$MODEL_REPOSITORY_REVISION/ggml-large-v3-turbo-q5_0.bin",
            manifest = ModelManifest(
                modelId = "large-v3-turbo-q5_0",
                version = "hf-5359861c",
                expectedBytes = 574_041_195L,
                sha256 = "394221709cd5ad1f40c46e6031ca61bce88931e6e088c188294c6d5a55ffa7e2",
                engineVersion = ENGINE_VERSION,
            ),
        ),
    )

    fun find(modelId: String): CatalogModel? = candidates.firstOrNull {
        it.manifest.modelId == modelId
    }
}

data class CatalogModel(
    val displayName: String,
    val profile: ModelPerformanceProfile,
    val downloadUrl: String,
    val manifest: ModelManifest,
)

enum class ModelPerformanceProfile {
    LIGHT,
    BALANCED,
    QUALITY,
    HIGH_QUALITY,
}

package com.nanzhufeng.transcriber.data.modelstore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialModelCatalogTest {
    @Test
    fun onlyCurrentSenseVoiceAndQwenModelsArePublic() {
        val senseVoice = OfficialModelCatalog.experimentalCandidates.single()
        assertEquals(AsrProviderId.SENSEVOICE, senseVoice.provider)
        assertEquals(2, senseVoice.manifest.files.size)
        assertEquals("model.int8.onnx", senseVoice.manifest.entryFile)
        assertTrue(senseVoice.downloads.all { it.url.contains(OfficialModelCatalog.SENSEVOICE_REPOSITORY_REVISION) })
        assertEquals(30_000L, senseVoice.capabilities.recommendedChunkMillis)
        assertEquals(senseVoice, OfficialModelCatalog.find("sensevoice-small-int8"))
        assertEquals(2, OfficialModelCatalog.modelSelectionCandidates.size)
        assertEquals(
            listOf("sensevoice-small-int8", "qwen3-asr-api"),
            OfficialModelCatalog.allCandidates.map { it.manifest.modelId },
        )
        assertEquals(null, OfficialModelCatalog.find("small-q5_1"))
        assertEquals(null, OfficialModelCatalog.find("unknown-model"))
    }
}

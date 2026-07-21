package com.nanzhufeng.transcriber.data.modelstore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialModelCatalogTest {
    @Test
    fun candidateIdsAndUrlsAreUniqueAndFullyVerifiable() {
        val candidates = OfficialModelCatalog.candidates

        assertEquals(3, candidates.size)
        assertEquals("small-q5_1", candidates[1].manifest.modelId)
        assertEquals(candidates.size, candidates.map { it.manifest.modelId }.distinct().size)
        assertEquals(candidates.size, candidates.map { it.downloadUrl }.distinct().size)
        candidates.forEach { candidate ->
            assertEquals(candidate, OfficialModelCatalog.find(candidate.manifest.modelId))
            assertTrue(candidate.downloadUrl.startsWith("https://huggingface.co/ggerganov/whisper.cpp/"))
            assertTrue(candidate.downloadUrl.contains(OfficialModelCatalog.MODEL_REPOSITORY_REVISION))
            assertTrue(candidate.manifest.expectedBytes > 100_000_000L)
            assertEquals(64, candidate.manifest.sha256.length)
            assertEquals(OfficialModelCatalog.ENGINE_VERSION, candidate.manifest.engineVersion)
        }
        assertEquals(null, OfficialModelCatalog.find("unknown-model"))
    }
}

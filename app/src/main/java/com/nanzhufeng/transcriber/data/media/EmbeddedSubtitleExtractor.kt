package com.nanzhufeng.transcriber.data.media

import android.content.Context
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CueDecoder
import androidx.media3.inspector.MediaExtractorCompat
import com.nanzhufeng.transcriber.domain.export.TranscriptDocumentSegment
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path

@UnstableApi
class EmbeddedSubtitleExtractor(context: Context) {
    private val applicationContext = context.applicationContext
    private val cueDecoder = CueDecoder()

    fun extract(
        sourceUri: Uri,
        stagedSourcePath: Path?,
        preferredLanguage: String?,
    ): EmbeddedSubtitleResult? {
        val extractor = MediaExtractorCompat(applicationContext)
        return try {
            if (stagedSourcePath != null && Files.isRegularFile(stagedSourcePath)) {
                extractor.setDataSource(stagedSourcePath.toString())
                extractSelectedTrack(extractor, preferredLanguage)
            } else {
                applicationContext.contentResolver.openAssetFileDescriptor(sourceUri, "r")?.use { source ->
                    extractor.setDataSource(source)
                    extractSelectedTrack(extractor, preferredLanguage)
                }
            }
        } catch (_: Exception) {
            null
        } finally {
            extractor.release()
        }
    }

    private fun extractSelectedTrack(
        extractor: MediaExtractorCompat,
        preferredLanguage: String?,
    ): EmbeddedSubtitleResult? {
        val tracks = buildList {
            for (index in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(index)
                val mimeType = format.getString(MediaFormat.KEY_MIME).orEmpty().lowercase()
                Log.d(LogTag, "track=$index mime=$mimeType language=${format.getString(MediaFormat.KEY_LANGUAGE).orEmpty()}")
                if (mimeType in SupportedSubtitleMimeTypes) {
                    add(
                        SubtitleTrack(
                            index = index,
                            mimeType = mimeType,
                            language = format.getString(MediaFormat.KEY_LANGUAGE),
                            durationMicros = format.getLongOrNull(MediaFormat.KEY_DURATION),
                        ),
                    )
                }
            }
        }
        val selected = tracks.sortedWith(
            compareBy<SubtitleTrack> { track -> languageRank(track.language, preferredLanguage) }
                .thenBy(SubtitleTrack::index),
        ).firstOrNull() ?: return null
        Log.d(LogTag, "selected track=${selected.index} mime=${selected.mimeType} language=${selected.language.orEmpty()}")

        extractor.selectTrack(selected.index)
        val buffer = ByteBuffer.allocate(MaxSubtitleSampleBytes)
        val samples = mutableListOf<SubtitleSample>()
        while (samples.size < MaxSubtitleCueCount) {
            buffer.clear()
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) break
            if (sampleSize in 1..MaxSubtitleSampleBytes && extractor.sampleTrackIndex == selected.index) {
                val payload = ByteArray(sampleSize)
                buffer.position(0)
                buffer.get(payload)
                decodeSample(selected.mimeType, extractor.sampleTime, payload)?.let { sample ->
                    if (samples.lastOrNull()?.text != sample.text) samples += sample
                }
            }
            extractor.advance()
        }
        if (samples.isEmpty()) return null
        val fallbackDuration = selected.durationMicros
            ?.takeIf { it > 0L }
            ?: (samples.last().startMicros + DefaultCueDurationMicros)
        val readableCharacters = samples.sumOf { it.text.count(Char::isLetterOrDigit) }
        val durationSeconds = (fallbackDuration / 1_000_000.0).coerceAtLeast(1.0)
        if (
            samples.size < MinimumCueCount ||
            readableCharacters < MinimumReadableCharacters ||
            readableCharacters / durationSeconds < MinimumReadableCharactersPerSecond
        ) {
            Log.d(
                LogTag,
                "subtitle rejected cues=${samples.size} chars=$readableCharacters durationUs=$fallbackDuration",
            )
            return null
        }
        Log.d(LogTag, "subtitle accepted cues=${samples.size} chars=$readableCharacters durationUs=$fallbackDuration")
        val segments = samples.mapIndexed { index, sample ->
            val nextStart = samples.getOrNull(index + 1)?.startMicros
            val endMicros = (sample.endMicros ?: nextStart ?: (sample.startMicros + DefaultCueDurationMicros))
                .coerceAtMost(fallbackDuration)
                .coerceAtLeast(sample.startMicros + MinimumCueDurationMicros)
            TranscriptDocumentSegment(
                startMillis = sample.startMicros / 1_000L,
                endMillis = endMicros / 1_000L,
                text = sample.text,
            )
        }
        return EmbeddedSubtitleResult(
            language = selected.language,
            mimeType = selected.mimeType,
            durationMillis = fallbackDuration / 1_000L,
            segments = segments,
        )
    }

    private fun languageRank(trackLanguage: String?, preferredLanguage: String?): Int {
        val track = trackLanguage.orEmpty().lowercase()
        val preferred = preferredLanguage.orEmpty().lowercase()
        return when {
            preferred.isNotBlank() && (track == preferred || track.startsWith("$preferred-")) -> 0
            preferred.isBlank() && (track == "zh" || track.startsWith("zh-")) -> 1
            track.isBlank() || track == "und" -> 2
            else -> 3
        }
    }

    private fun decodeSample(mimeType: String, sampleTimeMicros: Long, payload: ByteArray): SubtitleSample? {
        if (mimeType == Media3CuesMimeType) {
            val fallbackStart = sampleTimeMicros.coerceAtLeast(0L)
            val decoded = runCatching {
                cueDecoder.decode(fallbackStart, payload, 0, payload.size)
            }.getOrNull() ?: return null
            val text = decoded.cues
                .mapNotNull { cue -> cue.text?.toString()?.trim()?.takeIf(String::isNotBlank) }
                .joinToString("\n")
                .trim()
                .takeIf(String::isNotBlank)
                ?: return null
            val start = decoded.startTimeUs.takeUnless { it == C.TIME_UNSET }?.coerceAtLeast(0L) ?: fallbackStart
            val end = decoded.durationUs
                .takeUnless { it == C.TIME_UNSET }
                ?.takeIf { it > 0L }
                ?.let { start + it }
            return SubtitleSample(start, end, text)
        }
        val text = decodeSubtitleSample(mimeType, payload) ?: return null
        return SubtitleSample(sampleTimeMicros.coerceAtLeast(0L), null, text)
    }

    private data class SubtitleTrack(
        val index: Int,
        val mimeType: String,
        val language: String?,
        val durationMicros: Long?,
    )

    private data class SubtitleSample(val startMicros: Long, val endMicros: Long?, val text: String)

    private companion object {
        const val MaxSubtitleSampleBytes = 1024 * 1024
        const val MaxSubtitleCueCount = 20_000
        const val MinimumCueCount = 2
        const val MinimumReadableCharacters = 20
        const val MinimumReadableCharactersPerSecond = 0.5
        const val DefaultCueDurationMicros = 3_000_000L
        const val MinimumCueDurationMicros = 200_000L
        const val LogTag = "NanfengSubtitle"
    }
}

data class EmbeddedSubtitleResult(
    val language: String?,
    val mimeType: String,
    val durationMillis: Long,
    val segments: List<TranscriptDocumentSegment>,
)

private val SupportedSubtitleMimeTypes = setOf(
    "application/x-media3-cues",
    "application/x-quicktime-tx3g",
    "text/3gpp",
    "text/3gpp-tt",
    "application/x-mp4-vtt",
    "text/vtt",
    "application/x-subrip",
    "text/srt",
    "text/x-ssa",
    "text/ass",
    "application/ttml+xml",
    "application/ttml",
)

private const val Media3CuesMimeType = "application/x-media3-cues"

private fun MediaFormat.getLongOrNull(key: String): Long? =
    if (containsKey(key)) runCatching { getLong(key) }.getOrNull() else null

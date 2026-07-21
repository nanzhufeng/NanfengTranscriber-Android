package com.nanzhufeng.transcriber.data.media

import android.content.ContentResolver
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.coroutines.coroutineContext
import kotlin.math.roundToInt

class MediaCodecAudioDecoder(
    private val contentResolver: ContentResolver,
) {
    suspend fun decodeDocument(
        source: Uri,
        targetPcm: Path,
        onProgress: suspend (AudioDecodeProgress) -> Unit = {},
    ): AudioDecodeResult = decode(targetPcm, onProgress) { extractor ->
        contentResolver.openAssetFileDescriptor(source, "r")?.use { descriptor ->
            if (descriptor.declaredLength >= 0L) {
                extractor.setDataSource(
                    descriptor.fileDescriptor,
                    descriptor.startOffset,
                    descriptor.declaredLength,
                )
            } else {
                extractor.setDataSource(descriptor.fileDescriptor)
            }
        } ?: throw IOException("系统未能打开所选音视频文件")
    }

    suspend fun decodeFile(
        source: Path,
        targetPcm: Path,
        onProgress: suspend (AudioDecodeProgress) -> Unit = {},
    ): AudioDecodeResult = decode(targetPcm, onProgress) { extractor ->
        extractor.setDataSource(source.toString())
    }

    private suspend fun decode(
        targetPcm: Path,
        onProgress: suspend (AudioDecodeProgress) -> Unit,
        configureDataSource: (MediaExtractor) -> Unit,
    ): AudioDecodeResult = withContext(Dispatchers.IO) {
        Files.createDirectories(targetPcm.parent)
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            configureDataSource(extractor)
            val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return@withContext AudioDecodeResult.Failure(
                category = AudioDecodeFailureCategory.NO_AUDIO_TRACK,
                message = "文件中没有可转写的音轨",
            )
            val inputFormat = extractor.getTrackFormat(trackIndex)
            val mime = requireNotNull(inputFormat.getString(MediaFormat.KEY_MIME))
            val durationMicros = inputFormat.longOrNull(MediaFormat.KEY_DURATION)
            extractor.selectTrack(trackIndex)

            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(inputFormat, null, null, 0)
            codec.start()

            FileChannel.open(
                targetPcm,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE,
            ).use { output ->
                val bufferInfo = MediaCodec.BufferInfo()
                var inputEnded = false
                var outputEnded = false
                var outputFormat = inputFormat
                var resampler: StreamingMonoResampler? = null
                var writtenSamples = 0L

                while (!outputEnded) {
                    coroutineContext.ensureActive()
                    if (!inputEnded) {
                        val inputIndex = codec.dequeueInputBuffer(CODEC_TIMEOUT_US)
                        if (inputIndex >= 0) {
                            val inputBuffer = requireNotNull(codec.getInputBuffer(inputIndex))
                            val sampleSize = extractor.readSampleData(inputBuffer, 0)
                            if (sampleSize < 0) {
                                codec.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    0,
                                    0L,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                                )
                                inputEnded = true
                            } else {
                                codec.queueInputBuffer(
                                    inputIndex,
                                    0,
                                    sampleSize,
                                    extractor.sampleTime.coerceAtLeast(0L),
                                    0,
                                )
                                extractor.advance()
                            }
                        }
                    }

                    when (val outputIndex = codec.dequeueOutputBuffer(bufferInfo, CODEC_TIMEOUT_US)) {
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            outputFormat = codec.outputFormat
                            resampler = StreamingMonoResampler(
                                inputSampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                                inputChannels = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
                            )
                        }
                        MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                        else -> if (outputIndex >= 0) {
                            val outputBuffer = codec.getOutputBuffer(outputIndex)
                            if (bufferInfo.size > 0 && outputBuffer != null) {
                                outputBuffer.position(bufferInfo.offset)
                                outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                                val samples = decodePcm(
                                    buffer = outputBuffer.slice().order(ByteOrder.LITTLE_ENDIAN),
                                    encoding = outputFormat.integerOrDefault(
                                        MediaFormat.KEY_PCM_ENCODING,
                                        AudioFormat.ENCODING_PCM_16BIT,
                                    ),
                                )
                                val activeResampler = resampler ?: StreamingMonoResampler(
                                    inputSampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                                    inputChannels = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
                                ).also { resampler = it }
                                val targetSamples = activeResampler.process(samples)
                                writePcm16(output, targetSamples)
                                writtenSamples += targetSamples.size
                                onProgress(
                                    AudioDecodeProgress(
                                        presentationTimeMicros = bufferInfo.presentationTimeUs,
                                        durationMicros = durationMicros,
                                        writtenSamples = writtenSamples,
                                    ),
                                )
                            }
                            outputEnded = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                            codec.releaseOutputBuffer(outputIndex, false)
                        }
                    }
                }
                output.force(true)
                AudioDecodeResult.Success(
                    artifact = PcmAudioArtifact(
                        path = targetPcm,
                        sampleRate = TARGET_SAMPLE_RATE,
                        channels = 1,
                        sampleCount = writtenSamples,
                        durationMillis = writtenSamples * 1_000L / TARGET_SAMPLE_RATE,
                    ),
                    sourceMime = mime,
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            AudioDecodeResult.Failure(
                category = AudioDecodeFailureCategory.DECODER,
                message = "无法解码此音视频，请尝试 MP4、M4A、MP3 或 WAV 文件",
                technicalDetail = error.message,
            )
        } finally {
            runCatching { codec?.stop() }
            codec?.release()
            extractor.release()
        }
    }

    private fun decodePcm(buffer: ByteBuffer, encoding: Int): FloatArray = when (encoding) {
        AudioFormat.ENCODING_PCM_FLOAT -> FloatArray(buffer.remaining() / 4) { buffer.float }
        AudioFormat.ENCODING_PCM_8BIT -> FloatArray(buffer.remaining()) {
            ((buffer.get().toInt() and 0xff) - 128) / 128.0f
        }
        AudioFormat.ENCODING_PCM_16BIT -> FloatArray(buffer.remaining() / 2) {
            buffer.short / 32768.0f
        }
        AudioFormat.ENCODING_PCM_24BIT_PACKED -> FloatArray(buffer.remaining() / 3) {
            val value = (buffer.get().toInt() and 0xff) or
                ((buffer.get().toInt() and 0xff) shl 8) or
                (buffer.get().toInt() shl 16)
            value / 8_388_608.0f
        }
        AudioFormat.ENCODING_PCM_32BIT -> FloatArray(buffer.remaining() / 4) {
            buffer.int / 2_147_483_648.0f
        }
        else -> throw IOException("不支持的 PCM 编码：$encoding")
    }

    private fun writePcm16(channel: FileChannel, samples: FloatArray) {
        val bytes = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
        samples.forEach { sample ->
            val value = (sample.coerceIn(-1.0f, 1.0f) * 32767.0f).roundToInt()
            bytes.putShort(value.toShort())
        }
        bytes.flip()
        while (bytes.hasRemaining()) channel.write(bytes)
    }

    private fun MediaFormat.longOrNull(key: String): Long? =
        if (containsKey(key)) getLong(key) else null

    private fun MediaFormat.integerOrDefault(key: String, default: Int): Int =
        if (containsKey(key)) getInteger(key) else default

    companion object {
        private const val TARGET_SAMPLE_RATE = 16_000
        private const val CODEC_TIMEOUT_US = 10_000L
    }
}

data class PcmAudioArtifact(
    val path: Path,
    val sampleRate: Int,
    val channels: Int,
    val sampleCount: Long,
    val durationMillis: Long,
)

data class AudioDecodeProgress(
    val presentationTimeMicros: Long,
    val durationMicros: Long?,
    val writtenSamples: Long,
)

enum class AudioDecodeFailureCategory {
    NO_AUDIO_TRACK,
    DECODER,
}

sealed interface AudioDecodeResult {
    data class Success(
        val artifact: PcmAudioArtifact,
        val sourceMime: String,
    ) : AudioDecodeResult

    data class Failure(
        val category: AudioDecodeFailureCategory,
        val message: String,
        val technicalDetail: String? = null,
    ) : AudioDecodeResult
}

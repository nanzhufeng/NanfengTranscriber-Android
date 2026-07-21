package com.nanzhufeng.transcriber.data.media

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

class EmbeddedSubtitleParserTest {
    @Test
    fun `tx3g removes length prefix and keeps mixed Chinese English text`() {
        val text = "欢迎使用 Nanfeng Transcriber"
        val encoded = text.toByteArray(StandardCharsets.UTF_8)
        val sample = ByteBuffer.allocate(encoded.size + 2).order(ByteOrder.BIG_ENDIAN)
            .putShort(encoded.size.toShort())
            .put(encoded)
            .array()

        assertEquals(text, decodeSubtitleSample("text/3gpp-tt", sample))
    }

    @Test
    fun `mp4 webvtt reads payl box`() {
        val text = "Ask what you can do"
        val payl = mp4Box("payl", text.toByteArray(StandardCharsets.UTF_8))
        val vttc = mp4Box("vttc", payl)

        assertEquals(text, decodeSubtitleSample("application/x-mp4-vtt", vttc))
    }

    @Test
    fun `subtitle control lines and markup are removed`() {
        val payload = "WEBVTT\n00:00:00.000 --> 00:00:02.000\n<b>Hello</b> &amp; 你好"
            .toByteArray(StandardCharsets.UTF_8)

        assertEquals("Hello & 你好", decodeSubtitleSample("text/vtt", payload))
    }

    private fun mp4Box(type: String, content: ByteArray): ByteArray =
        ByteBuffer.allocate(content.size + 8).order(ByteOrder.BIG_ENDIAN)
            .putInt(content.size + 8)
            .put(type.toByteArray(StandardCharsets.US_ASCII))
            .put(content)
            .array()
}

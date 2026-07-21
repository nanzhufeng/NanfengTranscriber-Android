package com.nanzhufeng.transcriber.data.media

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

internal fun decodeSubtitleSample(mimeType: String, payload: ByteArray): String? {
    if (payload.isEmpty()) return null
    val raw = when (mimeType.lowercase()) {
        "application/x-quicktime-tx3g", "text/3gpp", "text/3gpp-tt" -> decodeTx3g(payload)
        "application/x-mp4-vtt" -> findMp4VttPayload(payload)
        else -> decodeText(payload)
    }
    return raw
        ?.replace("\\N", "\n")
        ?.replace(Regex("\\{\\\\[^}]*}"), "")
        ?.replace(Regex("<[^>]+>"), "")
        ?.replace("&nbsp;", " ")
        ?.replace("&amp;", "&")
        ?.replace("&lt;", "<")
        ?.replace("&gt;", ">")
        ?.lineSequence()
        ?.map(String::trim)
        ?.filterNot(::isSubtitleControlLine)
        ?.joinToString("\n")
        ?.trim()
        ?.takeIf(String::isNotBlank)
}

private fun decodeTx3g(payload: ByteArray): String {
    if (payload.size < 2) return decodeText(payload)
    val declaredLength = ((payload[0].toInt() and 0xff) shl 8) or (payload[1].toInt() and 0xff)
    if (declaredLength <= 0 || declaredLength > payload.size - 2) return decodeText(payload)
    return decodeText(payload.copyOfRange(2, 2 + declaredLength))
}

private fun findMp4VttPayload(payload: ByteArray): String? = findBoxPayload(payload, "payl")
    ?.let(::decodeText)

private fun findBoxPayload(bytes: ByteArray, wantedType: String): ByteArray? {
    var offset = 0
    while (offset + 8 <= bytes.size) {
        val size = ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.BIG_ENDIAN).int
        if (size < 8 || offset + size > bytes.size) break
        val type = String(bytes, offset + 4, 4, StandardCharsets.US_ASCII)
        val content = bytes.copyOfRange(offset + 8, offset + size)
        if (type == wantedType) return content
        if (type == "vttc") findBoxPayload(content, wantedType)?.let { return it }
        offset += size
    }
    return null
}

private fun decodeText(payload: ByteArray): String {
    val charset = when {
        payload.size >= 2 && payload[0] == 0xfe.toByte() && payload[1] == 0xff.toByte() ->
            StandardCharsets.UTF_16BE
        payload.size >= 2 && payload[0] == 0xff.toByte() && payload[1] == 0xfe.toByte() ->
            StandardCharsets.UTF_16LE
        else -> StandardCharsets.UTF_8
    }
    val bomBytes = if (charset == StandardCharsets.UTF_8) 0 else 2
    val decoded = String(payload, bomBytes, payload.size - bomBytes, charset as Charset)
    if (!decoded.startsWith("Dialogue:", ignoreCase = true)) return decoded
    return decoded.substringAfter(':').split(',', limit = 10).lastOrNull().orEmpty()
}

private fun isSubtitleControlLine(line: String): Boolean {
    if (line.isBlank()) return true
    if (line.equals("WEBVTT", ignoreCase = true)) return true
    if (line.startsWith("NOTE", ignoreCase = true) || line.startsWith("STYLE", ignoreCase = true)) return true
    if (line.all(Char::isDigit)) return true
    return "-->" in line
}

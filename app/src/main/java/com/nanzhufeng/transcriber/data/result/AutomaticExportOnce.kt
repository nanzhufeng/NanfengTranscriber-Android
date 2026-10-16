package com.nanzhufeng.transcriber.data.result

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.FileAlreadyExistsException
import java.util.concurrent.CancellationException

/** Claim before external IO: an interrupted write must never create another automatic copy. */
class AutomaticExportOnce {
    fun run(directory: Path, export: () -> String): String {
        val receipt = directory.resolve("automatic-export.receipt")
        try {
            Files.createFile(receipt)
        } catch (_: FileAlreadyExistsException) {
            return Files.newBufferedReader(receipt).use { it.readText() }.ifBlank { "自动导出曾中断，请检查输出目录，必要时手动导出" }
        }
        val message = try {
            export()
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            "自动导出失败，完整结果已保留，请检查默认目录后手动导出"
        }
        Files.newBufferedWriter(receipt).use { it.write(message) }
        return message
    }
}

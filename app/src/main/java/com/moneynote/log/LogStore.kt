package com.moneynote.log

import android.util.Log
import java.io.File
import java.time.ZoneId

/**
 * 日志的本地文件存储，带按大小轮转。
 *
 * 写入发生在 [AppLog] 的单线程执行器上，读取发生在 UI 线程，
 * 因此所有公开方法都按同一把锁同步。
 *
 * 注意：这里**绝不**调用 [AppLog]。日志系统自身出错时如果再回调 AppLog，
 * 会形成无限递归。内部异常一律直接写 logcat。
 */
class LogStore(
    private val directory: File,
    val maxFileBytes: Long = DEFAULT_MAX_FILE_BYTES,
    val maxBackups: Int = DEFAULT_MAX_BACKUPS,
    private val zone: ZoneId = ZoneId.systemDefault(),
) {

    private val lock = Any()

    val directoryPath: String get() = directory.absolutePath

    fun currentFile(): File = File(directory, FILE_NAME)

    fun append(entry: LogEntry): Boolean = synchronized(lock) {
        try {
            ensureDirectory()
            val current = currentFile()
            if (current.exists() && current.length() >= maxFileBytes) {
                rotate()
            }
            currentFile().appendText(LogFormatter.format(entry, zone), Charsets.UTF_8)
            true
        } catch (t: Throwable) {
            Log.w(TAG, "写入日志文件失败", t)
            false
        }
    }

    /** 按时间顺序读出全部日志：先旧备份，再当前文件。 */
    fun readAll(): String = synchronized(lock) {
        buildString {
            for (index in maxBackups downTo 1) {
                append(readQuietly(backupFile(index)))
            }
            append(readQuietly(currentFile()))
        }
    }

    /** 只取最后 [maxLines] 行，用于界面展示，避免一次渲染上万行。 */
    fun readTail(maxLines: Int): String {
        val text = readAll()
        if (text.isEmpty()) return ""
        val lines = text.trimEnd('\n').lines()
        if (lines.size <= maxLines) return lines.joinToString("\n")
        return lines.takeLast(maxLines).joinToString("\n")
    }

    fun clear() = synchronized(lock) {
        currentFile().delete()
        for (index in 1..maxBackups) {
            backupFile(index).delete()
        }
        Unit
    }

    fun totalBytes(): Long = synchronized(lock) {
        var total = 0L
        val current = currentFile()
        if (current.exists()) total += current.length()
        for (index in 1..maxBackups) {
            val file = backupFile(index)
            if (file.exists()) total += file.length()
        }
        total
    }

    private fun ensureDirectory() {
        if (!directory.exists()) directory.mkdirs()
    }

    private fun backupFile(index: Int): File = File(directory, "$FILE_NAME.$index")

    private fun readQuietly(file: File): String =
        if (file.exists()) runCatching { file.readText(Charsets.UTF_8) }.getOrDefault("") else ""

    /** moneynote.log → .1 → .2 → …，最旧的一份被丢弃。 */
    private fun rotate() {
        for (index in maxBackups - 1 downTo 1) {
            val source = backupFile(index)
            if (source.exists()) moveOverwriting(source, backupFile(index + 1))
        }
        moveOverwriting(currentFile(), backupFile(1))
    }

    private fun moveOverwriting(source: File, target: File) {
        if (target.exists()) target.delete()
        if (!source.renameTo(target)) {
            runCatching {
                source.copyTo(target, overwrite = true)
                source.delete()
            }
        }
    }

    companion object {
        const val FILE_NAME: String = "moneynote.log"
        const val DEFAULT_MAX_FILE_BYTES: Long = 512L * 1024
        const val DEFAULT_MAX_BACKUPS: Int = 3

        private const val TAG = "MoneyNote/AppLog"
    }
}

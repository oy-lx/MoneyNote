package com.moneynote.log

import java.io.PrintWriter
import java.io.StringWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 把 [LogEntry] 渲染成一行文本。
 *
 * 刻意不依赖任何 Android API，便于单元测试；也不做 IO，
 * 所有落盘行为都交给 [LogStore]。
 */
object LogFormatter {

    private val timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    /** 堆栈最多保留这么多行，避免异常循环把日志文件撑爆。 */
    const val MAX_STACK_LINES: Int = 40

    fun format(entry: LogEntry, zone: ZoneId = ZoneId.systemDefault()): String = buildString {
        append(Instant.ofEpochMilli(entry.timestampMillis).atZone(zone).format(timeFormat))
        append(' ')
        append(entry.level.label)
        append('/')
        append(entry.tag)
        append(": ")
        append(entry.message)
        entry.throwable?.let { throwable ->
            append('\n')
            append(indent(formatThrowable(throwable)))
        }
        append('\n')
    }

    fun formatThrowable(throwable: Throwable, maxLines: Int = MAX_STACK_LINES): String {
        val writer = StringWriter()
        throwable.printStackTrace(PrintWriter(writer))
        val lines = writer.toString().trimEnd().lines()
        if (lines.size <= maxLines) return lines.joinToString("\n")
        val omitted = lines.size - maxLines
        return (lines.take(maxLines) + "    ... 已省略 $omitted 行堆栈").joinToString("\n")
    }

    /** 堆栈多行时整体缩进，读起来和上面的正文有层次。 */
    private fun indent(text: String): String =
        text.lines().joinToString("\n") { line -> "    $line" }
}

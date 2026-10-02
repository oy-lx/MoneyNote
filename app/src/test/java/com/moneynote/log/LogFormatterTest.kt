package com.moneynote.log

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class LogFormatterTest {

    private val utc = ZoneOffset.UTC

    /** 2025-06-15T08:30:00.123Z */
    private val fixedMillis = Instant.parse("2025-06-15T08:30:00.123Z").toEpochMilli()

    private fun entry(
        level: LogLevel = LogLevel.INFO,
        tag: String = "HomeViewModel",
        message: String = "保存成功",
        throwable: Throwable? = null,
    ) = LogEntry(fixedMillis, level, tag, message, throwable)

    @Test
    fun `基础格式为 时间 级别 标签 消息`() {
        assertEquals(
            "2025-06-15 08:30:00.123 I/HomeViewModel: 保存成功\n",
            LogFormatter.format(entry(), utc),
        )
    }

    @Test
    fun `四个级别各有唯一标记且顺序固定`() {
        assertEquals(listOf("D", "I", "W", "E"), LogLevel.entries.map { it.label })
    }

    @Test
    fun `每条日志都以换行结尾`() {
        LogLevel.entries.forEach { level ->
            assertTrue(LogFormatter.format(entry(level = level), utc).endsWith("\n"))
        }
    }

    @Test
    fun `无异常时只有一行`() {
        val lines = LogFormatter.format(entry(level = LogLevel.WARN), utc).trimEnd('\n').lines()
        assertEquals(1, lines.size)
    }

    @Test
    fun `多行消息原样保留`() {
        val text = LogFormatter.format(entry(message = "第一行\n第二行"), utc)
        assertTrue(text.contains("第一行\n第二行"))
    }

    @Test
    fun `异常另起一段且整体缩进`() {
        val text = LogFormatter.format(
            entry(level = LogLevel.ERROR, message = "保存失败", throwable = IllegalStateException("boom")),
            utc,
        )
        val lines = text.trimEnd('\n').lines()
        assertEquals("2025-06-15 08:30:00.123 E/HomeViewModel: 保存失败", lines.first())
        assertTrue(lines[1].startsWith("    java.lang.IllegalStateException: boom"))
        assertTrue("堆栈每一行都应有缩进", lines.drop(1).all { it.startsWith("    ") })
    }

    @Test
    fun `未超限的堆栈完整保留且不加省略提示`() {
        val text = LogFormatter.formatThrowable(IllegalStateException("boom"), maxLines = 500)
        assertTrue(text.startsWith("java.lang.IllegalStateException: boom"))
        assertFalse(text.contains("已省略"))
    }

    @Test
    fun `超长堆栈被截断并标注省略行数`() {
        val text = LogFormatter.formatThrowable(deepThrowable(200), maxLines = 10)
        val lines = text.lines()
        assertEquals("截断后应为 10 行堆栈 + 1 行省略提示", 11, lines.size)
        assertTrue(lines.last().contains("已省略"))
    }

    @Test
    fun `恰好等于上限时不截断`() {
        val throwable = IllegalStateException("boom")
        val total = LogFormatter.formatThrowable(throwable, maxLines = 500).lines().size
        val text = LogFormatter.formatThrowable(throwable, maxLines = total)
        assertFalse(text.contains("已省略"))
        assertEquals(total, text.lines().size)
    }

    @Test
    fun `超过上限一行即开始截断`() {
        val throwable = IllegalStateException("boom")
        val total = LogFormatter.formatThrowable(throwable, maxLines = 500).lines().size
        val text = LogFormatter.formatThrowable(throwable, maxLines = total - 1)
        assertTrue(text.contains("已省略"))
        assertEquals("total-1 行堆栈 + 1 行提示", total, text.lines().size)
    }

    /** 构造一条 depth 层嵌套的 cause 链，用来撑出很长的堆栈。 */
    private fun deepThrowable(depth: Int): Throwable {
        var current: Throwable = IllegalStateException("底部")
        repeat(depth) { index ->
            current = RuntimeException("第 $index 层", current)
        }
        return current
    }
}

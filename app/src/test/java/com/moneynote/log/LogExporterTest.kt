package com.moneynote.log

import com.moneynote.util.DeviceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class LogExporterTest {

    private val device = DeviceInfo(
        appVersion = "1.0.0 (1)",
        androidVersion = "Android 14 (API 34)",
        deviceModel = "Google Pixel 7",
    )

    private val fixedMillis = Instant.parse("2025-06-15T08:30:00Z").toEpochMilli()

    @Test
    fun `导出文档包含完整环境信息`() {
        val text = LogExporter.buildDocument(device, fixedMillis, "line1\n", ZoneOffset.UTC)

        assertTrue(text.startsWith("轻记账 运行日志\n"))
        assertTrue(text.contains("导出时间：2025-06-15 08:30:00\n"))
        assertTrue(text.contains("应用版本：1.0.0 (1)\n"))
        assertTrue(text.contains("系统版本：Android 14 (API 34)\n"))
        assertTrue(text.contains("设备型号：Google Pixel 7\n"))
    }

    @Test
    fun `日志正文附在环境信息之后且只保留一个结尾换行`() {
        val text = LogExporter.buildDocument(device, fixedMillis, "line1\nline2\n\n", ZoneOffset.UTC)

        assertTrue(text.endsWith("line1\nline2\n"))
        assertEquals(1, text.length - text.trimEnd('\n').length)
    }

    @Test
    fun `没有日志时给出占位文案`() {
        val text = LogExporter.buildDocument(device, fixedMillis, "", ZoneOffset.UTC)
        assertTrue(text.endsWith("（暂无日志）\n"))
    }

    @Test
    fun `只有空白字符也按无内容处理`() {
        val text = LogExporter.buildDocument(device, fixedMillis, "   \n\n  ", ZoneOffset.UTC)
        assertTrue(text.contains("（暂无日志）"))
    }

    @Test
    fun `字节数按单位换算`() {
        assertEquals("0 B", LogExporter.formatBytes(0L))
        assertEquals("512 B", LogExporter.formatBytes(512L))
        assertEquals("1023 B", LogExporter.formatBytes(1023L))
        assertEquals("1.0 KB", LogExporter.formatBytes(1024L))
        assertEquals("1.5 KB", LogExporter.formatBytes(1536L))
        assertEquals("1.0 MB", LogExporter.formatBytes(1024L * 1024L))
        assertEquals("2.5 MB", LogExporter.formatBytes((2.5 * 1024 * 1024).toLong()))
    }

    @Test
    fun `负数按零处理而不是显示负值`() {
        assertEquals("0 B", LogExporter.formatBytes(-1L))
    }
}

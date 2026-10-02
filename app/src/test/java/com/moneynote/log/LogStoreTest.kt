package com.moneynote.log

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.ZoneOffset

class LogStoreTest {

    private lateinit var directory: File

    @Before
    fun setUp() {
        directory = File(
            System.getProperty("java.io.tmpdir"),
            "moneynote-logstore-test-${System.nanoTime()}",
        )
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    private fun store(
        maxFileBytes: Long = LogStore.DEFAULT_MAX_FILE_BYTES,
        maxBackups: Int = LogStore.DEFAULT_MAX_BACKUPS,
    ) = LogStore(directory, maxFileBytes, maxBackups, ZoneOffset.UTC)

    private fun entry(message: String, level: LogLevel = LogLevel.INFO) =
        LogEntry(0L, level, "T", message)

    /** 每条日志形如 "1970-01-01 00:00:00.000 I/T: xxx\n"，前缀固定 30 字节。 */
    private fun messages(store: LogStore): List<String> =
        store.readAll().trimEnd('\n').lines().map { it.substringAfter(": ") }

    private fun tailMessages(store: LogStore, count: Int): List<String> =
        store.readTail(count).lines().map { it.substringAfter(": ") }

    @Test
    fun `目录不存在时自动创建并写入`() {
        val nested = File(directory, "a/b/c")
        val logStore = LogStore(nested, zone = ZoneOffset.UTC)

        assertTrue(logStore.append(entry("hi")))
        assertTrue(nested.exists())
        assertEquals(listOf("hi"), messages(logStore))
    }

    @Test
    fun `写入后可以按顺序读回`() {
        val logStore = store()
        listOf("a", "b", "c").forEach { logStore.append(entry(it)) }

        assertEquals(listOf("a", "b", "c"), messages(logStore))
    }

    @Test
    fun `达到大小上限后轮转并保留最旧到最新的顺序`() {
        // 单条 31 字节，上限 40：写第二条后即超过，下一条触发轮转
        val logStore = store(maxFileBytes = 40L, maxBackups = 2)
        listOf("a", "b", "c", "d", "e", "f").forEach { logStore.append(entry(it)) }

        assertEquals(listOf("a", "b", "c", "d", "e", "f"), messages(logStore))
        assertTrue("应产生备份文件", File(directory, "${LogStore.FILE_NAME}.1").exists())
        assertTrue("应产生第二份备份", File(directory, "${LogStore.FILE_NAME}.2").exists())
    }

    @Test
    fun `备份数量超过上限时丢弃最旧的一份`() {
        val logStore = store(maxFileBytes = 40L, maxBackups = 2)
        repeat(12) { logStore.append(entry("m$it")) }

        val kept = messages(logStore)
        assertEquals("最多保留 (maxBackups + 1) * 2 条", 6, kept.size)
        assertEquals(listOf("m6", "m7", "m8", "m9", "m10", "m11"), kept)
    }

    @Test
    fun `readTail 只取最后 N 行`() {
        val logStore = store()
        repeat(10) { logStore.append(entry("m$it")) }

        assertEquals(listOf("m7", "m8", "m9"), tailMessages(logStore, 3))
    }

    @Test
    fun `readTail 请求行数超过实际行数时返回全部`() {
        val logStore = store()
        repeat(3) { logStore.append(entry("m$it")) }

        assertEquals(listOf("m0", "m1", "m2"), tailMessages(logStore, 100))
    }

    @Test
    fun `空日志 readTail 返回空串而不是空列表`() {
        assertEquals("", store().readTail(10))
    }

    @Test
    fun `clear 会删除当前文件与所有备份`() {
        val logStore = store(maxFileBytes = 40L, maxBackups = 2)
        repeat(12) { logStore.append(entry("m$it")) }
        assertTrue(logStore.totalBytes() > 0L)

        logStore.clear()

        assertEquals("", logStore.readAll())
        assertEquals(0L, logStore.totalBytes())
        assertTrue(!File(directory, "${LogStore.FILE_NAME}.1").exists())
    }

    @Test
    fun `totalBytes 统计当前文件与备份之和`() {
        val logStore = store(maxFileBytes = 40L, maxBackups = 2)
        listOf("a", "b", "c", "d", "e", "f").forEach { logStore.append(entry(it)) }

        val expected = listOf(logStore.currentFile(), File(directory, "${LogStore.FILE_NAME}.1"), File(directory, "${LogStore.FILE_NAME}.2"))
            .filter { it.exists() }
            .sumOf { it.length() }

        assertEquals(expected, logStore.totalBytes())
    }
}

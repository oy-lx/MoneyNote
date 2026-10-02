package com.moneynote.log

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 全局日志门面。
 *
 * 设计要点：
 * - **两级输出**：始终写 logcat；初始化后同时异步落盘，供用户导出排查问题。
 * - **Release 自动降噪**：非 debuggable 包只保留 WARN / ERROR，DEBUG 与 INFO 完全不产生。
 * - **不阻塞调用方**：文件写入排在单线程执行器上，顺序有保证且不占用 UI 线程。
 * - **进程退出前不丢日志**：崩溃处理器会先 [flush] 再交回系统默认处理。
 *
 * 用法：类里定义 `private const val TAG = "HomeViewModel"`，
 * 然后 `AppLog.info(TAG, "...")`，logcat 里会显示为 `MoneyNote/HomeViewModel`。
 */
object AppLog {

    private const val TAG_PREFIX = "MoneyNote"
    private const val LOG_DIR_NAME = "logs"
    private const val MAX_MESSAGE_LENGTH = 2000
    private const val FLUSH_TIMEOUT_MILLIS = 500L

    @Volatile
    private var store: LogStore? = null

    /** 未初始化时保守取 INFO，避免启动早期的调试日志混进 logcat。 */
    @Volatile
    private var minLevel: LogLevel = LogLevel.INFO

    @Volatile
    private var crashHandlerInstalled = false

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "MoneyNote-Log").apply { isDaemon = true }
    }

    val currentStore: LogStore? get() = store

    val currentMinLevel: LogLevel get() = minLevel

    fun init(context: Context) {
        val debuggable =
            (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        init(LogStore(File(context.filesDir, LOG_DIR_NAME)), debuggable)
    }

    fun init(logStore: LogStore, debuggable: Boolean) {
        store = logStore
        minLevel = if (debuggable) LogLevel.DEBUG else LogLevel.WARN
        info(tag = "AppLog", message = "日志系统就绪：级别=$minLevel，目录=${logStore.directoryPath}")
    }

    /**
     * 安装崩溃处理器：先把异常连同堆栈落盘，再交回原有的处理器，
     * 这样系统默认的崩溃行为（弹窗、上报）完全不受影响。
     */
    fun installCrashHandler() {
        if (crashHandlerInstalled) return
        crashHandlerInstalled = true
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            error(
                tag = "Crash",
                message = "线程 ${thread.name} 发生未捕获异常",
                throwable = throwable,
            )
            // 进程马上就会死，必须等写入完成，否则这条最关键的日志会丢
            flush()
            previous?.uncaughtException(thread, throwable)
        }
    }

    fun debug(tag: String, message: String) {
        log(LogLevel.DEBUG, tag, message, null)
    }

    fun info(tag: String, message: String) {
        log(LogLevel.INFO, tag, message, null)
    }

    fun warn(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.WARN, tag, message, throwable)
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        log(LogLevel.ERROR, tag, message, throwable)
    }

    /** 等待已排队的日志全部写完。超时即放弃，绝不拖住调用方。 */
    fun flush(timeoutMillis: Long = FLUSH_TIMEOUT_MILLIS) {
        runCatching { executor.submit { }.get(timeoutMillis, TimeUnit.MILLISECONDS) }
    }

    private fun log(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        if (level.ordinal < minLevel.ordinal) return

        val text = message.take(MAX_MESSAGE_LENGTH)
        val fullTag = "$TAG_PREFIX/$tag"
        when (level) {
            LogLevel.DEBUG -> Log.d(fullTag, text, throwable)
            LogLevel.INFO -> Log.i(fullTag, text, throwable)
            LogLevel.WARN -> Log.w(fullTag, text, throwable)
            LogLevel.ERROR -> Log.e(fullTag, text, throwable)
        }

        val target = store ?: return
        val entry = LogEntry(
            timestampMillis = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = text,
            throwable = throwable,
        )
        runCatching { executor.execute { target.append(entry) } }
    }
}

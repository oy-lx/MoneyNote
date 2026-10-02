package com.moneynote.log

/** 日志级别。ordinal 用于门槛比较，顺序不可调整。 */
enum class LogLevel(val label: String) {
    DEBUG("D"),
    INFO("I"),
    WARN("W"),
    ERROR("E"),
}

/**
 * 一条日志记录。
 *
 * 时间戳在这里就固定下来，而不是写文件时才取，
 * 这样异步落盘也不会导致时间错乱。
 */
data class LogEntry(
    val timestampMillis: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null,
)

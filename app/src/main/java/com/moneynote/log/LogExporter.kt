package com.moneynote.log

import com.moneynote.util.DeviceInfo
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 把日志拼成一份可导出的文本文件。
 *
 * 带上环境信息是有意的：用户把文件发过来时，
 * 不需要再追问「你什么机型、什么系统版本」。
 */
object LogExporter {

    private val timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun buildDocument(
        deviceInfo: DeviceInfo,
        exportedAtMillis: Long,
        logText: String,
        zone: ZoneId = ZoneId.systemDefault(),
    ): String = buildString {
        append("轻记账 运行日志\n")
        append("导出时间：")
        append(Instant.ofEpochMilli(exportedAtMillis).atZone(zone).format(timeFormat))
        append('\n')
        append("应用版本：").append(deviceInfo.appVersion).append('\n')
        append("系统版本：").append(deviceInfo.androidVersion).append('\n')
        append("设备型号：").append(deviceInfo.deviceModel).append('\n')
        append("==============================\n")
        if (logText.isBlank()) {
            append("（暂无日志）\n")
        } else {
            append(logText.trimEnd('\n')).append('\n')
        }
    }

    /** 把字节数格式化成人看的大小，例如 1536 → "1.5 KB"。 */
    fun formatBytes(bytes: Long): String {
        if (bytes < 0L) return "0 B"
        return when {
            bytes < 1024L -> "$bytes B"
            bytes < 1024L * 1024L ->
                String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            else ->
                String.format(Locale.US, "%.1f MB", bytes / 1024.0 / 1024.0)
        }
    }
}

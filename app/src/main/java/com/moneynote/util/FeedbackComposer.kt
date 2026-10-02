package com.moneynote.util

/** 反馈类型，会写进邮件主题，方便收件时归类。 */
enum class FeedbackCategory(val label: String) {
    SUGGESTION("功能建议"),
    BUG("问题反馈"),
    EXPERIENCE("界面体验"),
    OTHER("其他"),
}

/**
 * 自动附加的运行环境信息。
 * 纯数据类，刻意不依赖 Android API，方便单元测试与复用。
 */
data class DeviceInfo(
    val appVersion: String,
    val androidVersion: String,
    val deviceModel: String,
)

/**
 * 把用户输入拼装成邮件主题与正文。
 *
 * 这里只做纯字符串处理；真正唤起邮件应用的 Intent 构造放在 UI 层，
 * 这样拼装逻辑可以脱离 Android 环境做单元测试。
 */
object FeedbackComposer {

    /** 反馈邮件的收件地址。 */
    const val FEEDBACK_EMAIL: String = "3986107436@qq.com"

    private const val SUBJECT_PREFIX = "[轻记账 反馈]"

    private const val CONTACT_PLACEHOLDER = "（未填写）"

    private const val DIVIDER = "—————————————"

    fun subject(category: FeedbackCategory): String = "$SUBJECT_PREFIX ${category.label}"

    fun body(
        category: FeedbackCategory,
        message: String,
        contact: String,
        deviceInfo: DeviceInfo,
        txnCount: Int,
        includeDeviceInfo: Boolean,
    ): String = buildString {
        append("反馈类型：").append(category.label).append("\n\n")
        append("反馈内容：\n").append(message.trim()).append("\n\n")
        append("联系方式：").append(contact.trim().ifBlank { CONTACT_PLACEHOLDER }).append('\n')

        if (includeDeviceInfo) {
            append('\n').append(DIVIDER).append('\n')
            append("以下信息由 App 自动附加，仅用于定位问题：\n")
            append("应用版本：").append(deviceInfo.appVersion).append('\n')
            append("系统版本：").append(deviceInfo.androidVersion).append('\n')
            append("设备型号：").append(deviceInfo.deviceModel).append('\n')
            append("流水记录数：").append(txnCount).append('\n')
        }
    }
}

package com.moneynote.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackComposerTest {

    private val device = DeviceInfo(
        appVersion = "1.0.0 (1)",
        androidVersion = "Android 14 (API 34)",
        deviceModel = "Google Pixel 7",
    )

    private fun body(
        category: FeedbackCategory = FeedbackCategory.SUGGESTION,
        message: String = "希望支持多账本",
        contact: String = "me@example.com",
        includeDeviceInfo: Boolean = true,
    ) = FeedbackComposer.body(
        category = category,
        message = message,
        contact = contact,
        deviceInfo = device,
        txnCount = 42,
        includeDeviceInfo = includeDeviceInfo,
    )

    @Test
    fun `收件邮箱已固定`() {
        assertEquals("3986107436@qq.com", FeedbackComposer.FEEDBACK_EMAIL)
    }

    @Test
    fun `主题带前缀与反馈类型`() {
        assertEquals("[轻记账 反馈] 功能建议", FeedbackComposer.subject(FeedbackCategory.SUGGESTION))
        assertEquals("[轻记账 反馈] 问题反馈", FeedbackComposer.subject(FeedbackCategory.BUG))
    }

    @Test
    fun `每种反馈类型都有非空标签`() {
        FeedbackCategory.entries.forEach { category ->
            assertTrue(category.label.isNotBlank())
        }
    }

    @Test
    fun `正文包含类型_内容_联系方式`() {
        val text = body(category = FeedbackCategory.BUG, message = "统计页数字对不上")
        assertTrue(text.contains("反馈类型：问题反馈"))
        assertTrue(text.contains("统计页数字对不上"))
        assertTrue(text.contains("me@example.com"))
    }

    @Test
    fun `正文首尾空白会被去掉`() {
        val text = body(message = "   有空白   ", contact = "  a@b.com  ")
        assertTrue(text.contains("反馈内容：\n有空白\n"))
        assertTrue(text.contains("联系方式：a@b.com"))
    }

    @Test
    fun `未填联系方式时给出占位文案`() {
        val text = body(contact = "")
        assertTrue(text.contains("联系方式：（未填写）"))
    }

    @Test
    fun `勾选时附带完整环境信息`() {
        val text = body(includeDeviceInfo = true)
        assertTrue(text.contains("应用版本：1.0.0 (1)"))
        assertTrue(text.contains("系统版本：Android 14 (API 34)"))
        assertTrue(text.contains("设备型号：Google Pixel 7"))
        assertTrue(text.contains("流水记录数：42"))
    }

    @Test
    fun `取消勾选时不出现任何环境信息`() {
        val text = body(includeDeviceInfo = false)
        assertFalse(text.contains("应用版本"))
        assertFalse(text.contains("系统版本"))
        assertFalse(text.contains("设备型号"))
        assertFalse(text.contains("流水记录数"))
        assertFalse(text.contains("流水记录数：42"))
    }

    @Test
    fun `取消勾选后正文仍保留用户填写的内容`() {
        val text = body(includeDeviceInfo = false)
        assertTrue(text.contains("反馈类型：功能建议"))
        assertTrue(text.contains("希望支持多账本"))
        assertTrue(text.contains("me@example.com"))
    }
}

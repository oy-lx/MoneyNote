package com.moneynote.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormattersTest {

    // ------------------------------------------------------------ 金额解析

    @Test
    fun `parse 整数金额`() {
        assertEquals(1200L, parseYuanToCents("12"))
    }

    @Test
    fun `parse 两位小数金额`() {
        assertEquals(1234L, parseYuanToCents("12.34"))
    }

    @Test
    fun `parse 一位小数按角补齐`() {
        assertEquals(1250L, parseYuanToCents("12.5"))
    }

    @Test
    fun `parse 末尾小数点视为整数`() {
        assertEquals(1200L, parseYuanToCents("12."))
    }

    @Test
    fun `parse 忽略千分位逗号`() {
        assertEquals(123456L, parseYuanToCents("1,234.56"))
    }

    @Test
    fun `parse 空串返回 null`() {
        assertNull(parseYuanToCents(""))
        assertNull(parseYuanToCents("   "))
    }

    @Test
    fun `parse 非数字返回 null`() {
        assertNull(parseYuanToCents("abc"))
        assertNull(parseYuanToCents("12a"))
        assertNull(parseYuanToCents("-5"))
    }

    @Test
    fun `parse 超过两位小数返回 null`() {
        assertNull(parseYuanToCents("1.234"))
    }

    @Test
    fun `parse 超过九位整数返回 null`() {
        assertNull(parseYuanToCents("1234567890"))
    }

    @Test
    fun `parse 零值合法`() {
        assertEquals(0L, parseYuanToCents("0"))
        assertEquals(0L, parseYuanToCents("0.00"))
    }

    @Test
    fun `parse 边界 999999999_99`() {
        assertEquals(99_999_999_999L, parseYuanToCents("999999999.99"))
    }

    // ------------------------------------------------------------ 金额格式化

    @Test
    fun `format 带千分位与两位小数`() {
        assertEquals("1,234.56", formatCents(123456L))
        assertEquals("0.05", formatCents(5L))
        assertEquals("0.00", formatCents(0L))
    }

    @Test
    fun `format 取绝对值 负数不会出现减号`() {
        assertEquals("5.00", formatCents(-500L))
    }

    @Test
    fun `formatPlain 不含千分位`() {
        assertEquals("1234.56", formatCentsPlain(123456L))
        assertEquals("12.00", formatCentsPlain(1200L))
    }

    @Test
    fun `formatSigned 支出为负 收入为正`() {
        assertEquals("-5.00", formatSignedCents(500L, isExpense = true))
        assertEquals("+5.00", formatSignedCents(500L, isExpense = false))
    }

    @Test
    fun `format 与 parse 互为逆运算`() {
        val cents = 987_654L
        assertEquals(cents, parseYuanToCents(formatCentsPlain(cents)))
    }
}

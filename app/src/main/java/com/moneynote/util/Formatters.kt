package com.moneynote.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

// 固定使用 Locale.US 的分隔符：金额展示不应随系统区域变化
// （部分区域会用 '.' 分组、',' 作小数点）。String.format 本身线程安全，
// 也就避免了 DecimalFormat 非线程安全带来的额外同步成本。
private const val MONEY_PATTERN = "%,.2f"
private const val PLAIN_PATTERN = "%.2f"

private val fullDate = DateTimeFormatter.ofPattern("yyyy年M月d日")
private val monthDay = DateTimeFormatter.ofPattern("M月d日")
private val dayOnly = DateTimeFormatter.ofPattern("d日")
private val yearMonth = DateTimeFormatter.ofPattern("yyyy年M月")
private val weekdayNames = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

/** 分 → 带千分位的元字符串，例如 1234567 → "12,345.67"。 */
fun formatCents(cents: Long): String =
    String.format(Locale.US, MONEY_PATTERN, abs(cents) / 100.0)

/** 分 → 固定两位小数的元字符串（用于输入框回填）。 */
fun formatCentsPlain(cents: Long): String =
    String.format(Locale.US, PLAIN_PATTERN, abs(cents) / 100.0)

/** 带正负号的金额，支出为负、收入为正。 */
fun formatSignedCents(cents: Long, isExpense: Boolean): String =
    (if (isExpense) "-" else "+") + formatCents(cents)

/** "12.34" / "12." / "12" → 1234 分；非法输入返回 null。 */
fun parseYuanToCents(text: String): Long? {
    val trimmed = text.trim().replace(",", "")
    if (trimmed.isEmpty()) return null
    if (!Regex("^\\d{0,9}(\\.\\d{0,2})?$").matches(trimmed)) return null
    val value = trimmed.toDoubleOrNull() ?: return null
    return Math.round(value * 100)
}

fun formatFullDate(date: LocalDate): String = date.format(fullDate)

fun formatMonthDay(date: LocalDate): String = date.format(monthDay)

fun formatDay(date: LocalDate): String = date.format(dayOnly)

fun formatYearMonth(month: YearMonth): String = month.format(yearMonth)

fun formatWeekday(date: LocalDate): String = weekdayNames[date.dayOfWeek.value - 1]

/** 相对今天的友好描述，用于流水分组标题。 */
fun formatRelativeDay(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
    today -> "今天"
    today.minusDays(1) -> "昨天"
    today.minusDays(2) -> "前天"
    else -> formatWeekday(date)
}

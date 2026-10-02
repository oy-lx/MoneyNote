package com.moneynote.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------- 品牌主色（墨绿） ----------------
val Teal10 = Color(0xFF00201A)
val Teal20 = Color(0xFF00382E)
val Teal30 = Color(0xFF005143)
val Teal40 = Color(0xFF2E7D6B)
val Teal80 = Color(0xFF6FD3B8)
val Teal90 = Color(0xFFB8E6D8)

// ---------------- 中性色 ----------------
val NeutralLightBg = Color(0xFFF6FBF7)
val NeutralLightSurface = Color(0xFFFFFFFF)
val NeutralLightSurfaceVariant = Color(0xFFDBE5DE)
val NeutralDarkBg = Color(0xFF101418)
val NeutralDarkSurface = Color(0xFF171D1A)
val NeutralDarkSurfaceVariant = Color(0xFF3F4945)

// ---------------- 语义色 ----------------
/** 支出：红 */
val ExpenseRed = Color(0xFFD3574F)
val ExpenseRedDark = Color(0xFFFFB4AB)

/** 收入：绿 */
val IncomeGreen = Color(0xFF2E9E6B)
val IncomeGreenDark = Color(0xFF7BDBA9)

/** 预算告警 */
val WarnAmber = Color(0xFFE0952F)

/** 图表配色：色相均匀分布，相邻色差明显，便于区分分类。 */
val ChartPalette = listOf(
    Color(0xFF2E7D6B),
    Color(0xFF3E7CB1),
    Color(0xFFE0952F),
    Color(0xFFD3574F),
    Color(0xFF8E6FC4),
    Color(0xFF35949B),
    Color(0xFFC2659B),
    Color(0xFF6D9A3F),
    Color(0xFFB0743A),
    Color(0xFF5B7FBF),
    Color(0xFF9AA83C),
    Color(0xFF7A6A9E),
)

fun chartColorAt(index: Int): Color = ChartPalette[index % ChartPalette.size]

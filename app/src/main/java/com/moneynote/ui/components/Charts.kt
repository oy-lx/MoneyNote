package com.moneynote.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class ChartSlice(
    val label: String,
    val value: Long,
    val color: Color,
)

/**
 * 环形图。全部用 Canvas 手绘，不引入第三方图表库，
 * 既能控制视觉，也避免额外依赖带来的体积与兼容问题。
 */
@Composable
fun DonutChart(
    slices: List<ChartSlice>,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 22.dp,
    gapDegrees: Float = 1.8f,
    center: @Composable BoxScope.() -> Unit = {},
) {
    val emptyTrack = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val diameter = minOf(size.width, size.height) - stroke
            if (diameter <= 0f) return@Canvas
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            val total = slices.sumOf { it.value }

            if (total <= 0L || slices.isEmpty()) {
                drawArc(
                    color = emptyTrack,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke),
                )
                return@Canvas
            }

            val gap = if (slices.size > 1) gapDegrees else 0f
            var startAngle = -90f
            slices.forEach { slice ->
                val sweep = slice.value.toFloat() / total.toFloat() * 360f
                drawArc(
                    color = slice.color,
                    startAngle = startAngle + gap / 2f,
                    sweepAngle = (sweep - gap).coerceAtLeast(0.8f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                )
                startAngle += sweep
            }
        }
        center()
    }
}

/** 柱状图，用于展示每日/每月趋势。 */
@Composable
fun BarChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    highlightIndex: Int = -1,
    highlightColor: Color = MaterialTheme.colorScheme.tertiary,
) {
    val maxValue = values.maxOrNull() ?: 0f
    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val count = values.size
        val gap = if (count > 1) size.width * 0.012f else 0f
        val barWidth = (size.width - gap * (count - 1)) / count
        if (barWidth <= 0f) return@Canvas
        val corner = CornerRadius(barWidth / 2.2f, barWidth / 2.2f)
        val minHeight = size.height * 0.03f

        values.forEachIndexed { index, value ->
            val ratio = if (maxValue > 0f) (value / maxValue).coerceIn(0f, 1f) else 0f
            val barHeight = (size.height * ratio).coerceAtLeast(minHeight)
            drawRoundRect(
                color = if (index == highlightIndex) highlightColor else color,
                topLeft = Offset(index * (barWidth + gap), size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = corner,
            )
        }
    }
}

/** 预算完成度圆环。 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 12.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val diameter = minOf(size.width, size.height) - stroke
            if (diameter <= 0f) return@Canvas
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke),
            )
            val clamped = progress.coerceIn(0f, 1f)
            if (clamped > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * clamped,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

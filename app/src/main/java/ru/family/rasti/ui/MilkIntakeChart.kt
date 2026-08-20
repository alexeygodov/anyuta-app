package ru.family.rasti.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import ru.family.rasti.data.FoodEntry
import ru.family.rasti.feeding.MilkGuide
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.max

private data class IntakePoint(val minute: Int, val totalMl: Float)

@Composable
internal fun MilkIntakeChart(guide: MilkGuide?, entries: List<FoodEntry>) {
    val actualColor = MaterialTheme.colorScheme.primary
    val rangeColor = MaterialTheme.colorScheme.tertiaryContainer
    val rangeLineColor = MaterialTheme.colorScheme.tertiary
    val outlineColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val points = cumulativePoints(entries)
    val consumed = points.lastOrNull()?.totalMl ?: 0f

    Canvas(Modifier.fillMaxWidth().height(205.dp)) {
        val left = 40.dp.toPx()
        val right = size.width - 8.dp.toPx()
        val top = 14.dp.toPx()
        val bottom = size.height - 28.dp.toPx()
        val maxY = max(max(guide?.maximumMl?.toFloat() ?: 0f, consumed), 500f) * 1.12f

        fun x(minute: Int): Float = left + minute.coerceIn(0, 1440) / 1440f * (right - left)
        fun y(value: Float): Float = bottom - value.coerceAtLeast(0f) / maxY * (bottom - top)

        val bandTop = guide?.let { y(it.maximumMl.toFloat()) }
        val bandBottom = guide?.let { y(it.minimumMl.toFloat()) }
        if (bandTop != null && bandBottom != null) {
            drawRoundRect(
                color = rangeColor.copy(alpha = .55f),
                topLeft = Offset(left, bandTop),
                size = Size(right - left, bandBottom - bandTop),
                cornerRadius = CornerRadius(10.dp.toPx()),
            )
            val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 7f))
            drawLine(
                rangeLineColor.copy(alpha = .85f),
                Offset(left, bandTop),
                Offset(right, bandTop),
                1.2.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = dash,
            )
            drawLine(
                rangeLineColor.copy(alpha = .85f),
                Offset(left, bandBottom),
                Offset(right, bandBottom),
                1.2.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = dash,
            )
        }

        listOf(0, 360, 720, 1080, 1440).forEach { minute ->
            drawLine(
                outlineColor.copy(alpha = .18f),
                Offset(x(minute), top),
                Offset(x(minute), bottom),
                1.dp.toPx(),
            )
        }
        drawLine(
            outlineColor.copy(alpha = .7f),
            Offset(left, bottom),
            Offset(right, bottom),
            1.2.dp.toPx(),
            cap = StrokeCap.Round,
        )

        if (points.isNotEmpty()) {
            val linePath = Path().apply {
                moveTo(x(0), y(0f))
                var previousTotal = 0f
                points.forEach { point ->
                    lineTo(x(point.minute), y(previousTotal))
                    lineTo(x(point.minute), y(point.totalMl))
                    previousTotal = point.totalMl
                }
                lineTo(x(1440), y(previousTotal))
            }
            val areaPath = Path().apply {
                addPath(linePath)
                lineTo(x(1440), bottom)
                lineTo(x(0), bottom)
                close()
            }
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    0f to actualColor.copy(alpha = .22f),
                    1f to actualColor.copy(alpha = .02f),
                    startY = top,
                    endY = bottom,
                ),
            )
            drawPath(
                path = linePath,
                color = actualColor,
                style = Stroke(3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            points.forEachIndexed { index, point ->
                val center = Offset(x(point.minute), y(point.totalMl))
                if (index == points.lastIndex) {
                    drawCircle(actualColor.copy(alpha = .18f), 11.dp.toPx(), center)
                    drawCircle(actualColor, 6.5.dp.toPx(), center)
                    drawCircle(surfaceColor, 3.dp.toPx(), center)
                } else {
                    drawCircle(actualColor, 5.dp.toPx(), center)
                    drawCircle(surfaceColor, 2.4.dp.toPx(), center)
                }
            }
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor.toArgb()
            textSize = 10.5.dp.toPx()
        }
        drawContext.canvas.nativeCanvas.apply {
            if (guide != null && bandTop != null && bandBottom != null) {
                drawText("${guide.maximumMl}", 2.dp.toPx(), bandTop + 4.dp.toPx(), paint)
                drawText("${guide.minimumMl}", 2.dp.toPx(), bandBottom + 4.dp.toPx(), paint)
            }
            listOf(0 to "0", 360 to "6", 720 to "12", 1080 to "18", 1440 to "24").forEach { (minute, label) ->
                val labelX = (x(minute) - paint.measureText(label) / 2).coerceIn(left, right - paint.measureText(label))
                drawText(label, labelX, size.height - 8.dp.toPx(), paint)
            }
            drawText("мл", 2.dp.toPx(), top, paint)
        }
    }
}

private fun cumulativePoints(entries: List<FoodEntry>): List<IntakePoint> {
    var total = 0f
    return entries.mapNotNull { entry ->
        val time = runCatching { LocalTime.parse(entry.time, DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull()
            ?: return@mapNotNull null
        time.toSecondOfDay() / 60 to entry.amount.toFloat()
    }.sortedBy { it.first }
        .map { (minute, amount) ->
            total += amount
            IntakePoint(minute, total)
        }
}

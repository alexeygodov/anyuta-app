package ru.family.rasti.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
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
internal fun MilkIntakeChart(guide: MilkGuide, entries: List<FoodEntry>) {
    val actualColor = MaterialTheme.colorScheme.primary
    val rangeColor = MaterialTheme.colorScheme.tertiaryContainer
    val rangeLineColor = MaterialTheme.colorScheme.tertiary
    val outlineColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val points = cumulativePoints(entries)
    val consumed = points.lastOrNull()?.totalMl ?: 0f

    Canvas(Modifier.fillMaxWidth().height(220.dp)) {
        val left = 46.dp.toPx()
        val right = size.width - 10.dp.toPx()
        val top = 12.dp.toPx()
        val bottom = size.height - 30.dp.toPx()
        val maxY = max(guide.maximumMl.toFloat(), consumed) * 1.12f

        fun x(minute: Int): Float = left + minute.coerceIn(0, 1440) / 1440f * (right - left)
        fun y(value: Float): Float = bottom - value.coerceAtLeast(0f) / maxY * (bottom - top)

        val bandTop = y(guide.maximumMl.toFloat())
        val bandBottom = y(guide.minimumMl.toFloat())
        drawRect(
            color = rangeColor.copy(alpha = .75f),
            topLeft = Offset(left, bandTop),
            size = Size(right - left, bandBottom - bandTop),
        )
        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 7f))
        drawLine(
            rangeLineColor,
            Offset(left, bandTop),
            Offset(right, bandTop),
            1.dp.toPx(),
            pathEffect = dash,
        )
        drawLine(
            rangeLineColor,
            Offset(left, bandBottom),
            Offset(right, bandBottom),
            1.dp.toPx(),
            pathEffect = dash,
        )

        listOf(0, 360, 720, 1080, 1440).forEach { minute ->
            drawLine(
                outlineColor.copy(alpha = .28f),
                Offset(x(minute), top),
                Offset(x(minute), bottom),
                1.dp.toPx(),
            )
        }
        drawLine(outlineColor, Offset(left, bottom), Offset(right, bottom), 1.dp.toPx())
        drawLine(outlineColor, Offset(left, top), Offset(left, bottom), 1.dp.toPx())

        if (points.isNotEmpty()) {
            val path = Path().apply {
                moveTo(x(0), y(0f))
                var previousTotal = 0f
                points.forEach { point ->
                    lineTo(x(point.minute), y(previousTotal))
                    lineTo(x(point.minute), y(point.totalMl))
                    previousTotal = point.totalMl
                }
                lineTo(x(1440), y(previousTotal))
            }
            drawPath(path, actualColor, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            points.forEach { point ->
                drawCircle(actualColor, 4.dp.toPx(), Offset(x(point.minute), y(point.totalMl)))
            }
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor.toArgb()
            textSize = 10.dp.toPx()
        }
        drawContext.canvas.nativeCanvas.apply {
            drawText("${guide.maximumMl}", 2.dp.toPx(), bandTop + 4.dp.toPx(), paint)
            drawText("${guide.minimumMl}", 2.dp.toPx(), bandBottom + 4.dp.toPx(), paint)
            listOf(0 to "0", 360 to "6", 720 to "12", 1080 to "18", 1440 to "24").forEach { (minute, label) ->
                val labelX = (x(minute) - paint.measureText(label) / 2).coerceIn(left, right - paint.measureText(label))
                drawText(label, labelX, size.height - 7.dp.toPx(), paint)
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

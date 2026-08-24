package ru.family.rasti.ui

import android.graphics.Paint
import android.graphics.RectF
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.max

private data class IntakePoint(
    val minute: Int,
    val totalMl: Float,
    val amountMl: Float,
    val isMilk: Boolean,
)

@Composable
internal fun MilkIntakeChart(
    guide: MilkGuide?,
    entries: List<FoodEntry>,
    date: LocalDate,
) {
    val actualColor = MaterialTheme.colorScheme.primary
    val rangeColor = MaterialTheme.colorScheme.tertiaryContainer
    val rangeLineColor = MaterialTheme.colorScheme.tertiary
    val outlineColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val nowLineColor = MaterialTheme.colorScheme.error
    val milkColor = MaterialTheme.colorScheme.primary
    val formulaColor = MaterialTheme.colorScheme.secondary
    val points = cumulativePoints(entries)
    val consumed = points.lastOrNull()?.totalMl ?: 0f
    val milkTotal = points.filter(IntakePoint::isMilk).sumOf { it.amountMl.toDouble() }
    val formulaTotal = points.filterNot(IntakePoint::isMilk).sumOf { it.amountMl.toDouble() }
    val isToday = date == LocalDate.now()
    val nowMinute = if (isToday) LocalTime.now().toSecondOfDay() / 60 else null

    Canvas(Modifier.fillMaxWidth().height(278.dp)) {
        val left = 40.dp.toPx()
        val right = size.width - 8.dp.toPx()
        val top = 22.dp.toPx()
        val bottom = size.height - 66.dp.toPx()
        val maxY = max(max(guide?.maximumMl?.toFloat() ?: 0f, consumed), 500f) * 1.12f
        val chartEndMinute = if (nowMinute != null) {
            max(nowMinute, points.lastOrNull()?.minute ?: 0)
        } else {
            1440
        }

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

        if (nowMinute != null) {
            val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
            drawLine(
                color = nowLineColor.copy(alpha = .7f),
                start = Offset(x(nowMinute), top),
                end = Offset(x(nowMinute), bottom),
                strokeWidth = 1.6.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = dash,
            )
        }

        if (points.isNotEmpty()) {
            val linePath = Path().apply {
                moveTo(x(0), y(0f))
                var previousTotal = 0f
                points.forEach { point ->
                    lineTo(x(point.minute), y(previousTotal))
                    lineTo(x(point.minute), y(point.totalMl))
                    previousTotal = point.totalMl
                }
                lineTo(x(chartEndMinute), y(previousTotal))
            }
            val areaPath = Path().apply {
                addPath(linePath)
                lineTo(x(chartEndMinute), bottom)
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
                val eventColor = if (point.isMilk) milkColor else formulaColor
                if (index == points.lastIndex) {
                    drawCircle(actualColor.copy(alpha = .18f), 11.dp.toPx(), center)
                    drawCircle(eventColor, 6.5.dp.toPx(), center)
                    drawCircle(surfaceColor, 3.dp.toPx(), center)
                } else {
                    drawCircle(eventColor, 5.dp.toPx(), center)
                    drawCircle(surfaceColor, 2.4.dp.toPx(), center)
                }
            }
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor.toArgb()
            textSize = 10.5.dp.toPx()
        }
        val chipTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = surfaceColor.toArgb()
            textSize = 9.5.dp.toPx()
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val chipFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
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
            if (nowMinute != null) {
                val nowLabel = "сейчас"
                val nowX = x(nowMinute)
                val labelX = (nowX - paint.measureText(nowLabel) / 2)
                    .coerceIn(left, right - paint.measureText(nowLabel))
                drawText(nowLabel, labelX, top - 6.dp.toPx(), paint)
            }

            val labelIndexes = pointLabelIndexes(points.size)
            val occupied = mutableListOf<RectF>()
            val labelRects = mutableMapOf<Int, RectF>()
            val placementOrder = buildList {
                labelIndexes.lastOrNull()?.let(::add)
                addAll(labelIndexes.dropLast(1))
            }.distinct()
            placementOrder.forEach { index ->
                val point = points[index]
                val centerX = x(point.minute)
                val centerY = y(point.totalMl)
                val chipText = "${if (point.isMilk) "М" else "С"} ${formatNumber(point.amountMl.toDouble())}"
                val paddingX = 7.dp.toPx()
                val chipHeight = 17.dp.toPx()
                val textWidth = chipTextPaint.measureText(chipText)
                val chipWidth = textWidth + paddingX * 2
                val baseLeft = (centerX - chipWidth / 2).coerceIn(left, right - chipWidth)
                val verticalCandidates = listOf(
                    centerY - chipHeight - 8.dp.toPx(),
                    centerY + 8.dp.toPx(),
                    centerY - chipHeight - 29.dp.toPx(),
                    centerY + 29.dp.toPx(),
                )
                val horizontalOffsets = listOf(0f, -chipWidth * .55f, chipWidth * .55f)
                val chosen = verticalCandidates.firstNotNullOfOrNull { candidateTop ->
                    horizontalOffsets.firstNotNullOfOrNull { offsetX ->
                        val chipLeft = (baseLeft + offsetX).coerceIn(left, right - chipWidth)
                        val chipTop = candidateTop.coerceIn(top, bottom - chipHeight)
                        val candidate = RectF(chipLeft, chipTop, chipLeft + chipWidth, chipTop + chipHeight)
                        val padded = RectF(candidate).apply { inset(-3.dp.toPx(), -3.dp.toPx()) }
                        candidate.takeIf { rect -> occupied.none { RectF.intersects(it, padded) } }
                    }
                }
                if (chosen != null) {
                    labelRects[index] = chosen
                    occupied += RectF(chosen).apply { inset(-3.dp.toPx(), -3.dp.toPx()) }
                }
            }
            labelIndexes.forEach { index ->
                val point = points[index]
                val rect = labelRects[index] ?: return@forEach
                val chipText = "${if (point.isMilk) "М" else "С"} ${formatNumber(point.amountMl.toDouble())}"
                chipFillPaint.color = if (point.isMilk) milkColor.copy(alpha = .92f).toArgb() else formulaColor.copy(alpha = .9f).toArgb()
                drawRoundRect(
                    rect,
                    9.dp.toPx(),
                    9.dp.toPx(),
                    chipFillPaint,
                )
                drawText(
                    chipText,
                    rect.centerX(),
                    rect.centerY() + chipTextPaint.textSize / 2.9f,
                    chipTextPaint,
                )
            }

            val legendTop = bottom + 49.dp.toPx()
            val milkText = "Молоко ${formatNumber(milkTotal)} мл"
            val formulaText = "Смесь ${formatNumber(formulaTotal)} мл"
            val dotSize = 8.dp.toPx()
            val dotGap = 7.dp.toPx()
            val groupGap = 18.dp.toPx()
            val milkWidth = dotSize + dotGap + paint.measureText(milkText)
            val formulaWidth = dotSize + dotGap + paint.measureText(formulaText)
            val legendWidth = milkWidth + groupGap + formulaWidth
            val legendLeft = (left + (right - left - legendWidth) / 2).coerceAtLeast(left)
            chipFillPaint.color = milkColor.toArgb()
            drawCircle(legendLeft + dotSize / 2, legendTop - 4.dp.toPx(), dotSize / 2, chipFillPaint)
            drawText(milkText, legendLeft + dotSize + dotGap, legendTop, paint)
            val formulaLeft = legendLeft + milkWidth + groupGap
            chipFillPaint.color = formulaColor.toArgb()
            drawCircle(formulaLeft + dotSize / 2, legendTop - 4.dp.toPx(), dotSize / 2, chipFillPaint)
            drawText(formulaText, formulaLeft + dotSize + dotGap, legendTop, paint)
        }
    }
}

internal fun pointLabelIndexes(pointCount: Int, maximumLabels: Int = 8): List<Int> {
    if (pointCount <= 0 || maximumLabels <= 0) return emptyList()
    if (pointCount <= maximumLabels) return (0 until pointCount).toList()
    val step = ceil(pointCount / maximumLabels.toFloat()).toInt()
    return buildList {
        for (index in 0 until pointCount step step) add(index)
        if (lastOrNull() != pointCount - 1) add(pointCount - 1)
    }
}

private fun cumulativePoints(entries: List<FoodEntry>): List<IntakePoint> {
    var total = 0f
    return entries.mapNotNull { entry ->
        val time = runCatching { LocalTime.parse(entry.time, DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull()
            ?: return@mapNotNull null
        Triple(time.toSecondOfDay() / 60, entry.amount.toFloat(), entry.name.trim())
    }.sortedBy { it.first }
        .map { (minute, amount, name) ->
            total += amount
            IntakePoint(
                minute = minute,
                totalMl = total,
                amountMl = amount,
                isMilk = name.equals("Молоко", ignoreCase = true),
            )
        }
}

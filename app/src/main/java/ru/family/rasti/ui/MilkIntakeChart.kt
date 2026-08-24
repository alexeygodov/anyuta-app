package ru.family.rasti.ui

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.text.font.FontWeight
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
    val plotColor = MaterialTheme.colorScheme.surfaceVariant
    val nowLineColor = MaterialTheme.colorScheme.error
    val milkColor = MaterialTheme.colorScheme.primary
    val formulaColor = MaterialTheme.colorScheme.secondary
    val onMilkColor = MaterialTheme.colorScheme.onPrimary
    val onFormulaColor = MaterialTheme.colorScheme.onSecondary
    val points = cumulativePoints(entries)
    val consumed = points.lastOrNull()?.totalMl ?: 0f
    val milkTotal = points.filter(IntakePoint::isMilk).sumOf { it.amountMl.toDouble() }
    val formulaTotal = points.filterNot(IntakePoint::isMilk).sumOf { it.amountMl.toDouble() }
    val isToday = date == LocalDate.now()
    val nowMinute = if (isToday) LocalTime.now().toSecondOfDay() / 60 else null

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(Modifier.fillMaxWidth().height(250.dp)) {
        val left = 40.dp.toPx()
        val right = size.width - 8.dp.toPx()
        val top = 22.dp.toPx()
        val bottom = size.height - 30.dp.toPx()
        val maxY = max(max(guide?.maximumMl?.toFloat() ?: 0f, consumed), 500f) * 1.12f
        val chartEndMinute = if (nowMinute != null) {
            max(nowMinute, points.lastOrNull()?.minute ?: 0)
        } else {
            1440
        }

        fun x(minute: Int): Float = left + minute.coerceIn(0, 1440) / 1440f * (right - left)
        fun y(value: Float): Float = bottom - value.coerceAtLeast(0f) / maxY * (bottom - top)

        drawRoundRect(
            color = plotColor.copy(alpha = .16f),
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
            cornerRadius = CornerRadius(18.dp.toPx()),
        )

        val bandTop = guide?.let { y(it.maximumMl.toFloat()) }
        val bandBottom = guide?.let { y(it.minimumMl.toFloat()) }
        if (bandTop != null && bandBottom != null) {
            drawRoundRect(
                brush = Brush.verticalGradient(
                    0f to rangeColor.copy(alpha = .62f),
                    1f to rangeColor.copy(alpha = .28f),
                    startY = bandTop,
                    endY = bandBottom,
                ),
                topLeft = Offset(left, bandTop),
                size = Size(right - left, bandBottom - bandTop),
                cornerRadius = CornerRadius(14.dp.toPx()),
            )
            val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 9f))
            drawLine(
                rangeLineColor.copy(alpha = .55f),
                Offset(left, bandTop),
                Offset(right, bandTop),
                1.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = dash,
            )
            drawLine(
                rangeLineColor.copy(alpha = .55f),
                Offset(left, bandBottom),
                Offset(right, bandBottom),
                1.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = dash,
            )
        }

        listOf(.25f, .5f, .75f).forEach { fraction ->
            val gridY = bottom - (bottom - top) * fraction
            drawLine(
                outlineColor.copy(alpha = .1f),
                Offset(left, gridY),
                Offset(right, gridY),
                1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 9f)),
            )
        }
        listOf(0, 360, 720, 1080, 1440).forEach { minute ->
            drawLine(
                outlineColor.copy(alpha = .1f),
                Offset(x(minute), top),
                Offset(x(minute), bottom),
                1.dp.toPx(),
            )
        }
        drawLine(
            outlineColor.copy(alpha = .38f),
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
                    0f to actualColor.copy(alpha = .2f),
                    .72f to actualColor.copy(alpha = .06f),
                    1f to actualColor.copy(alpha = 0f),
                    startY = top,
                    endY = bottom,
                ),
            )
            drawPath(
                path = linePath,
                color = actualColor.copy(alpha = .16f),
                style = Stroke(8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawPath(
                path = linePath,
                color = actualColor,
                style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            points.forEachIndexed { index, point ->
                val center = Offset(x(point.minute), y(point.totalMl))
                val eventColor = if (point.isMilk) milkColor else formulaColor
                if (index == points.lastIndex) {
                    drawCircle(eventColor.copy(alpha = .18f), 12.dp.toPx(), center)
                    drawCircle(surfaceColor, 7.dp.toPx(), center)
                    drawCircle(eventColor, 5.dp.toPx(), center)
                } else {
                    drawCircle(surfaceColor, 6.dp.toPx(), center)
                    drawCircle(eventColor, 4.2.dp.toPx(), center)
                }
            }
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor.toArgb()
            textSize = 10.5.dp.toPx()
        }
        val chipTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = onMilkColor.toArgb()
            textSize = 9.5.dp.toPx()
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val chipFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        drawContext.canvas.nativeCanvas.apply {
            if (guide != null && bandTop != null && bandBottom != null) {
                drawText("${guide.maximumMl}", 2.dp.toPx(), bandTop + 4.dp.toPx(), paint)
                drawText("${guide.minimumMl}", 2.dp.toPx(), bandBottom + 4.dp.toPx(), paint)
                val rangePaint = Paint(paint).apply {
                    color = rangeLineColor.toArgb()
                    textSize = 9.dp.toPx()
                    isFakeBoldText = true
                }
                val rangeLabel = "норма"
                drawText(
                    rangeLabel,
                    right - rangePaint.measureText(rangeLabel) - 8.dp.toPx(),
                    (bandTop + bandBottom) / 2 + rangePaint.textSize / 3,
                    rangePaint,
                )
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
                val chipText = formatNumber(point.amountMl.toDouble())
                val paddingX = 8.dp.toPx()
                val chipHeight = 19.dp.toPx()
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
                val chipText = formatNumber(point.amountMl.toDouble())
                chipFillPaint.color = labelColor.copy(alpha = .12f).toArgb()
                val shadow = RectF(rect).apply { offset(0f, 2.dp.toPx()) }
                drawRoundRect(shadow, 10.dp.toPx(), 10.dp.toPx(), chipFillPaint)
                chipFillPaint.color = if (point.isMilk) milkColor.toArgb() else formulaColor.toArgb()
                drawRoundRect(
                    rect,
                    10.dp.toPx(),
                    10.dp.toPx(),
                    chipFillPaint,
                )
                chipTextPaint.color = if (point.isMilk) onMilkColor.toArgb() else onFormulaColor.toArgb()
                drawText(
                    chipText,
                    rect.centerX(),
                    rect.centerY() + chipTextPaint.textSize / 2.9f,
                    chipTextPaint,
                )
            }

        }
    }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FeedingLegendChip(milkColor, "Молоко", milkTotal)
            Box(Modifier.width(8.dp))
            FeedingLegendChip(formulaColor, "Смесь", formulaTotal)
        }
    }
}

@Composable
private fun FeedingLegendChip(color: androidx.compose.ui.graphics.Color, label: String, amountMl: Double) {
    Surface(
        color = color.copy(alpha = .14f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(50),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(
                "${formatNumber(amountMl)} мл",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
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

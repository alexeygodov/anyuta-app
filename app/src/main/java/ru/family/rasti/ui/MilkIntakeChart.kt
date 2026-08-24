package ru.family.rasti.ui

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.family.rasti.data.FoodEntry
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

private data class FeedingPoint(
    val entry: FoodEntry,
    val minute: Int,
    val amountMl: Float,
    val isMilk: Boolean,
)

@Composable
internal fun MilkIntakeChart(
    entries: List<FoodEntry>,
    date: LocalDate,
    onEntryClick: (FoodEntry) -> Unit,
) {
    val milkColor = MaterialTheme.colorScheme.primary
    val formulaColor = MaterialTheme.colorScheme.secondary
    val onMilkColor = MaterialTheme.colorScheme.onPrimary
    val onFormulaColor = MaterialTheme.colorScheme.onSecondary
    val outlineColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val plotColor = MaterialTheme.colorScheme.surfaceVariant
    val nowColor = MaterialTheme.colorScheme.error
    val points = remember(entries) { feedingPoints(entries) }
    val milkTotal = points.filter(FeedingPoint::isMilk).sumOf { it.amountMl.toDouble() }
    val formulaTotal = points.filterNot(FeedingPoint::isMilk).sumOf { it.amountMl.toDouble() }
    val nowMinute = if (date == LocalDate.now()) LocalTime.now().toSecondOfDay() / 60 else null
    val density = LocalDensity.current
    val leftTapPadding = with(density) { 36.dp.toPx() }
    val rightTapPadding = with(density) { 8.dp.toPx() }
    val tapRadius = with(density) { 28.dp.toPx() }
    var chartWidth by remember { mutableFloatStateOf(0f) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(242.dp)
                .onSizeChanged { chartWidth = it.width.toFloat() }
                .pointerInput(points, chartWidth) {
                    detectTapGestures { tap ->
                        val plotWidth = chartWidth - leftTapPadding - rightTapPadding
                        if (plotWidth <= 0f || points.isEmpty()) return@detectTapGestures
                        val nearest = points.minByOrNull { point ->
                            abs(leftTapPadding + point.minute / 1440f * plotWidth - tap.x)
                        } ?: return@detectTapGestures
                        val nearestX = leftTapPadding + nearest.minute / 1440f * plotWidth
                        if (abs(nearestX - tap.x) <= tapRadius) onEntryClick(nearest.entry)
                    }
                },
        ) {
            val left = 36.dp.toPx()
            val right = size.width - 8.dp.toPx()
            val top = 34.dp.toPx()
            val bottom = size.height - 30.dp.toPx()
            val rawMaximum = points.maxOfOrNull(FeedingPoint::amountMl) ?: 0f
            val maximumMl = max(200f, ceil(rawMaximum / 50f) * 50f)
            val barWidth = if (points.size <= 8) 12.dp.toPx() else 8.dp.toPx()

            fun x(minute: Int): Float = left + minute.coerceIn(0, 1440) / 1440f * (right - left)
            fun y(amountMl: Float): Float = bottom - amountMl.coerceAtLeast(0f) / maximumMl * (bottom - top)

            drawRoundRect(
                color = plotColor.copy(alpha = .16f),
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                cornerRadius = CornerRadius(18.dp.toPx()),
            )

            if (nowMinute != null) {
                val nowX = x(nowMinute)
                drawRoundRect(
                    color = surfaceColor.copy(alpha = .32f),
                    topLeft = Offset(nowX, top),
                    size = Size((right - nowX).coerceAtLeast(0f), bottom - top),
                    cornerRadius = CornerRadius(18.dp.toPx()),
                )
            }

            listOf(.25f, .5f, .75f, 1f).forEach { fraction ->
                val gridY = bottom - (bottom - top) * fraction
                drawLine(
                    outlineColor.copy(alpha = if (fraction == 1f) .18f else .1f),
                    Offset(left, gridY),
                    Offset(right, gridY),
                    1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 9f)),
                )
            }
            listOf(0, 360, 720, 1080, 1440).forEach { minute ->
                drawLine(
                    outlineColor.copy(alpha = .09f),
                    Offset(x(minute), top),
                    Offset(x(minute), bottom),
                    1.dp.toPx(),
                )
            }
            drawLine(
                outlineColor.copy(alpha = .4f),
                Offset(left, bottom),
                Offset(right, bottom),
                1.2.dp.toPx(),
                cap = StrokeCap.Round,
            )

            if (nowMinute != null) {
                drawLine(
                    nowColor.copy(alpha = .72f),
                    Offset(x(nowMinute), top),
                    Offset(x(nowMinute), bottom),
                    1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                )
            }

            points.forEach { point ->
                val centerX = x(point.minute)
                val barTop = y(point.amountMl).coerceAtMost(bottom - 5.dp.toPx())
                val eventColor = if (point.isMilk) milkColor else formulaColor
                drawRoundRect(
                    color = eventColor.copy(alpha = .13f),
                    topLeft = Offset(centerX - barWidth / 2 - 3.dp.toPx(), barTop - 2.dp.toPx()),
                    size = Size(barWidth + 6.dp.toPx(), bottom - barTop + 2.dp.toPx()),
                    cornerRadius = CornerRadius((barWidth + 6.dp.toPx()) / 2),
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        0f to eventColor,
                        1f to eventColor.copy(alpha = .7f),
                        startY = barTop,
                        endY = bottom,
                    ),
                    topLeft = Offset(centerX - barWidth / 2, barTop),
                    size = Size(barWidth, bottom - barTop),
                    cornerRadius = CornerRadius(barWidth / 2),
                )
            }

            val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = labelColor.toArgb()
                textSize = 10.dp.toPx()
            }
            val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 10.dp.toPx()
                textAlign = Paint.Align.CENTER
                isFakeBoldText = true
            }
            val valueFillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val labelIndexes = pointLabelIndexes(points.size, maximumLabels = 9)
            val occupied = mutableListOf<RectF>()
            val labelRects = mutableMapOf<Int, RectF>()
            val placementOrder = buildList {
                labelIndexes.lastOrNull()?.let(::add)
                addAll(labelIndexes.dropLast(1))
            }.distinct()
            placementOrder.forEach { index ->
                val point = points[index]
                val text = formatNumber(point.amountMl.toDouble())
                val textWidth = valuePaint.measureText(text)
                val chipWidth = textWidth + 15.dp.toPx()
                val chipHeight = 20.dp.toPx()
                val baseLeft = (x(point.minute) - chipWidth / 2).coerceIn(left, right - chipWidth)
                val barTop = y(point.amountMl)
                val verticalCandidates = listOf(
                    barTop - chipHeight - 7.dp.toPx(),
                    barTop - chipHeight - 30.dp.toPx(),
                    barTop + 7.dp.toPx(),
                )
                val horizontalOffsets = listOf(0f, -chipWidth * .55f, chipWidth * .55f)
                val chosen = verticalCandidates.firstNotNullOfOrNull { candidateTop ->
                    horizontalOffsets.firstNotNullOfOrNull { offsetX ->
                        val chipLeft = (baseLeft + offsetX).coerceIn(left, right - chipWidth)
                        val chipTop = candidateTop.coerceIn(top, bottom - chipHeight)
                        val candidate = RectF(chipLeft, chipTop, chipLeft + chipWidth, chipTop + chipHeight)
                        val padded = RectF(candidate).apply { inset(-3.dp.toPx(), -3.dp.toPx()) }
                        candidate.takeIf { occupied.none { previous -> RectF.intersects(previous, padded) } }
                    }
                }
                if (chosen != null) {
                    labelRects[index] = chosen
                    occupied += RectF(chosen).apply { inset(-3.dp.toPx(), -3.dp.toPx()) }
                }
            }

            drawContext.canvas.nativeCanvas.apply {
                drawText(formatNumber(maximumMl.toDouble()), 2.dp.toPx(), top + 4.dp.toPx(), axisPaint)
                drawText(formatNumber((maximumMl / 2).toDouble()), 2.dp.toPx(), y(maximumMl / 2) + 4.dp.toPx(), axisPaint)
                listOf(0 to "0", 360 to "6", 720 to "12", 1080 to "18", 1440 to "24").forEach { (minute, label) ->
                    val labelX = (x(minute) - axisPaint.measureText(label) / 2)
                        .coerceIn(left, right - axisPaint.measureText(label))
                    drawText(label, labelX, size.height - 8.dp.toPx(), axisPaint)
                }
                drawText("мл", 2.dp.toPx(), top - 10.dp.toPx(), axisPaint)
                if (nowMinute != null) {
                    val text = "сейчас"
                    val labelX = (x(nowMinute) - axisPaint.measureText(text) / 2)
                        .coerceIn(left, right - axisPaint.measureText(text))
                    drawText(text, labelX, top - 8.dp.toPx(), axisPaint)
                }
                labelIndexes.forEach { index ->
                    val point = points[index]
                    val rect = labelRects[index] ?: return@forEach
                    val eventColor = if (point.isMilk) milkColor else formulaColor
                    valueFillPaint.color = labelColor.copy(alpha = .12f).toArgb()
                    val shadow = RectF(rect).apply { offset(0f, 2.dp.toPx()) }
                    drawRoundRect(shadow, 11.dp.toPx(), 11.dp.toPx(), valueFillPaint)
                    valueFillPaint.color = eventColor.toArgb()
                    drawRoundRect(rect, 11.dp.toPx(), 11.dp.toPx(), valueFillPaint)
                    valuePaint.color = if (point.isMilk) onMilkColor.toArgb() else onFormulaColor.toArgb()
                    drawText(
                        formatNumber(point.amountMl.toDouble()),
                        rect.centerX(),
                        rect.centerY() + valuePaint.textSize / 2.9f,
                        valuePaint,
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
        if (points.isNotEmpty()) {
            Text(
                "Нажмите на столбик, чтобы изменить кормление",
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun FeedingLegendChip(color: Color, label: String, amountMl: Double) {
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

private fun feedingPoints(entries: List<FoodEntry>): List<FeedingPoint> =
    entries.mapNotNull { entry ->
        val time = runCatching { LocalTime.parse(entry.time, DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull()
            ?: return@mapNotNull null
        FeedingPoint(
            entry = entry,
            minute = time.toSecondOfDay() / 60,
            amountMl = entry.amount.toFloat(),
            isMilk = entry.name.equals("Молоко", ignoreCase = true),
        )
    }.sortedBy(FeedingPoint::minute)

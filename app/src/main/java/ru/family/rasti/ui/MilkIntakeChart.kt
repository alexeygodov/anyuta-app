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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import kotlin.math.roundToInt

private data class FeedingPoint(
    val entry: FoodEntry,
    val minute: Int,
    val amountMl: Float,
    val cumulativeMl: Float,
    val isMilk: Boolean,
)

@Composable
internal fun MilkIntakeChart(
    entries: List<FoodEntry>,
    date: LocalDate,
    minimumMl: Int?,
    targetMl: Int?,
    maximumMl: Int?,
    onEntryClick: (FoodEntry) -> Unit,
) {
    val milkColor = MaterialTheme.colorScheme.primary
    val formulaColor = MaterialTheme.colorScheme.secondary
    val paceColor = MaterialTheme.colorScheme.tertiary
    val actualColor = MaterialTheme.colorScheme.onSurface
    val outlineColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val plotColor = MaterialTheme.colorScheme.surfaceVariant
    val nowColor = MaterialTheme.colorScheme.error
    val points = remember(entries) { feedingPoints(entries) }
    val total = points.lastOrNull()?.cumulativeMl ?: 0f
    val milkTotal = points.filter(FeedingPoint::isMilk).sumOf { it.amountMl.toDouble() }
    val formulaTotal = points.filterNot(FeedingPoint::isMilk).sumOf { it.amountMl.toDouble() }
    val progressPercent = feedingProgressPercent(total.toDouble(), targetMl)
    val scaleMaximum = feedingChartScaleMaximum(total.toDouble(), maximumMl)
    val nowMinute = if (date == LocalDate.now()) LocalTime.now().toSecondOfDay() / 60 else null
    val endMinute = max(points.lastOrNull()?.minute ?: 0, nowMinute ?: 1440).coerceAtMost(1440)
    val density = LocalDensity.current
    val leftTapPadding = with(density) { 42.dp.toPx() }
    val rightTapPadding = with(density) { 8.dp.toPx() }
    val tapRadius = with(density) { 28.dp.toPx() }
    var chartWidth by remember { mutableFloatStateOf(0f) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Накопление", style = MaterialTheme.typography.labelLarge)
            Text(
                if (minimumMl != null && maximumMl != null) {
                    "ориентир $minimumMl–$maximumMl мл"
                } else {
                    "за сутки"
                },
                color = labelColor,
                style = MaterialTheme.typography.labelSmall,
            )
        }

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(232.dp)
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
            val left = 42.dp.toPx()
            val right = size.width - 8.dp.toPx()
            val top = 16.dp.toPx()
            val bottom = size.height - 28.dp.toPx()
            val chartHeight = bottom - top

            fun x(minute: Int): Float = left + minute.coerceIn(0, 1440) / 1440f * (right - left)
            fun y(amountMl: Float): Float = bottom - amountMl.coerceIn(0f, scaleMaximum) / scaleMaximum * chartHeight

            drawRoundRect(
                color = plotColor.copy(alpha = .16f),
                topLeft = Offset(left, top),
                size = Size(right - left, chartHeight),
                cornerRadius = CornerRadius(16.dp.toPx()),
            )

            val tickValues = listOf(0f, scaleMaximum / 2f, scaleMaximum)
            tickValues.forEach { value ->
                drawLine(
                    outlineColor.copy(alpha = if (value == 0f) .3f else .13f),
                    Offset(left, y(value)),
                    Offset(right, y(value)),
                    if (value == 0f) 1.2.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            listOf(0, 360, 720, 1080, 1440).forEach { minute ->
                drawLine(
                    outlineColor.copy(alpha = .08f),
                    Offset(x(minute), top),
                    Offset(x(minute), bottom),
                    1.dp.toPx(),
                )
            }

            if (minimumMl != null && targetMl != null && maximumMl != null) {
                val paceBand = Path().apply {
                    moveTo(x(0), y(0f))
                    lineTo(x(1440), y(maximumMl.toFloat()))
                    lineTo(x(1440), y(minimumMl.toFloat()))
                    close()
                }
                drawPath(paceBand, paceColor.copy(alpha = .13f))
                drawLine(
                    paceColor.copy(alpha = .72f),
                    Offset(x(0), y(0f)),
                    Offset(x(1440), y(targetMl.toFloat())),
                    1.4.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)),
                )
            }

            if (nowMinute != null) {
                drawLine(
                    nowColor.copy(alpha = .72f),
                    Offset(x(nowMinute), top),
                    Offset(x(nowMinute), bottom),
                    1.4.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 7f)),
                )
            }

            var previousMinute = 0
            var previousTotal = 0f
            points.forEach { point ->
                val eventColor = if (point.isMilk) milkColor else formulaColor
                drawLine(
                    actualColor.copy(alpha = .78f),
                    Offset(x(previousMinute), y(previousTotal)),
                    Offset(x(point.minute), y(previousTotal)),
                    2.4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    eventColor,
                    Offset(x(point.minute), y(previousTotal)),
                    Offset(x(point.minute), y(point.cumulativeMl)),
                    5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawCircle(
                    color = surfaceColor,
                    radius = 5.2.dp.toPx(),
                    center = Offset(x(point.minute), y(point.cumulativeMl)),
                )
                drawCircle(
                    color = eventColor,
                    radius = 3.4.dp.toPx(),
                    center = Offset(x(point.minute), y(point.cumulativeMl)),
                )
                previousMinute = point.minute
                previousTotal = point.cumulativeMl
            }
            if (points.isNotEmpty()) {
                drawLine(
                    actualColor.copy(alpha = .78f),
                    Offset(x(previousMinute), y(previousTotal)),
                    Offset(x(endMinute), y(previousTotal)),
                    2.4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = labelColor.toArgb()
                textSize = 10.dp.toPx()
            }
            drawContext.canvas.nativeCanvas.apply {
                tickValues.forEach { value ->
                    val label = formatNumber(value.toDouble())
                    drawText(label, left - axisPaint.measureText(label) - 7.dp.toPx(), y(value) + 4.dp.toPx(), axisPaint)
                }
                listOf(0 to "0", 360 to "6", 720 to "12", 1080 to "18", 1440 to "24").forEach { (minute, label) ->
                    val labelX = (x(minute) - axisPaint.measureText(label) / 2)
                        .coerceIn(left, right - axisPaint.measureText(label))
                    drawText(label, labelX, size.height - 7.dp.toPx(), axisPaint)
                }
                drawText("мл", 2.dp.toPx(), top + 4.dp.toPx(), axisPaint)
            }

            if (points.isNotEmpty()) {
                val endpointText = buildString {
                    append(formatNumber(total.toDouble()))
                    append(" мл")
                    progressPercent?.let { append(" · $it%") }
                }
                val endpointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = actualColor.toArgb()
                    textSize = 11.dp.toPx()
                    isFakeBoldText = true
                }
                val horizontalPadding = 9.dp.toPx()
                val chipWidth = endpointPaint.measureText(endpointText) + horizontalPadding * 2
                val chipHeight = 25.dp.toPx()
                val anchorX = x(endMinute)
                val anchorY = y(total)
                val chipLeft = (anchorX + 7.dp.toPx()).coerceIn(left, right - chipWidth)
                val chipTop = (anchorY - chipHeight - 7.dp.toPx()).coerceIn(top, bottom - chipHeight)
                val chip = RectF(chipLeft, chipTop, chipLeft + chipWidth, chipTop + chipHeight)
                drawRoundRect(
                    color = surfaceColor.copy(alpha = .95f),
                    topLeft = Offset(chip.left, chip.top),
                    size = Size(chip.width(), chip.height()),
                    cornerRadius = CornerRadius(chipHeight / 2),
                )
                drawContext.canvas.nativeCanvas.drawText(
                    endpointText,
                    chip.left + horizontalPadding,
                    chip.centerY() + endpointPaint.textSize / 2.9f,
                    endpointPaint,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FeedingLegendItem(milkColor, "Молоко", milkTotal)
            FeedingLegendItem(formulaColor, "Смесь", formulaTotal)
            if (minimumMl != null && maximumMl != null) {
                FeedingLegendItem(paceColor, "Ориентир", null)
            }
        }
        if (points.isNotEmpty()) {
            Text(
                "Нажмите на точку, чтобы изменить кормление",
                modifier = Modifier.fillMaxWidth(),
                color = labelColor,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun FeedingLegendItem(color: Color, label: String, amountMl: Double?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(
            buildString {
                append(label)
                amountMl?.let { append(" ${formatNumber(it)}") }
            },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (amountMl != null) FontWeight.Medium else FontWeight.Normal,
        )
    }
}

internal fun feedingChartScaleMaximum(totalMl: Double, guideMaximumMl: Int?): Float {
    val guideBasis = guideMaximumMl?.toDouble()?.times(1.06) ?: 0.0
    val actualBasis = totalMl.coerceAtLeast(0.0) * 1.14
    val basis = max(200.0, max(guideBasis, actualBasis))
    return (ceil(basis / 100.0) * 100.0).toFloat()
}

internal fun feedingProgressPercent(totalMl: Double, targetMl: Int?): Int? {
    if (targetMl == null || targetMl <= 0) return null
    return (totalMl.coerceAtLeast(0.0) / targetMl * 100.0).roundToInt()
}

private fun feedingPoints(entries: List<FoodEntry>): List<FeedingPoint> {
    var cumulative = 0f
    return entries.mapNotNull { entry ->
        val time = runCatching { LocalTime.parse(entry.time, DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull()
            ?: return@mapNotNull null
        Triple(entry, time.toSecondOfDay() / 60, entry.amount.toFloat().coerceAtLeast(0f))
    }.sortedBy { it.second }.map { (entry, minute, amount) ->
        cumulative += amount
        FeedingPoint(
            entry = entry,
            minute = minute,
            amountMl = amount,
            cumulativeMl = cumulative,
            isMilk = entry.name.equals("Молоко", ignoreCase = true),
        )
    }
}

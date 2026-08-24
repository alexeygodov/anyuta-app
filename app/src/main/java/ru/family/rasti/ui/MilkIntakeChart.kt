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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
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
    val goalColor = MaterialTheme.colorScheme.tertiary
    val actualColor = MaterialTheme.colorScheme.onSurface
    val outlineColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val plotColor = MaterialTheme.colorScheme.surfaceVariant
    val nowColor = MaterialTheme.colorScheme.error
    val points = remember(entries) { feedingPoints(entries) }
    val total = points.lastOrNull()?.cumulativeMl?.toDouble() ?: 0.0
    val milkTotal = points.filter(FeedingPoint::isMilk).sumOf { it.amountMl.toDouble() }
    val formulaTotal = points.filterNot(FeedingPoint::isMilk).sumOf { it.amountMl.toDouble() }
    val progressPercent = feedingGoalProgressPercent(total, targetMl)
    val scaleMaximum = feedingCumulativeScaleMaximum(total, maximumMl)
    val nowMinute = if (date == LocalDate.now()) LocalTime.now().toSecondOfDay() / 60 else null
    val endMinute = max(points.lastOrNull()?.minute ?: 0, nowMinute ?: 1440).coerceAtMost(1440)
    val density = LocalDensity.current
    val leftTapPadding = with(density) { 42.dp.toPx() }
    val rightTapPadding = with(density) { 12.dp.toPx() }
    val tapRadius = with(density) { 28.dp.toPx() }
    var chartWidth by remember { mutableFloatStateOf(0f) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    "${formatNumber(total)} мл",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    if (targetMl != null) {
                        "цель ≈$targetMl мл"
                    } else {
                        feedingCountLabel(points.size)
                    },
                    color = labelColor,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            if (progressPercent != null) {
                Surface(
                    color = milkColor.copy(alpha = .14f),
                    contentColor = actualColor,
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        "$progressPercent% цели",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            } else {
                Text(feedingCountLabel(points.size), color = labelColor, style = MaterialTheme.typography.labelMedium)
            }
        }

        if (minimumMl != null && maximumMl != null) {
            Text(
                "Суточный диапазон $minimumMl–$maximumMl мл",
                color = labelColor,
                style = MaterialTheme.typography.labelSmall,
            )
        }

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(230.dp)
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
            val right = size.width - 12.dp.toPx()
            val top = 16.dp.toPx()
            val bottom = size.height - 28.dp.toPx()
            val plotHeight = bottom - top

            fun x(minute: Int): Float = left + minute.coerceIn(0, 1440) / 1440f * (right - left)
            fun y(amountMl: Float): Float = bottom -
                amountMl.coerceIn(0f, scaleMaximum.toFloat()) / scaleMaximum.toFloat() * plotHeight

            drawRoundRect(
                color = plotColor.copy(alpha = .15f),
                topLeft = Offset(left, top),
                size = Size(right - left, plotHeight),
                cornerRadius = CornerRadius(18.dp.toPx()),
            )

            val tickValues = listOf(0f, scaleMaximum.toFloat() / 2f, scaleMaximum.toFloat())
            tickValues.forEach { value ->
                drawLine(
                    color = outlineColor.copy(alpha = if (value == 0f) .28f else .11f),
                    start = Offset(left, y(value)),
                    end = Offset(right, y(value)),
                    strokeWidth = if (value == 0f) 1.2.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
            listOf(0, 360, 720, 1080, 1440).forEach { minute ->
                drawLine(
                    color = outlineColor.copy(alpha = .07f),
                    start = Offset(x(minute), top),
                    end = Offset(x(minute), bottom),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            if (nowMinute != null) {
                drawLine(
                    color = nowColor.copy(alpha = .58f),
                    start = Offset(x(nowMinute), top),
                    end = Offset(x(nowMinute), bottom),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 7f)),
                )
            }

            if (points.isNotEmpty()) {
                val linePath = Path().apply {
                    moveTo(x(0), y(0f))
                    var previousTotal = 0f
                    points.forEach { point ->
                        lineTo(x(point.minute), y(previousTotal))
                        lineTo(x(point.minute), y(point.cumulativeMl))
                        previousTotal = point.cumulativeMl
                    }
                    lineTo(x(endMinute), y(total.toFloat()))
                }
                val areaPath = Path().apply {
                    addPath(linePath)
                    lineTo(x(endMinute), bottom)
                    lineTo(x(0), bottom)
                    close()
                }
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(milkColor.copy(alpha = .24f), milkColor.copy(alpha = .02f)),
                        startY = top,
                        endY = bottom,
                    ),
                )
                drawPath(
                    path = linePath,
                    color = actualColor.copy(alpha = .82f),
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )

                var previousTotal = 0f
                points.forEach { point ->
                    val pointX = x(point.minute)
                    val pointY = y(point.cumulativeMl)
                    val eventColor = if (point.isMilk) milkColor else formulaColor
                    drawLine(
                        color = eventColor,
                        start = Offset(pointX, y(previousTotal)),
                        end = Offset(pointX, pointY),
                        strokeWidth = 4.5.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    if (point.isMilk) {
                        drawCircle(surfaceColor, 5.2.dp.toPx(), Offset(pointX, pointY))
                        drawCircle(eventColor, 3.4.dp.toPx(), Offset(pointX, pointY))
                    } else {
                        drawRoundRect(
                            color = surfaceColor,
                            topLeft = Offset(pointX - 5.2.dp.toPx(), pointY - 5.2.dp.toPx()),
                            size = Size(10.4.dp.toPx(), 10.4.dp.toPx()),
                            cornerRadius = CornerRadius(2.2.dp.toPx()),
                        )
                        drawRoundRect(
                            color = eventColor,
                            topLeft = Offset(pointX - 3.4.dp.toPx(), pointY - 3.4.dp.toPx()),
                            size = Size(6.8.dp.toPx(), 6.8.dp.toPx()),
                            cornerRadius = CornerRadius(1.4.dp.toPx()),
                        )
                    }
                    previousTotal = point.cumulativeMl
                }
            }

            if (minimumMl != null && targetMl != null && maximumMl != null) {
                val goalX = right - 5.dp.toPx()
                val rangeTop = y(maximumMl.toFloat())
                val rangeBottom = y(minimumMl.toFloat())
                drawRoundRect(
                    color = goalColor.copy(alpha = .42f),
                    topLeft = Offset(goalX - 5.dp.toPx(), rangeTop),
                    size = Size(10.dp.toPx(), (rangeBottom - rangeTop).coerceAtLeast(5.dp.toPx())),
                    cornerRadius = CornerRadius(5.dp.toPx()),
                )
                drawCircle(
                    color = surfaceColor,
                    radius = 7.dp.toPx(),
                    center = Offset(goalX, y(targetMl.toFloat())),
                )
                drawCircle(
                    color = goalColor,
                    radius = 5.dp.toPx(),
                    center = Offset(goalX, y(targetMl.toFloat())),
                )
                drawCircle(
                    color = surfaceColor,
                    radius = 2.dp.toPx(),
                    center = Offset(goalX, y(targetMl.toFloat())),
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
                    drawText(label, labelX, size.height - 6.dp.toPx(), axisPaint)
                }
                drawText("мл", 2.dp.toPx(), top + 4.dp.toPx(), axisPaint)
            }

            if (points.isNotEmpty()) {
                val endpointText = "${formatNumber(total)} мл"
                val endpointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = actualColor.toArgb()
                    textSize = 11.dp.toPx()
                    isFakeBoldText = true
                }
                val paddingX = 9.dp.toPx()
                val chipWidth = endpointPaint.measureText(endpointText) + paddingX * 2
                val chipHeight = 25.dp.toPx()
                val anchorX = x(endMinute)
                val anchorY = y(total.toFloat())
                val preferredLeft = if (anchorX > left + (right - left) * .68f) {
                    anchorX - chipWidth - 8.dp.toPx()
                } else {
                    anchorX + 8.dp.toPx()
                }
                val chipLeft = preferredLeft.coerceIn(left, right - chipWidth)
                val chipTop = (anchorY - chipHeight - 7.dp.toPx()).coerceIn(top, bottom - chipHeight)
                val chip = RectF(chipLeft, chipTop, chipLeft + chipWidth, chipTop + chipHeight)
                drawRoundRect(
                    color = surfaceColor.copy(alpha = .96f),
                    topLeft = Offset(chip.left, chip.top),
                    size = Size(chip.width(), chip.height()),
                    cornerRadius = CornerRadius(chipHeight / 2),
                )
                drawContext.canvas.nativeCanvas.drawText(
                    endpointText,
                    chip.left + paddingX,
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
            FeedingLegendItem(milkColor, isMilk = true, label = "Молоко ${formatNumber(milkTotal)}")
            FeedingLegendItem(formulaColor, isMilk = false, label = "Смесь ${formatNumber(formulaTotal)}")
            if (targetMl != null) GoalLegendItem(goalColor)
        }

        if (points.isNotEmpty()) {
            Text(
                "Нажмите на маркер, чтобы изменить кормление",
                modifier = Modifier.fillMaxWidth(),
                color = labelColor,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun FeedingLegendItem(color: Color, isMilk: Boolean, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(
            Modifier
                .size(8.dp)
                .background(color, if (isMilk) CircleShape else RoundedCornerShape(2.dp)),
        )
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun GoalLegendItem(color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(width = 6.dp, height = 13.dp).background(color.copy(alpha = .55f), RoundedCornerShape(50)))
        Text("Цель", style = MaterialTheme.typography.labelSmall)
    }
}

internal fun feedingCumulativeScaleMaximum(totalMl: Double, guideMaximumMl: Int?): Double {
    val guideBasis = guideMaximumMl?.takeIf { it > 0 }?.toDouble()?.times(1.08) ?: 0.0
    val actualBasis = totalMl.coerceAtLeast(0.0) * 1.12
    val basis = max(200.0, max(guideBasis, actualBasis))
    return ceil(basis / 100.0) * 100.0
}

internal fun feedingGoalProgressPercent(totalMl: Double, targetMl: Int?): Int? {
    if (targetMl == null || targetMl <= 0) return null
    return (totalMl.coerceAtLeast(0.0) / targetMl * 100.0).roundToInt()
}

internal fun feedingCountLabel(count: Int): String {
    val word = if (count % 100 in 11..14) {
        "кормлений"
    } else {
        when (count % 10) {
            1 -> "кормление"
            in 2..4 -> "кормления"
            else -> "кормлений"
        }
    }
    return "$count $word"
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

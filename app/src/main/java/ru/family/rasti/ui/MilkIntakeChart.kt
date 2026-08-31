package ru.family.rasti.ui

import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.family.rasti.data.FoodEntry
import ru.family.rasti.sleep.SleepSegment
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
    sleepSegments: List<SleepSegment> = emptyList(),
    date: LocalDate,
    minimumMl: Int?,
    targetMl: Int?,
    maximumMl: Int?,
    onEntryClick: (FoodEntry) -> Unit,
) {
    val milkColor = MaterialTheme.colorScheme.primary
    val formulaColor = MaterialTheme.colorScheme.secondary
    val normColor = MaterialTheme.colorScheme.tertiary
    val actualColor = MaterialTheme.colorScheme.onSurface
    val onMilkColor = MaterialTheme.colorScheme.onPrimary
    val onFormulaColor = MaterialTheme.colorScheme.onSecondary
    val onNormColor = MaterialTheme.colorScheme.onTertiary
    val outlineColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val plotColor = MaterialTheme.colorScheme.surfaceVariant
    val nowColor = MaterialTheme.colorScheme.error
    val sleepColor = MaterialTheme.colorScheme.inversePrimary
    val points = remember(entries) { feedingPoints(entries) }
    val total = points.lastOrNull()?.cumulativeMl?.toDouble() ?: 0.0
    val milkTotal = points.filter(FeedingPoint::isMilk).sumOf { it.amountMl.toDouble() }
    val formulaTotal = points.filterNot(FeedingPoint::isMilk).sumOf { it.amountMl.toDouble() }
    val progressPercent = feedingGoalProgressPercent(total, targetMl)
    val scaleMaximum = feedingCumulativeScaleMaximum(total, maximumMl)
    val nowMinute = if (date == LocalDate.now()) LocalTime.now().toSecondOfDay() / 60 else null
    val endMinute = max(points.lastOrNull()?.minute ?: 0, nowMinute ?: 1440).coerceAtMost(1440)
    val density = LocalDensity.current
    val leftTapPadding = with(density) { 34.dp.toPx() }
    val rightTapPadding = with(density) { 6.dp.toPx() }
    val tapRadius = with(density) { 28.dp.toPx() }
    var chartWidth by remember { mutableFloatStateOf(0f) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var viewportCenter by remember { mutableFloatStateOf(720f) }
    val visibleMinutes = 1440f / zoom
    val viewportStart = (viewportCenter - visibleMinutes / 2f).coerceIn(0f, 1440f - visibleMinutes)
    val viewportEnd = viewportStart + visibleMinutes

    val transformState = rememberTransformableState { centroid, zoomChange, panChange, _ ->
        val oldVisibleMinutes = 1440f / zoom
        val oldStart = (viewportCenter - oldVisibleMinutes / 2f).coerceIn(0f, 1440f - oldVisibleMinutes)
        val newZoom = (zoom * zoomChange).coerceIn(1f, 4f)
        val newVisibleMinutes = 1440f / newZoom
        val plotWidth = (chartWidth - leftTapPadding - rightTapPadding).coerceAtLeast(1f)
        val centroidFraction = if (centroid.isSpecified) {
            ((centroid.x - leftTapPadding) / plotWidth).coerceIn(0f, 1f)
        } else {
            .5f
        }
        val minuteAtCentroid = oldStart + centroidFraction * oldVisibleMinutes
        val panMinutes = -panChange.x / plotWidth * newVisibleMinutes
        zoom = newZoom
        viewportCenter = if (newZoom <= 1.01f) {
            720f
        } else {
            val newStart = (minuteAtCentroid - centroidFraction * newVisibleMinutes + panMinutes)
                .coerceIn(0f, 1440f - newVisibleMinutes)
            newStart + newVisibleMinutes / 2f
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val summaryShape = RoundedCornerShape(16.dp)
        val summaryProgress = ((progressPercent ?: 0) / 100f).coerceIn(0f, 1f)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(summaryShape)
                .background(plotColor.copy(alpha = .42f))
                .drawBehind {
                    drawRoundRect(
                        color = milkColor.copy(alpha = .22f),
                        size = Size(size.width * summaryProgress, size.height),
                        cornerRadius = CornerRadius(16.dp.toPx()),
                    )
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
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
                    buildString {
                        append(feedingCountLabel(points.size))
                        targetMl?.let { append(" · цель ≈$it мл") }
                    },
                    color = labelColor,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            progressPercent?.let {
                Text(
                    "$it% цели",
                    color = actualColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(242.dp)
                .onSizeChanged { chartWidth = it.width.toFloat() }
                .transformable(transformState)
                .pointerInput(points, chartWidth, viewportStart, visibleMinutes) {
                    detectTapGestures(
                        onDoubleTap = {
                            zoom = 1f
                            viewportCenter = 720f
                        },
                        onTap = { tap ->
                            val plotWidth = chartWidth - leftTapPadding - rightTapPadding
                            if (plotWidth <= 0f || points.isEmpty()) return@detectTapGestures
                            val visiblePoints = points.filter { it.minute in viewportStart.roundToInt()..viewportEnd.roundToInt() }
                            val nearest = visiblePoints.minByOrNull { point ->
                                abs(leftTapPadding + (point.minute - viewportStart) / visibleMinutes * plotWidth - tap.x)
                            } ?: return@detectTapGestures
                            val nearestX = leftTapPadding + (nearest.minute - viewportStart) / visibleMinutes * plotWidth
                            if (abs(nearestX - tap.x) <= tapRadius) onEntryClick(nearest.entry)
                        },
                    )
                },
        ) {
            val left = 34.dp.toPx()
            val right = size.width - 6.dp.toPx()
            val top = 18.dp.toPx()
            val bottom = size.height - 27.dp.toPx()
            val plotWidth = right - left
            val plotHeight = bottom - top

            fun x(minute: Float): Float = left + (minute - viewportStart) / visibleMinutes * plotWidth
            fun y(amountMl: Float): Float = bottom -
                amountMl.coerceIn(0f, scaleMaximum.toFloat()) / scaleMaximum.toFloat() * plotHeight

            drawRoundRect(
                color = plotColor.copy(alpha = .15f),
                topLeft = Offset(left, top),
                size = Size(plotWidth, plotHeight),
                cornerRadius = CornerRadius(18.dp.toPx()),
            )

            val tickValues = listOf(0f, scaleMaximum.toFloat() / 2f, scaleMaximum.toFloat())
            tickValues.forEach { value ->
                drawLine(
                    color = outlineColor.copy(alpha = if (value == 0f) .28f else .1f),
                    start = Offset(left, y(value)),
                    end = Offset(right, y(value)),
                    strokeWidth = if (value == 0f) 1.2.dp.toPx() else 1.dp.toPx(),
                )
            }

            val timeTickStep = when {
                zoom < 1.5f -> 360
                zoom < 2.5f -> 180
                else -> 120
            }
            val firstTick = (ceil(viewportStart / timeTickStep) * timeTickStep).toInt()
            val timeTicks = generateSequence(firstTick) { it + timeTickStep }
                .takeWhile { it <= viewportEnd + 1f && it <= 1440 }
                .toList()

            timeTicks.forEach { minute ->
                drawLine(
                    color = outlineColor.copy(alpha = .07f),
                    start = Offset(x(minute.toFloat()), top),
                    end = Offset(x(minute.toFloat()), bottom),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            clipRect(left, top, right, bottom) {
                sleepSegments.forEach { sleep ->
                    val start = sleep.startMinute.toFloat().coerceIn(viewportStart, viewportEnd)
                    val end = sleep.endMinute.toFloat().coerceIn(viewportStart, viewportEnd)
                    if (end > start) {
                        val sleepLeft = x(start)
                        val sleepRight = x(end)
                        drawRect(
                            color = sleepColor.copy(alpha = .09f),
                            topLeft = Offset(sleepLeft, top),
                            size = Size(sleepRight - sleepLeft, plotHeight),
                        )
                        drawRoundRect(
                            color = sleepColor.copy(alpha = if (sleep.ongoing) .78f else .58f),
                            topLeft = Offset(sleepLeft, top + 3.dp.toPx()),
                            size = Size((sleepRight - sleepLeft).coerceAtLeast(3.dp.toPx()), 7.dp.toPx()),
                            cornerRadius = CornerRadius(4.dp.toPx()),
                        )
                    }
                }

                if (minimumMl != null && targetMl != null && maximumMl != null) {
                    fun expected(value: Int, minute: Float): Float = value * minute.coerceIn(0f, 1440f) / 1440f
                    val normBand = Path().apply {
                        moveTo(x(viewportStart), y(expected(maximumMl, viewportStart)))
                        lineTo(x(viewportEnd), y(expected(maximumMl, viewportEnd)))
                        lineTo(x(viewportEnd), y(expected(minimumMl, viewportEnd)))
                        lineTo(x(viewportStart), y(expected(minimumMl, viewportStart)))
                        close()
                    }
                    drawPath(normBand, normColor.copy(alpha = .14f))
                    drawLine(
                        color = normColor.copy(alpha = .72f),
                        start = Offset(x(viewportStart), y(expected(targetMl, viewportStart))),
                        end = Offset(x(viewportEnd), y(expected(targetMl, viewportEnd))),
                        strokeWidth = 1.4.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)),
                    )
                }

                if (nowMinute != null && nowMinute in viewportStart.roundToInt()..viewportEnd.roundToInt()) {
                    drawLine(
                        color = nowColor.copy(alpha = .56f),
                        start = Offset(x(nowMinute.toFloat()), top),
                        end = Offset(x(nowMinute.toFloat()), bottom),
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 7f)),
                    )
                }

                if (points.isNotEmpty()) {
                    val linePath = Path().apply {
                        moveTo(x(0f), y(0f))
                        var previousTotal = 0f
                        points.forEach { point ->
                            lineTo(x(point.minute.toFloat()), y(previousTotal))
                            lineTo(x(point.minute.toFloat()), y(point.cumulativeMl))
                            previousTotal = point.cumulativeMl
                        }
                        lineTo(x(endMinute.toFloat()), y(total.toFloat()))
                    }
                    val areaPath = Path().apply {
                        addPath(linePath)
                        lineTo(x(endMinute.toFloat()), bottom)
                        lineTo(x(0f), bottom)
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
                        color = actualColor.copy(alpha = .84f),
                        style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )

                    var previousTotal = 0f
                    points.forEach { point ->
                        if (point.minute.toFloat() in (viewportStart - 1f)..(viewportEnd + 1f)) {
                            val pointX = x(point.minute.toFloat())
                            val pointY = y(point.cumulativeMl)
                            val eventColor = if (point.isMilk) milkColor else formulaColor
                            drawLine(
                                eventColor,
                                Offset(pointX, y(previousTotal)),
                                Offset(pointX, pointY),
                                4.5.dp.toPx(),
                                cap = StrokeCap.Round,
                            )
                            if (point.isMilk) {
                                drawCircle(surfaceColor, 5.2.dp.toPx(), Offset(pointX, pointY))
                                drawCircle(eventColor, 3.4.dp.toPx(), Offset(pointX, pointY))
                            } else {
                                drawRoundRect(
                                    surfaceColor,
                                    Offset(pointX - 5.2.dp.toPx(), pointY - 5.2.dp.toPx()),
                                    Size(10.4.dp.toPx(), 10.4.dp.toPx()),
                                    CornerRadius(2.2.dp.toPx()),
                                )
                                drawRoundRect(
                                    eventColor,
                                    Offset(pointX - 3.4.dp.toPx(), pointY - 3.4.dp.toPx()),
                                    Size(6.8.dp.toPx(), 6.8.dp.toPx()),
                                    CornerRadius(1.4.dp.toPx()),
                                )
                            }
                        }
                        previousTotal = point.cumulativeMl
                    }

                    val visiblePoints = points.filter { it.minute.toFloat() in viewportStart..viewportEnd }
                    val minimumLabelGapPx = 44.dp.toPx()
                    val thresholdMinutes = (visibleMinutes * minimumLabelGapPx / plotWidth)
                        .roundToInt()
                        .coerceIn(12, 120)
                    val labelRanges = feedingLabelGroupRanges(visiblePoints.map(FeedingPoint::minute), thresholdMinutes)
                    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 10.dp.toPx()
                        textAlign = Paint.Align.CENTER
                        isFakeBoldText = true
                    }
                    labelRanges.forEach { range ->
                        val group = visiblePoints.subList(range.first, range.last + 1)
                        val amount = group.sumOf { it.amountMl.toDouble() }
                        val label = formatNumber(amount)
                        val centerMinute = group.map(FeedingPoint::minute).average().toFloat()
                        val anchorY = y(group.last().cumulativeMl)
                        val chipWidth = valuePaint.measureText(label) + 16.dp.toPx()
                        val chipHeight = 21.dp.toPx()
                        val chipLeft = (x(centerMinute) - chipWidth / 2f).coerceIn(left, right - chipWidth)
                        val chipTop = (anchorY - chipHeight - 8.dp.toPx()).coerceIn(top + 3.dp.toPx(), bottom - chipHeight)
                        val chip = RectF(chipLeft, chipTop, chipLeft + chipWidth, chipTop + chipHeight)
                        val milkOnly = group.all(FeedingPoint::isMilk)
                        val formulaOnly = group.none(FeedingPoint::isMilk)
                        val chipColor = when {
                            milkOnly -> milkColor
                            formulaOnly -> formulaColor
                            else -> normColor
                        }
                        valuePaint.color = when {
                            milkOnly -> onMilkColor.toArgb()
                            formulaOnly -> onFormulaColor.toArgb()
                            else -> onNormColor.toArgb()
                        }
                        drawRoundRect(
                            color = chipColor,
                            topLeft = Offset(chip.left, chip.top),
                            size = Size(chip.width(), chip.height()),
                            cornerRadius = CornerRadius(chipHeight / 2f),
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            chip.centerX(),
                            chip.centerY() + valuePaint.textSize / 2.9f,
                            valuePaint,
                        )
                    }
                }

                if (minimumMl != null && targetMl != null && maximumMl != null && viewportEnd >= 1439f) {
                    val goalX = x(1440f) - 4.dp.toPx()
                    val rangeTop = y(maximumMl.toFloat())
                    val rangeBottom = y(minimumMl.toFloat())
                    drawRoundRect(
                        color = normColor.copy(alpha = .48f),
                        topLeft = Offset(goalX - 5.dp.toPx(), rangeTop),
                        size = Size(10.dp.toPx(), (rangeBottom - rangeTop).coerceAtLeast(5.dp.toPx())),
                        cornerRadius = CornerRadius(5.dp.toPx()),
                    )
                    drawCircle(surfaceColor, 7.dp.toPx(), Offset(goalX, y(targetMl.toFloat())))
                    drawCircle(normColor, 5.dp.toPx(), Offset(goalX, y(targetMl.toFloat())))
                    drawCircle(surfaceColor, 2.dp.toPx(), Offset(goalX, y(targetMl.toFloat())))
                }
            }

            val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = labelColor.toArgb()
                textSize = 10.dp.toPx()
            }
            drawContext.canvas.nativeCanvas.apply {
                tickValues.forEach { value ->
                    val label = formatNumber(value.toDouble())
                    drawText(label, left - axisPaint.measureText(label) - 5.dp.toPx(), y(value) + 4.dp.toPx(), axisPaint)
                }
                timeTicks.forEach { minute ->
                    val hour = minute / 60
                    val label = hour.toString()
                    val labelX = (x(minute.toFloat()) - axisPaint.measureText(label) / 2f)
                        .coerceIn(left, right - axisPaint.measureText(label))
                    drawText(label, labelX, size.height - 6.dp.toPx(), axisPaint)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FeedingLegendItem(milkColor, isMilk = true, label = "Молоко ${formatNumber(milkTotal)}")
            FeedingLegendItem(formulaColor, isMilk = false, label = "Смесь ${formatNumber(formulaTotal)}")
            if (targetMl != null) GoalLegendItem(normColor)
            if (sleepSegments.isNotEmpty()) SleepLegendItem(sleepColor)
        }
    }
}

@Composable
private fun SleepLegendItem(color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(width = 13.dp, height = 6.dp).background(color.copy(alpha = .7f), RoundedCornerShape(50)))
        Text("Сон", style = MaterialTheme.typography.labelSmall)
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
        Box(Modifier.size(width = 6.dp, height = 13.dp).background(color.copy(alpha = .6f), RoundedCornerShape(50)))
        Text("Норма", style = MaterialTheme.typography.labelSmall)
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

internal fun feedingLabelGroupRanges(minutes: List<Int>, thresholdMinutes: Int): List<IntRange> {
    if (minutes.isEmpty()) return emptyList()
    val threshold = thresholdMinutes.coerceAtLeast(0)
    val groups = mutableListOf<IntRange>()
    var start = 0
    for (index in 1..minutes.lastIndex) {
        if (minutes[index] - minutes[index - 1] > threshold) {
            groups += start..(index - 1)
            start = index
        }
    }
    groups += start..minutes.lastIndex
    return groups
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

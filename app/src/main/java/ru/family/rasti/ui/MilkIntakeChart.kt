package ru.family.rasti.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
    minimumMl: Int?,
    targetMl: Int?,
    maximumMl: Int?,
    onEntryClick: (FoodEntry) -> Unit,
) {
    val milkColor = MaterialTheme.colorScheme.primary
    val formulaColor = MaterialTheme.colorScheme.secondary
    val rangeColor = MaterialTheme.colorScheme.tertiary
    val markerColor = MaterialTheme.colorScheme.onSurface
    val outlineColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val nowColor = MaterialTheme.colorScheme.error
    val points = remember(entries) { feedingPoints(entries) }
    val milkTotal = points.filter(FeedingPoint::isMilk).sumOf { it.amountMl.toDouble() }
    val formulaTotal = points.filterNot(FeedingPoint::isMilk).sumOf { it.amountMl.toDouble() }
    val total = milkTotal + formulaTotal
    val dailyScaleMaximum = feedingDailyScaleMaximum(total, maximumMl)
    val eventScaleMaximum = feedingEventScaleMaximum(points.maxOfOrNull(FeedingPoint::amountMl)?.toDouble() ?: 0.0)
    val nowMinute = if (date == LocalDate.now()) LocalTime.now().toSecondOfDay() / 60 else null
    val density = LocalDensity.current
    val timelineHorizontalPadding = with(density) { 8.dp.toPx() }
    val tapRadius = with(density) { 28.dp.toPx() }
    var timelineWidth by remember { mutableFloatStateOf(0f) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    "${formatNumber(total)} мл",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    feedingCountLabel(points.size),
                    color = labelColor,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            Text(
                if (minimumMl != null && maximumMl != null) {
                    "ориентир $minimumMl–$maximumMl мл"
                } else {
                    "учтено за сутки"
                },
                color = labelColor,
                style = MaterialTheme.typography.labelSmall,
            )
        }

        Canvas(Modifier.fillMaxWidth().height(58.dp)) {
            val left = 4.dp.toPx()
            val right = size.width - 4.dp.toPx()
            val trackTop = 7.dp.toPx()
            val trackHeight = 18.dp.toPx()
            val trackBottom = trackTop + trackHeight
            val radius = trackHeight / 2f

            fun x(amountMl: Double): Float = left +
                (amountMl.coerceIn(0.0, dailyScaleMaximum) / dailyScaleMaximum).toFloat() * (right - left)

            drawRoundRect(
                color = trackColor.copy(alpha = .5f),
                topLeft = Offset(left, trackTop),
                size = Size(right - left, trackHeight),
                cornerRadius = CornerRadius(radius),
            )

            if (minimumMl != null && maximumMl != null) {
                val rangeLeft = x(minimumMl.toDouble())
                val rangeRight = x(maximumMl.toDouble())
                drawRoundRect(
                    color = rangeColor.copy(alpha = .25f),
                    topLeft = Offset(rangeLeft, trackTop),
                    size = Size((rangeRight - rangeLeft).coerceAtLeast(0f), trackHeight),
                    cornerRadius = CornerRadius(radius),
                )
            }

            if (milkTotal > 0.0) {
                drawRoundRect(
                    color = milkColor,
                    topLeft = Offset(left, trackTop),
                    size = Size((x(milkTotal) - left).coerceAtLeast(0f), trackHeight),
                    cornerRadius = CornerRadius(radius),
                )
            }
            if (formulaTotal > 0.0) {
                val gap = if (milkTotal > 0.0) 2.dp.toPx() else 0f
                val formulaLeft = x(milkTotal) + gap
                val formulaRight = x(total)
                drawRoundRect(
                    color = formulaColor,
                    topLeft = Offset(formulaLeft.coerceAtMost(formulaRight), trackTop),
                    size = Size((formulaRight - formulaLeft).coerceAtLeast(0f), trackHeight),
                    cornerRadius = CornerRadius(radius),
                )
            }

            if (targetMl != null && targetMl > 0) {
                val targetX = x(targetMl.toDouble())
                drawLine(
                    color = markerColor,
                    start = Offset(targetX, trackTop - 4.dp.toPx()),
                    end = Offset(targetX, trackBottom + 4.dp.toPx()),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = labelColor.toArgb()
                textSize = 10.dp.toPx()
            }
            drawContext.canvas.nativeCanvas.apply {
                drawText("0", left, size.height - 5.dp.toPx(), labelPaint)
                targetMl?.takeIf { it > 0 }?.let { target ->
                    val text = "≈$target"
                    val textX = (x(target.toDouble()) - labelPaint.measureText(text) / 2)
                        .coerceIn(left, right - labelPaint.measureText(text))
                    drawText(text, textX, size.height - 5.dp.toPx(), labelPaint)
                }
                val maximumText = formatNumber(dailyScaleMaximum)
                drawText(maximumText, right - labelPaint.measureText(maximumText), size.height - 5.dp.toPx(), labelPaint)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FeedingLegendItem(milkColor, isMilk = true, label = "Молоко ${formatNumber(milkTotal)} мл")
            FeedingLegendItem(formulaColor, isMilk = false, label = "Смесь ${formatNumber(formulaTotal)} мл")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Кормления по времени", style = MaterialTheme.typography.labelLarge)
            Text("объём каждого кормления", color = labelColor, style = MaterialTheme.typography.labelSmall)
        }

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(112.dp)
                .onSizeChanged { timelineWidth = it.width.toFloat() }
                .pointerInput(points, timelineWidth) {
                    detectTapGestures { tap ->
                        val plotWidth = timelineWidth - timelineHorizontalPadding * 2
                        if (plotWidth <= 0f || points.isEmpty()) return@detectTapGestures
                        val nearest = points.minByOrNull { point ->
                            abs(timelineHorizontalPadding + point.minute / 1440f * plotWidth - tap.x)
                        } ?: return@detectTapGestures
                        val nearestX = timelineHorizontalPadding + nearest.minute / 1440f * plotWidth
                        if (abs(nearestX - tap.x) <= tapRadius) onEntryClick(nearest.entry)
                    }
                },
        ) {
            val left = 8.dp.toPx()
            val right = size.width - 8.dp.toPx()
            val top = 8.dp.toPx()
            val bottom = size.height - 24.dp.toPx()
            val plotHeight = bottom - top
            val barWidth = when {
                points.size <= 8 -> 10.dp.toPx()
                points.size <= 14 -> 7.dp.toPx()
                else -> 5.dp.toPx()
            }

            fun x(minute: Int): Float = left + minute.coerceIn(0, 1440) / 1440f * (right - left)
            fun y(amountMl: Float): Float = bottom -
                amountMl.coerceIn(0f, eventScaleMaximum.toFloat()) / eventScaleMaximum.toFloat() * plotHeight

            drawLine(
                color = outlineColor.copy(alpha = .35f),
                start = Offset(left, bottom),
                end = Offset(right, bottom),
                strokeWidth = 1.2.dp.toPx(),
                cap = StrokeCap.Round,
            )
            listOf(0, 360, 720, 1080, 1440).forEach { minute ->
                drawLine(
                    color = outlineColor.copy(alpha = .09f),
                    start = Offset(x(minute), top),
                    end = Offset(x(minute), bottom),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            if (nowMinute != null) {
                drawLine(
                    color = nowColor.copy(alpha = .6f),
                    start = Offset(x(nowMinute), top),
                    end = Offset(x(nowMinute), bottom),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 7f)),
                )
            }

            points.forEach { point ->
                val eventColor = if (point.isMilk) milkColor else formulaColor
                val barTop = y(point.amountMl).coerceAtMost(bottom - 7.dp.toPx())
                if (point.isMilk) {
                    drawRoundRect(
                        color = eventColor,
                        topLeft = Offset(x(point.minute) - barWidth / 2, barTop),
                        size = Size(barWidth, bottom - barTop),
                        cornerRadius = CornerRadius(barWidth / 2),
                    )
                } else {
                    drawRoundRect(
                        color = eventColor,
                        topLeft = Offset(x(point.minute) - barWidth / 2, barTop),
                        size = Size(barWidth, bottom - barTop),
                        cornerRadius = CornerRadius(1.5.dp.toPx()),
                    )
                }
            }

            val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = labelColor.toArgb()
                textSize = 10.dp.toPx()
            }
            drawContext.canvas.nativeCanvas.apply {
                listOf(0 to "0", 360 to "6", 720 to "12", 1080 to "18", 1440 to "24").forEach { (minute, label) ->
                    val labelX = (x(minute) - axisPaint.measureText(label) / 2)
                        .coerceIn(left, right - axisPaint.measureText(label))
                    drawText(label, labelX, size.height - 5.dp.toPx(), axisPaint)
                }
            }
        }

        if (points.isNotEmpty()) {
            Text(
                "Нажмите на столбик, чтобы изменить кормление",
                modifier = Modifier.fillMaxWidth(),
                color = labelColor,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun FeedingLegendItem(color: Color, isMilk: Boolean, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(9.dp)
                .background(color, if (isMilk) CircleShape else RoundedCornerShape(2.dp)),
        )
        Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
    }
}

internal fun feedingDailyScaleMaximum(totalMl: Double, guideMaximumMl: Int?): Double {
    val safeTotal = totalMl.coerceAtLeast(0.0)
    if (guideMaximumMl != null && guideMaximumMl > 0 && safeTotal <= guideMaximumMl) {
        return guideMaximumMl.toDouble()
    }
    val basis = if (guideMaximumMl == null) max(200.0, safeTotal * 1.15) else max(guideMaximumMl.toDouble(), safeTotal)
    return ceil(basis / 100.0) * 100.0
}

internal fun feedingEventScaleMaximum(maximumFeedingMl: Double): Int {
    val basis = max(100.0, maximumFeedingMl.coerceAtLeast(0.0))
    return (ceil(basis / 50.0) * 50.0).toInt()
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

private fun feedingPoints(entries: List<FoodEntry>): List<FeedingPoint> =
    entries.mapNotNull { entry ->
        val time = runCatching { LocalTime.parse(entry.time, DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull()
            ?: return@mapNotNull null
        FeedingPoint(
            entry = entry,
            minute = time.toSecondOfDay() / 60,
            amountMl = entry.amount.toFloat().coerceAtLeast(0f),
            isMilk = entry.name.equals("Молоко", ignoreCase = true),
        )
    }.sortedBy(FeedingPoint::minute)

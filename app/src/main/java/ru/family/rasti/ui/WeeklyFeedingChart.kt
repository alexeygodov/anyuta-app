package ru.family.rasti.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.family.rasti.data.AppData
import ru.family.rasti.data.day
import ru.family.rasti.feeding.FeedingGuide
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

internal data class DailyFeedingSummary(
    val date: LocalDate,
    val milkMl: Float,
    val formulaMl: Float,
    val minimumMl: Float?,
    val maximumMl: Float?,
) {
    val totalMl: Float get() = milkMl + formulaMl
}

internal fun weeklyFeedingSummaries(data: AppData, endDate: LocalDate = LocalDate.now()): List<DailyFeedingSummary> =
    (6L downTo 0L).map { daysAgo ->
        val date = endDate.minusDays(daysAgo)
        val entries = data.day(date).food.filter { it.unit.trim().lowercase() in setOf("мл", "ml") }
        val milk = entries.filter { it.name.trim().lowercase() == "молоко" }.sumOf { it.amount }.toFloat()
        val formula = entries.filter { it.name.trim().lowercase() == "смесь" }.sumOf { it.amount }.toFloat()
        val guide = FeedingGuide.calculate(data, date).guide
        DailyFeedingSummary(
            date = date,
            milkMl = milk,
            formulaMl = formula,
            minimumMl = guide?.minimumMl?.toFloat(),
            maximumMl = guide?.maximumMl?.toFloat(),
        )
    }

@Composable
internal fun WeeklyFeedingCard(data: AppData) {
    val points = remember(data) { weeklyFeedingSummaries(data) }
    val milkColor = MaterialTheme.colorScheme.primary
    val formulaColor = MaterialTheme.colorScheme.secondary
    val rangeColor = MaterialTheme.colorScheme.tertiary
    val axisColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Питание за 7 дней", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("● Молоко", color = milkColor, style = MaterialTheme.typography.bodySmall)
                Text("● Смесь", color = formulaColor, style = MaterialTheme.typography.bodySmall)
                Text("▥ Норма", color = rangeColor, style = MaterialTheme.typography.bodySmall)
            }
            Canvas(Modifier.fillMaxWidth().height(240.dp)) {
                val left = 42.dp.toPx()
                val right = size.width - 8.dp.toPx()
                val top = 14.dp.toPx()
                val bottom = size.height - 42.dp.toPx()
                val chartHeight = bottom - top
                val yMax = max(
                    100f,
                    points.maxOf { max(it.totalMl, it.maximumMl ?: 0f) } * 1.12f,
                )
                fun y(value: Float): Float = bottom - value.coerceAtLeast(0f) / yMax * chartHeight
                val slot = (right - left) / points.size
                val barWidth = slot * .5f
                val rangeWidth = slot * .72f

                drawLine(axisColor, Offset(left, bottom), Offset(right, bottom), 1.dp.toPx())
                drawLine(axisColor, Offset(left, top), Offset(left, bottom), 1.dp.toPx())
                points.forEachIndexed { index, point ->
                    val centerX = left + slot * (index + .5f)
                    if (point.minimumMl != null && point.maximumMl != null) {
                        drawRoundRect(
                            color = rangeColor.copy(alpha = .20f),
                            topLeft = Offset(centerX - rangeWidth / 2f, y(point.maximumMl)),
                            size = Size(rangeWidth, y(point.minimumMl) - y(point.maximumMl)),
                            cornerRadius = CornerRadius(4.dp.toPx()),
                        )
                        drawLine(
                            rangeColor.copy(alpha = .8f),
                            Offset(centerX - rangeWidth / 2f, y(point.minimumMl)),
                            Offset(centerX + rangeWidth / 2f, y(point.minimumMl)),
                            1.dp.toPx(),
                        )
                        drawLine(
                            rangeColor.copy(alpha = .8f),
                            Offset(centerX - rangeWidth / 2f, y(point.maximumMl)),
                            Offset(centerX + rangeWidth / 2f, y(point.maximumMl)),
                            1.dp.toPx(),
                        )
                    }
                    if (point.formulaMl > 0f) {
                        drawRect(
                            formulaColor,
                            topLeft = Offset(centerX - barWidth / 2f, y(point.formulaMl)),
                            size = Size(barWidth, bottom - y(point.formulaMl)),
                        )
                    }
                    if (point.milkMl > 0f) {
                        drawRect(
                            milkColor,
                            topLeft = Offset(centerX - barWidth / 2f, y(point.totalMl)),
                            size = Size(barWidth, y(point.formulaMl) - y(point.totalMl)),
                        )
                    }
                }

                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = labelColor.toArgb()
                    textSize = 10.dp.toPx()
                    textAlign = Paint.Align.CENTER
                }
                val formatter = DateTimeFormatter.ofPattern("EE\ndd", Locale.forLanguageTag("ru"))
                drawContext.canvas.nativeCanvas.apply {
                    points.forEachIndexed { index, point ->
                        val x = left + slot * (index + .5f)
                        val labels = point.date.format(formatter).split('\n')
                        drawText(labels[0].replace(".", ""), x, bottom + 16.dp.toPx(), paint)
                        drawText(labels[1], x, bottom + 31.dp.toPx(), paint)
                    }
                    paint.textAlign = Paint.Align.RIGHT
                    drawText("${yMax.toInt()}", left - 5.dp.toPx(), top + 4.dp.toPx(), paint)
                    drawText("0", left - 5.dp.toPx(), bottom, paint)
                }
            }
            val today = points.last()
            Text(
                "Сегодня: ${today.totalMl.toInt()} мл — молоко ${today.milkMl.toInt()}, смесь ${today.formulaMl.toInt()}.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Полупрозрачный коридор — расчётный суточный диапазон по последнему известному весу на каждую дату.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

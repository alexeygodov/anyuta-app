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
import ru.family.rasti.sleep.SleepSegment
import ru.family.rasti.sleep.formatSleepDuration
import ru.family.rasti.sleep.sleepsForDate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

internal data class DailySleepSummary(
    val date: LocalDate,
    val segments: List<SleepSegment>,
) {
    val totalMinutes: Long get() = segments.sumOf { (it.endMinute - it.startMinute).toLong() }
}

internal fun weeklySleepSummaries(
    data: AppData,
    endDate: LocalDate = LocalDate.now(),
    now: LocalDateTime = LocalDateTime.now(),
): List<DailySleepSummary> = (6L downTo 0L).map { daysAgo ->
    val date = endDate.minusDays(daysAgo)
    DailySleepSummary(date, sleepsForDate(data, date, now))
}

@Composable
internal fun WeeklySleepCard(data: AppData) {
    val points = remember(data.days) { weeklySleepSummaries(data) }
    val sleepColor = Color(0xFF7C9EE8)
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val columnColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f)

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Сон за 7 дней", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("■", color = sleepColor, style = MaterialTheme.typography.bodySmall)
                Text(
                    "Закрашено время, когда ребёнок спал",
                    color = labelColor,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Canvas(Modifier.fillMaxWidth().height(310.dp)) {
                val left = 38.dp.toPx()
                val right = size.width - 7.dp.toPx()
                val top = 12.dp.toPx()
                val bottom = size.height - 38.dp.toPx()
                val chartHeight = bottom - top
                val slot = (right - left) / points.size
                val columnWidth = slot * .58f
                fun y(minute: Int): Float = top + minute.coerceIn(0, 1440) / 1440f * chartHeight

                listOf(0, 360, 720, 1080, 1440).forEach { minute ->
                    drawLine(
                        color = axisColor,
                        start = Offset(left, y(minute)),
                        end = Offset(right, y(minute)),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                points.forEachIndexed { index, point ->
                    val centerX = left + slot * (index + .5f)
                    val x = centerX - columnWidth / 2f
                    drawRoundRect(
                        color = columnColor,
                        topLeft = Offset(x, top),
                        size = Size(columnWidth, chartHeight),
                        cornerRadius = CornerRadius(7.dp.toPx()),
                    )
                    point.segments.forEach { segment ->
                        val segmentTop = y(segment.startMinute)
                        val segmentHeight = max(3.dp.toPx(), y(segment.endMinute) - segmentTop)
                        drawRoundRect(
                            color = sleepColor.copy(alpha = if (segment.ongoing) 1f else .86f),
                            topLeft = Offset(x, segmentTop),
                            size = Size(columnWidth, segmentHeight),
                            cornerRadius = CornerRadius(5.dp.toPx()),
                        )
                    }
                }

                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = labelColor.toArgb()
                    textSize = 10.dp.toPx()
                }
                val formatter = DateTimeFormatter.ofPattern("EE\ndd", Locale.forLanguageTag("ru"))
                drawContext.canvas.nativeCanvas.apply {
                    paint.textAlign = Paint.Align.RIGHT
                    listOf(0, 360, 720, 1080, 1440).forEach { minute ->
                        drawText("%02d".format(minute / 60), left - 6.dp.toPx(), y(minute) + 4.dp.toPx(), paint)
                    }
                    paint.textAlign = Paint.Align.CENTER
                    points.forEachIndexed { index, point ->
                        val x = left + slot * (index + .5f)
                        val labels = point.date.format(formatter).split('\n')
                        drawText(labels[0].replace(".", ""), x, bottom + 15.dp.toPx(), paint)
                        drawText(labels[1], x, bottom + 29.dp.toPx(), paint)
                    }
                }
            }
            val today = points.last()
            Text(
                if (today.totalMinutes > 0) {
                    "Сегодня: ${formatSleepDuration(today.totalMinutes)} сна."
                } else {
                    "Сегодня сон пока не записан."
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "Сон через полночь делится между двумя днями по фактическому времени в каждом дне.",
                style = MaterialTheme.typography.bodySmall,
                color = labelColor,
            )
        }
    }
}

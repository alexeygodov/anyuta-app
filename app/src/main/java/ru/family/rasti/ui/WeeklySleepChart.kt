package ru.family.rasti.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.family.rasti.data.AppData
import ru.family.rasti.sleep.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max

internal data class DailySleepSummary(
    val date: LocalDate,
    val segments: List<SleepSegment>,
    val awake: List<Pair<Int, Int>> = emptyList(),
) {
    val totalMinutes: Long get() = mergedSleepSegments(segments).sumOf { (it.endMinute - it.startMinute).toLong() }
}

internal fun weeklySleepSummaries(
    data: AppData,
    endDate: LocalDate = LocalDate.now(),
    now: LocalDateTime = LocalDateTime.now(),
): List<DailySleepSummary> = (6L downTo 0L).map { daysAgo ->
    val date = endDate.minusDays(daysAgo)
    DailySleepSummary(date, mergedSleepSegments(sleepsForDate(data, date, now)), awakeSegmentsForDate(data, date, now))
}

private fun clockMinute(minute: Int) = "%02d:%02d".format(minute / 60, minute % 60)
private fun shortDuration(minutes: Int) = if (minutes < 60) "${minutes}м" else "${minutes / 60}ч" + if (minutes % 60 == 0) "" else "${minutes % 60}"

@Composable
internal fun WeeklySleepCard(data: AppData) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { delay(60_000); now = LocalDateTime.now() } }
    val points = remember(data.days, now) { weeklySleepSummaries(data, now.toLocalDate(), now) }
    var selected by remember { mutableStateOf(6) }
    var expanded by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val scroll = rememberScrollState()
    LaunchedEffect(scroll.maxValue) { scroll.scrollTo(scroll.maxValue) }
    val formatter = remember { DateTimeFormatter.ofPattern("EE dd.MM", Locale.forLanguageTag("ru")) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Сон и бодрствование", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("■ Сон", style = MaterialTheme.typography.bodySmall, color = colors.tertiary)
                Text("■ Бодрствование ≈", style = MaterialTheme.typography.bodySmall, color = colors.secondary)
            }
            Row(Modifier.fillMaxWidth()) {
                Canvas(Modifier.width(28.dp).height(440.dp)) {
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 11.sp.toPx(); textAlign = Paint.Align.CENTER; color = colors.onSurfaceVariant.toArgb()
                    }
                    listOf(0, 6, 12, 18, 24).forEach { hour ->
                        val y = 26.dp.toPx() + hour / 24f * (size.height - 62.dp.toPx())
                        drawContext.canvas.nativeCanvas.drawText("$hour", 12.dp.toPx(), y + 4.dp.toPx(), paint)
                    }
                }
            Box(Modifier.weight(1f).horizontalScroll(scroll)) {
                Canvas(Modifier.width(538.dp).height(440.dp).pointerInput(points) {
                    detectTapGestures { position ->
                        selected = ((position.x - 4.dp.toPx()) / 76.dp.toPx()).toInt().coerceIn(0, 6)
                    }
                }) {
                    val left = 4.dp.toPx()
                    val top = 26.dp.toPx()
                    val bottom = size.height - 36.dp.toPx()
                    val slot = 76.dp.toPx()
                    val width = 66.dp.toPx()
                    fun y(minute: Int) = top + minute / 1440f * (bottom - top)
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11.sp.toPx(); textAlign = Paint.Align.CENTER }
                    listOf(0, 360, 720, 1080, 1440).forEach { minute ->
                        drawLine(colors.outlineVariant, Offset(left, y(minute)), Offset(size.width, y(minute)), 1.dp.toPx())
                    }
                    points.forEachIndexed { index, point ->
                        val x = left + slot * index + 4.dp.toPx()
                        drawRoundRect(if (index == selected) colors.surfaceContainerHighest else colors.surfaceContainerLow,
                            Offset(x, top), Size(width, bottom - top), CornerRadius(8.dp.toPx()))
                        fun block(start: Int, end: Int, sleep: Boolean) {
                            val blockHeight = max(2.dp.toPx(), y(end) - y(start))
                            drawRoundRect(if (sleep) colors.tertiary else colors.secondaryContainer, Offset(x, y(start)),
                                Size(width, blockHeight), CornerRadius(4.dp.toPx()))
                            if (blockHeight >= 16.sp.toPx()) {
                                paint.color = (if (sleep) colors.onTertiary else colors.onSecondaryContainer).toArgb()
                                val baseline = (y(start) + y(end)) / 2 - (paint.ascent() + paint.descent()) / 2
                                drawContext.canvas.nativeCanvas.drawText(shortDuration(end - start), x + width / 2, baseline, paint)
                            }
                        }
                        point.awake.forEach { block(it.first, it.second, false) }
                        point.segments.forEach { block(it.startMinute, it.endMinute, true) }
                        paint.color = (if (index == selected) colors.primary else colors.onSurfaceVariant).toArgb()
                        drawContext.canvas.nativeCanvas.apply {
                            drawText(shortDuration(point.totalMinutes.toInt()), x + width / 2, 16.dp.toPx(), paint)
                            drawText(point.date.format(DateTimeFormatter.ofPattern("dd.MM")), x + width / 2, bottom + 22.dp.toPx(), paint)
                        }
                    }
                }
            }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                points.forEachIndexed { index, point ->
                    FilterChip(selected == index, { selected = index }, label = { Text(point.date.format(formatter)) })
                }
            }
            val day = points[selected]
            Text("${day.date.format(formatter)} · Сон ${formatSleepDuration(day.totalMinutes)}", style = MaterialTheme.typography.titleMedium)
            if (day.awake.isNotEmpty()) Text(
                "Бодрствование ≈ ${formatSleepDuration(day.awake.sumOf { (it.second - it.first).toLong() })}",
                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant,
            )
            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Скрыть интервалы" else "Все длительности дня") }
            if (expanded) {
                val rows = day.segments.map { Triple(it.startMinute, it.endMinute, true) } + day.awake.map { Triple(it.first, it.second, false) }
                if (rows.isEmpty()) Text("Нет записей сна")
                rows.sortedBy { it.first }.forEach { (start, end, sleep) ->
                    Text("${clockMinute(start)}–${clockMinute(end)} · ${if (sleep) "Сон" else "Бодрствование ≈"} ${formatSleepDuration((end - start).toLong())}",
                        style = MaterialTheme.typography.bodyMedium, color = if (sleep) colors.tertiary else colors.onSurfaceVariant)
                }
                Text("Паузы — оценка по дневнику, пропущенный сон неизвестен. Пробелы более 12 часов не считаются бодрствованием. Через полночь длительности делятся по дням.",
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
            Text("Листайте вбок · сверху суммарный сон · паузы оценены по записям.",
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}

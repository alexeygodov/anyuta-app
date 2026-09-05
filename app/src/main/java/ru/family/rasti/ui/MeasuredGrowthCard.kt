package ru.family.rasti.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import ru.family.rasti.data.AppData
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

internal data class GrowthInterval(val start: LocalDate, val end: LocalDate, val change: Double) {
    val days: Long get() = ChronoUnit.DAYS.between(start, end)
    val perDay: Double get() = change / days
}

/** Use at least a week, not an extrapolation of noisy day-to-day measurements. */
internal fun measuredGrowthIntervals(data: AppData, weight: Boolean, today: LocalDate = LocalDate.now()): List<GrowthInterval> {
    val measurements = data.days.values.mapNotNull { day ->
        val date = runCatching { LocalDate.parse(day.date) }.getOrNull() ?: return@mapNotNull null
        val value = (if (weight) day.measurement?.weightKg?.times(1000) else day.measurement?.heightCm)
            ?.takeIf { it.isFinite() && it > 0 } ?: return@mapNotNull null
        if (date > today) null else date to value
    }.sortedBy { it.first }
    val result = mutableListOf<GrowthInterval>()
    var anchor = measurements.firstOrNull() ?: return result
    measurements.drop(1).forEach { current ->
        if (ChronoUnit.DAYS.between(anchor.first, current.first) >= 7) {
            result.add(GrowthInterval(anchor.first, current.first, current.second - anchor.second))
            anchor = current
        }
    }
    return result
}

@Composable
internal fun MeasuredGrowthCard(data: AppData) {
    var weight by remember { mutableStateOf(true) }
    val today = LocalDate.now()
    val points = remember(data.days, weight, today) { measuredGrowthIntervals(data, weight, today).takeLast(6) }
    val colors = MaterialTheme.colorScheme
    val formatter = remember { DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("ru")) }
    fun value(number: Double) = String.format(Locale.forLanguageTag("ru"), "%+.1f", number)
    val unit = if (weight) "г/день" else "см/30 дней"
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Темп роста", style = MaterialTheme.typography.titleLarge)
            Text("По вашим замерам · без прогноза скачков", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(weight, { weight = true }, label = { Text("Вес") })
                FilterChip(!weight, { weight = false }, label = { Text("Рост") })
            }
            if (points.isEmpty()) {
                Text("Нужны два замера ${if (weight) "веса" else "роста"} с разницей не менее 7 дней.")
            } else {
                val maxRate = points.maxOf { abs(it.perDay) }.coerceAtLeast(.001)
                points.forEach { point ->
                    val rate = point.perDay * if (weight) 1 else 30
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("${point.start.format(formatter)} — ${point.end.format(formatter)} · ${point.days} дн.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        Text("${value(rate)} $unit", style = MaterialTheme.typography.titleMedium)
                        Canvas(Modifier.fillMaxWidth().height(18.dp)) {
                            val center = size.width / 2
                            drawLine(colors.outlineVariant, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), 3.dp.toPx(), StrokeCap.Round)
                            drawLine(colors.outline, Offset(center, 0f), Offset(center, size.height), 1.dp.toPx())
                            drawLine(if (weight) colors.primary else colors.tertiary, Offset(center, size.height / 2),
                                Offset(center + (point.perDay / maxRate).toFloat() * (center - 6.dp.toPx()), size.height / 2), 9.dp.toPx(), StrokeCap.Round)
                        }
                        Text("Всего ${value(point.change)} ${if (weight) "г" else "см"}", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                }
            }
            Text("Это средний темп между замерами, а не дата скачка или оценка здоровья. Короткие интервалы объединены минимум до 7 дней; оставшийся короткий интервал пока не показан. Погрешность измерений влияет на результат. Возрастные ориентиры — на графиках ВОЗ выше.",
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}

package ru.family.rasti.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import ru.family.rasti.data.AppData
import ru.family.rasti.data.ChildProfile
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs

internal data class CalendarLeap(val number: Int, val week: Int, val name: String)
internal val calendarLeaps = listOf(
    CalendarLeap(1, 5, "Ощущения"), CalendarLeap(2, 8, "Закономерности"),
    CalendarLeap(3, 12, "Переходы"), CalendarLeap(4, 19, "События"),
    CalendarLeap(5, 26, "Связи"), CalendarLeap(6, 37, "Категории"),
    CalendarLeap(7, 46, "Последовательности"), CalendarLeap(8, 55, "Программы"),
    CalendarLeap(9, 64, "Принципы"), CalendarLeap(10, 75, "Системы"),
)

internal fun leapReference(profile: ChildProfile): LocalDate? =
    runCatching { LocalDate.parse(profile.dueDate.ifBlank { profile.birthDate }) }.getOrNull()

internal fun nearbyCalendarLeap(reference: LocalDate, date: LocalDate): CalendarLeap? =
    calendarLeaps.minByOrNull { abs(ChronoUnit.DAYS.between(reference.plusWeeks(it.week.toLong()), date)) }
        ?.takeIf { abs(ChronoUnit.DAYS.between(reference.plusWeeks(it.week.toLong()), date)) <= 7 }

@Composable
internal fun DevelopmentCalendarCard(data: AppData, onFussinessChange: (LocalDate, Int?) -> Unit) {
    val today = LocalDate.now()
    val reference = leapReference(data.profile)
    var selectedDate by remember { mutableStateOf(today) }
    var showAll by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val colors = MaterialTheme.colorScheme
    val uri = LocalUriHandler.current
    val formatter = remember { DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("ru")) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    LaunchedEffect(scroll.maxValue) { scroll.scrollTo((with(density) { (30 * 38).dp.toPx() }.toInt() - scroll.viewportSize / 2).coerceAtLeast(0)) }
    fun moodColor(level: Int?): Color = when (level) { 0 -> colors.primaryContainer; 1 -> colors.secondaryContainer; 2 -> colors.errorContainer; else -> colors.surfaceContainerHighest }
    fun moodLabel(level: Int?) = when (level) { 0 -> "Спокойно"; 1 -> "Капризничает"; 2 -> "Сильно беспокоится"; else -> "Нет отметки" }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Календарь скачков развития", style = MaterialTheme.typography.titleLarge)
            Text("Условный календарь Wonder Weeks, не прогноз состояния ребёнка. Это развитие, не прибавка роста или веса.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            if (reference == null) {
                Text("Проверьте дату рождения и ПДР в настройках.")
            } else {
                Text(if (data.profile.dueDate.isNotBlank()) "Отсчёт от ПДР ${reference.format(formatter)}" else "ПДР не указана: приблизительно от рождения", style = MaterialTheme.typography.bodySmall)
                Text("Верхний ряд ≈ календарь · нижний — ваши отметки", style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth().horizontalScroll(scroll), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    (-30L..30L).forEach { offset ->
                        val date = today.plusDays(offset)
                        val near = nearbyCalendarLeap(reference, date)
                        val level = data.days[date.toString()]?.fussiness
                        Column(Modifier.width(34.dp).clickable { selectedDate = date }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (offset == 0L) "●" else "", color = colors.primary, style = MaterialTheme.typography.labelSmall)
                            Surface(color = if (near != null) colors.secondaryContainer else colors.surfaceContainerLow, shape = MaterialTheme.shapes.small) {
                                Box(Modifier.size(34.dp, 30.dp), contentAlignment = Alignment.Center) { Text(near?.let { "≈${it.number}" } ?: "·", style = MaterialTheme.typography.labelSmall) }
                            }
                            Surface(color = moodColor(level), shape = MaterialTheme.shapes.small) {
                                Box(Modifier.size(34.dp, 30.dp), contentAlignment = Alignment.Center) { Text(when (level) { 0 -> "✓"; 1 -> "!"; 2 -> "!!"; else -> "—" }, color = when(level) { 0 -> colors.onPrimaryContainer; 1 -> colors.onSecondaryContainer; 2 -> colors.onErrorContainer; else -> colors.onSurfaceVariant }) }
                            }
                            Text(date.format(DateTimeFormatter.ofPattern("dd.MM")), color = if (date == selectedDate) colors.primary else colors.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                Text("${selectedDate.format(formatter)} · ${moodLabel(data.days[selectedDate.toString()]?.fussiness)}", style = MaterialTheme.typography.titleMedium)
                if (selectedDate <= today) {
                    // A single tap records observation; no extra diary or skill tracking.
                    Column {
                        listOf("Спокойно", "Капризничает", "Сильно беспокоится").forEachIndexed { level, label ->
                            FilterChip(selected = data.days[selectedDate.toString()]?.fussiness == level,
                                onClick = { onFussinessChange(selectedDate, level) }, label = { Text(label) })
                        }
                        if (data.days[selectedDate.toString()]?.fussiness != null) TextButton(onClick = { onFussinessChange(selectedDate, null) }) { Text("Убрать отметку") }
                    }
                } else Text("Будущее состояние неизвестно — оно не окрашивается как факт.", style = MaterialTheme.typography.bodySmall)

                val nextIndex = calendarLeaps.indexOfFirst { reference.plusWeeks(it.week.toLong()) >= today }.let { if (it < 0) calendarLeaps.lastIndex else it }
                val visibleLeaps = if (showAll) calendarLeaps else calendarLeaps.subList((nextIndex - 1).coerceAtLeast(0), (nextIndex + 2).coerceAtMost(calendarLeaps.size))
                visibleLeaps.forEach { leap ->
                    val date = reference.plusWeeks(leap.week.toLong())
                    val delta = ChronoUnit.DAYS.between(today, date)
                    val close = abs(delta) <= 7
                    Surface(color = if (close) colors.secondaryContainer else colors.surfaceContainerLow, shape = MaterialTheme.shapes.small) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text("${leap.number}. ${leap.name} · ≈${leap.week} нед.", style = MaterialTheme.typography.titleMedium)
                            Text("${date.format(formatter)} · " + when { delta < 0 -> "календарная дата прошла"; delta == 0L -> "календарный ориентир сегодня"; else -> "ориентир через $delta дн." }, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                TextButton(onClick = { showAll = !showAll }) { Text(if (showAll) "Только ближайшие" else "Все 10 ориентиров: прошедшие и будущие") }
            }
            Text("Жёлтая полоса — условное окно ±7 дней от даты модели, не степень беспокойства. Зелёный / персиковый / красный в нижнем ряду — только ваша оценка. Календарь не подтверждает, что скачок произошёл; необычный плач или недомогание нельзя объяснять одним скачком.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            TextButton(onClick = { uri.openUri("https://thewonderweeks.com/blog/leaps/crankiness-and-crying/") }) { Text("Источник календаря") }
        }
    }
}

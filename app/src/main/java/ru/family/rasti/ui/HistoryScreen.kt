package ru.family.rasti.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.family.rasti.RastiViewModel
import ru.family.rasti.data.DayRecord
import ru.family.rasti.data.FoodEntry
import ru.family.rasti.data.Measurement
import ru.family.rasti.data.SleepEntry
import ru.family.rasti.data.VitaminEntry
import ru.family.rasti.data.displayDose
import ru.family.rasti.sleep.formatSleepDuration
import ru.family.rasti.sleep.sleepDurationMinutes
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class HistoryFoodEdit(val date: LocalDate, val entry: FoodEntry)
private data class HistoryVitaminEdit(val date: LocalDate, val entry: VitaminEntry)
private data class HistoryMeasurementEdit(val date: LocalDate, val measurement: Measurement)
private data class HistorySleepEdit(val date: LocalDate, val entry: SleepEntry)

@Composable
fun HistoryScreen(viewModel: RastiViewModel, modifier: Modifier = Modifier) {
    val days = viewModel.data.days.values.sortedByDescending { it.date }
    val weekStart = LocalDate.now().minusDays(6).toString()
    val week = days.filter { it.date >= weekStart && it.date <= LocalDate.now().toString() }
    var foodEdit by remember { mutableStateOf<HistoryFoodEdit?>(null) }
    var vitaminEdit by remember { mutableStateOf<HistoryVitaminEdit?>(null) }
    var measurementEdit by remember { mutableStateOf<HistoryMeasurementEdit?>(null) }
    var sleepEdit by remember { mutableStateOf<HistorySleepEdit?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                eyebrow = "Дневник",
                title = "История",
                subtitle = "Кормления, сон, витамины и измерения по дням",
            )
        }
        item { WeekReport(week) }
        if (days.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Text("Записи появятся здесь после заполнения первого дня.", Modifier.padding(20.dp))
                }
            }
        } else {
            items(days, key = { it.date }) { day ->
                HistoryDay(
                    day = day,
                    onFoodEdit = { foodEdit = HistoryFoodEdit(LocalDate.parse(day.date), it) },
                    onVitaminEdit = { vitaminEdit = HistoryVitaminEdit(LocalDate.parse(day.date), it) },
                    onMeasurementEdit = { measurementEdit = HistoryMeasurementEdit(LocalDate.parse(day.date), it) },
                    onSleepEdit = { sleepEdit = HistorySleepEdit(LocalDate.parse(day.date), it) },
                )
            }
        }
    }

    foodEdit?.let { edit ->
        FoodEditorDialog(
            title = "Изменить запись",
            initialDate = edit.date,
            initial = edit.entry,
            days = viewModel.data.days,
            onDismiss = { foodEdit = null },
            onSave = { targetDate, name, amount, unit, time ->
                viewModel.updateFood(edit.date, targetDate, edit.entry, name, amount, unit, time)
                foodEdit = null
            },
        )
    }
    vitaminEdit?.let { edit ->
        VitaminEditorDialog(
            title = "Изменить витамин",
            initialDate = edit.date,
            initial = edit.entry,
            onDismiss = { vitaminEdit = null },
            onSave = { targetDate, name, amount, unit, time ->
                viewModel.updateVitamin(edit.date, targetDate, edit.entry, name, amount, unit, time)
                vitaminEdit = null
            },
        )
    }
    measurementEdit?.let { edit ->
        MeasurementEditorDialog(
            initialDate = edit.date,
            initial = edit.measurement,
            onDismiss = { measurementEdit = null },
            onSave = { targetDate, height, weight, time ->
                viewModel.updateMeasurement(edit.date, targetDate, height, weight, time)
                measurementEdit = null
            },
        )
    }
    sleepEdit?.let { edit ->
        SleepEditorDialog(
            title = "Изменить сон",
            initialStartDate = edit.date,
            initial = edit.entry,
            requireEnd = edit.entry.endTime != null,
            onDismiss = { sleepEdit = null },
            onSave = { startDate, startTime, endDate, endTime ->
                viewModel.updateSleep(edit.date, startDate, edit.entry, startTime, endDate, endTime)
                sleepEdit = null
            },
        )
    }
}

@Composable
private fun WeekReport(days: List<DayRecord>) {
    val foodCount = days.sumOf { it.food.size }
    val vitaminCount = days.sumOf { it.vitamins.size }
    val measured = days.count { it.measurement != null }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Последние 7 дней", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ReportValue("Заполнено", "${days.size} дн.")
                ReportValue("Еда", foodCount.toString())
                ReportValue("Витамины", vitaminCount.toString())
                ReportValue("Измерения", measured.toString())
            }
        }
    }
}

@Composable
private fun ReportValue(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun HistoryDay(
    day: DayRecord,
    onFoodEdit: (FoodEntry) -> Unit,
    onVitaminEdit: (VitaminEntry) -> Unit,
    onMeasurementEdit: (Measurement) -> Unit,
    onSleepEdit: (SleepEntry) -> Unit,
) {
    val date = runCatching { LocalDate.parse(day.date) }.getOrNull()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                date?.format(DateTimeFormatter.ofPattern("d MMMM, EEEE", Locale.forLanguageTag("ru"))) ?: day.date,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (day.food.isEmpty() && day.vitamins.isEmpty() && day.sleeps.isEmpty() && day.measurement == null && day.note.isBlank()) {
                Text("Нет записей", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            day.sleeps.sortedBy { it.startTime }.forEach { entry ->
                val duration = date?.let { sleepDurationMinutes(it, entry) }
                EditableHistoryRow(
                    title = if (entry.endTime == null) "Сон идёт" else duration?.let { "Сон · ${formatSleepDuration(it)}" } ?: "Сон",
                    subtitle = if (entry.endTime == null) "с ${entry.startTime}" else "${entry.startTime}–${entry.endTime}",
                    onEdit = { onSleepEdit(entry) },
                )
            }
            day.food.sortedBy { it.time }.forEach { entry ->
                EditableHistoryRow(
                    title = entry.name,
                    subtitle = "${formatNumber(entry.amount)} ${entry.unit} · ${entry.time}",
                    onEdit = { onFoodEdit(entry) },
                )
            }
            day.vitamins.sortedBy { it.time }.forEach { entry ->
                EditableHistoryRow(
                    title = entry.name,
                    subtitle = listOf(entry.displayDose(), entry.time).filter { it.isNotBlank() }.joinToString(" · "),
                    onEdit = { onVitaminEdit(entry) },
                )
            }
            day.measurement?.let { measurement ->
                EditableHistoryRow(
                    title = "Рост и вес",
                    subtitle = listOfNotNull(
                        measurement.heightCm?.let { "${formatNumber(it)} см" },
                        measurement.weightKg?.let { "${formatNumber(it)} кг" },
                        measurement.time.takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    onEdit = { onMeasurementEdit(measurement) },
                )
            }
            if (day.note.isNotBlank()) {
                HorizontalDivider()
                Text(day.note, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EditableHistoryRow(title: String, subtitle: String, onEdit: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onEdit) { Text("Изменить") }
    }
}

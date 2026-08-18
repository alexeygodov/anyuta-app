package ru.family.rasti.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.family.rasti.RastiViewModel
import ru.family.rasti.data.AppData
import ru.family.rasti.data.DayRecord
import ru.family.rasti.data.FoodEntry
import ru.family.rasti.data.Measurement
import ru.family.rasti.data.VitaminEntry
import ru.family.rasti.feeding.FeedingGuide
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class FoodEditorState(
    val originalDate: LocalDate,
    val entry: FoodEntry? = null,
    val fixedName: String? = null,
)

private data class VitaminEditorState(
    val originalDate: LocalDate,
    val entry: VitaminEntry? = null,
    val fixedName: String? = null,
)

private data class MeasurementEditorState(
    val originalDate: LocalDate,
    val measurement: Measurement? = null,
)

@Composable
fun TodayScreen(viewModel: RastiViewModel, modifier: Modifier = Modifier) {
    var selectedDateRaw by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val selectedDate = LocalDate.parse(selectedDateRaw)
    val day = viewModel.day(selectedDate)
    val vitaminD = day.vitamins.firstOrNull(::isVitaminD)
    var foodEditor by remember { mutableStateOf<FoodEditorState?>(null) }
    var vitaminEditor by remember { mutableStateOf<VitaminEditorState?>(null) }
    var measurementEditor by remember { mutableStateOf<MeasurementEditorState?>(null) }
    var note by remember(day.date, day.note) { mutableStateOf(day.note) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Анюта", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(viewModel.data.profile.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            DateNavigator(
                date = selectedDate,
                onPrevious = { selectedDateRaw = selectedDate.minusDays(1).toString() },
                onNext = { selectedDateRaw = selectedDate.plusDays(1).toString() },
            )
        }
        item {
            QuickActions(
                vitaminDTaken = vitaminD != null,
                onFormula = { foodEditor = FoodEditorState(selectedDate, fixedName = "Смесь") },
                onMilk = { foodEditor = FoodEditorState(selectedDate, fixedName = "Молоко") },
                onVitaminD = { vitaminEditor = VitaminEditorState(selectedDate, vitaminD, "Витамин D") },
                onMeasurement = { measurementEditor = MeasurementEditorState(selectedDate, day.measurement) },
            )
        }
        item { MilkProgressCard(viewModel.data, selectedDate, day) }
        item { DaySummary(day) }
        item { SectionHeader("Еда и питьё", onAdd = { foodEditor = FoodEditorState(selectedDate) }) }
        if (day.food.isEmpty()) {
            item { EmptyHint("Пока ничего не добавлено") }
        } else {
            items(day.food.sortedByDescending { it.time }, key = { it.id }) { entry ->
                EntryRow(
                    title = entry.name,
                    subtitle = "${formatNumber(entry.amount)} ${entry.unit} · ${entry.time}",
                    onEdit = { foodEditor = FoodEditorState(selectedDate, entry) },
                    onDelete = { viewModel.removeFood(selectedDate, entry.id) },
                )
            }
        }
        item { SectionHeader("Витамины", onAdd = { vitaminEditor = VitaminEditorState(selectedDate) }) }
        if (day.vitamins.isEmpty()) {
            item { EmptyHint("Сегодня ещё не отмечены") }
        } else {
            items(day.vitamins.sortedByDescending { it.time }, key = { it.id }) { entry ->
                EntryRow(
                    title = entry.name,
                    subtitle = listOf(entry.dose, entry.time).filter { it.isNotBlank() }.joinToString(" · "),
                    onEdit = { vitaminEditor = VitaminEditorState(selectedDate, entry) },
                    onDelete = { viewModel.removeVitamin(selectedDate, entry.id) },
                )
            }
        }
        item {
            SectionHeader(
                "Рост и вес",
                onAdd = { measurementEditor = MeasurementEditorState(selectedDate, day.measurement) },
            )
            val measurement = day.measurement
            if (measurement == null) {
                EmptyHint("Измерений за этот день нет")
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 18.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                            MetricValue("Рост", measurement.heightCm?.let { "${formatNumber(it)} см" } ?: "—")
                            MetricValue("Вес", measurement.weightKg?.let { "${formatNumber(it)} кг" } ?: "—")
                            MetricValue("Время", measurement.time.ifBlank { "—" })
                        }
                        IconButton(onClick = { measurementEditor = MeasurementEditorState(selectedDate, measurement) }) {
                            Icon(Icons.Outlined.Edit, "Изменить")
                        }
                    }
                }
            }
        }
        item {
            Text("Заметка", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Самочувствие, аппетит, что-то важное…") },
                minLines = 2,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { viewModel.saveNote(selectedDate, note) }, enabled = note != day.note) {
                Text("Сохранить заметку")
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }

    foodEditor?.let { state ->
        FoodEditorDialog(
            title = when {
                state.entry != null -> "Изменить запись"
                state.fixedName != null -> state.fixedName
                else -> "Добавить еду или питьё"
            },
            initialDate = state.originalDate,
            initial = state.entry,
            fixedName = state.fixedName,
            fixedUnit = state.fixedName?.let { "мл" },
            onDismiss = { foodEditor = null },
            onSave = { targetDate, name, amount, unit, time ->
                val original = state.entry
                if (original == null) {
                    viewModel.addFood(targetDate, name, amount, unit, time)
                } else {
                    viewModel.updateFood(state.originalDate, targetDate, original, name, amount, unit, time)
                }
                foodEditor = null
            },
        )
    }
    vitaminEditor?.let { state ->
        VitaminEditorDialog(
            title = if (state.fixedName == "Витамин D") "Витамин D" else if (state.entry == null) "Отметить витамин" else "Изменить витамин",
            initialDate = state.originalDate,
            initial = state.entry,
            fixedName = state.fixedName,
            onDismiss = { vitaminEditor = null },
            onSave = { targetDate, name, dose, time ->
                val original = state.entry
                if (original == null) {
                    viewModel.addVitamin(targetDate, name, dose, time)
                } else {
                    viewModel.updateVitamin(state.originalDate, targetDate, original, name, dose, time)
                }
                vitaminEditor = null
            },
        )
    }
    measurementEditor?.let { state ->
        MeasurementEditorDialog(
            initialDate = state.originalDate,
            initial = state.measurement,
            onDismiss = { measurementEditor = null },
            onSave = { targetDate, height, weight, time ->
                if (state.measurement == null) {
                    viewModel.saveMeasurement(targetDate, height, weight, time)
                } else {
                    viewModel.updateMeasurement(state.originalDate, targetDate, height, weight, time)
                }
                measurementEditor = null
            },
        )
    }
}

@Composable
private fun QuickActions(
    vitaminDTaken: Boolean,
    onFormula: () -> Unit,
    onMilk: () -> Unit,
    onVitaminD: () -> Unit,
    onMeasurement: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Быстрый ввод", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onFormula, modifier = Modifier.weight(1f)) { Text("Смесь") }
                Button(onClick = onMilk, modifier = Modifier.weight(1f)) { Text("Молоко") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    onClick = onVitaminD,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (vitaminDTaken) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                        contentColor = if (vitaminDTaken) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) { Text(if (vitaminDTaken) "Витамин D ✓" else "Витамин D") }
                OutlinedButton(onClick = onMeasurement, modifier = Modifier.weight(1f)) { Text("Рост / вес") }
            }
        }
    }
}

@Composable
private fun MilkProgressCard(data: AppData, date: LocalDate, day: DayRecord) {
    val milkEntries = day.food
        .filter { it.unit.trim().lowercase() in setOf("мл", "ml") }
        .filter { it.name.trim().lowercase() in setOf("смесь", "молоко") }
    val consumed = milkEntries.sumOf { it.amount }
    val result = FeedingGuide.calculate(data, date)
    val guide = result.guide
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Объём питания", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (guide != null) {
                val progress = (consumed / guide.targetMl).toFloat().coerceIn(0f, 1f)
                Text("${formatNumber(consumed)} из ≈${guide.targetMl} мл")
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            } else {
                Text("Учтено: ${formatNumber(consumed)} мл")
            }
            Text("График за сутки", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            MilkIntakeChart(guide, milkEntries)
            if (milkEntries.isEmpty()) {
                Text(
                    "Нажмите «Смесь» или «Молоко» — здесь появится линия накопленного объёма.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (guide != null) {
                Text(
                    "Цветная полоса — расчётный диапазон за сутки; тёмная линия — накопительно смесь + молоко по времени.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Справочный диапазон: ${guide.minimumMl}–${guide.maximumMl} мл · вес ${formatNumber(guide.weightKg)} кг",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Text(
                    "Тёмная линия — смесь + молоко по времени. Цветной диапазон появится после заполнения даты рождения и веса.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(result.explanation, style = MaterialTheme.typography.bodySmall)
            Text(
                "Суммируются смесь и измеренное сцеженное молоко. Прямое грудное вскармливание в мл не оценивается; ориентируйтесь на сигналы ребёнка и рекомендации врача.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DateNavigator(date: LocalDate, onPrevious: () -> Unit, onNext: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onPrevious) { Icon(Icons.Outlined.ChevronLeft, "Предыдущий день") }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    when (date) {
                        LocalDate.now() -> "Сегодня"
                        LocalDate.now().minusDays(1) -> "Вчера"
                        else -> date.format(DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru")))
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(date.format(DateTimeFormatter.ofPattern("EEEE, yyyy", Locale.forLanguageTag("ru"))))
            }
            IconButton(onClick = onNext) { Icon(Icons.Outlined.ChevronRight, "Следующий день") }
        }
    }
}

@Composable
private fun DaySummary(day: DayRecord) {
    val totals = day.food.groupBy { it.unit }.mapValues { entry -> entry.value.sumOf { it.amount } }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text("Коротко за день", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                if (totals.isEmpty()) "Еда: нет записей"
                else "Еда: " + totals.entries.joinToString(" · ") { "${formatNumber(it.value)} ${it.key}" },
            )
            Text("Витамины: ${day.vitamins.size}")
        }
    }
}

@Composable
private fun SectionHeader(title: String, onAdd: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        FilledTonalButton(onClick = onAdd) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Text("Добавить")
        }
    }
}

@Composable
private fun EntryRow(title: String, subtitle: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, "Изменить") }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.DeleteOutline, "Удалить") }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun MetricValue(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

private fun isVitaminD(entry: VitaminEntry): Boolean {
    val normalized = entry.name.lowercase().replace("ё", "е")
    return normalized.contains("витамин d") || normalized.contains("витамин д") || normalized.contains("d3")
}

internal fun formatNumber(number: Double): String =
    if (number % 1.0 == 0.0) number.toInt().toString() else "%.1f".format(Locale.US, number).replace('.', ',')

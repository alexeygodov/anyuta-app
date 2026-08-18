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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.family.rasti.RastiViewModel
import ru.family.rasti.data.DayRecord
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayScreen(viewModel: RastiViewModel, modifier: Modifier = Modifier) {
    var selectedDateRaw by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val selectedDate = LocalDate.parse(selectedDateRaw)
    val day = viewModel.day(selectedDate)
    var foodDialog by remember { mutableStateOf(false) }
    var vitaminDialog by remember { mutableStateOf(false) }
    var measurementDialog by remember { mutableStateOf(false) }
    var note by remember(day.date, day.note) { mutableStateOf(day.note) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Анюта", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(
                viewModel.data.profile.name,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            DateNavigator(
                date = selectedDate,
                onPrevious = { selectedDateRaw = selectedDate.minusDays(1).toString() },
                onNext = { selectedDateRaw = selectedDate.plusDays(1).toString() },
            )
        }
        item { DaySummary(day) }
        item {
            SectionHeader("Еда и питьё", onAdd = { foodDialog = true })
        }
        if (day.food.isEmpty()) {
            item { EmptyHint("Пока ничего не добавлено") }
        } else {
            items(day.food.sortedByDescending { it.time }, key = { it.id }) { item ->
                EntryRow(
                    title = item.name,
                    subtitle = "${formatNumber(item.amount)} ${item.unit} · ${item.time}",
                    onDelete = { viewModel.removeFood(selectedDate, item.id) },
                )
            }
        }
        item {
            SectionHeader("Витамины", onAdd = { vitaminDialog = true })
        }
        if (day.vitamins.isEmpty()) {
            item { EmptyHint("Сегодня ещё не отмечены") }
        } else {
            items(day.vitamins.sortedByDescending { it.time }, key = { it.id }) { item ->
                EntryRow(
                    title = item.name,
                    subtitle = listOf(item.dose, item.time).filter { it.isNotBlank() }.joinToString(" · "),
                    onDelete = { viewModel.removeVitamin(selectedDate, item.id) },
                )
            }
        }
        item {
            SectionHeader("Рост и вес", onAdd = { measurementDialog = true })
            val measurement = day.measurement
            if (measurement == null) {
                EmptyHint("Измерений за этот день нет")
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        MetricValue("Рост", measurement.heightCm?.let { "${formatNumber(it)} см" } ?: "—")
                        MetricValue("Вес", measurement.weightKg?.let { "${formatNumber(it)} кг" } ?: "—")
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
            OutlinedButton(
                onClick = { viewModel.saveNote(selectedDate, note) },
                enabled = note != day.note,
            ) { Text("Сохранить заметку") }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }

    if (foodDialog) {
        FoodDialog(
            onDismiss = { foodDialog = false },
            onSave = { name, amount, unit, time ->
                viewModel.addFood(selectedDate, name, amount, unit, time)
                foodDialog = false
            },
        )
    }
    if (vitaminDialog) {
        VitaminDialog(
            onDismiss = { vitaminDialog = false },
            onSave = { name, dose, time ->
                viewModel.addVitamin(selectedDate, name, dose, time)
                vitaminDialog = false
            },
        )
    }
    if (measurementDialog) {
        MeasurementDialog(
            day = day,
            onDismiss = { measurementDialog = false },
            onSave = { height, weight ->
                viewModel.saveMeasurement(selectedDate, height, weight)
                measurementDialog = false
            },
        )
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
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
private fun EntryRow(title: String, subtitle: String, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
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
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FoodDialog(onDismiss: () -> Unit, onSave: (String, Double, String, String?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("г") }
    var time by remember { mutableStateOf("") }
    val amountNumber = amount.replace(',', '.').toDoubleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить еду или питьё") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Что съела") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        amount,
                        { amount = it },
                        label = { Text("Количество") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(unit, { unit = it }, label = { Text("г / мл") }, modifier = Modifier.weight(.7f), singleLine = true)
                }
                OutlinedTextField(time, { time = it }, label = { Text("Время, например 09:30") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name.trim(), amountNumber ?: 0.0, unit.trim(), time) },
                enabled = name.isNotBlank() && amountNumber != null && amountNumber > 0,
            ) { Text("Добавить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun VitaminDialog(onDismiss: () -> Unit, onSave: (String, String, String?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Отметить витамин") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Название") }, singleLine = true)
                OutlinedTextField(dose, { dose = it }, label = { Text("Доза, например 1 капля") }, singleLine = true)
                OutlinedTextField(time, { time = it }, label = { Text("Время, например 09:30") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name.trim(), dose.trim(), time) }, enabled = name.isNotBlank()) {
                Text("Отметить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
private fun MeasurementDialog(day: DayRecord, onDismiss: () -> Unit, onSave: (Double?, Double?) -> Unit) {
    var height by remember { mutableStateOf(day.measurement?.heightCm?.let(::formatNumber).orEmpty()) }
    var weight by remember { mutableStateOf(day.measurement?.weightKg?.let(::formatNumber).orEmpty()) }
    val heightNumber = height.replace(',', '.').toDoubleOrNull()
    val weightNumber = weight.replace(',', '.').toDoubleOrNull()
    val valid = (height.isBlank() || heightNumber != null) && (weight.isBlank() || weightNumber != null)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Рост и вес") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    height,
                    { height = it },
                    label = { Text("Рост, см") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                OutlinedTextField(
                    weight,
                    { weight = it },
                    label = { Text("Вес, кг") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(heightNumber, weightNumber) },
                enabled = valid,
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

internal fun formatNumber(number: Double): String =
    if (number % 1.0 == 0.0) number.toInt().toString() else "%.1f".format(Locale.US, number).replace('.', ',')

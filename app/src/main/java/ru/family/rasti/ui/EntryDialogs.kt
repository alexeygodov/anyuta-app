package ru.family.rasti.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.family.rasti.data.FoodEntry
import ru.family.rasti.data.Measurement
import ru.family.rasti.data.VitaminEntry
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val displayDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
internal fun FoodEditorDialog(
    title: String,
    initialDate: LocalDate,
    initial: FoodEntry? = null,
    fixedName: String? = null,
    fixedUnit: String? = null,
    onDismiss: () -> Unit,
    onSave: (LocalDate, String, Double, String, String) -> Unit,
) {
    val stateKey = initial?.id ?: "$fixedName-$initialDate"
    var name by rememberSaveable(stateKey) { mutableStateOf(initial?.name ?: fixedName.orEmpty()) }
    var amount by rememberSaveable(stateKey) { mutableStateOf(initial?.amount?.let(::formatNumber).orEmpty()) }
    var unit by rememberSaveable(stateKey) { mutableStateOf(initial?.unit ?: fixedUnit ?: "г") }
    var dateRaw by rememberSaveable(stateKey) { mutableStateOf(initialDate.toString()) }
    var time by rememberSaveable(stateKey) { mutableStateOf(initial?.time?.ifBlank { currentTime() } ?: currentTime()) }
    val date = LocalDate.parse(dateRaw)
    val amountNumber = amount.replace(',', '.').toDoubleOrNull()
    val normalizedTime = normalizeTimeInput(time)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (fixedName == null) {
                    OutlinedTextField(name, { name = it }, label = { Text("Название") }, singleLine = true)
                } else {
                    Text(fixedName)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        amount,
                        { amount = it },
                        label = { Text("Количество") },
                        suffix = { Text(fixedUnit ?: unit) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    if (fixedUnit == null) {
                        OutlinedTextField(
                            unit,
                            { unit = it },
                            label = { Text("Единица") },
                            modifier = Modifier.weight(.72f),
                            singleLine = true,
                        )
                    }
                }
                DateTimePickerRow(
                    date = date,
                    time = time,
                    onDateChange = { dateRaw = it.toString() },
                    onTimeChange = { time = it },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(date, name.trim(), amountNumber ?: 0.0, fixedUnit ?: unit.trim(), normalizedTime ?: time) },
                enabled = name.isNotBlank() && amountNumber != null && amountNumber > 0 &&
                    (fixedUnit != null || unit.isNotBlank()) && normalizedTime != null,
            ) { Text(if (initial == null) "Добавить" else "Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
internal fun VitaminEditorDialog(
    title: String,
    initialDate: LocalDate,
    initial: VitaminEntry? = null,
    fixedName: String? = null,
    onDismiss: () -> Unit,
    onSave: (LocalDate, String, String, String) -> Unit,
) {
    val stateKey = initial?.id ?: "$fixedName-$initialDate"
    var name by rememberSaveable(stateKey) { mutableStateOf(initial?.name ?: fixedName.orEmpty()) }
    var dose by rememberSaveable(stateKey) {
        mutableStateOf(initial?.dose ?: if (fixedName == "Витамин D") "2 капли" else "")
    }
    var dateRaw by rememberSaveable(stateKey) { mutableStateOf(initialDate.toString()) }
    var time by rememberSaveable(stateKey) { mutableStateOf(initial?.time?.ifBlank { currentTime() } ?: currentTime()) }
    val date = LocalDate.parse(dateRaw)
    val normalizedTime = normalizeTimeInput(time)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (fixedName == null) {
                    OutlinedTextField(name, { name = it }, label = { Text("Название") }, singleLine = true)
                } else {
                    Text(fixedName)
                }
                OutlinedTextField(
                    dose,
                    { dose = it },
                    label = { Text("Доза, например 1 капля") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                DateTimePickerRow(
                    date = date,
                    time = time,
                    onDateChange = { dateRaw = it.toString() },
                    onTimeChange = { time = it },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(date, name.trim(), dose.trim(), normalizedTime ?: time) },
                enabled = name.isNotBlank() && normalizedTime != null,
            ) { Text(if (initial == null) "Отметить" else "Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
internal fun MeasurementEditorDialog(
    initialDate: LocalDate,
    initial: Measurement? = null,
    onDismiss: () -> Unit,
    onSave: (LocalDate, Double?, Double?, String) -> Unit,
) {
    val stateKey = "${initial?.updatedAt}-$initialDate"
    var height by rememberSaveable(stateKey) { mutableStateOf(initial?.heightCm?.let(::formatNumber).orEmpty()) }
    var weight by rememberSaveable(stateKey) { mutableStateOf(initial?.weightKg?.let(::formatNumber).orEmpty()) }
    var dateRaw by rememberSaveable(stateKey) { mutableStateOf(initialDate.toString()) }
    var time by rememberSaveable(stateKey) { mutableStateOf(initial?.time?.ifBlank { currentTime() } ?: currentTime()) }
    val date = LocalDate.parse(dateRaw)
    val heightNumber = height.replace(',', '.').toDoubleOrNull()
    val weightNumber = weight.replace(',', '.').toDoubleOrNull()
    val validHeight = height.isBlank() || (heightNumber != null && heightNumber > 0)
    val validWeight = weight.isBlank() || (weightNumber != null && weightNumber > 0)
    val normalizedTime = normalizeTimeInput(time)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Рост и вес") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    height,
                    { height = it },
                    label = { Text("Рост, см") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    weight,
                    { weight = it },
                    label = { Text("Вес, кг") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                DateTimePickerRow(
                    date = date,
                    time = time,
                    onDateChange = { dateRaw = it.toString() },
                    onTimeChange = { time = it },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(date, heightNumber, weightNumber, normalizedTime ?: time) },
                enabled = validHeight && validWeight && (height.isNotBlank() || weight.isNotBlank()) && normalizedTime != null,
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
internal fun DateTimePickerRow(
    date: LocalDate,
    time: String,
    onDateChange: (LocalDate) -> Unit,
    onTimeChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        DatePickerButton(date, onDateChange, Modifier.fillMaxWidth())
        TimeInput(time, onTimeChange)
    }
}

@Composable
internal fun DatePickerButton(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    maximumDate: LocalDate? = null,
) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            DatePickerDialog(
                context,
                { _, year, month, day -> onDateChange(LocalDate.of(year, month + 1, day)) },
                date.year,
                date.monthValue - 1,
                date.dayOfMonth,
            ).apply {
                maximumDate?.let {
                    datePicker.maxDate = it.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }
            }.show()
        },
        modifier = modifier,
    ) {
        Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
        Text("  ${date.format(displayDateFormatter)}")
    }
}

@Composable
private fun TimeInput(time: String, onTimeChange: (String) -> Unit) {
    val parts = time.split(":", limit = 2)
    val hours = parts.getOrElse(0) { "" }
    val minutes = parts.getOrElse(1) { "" }
    val valid = normalizeTimeInput(time) != null
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Icon(Icons.Outlined.Schedule, contentDescription = null)
            Text("  Время", style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
        }
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(
                value = hours,
                onValueChange = { value -> onTimeChange("${value.filter(Char::isDigit).take(2)}:$minutes") },
                label = { Text("Часы") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = !valid,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center),
                modifier = Modifier.weight(1f),
            )
            Text(" : ")
            OutlinedTextField(
                value = minutes,
                onValueChange = { value -> onTimeChange("$hours:${value.filter(Char::isDigit).take(2)}") },
                label = { Text("Минуты") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = !valid,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { onTimeChange(adjustTimeInput(time, -15)) }) { Text("−15 мин") }
            FilledTonalButton(onClick = { onTimeChange(currentTime()) }) { Text("Сейчас") }
            TextButton(onClick = { onTimeChange(adjustTimeInput(time, 15)) }) { Text("+15 мин") }
        }
        if (!valid) {
            Text("Введите часы 0–23 и минуты 0–59", color = androidx.compose.material3.MaterialTheme.colorScheme.error)
        }
    }
}

private fun currentTime(): String = LocalTime.now().format(timeFormatter)

internal fun normalizeTimeInput(value: String): String? {
    val parts = value.split(":", limit = 2)
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return LocalTime.of(hour, minute).format(timeFormatter)
}

internal fun adjustTimeInput(value: String, minutes: Long): String {
    val normalized = normalizeTimeInput(value)
    val base = normalized?.let { LocalTime.parse(it, timeFormatter) } ?: LocalTime.now()
    return base.plusMinutes(minutes).format(timeFormatter)
}

package ru.family.rasti.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
                onClick = { onSave(date, name.trim(), amountNumber ?: 0.0, fixedUnit ?: unit.trim(), time) },
                enabled = name.isNotBlank() && amountNumber != null && amountNumber > 0 && (fixedUnit != null || unit.isNotBlank()),
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
    var dose by rememberSaveable(stateKey) { mutableStateOf(initial?.dose.orEmpty()) }
    var dateRaw by rememberSaveable(stateKey) { mutableStateOf(initialDate.toString()) }
    var time by rememberSaveable(stateKey) { mutableStateOf(initial?.time?.ifBlank { currentTime() } ?: currentTime()) }
    val date = LocalDate.parse(dateRaw)

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
                onClick = { onSave(date, name.trim(), dose.trim(), time) },
                enabled = name.isNotBlank(),
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
                onClick = { onSave(date, heightNumber, weightNumber, time) },
                enabled = validHeight && validWeight && (height.isNotBlank() || weight.isNotBlank()),
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
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        DatePickerButton(date, onDateChange, Modifier.weight(1.25f))
        TimePickerButton(time, onTimeChange, Modifier.weight(.85f))
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
private fun TimePickerButton(time: String, onTimeChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val parsed = runCatching { LocalTime.parse(time, timeFormatter) }.getOrDefault(LocalTime.now())
    OutlinedButton(
        onClick = {
            TimePickerDialog(
                context,
                { _, hour, minute -> onTimeChange(LocalTime.of(hour, minute).format(timeFormatter)) },
                parsed.hour,
                parsed.minute,
                true,
            ).show()
        },
        modifier = modifier,
    ) {
        Icon(Icons.Outlined.Schedule, contentDescription = null)
        Text("  $time")
    }
}

private fun currentTime(): String = LocalTime.now().format(timeFormatter)

package ru.family.rasti.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import ru.family.rasti.data.DayRecord
import ru.family.rasti.data.FoodEntry
import ru.family.rasti.data.Measurement
import ru.family.rasti.data.SleepEntry
import ru.family.rasti.data.VitaminEntry
import ru.family.rasti.sleep.formatSleepDuration
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val displayDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@Composable
internal fun FoodEditorDialog(
    title: String,
    initialDate: LocalDate,
    initial: FoodEntry? = null,
    fixedName: String? = null,
    fixedUnit: String? = null,
    initialAmountMl: Double? = null,
    days: Map<String, DayRecord> = emptyMap(),
    onDismiss: () -> Unit,
    onSave: (LocalDate, String, Double, String, String) -> Unit,
) {
    val stateKey = initial?.id ?: "$fixedName-$initialDate-$initialAmountMl"
    var name by rememberSaveable(stateKey) { mutableStateOf(initial?.name ?: fixedName.orEmpty()) }
    var amount by rememberSaveable(stateKey) { mutableStateOf(initial?.amount?.let(::formatNumber).orEmpty()) }
    val feedingName = (fixedName ?: initial?.name).orEmpty().trim().lowercase()
    val feedingUnit = (fixedUnit ?: initial?.unit).orEmpty().trim().lowercase()
    val usesMilkSlider = feedingUnit in setOf("мл", "ml") && feedingName in setOf("молоко", "смесь")
    var milkAmount by rememberSaveable(stateKey) {
        mutableStateOf(normalizeMilkAmount((initial?.amount ?: initialAmountMl ?: 100.0).toFloat()))
    }
    var unit by rememberSaveable(stateKey) { mutableStateOf(initial?.unit ?: fixedUnit ?: "г") }
    var time by rememberSaveable(stateKey) { mutableStateOf(initial?.time?.ifBlank { currentTime() } ?: currentTime()) }
    val amountNumber = if (usesMilkSlider) milkAmount.toDouble() else amount.replace(',', '.').toDoubleOrNull()
    val normalizedTime = normalizeTimeToFiveMinutes(time)
    val milkSuggestions = remember(days, usesMilkSlider, feedingName) {
        if (usesMilkSlider) popularMilkAmounts(days.values, feedingName) else emptyList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (fixedName == null) {
                    OutlinedTextField(name, { name = it }, label = { Text("Название") }, singleLine = true)
                }
                if (usesMilkSlider) {
                    BottleAmountPicker(
                        amountMl = milkAmount,
                        onAmountChange = { milkAmount = normalizeMilkAmount(it) },
                        suggestions = milkSuggestions,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            amount,
                            { amount = it },
                            label = { Text("Количество") },
                            suffix = { Text(unit) },
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
                }
                TimeInput(time, onTimeChange = { time = it })
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(initialDate, name.trim(), amountNumber ?: 0.0, fixedUnit ?: unit.trim(), normalizedTime ?: time) },
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
    onSave: (LocalDate, String, Double, String, String) -> Unit,
) {
    val stateKey = initial?.id ?: "$fixedName-$initialDate"
    var name by rememberSaveable(stateKey) { mutableStateOf(initial?.name ?: fixedName.orEmpty()) }
    var amount by rememberSaveable(stateKey) {
        mutableStateOf(initial?.amount?.let(::formatNumber) ?: if (fixedName == "Витамин D") "2" else "")
    }
    var unit by rememberSaveable(stateKey) { mutableStateOf(initial?.unit ?: if (fixedName == "Витамин D") "капля" else "ед.") }
    var time by rememberSaveable(stateKey) { mutableStateOf(initial?.time?.ifBlank { currentTime() } ?: currentTime()) }
    val amountNumber = amount.replace(',', '.').toDoubleOrNull()
    val normalizedTime = normalizeTimeToFiveMinutes(time)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (fixedName == null) {
                    OutlinedTextField(name, { name = it }, label = { Text("Название") }, singleLine = true)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        amount,
                        { value -> amount = value.filter { it.isDigit() || it == ',' || it == '.' } },
                        label = { Text("Количество") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        unit,
                        { unit = it },
                        label = { Text("Единица") },
                        supportingText = { if (fixedName == "Витамин D") Text("например, капля") },
                        modifier = Modifier.weight(1.15f),
                        singleLine = true,
                    )
                }
                TimeInput(time, onTimeChange = { time = it })
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(initialDate, name.trim(), amountNumber ?: 0.0, unit.trim(), normalizedTime ?: time) },
                enabled = name.isNotBlank() && amountNumber != null && amountNumber > 0.0 && unit.isNotBlank() && normalizedTime != null,
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
    var time by rememberSaveable(stateKey) { mutableStateOf(initial?.time?.ifBlank { currentTime() } ?: currentTime()) }
    val heightNumber = height.replace(',', '.').toDoubleOrNull()
    val weightNumber = weight.replace(',', '.').toDoubleOrNull()
    val validHeight = height.isBlank() || (heightNumber != null && heightNumber > 0)
    val validWeight = weight.isBlank() || (weightNumber != null && weightNumber > 0)
    val normalizedTime = normalizeTimeToFiveMinutes(time)

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
                TimeInput(time, onTimeChange = { time = it })
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(initialDate, heightNumber, weightNumber, normalizedTime ?: time) },
                enabled = validHeight && validWeight && (height.isNotBlank() || weight.isNotBlank()) && normalizedTime != null,
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

@Composable
internal fun SleepEditorDialog(
    title: String,
    initialStartDate: LocalDate,
    initial: SleepEntry? = null,
    requireEnd: Boolean,
    onDismiss: () -> Unit,
    onSave: (LocalDate, String, LocalDate?, String?) -> Unit,
) {
    val now = remember { LocalDateTime.now().withSecond(0).withNano(0) }
    val stateKey = "${initial?.id}-$initialStartDate-$requireEnd"
    var startDate by rememberSaveable(stateKey) { mutableStateOf(initialStartDate) }
    var startTime by rememberSaveable(stateKey) { mutableStateOf(initial?.startTime ?: currentTime()) }
    var endDate by rememberSaveable(stateKey) {
        mutableStateOf(initial?.endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: now.toLocalDate())
    }
    var endTime by rememberSaveable(stateKey) { mutableStateOf(initial?.endTime ?: currentTime()) }
    var editingEnd by rememberSaveable(stateKey) { mutableStateOf(requireEnd) }
    val normalizedStart = normalizeTimeToFiveMinutes(startTime)
    val normalizedEnd = if (requireEnd) normalizeTimeToFiveMinutes(endTime) else null
    val startDateTime = normalizedStart?.let { startDate.atTime(LocalTime.parse(it, timeFormatter)) }
    val endDateTime = normalizedEnd?.let { endDate.atTime(LocalTime.parse(it, timeFormatter)) }
    val durationMinutes = if (startDateTime != null && endDateTime != null) {
        Duration.between(startDateTime, endDateTime).toMinutes().takeIf { it >= 0 }
    } else {
        null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (requireEnd) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (editingEnd) {
                            OutlinedButton(onClick = { editingEnd = false }, modifier = Modifier.weight(1f)) { Text("Начало") }
                            Button(onClick = {}, modifier = Modifier.weight(1f)) { Text("Конец") }
                        } else {
                            Button(onClick = {}, modifier = Modifier.weight(1f)) { Text("Начало") }
                            OutlinedButton(onClick = { editingEnd = true }, modifier = Modifier.weight(1f)) { Text("Конец") }
                        }
                    }
                }
                Text(if (editingEnd) "Время пробуждения" else "Время засыпания", fontWeight = FontWeight.SemiBold)
                DatePickerButton(
                    date = if (editingEnd) endDate else startDate,
                    onDateChange = { if (editingEnd) endDate = it else startDate = it },
                    modifier = Modifier.fillMaxWidth(),
                    maximumDate = LocalDate.now(),
                )
                TimeInput(
                    time = if (editingEnd) endTime else startTime,
                    onTimeChange = { if (editingEnd) endTime = it else startTime = it },
                )
                if (requireEnd) {
                    Text(
                        durationMinutes?.let { "Длительность сна: ${formatSleepDuration(it)}" }
                            ?: "Время пробуждения должно быть позже засыпания",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (durationMinutes == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(startDate, normalizedStart!!, endDate.takeIf { requireEnd }, normalizedEnd) },
                enabled = normalizedStart != null && (!requireEnd || durationMinutes != null),
            ) { Text(if (requireEnd) "Сохранить" else "Уснула") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
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

private val wheelItemHeight = 44.dp

@Composable
private fun TimeInput(time: String, onTimeChange: (String) -> Unit) {
    val selected = normalizeTimeToFiveMinutes(time)?.let { LocalTime.parse(it, timeFormatter) }
        ?: roundedCurrentTime()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NumberWheel(
                values = (0..23).toList(),
                selectedValue = selected.hour,
                label = "Часы",
                onValueSelected = { hour ->
                    onTimeChange(LocalTime.of(hour, selected.minute).format(timeFormatter))
                },
            )
            Box(Modifier.height(wheelItemHeight * 3), contentAlignment = Alignment.Center) {
                Text(":", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            NumberWheel(
                values = (0..55 step 5).toList(),
                selectedValue = selected.minute,
                label = "Минуты",
                onValueSelected = { minute ->
                    onTimeChange(LocalTime.of(selected.hour, minute).format(timeFormatter))
                },
            )
        }
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            FilledTonalButton(onClick = { onTimeChange(currentTime()) }) { Text("Сейчас") }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NumberWheel(
    values: List<Int>,
    selectedValue: Int,
    label: String,
    onValueSelected: (Int) -> Unit,
) {
    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    var positioned by remember { mutableStateOf(false) }
    val centeredIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) {
                -1
            } else {
                val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
                info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index ?: -1
            }
        }
    }

    LaunchedEffect(Unit) {
        listState.scrollToItem(values.indexOf(selectedValue).coerceAtLeast(0))
        positioned = true
    }
    LaunchedEffect(listState.isScrollInProgress, centeredIndex) {
        if (positioned && !listState.isScrollInProgress && centeredIndex >= 0 && values[centeredIndex] != selectedValue) {
            onValueSelected(values[centeredIndex])
        }
    }
    LaunchedEffect(selectedValue) {
        val index = values.indexOf(selectedValue)
        if (!listState.isScrollInProgress && index >= 0 && centeredIndex >= 0 && centeredIndex != index) {
            listState.animateScrollToItem(index)
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.width(64.dp).height(wheelItemHeight * 3),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(wheelItemHeight)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
            )
            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                contentPadding = PaddingValues(vertical = wheelItemHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(values.size) { index ->
                    val isSelected = index == centeredIndex
                    Box(Modifier.fillMaxWidth().height(wheelItemHeight), contentAlignment = Alignment.Center) {
                        Text(
                            text = "%02d".format(Locale.US, values[index]),
                            style = if (isSelected) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun roundedCurrentTime(): LocalTime {
    val now = LocalTime.now()
    return now.withMinute(now.minute / 5 * 5).withSecond(0).withNano(0)
}

private fun currentTime(): String = roundedCurrentTime().format(timeFormatter)

internal fun normalizeTimeInput(value: String): String? {
    val parts = value.split(":", limit = 2)
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return LocalTime.of(hour, minute).format(timeFormatter)
}

internal fun normalizeTimeToFiveMinutes(value: String): String? {
    val normalized = normalizeTimeInput(value) ?: return null
    val parsed = LocalTime.parse(normalized, timeFormatter)
    return parsed.withMinute(parsed.minute / 5 * 5).format(timeFormatter)
}

internal fun adjustTimeInput(value: String, minutes: Long): String {
    val normalized = normalizeTimeInput(value)
    val base = normalized?.let { LocalTime.parse(it, timeFormatter) } ?: LocalTime.now()
    return base.plusMinutes(minutes).format(timeFormatter)
}

internal fun normalizeMilkAmount(value: Float): Float =
    ((value / 5f).roundToInt() * 5f).coerceIn(0f, 200f)

internal fun popularMilkAmounts(days: Collection<DayRecord>, feedingName: String, limit: Int = 4): List<Int> {
    val normalizedName = feedingName.trim().lowercase()
    if (normalizedName.isEmpty()) return emptyList()
    return days.asSequence()
        .flatMap { it.food.asSequence() }
        .filter {
            it.name.trim().lowercase() == normalizedName &&
                it.unit.trim().lowercase() in setOf("мл", "ml") &&
                it.amount > 0
        }
        .groupBy { it.amount.roundToInt() }
        .mapValues { (_, entries) -> entries.size to entries.maxOf { entry -> entry.updatedAt } }
        .entries
        .sortedWith(
            compareByDescending<Map.Entry<Int, Pair<Int, String>>> { it.value.first }
                .thenByDescending { it.value.second },
        )
        .take(limit)
        .map { it.key }
        .sorted()
}

package ru.family.rasti.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import ru.family.rasti.RastiViewModel
import ru.family.rasti.data.AppData
import ru.family.rasti.data.DayRecord
import ru.family.rasti.data.FoodEntry
import ru.family.rasti.data.Measurement
import ru.family.rasti.data.VitaminEntry
import ru.family.rasti.data.displayDose
import ru.family.rasti.feeding.FeedingGuide
import java.time.LocalDate
import java.time.LocalTime
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
    var lastSeenTodayRaw by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        fun followNewDay() {
            val today = LocalDate.now()
            val previousToday = LocalDate.parse(lastSeenTodayRaw)
            if (today == previousToday) return
            if (LocalDate.parse(selectedDateRaw) == previousToday) {
                selectedDateRaw = today.toString()
            }
            lastSeenTodayRaw = today.toString()
        }
        followNewDay()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) followNewDay()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
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
            DateNavigator(
                date = selectedDate,
                onPrevious = { selectedDateRaw = selectedDate.minusDays(1).toString() },
                onNext = { selectedDateRaw = selectedDate.plusDays(1).toString() },
            )
        }
        if (vitaminD == null) {
            item {
                VitaminDReminder(
                    shouldPulse = selectedDate == LocalDate.now(),
                    onClick = { vitaminEditor = VitaminEditorState(selectedDate, null, "Витамин D") },
                )
            }
        }
        item {
            MilkProgressCard(
                data = viewModel.data,
                date = selectedDate,
                day = day,
                onFormula = { foodEditor = FoodEditorState(selectedDate, fixedName = "Смесь") },
                onMilk = { foodEditor = FoodEditorState(selectedDate, fixedName = "Молоко") },
                onEditFood = { foodEditor = FoodEditorState(selectedDate, it) },
                onMeasurement = { measurementEditor = MeasurementEditorState(selectedDate, day.measurement) },
            )
        }
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
                    subtitle = listOf(entry.displayDose(), entry.time).filter { it.isNotBlank() }.joinToString(" · "),
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
            days = viewModel.data.days,
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
            onSave = { targetDate, name, amount, unit, time ->
                val original = state.entry
                if (original == null) {
                    viewModel.addVitamin(targetDate, name, amount, unit, time)
                } else {
                    viewModel.updateVitamin(state.originalDate, targetDate, original, name, amount, unit, time)
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
private fun VitaminDReminder(
    shouldPulse: Boolean,
    onClick: () -> Unit,
) {
    val pulseTransition = rememberInfiniteTransition(label = "vitamin-d-reminder")
    val pulse by pulseTransition.animateFloat(
        initialValue = if (shouldPulse) 0f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(480), repeatMode = RepeatMode.Reverse),
        label = "vitamin-d-attention",
    )
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .graphicsLayer {
                alpha = if (shouldPulse) .52f + pulse * .48f else 1f
                scaleX = if (shouldPulse) .97f + pulse * .05f else 1f
                scaleY = if (shouldPulse) .97f + pulse * .05f else 1f
            },
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = lerp(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.errorContainer, pulse * .38f),
            contentColor = MaterialTheme.colorScheme.onError,
        ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "ВИТАМИН D НЕ ПРИНЯТ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text("НАЖМИТЕ СЕЙЧАС · 2 капли", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun MilkProgressCard(
    data: AppData,
    date: LocalDate,
    day: DayRecord,
    onFormula: () -> Unit,
    onMilk: () -> Unit,
    onEditFood: (FoodEntry) -> Unit,
    onMeasurement: () -> Unit,
) {
    val milkEntries = day.food
        .filter { it.unit.trim().lowercase() in setOf("мл", "ml") }
        .filter { it.name.trim().lowercase() in setOf("смесь", "молоко") }
    val consumed = milkEntries.sumOf { it.amount }
    val result = FeedingGuide.calculate(data, date)
    val guide = result.guide
    val startColor = MaterialTheme.colorScheme.errorContainer
    val targetColor = MaterialTheme.colorScheme.primaryContainer
    val fallbackColor = MaterialTheme.colorScheme.secondaryContainer
    val progressToMinimum = guide?.let { (consumed / it.minimumMl).toFloat().coerceIn(0f, 1f) }
    val desiredColor = progressToMinimum?.let { lerp(startColor, targetColor, it) } ?: fallbackColor
    val cardColor by animateColorAsState(desiredColor, label = "feeding-progress-background")
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            var minuteTick by remember { mutableStateOf(0) }
            LaunchedEffect(Unit) {
                while (true) {
                    delay(60_000)
                    minuteTick++
                }
            }
            val lastFeeding = remember(minuteTick, date, day) { lastFeedingInfo(date, milkEntries) }
            lastFeeding?.let { LastFeedingLabel(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onMilk, modifier = Modifier.weight(1f).height(54.dp)) { Text("Молоко") }
                Button(onClick = onFormula, modifier = Modifier.weight(1f).height(54.dp)) { Text("Смесь") }
            }
            Text("Питание за сутки", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            MilkIntakeChart(
                entries = milkEntries,
                date = date,
                minimumMl = guide?.minimumMl,
                targetMl = guide?.targetMl,
                maximumMl = guide?.maximumMl,
                onEntryClick = onEditFood,
            )
            if (milkEntries.isEmpty()) {
                Text(
                    "Нажмите «Смесь» или «Молоко» — здесь появится накопительный график.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            var detailsExpanded by rememberSaveable { mutableStateOf(false) }
            TextButton(onClick = { detailsExpanded = !detailsExpanded }, modifier = Modifier.fillMaxWidth()) {
                Text(if (detailsExpanded) "Скрыть подробности" else "Подробности и ориентиры")
                Icon(
                    if (detailsExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = null,
                )
            }
            AnimatedVisibility(visible = detailsExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (guide != null) {
                        Text(
                            "График показывает накопленный объём за сутки. Полупрозрачная полоса — норма по времени, вертикальная зона справа — суточный диапазон, круг внутри неё — цель. Круглые маркеры означают молоко, квадратные — смесь.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "Справочный диапазон: ${guide.minimumMl}–${guide.maximumMl} мл · вес ${formatNumber(guide.weightKg)} кг",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        Text(
                            "Столбики показывают кормления по времени и объёму. Суточная норма появится после заполнения даты рождения и веса.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Text(result.explanation, style = MaterialTheme.typography.bodySmall)
                    Text(
                        "Суммируются смесь и измеренное молоко. Прямое грудное вскармливание в мл не оценивается; ориентируйтесь на сигналы ребёнка и рекомендации врача.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            OutlinedButton(onClick = onMeasurement, modifier = Modifier.fillMaxWidth()) { Text("Ввести рост / вес") }
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
                val dayMonth = date.format(DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru")))
                Text(
                    when (date) {
                        LocalDate.now() -> "Сегодня, $dayMonth"
                        LocalDate.now().minusDays(1) -> "Вчера, $dayMonth"
                        else -> dayMonth
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

private data class LastFeedingInfo(val text: String, val minutesAgo: Long?)

private fun lastFeedingInfo(date: LocalDate, milkEntries: List<FoodEntry>): LastFeedingInfo? {
    val lastEntry = milkEntries
        .mapNotNull { entry ->
            runCatching { LocalTime.parse(entry.time) }.getOrNull()?.let { time -> time to entry }
        }
        .maxByOrNull { it.first }
        ?: return null
    val last = lastEntry.first
    val entry = lastEntry.second
    val minutesAgo = if (date == LocalDate.now()) {
        java.time.Duration.between(last, LocalTime.now()).toMinutes().takeIf { it >= 0 }
    } else {
        null
    }
    val ago = when {
        minutesAgo == null -> null
        minutesAgo < 1 -> "Только что"
        minutesAgo < 60 -> "$minutesAgo мин назад"
        else -> {
            val rest = minutesAgo % 60
            if (rest == 0L) "${minutesAgo / 60} ч назад" else "${minutesAgo / 60} ч $rest мин назад"
        }
    }
    val timeText = last.format(DateTimeFormatter.ofPattern("HH:mm"))
    val amountText = "${entry.name} ${formatNumber(entry.amount)} ${entry.unit}"
    val text = if (ago != null) {
        "$ago, $amountText в $timeText"
    } else {
        "$amountText в $timeText"
    }
    return LastFeedingInfo(text, minutesAgo)
}

@Composable
private fun LastFeedingLabel(info: LastFeedingInfo) {
    val minutes = info.minutesAgo
    val escalation = when {
        minutes == null || minutes < 120 -> 0f
        minutes < 180 -> (minutes - 120).toFloat() / 60f
        else -> 1f
    }
    val animatedEscalation by animateFloatAsState(escalation, label = "last-feeding-attention")
    Text(
        text = info.text,
        color = lerp(LocalContentColor.current, MaterialTheme.colorScheme.error, animatedEscalation),
        fontSize = (15f + 6f * animatedEscalation).sp,
        fontWeight = if (animatedEscalation >= 1f) FontWeight.Bold else FontWeight.Medium,
    )
}

internal fun formatNumber(number: Double): String =
    if (number % 1.0 == 0.0) number.toInt().toString() else "%.1f".format(Locale.US, number).replace('.', ',')

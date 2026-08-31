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
import androidx.compose.material.icons.outlined.Bedtime
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
import ru.family.rasti.data.SleepEntry
import ru.family.rasti.data.VitaminEntry
import ru.family.rasti.data.displayDose
import ru.family.rasti.feeding.FeedingMoment
import ru.family.rasti.feeding.FeedingGuide
import ru.family.rasti.feeding.SmartFeedingGuide
import ru.family.rasti.feeding.SmartFeedingRecommendation
import ru.family.rasti.sleep.DatedSleep
import ru.family.rasti.sleep.activeSleep
import ru.family.rasti.sleep.formatSleepDuration
import ru.family.rasti.sleep.lastCompletedSleep
import ru.family.rasti.sleep.sleepDurationMinutes
import ru.family.rasti.sleep.sleepsForDate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class FoodEditorState(
    val originalDate: LocalDate,
    val entry: FoodEntry? = null,
    val fixedName: String? = null,
    val suggestedAmountMl: Int? = null,
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

private data class SleepEditorState(
    val originalDate: LocalDate,
    val entry: SleepEntry? = null,
    val requireEnd: Boolean = false,
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
    var sleepEditor by remember { mutableStateOf<SleepEditorState?>(null) }
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
            SleepControlCard(
                data = viewModel.data,
                selectedDate = selectedDate,
                onStart = { sleepEditor = SleepEditorState(selectedDate) },
                onWake = { sleep ->
                    sleepEditor = SleepEditorState(sleep.startDate, sleep.entry, requireEnd = true)
                },
            )
        }
        item {
            MilkProgressCard(
                data = viewModel.data,
                date = selectedDate,
                day = day,
                onFormula = { amount ->
                    foodEditor = FoodEditorState(selectedDate, fixedName = "Смесь", suggestedAmountMl = amount)
                },
                onMilk = { amount ->
                    foodEditor = FoodEditorState(selectedDate, fixedName = "Молоко", suggestedAmountMl = amount)
                },
                onEditFood = { foodEditor = FoodEditorState(selectedDate, it) },
                onMeasurement = { measurementEditor = MeasurementEditorState(selectedDate, day.measurement) },
            )
        }
        item { SectionHeader("Сон", onAdd = { sleepEditor = SleepEditorState(selectedDate) }) }
        if (day.sleeps.isEmpty()) {
            item { EmptyHint("Снов за этот день пока нет") }
        } else {
            items(day.sleeps.sortedByDescending { it.startTime }, key = { it.id }) { entry ->
                val duration = sleepDurationMinutes(selectedDate, entry)
                EntryRow(
                    title = if (entry.endTime == null) "Сон идёт" else duration?.let { "Сон · ${formatSleepDuration(it)}" } ?: "Сон",
                    subtitle = if (entry.endTime == null) {
                        "с ${entry.startTime}"
                    } else {
                        "${entry.startTime}–${entry.endTime}"
                    },
                    onEdit = {
                        sleepEditor = SleepEditorState(
                            originalDate = selectedDate,
                            entry = entry,
                            requireEnd = entry.endTime != null,
                        )
                    },
                    onDelete = { viewModel.removeSleep(selectedDate, entry.id) },
                )
            }
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
            initialAmountMl = state.suggestedAmountMl?.toDouble(),
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
    sleepEditor?.let { state ->
        SleepEditorDialog(
            title = when {
                state.entry == null -> "Ребёнок уснул"
                state.requireEnd && state.entry.endTime == null -> "Ребёнок проснулся"
                else -> "Изменить сон"
            },
            initialStartDate = state.originalDate,
            initial = state.entry,
            requireEnd = state.requireEnd,
            onDismiss = { sleepEditor = null },
            onSave = { startDate, startTime, endDate, endTime ->
                val original = state.entry
                if (original == null) {
                    viewModel.startSleep(startDate, startTime)
                } else {
                    viewModel.updateSleep(
                        originalDate = state.originalDate,
                        targetStartDate = startDate,
                        original = original,
                        startTime = startTime,
                        endDate = endDate,
                        endTime = endTime,
                    )
                }
                sleepEditor = null
            },
        )
    }
}

@Composable
private fun SleepControlCard(
    data: AppData,
    selectedDate: LocalDate,
    onStart: () -> Unit,
    onWake: (DatedSleep) -> Unit,
) {
    var minuteTick by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            minuteTick++
        }
    }
    val active = remember(data.days, minuteTick) { activeSleep(data) }
    val lastCompleted = remember(data.days, minuteTick) { lastCompletedSleep(data) }
    val canStart = active == null && selectedDate <= LocalDate.now()
    val status = active?.let { sleep ->
        val duration = sleepDurationMinutes(sleep.startDate, sleep.entry)
        "Спит${duration?.let { " · ${formatSleepDuration(it)}" }.orEmpty()} · с ${sleep.entry.startTime}"
    } ?: lastCompleted?.let { sleep ->
        val duration = sleepDurationMinutes(sleep.startDate, sleep.entry)
        "Последний сон${duration?.let { " · ${formatSleepDuration(it)}" }.orEmpty()} · до ${sleep.entry.endTime}"
    } ?: "Сейчас не спит"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Bedtime, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text(status, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onStart, enabled = canStart, modifier = Modifier.weight(1f).height(52.dp)) {
                    Text("Уснула")
                }
                Button(
                    onClick = { active?.let(onWake) },
                    enabled = active != null,
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Text("Проснулась")
                }
            }
        }
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
private fun SmartFeedingLabel(recommendation: SmartFeedingRecommendation) {
    Text(
        text = "Расчётная порция: ${recommendation.amountMl} мл",
        color = MaterialTheme.colorScheme.primary,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
    )
}

private fun smartFeedingTimingText(recommendation: SmartFeedingRecommendation): String =
    when (recommendation.moment) {
        FeedingMoment.EARLY -> recommendation.minutesUntilUsual
            ?.takeIf { it > 0 }
            ?.let { "По обычному ритму через ${formatMinutes(it)}" }
            ?: "Ориентир на следующее кормление"
        FeedingMoment.USUAL_TIME -> "Сейчас привычное время кормления"
        FeedingMoment.LATER_THAN_USUAL -> "По обычному ритму уже пора"
        FeedingMoment.NO_HISTORY -> "Пока считаю по суточной цели"
    }

private fun formatMinutes(minutes: Int): String = when {
    minutes < 60 -> "$minutes мин"
    minutes % 60 == 0 -> "${minutes / 60} ч"
    else -> "${minutes / 60} ч ${minutes % 60} мин"
}

@Composable
private fun MilkProgressCard(
    data: AppData,
    date: LocalDate,
    day: DayRecord,
    onFormula: (Int?) -> Unit,
    onMilk: (Int?) -> Unit,
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
            val lastFeeding = remember(minuteTick, date, data.days) {
                lastFeedingInfo(date, data.days.values)
            }
            val smartRecommendation = remember(minuteTick, date, data.days, guide) {
                SmartFeedingGuide.calculate(data, date, guide)
            }
            lastFeeding?.let { LastFeedingLabel(it) }
            smartRecommendation?.let { SmartFeedingLabel(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onMilk(smartRecommendation?.amountMl) },
                    modifier = Modifier.weight(1f).height(54.dp),
                ) {
                    Text("Молоко")
                }
                Button(
                    onClick = { onFormula(smartRecommendation?.amountMl) },
                    modifier = Modifier.weight(1f).height(54.dp),
                ) {
                    Text("Смесь")
                }
            }
            Text("Питание за сутки", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            MilkIntakeChart(
                entries = milkEntries,
                sleepSegments = sleepsForDate(data, date),
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
                            "Норма по времени: ${guide.minimumMl}–${guide.maximumMl} мл/сутки · вес ${formatNumber(guide.weightKg)} кг",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        Text(
                            "Столбики показывают кормления по времени и объёму. Суточная норма появится после заполнения даты рождения и веса.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (milkEntries.isNotEmpty()) {
                        Text(
                            "Нажмите на маркер, чтобы изменить кормление. График можно увеличить щипком, двигать пальцем и сбросить двойным нажатием.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (smartRecommendation != null) {
                        Text(
                            buildString {
                                append("Расчётная порция: ${smartRecommendation.amountMl} мл. ")
                                append("${smartFeedingTimingText(smartRecommendation)}. ")
                                append("Обычный объём: ${smartRecommendation.usualAmountMl} мл, ")
                                append("обычный интервал: ${formatMinutes(smartRecommendation.usualIntervalMinutes)}. ")
                                if (smartRecommendation.recentIntakeMl > 0) {
                                    append("За последние ${formatMinutes(smartRecommendation.usualIntervalMinutes)}: ${smartRecommendation.recentIntakeMl} мл. ")
                                }
                                if (smartRecommendation.remainingToTargetMl > 0) {
                                    append("До суточной цели: ${smartRecommendation.remainingToTargetMl} мл.")
                                } else {
                                    append("Суточная цель уже набрана.")
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "Расчёт учитывает обычный объём и ритм за 7 дней, суточную цель, а также сколько и как давно ребёнок ел. Влияние недавних кормлений плавно снижается за обычный интервал. Это ориентир: сигналы голода и насыщения ребёнка важнее числа.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            val date = runCatching { LocalDate.parse(day.date) }.getOrNull()
            val sleepMinutes = date?.let { value -> day.sleeps.sumOf { sleepDurationMinutes(value, it) ?: 0L } } ?: 0L
            Text(if (sleepMinutes > 0) "Сон: ${formatSleepDuration(sleepMinutes)}" else "Сон: нет записей")
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

internal data class LastFeedingInfo(val text: String, val minutesAgo: Long?)

internal fun lastFeedingInfo(
    date: LocalDate,
    days: Collection<DayRecord>,
    now: LocalDateTime = LocalDateTime.now(),
): LastFeedingInfo? {
    val cutoff = if (date == now.toLocalDate()) now else date.atTime(LocalTime.MAX)
    val lastEntry = days.asSequence()
        .mapNotNull { record ->
            runCatching { LocalDate.parse(record.date) }.getOrNull()?.let { recordDate -> recordDate to record }
        }
        .filter { (recordDate) -> recordDate <= date }
        .flatMap { (recordDate, record) ->
            record.food.asSequence()
                .filter { it.unit.trim().lowercase() in setOf("мл", "ml") }
                .filter { it.name.trim().lowercase() in setOf("смесь", "молоко") }
                .mapNotNull { entry ->
                    runCatching { LocalTime.parse(entry.time) }.getOrNull()?.let { time ->
                        recordDate.atTime(time) to entry
                    }
                }
        }
        .filter { (entryDateTime) -> entryDateTime <= cutoff }
        .maxByOrNull { it.first }
        ?: return null
    val last = lastEntry.first
    val entry = lastEntry.second
    val minutesAgo = if (date == now.toLocalDate()) {
        java.time.Duration.between(last, now).toMinutes().takeIf { it >= 0 }
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
    val timeText = last.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))
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

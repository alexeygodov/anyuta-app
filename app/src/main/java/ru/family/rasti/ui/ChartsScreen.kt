package ru.family.rasti.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.family.rasti.RastiViewModel
import ru.family.rasti.data.AppData
import ru.family.rasti.data.VaccinationEntry
import ru.family.rasti.data.VaccinationStatus
import ru.family.rasti.growth.GrowthBand
import ru.family.rasti.growth.GrowthMetric
import ru.family.rasti.growth.GrowthStandards
import java.util.Locale
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

private data class ChartPoint(val day: Int, val value: Float)
private data class VaccinationEdit(val date: LocalDate, val entry: VaccinationEntry? = null)
private data class HeightVelocityPoint(
    val endDay: Int,
    val changeCm: Float,
    val intervalDays: Long,
    val cmPerThirtyDays: Float,
)

@Composable
fun ChartsScreen(viewModel: RastiViewModel, modifier: Modifier = Modifier) {
    val data = viewModel.data
    val context = LocalContext.current
    val standards = remember(context) { GrowthStandards(context) }
    var vaccinationEdit by remember { mutableStateOf<VaccinationEdit?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { WeeklyFeedingCard(data) }
        item { WeeklySleepCard(data) }
        item {
            ScreenHeader(
                eyebrow = "Динамика",
                title = "Рост и развитие",
                subtitle = "Коридор −2…+2 SD по стандартам WHO для детей до 5 лет",
            )
        }
        item {
            GrowthChartCard(data, standards, GrowthMetric.HEIGHT, "Рост", "см")
        }
        item {
            GrowthChartCard(data, standards, GrowthMetric.WEIGHT, "Вес", "кг")
        }
        item { MeasuredGrowthCard(data) }
        item { DevelopmentCalendarCard(data, onFussinessChange = viewModel::saveFussiness) }
        item {
            VaccinationTimelineCard(
                data = data,
                onAdd = { vaccinationEdit = VaccinationEdit(LocalDate.now()) },
                onEdit = { date, entry -> vaccinationEdit = VaccinationEdit(date, entry) },
                onDelete = viewModel::removeVaccination,
            )
        }
        item {
            Text(
                "Справочные границы помогают видеть динамику, но не заменяют оценку педиатра. " +
                    "Для недоношенных детей может требоваться скорректированный возраст.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    vaccinationEdit?.let { edit ->
        VaccinationEditorDialog(
            initialDate = edit.date,
            initial = edit.entry,
            onDismiss = { vaccinationEdit = null },
            onSave = { date, name, status, note ->
                if (edit.entry == null) {
                    viewModel.addVaccination(date, name, status, note)
                } else {
                    viewModel.updateVaccination(edit.date, date, edit.entry, name, status, note)
                }
                vaccinationEdit = null
            },
        )
    }
}

@Composable
private fun GrowthVelocityCard(data: AppData, standards: GrowthStandards) {
    val points = remember(data.days, data.profile.birthDate) {
        val measurements = data.days.values.mapNotNull { day ->
            val date = runCatching { LocalDate.parse(day.date) }.getOrNull() ?: return@mapNotNull null
            val age = standards.ageInDays(data.profile.birthDate, day.date) ?: return@mapNotNull null
            val height = day.measurement?.heightCm ?: return@mapNotNull null
            Triple(date, age, height.toFloat())
        }.sortedBy { it.first }
        measurements.zipWithNext().mapNotNull { (first, second) ->
            val days = ChronoUnit.DAYS.between(first.first, second.first)
            if (days <= 0) return@mapNotNull null
            val change = second.third - first.third
            HeightVelocityPoint(
                endDay = second.second,
                changeCm = change,
                intervalDays = days,
                cmPerThirtyDays = change / days * 30f,
            )
        }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Скорость роста по измерениям", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Скорость изменения роста между измерениями, приведённая к 30 дням.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (points.isEmpty()) {
                Text("Добавьте рост минимум в две разные даты.")
            } else {
                HeightVelocityCanvas(points.takeLast(10))
                val last = points.last()
                val sign = if (last.changeCm >= 0f) "+" else ""
                Text(
                    "Последний интервал: $sign${formatFloat(last.changeCm)} см за ${last.intervalDays} дн. " +
                        "(≈${formatFloat(last.cmPerThirtyDays)} см/30 дней)",
                )
                Text(
                    "Небольшие перепады и отрицательные значения возможны из-за разницы техники измерения.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun HeightVelocityCanvas(points: List<HeightVelocityPoint>) {
    val positiveColor = MaterialTheme.colorScheme.primary
    val negativeColor = MaterialTheme.colorScheme.error
    val outline = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.fillMaxWidth().height(210.dp)) {
        val left = 52.dp.toPx()
        val right = size.width - 12.dp.toPx()
        val top = 12.dp.toPx()
        val bottom = size.height - 32.dp.toPx()
        val rawMin = min(0f, points.minOf { it.cmPerThirtyDays })
        val rawMax = max(1f, points.maxOf { it.cmPerThirtyDays })
        val padding = max(.35f, (rawMax - rawMin) * .12f)
        val yMin = rawMin - if (rawMin < 0f) padding else 0f
        val yMax = rawMax + padding
        fun y(value: Float): Float = bottom - (value - yMin) / max(.01f, yMax - yMin) * (bottom - top)
        val zeroY = y(0f)
        val slot = (right - left) / points.size
        val barWidth = min(28.dp.toPx(), slot * .58f)
        drawLine(outline, Offset(left, zeroY), Offset(right, zeroY), 1.dp.toPx())
        points.forEachIndexed { index, point ->
            val centerX = left + slot * (index + .5f)
            val valueY = y(point.cmPerThirtyDays)
            drawRect(
                color = if (point.cmPerThirtyDays >= 0f) positiveColor else negativeColor,
                topLeft = Offset(centerX - barWidth / 2f, min(zeroY, valueY)),
                size = androidx.compose.ui.geometry.Size(barWidth, max(2.dp.toPx(), abs(zeroY - valueY))),
            )
        }
        drawLine(outline, Offset(left, top), Offset(left, bottom), 1.dp.toPx())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor.toArgb()
            textSize = 10.dp.toPx()
        }
        drawContext.canvas.nativeCanvas.apply {
            drawText(formatFloat(yMax), 2.dp.toPx(), top + 5.dp.toPx(), paint)
            drawText(formatFloat(yMin), 2.dp.toPx(), bottom, paint)
            drawText("см/30д", 2.dp.toPx(), (top + bottom) / 2, paint)
            drawText(ageLabel(points.first().endDay), left, size.height - 7.dp.toPx(), paint)
            val end = ageLabel(points.last().endDay)
            drawText(end, right - paint.measureText(end), size.height - 7.dp.toPx(), paint)
        }
    }
}

@Composable
private fun VaccinationTimelineCard(
    data: AppData,
    onAdd: () -> Unit,
    onEdit: (LocalDate, VaccinationEntry) -> Unit,
    onDelete: (LocalDate, String) -> Unit,
) {
    val entries = remember(data.days) {
        data.days.values.flatMap { day ->
            val date = runCatching { LocalDate.parse(day.date) }.getOrNull() ?: return@flatMap emptyList()
            day.vaccinations.map { date to it }
        }.sortedBy { it.first }
    }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("ru")) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Прививки", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Личный график: запланированные и сделанные", style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = onAdd) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Text("Добавить")
                }
            }
            if (entries.isEmpty()) {
                Text("Добавьте назначенную или уже сделанную прививку.")
            } else {
                entries.forEachIndexed { index, (date, entry) ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        VaccinationTimelineMarker(
                            completed = entry.status == VaccinationStatus.COMPLETED,
                            drawTop = index > 0,
                            drawBottom = index < entries.lastIndex,
                        )
                        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                            Text(entry.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${date.format(dateFormatter)} · " +
                                    if (entry.status == VaccinationStatus.COMPLETED) "сделана" else "запланирована",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (entry.note.isNotBlank()) Text(entry.note, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { onEdit(date, entry) }) { Icon(Icons.Outlined.Edit, "Изменить") }
                        IconButton(onClick = { onDelete(date, entry.id) }) { Icon(Icons.Outlined.DeleteOutline, "Удалить") }
                    }
                }
            }
            Text(
                "Приложение не назначает прививки: даты и названия вносятся по вашему календарю и рекомендациям врача.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun VaccinationTimelineMarker(completed: Boolean, drawTop: Boolean, drawBottom: Boolean) {
    val color = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    val line = MaterialTheme.colorScheme.outlineVariant
    val surface = MaterialTheme.colorScheme.surface
    Canvas(Modifier.width(26.dp).height(58.dp)) {
        val x = size.width / 2f
        val center = size.height / 2f
        if (drawTop) drawLine(line, Offset(x, 0f), Offset(x, center), 2.dp.toPx())
        if (drawBottom) drawLine(line, Offset(x, center), Offset(x, size.height), 2.dp.toPx())
        drawCircle(color, radius = 6.dp.toPx(), center = Offset(x, center))
        if (completed) drawCircle(surface, radius = 2.dp.toPx(), center = Offset(x, center))
    }
}

@Composable
private fun GrowthChartCard(
    data: AppData,
    standards: GrowthStandards,
    metric: GrowthMetric,
    title: String,
    unit: String,
) {
    val curve = remember(metric, data.profile.sex) { standards.curve(metric, data.profile.sex) }
    val points = remember(data.days, data.profile.birthDate, metric) {
        data.days.values.mapNotNull { day ->
            val age = standards.ageInDays(data.profile.birthDate, day.date) ?: return@mapNotNull null
            val value = when (metric) {
                GrowthMetric.HEIGHT -> day.measurement?.heightCm
                GrowthMetric.WEIGHT -> day.measurement?.weightKg
            } ?: return@mapNotNull null
            ChartPoint(age, value.toFloat())
        }.sortedBy { it.day }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (points.isEmpty()) {
                Text("Добавьте хотя бы одно измерение на экране «Сегодня».")
            } else {
                val visible = points.filter { it.day in curve.indices }
                if (visible.isEmpty()) {
                    Text("Сейчас встроены нормы WHO для возраста от рождения до 5 лет.")
                } else {
                    GrowthCanvas(curve, visible, unit)
                    val last = visible.last()
                    val band = curve[last.day]
                    val position = when {
                        last.value < band.low -> "ниже справочного коридора"
                        last.value > band.high -> "выше справочного коридора"
                        else -> "в справочном коридоре"
                    }
                    Text(
                        "Последнее: ${formatFloat(last.value)} $unit — $position",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun GrowthCanvas(curve: List<GrowthBand>, points: List<ChartPoint>, unit: String) {
    val primary = MaterialTheme.colorScheme.primary
    val bandColor = MaterialTheme.colorScheme.primaryContainer
    val outline = MaterialTheme.colorScheme.outline
    val medianColor = MaterialTheme.colorScheme.secondary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(Modifier.fillMaxWidth().height(250.dp)) {
        val left = 44.dp.toPx()
        val right = size.width - 12.dp.toPx()
        val top = 12.dp.toPx()
        val bottom = size.height - 34.dp.toPx()

        val rawMin = points.minOf { it.day }
        val rawMax = points.maxOf { it.day }
        val paddingDays = max(45, (rawMax - rawMin) / 8)
        var xMin = max(0, rawMin - paddingDays)
        var xMax = min(curve.lastIndex, rawMax + paddingDays)
        if (xMax - xMin < 90) {
            val center = (xMin + xMax) / 2
            xMin = max(0, center - 45)
            xMax = min(curve.lastIndex, center + 45)
        }
        val displayedCurve = curve.subList(xMin, xMax + 1)
        val values = displayedCurve.flatMap { listOf(it.low, it.high) } + points.map { it.value }
        val rawYMin = values.minOrNull() ?: 0f
        val rawYMax = values.maxOrNull() ?: 1f
        val yPadding = max(0.5f, (rawYMax - rawYMin) * .08f)
        val yMin = floor((rawYMin - yPadding).toDouble()).toFloat()
        val yMax = ceil((rawYMax + yPadding).toDouble()).toFloat()

        fun x(day: Int): Float = left + (day - xMin).toFloat() / max(1, xMax - xMin) * (right - left)
        fun y(value: Float): Float = bottom - (value - yMin) / max(.01f, yMax - yMin) * (bottom - top)

        val step = max(1, (xMax - xMin) / 220)
        val sampled = displayedCurve.filterIndexed { index, _ -> index % step == 0 }.let {
            if (it.last().day != displayedCurve.last().day) it + displayedCurve.last() else it
        }
        val bandPath = Path().apply {
            sampled.forEachIndexed { index, item ->
                if (index == 0) moveTo(x(item.day), y(item.low)) else lineTo(x(item.day), y(item.low))
            }
            sampled.asReversed().forEach { item -> lineTo(x(item.day), y(item.high)) }
            close()
        }
        drawPath(bandPath, bandColor.copy(alpha = .8f))

        fun linePath(selector: (GrowthBand) -> Float): Path = Path().apply {
            sampled.forEachIndexed { index, item ->
                if (index == 0) moveTo(x(item.day), y(selector(item))) else lineTo(x(item.day), y(selector(item)))
            }
        }
        drawPath(
            linePath { it.low },
            outline,
            style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 7f))),
        )
        drawPath(
            linePath { it.high },
            outline,
            style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 7f))),
        )
        drawPath(linePath { it.median }, medianColor, style = Stroke(1.5.dp.toPx()))

        val visiblePoints = points.filter { it.day in xMin..xMax }
        if (visiblePoints.size > 1) {
            val pointPath = Path().apply {
                visiblePoints.forEachIndexed { index, point ->
                    if (index == 0) moveTo(x(point.day), y(point.value)) else lineTo(x(point.day), y(point.value))
                }
            }
            drawPath(pointPath, primary, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        }
        visiblePoints.forEach { point ->
            drawCircle(primary, radius = 4.dp.toPx(), center = Offset(x(point.day), y(point.value)))
            drawCircle(Color.White, radius = 1.5.dp.toPx(), center = Offset(x(point.day), y(point.value)))
        }

        drawLine(outline, Offset(left, bottom), Offset(right, bottom), 1.dp.toPx())
        drawLine(outline, Offset(left, top), Offset(left, bottom), 1.dp.toPx())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor.toArgb()
            textSize = 11.dp.toPx()
        }
        drawContext.canvas.nativeCanvas.apply {
            drawText(formatFloat(yMax), 2.dp.toPx(), top + 5.dp.toPx(), paint)
            drawText(formatFloat(yMin), 2.dp.toPx(), bottom, paint)
            drawText("$unit", 2.dp.toPx(), (top + bottom) / 2, paint)
            drawText(ageLabel(xMin), left, size.height - 8.dp.toPx(), paint)
            val endLabel = ageLabel(xMax)
            drawText(endLabel, right - paint.measureText(endLabel), size.height - 8.dp.toPx(), paint)
        }
    }
}

private fun ageLabel(day: Int): String {
    val months = day / 30.4375
    return if (months < 24) "${months.toInt()} мес." else "${"%.1f".format(Locale.US, months / 12)} г."
}

private fun formatFloat(value: Float): String =
    "%.1f".format(Locale.US, value).replace('.', ',')

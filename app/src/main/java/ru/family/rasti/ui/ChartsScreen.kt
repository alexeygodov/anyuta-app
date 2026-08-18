package ru.family.rasti.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import ru.family.rasti.data.AppData
import ru.family.rasti.growth.GrowthBand
import ru.family.rasti.growth.GrowthMetric
import ru.family.rasti.growth.GrowthStandards
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

private data class ChartPoint(val day: Int, val value: Float)

@Composable
fun ChartsScreen(data: AppData, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val standards = remember(context) { GrowthStandards(context) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Графики роста", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Коридор −2…+2 SD по стандартам WHO для детей до 5 лет")
        }
        item {
            GrowthChartCard(data, standards, GrowthMetric.HEIGHT, "Рост", "см")
        }
        item {
            GrowthChartCard(data, standards, GrowthMetric.WEIGHT, "Вес", "кг")
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

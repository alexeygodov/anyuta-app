package ru.family.rasti.ui

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.family.rasti.data.ChildProfile
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

internal data class DevelopmentLeap(val number: Int, val peakWeek: Int, val title: String)

internal val developmentLeaps = listOf(
    DevelopmentLeap(1, 5, "Ощущения"),
    DevelopmentLeap(2, 8, "Закономерности"),
    DevelopmentLeap(3, 12, "Плавные переходы"),
    DevelopmentLeap(4, 17, "События"),
    DevelopmentLeap(5, 24, "Отношения"),
    DevelopmentLeap(6, 35, "Категории"),
    DevelopmentLeap(7, 44, "Последовательности"),
    DevelopmentLeap(8, 52, "Программы"),
    DevelopmentLeap(9, 60, "Принципы"),
    DevelopmentLeap(10, 71, "Системы"),
)

internal fun leapIntensity(referenceDate: LocalDate, date: LocalDate): Float =
    developmentLeaps.maxOf { leap ->
        val peakDate = referenceDate.plusWeeks(leap.peakWeek.toLong())
        val distance = abs(ChronoUnit.DAYS.between(peakDate, date)).toFloat()
        (1f - distance / 14f).coerceIn(0f, 1f)
    }

@Composable
internal fun DevelopmentLeapCard(profile: ChildProfile) {
    val today = LocalDate.now()
    val birthDate = runCatching { LocalDate.parse(profile.birthDate) }.getOrNull()
    val dueDate = runCatching { LocalDate.parse(profile.dueDate) }.getOrNull()
    val referenceDate = dueDate ?: birthDate
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("ru")) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Периоды скачков развития", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Сегодня в центре · месяц до и месяц после",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (referenceDate == null) {
                Text("Укажите дату рождения в настройках.")
            } else {
                val points = remember(referenceDate, today) {
                    (-30..30).map { offset ->
                        val date = today.plusDays(offset.toLong())
                        offset to leapIntensity(referenceDate, date)
                    }
                }
                val peaks = remember(referenceDate, today) {
                    developmentLeaps.map { leap ->
                        leap to referenceDate.plusWeeks(leap.peakWeek.toLong())
                    }.filter { (_, date) -> date in today.minusDays(30)..today.plusDays(30) }
                }
                LeapIntensityCanvas(points, peaks, today, dateFormatter)
                val nearest = developmentLeaps.minBy { leap ->
                    abs(ChronoUnit.DAYS.between(today, referenceDate.plusWeeks(leap.peakWeek.toLong())))
                }
                val nearestDate = referenceDate.plusWeeks(nearest.peakWeek.toLong())
                Text("Ближайший ориентир: скачок ${nearest.number} «${nearest.title}» · ${nearestDate.format(dateFormatter)}")
                Text(
                    if (dueDate != null) "Расчёт от указанной ПДР."
                    else "ПДР не указана — приблизительный расчёт от даты рождения.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                "Это популярная модель Wonder Weeks, а не медицинская норма: независимые исследования не подтвердили " +
                    "универсальную точность строго заданных недель. Не объясняйте болезненное или необычное беспокойство только графиком.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LeapIntensityCanvas(
    points: List<Pair<Int, Float>>,
    peaks: List<Pair<DevelopmentLeap, LocalDate>>,
    today: LocalDate,
    formatter: DateTimeFormatter,
) {
    val curveColor = MaterialTheme.colorScheme.error
    val fillColor = MaterialTheme.colorScheme.errorContainer
    val todayColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.fillMaxWidth().height(230.dp)) {
        val left = 42.dp.toPx()
        val right = size.width - 8.dp.toPx()
        val top = 30.dp.toPx()
        val bottom = size.height - 38.dp.toPx()
        fun x(offset: Int): Float = left + (offset + 30) / 60f * (right - left)
        fun y(intensity: Float): Float = bottom - intensity.coerceIn(0f, 1f) * (bottom - top)

        val area = Path().apply {
            moveTo(x(-30), bottom)
            points.forEach { (offset, intensity) -> lineTo(x(offset), y(intensity)) }
            lineTo(x(30), bottom)
            close()
        }
        val curve = Path().apply {
            points.forEachIndexed { index, (offset, intensity) ->
                if (index == 0) moveTo(x(offset), y(intensity)) else lineTo(x(offset), y(intensity))
            }
        }
        drawPath(area, fillColor.copy(alpha = .75f))
        drawPath(curve, curveColor, style = Stroke(2.5.dp.toPx()))
        drawLine(axisColor, Offset(left, bottom), Offset(right, bottom), 1.dp.toPx())
        drawLine(axisColor, Offset(left, top), Offset(left, bottom), 1.dp.toPx())
        drawLine(todayColor, Offset(x(0), top), Offset(x(0), bottom), 2.dp.toPx())

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor.toArgb()
            textSize = 10.dp.toPx()
            textAlign = Paint.Align.CENTER
        }
        drawContext.canvas.nativeCanvas.apply {
            drawText("Ожидаемая интенсивность беспокойства", (left + right) / 2f, 15.dp.toPx(), paint)
            drawText(today.minusDays(30).format(formatter), left, bottom + 20.dp.toPx(), paint)
            drawText("сегодня", x(0), bottom + 20.dp.toPx(), paint)
            drawText(today.plusDays(30).format(formatter), right, bottom + 20.dp.toPx(), paint)
            paint.textAlign = Paint.Align.RIGHT
            drawText("выше", left - 5.dp.toPx(), top + 4.dp.toPx(), paint)
            drawText("ниже", left - 5.dp.toPx(), bottom, paint)
            paint.textAlign = Paint.Align.CENTER
            peaks.forEach { (leap, date) ->
                val offset = ChronoUnit.DAYS.between(today, date).toInt().coerceIn(-30, 30)
                drawText("${leap.number}", x(offset), max(top + 12.dp.toPx(), y(1f) - 4.dp.toPx()), paint)
            }
        }
    }
}

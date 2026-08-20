package ru.family.rasti.ui

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.family.rasti.data.ChildProfile
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot

internal data class DevelopmentLeap(val number: Int, val peakWeek: Int, val title: String, val description: String)

internal val developmentLeaps = listOf(
    DevelopmentLeap(
        1, 5, "Ощущения",
        "Обостряются органы чувств: зрение, слух, осязание. Мир внезапно становится ярче и громче — это пугает и утомляет, поэтому больше плача и желания быть на ручках.",
    ),
    DevelopmentLeap(
        2, 8, "Закономерности",
        "Малыш начинает замечать повторяющееся: лица, голоса, ритм дня. Хаос ощущений складывается в первые закономерности — пока мозг перестраивается, ребёнок капризнее и хуже спит.",
    ),
    DevelopmentLeap(
        3, 12, "Плавные переходы",
        "Движения становятся плавнее, ребёнок замечает непрерывные изменения: как движутся предметы, как меняются звук и свет. Новый навык даётся нелегко — больше беспокойства и цепляния за маму.",
    ),
    DevelopmentLeap(
        4, 17, "События",
        "Ребёнок понимает, что у действий есть начало и конец: взял игрушку, мама вышла из комнаты. Появляются первые ожидания — и тревога, когда привычная цепочка событий нарушается.",
    ),
    DevelopmentLeap(
        5, 24, "Отношения",
        "Малыш осознаёт расстояние и связи между предметами и людьми: мама может быть далеко! Пик тревоги разлуки, возможен регресс сна — ребёнок проверяет, что мама рядом.",
    ),
    DevelopmentLeap(
        6, 35, "Категории",
        "Ребёнок учится объединять предметы в группы: животные, еда, игрушки. Мир требует всё больше «вычислений» — отсюда усталость, перепады настроения и беспокойный сон.",
    ),
    DevelopmentLeap(
        7, 44, "Последовательности",
        "Малыш понимает порядок шагов: чтобы поесть, нужна ложка; чтобы погулять — одеться. Осознание идёт через пробы и ошибки, а несовпадение ожиданий с реальностью вызывает слёзы.",
    ),
    DevelopmentLeap(
        8, 52, "Программы",
        "Последовательности складываются в «программы»: утренний ритуал, купание, укладывание. Ребёнок активно тренирует понимание устройства дня — и бунтует, когда программа ломается.",
    ),
    DevelopmentLeap(
        9, 60, "Принципы",
        "Приходит понимание правил и причинно-следственных связей. Малыш начинает проверять границы и последствия своих действий — классический период «нет!» и испытания родительских нервов.",
    ),
    DevelopmentLeap(
        10, 71, "Системы",
        "Ребёнок видит системы целиком: семью, распорядок дня, свою роль в них. Мышление заметно взрослеет, но новая картина мира требует адаптации — беспокойство и капризы на время возвращаются.",
    ),
)

internal fun leapIntensity(referenceDate: LocalDate, date: LocalDate): Float =
    developmentLeaps.maxOf { leap ->
        val peakDate = referenceDate.plusWeeks(leap.peakWeek.toLong())
        val distance = abs(ChronoUnit.DAYS.between(peakDate, date)).toFloat()
        (1f - distance / 14f).coerceIn(0f, 1f)
    }

private const val TOTAL_WEEKS = 76

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
                "Шкала — недели от рождения · вертикаль — интенсивность беспокойства",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (referenceDate == null) {
                Text("Укажите дату рождения в настройках.")
            } else {
                val totalDays = TOTAL_WEEKS * 7
                val points = remember(referenceDate) {
                    (0..totalDays).map { day ->
                        day to leapIntensity(referenceDate, referenceDate.plusDays(day.toLong()))
                    }
                }
                val todayWeek = (ChronoUnit.DAYS.between(referenceDate, today).toFloat() / 7f)
                    .takeIf { it in 0f..TOTAL_WEEKS.toFloat() }
                var selectedLeap by remember { mutableStateOf<DevelopmentLeap?>(null) }
                LeapIntensityCanvas(
                    points = points,
                    todayWeek = todayWeek,
                    selectedLeapNumber = selectedLeap?.number,
                    onLeapSelected = { leap ->
                        selectedLeap = if (leap != null && leap.number == selectedLeap?.number) null else leap
                    },
                )
                val infoLeap = selectedLeap
                if (infoLeap != null) {
                    val peakDate = referenceDate.plusWeeks(infoLeap.peakWeek.toLong())
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = .35f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Скачок ${infoLeap.number} «${infoLeap.title}» — неделя ${infoLeap.peakWeek}, пик ${peakDate.format(dateFormatter)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                infoLeap.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                } else {
                    Text(
                        "Нажмите на точку с номером недели — покажем, что это за скачок.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val nearest = developmentLeaps.minBy { leap ->
                    abs(ChronoUnit.DAYS.between(today, referenceDate.plusWeeks(leap.peakWeek.toLong())))
                }
                val nearestDate = referenceDate.plusWeeks(nearest.peakWeek.toLong())
                Text("Ближайший ориентир: скачок ${nearest.number} «${nearest.title}» · неделя ${nearest.peakWeek} · ${nearestDate.format(dateFormatter)}")
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
    todayWeek: Float?,
    selectedLeapNumber: Int?,
    onLeapSelected: (DevelopmentLeap?) -> Unit,
) {
    val curveColor = MaterialTheme.colorScheme.error
    val todayColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = .22f)
    val axisColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surfaceColor = MaterialTheme.colorScheme.surface

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(230.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val left = 8.dp.toPx()
                    val right = size.width - 8.dp.toPx()
                    val top = 26.dp.toPx()
                    val totalDays = (TOTAL_WEEKS * 7).toFloat()
                    val nearest = developmentLeaps
                        .map { leap ->
                            val px = left + (leap.peakWeek * 7).toFloat() / totalDays * (right - left)
                            leap to hypot(px - offset.x, top - offset.y)
                        }
                        .minByOrNull { (_, distance) -> distance }
                    onLeapSelected(nearest?.takeIf { (_, distance) -> distance <= 18.dp.toPx() }?.first)
                }
            },
    ) {
        val left = 8.dp.toPx()
        val right = size.width - 8.dp.toPx()
        val top = 26.dp.toPx()
        val bottom = size.height - 32.dp.toPx()
        val totalDays = (TOTAL_WEEKS * 7).toFloat()
        fun x(day: Float): Float = left + day / totalDays * (right - left)
        fun y(intensity: Float): Float = bottom - intensity.coerceIn(0f, 1f) * (bottom - top)

        listOf(0.25f, 0.5f, 0.75f).forEach { level ->
            drawLine(gridColor, Offset(left, y(level)), Offset(right, y(level)), 1.dp.toPx())
        }

        if (points.isNotEmpty()) {
            val curve = Path()
            var prevX = x(points.first().first.toFloat())
            var prevY = y(points.first().second)
            curve.moveTo(prevX, prevY)
            for (index in 1 until points.size) {
                val px = x(points[index].first.toFloat())
                val py = y(points[index].second)
                curve.quadraticTo(prevX, prevY, (prevX + px) / 2f, (prevY + py) / 2f)
                prevX = px
                prevY = py
            }
            curve.lineTo(prevX, prevY)
            val area = Path().apply {
                addPath(curve)
                lineTo(prevX, bottom)
                lineTo(x(0f), bottom)
                close()
            }
            drawPath(
                area,
                Brush.verticalGradient(
                    listOf(curveColor.copy(alpha = .30f), curveColor.copy(alpha = .03f)),
                    startY = top,
                    endY = bottom,
                ),
            )
            drawPath(
                curve,
                curveColor,
                style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }

        drawRoundRect(
            color = axisColor,
            topLeft = Offset(left, bottom - 1.dp.toPx()),
            size = Size(right - left, 2.dp.toPx()),
            cornerRadius = CornerRadius(1.dp.toPx()),
        )

        developmentLeaps.forEach { leap ->
            val px = x((leap.peakWeek * 7).toFloat())
            if (leap.number == selectedLeapNumber) {
                drawCircle(curveColor.copy(alpha = .25f), 8.dp.toPx(), Offset(px, y(1f)))
            }
            drawCircle(surfaceColor, 4.5.dp.toPx(), Offset(px, y(1f)))
            drawCircle(curveColor, 3.dp.toPx(), Offset(px, y(1f)))
        }

        todayWeek?.let { week ->
            val tx = x(week * 7f)
            drawLine(
                todayColor,
                Offset(tx, top),
                Offset(tx, bottom),
                2.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
            )
        }

        val weekPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor.toArgb()
            textSize = 9.sp.toPx()
            textAlign = Paint.Align.CENTER
        }
        val leapPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = curveColor.toArgb()
            textSize = 9.sp.toPx()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val todayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = todayColor.toArgb()
            textSize = 9.sp.toPx()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        drawContext.canvas.nativeCanvas.apply {
            for (week in 0..TOTAL_WEEKS step 8) {
                drawText("$week", x((week * 7).toFloat()), bottom + 15.dp.toPx(), weekPaint)
            }
            developmentLeaps.forEach { leap ->
                drawText("${leap.peakWeek}", x((leap.peakWeek * 7).toFloat()), top - 10.dp.toPx(), leapPaint)
            }
            weekPaint.textAlign = Paint.Align.RIGHT
            drawText("беспокойно", right - 2.dp.toPx(), top + 11.dp.toPx(), weekPaint)
            weekPaint.textAlign = Paint.Align.LEFT
            drawText("спокойно", left + 2.dp.toPx(), bottom - 6.dp.toPx(), weekPaint)
            todayWeek?.let { week ->
                val labelX = x(week * 7f).coerceIn(left + 34.dp.toPx(), right - 34.dp.toPx())
                drawText("сегодня · ${week.toInt()} нед", labelX, bottom + 28.dp.toPx(), todayPaint)
            }
        }
    }
}

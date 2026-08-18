package ru.family.rasti.feeding

import ru.family.rasti.data.AppData
import java.time.LocalDate
import kotlin.math.roundToInt

data class MilkGuide(
    val weightKg: Double,
    val weightDate: LocalDate,
    val minimumMl: Int,
    val targetMl: Int,
    val maximumMl: Int,
)

data class MilkGuideResult(
    val guide: MilkGuide? = null,
    val explanation: String,
)

object FeedingGuide {
    fun calculate(data: AppData, date: LocalDate): MilkGuideResult {
        val birthDate = runCatching { LocalDate.parse(data.profile.birthDate) }.getOrNull()
            ?: return MilkGuideResult(explanation = "Укажите корректную дату рождения в настройках")
        if (date.isBefore(birthDate)) return MilkGuideResult(explanation = "Выбранная дата раньше даты рождения")
        if (date.isBefore(birthDate.plusDays(7))) {
            return MilkGuideResult(explanation = "В первую неделю объём подбирают индивидуально вместе с врачом")
        }
        if (date.isAfter(birthDate.plusMonths(6))) {
            return MilkGuideResult(explanation = "После 6 месяцев простой ориентир в мл/кг не применяется")
        }

        val latestWeight = data.days.values
            .asSequence()
            .mapNotNull { day ->
                val measurementDate = runCatching { LocalDate.parse(day.date) }.getOrNull() ?: return@mapNotNull null
                val weight = day.measurement?.weightKg ?: return@mapNotNull null
                if (measurementDate > date || weight <= 0.0) null else measurementDate to weight
            }
            .maxByOrNull { it.first }
            ?: return MilkGuideResult(explanation = "Добавьте вес — он нужен для расчёта ориентира")

        val (weightDate, weightKg) = latestWeight
        return MilkGuideResult(
            guide = MilkGuide(
                weightKg = weightKg,
                weightDate = weightDate,
                minimumMl = roundedToTen(weightKg * 150.0),
                targetMl = roundedToTen(weightKg * 175.0),
                maximumMl = roundedToTen(weightKg * 200.0),
            ),
            explanation = "Справочный диапазон для смеси: 150–200 мл/кг за сутки",
        )
    }

    private fun roundedToTen(value: Double): Int = (value / 10.0).roundToInt() * 10
}

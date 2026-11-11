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
    val isComplementaryFeeding: Boolean = false,
)

data class MilkGuideResult(
    val guide: MilkGuide? = null,
    val explanation: String,
    val sourceTitle: String? = null,
    val sourceUrl: String? = null,
)

object FeedingGuide {
    fun calculate(data: AppData, date: LocalDate): MilkGuideResult {
        val birthDate = runCatching { LocalDate.parse(data.profile.birthDate) }.getOrNull()
            ?: return MilkGuideResult(explanation = "Укажите корректную дату рождения в настройках")
        if (date.isBefore(birthDate)) return MilkGuideResult(explanation = "Выбранная дата раньше даты рождения")
        if (date.isBefore(birthDate.plusDays(7))) {
            return MilkGuideResult(explanation = "В первую неделю объём подбирают индивидуально вместе с врачом")
        }
        if (!date.isBefore(birthDate.plusYears(1))) {
            return MilkGuideResult(explanation = "С года питание оценивают с учётом всего рациона; ориентир смеси до года больше не применяется")
        }
        val isComplementaryFeeding = !date.isBefore(birthDate.plusMonths(6))

        val latestWeight = data.days.values
            .asSequence()
            .mapNotNull { day ->
                val measurementDate = runCatching { LocalDate.parse(day.date) }.getOrNull() ?: return@mapNotNull null
                val weight = day.measurement?.weightKg ?: return@mapNotNull null
                if (measurementDate > date || !weight.isFinite() || weight <= 0.0) null else measurementDate to weight
            }
            .maxByOrNull { it.first }
            ?: return MilkGuideResult(explanation = "Добавьте вес — он нужен для расчёта ориентира")

        val (weightDate, weightKg) = latestWeight
        return MilkGuideResult(
            guide = MilkGuide(
                weightKg = weightKg,
                weightDate = weightDate,
                // Better Health Channel (Victoria): 90–120 ml/kg/day at 6–12 months.
                // The midpoint is only an app reference, not a separate medical recommendation.
                minimumMl = roundedToTen(weightKg * if (isComplementaryFeeding) 90.0 else 150.0),
                targetMl = roundedToTen(weightKg * if (isComplementaryFeeding) 105.0 else 175.0),
                maximumMl = roundedToTen(weightKg * if (isComplementaryFeeding) 120.0 else 200.0),
                isComplementaryFeeding = isComplementaryFeeding,
            ),
            explanation = if (isComplementaryFeeding) {
                "В 6–12 месяцев: справочный диапазон молока/смеси 90–120 мл/кг за сутки (Better Health Channel, штат Виктория). " +
                    "Середина 105 мл/кг — отметка приложения, а не отдельная рекомендация. " +
                    "Учитывайте аппетит и прикорм; не нужно добирать объём до отметки или ограничивать им кормления. Прикорм и воду в молочный объём не включаем."
            } else {
                "Справочный диапазон для смеси: 150–200 мл/кг за сутки"
            },
            sourceTitle = if (isComplementaryFeeding) "Better Health Channel — молочное питание" else null,
            sourceUrl = if (isComplementaryFeeding) {
                "https://www.betterhealth.vic.gov.au/health/healthyliving/bottle-feeding-nutrition-and-safety"
            } else null,
        )
    }

    private fun roundedToTen(value: Double): Int = (value / 10.0).roundToInt() * 10
}

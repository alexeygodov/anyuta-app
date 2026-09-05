package ru.family.rasti.data

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

enum class ChildSex { GIRL, BOY }

data class ChildProfile(
    val name: String = "Малыш",
    val birthDate: String = LocalDate.now().minusYears(1).toString(),
    val dueDate: String = "",
    val sex: ChildSex = ChildSex.GIRL,
    val updatedAt: String = "1970-01-01T00:00:00Z",
)

fun ChildProfile.isPlaceholder(): Boolean = name.trim().equals("Малыш", ignoreCase = true)

data class FoodEntry(
    val id: String = UUID.randomUUID().toString(),
    val time: String,
    val name: String,
    val amount: Double,
    val unit: String,
    val updatedAt: String = OffsetDateTime.now().toString(),
)

data class VitaminEntry(
    val id: String = UUID.randomUUID().toString(),
    val time: String,
    val name: String,
    val amount: Double,
    val unit: String,
    val updatedAt: String = OffsetDateTime.now().toString(),
)

data class SleepEntry(
    val id: String = UUID.randomUUID().toString(),
    val startTime: String,
    val endDate: String? = null,
    val endTime: String? = null,
    val updatedAt: String = OffsetDateTime.now().toString(),
)

enum class VaccinationStatus { PLANNED, COMPLETED }

data class VaccinationEntry(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val status: VaccinationStatus = VaccinationStatus.PLANNED,
    val note: String = "",
    val updatedAt: String = OffsetDateTime.now().toString(),
)

data class Measurement(
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val time: String = "",
    val updatedAt: String = OffsetDateTime.now().toString(),
)

data class DayRecord(
    val date: String,
    val food: List<FoodEntry> = emptyList(),
    val vitamins: List<VitaminEntry> = emptyList(),
    val vaccinations: List<VaccinationEntry> = emptyList(),
    val sleeps: List<SleepEntry> = emptyList(),
    val deletedFoodIds: Set<String> = emptySet(),
    val deletedVitaminIds: Set<String> = emptySet(),
    val deletedVaccinationIds: Set<String> = emptySet(),
    val deletedSleepIds: Set<String> = emptySet(),
    val measurement: Measurement? = null,
    val measurementDeletedAt: String? = null,
    val note: String = "",
    val fussiness: Int? = null,
    val fussinessUpdatedAt: String = "",
    val updatedAt: String = OffsetDateTime.now().toString(),
)

data class AppData(
    val profile: ChildProfile = ChildProfile(),
    val days: Map<String, DayRecord> = emptyMap(),
)

data class GitHubConfig(
    val owner: String = "alexeygodov",
    val repo: String = "anyuta-data",
    val branch: String = "main",
    val token: String = "",
)

data class MaxConfig(
    val enabled: Boolean = false,
    val token: String = "",
    val chatId: String = "",
) {
    fun isConfigured(): Boolean = enabled && token.isNotBlank() && chatId.toLongOrNull() != null
}

data class NotificationPreferences(
    val feedingReminders: Boolean = true,
    val vitaminReminders: Boolean = true,
    val syncUpdates: Boolean = true,
)

fun AppData.day(date: LocalDate): DayRecord =
    days[date.toString()] ?: DayRecord(date = date.toString())

fun AppData.updateDay(day: DayRecord): AppData =
    copy(days = days + (day.date to day.copy(updatedAt = OffsetDateTime.now().toString())))

fun formatVitaminDose(amount: Double, unit: String): String {
    val number = if (amount % 1.0 == 0.0) amount.toInt().toString() else amount.toString().replace('.', ',')
    val normalizedUnit = unit.trim().lowercase()
    if (normalizedUnit != "капля") return "$number ${unit.trim()}".trim()
    val integer = amount.toInt()
    val word = if (amount % 1.0 != 0.0) {
        "капли"
    } else if (integer % 100 in 11..14) {
        "капель"
    } else {
        when (integer % 10) {
            1 -> "капля"
            in 2..4 -> "капли"
            else -> "капель"
        }
    }
    return "$number $word"
}

fun VitaminEntry.displayDose(): String = formatVitaminDose(amount, unit)

internal fun parseLegacyVitaminDose(value: String): Pair<Double, String> {
    val numberPattern = Regex("[-+]?\\d+(?:[.,]\\d+)?")
    val amount = numberPattern.find(value)?.value?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
    val unit = normalizeVitaminUnit(value.replace(numberPattern, "").trim())
    return amount to unit
}

internal fun normalizeVitaminUnit(value: String): String = when (value.trim().lowercase()) {
    "капля", "капли", "капель" -> "капля"
    else -> value.trim().ifBlank { "ед." }
}

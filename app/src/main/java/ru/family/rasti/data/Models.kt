package ru.family.rasti.data

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

enum class ChildSex { GIRL, BOY }

data class ChildProfile(
    val name: String = "Малыш",
    val birthDate: String = LocalDate.now().minusYears(1).toString(),
    val sex: ChildSex = ChildSex.GIRL,
    val updatedAt: String = OffsetDateTime.now().toString(),
)

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
    val dose: String,
    val updatedAt: String = OffsetDateTime.now().toString(),
)

data class Measurement(
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val updatedAt: String = OffsetDateTime.now().toString(),
)

data class DayRecord(
    val date: String,
    val food: List<FoodEntry> = emptyList(),
    val vitamins: List<VitaminEntry> = emptyList(),
    val deletedFoodIds: Set<String> = emptySet(),
    val deletedVitaminIds: Set<String> = emptySet(),
    val measurement: Measurement? = null,
    val note: String = "",
    val updatedAt: String = OffsetDateTime.now().toString(),
)

data class AppData(
    val profile: ChildProfile = ChildProfile(),
    val days: Map<String, DayRecord> = emptyMap(),
)

data class GitHubConfig(
    val owner: String = "",
    val repo: String = "",
    val branch: String = "main",
    val token: String = "",
)

fun AppData.day(date: LocalDate): DayRecord =
    days[date.toString()] ?: DayRecord(date = date.toString())

fun AppData.updateDay(day: DayRecord): AppData =
    copy(days = days + (day.date to day.copy(updatedAt = OffsetDateTime.now().toString())))

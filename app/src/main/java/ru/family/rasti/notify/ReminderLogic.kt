package ru.family.rasti.notify

import ru.family.rasti.data.AppData
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

const val FEEDING_THRESHOLD_MINUTES: Long = 180
val VITAMIN_REMIND_TIME: LocalTime = LocalTime.NOON

internal fun isMilkEntry(name: String, unit: String): Boolean =
    unit.trim().lowercase() in setOf("мл", "ml") &&
        name.trim().lowercase() in setOf("смесь", "молоко")

internal fun isVitaminDName(name: String): Boolean {
    val normalized = name.lowercase().replace("ё", "е")
    return normalized.contains("витамин d") || normalized.contains("витамин д") || normalized.contains("d3")
}

internal fun lastMilkFeeding(data: AppData, today: LocalDate): LocalDateTime? =
    (0L..2L).asSequence()
        .mapNotNull { offset -> data.days[today.minusDays(offset).toString()] }
        .flatMap { day ->
            val date = runCatching { LocalDate.parse(day.date) }.getOrNull() ?: return@flatMap emptySequence<LocalDateTime>()
            day.food.asSequence()
                .filter { isMilkEntry(it.name, it.unit) }
                .mapNotNull { entry ->
                    runCatching { LocalTime.parse(entry.time) }.getOrNull()
                        ?.let { LocalDateTime.of(date, it) }
                }
        }
        .maxOrNull()

internal fun feedingReminderDue(data: AppData, now: LocalDateTime): LocalDateTime? {
    val last = lastMilkFeeding(data, now.toLocalDate()) ?: return null
    val minutes = Duration.between(last, now).toMinutes()
    return if (minutes >= FEEDING_THRESHOLD_MINUTES) last else null
}

internal fun vitaminReminderDue(data: AppData, now: LocalDateTime): Boolean {
    if (now.toLocalTime() < VITAMIN_REMIND_TIME) return false
    val day = data.days[now.toLocalDate().toString()] ?: return true
    return day.vitamins.none { isVitaminDName(it.name) }
}

internal fun formatElapsed(minutes: Long): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours <= 0 -> "$mins мин"
        mins == 0L -> "$hours ч"
        else -> "$hours ч $mins мин"
    }
}

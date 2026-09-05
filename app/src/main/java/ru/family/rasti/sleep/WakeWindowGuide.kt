package ru.family.rasti.sleep

import java.time.LocalDate
import java.time.Period

data class WakeWindowGuide(val ageMonths: Int, val minimumMinutes: Int, val maximumMinutes: Int) {
    val suggestedMinutes: Int get() = kotlin.math.round((minimumMinutes + maximumMinutes) / 10f).toInt() * 5
}

/** Broad educational ranges, not a clinical prescription or a prediction of tiredness. */
fun wakeWindowGuide(birthDate: String, today: LocalDate = LocalDate.now()): WakeWindowGuide? {
    val birth = runCatching { LocalDate.parse(birthDate) }.getOrNull() ?: return null
    if (birth > today || today >= birth.plusMonths(13)) return null
    val age = Period.between(birth, today).toTotalMonths().toInt()
    val range = when (age) {
        0 -> 30 to 60
        1, 2 -> 60 to 120
        3, 4 -> 75 to 150
        5, 6 -> 120 to 240
        7, 8, 9 -> 150 to 270
        else -> 180 to 360
    }
    return WakeWindowGuide(age, range.first, range.second)
}

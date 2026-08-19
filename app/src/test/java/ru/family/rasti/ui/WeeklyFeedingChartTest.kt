package ru.family.rasti.ui

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import ru.family.rasti.data.AppData
import ru.family.rasti.data.ChildProfile
import ru.family.rasti.data.DayRecord
import ru.family.rasti.data.FoodEntry
import ru.family.rasti.data.Measurement

class WeeklyFeedingChartTest {
    @Test
    fun weeklySummaries_split_milk_and_formula_and_include_guide() {
        val endDate = LocalDate.of(2026, 8, 19)
        val weightDay = DayRecord("2026-08-13", measurement = Measurement(weightKg = 6.0))
        val today = DayRecord(
            "2026-08-19",
            food = listOf(
                FoodEntry(time = "08:00", name = "Молоко", amount = 120.0, unit = "мл"),
                FoodEntry(time = "11:00", name = "Смесь", amount = 90.0, unit = "мл"),
            ),
        )
        val data = AppData(
            profile = ChildProfile(name = "Аня", birthDate = "2026-05-10"),
            days = mapOf(weightDay.date to weightDay, today.date to today),
        )

        val summaries = weeklyFeedingSummaries(data, endDate)

        assertEquals(7, summaries.size)
        assertEquals(120f, summaries.last().milkMl, 0f)
        assertEquals(90f, summaries.last().formulaMl, 0f)
        assertNotNull(summaries.last().minimumMl)
    }
}

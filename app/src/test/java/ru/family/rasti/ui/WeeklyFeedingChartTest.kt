package ru.family.rasti.ui

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import ru.family.rasti.data.AppData
import ru.family.rasti.data.ChildProfile
import ru.family.rasti.data.DayRecord
import ru.family.rasti.data.FoodEntry
import ru.family.rasti.data.Measurement

class WeeklyFeedingChartTest {
    @Test
    fun weekly_summaries_keep_weight_ranges_across_six_months_and_stop_at_birthday() {
        val data = AppData(
            profile = ChildProfile(birthDate = "2026-01-01"),
            days = mapOf("2026-06-01" to DayRecord("2026-06-01", measurement = Measurement(weightKg = 8.0))),
        )
        val sixMonths = weeklyFeedingSummaries(data, LocalDate.parse("2026-07-02"))
        assertEquals(1200f, sixMonths[4].minimumMl)
        assertEquals(1600f, sixMonths[4].maximumMl)
        assertEquals(720f, sixMonths[5].minimumMl)
        assertEquals(960f, sixMonths[5].maximumMl)
        val tenMonths = weeklyFeedingSummaries(data, LocalDate.parse("2026-11-02"))
        assertEquals(720f, tenMonths[4].minimumMl)
        assertEquals(960f, tenMonths[5].maximumMl)
        val birthday = weeklyFeedingSummaries(data, LocalDate.parse("2027-01-02"))
        assertEquals(960f, birthday[4].maximumMl)
        assertNull(birthday[5].minimumMl)
        assertNull(birthday[6].maximumMl)
        val withoutWeight = data.copy(days = emptyMap())
        assertNull(weeklyFeedingSummaries(withoutWeight, LocalDate.parse("2026-08-01")).last().maximumMl)
    }

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

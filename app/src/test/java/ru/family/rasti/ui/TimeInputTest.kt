package ru.family.rasti.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.family.rasti.data.DayRecord
import ru.family.rasti.data.FoodEntry

class TimeInputTest {
    @Test
    fun normalizeTimeInput_accepts_short_hours_and_formats_time() {
        assertEquals("09:05", normalizeTimeInput("9:5"))
        assertEquals("23:59", normalizeTimeInput("23:59"))
    }

    @Test
    fun normalizeTimeInput_rejects_invalid_values() {
        assertNull(normalizeTimeInput("24:00"))
        assertNull(normalizeTimeInput("12:60"))
        assertNull(normalizeTimeInput("без времени"))
    }

    @Test
    fun adjustTimeInput_moves_in_fifteen_minute_steps() {
        assertEquals("10:15", adjustTimeInput("10:00", 15))
        assertEquals("23:50", adjustTimeInput("00:05", -15))
    }

    @Test
    fun normalizeTimeToFiveMinutes_rounds_down_to_wheel_step() {
        assertEquals("09:05", normalizeTimeToFiveMinutes("09:09"))
        assertEquals("23:55", normalizeTimeToFiveMinutes("23:59"))
    }

    @Test
    fun normalizeMilkAmount_uses_five_ml_steps_and_limits() {
        assertEquals(100f, normalizeMilkAmount(102f), 0f)
        assertEquals(105f, normalizeMilkAmount(103f), 0f)
        assertEquals(0f, normalizeMilkAmount(0f), 0f)
        assertEquals(200f, normalizeMilkAmount(500f), 0f)
    }

    @Test
    fun popularMilkAmounts_picks_most_frequent_sorted_ascending() {
        val updatedAt = "2026-08-19T20:00:00+04:00"
        val days = listOf(
            DayRecord(
                date = "2026-08-18",
                food = listOf(
                    FoodEntry(time = "08:00", name = "Молоко", amount = 120.0, unit = "мл", updatedAt = updatedAt),
                    FoodEntry(time = "12:00", name = "Молоко", amount = 120.0, unit = "мл", updatedAt = updatedAt),
                    FoodEntry(time = "16:00", name = "Молоко", amount = 90.0, unit = "мл", updatedAt = updatedAt),
                ),
            ),
            DayRecord(
                date = "2026-08-19",
                food = listOf(
                    FoodEntry(time = "09:00", name = "молоко", amount = 150.0, unit = "мл", updatedAt = updatedAt),
                    FoodEntry(time = "13:00", name = "Смесь", amount = 200.0, unit = "мл", updatedAt = updatedAt),
                    FoodEntry(time = "18:00", name = "Молоко", amount = 60.0, unit = "мл", updatedAt = updatedAt),
                    FoodEntry(time = "20:00", name = "Каша", amount = 50.0, unit = "г", updatedAt = updatedAt),
                ),
            ),
        )
        assertEquals(listOf(60, 90, 120, 150), popularMilkAmounts(days, "Молоко"))
        assertEquals(listOf(200), popularMilkAmounts(days, "Смесь"))
        assertEquals(emptyList<Int>(), popularMilkAmounts(days, "Каша"))
    }
}

package ru.family.rasti.notify

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.family.rasti.data.DayRecord
import ru.family.rasti.data.FoodEntry
import ru.family.rasti.data.VitaminEntry
import java.time.LocalDate

class DaySummaryTest {

    private val date: LocalDate = LocalDate.of(2026, 8, 21)

    @Test
    fun buildDaySummary_full_day() {
        val day = DayRecord(
            date = date.toString(),
            food = listOf(
                FoodEntry(time = "09:00", name = "Молоко", amount = 120.0, unit = "мл"),
                FoodEntry(time = "12:30", name = "Смесь", amount = 90.5, unit = "мл"),
                FoodEntry(time = "15:00", name = "Каша", amount = 150.0, unit = "г"),
                FoodEntry(time = "20:45", name = "Молоко", amount = 110.0, unit = "мл"),
            ),
            vitamins = listOf(
                VitaminEntry(time = "09:15", name = "Витамин D", amount = 2.0, unit = "капля"),
            ),
        )
        assertEquals(
            "📋 Итог дня — 21 августа\n" +
                "🍼 Кормлений: 3 · всего 320,5 мл\n" +
                "🕘 Последнее: Молоко 110 мл в 20:45\n" +
                "💊 Витамин D: принят в 09:15",
            buildDaySummary(day, date),
        )
    }

    @Test
    fun buildDaySummary_empty_day() {
        val day = DayRecord(date = date.toString())
        assertEquals(
            "📋 Итог дня — 21 августа\n" +
                "🍼 Кормлений не было\n" +
                "💊 Витамин D: не принят",
            buildDaySummary(day, date),
        )
    }

    @Test
    fun buildDaySummary_whole_ml_not_decimal() {
        val day = DayRecord(
            date = date.toString(),
            food = listOf(FoodEntry(time = "10:00", name = "Смесь", amount = 120.0, unit = "мл")),
        )
        assertEquals(
            "📋 Итог дня — 21 августа\n" +
                "🍼 Кормлений: 1 · всего 120 мл\n" +
                "🕘 Последнее: Смесь 120 мл в 10:00\n" +
                "💊 Витамин D: не принят",
            buildDaySummary(day, date),
        )
    }
}

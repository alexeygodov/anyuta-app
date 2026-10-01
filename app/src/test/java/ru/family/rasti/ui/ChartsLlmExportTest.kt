package ru.family.rasti.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.family.rasti.data.AppData
import ru.family.rasti.data.ChildProfile
import ru.family.rasti.data.DayRecord
import ru.family.rasti.data.FoodEntry
import ru.family.rasti.data.Measurement
import ru.family.rasti.data.SleepEntry
import java.time.LocalDateTime

class ChartsLlmExportTest {
    @Test fun exportsAllFourMetricsInDateOrderWithoutNameOrNotes() {
        val data = AppData(
            profile = ChildProfile(name = "Секретное имя", birthDate = "2026-01-01", dueDate = "2026-01-10"),
            days = mapOf(
                "2026-05-02" to DayRecord(
                    date = "2026-05-02",
                    food = listOf(FoodEntry(id = "formula", time = "08:00", name = "Смесь", amount = 110.0, unit = "мл")),
                    measurement = Measurement(heightCm = 62.5, weightKg = 6.4),
                    note = "PRIVATE-NOTE",
                ),
                "2026-05-01" to DayRecord(
                    date = "2026-05-01",
                    food = listOf(FoodEntry(id = "milk", time = "21:00", name = "Молоко", amount = 90.0, unit = "мл")),
                    sleeps = listOf(SleepEntry(id = "sleep", startTime = "23:00", endDate = "2026-05-02", endTime = "01:00")),
                ),
            ),
        )

        val text = buildChartsLlmPrompt(data, LocalDateTime.of(2026, 5, 3, 12, 0))
        assertTrue(text.contains("2026-05-02 | 62,5 см | 6,4 кг"))
        assertTrue(text.contains("2026-05-01 21:00 | Молоко | 90 мл"))
        assertTrue(text.contains("2026-05-02 08:00 | Смесь | 110 мл"))
        assertTrue(text.contains("2026-05-01 23:00 → 2026-05-02 01:00 | 120 мин"))
        assertTrue(text.indexOf("2026-05-01 21:00") < text.indexOf("2026-05-02 08:00"))
        assertTrue(text.contains("Отсутствие записи не означает ноль"))
        assertFalse(text.contains("Секретное имя"))
        assertFalse(text.contains("PRIVATE-NOTE"))
    }

    @Test fun missingRecordsStayUnknownAndUserTextCannotBreakRows() {
        val empty = buildChartsLlmPrompt(AppData(), LocalDateTime.of(2026, 5, 3, 12, 0))
        assertTrue(empty.contains("ИЗМЕРЕНИЯ (дата | рост | вес)\nНет записей"))
        assertTrue(empty.contains("ПИТАНИЕ (дата время | вид | количество)\nНет записей"))
        assertTrue(empty.contains("СОН (начало → конец | длительность)\nНет записей"))

        val data = AppData(days = mapOf("2026-05-01" to DayRecord(
            date = "2026-05-01",
            food = listOf(FoodEntry(id = "food", time = "09:00", name = "Каша\nновая строка", amount = 30.0, unit = "г")),
            sleeps = listOf(SleepEntry(id = "ongoing", startTime = "10:00")),
        )))
        val text = buildChartsLlmPrompt(data, LocalDateTime.of(2026, 5, 1, 11, 0))
        assertTrue(text.contains("Каша новая строка | 30 г"))
        assertTrue(text.contains("2026-05-01 10:00 → продолжается | не завершён"))
    }
}
package ru.family.rasti.notify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.family.rasti.data.AppData
import ru.family.rasti.data.DayRecord
import ru.family.rasti.data.FoodEntry
import ru.family.rasti.data.VitaminEntry
import java.time.LocalDate

class SyncDiffTest {

    private val today: LocalDate = LocalDate.of(2026, 8, 21)

    private fun dataWith(vararg days: DayRecord) = AppData(days = days.associateBy { it.date })

    @Test
    fun collectSyncUpdates_detects_new_milk_feeding() {
        val before = dataWith(DayRecord(date = today.toString()))
        val after = dataWith(
            DayRecord(
                date = today.toString(),
                food = listOf(FoodEntry(id = "a", time = "12:30", name = "Молоко", amount = 120.0, unit = "мл")),
            ),
        )
        val updates = collectSyncUpdates(before, after, today)
        assertEquals(1, updates.size)
        assertEquals("Новое кормление", updates[0].title)
        assertEquals("Молоко · 120 мл · 12:30", updates[0].text)
    }

    @Test
    fun collectSyncUpdates_ignores_already_known_entries() {
        val entry = FoodEntry(id = "a", time = "12:30", name = "Смесь", amount = 90.0, unit = "мл")
        val before = dataWith(DayRecord(date = today.toString(), food = listOf(entry)))
        val after = dataWith(DayRecord(date = today.toString(), food = listOf(entry)))
        assertTrue(collectSyncUpdates(before, after, today).isEmpty())
    }

    @Test
    fun collectSyncUpdates_ignores_non_milk_food() {
        val before = dataWith(DayRecord(date = today.toString()))
        val after = dataWith(
            DayRecord(
                date = today.toString(),
                food = listOf(FoodEntry(id = "a", time = "12:30", name = "Каша", amount = 150.0, unit = "г")),
            ),
        )
        assertTrue(collectSyncUpdates(before, after, today).isEmpty())
    }

    @Test
    fun collectSyncUpdates_detects_new_vitamin_d() {
        val before = dataWith(DayRecord(date = today.toString()))
        val after = dataWith(
            DayRecord(
                date = today.toString(),
                vitamins = listOf(VitaminEntry(id = "v", time = "09:15", name = "Витамин D", amount = 2.0, unit = "капля")),
            ),
        )
        val updates = collectSyncUpdates(before, after, today)
        assertEquals(1, updates.size)
        assertEquals("Витамин D принят", updates[0].title)
        assertEquals("2 капли · 09:15", updates[0].text)
    }

    @Test
    fun collectSyncUpdates_ignores_other_vitamins() {
        val before = dataWith(DayRecord(date = today.toString()))
        val after = dataWith(
            DayRecord(
                date = today.toString(),
                vitamins = listOf(VitaminEntry(id = "v", time = "09:15", name = "Омега-3", amount = 1.0, unit = "капсула")),
            ),
        )
        assertTrue(collectSyncUpdates(before, after, today).isEmpty())
    }

    @Test
    fun collectSyncUpdates_ignores_entries_older_than_yesterday() {
        val old = today.minusDays(3)
        val before = dataWith()
        val after = dataWith(
            DayRecord(
                date = old.toString(),
                food = listOf(FoodEntry(id = "a", time = "12:30", name = "Молоко", amount = 120.0, unit = "мл")),
            ),
        )
        assertTrue(collectSyncUpdates(before, after, today).isEmpty())
    }

    @Test
    fun collectSyncUpdates_checks_yesterday_too() {
        val yesterday = today.minusDays(1)
        val before = dataWith(DayRecord(date = yesterday.toString()))
        val after = dataWith(
            DayRecord(
                date = yesterday.toString(),
                food = listOf(FoodEntry(id = "a", time = "23:40", name = "Смесь", amount = 130.5, unit = "мл")),
            ),
        )
        val updates = collectSyncUpdates(before, after, today)
        assertEquals(1, updates.size)
        assertEquals("Смесь · 130,5 мл · 23:40", updates[0].text)
    }
}

package ru.family.rasti.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.family.rasti.data.AppData
import ru.family.rasti.data.DayRecord
import ru.family.rasti.data.FoodEntry

class GitHubSyncTest {
    @Test
    fun merge_keeps_entries_from_both_devices() {
        val first = FoodEntry("first", "08:00", "Каша", 100.0, "г", "2026-08-18T08:00:00Z")
        val second = FoodEntry("second", "09:00", "Смесь", 150.0, "мл", "2026-08-18T09:00:00Z")
        val local = AppData(days = mapOf("2026-08-18" to DayRecord("2026-08-18", food = listOf(first))))
        val remote = AppData(days = mapOf("2026-08-18" to DayRecord("2026-08-18", food = listOf(second))))

        val merged = GitHubSync().merge(local, remote).days.getValue("2026-08-18")

        assertEquals(setOf("first", "second"), merged.food.map { it.id }.toSet())
    }

    @Test
    fun merge_does_not_resurrect_deleted_entry() {
        val deleted = FoodEntry("deleted", "08:00", "Каша", 100.0, "г", "2026-08-18T08:00:00Z")
        val localDay = DayRecord("2026-08-18", deletedFoodIds = setOf("deleted"))
        val remoteDay = DayRecord("2026-08-18", food = listOf(deleted))

        val merged = GitHubSync().merge(
            AppData(days = mapOf(localDay.date to localDay)),
            AppData(days = mapOf(remoteDay.date to remoteDay)),
        ).days.getValue("2026-08-18")

        assertTrue(merged.food.isEmpty())
        assertTrue("deleted" in merged.deletedFoodIds)
    }
}

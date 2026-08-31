package ru.family.rasti.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.family.rasti.data.AppData
import ru.family.rasti.data.ChildProfile
import ru.family.rasti.data.DayRecord
import ru.family.rasti.data.FoodEntry
import ru.family.rasti.data.Measurement
import ru.family.rasti.data.SleepEntry
import ru.family.rasti.data.VaccinationEntry

class GitHubSyncTest {
    @Test
    fun merge_prefers_real_remote_profile_over_newer_placeholder() {
        val placeholder = ChildProfile(name = "Малыш", updatedAt = "2026-08-19T12:37:00Z")
        val real = ChildProfile(name = "Аня", birthDate = "2026-05-10", updatedAt = "2026-08-18T23:00:00Z")

        val merged = GitHubSync().merge(AppData(profile = placeholder), AppData(profile = real))

        assertEquals("Аня", merged.profile.name)
        assertEquals("2026-05-10", merged.profile.birthDate)
    }

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

    @Test
    fun merge_does_not_resurrect_deleted_measurement() {
        val localDay = DayRecord(
            date = "2026-08-18",
            measurementDeletedAt = "2026-08-18T10:00:00Z",
        )
        val remoteDay = DayRecord(
            date = "2026-08-18",
            measurement = Measurement(weightKg = 6.0, updatedAt = "2026-08-18T09:00:00Z"),
        )

        val merged = GitHubSync().merge(
            AppData(days = mapOf(localDay.date to localDay)),
            AppData(days = mapOf(remoteDay.date to remoteDay)),
        ).days.getValue("2026-08-18")

        assertNull(merged.measurement)
        assertEquals("2026-08-18T10:00:00Z", merged.measurementDeletedAt)
    }

    @Test
    fun merge_does_not_resurrect_deleted_vaccination() {
        val vaccination = VaccinationEntry(id = "vaccine", name = "Пентаксим")
        val localDay = DayRecord("2026-08-18", deletedVaccinationIds = setOf("vaccine"))
        val remoteDay = DayRecord("2026-08-18", vaccinations = listOf(vaccination))

        val merged = GitHubSync().merge(
            AppData(days = mapOf(localDay.date to localDay)),
            AppData(days = mapOf(remoteDay.date to remoteDay)),
        ).days.getValue("2026-08-18")

        assertTrue(merged.vaccinations.isEmpty())
        assertTrue("vaccine" in merged.deletedVaccinationIds)
    }

    @Test
    fun merge_keepsNewestSleepAndDoesNotResurrectDeletedSleep() {
        val older = SleepEntry(id = "sleep", startTime = "12:00", updatedAt = "2026-08-31T12:00:00Z")
        val newer = older.copy(
            endDate = "2026-08-31",
            endTime = "13:30",
            updatedAt = "2026-08-31T13:30:00Z",
        )
        val deleted = SleepEntry(id = "deleted-sleep", startTime = "15:00")
        val localDay = DayRecord("2026-08-31", sleeps = listOf(older), deletedSleepIds = setOf(deleted.id))
        val remoteDay = DayRecord("2026-08-31", sleeps = listOf(newer, deleted))

        val merged = GitHubSync().merge(
            AppData(days = mapOf(localDay.date to localDay)),
            AppData(days = mapOf(remoteDay.date to remoteDay)),
        ).days.getValue("2026-08-31")

        assertEquals(listOf("sleep"), merged.sleeps.map { it.id })
        assertEquals("13:30", merged.sleeps.single().endTime)
        assertTrue(deleted.id in merged.deletedSleepIds)
    }
}

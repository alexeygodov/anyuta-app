package ru.family.rasti.sleep

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.family.rasti.data.AppData
import ru.family.rasti.data.DayRecord
import ru.family.rasti.data.SleepEntry
import java.time.LocalDate
import java.time.LocalDateTime

class SleepLogicTest {
    @Test
    fun duration_crossingMidnight_isCalculatedFromBothDates() {
        val sleep = SleepEntry(startTime = "23:30", endDate = "2026-08-31", endTime = "01:10")

        assertEquals(100L, sleepDurationMinutes(LocalDate.parse("2026-08-30"), sleep))
    }

    @Test
    fun segments_crossingMidnight_areClippedForEachDay() {
        val sleep = SleepEntry(startTime = "23:30", endDate = "2026-08-31", endTime = "01:10")
        val data = AppData(days = mapOf("2026-08-30" to DayRecord("2026-08-30", sleeps = listOf(sleep))))

        val first = sleepsForDate(data, LocalDate.parse("2026-08-30"))
        val second = sleepsForDate(data, LocalDate.parse("2026-08-31"))

        assertEquals(1410, first.single().startMinute)
        assertEquals(1440, first.single().endMinute)
        assertEquals(0, second.single().startMinute)
        assertEquals(70, second.single().endMinute)
    }

    @Test
    fun activeAndLastCompleted_areSelectedIndependently() {
        val completed = SleepEntry(startTime = "10:00", endDate = "2026-08-31", endTime = "11:00")
        val active = SleepEntry(startTime = "12:00")
        val data = AppData(
            days = mapOf("2026-08-31" to DayRecord("2026-08-31", sleeps = listOf(completed, active))),
        )

        assertEquals(active.id, activeSleep(data)?.entry?.id)
        assertEquals(
            completed.id,
            lastCompletedSleep(data, LocalDateTime.parse("2026-08-31T13:00"))?.entry?.id,
        )
        assertTrue(sleepDurationMinutes(LocalDate.parse("2026-08-31"), active, LocalDateTime.parse("2026-08-31T13:00")) == 60L)
        assertFalse(formatSleepDuration(60).isBlank())
        assertNull(sleepEnd(active))
    }
}

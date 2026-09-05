package ru.family.rasti.sleep

import org.junit.Assert.*
import org.junit.Test
import ru.family.rasti.data.*
import java.time.LocalDate
import java.time.LocalDateTime

class SleepAwakeTest {
    private val date = LocalDate.parse("2026-09-05")
    private fun data(vararg sleeps: SleepEntry) = AppData(days = mapOf(date.toString() to DayRecord(date.toString(), sleeps = sleeps.toList())))

    @Test fun overlappingSleepCountedOnce() {
        val data = data(SleepEntry(startTime = "08:00", endDate = date.toString(), endTime = "10:00"),
            SleepEntry(startTime = "09:00", endDate = date.toString(), endTime = "11:00"))
        assertEquals(180L, sleepMinutesForDate(data, date, date.atTime(12, 0)))
        assertEquals(listOf(660 to 720), awakeSegmentsForDate(data, date, date.atTime(12, 0)))
    }

    @Test fun wakeGapAcrossMidnightIsClippedToEachDay() {
        val data = AppData(days = mapOf("2026-09-04" to DayRecord("2026-09-04", sleeps = listOf(
            SleepEntry(startTime = "21:00", endDate = "2026-09-04", endTime = "23:30"))),
            date.toString() to DayRecord(date.toString(), sleeps = listOf(
                SleepEntry(startTime = "00:30", endDate = date.toString(), endTime = "08:00")))))
        assertEquals(listOf(1410 to 1440), awakeSegmentsForDate(data, date.minusDays(1), date.atTime(8, 0)))
        assertEquals(listOf(0 to 30), awakeSegmentsForDate(data, date, date.atTime(8, 0)))
    }

    @Test fun noRecordsOrStaleDiaryNeverSuggestAllDayWakefulness() {
        assertNull(awakeMinutes(AppData(), date.atTime(12, 0)))
        val data = data(SleepEntry(startTime = "00:00", endDate = date.toString(), endTime = "01:00"))
        assertNull(awakeMinutes(data, date.atTime(23, 0)))
        assertTrue(awakeSegmentsForDate(data, date, date.atTime(23, 0)).isEmpty())
    }

    @Test fun redIntensityStartsAfterThresholdAndCanBeDisabled() {
        assertEquals(0f, wakeAttention(120, 120), .001f)
        assertEquals(.5f, wakeAttention(135, 120), .001f)
        assertEquals(1f, wakeAttention(180, 120), .001f)
        assertEquals(0f, wakeAttention(180, 0), .001f)
        assertEquals(0f, wakeAttention(null, 120), .001f)
    }

    @Test fun activeSleepStopsWakeCounterAndFutureSleepDoesNotStartIt() {
        val now = date.atTime(12, 0)
        assertNull(awakeMinutes(data(SleepEntry(startTime = "11:00")), now))
        assertNull(activeSleep(data(SleepEntry(startTime = "13:00")), now))
    }
}

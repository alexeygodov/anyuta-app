package ru.family.rasti.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.family.rasti.data.AppData
import ru.family.rasti.data.DayRecord
import ru.family.rasti.data.SleepEntry
import java.time.LocalDate
import java.time.LocalDateTime

class WeeklySleepChartTest {
    @Test
    fun sleepAcrossMidnight_isSplitBetweenDailyColumns() {
        val sleep = SleepEntry(startTime = "23:30", endDate = "2026-09-02", endTime = "01:10")
        val data = AppData(
            days = mapOf("2026-09-01" to DayRecord("2026-09-01", sleeps = listOf(sleep))),
        )

        val points = weeklySleepSummaries(
            data = data,
            endDate = LocalDate.parse("2026-09-02"),
            now = LocalDateTime.parse("2026-09-02T12:00"),
        )

        assertEquals(30L, points[5].totalMinutes)
        assertEquals(70L, points[6].totalMinutes)
    }
}

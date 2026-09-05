package ru.family.rasti.ui

import org.junit.Assert.*
import org.junit.Test
import ru.family.rasti.data.*
import java.time.LocalDate

class MeasuredGrowthTest {
    private val today = LocalDate.parse("2026-09-05")

    @Test fun dailyMeasurementsAreGroupedAndFutureMeasurementsIgnored() {
        val data = AppData(days = listOf(
            DayRecord("2026-08-20", measurement = Measurement(weightKg = 5.0)),
            DayRecord("2026-08-21", measurement = Measurement(weightKg = 5.2)),
            DayRecord("2026-08-27", measurement = Measurement(weightKg = 5.21)),
            DayRecord("2026-09-06", measurement = Measurement(weightKg = 6.0)),
        ).associateBy { it.date })
        val points = measuredGrowthIntervals(data, true, today)
        assertEquals(1, points.size)
        assertEquals(210.0, points.single().change, .001)
        assertEquals(30.0, points.single().perDay, .001)
    }

    @Test fun heightPreservesNegativeChangesAndDoesNotUseWeight() {
        val data = AppData(days = listOf(
            DayRecord("2026-08-01", measurement = Measurement(heightCm = 60.0, weightKg = 5.0)),
            DayRecord("2026-08-31", measurement = Measurement(heightCm = 59.0, weightKg = 6.0)),
        ).associateBy { it.date })
        assertEquals(-1.0, measuredGrowthIntervals(data, false, today).single().change, .001)
    }
}

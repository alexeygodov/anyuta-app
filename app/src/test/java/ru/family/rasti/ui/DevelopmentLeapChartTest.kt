package ru.family.rasti.ui

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DevelopmentLeapChartTest {
    @Test
    fun leapIntensity_peaks_on_planned_date_and_fades_after_two_weeks() {
        val reference = LocalDate.of(2026, 1, 1)
        val firstPeak = reference.plusWeeks(5)

        assertEquals(1f, leapIntensity(reference, firstPeak), 0.001f)
        assertEquals(0.5f, leapIntensity(reference, firstPeak.plusDays(7)), 0.001f)
        assertEquals(0f, leapIntensity(reference, firstPeak.minusDays(14)), 0.001f)
    }
}

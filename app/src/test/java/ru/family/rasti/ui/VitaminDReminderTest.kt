package ru.family.rasti.ui

import org.junit.Assert.*
import org.junit.Test
import ru.family.rasti.data.VitaminEntry
import java.time.*

class VitaminDReminderTest {
    private val date = LocalDate.of(2026, 9, 14)

    @Test fun pulseStopsExactlyAtTwoAndOnlyForUnmarkedToday() {
        listOf("13:59:59" to true, "14:00:00" to false, "14:00:01" to false).forEach { (time, expected) ->
            val now = date.atTime(LocalTime.parse(time))
            assertEquals(expected, shouldPulseVitaminD(date, false, now))
            assertFalse(shouldPulseVitaminD(date, true, now))
            assertFalse(shouldPulseVitaminD(date.minusDays(1), false, now))
            assertFalse(shouldPulseVitaminD(date.plusDays(1), false, now))
        }
    }

    @Test fun differentVitaminDoesNotCountAsVitaminD() {
        fun vitamin(name: String) = VitaminEntry(id = name, time = "09:00", name = name,
            amount = 1.0, unit = "капля", updatedAt = "2026-09-14T09:00:00Z")
        assertFalse(isVitaminD(vitamin("Витамин C")))
        listOf("Витамин D", "Витамин Д", "D3").forEach { assertTrue(isVitaminD(vitamin(it))) }
        assertTrue(shouldPulseVitaminD(date, listOf(vitamin("Витамин C")).any(::isVitaminD), date.atTime(9, 0)))
    }

    @Test fun midnightAndZoneChangeUseTheNewLocalDay() {
        val instant = Instant.parse("2026-09-14T22:00:00Z")
        val utc = LocalDateTime.ofInstant(instant, ZoneOffset.UTC)
        val east = LocalDateTime.ofInstant(instant, ZoneOffset.ofHours(4))
        assertFalse(shouldPulseVitaminD(date, false, utc))
        assertFalse(shouldPulseVitaminD(date, false, east))
        assertTrue(shouldPulseVitaminD(date.plusDays(1), false, east))
        assertTrue(shouldPulseVitaminD(date.plusDays(1), false, date.plusDays(1).atStartOfDay()))
    }

    @Test fun nextWakeupIsCutoffOrNextMidnightIncludingDst() {
        val zone = ZoneId.of("Europe/Berlin")
        val before = date.atTime(13, 59, 59).atZone(zone)
        assertEquals(Duration.ofSeconds(1), Duration.between(before, nextReminderBoundary(before)))
        val cutoff = date.atTime(14, 0).atZone(zone)
        assertEquals(date.plusDays(1).atStartOfDay(zone), nextReminderBoundary(cutoff))
        val dst = LocalDate.of(2026, 3, 29).atStartOfDay(zone)
        assertEquals(Duration.ofHours(13), Duration.between(dst, nextReminderBoundary(dst)))
    }
}

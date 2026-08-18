package ru.family.rasti.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimeInputTest {
    @Test
    fun normalizeTimeInput_accepts_short_hours_and_formats_time() {
        assertEquals("09:05", normalizeTimeInput("9:5"))
        assertEquals("23:59", normalizeTimeInput("23:59"))
    }

    @Test
    fun normalizeTimeInput_rejects_invalid_values() {
        assertNull(normalizeTimeInput("24:00"))
        assertNull(normalizeTimeInput("12:60"))
        assertNull(normalizeTimeInput("без времени"))
    }

    @Test
    fun adjustTimeInput_moves_in_fifteen_minute_steps() {
        assertEquals("10:15", adjustTimeInput("10:00", 15))
        assertEquals("23:50", adjustTimeInput("00:05", -15))
    }
}

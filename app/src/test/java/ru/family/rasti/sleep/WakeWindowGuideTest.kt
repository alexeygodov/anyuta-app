package ru.family.rasti.sleep

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class WakeWindowGuideTest {
    private val birth = LocalDate.parse("2026-01-10")
    @Test fun ageUsesCalendarMonthsAndBoundaries() {
        val cases = listOf(0 to (30 to 60), 1 to (60 to 120), 3 to (75 to 150), 4 to (75 to 150), 5 to (120 to 240), 7 to (150 to 270), 10 to (180 to 360), 12 to (180 to 360))
        cases.forEach { (age, range) ->
            val result = wakeWindowGuide(birth.toString(), birth.plusMonths(age.toLong()))!!
            assertEquals(range.first, result.minimumMinutes)
            assertEquals(range.second, result.maximumMinutes)
            assertEquals(0, result.suggestedMinutes % 5)
        }
        assertEquals(60, wakeWindowGuide(birth.toString(), birth.plusMonths(1).minusDays(1))!!.maximumMinutes)
        assertNull(wakeWindowGuide(birth.toString(), birth.minusDays(1)))
        assertNull(wakeWindowGuide(birth.toString(), birth.plusMonths(13)))
        assertNull(wakeWindowGuide("bad", birth))
    }
}

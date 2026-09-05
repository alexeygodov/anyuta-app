package ru.family.rasti.ui

import org.junit.Assert.*
import org.junit.Test
import ru.family.rasti.data.ChildProfile
import java.time.LocalDate

class DevelopmentCalendarTest {
    @Test fun calendarUsesDueDateAndCorrectModelWeeks() {
        val profile = ChildProfile(birthDate = "2026-01-01", dueDate = "2026-01-20")
        val reference = leapReference(profile)!!
        assertEquals(LocalDate.parse("2026-01-20"), reference)
        assertEquals(4, nearbyCalendarLeap(reference, reference.plusWeeks(19))!!.number)
        assertNull(nearbyCalendarLeap(reference, reference.plusWeeks(90)))
        assertEquals(listOf(5, 8, 12, 19, 26, 37, 46, 55, 64, 75), calendarLeaps.map { it.week })
    }
    @Test fun arbitraryHighlightWindowIsBounded() {
        val reference = LocalDate.parse("2026-01-01")
        assertNotNull(nearbyCalendarLeap(reference, reference.plusWeeks(19).plusDays(7)))
        assertNull(nearbyCalendarLeap(reference, reference.plusWeeks(19).plusDays(8)))
        assertNull(leapReference(ChildProfile(birthDate = "bad")))
    }
}

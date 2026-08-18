package ru.family.rasti.feeding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.family.rasti.data.AppData
import ru.family.rasti.data.ChildProfile
import ru.family.rasti.data.DayRecord
import ru.family.rasti.data.Measurement
import java.time.LocalDate

class FeedingGuideTest {
    @Test
    fun calculate_uses_latest_weight_before_selected_date() {
        val data = AppData(
            profile = ChildProfile(birthDate = "2026-01-01"),
            days = mapOf(
                "2026-03-01" to DayRecord(
                    date = "2026-03-01",
                    measurement = Measurement(weightKg = 6.0),
                ),
                "2026-03-20" to DayRecord(
                    date = "2026-03-20",
                    measurement = Measurement(weightKg = 7.0),
                ),
            ),
        )

        val guide = FeedingGuide.calculate(data, LocalDate.parse("2026-03-10")).guide!!

        assertEquals(900, guide.minimumMl)
        assertEquals(1050, guide.targetMl)
        assertEquals(1200, guide.maximumMl)
        assertEquals(LocalDate.parse("2026-03-01"), guide.weightDate)
    }

    @Test
    fun calculate_is_unavailable_after_six_months() {
        val data = AppData(
            profile = ChildProfile(birthDate = "2025-01-01"),
            days = mapOf(
                "2025-08-01" to DayRecord("2025-08-01", measurement = Measurement(weightKg = 8.0)),
            ),
        )

        assertNull(FeedingGuide.calculate(data, LocalDate.parse("2025-08-01")).guide)
    }
}

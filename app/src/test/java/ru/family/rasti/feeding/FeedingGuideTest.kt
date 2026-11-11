package ru.family.rasti.feeding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun calculate_continues_by_weight_from_six_months_until_first_birthday() {
        val data = dataWithWeight()
        for (date in listOf("2026-07-01", "2026-07-02", "2026-08-01", "2026-10-31", "2026-11-01", "2026-12-31")) {
            val result = FeedingGuide.calculate(data, LocalDate.parse(date))
            val guide = requireNotNull(result.guide)
            assertEquals(date, 720, guide.minimumMl)
            assertEquals(date, 840, guide.targetMl)
            assertEquals(date, 960, guide.maximumMl)
            assertTrue(guide.isComplementaryFeeding)
            assertEquals(8.0, guide.weightKg, 0.0)
            assertEquals(LocalDate.parse("2026-06-01"), guide.weightDate)
            assertTrue(result.explanation.contains("прикорм"))
            assertEquals("https://www.betterhealth.vic.gov.au/health/healthyliving/bottle-feeding-nutrition-and-safety", result.sourceUrl)
        }
    }

    @Test
    fun calculate_stops_at_first_birthday() {
        val data = dataWithWeight()
        assertNull(FeedingGuide.calculate(data, LocalDate.parse("2027-01-01")).guide)
        assertNull(FeedingGuide.calculate(data, LocalDate.parse("2027-01-02")).guide)
    }

    @Test
    fun calculate_preserves_weight_based_guide_until_day_before_six_months() {
        val data = AppData(
            profile = ChildProfile(birthDate = "2026-01-01"),
            days = mapOf(
                "2026-06-01" to DayRecord("2026-06-01", measurement = Measurement(weightKg = 8.0)),
            ),
        )
        val guide = FeedingGuide.calculate(data, LocalDate.parse("2026-06-30")).guide!!
        assertEquals(1200, guide.minimumMl)
        assertEquals(1400, guide.targetMl)
        assertEquals(1600, guide.maximumMl)
        assertEquals(840, FeedingGuide.calculate(data, LocalDate.parse("2026-07-01")).guide!!.targetMl)
    }

    @Test
    fun calculate_uses_calendar_months_even_for_end_of_month_and_leap_day_births() {
        val data = dataWithWeight(birthDate = "2024-02-29", weightDate = "2024-08-01")
        assertEquals(1400, FeedingGuide.calculate(data, LocalDate.parse("2024-08-28")).guide!!.targetMl)
        assertEquals(840, FeedingGuide.calculate(data, LocalDate.parse("2024-08-29")).guide!!.targetMl)
        assertEquals(840, FeedingGuide.calculate(data, LocalDate.parse("2024-12-29")).guide!!.targetMl)
        assertNull(FeedingGuide.calculate(data, LocalDate.parse("2025-02-28")).guide)
    }

    @Test
    fun calculate_handles_missing_birth_date_first_week_and_missing_weight() {
        assertNull(FeedingGuide.calculate(AppData(profile = ChildProfile(birthDate = "")), LocalDate.parse("2026-08-01")).guide)
        val data = AppData(profile = ChildProfile(birthDate = "2026-01-01"))
        for (date in listOf("2025-12-31", "2026-01-01", "2026-01-07", "2026-01-08", "2026-06-30", "2026-07-01", "2026-11-01")) {
            assertNull(FeedingGuide.calculate(data, LocalDate.parse(date)).guide)
        }
    }

    @Test
    fun after_six_months_latest_weight_changes_reference_without_using_future_measurements() {
        val initial = dataWithWeight()
        val later = DayRecord("2026-09-01", measurement = Measurement(weightKg = 9.0))
        val data = initial.copy(days = initial.days + (later.date to later))
        assertEquals(840, FeedingGuide.calculate(data, LocalDate.parse("2026-08-31")).guide!!.targetMl)
        val guide = FeedingGuide.calculate(data, LocalDate.parse("2026-09-01")).guide!!
        assertEquals(810, guide.minimumMl)
        assertEquals(950, guide.targetMl)
        assertEquals(1080, guide.maximumMl)
        assertEquals(LocalDate.parse(later.date), guide.weightDate)
    }

    @Test
    fun after_six_months_invalid_weights_do_not_replace_last_valid_measurement() {
        val initial = dataWithWeight()
        val invalidDays = listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY).mapIndexed { index, weight ->
            val date = LocalDate.parse("2026-08-01").plusDays(index.toLong()).toString()
            date to DayRecord(date, measurement = Measurement(weightKg = weight))
        }.toMap()
        assertEquals(840, FeedingGuide.calculate(initial.copy(days = initial.days + invalidDays), LocalDate.parse("2026-08-05")).guide!!.targetMl)
        assertNull(FeedingGuide.calculate(initial.copy(days = invalidDays), LocalDate.parse("2026-08-05")).guide)
    }

    private fun dataWithWeight(birthDate: String = "2026-01-01", weightDate: String = "2026-06-01") = AppData(
        profile = ChildProfile(birthDate = birthDate),
        days = mapOf(weightDate to DayRecord(weightDate, measurement = Measurement(weightKg = 8.0))),
    )
}

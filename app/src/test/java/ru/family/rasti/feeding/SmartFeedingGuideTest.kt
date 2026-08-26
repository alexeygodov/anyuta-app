package ru.family.rasti.feeding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.family.rasti.data.AppData
import ru.family.rasti.data.DayRecord
import ru.family.rasti.data.FoodEntry
import java.time.LocalDate
import java.time.LocalDateTime

class SmartFeedingGuideTest {
    private val date = LocalDate.of(2026, 8, 26)
    private val guide = MilkGuide(
        weightKg = 5.3,
        weightDate = date.minusDays(2),
        minimumMl = 800,
        targetMl = 930,
        maximumMl = 1060,
    )

    @Test
    fun learns_usual_amount_and_interval_and_limits_catch_up() {
        val data = AppData(
            days = mapOf(
                date.toString() to DayRecord(
                    date = date.toString(),
                    food = listOf(
                        feeding("08:00", 120.0),
                        feeding("12:00", 130.0),
                        feeding("16:00", 140.0),
                    ),
                ),
            ),
        )

        val result = SmartFeedingGuide.calculate(
            data = data,
            date = date,
            guide = guide,
            now = LocalDateTime.of(2026, 8, 26, 20, 0),
        )!!

        assertEquals(130, result.usualAmountMl)
        assertEquals(240, result.usualIntervalMinutes)
        assertEquals(150, result.amountMl)
        assertEquals(540, result.remainingToTargetMl)
        assertEquals(FeedingMoment.USUAL_TIME, result.moment)
    }

    @Test
    fun falls_back_to_daily_target_when_there_is_no_history() {
        val result = SmartFeedingGuide.calculate(
            data = AppData(),
            date = date,
            guide = guide,
            now = LocalDateTime.of(2026, 8, 26, 0, 10),
        )!!

        assertEquals(135, result.usualAmountMl)
        assertEquals(135, result.amountMl)
        assertEquals(210, result.usualIntervalMinutes)
        assertEquals(FeedingMoment.NO_HISTORY, result.moment)
    }

    @Test
    fun full_recent_feeding_reduces_current_portion_to_zero() {
        val data = AppData(
            days = mapOf(
                date.toString() to DayRecord(
                    date = date.toString(),
                    food = listOf(
                        feeding("08:00", 130.0),
                        feeding("12:00", 130.0),
                        feeding("16:00", 130.0),
                    ),
                ),
            ),
        )

        val result = SmartFeedingGuide.calculate(
            data = data,
            date = date,
            guide = guide,
            now = LocalDateTime.of(2026, 8, 26, 16, 0),
        )!!

        assertEquals(0, result.amountMl)
        assertEquals(130, result.recentIntakeMl)
    }

    @Test
    fun small_recent_feeding_leaves_only_a_partial_portion() {
        val data = AppData(
            days = mapOf(
                date.toString() to DayRecord(
                    date = date.toString(),
                    food = listOf(
                        feeding("08:00", 130.0),
                        feeding("12:00", 130.0),
                        feeding("16:00", 50.0),
                    ),
                ),
            ),
        )

        val result = SmartFeedingGuide.calculate(
            data = data,
            date = date,
            guide = guide,
            now = LocalDateTime.of(2026, 8, 26, 16, 0),
        )!!

        assertEquals(90, result.amountMl)
        assertEquals(50, result.recentIntakeMl)
    }

    @Test
    fun is_only_shown_for_today() {
        assertNull(
            SmartFeedingGuide.calculate(
                data = AppData(),
                date = date.minusDays(1),
                guide = guide,
                now = LocalDateTime.of(2026, 8, 26, 12, 0),
            ),
        )
    }

    private fun feeding(time: String, amount: Double) =
        FoodEntry(time = time, name = "Молоко", amount = amount, unit = "мл")
}

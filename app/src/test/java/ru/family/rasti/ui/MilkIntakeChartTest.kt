package ru.family.rasti.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.family.rasti.data.DayRecord
import ru.family.rasti.data.FoodEntry
import java.time.LocalDate
import java.time.LocalDateTime

class MilkIntakeChartTest {

    @Test
    fun cumulative_scale_keeps_goal_range_with_headroom() {
        assertEquals(1200.0, feedingCumulativeScaleMaximum(totalMl = 495.0, guideMaximumMl = 1060), 0.0)
    }

    @Test
    fun cumulative_scale_expands_when_total_exceeds_guide() {
        assertEquals(1500.0, feedingCumulativeScaleMaximum(totalMl = 1300.0, guideMaximumMl = 1060), 0.0)
    }

    @Test
    fun goal_progress_uses_target_and_may_exceed_one_hundred_percent() {
        assertEquals(53, feedingGoalProgressPercent(totalMl = 495.0, targetMl = 930))
        assertEquals(108, feedingGoalProgressPercent(totalMl = 1000.0, targetMl = 930))
    }

    @Test
    fun nearby_labels_are_grouped_but_distant_feeds_stay_separate() {
        assertEquals(
            listOf(0..1, 2..3, 4..4),
            feedingLabelGroupRanges(listOf(100, 125, 300, 330, 700), thresholdMinutes = 45),
        )
        assertEquals(
            listOf(0..0, 1..1, 2..2),
            feedingLabelGroupRanges(listOf(100, 125, 300), thresholdMinutes = 20),
        )
    }

    @Test
    fun feeding_count_has_correct_russian_word_form() {
        assertEquals("1 кормление", feedingCountLabel(1))
        assertEquals("3 кормления", feedingCountLabel(3))
        assertEquals("5 кормлений", feedingCountLabel(5))
        assertEquals("11 кормлений", feedingCountLabel(11))
        assertEquals("21 кормление", feedingCountLabel(21))
    }

    @Test
    fun last_feeding_carries_over_from_previous_day() {
        val yesterday = DayRecord(
            date = "2026-08-25",
            food = listOf(FoodEntry(time = "23:50", name = "Молоко", amount = 125.0, unit = "мл")),
        )

        val result = lastFeedingInfo(
            date = LocalDate.of(2026, 8, 26),
            days = listOf(yesterday),
            now = LocalDateTime.of(2026, 8, 26, 0, 10),
        )

        assertEquals(20L, result?.minutesAgo)
        assertEquals("20 мин назад, Молоко 125 мл в 23:50", result?.text)
    }

    @Test
    fun last_feeding_ignores_entries_later_than_current_time() {
        val today = DayRecord(
            date = "2026-08-26",
            food = listOf(FoodEntry(time = "12:00", name = "Смесь", amount = 130.0, unit = "мл")),
        )
        val yesterday = DayRecord(
            date = "2026-08-25",
            food = listOf(FoodEntry(time = "23:40", name = "Молоко", amount = 120.0, unit = "мл")),
        )

        val result = lastFeedingInfo(
            date = LocalDate.of(2026, 8, 26),
            days = listOf(today, yesterday),
            now = LocalDateTime.of(2026, 8, 26, 0, 10),
        )

        assertEquals(30L, result?.minutesAgo)
        assertEquals("30 мин назад, Молоко 120 мл в 23:40", result?.text)
    }
}

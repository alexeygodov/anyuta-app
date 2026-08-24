package ru.family.rasti.ui

import org.junit.Assert.assertEquals
import org.junit.Test

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
    fun feeding_count_has_correct_russian_word_form() {
        assertEquals("1 кормление", feedingCountLabel(1))
        assertEquals("3 кормления", feedingCountLabel(3))
        assertEquals("5 кормлений", feedingCountLabel(5))
        assertEquals("11 кормлений", feedingCountLabel(11))
        assertEquals("21 кормление", feedingCountLabel(21))
    }
}

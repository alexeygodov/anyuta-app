package ru.family.rasti.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class MilkIntakeChartTest {

    @Test
    fun daily_scale_uses_guide_maximum_while_total_is_inside_range() {
        assertEquals(1060.0, feedingDailyScaleMaximum(totalMl = 495.0, guideMaximumMl = 1060), 0.0)
    }

    @Test
    fun daily_scale_expands_when_total_exceeds_guide() {
        assertEquals(1300.0, feedingDailyScaleMaximum(totalMl = 1250.0, guideMaximumMl = 1060), 0.0)
    }

    @Test
    fun event_scale_uses_readable_fifty_ml_steps() {
        assertEquals(150, feedingEventScaleMaximum(maximumFeedingMl = 135.0))
        assertEquals(100, feedingEventScaleMaximum(maximumFeedingMl = 60.0))
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

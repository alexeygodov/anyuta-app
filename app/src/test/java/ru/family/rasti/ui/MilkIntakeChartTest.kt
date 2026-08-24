package ru.family.rasti.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MilkIntakeChartTest {

    @Test
    fun scale_includes_daily_guide_with_headroom() {
        assertEquals(1200f, feedingChartScaleMaximum(totalMl = 495.0, guideMaximumMl = 1060))
    }

    @Test
    fun scale_expands_when_actual_total_exceeds_guide() {
        assertTrue(feedingChartScaleMaximum(totalMl = 1300.0, guideMaximumMl = 1060) >= 1500f)
    }

    @Test
    fun progress_uses_target_and_may_exceed_one_hundred_percent() {
        assertEquals(53, feedingProgressPercent(totalMl = 495.0, targetMl = 930))
        assertEquals(108, feedingProgressPercent(totalMl = 1000.0, targetMl = 930))
    }

    @Test
    fun progress_is_absent_without_valid_target() {
        assertNull(feedingProgressPercent(totalMl = 495.0, targetMl = null))
        assertNull(feedingProgressPercent(totalMl = 495.0, targetMl = 0))
    }
}

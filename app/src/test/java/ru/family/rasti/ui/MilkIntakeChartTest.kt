package ru.family.rasti.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MilkIntakeChartTest {

    @Test
    fun labels_all_points_when_day_is_not_dense() {
        assertEquals((0..5).toList(), pointLabelIndexes(pointCount = 6))
    }

    @Test
    fun labels_are_thinned_and_keep_last_point_when_day_is_dense() {
        val indexes = pointLabelIndexes(pointCount = 17)

        assertTrue(indexes.size <= 8)
        assertEquals(0, indexes.first())
        assertEquals(16, indexes.last())
        assertEquals(indexes.distinct(), indexes)
    }

    @Test
    fun labels_are_empty_without_points() {
        assertEquals(emptyList<Int>(), pointLabelIndexes(pointCount = 0))
    }
}

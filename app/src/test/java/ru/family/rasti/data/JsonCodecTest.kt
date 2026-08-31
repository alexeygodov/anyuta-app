package ru.family.rasti.data

import org.junit.Assert.assertEquals
import org.junit.Test

class JsonCodecTest {
    @Test
    fun parseLegacyVitaminDose_migrates_text_to_amount_and_unit() {
        val parsed = parseLegacyVitaminDose("2 капли")

        assertEquals(2.0, parsed.first, 0.0)
        assertEquals("капля", parsed.second)
    }

    @Test
    fun formatVitaminDose_inflects_drop_unit() {
        assertEquals("1 капля", formatVitaminDose(1.0, "капля"))
        assertEquals("2 капли", formatVitaminDose(2.0, "капля"))
        assertEquals("5 капель", formatVitaminDose(5.0, "капля"))
    }

    @Test
    fun sleep_roundTrip_preservesOpenAndCompletedIntervals() {
        val open = SleepEntry(id = "open", startTime = "12:00")
        val completed = SleepEntry(
            id = "completed",
            startTime = "23:30",
            endDate = "2026-08-31",
            endTime = "01:10",
        )
        val source = DayRecord(
            date = "2026-08-30",
            sleeps = listOf(open, completed),
            deletedSleepIds = setOf("deleted"),
        )

        val restored = JsonCodec.decodeDay(JsonCodec.encodeDay(source))

        assertEquals(source.sleeps, restored.sleeps)
        assertEquals(setOf("deleted"), restored.deletedSleepIds)
    }
}

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

}

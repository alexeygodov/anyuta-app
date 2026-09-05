package ru.family.rasti.data

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FussinessCodecTest {
    @Test fun observationsAndDeletionTimestampSurviveRoundTrip() {
        val day = DayRecord("2026-09-05", fussiness = 2, fussinessUpdatedAt = "2026-09-05T12:00:00Z")
        assertEquals(day, JsonCodec.decodeDay(JsonCodec.encodeDay(day)))
        val removed = day.copy(fussiness = null)
        assertEquals(removed, JsonCodec.decodeDay(JsonCodec.encodeDay(removed)))
        assertNull(JsonCodec.decodeDay("""{"date":"2026-09-05"}""").fussiness)
        assertNull(JsonCodec.decodeDay("""{"date":"2026-09-05","fussiness":9}""").fussiness)
    }
}

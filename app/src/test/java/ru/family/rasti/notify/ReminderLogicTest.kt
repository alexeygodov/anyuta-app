package ru.family.rasti.notify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.family.rasti.data.AppData
import ru.family.rasti.data.DayRecord
import ru.family.rasti.data.FoodEntry
import ru.family.rasti.data.VitaminEntry
import java.time.LocalDate
import java.time.LocalDateTime

class ReminderLogicTest {

    private val today: LocalDate = LocalDate.of(2026, 8, 21)

    private fun milk(time: String, name: String = "Молоко") =
        FoodEntry(time = time, name = name, amount = 120.0, unit = "мл")

    private fun dataWith(vararg days: DayRecord) = AppData(days = days.associateBy { it.date })

    @Test
    fun feedingReminder_due_after_three_hours() {
        val data = dataWith(DayRecord(date = today.toString(), food = listOf(milk("09:00"))))
        val now = LocalDateTime.of(today, java.time.LocalTime.of(12, 0))
        assertEquals(LocalDateTime.of(today, java.time.LocalTime.of(9, 0)), feedingReminderDue(data, now))
    }

    @Test
    fun feedingReminder_not_due_before_three_hours() {
        val data = dataWith(DayRecord(date = today.toString(), food = listOf(milk("09:30"))))
        val now = LocalDateTime.of(today, java.time.LocalTime.of(12, 0))
        assertNull(feedingReminderDue(data, now))
    }

    @Test
    fun feedingReminder_ignores_future_entries() {
        val data = dataWith(DayRecord(date = today.toString(), food = listOf(milk("23:00"))))
        val now = LocalDateTime.of(today, java.time.LocalTime.of(12, 0))
        assertNull(feedingReminderDue(data, now))
    }

    @Test
    fun feedingReminder_uses_yesterday_evening_feeding() {
        val yesterday = today.minusDays(1)
        val data = dataWith(DayRecord(date = yesterday.toString(), food = listOf(milk("23:30"))))
        val now = LocalDateTime.of(today, java.time.LocalTime.of(2, 45))
        assertEquals(LocalDateTime.of(yesterday, java.time.LocalTime.of(23, 30)), feedingReminderDue(data, now))
    }

    @Test
    fun feedingReminder_ignores_non_milk_entries() {
        val data = dataWith(
            DayRecord(
                date = today.toString(),
                food = listOf(
                    FoodEntry(time = "08:00", name = "Каша", amount = 150.0, unit = "г"),
                    FoodEntry(time = "08:30", name = "Сок", amount = 100.0, unit = "мл"),
                ),
            ),
        )
        val now = LocalDateTime.of(today, java.time.LocalTime.of(12, 0))
        assertNull(feedingReminderDue(data, now))
    }

    @Test
    fun vitaminReminder_due_after_noon_without_vitamin() {
        val data = dataWith(DayRecord(date = today.toString()))
        val now = LocalDateTime.of(today, java.time.LocalTime.of(12, 0))
        assertTrue(vitaminReminderDue(data, now))
    }

    @Test
    fun vitaminReminder_not_due_before_noon() {
        val data = dataWith(DayRecord(date = today.toString()))
        val now = LocalDateTime.of(today, java.time.LocalTime.of(11, 59))
        assertFalse(vitaminReminderDue(data, now))
    }

    @Test
    fun vitaminReminder_not_due_when_vitamin_d_taken() {
        val vitamins = listOf(
            VitaminEntry(time = "09:00", name = "Витамин D", amount = 2.0, unit = "капля"),
        )
        val data = dataWith(DayRecord(date = today.toString(), vitamins = vitamins))
        val now = LocalDateTime.of(today, java.time.LocalTime.of(13, 0))
        assertFalse(vitaminReminderDue(data, now))
    }

    @Test
    fun vitaminReminder_recognizes_d3_and_russian_d() {
        listOf("D3", "Аквадетрим D3", "витамин д").forEach { name ->
            val data = dataWith(
                DayRecord(
                    date = today.toString(),
                    vitamins = listOf(VitaminEntry(time = "09:00", name = name, amount = 1.0, unit = "капля")),
                ),
            )
            val now = LocalDateTime.of(today, java.time.LocalTime.of(13, 0))
            assertFalse("expected not due for $name", vitaminReminderDue(data, now))
        }
    }

    @Test
    fun vitaminReminder_due_when_only_other_vitamins_taken() {
        val data = dataWith(
            DayRecord(
                date = today.toString(),
                vitamins = listOf(VitaminEntry(time = "09:00", name = "Омега-3", amount = 1.0, unit = "капсула")),
            ),
        )
        val now = LocalDateTime.of(today, java.time.LocalTime.of(13, 0))
        assertTrue(vitaminReminderDue(data, now))
    }

    @Test
    fun formatElapsed_formats_hours_and_minutes() {
        assertEquals("3 ч", formatElapsed(180))
        assertEquals("3 ч 5 мин", formatElapsed(185))
        assertEquals("45 мин", formatElapsed(45))
    }
}

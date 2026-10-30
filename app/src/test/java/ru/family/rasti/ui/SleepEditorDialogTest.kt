package ru.family.rasti.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.family.rasti.data.SleepEntry
import ru.family.rasti.ui.theme.RastiTheme
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SleepEditorDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test fun switchingBetweenStartAndEndPreservesSavedTimes() {
        verifySwitching("10:15", "12:45", overnight = false)
    }

    @Test fun switchingAcrossMidnightPreservesSavedTimesAndDates() {
        verifySwitching("23:55", "00:05", overnight = true)
    }

    @Test fun switchingWhileWakeWheelIsMovingDoesNotOverwriteStart() {
        val date = LocalDate.of(2026, 9, 12)
        var savedStart: String? = null
        compose.setContent {
            RastiTheme {
                SleepEditorDialog(
                    title = "Изменить сон",
                    initialStartDate = date,
                    initial = SleepEntry(startTime = "01:15", endDate = date.plusDays(1).toString(), endTime = "12:45"),
                    requireEnd = true,
                    onDismiss = {},
                    onSave = { _, time, _, _ -> savedStart = time },
                )
            }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.onAllNodes(hasScrollToIndexAction()).onFirst().performTouchInput { swipeUp(durationMillis = 100) }
        compose.onNodeWithText("Начало").performClick()
        compose.mainClock.advanceTimeBy(5_000)
        compose.mainClock.autoAdvance = true
        compose.onNodeWithText("Сохранить").performClick()
        compose.runOnIdle { assertEquals("01:15", savedStart) }
    }

    @Test fun savingWaitsUntilWheelStops() {
        compose.setContent {
            RastiTheme {
                SleepEditorDialog(
                    title = "Ребёнок уснул",
                    initialStartDate = LocalDate.of(2026, 9, 12),
                    initial = SleepEntry(startTime = "10:15"),
                    requireEnd = false,
                    onDismiss = {},
                    onSave = { _, _, _, _ -> },
                )
            }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.onAllNodes(hasScrollToIndexAction()).onFirst().performTouchInput { swipeUp(durationMillis = 100) }
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("Уснула").assertIsNotEnabled()
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNodeWithText("Уснула").assertIsEnabled()
    }

    private fun verifySwitching(start: String, end: String, overnight: Boolean) {
        val startDate = LocalDate.of(2026, 9, 12)
        val endDate = if (overnight) startDate.plusDays(1) else startDate
        var saved: List<Any?>? = null
        compose.setContent {
            RastiTheme {
                SleepEditorDialog(
                    title = "Изменить сон",
                    initialStartDate = startDate,
                    initial = SleepEntry(startTime = start, endDate = endDate.toString(), endTime = end),
                    requireEnd = true,
                    onDismiss = {},
                    onSave = { date, time, wakeDate, wakeTime -> saved = listOf(date, time, wakeDate, wakeTime) },
                )
            }
        }
        repeat(3) {
            compose.onNodeWithText("Начало").performClick()
            compose.waitForIdle()
            compose.onNodeWithText("Конец").performClick()
            compose.waitForIdle()
        }
        compose.onNodeWithText("Сохранить").performClick()
        compose.runOnIdle { assertEquals(listOf(startDate, start, endDate, end), saved) }
    }
}

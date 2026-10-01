package ru.family.rasti.ui

import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.ViewModelStore
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.family.rasti.RastiViewModel
import ru.family.rasti.data.AppData
import ru.family.rasti.data.DayRecord
import ru.family.rasti.data.FoodEntry
import ru.family.rasti.data.LocalStore
import ru.family.rasti.data.SleepEntry
import ru.family.rasti.ui.theme.RastiTheme
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DeleteConfirmationUiTest {
    @get:Rule val compose = createComposeRule()
    private val date = LocalDate.of(2026, 9, 12)
    private val clock = Clock.fixed(Instant.parse("2026-09-12T14:00:00Z"), ZoneOffset.UTC)

    @Test fun foodRequiresConfirmationAndCancelPreservesIt() {
        val day = DayRecord(date.toString(), food = listOf(FoodEntry(id = "food-1", time = "09:00", name = "Молоко", amount = 100.0, unit = "мл")))
        verify(day, "delete-food-food-1", "food-1", food = true)
    }

    @Test fun sleepRequiresConfirmationAndCancelPreservesIt() {
        val day = DayRecord(date.toString(), sleeps = listOf(SleepEntry(id = "sleep-1", startTime = "10:00", endDate = date.toString(), endTime = "11:00")))
        verify(day, "delete-sleep-sleep-1", "sleep-1", food = false)
    }

    private fun verify(day: DayRecord, buttonTag: String, id: String, food: Boolean) {
        val store = LocalStore(RuntimeEnvironment.getApplication())
        store.saveData(AppData(days = mapOf(date.toString() to day)))
        val model = RastiViewModel(store)
        val models = ViewModelStore().apply { put("fixture", model) }
        try {
            compose.setContent { RastiTheme { TodayScreen(model, initialDate = date, clock = clock) } }
            compose.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToNode(hasTestTag(buttonTag))
            compose.onNodeWithTag(buttonTag).performClick()
            compose.onNodeWithText("Удалить запись?").assertExists()
            compose.onNodeWithText("Отмена").performClick()
            compose.runOnIdle {
                assertTrue(if (food) model.day(date).food.any { it.id == id } else model.day(date).sleeps.any { it.id == id })
            }
            compose.onNodeWithTag(buttonTag).performClick()
            compose.onNodeWithText("Удалить").performClick()
            compose.runOnIdle {
                assertFalse(if (food) model.day(date).food.any { it.id == id } else model.day(date).sleeps.any { it.id == id })
                assertTrue(if (food) id in model.day(date).deletedFoodIds else id in model.day(date).deletedSleepIds)
            }
        } finally {
            models.clear()
        }
    }
}
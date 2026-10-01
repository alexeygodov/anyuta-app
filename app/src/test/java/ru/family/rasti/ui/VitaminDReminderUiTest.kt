package ru.family.rasti.ui

import android.content.Intent
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.LocalLifecycleOwner
import ru.family.rasti.RastiViewModel
import ru.family.rasti.data.*
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.family.rasti.ui.theme.RastiTheme
import java.time.*

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class VitaminDReminderUiTest {
    @get:Rule val compose = createComposeRule()

    private class MutableClock(var current: Instant, var localZone: ZoneId = ZoneOffset.UTC) : Clock() {
        override fun instant() = current
        override fun getZone() = localZone
        override fun withZone(zone: ZoneId): Clock = MutableClock(current, zone)
    }

    @Test fun openReminderStopsAtBoundaryAndStillAcceptsClick() {
        val clock = MutableClock(Instant.parse("2026-09-14T13:59:59Z"))
        var clicks = 0
        compose.mainClock.autoAdvance = false
        compose.setContent {
            val now = rememberReminderTime(clock)
            RastiTheme { Surface {
                VitaminDReminder(shouldPulseVitaminD(LocalDate.of(2026, 9, 14), false, now)) { clicks++ }
            } }
        }
        compose.mainClock.advanceTimeBy(32)
        val node = compose.onNodeWithTag("vitamin-d-reminder")
        node.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Напоминание до 14:00"))
        clock.current = Instant.parse("2026-09-14T14:00:00Z")
        compose.mainClock.advanceTimeBy(1_100)
        node.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Статичное напоминание"))
        node.assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test fun timeAndZoneBroadcastsRefreshWithoutReopening() {
        val clock = MutableClock(Instant.parse("2026-09-14T13:00:00Z"))
        compose.setContent {
            val now = rememberReminderTime(clock)
            RastiTheme { VitaminDReminder(shouldPulseVitaminD(LocalDate.of(2026, 9, 14), false, now), {}) }
        }
        val node = compose.onNodeWithTag("vitamin-d-reminder")
        node.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Напоминание до 14:00"))
        compose.runOnIdle {
            clock.localZone = ZoneOffset.ofHours(4)
            RuntimeEnvironment.getApplication().sendBroadcast(Intent(Intent.ACTION_TIMEZONE_CHANGED))
        }
        compose.waitForIdle()
        node.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Статичное напоминание"))
        compose.runOnIdle {
            clock.current = Instant.parse("2026-09-14T08:00:00Z")
            RuntimeEnvironment.getApplication().sendBroadcast(Intent(Intent.ACTION_TIME_CHANGED))
        }
        compose.waitForIdle()
        node.assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Напоминание до 14:00"))
    }
    @Test fun returningToForegroundRefreshesTheCutoff() {
        val clock = MutableClock(Instant.parse("2026-09-14T13:59:00Z"))
        val owner = object : LifecycleOwner {
            val registry = LifecycleRegistry(this)
            override val lifecycle: Lifecycle get() = registry
        }
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                val now = rememberReminderTime(clock)
                RastiTheme { VitaminDReminder(shouldPulseVitaminD(LocalDate.of(2026, 9, 14), false, now), {}) }
            }
        }
        compose.onNodeWithTag("vitamin-d-reminder").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Напоминание до 14:00"))
        compose.runOnIdle {
            owner.registry.currentState = Lifecycle.State.CREATED
            clock.current = Instant.parse("2026-09-14T16:00:00Z")
            owner.registry.currentState = Lifecycle.State.RESUMED
        }
        compose.onNodeWithTag("vitamin-d-reminder").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Статичное напоминание"))
    }

    @Test fun midnightSelectsNewDayWithoutCarryingTheTakenVitamin() {
        val clock = MutableClock(Instant.parse("2026-09-14T23:59:59Z"))
        val date = LocalDate.of(2026, 9, 14)
        val store = LocalStore(RuntimeEnvironment.getApplication())
        store.saveData(AppData(days = mapOf(date.toString() to DayRecord(date.toString(), vitamins = listOf(
            VitaminEntry(id = "synthetic-d", time = "09:00", name = "Витамин D", amount = 2.0,
                unit = "капля", updatedAt = "2026-09-14T09:00:00Z"),
        )))))
        val model = RastiViewModel(store)
        val models = ViewModelStore().apply { put("fixture", model) }
        compose.mainClock.autoAdvance = false
        try {
            compose.setContent { RastiTheme { TodayScreen(model, initialDate = date, clock = clock) } }
            compose.mainClock.advanceTimeBy(32)
            compose.onNodeWithTag("vitamin-d-reminder").assertDoesNotExist()
            clock.current = Instant.parse("2026-09-15T00:00:00Z")
            compose.mainClock.advanceTimeBy(1_100)
            compose.onNodeWithTag("vitamin-d-reminder").assertIsEnabled().assert(
                SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Напоминание до 14:00"))
            compose.onNodeWithText("Сегодня, 15 сентября").assertExists()
        } finally { models.clear() }
    }

}

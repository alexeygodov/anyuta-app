package ru.family.rasti.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onAllNodesWithText
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
import ru.family.rasti.ui.theme.RastiTheme
import kotlin.math.abs

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TimeInputUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun externalTimeDuringFlingReplacesPendingSelection() {
        val time = mutableStateOf("12:45")
        compose.setContent { RastiTheme { TimeInput(time.value, onTimeChange = { time.value = it }) } }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.onAllNodes(hasScrollToIndexAction()).onFirst().performTouchInput { swipeUp(durationMillis = 100) }
        compose.runOnUiThread { time.value = "01:15" }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.runOnIdle { assertEquals("01:15", time.value) }
        assertWheelCenter(0, "01")
        assertWheelCenter(1, "15")
    }

    @Test fun userSelectionMatchesVisibleHourAndMinute() {
        val time = mutableStateOf("10:15")
        compose.setContent { RastiTheme { TimeInput(time.value, onTimeChange = { time.value = it }) } }
        compose.onAllNodes(hasScrollToIndexAction())[0].performTouchInput { swipeUp(durationMillis = 300) }
        compose.onAllNodes(hasScrollToIndexAction())[1].performTouchInput { swipeUp(durationMillis = 300) }
        compose.waitForIdle()
        val parts = time.value.split(":")
        assertWheelCenter(0, parts[0])
        assertWheelCenter(1, parts[1])
    }

    @Test fun nowCancelsHourFlingEvenWhenCurrentHourDoesNotChange() {
        val time = mutableStateOf("12:45")
        compose.setContent {
            RastiTheme {
                TimeInput(time.value, onTimeChange = { time.value = it }, nowTime = { "12:15" })
            }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.onAllNodes(hasScrollToIndexAction()).onFirst().performTouchInput { swipeUp(durationMillis = 100) }
        compose.onNodeWithText("Сейчас").performClick()
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.runOnIdle { assertEquals("12:15", time.value) }
        assertWheelCenter(0, "12")
        assertWheelCenter(1, "15")
    }

    private fun assertWheelCenter(wheel: Int, value: String) {
        val bounds = compose.onAllNodes(hasScrollToIndexAction())[wheel].fetchSemanticsNode().boundsInRoot
        val matching = compose.onAllNodesWithText(value).fetchSemanticsNodes().filter {
            abs(it.boundsInRoot.center.x - bounds.center.x) < 1f &&
                abs(it.boundsInRoot.center.y - bounds.center.y) < 1f
        }
        assertEquals("Wheel $wheel must display $value in its center", 1, matching.size)
    }
}

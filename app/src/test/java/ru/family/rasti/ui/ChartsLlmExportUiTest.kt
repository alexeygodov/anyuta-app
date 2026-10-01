package ru.family.rasti.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelStore
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.family.rasti.RastiViewModel
import ru.family.rasti.data.AppData
import ru.family.rasti.data.ChildProfile
import ru.family.rasti.data.DayRecord
import ru.family.rasti.data.LocalStore
import ru.family.rasti.data.Measurement
import ru.family.rasti.ui.theme.RastiTheme
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ChartsLlmExportUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun buttonCopiesCurrentDiaryAndPrompt() {
        val context = RuntimeEnvironment.getApplication()
        val store = LocalStore(context)
        store.saveData(AppData(
            profile = ChildProfile(name = "Не копировать имя", birthDate = "2026-01-01"),
            days = mapOf("2026-05-01" to DayRecord("2026-05-01", measurement = Measurement(heightCm = 62.0, weightKg = 6.0))),
        ))
        val model = RastiViewModel(store)
        val models = ViewModelStore().apply { put("fixture", model) }
        val clock = Clock.fixed(Instant.parse("2026-05-02T12:00:00Z"), ZoneOffset.UTC)
        try {
            compose.setContent { RastiTheme { ChartsScreen(model, clock = clock) } }
            compose.onNodeWithText("Копировать для LLM").performClick()
            compose.runOnIdle {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val text = clipboard.primaryClip?.getItemAt(0)?.coerceToText(context).toString()
                assertTrue(text.contains("2026-05-01 | 62 см | 6 кг"))
                assertTrue(text.contains("Отсутствие записи не означает ноль"))
                assertTrue(!text.contains("Не копировать имя"))
            }
        } finally {
            models.clear()
        }
    }
}
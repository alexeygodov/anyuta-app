package ru.family.rasti.visual

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ErrorCollector
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.shadows.ShadowDialog
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ru.family.rasti.RastiViewModel
import ru.family.rasti.data.*
import ru.family.rasti.ui.*
import ru.family.rasti.ui.theme.RastiTheme
import java.time.*
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en-rUS-w400dp-h800dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenScreenshotTest {
    @get:Rule val compose = createComposeRule()
    @get:Rule val errors = ErrorCollector()
    private val date = LocalDate.of(2026, 9, 12)
    private val clock = Clock.fixed(Instant.parse("2026-09-12T14:00:00Z"), ZoneOffset.UTC)

    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun todayLight360Normal() = captureProfile("today", false, 360, 1f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun todayLight320Large() = captureProfile("today", false, 320, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun todayLight360Large() = captureProfile("today", false, 360, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun todayLight320Normal() = captureProfile("today", false, 320, 1f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun todayDark360Normal() = captureProfile("today", true, 360, 1f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun todayDark320Large() = captureProfile("today", true, 320, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun todayDark360Large() = captureProfile("today", true, 360, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun todayDark320Normal() = captureProfile("today", true, 320, 1f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun historyLight360Normal() = captureProfile("history", false, 360, 1f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun historyLight320Large() = captureProfile("history", false, 320, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun historyLight360Large() = captureProfile("history", false, 360, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun historyLight320Normal() = captureProfile("history", false, 320, 1f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun historyDark360Normal() = captureProfile("history", true, 360, 1f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun historyDark320Large() = captureProfile("history", true, 320, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun historyDark360Large() = captureProfile("history", true, 360, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun historyDark320Normal() = captureProfile("history", true, 320, 1f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun chartsLight360Normal() = captureProfile("charts", false, 360, 1f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun chartsLight320Large() = captureProfile("charts", false, 320, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun chartsLight360Large() = captureProfile("charts", false, 360, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun chartsLight320Normal() = captureProfile("charts", false, 320, 1f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun chartsDark360Normal() = captureProfile("charts", true, 360, 1f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun chartsDark320Large() = captureProfile("charts", true, 320, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun chartsDark360Large() = captureProfile("charts", true, 360, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun chartsDark320Normal() = captureProfile("charts", true, 320, 1f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun settingsLight360Normal() = captureProfile("settings", false, 360, 1f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun settingsLight320Large() = captureProfile("settings", false, 320, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun settingsLight360Large() = captureProfile("settings", false, 360, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun settingsLight320Normal() = captureProfile("settings", false, 320, 1f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun settingsDark360Normal() = captureProfile("settings", true, 360, 1f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun settingsDark320Large() = captureProfile("settings", true, 320, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun settingsDark360Large() = captureProfile("settings", true, 360, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun settingsDark320Normal() = captureProfile("settings", true, 320, 1f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun bottleLight360Normal() = captureProfile("bottle", false, 360, 1f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun bottleLight320Large() = captureProfile("bottle", false, 320, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun bottleLight360Large() = captureProfile("bottle", false, 360, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun bottleLight320Normal() = captureProfile("bottle", false, 320, 1f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun bottleDark360Normal() = captureProfile("bottle", true, 360, 1f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun bottleDark320Large() = captureProfile("bottle", true, 320, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun bottleDark360Large() = captureProfile("bottle", true, 360, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun bottleDark320Normal() = captureProfile("bottle", true, 320, 1f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun milkSummaryLight360Normal() = captureProfile("milk-summary", false, 360, 1f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun milkSummaryLight320Large() = captureProfile("milk-summary", false, 320, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun milkSummaryLight360Large() = captureProfile("milk-summary", false, 360, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun milkSummaryLight320Normal() = captureProfile("milk-summary", false, 320, 1f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun milkSummaryDark360Normal() = captureProfile("milk-summary", true, 360, 1f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun milkSummaryDark320Large() = captureProfile("milk-summary", true, 320, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun milkSummaryDark360Large() = captureProfile("milk-summary", true, 360, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun milkSummaryDark320Normal() = captureProfile("milk-summary", true, 320, 1f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun recoveryLight360Normal() = captureProfile("recovery", false, 360, 1f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun recoveryLight320Large() = captureProfile("recovery", false, 320, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun recoveryLight360Large() = captureProfile("recovery", false, 360, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun recoveryLight320Normal() = captureProfile("recovery", false, 320, 1f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun recoveryDark360Normal() = captureProfile("recovery", true, 360, 1f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun recoveryDark320Large() = captureProfile("recovery", true, 320, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun recoveryDark360Large() = captureProfile("recovery", true, 360, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun recoveryDark320Normal() = captureProfile("recovery", true, 320, 1f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun storageerrorLight360Normal() = captureProfile("storage-error", false, 360, 1f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun storageerrorLight320Large() = captureProfile("storage-error", false, 320, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun storageerrorLight360Large() = captureProfile("storage-error", false, 360, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun storageerrorLight320Normal() = captureProfile("storage-error", false, 320, 1f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun storageerrorDark360Normal() = captureProfile("storage-error", true, 360, 1f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun storageerrorDark320Large() = captureProfile("storage-error", true, 320, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun storageerrorDark360Large() = captureProfile("storage-error", true, 360, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun storageerrorDark320Normal() = captureProfile("storage-error", true, 320, 1f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun appLight360Normal() = captureProfile("app", false, 360, 1f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun appLight320Large() = captureProfile("app", false, 320, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun appLight360Large() = captureProfile("app", false, 360, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun appLight320Normal() = captureProfile("app", false, 320, 1f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun appDark360Normal() = captureProfile("app", true, 360, 1f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun appDark320Large() = captureProfile("app", true, 320, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun appDark360Large() = captureProfile("app", true, 360, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun appDark320Normal() = captureProfile("app", true, 320, 1f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun fooddialogLight360Normal() = captureProfile("food-dialog", false, 360, 1f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun fooddialogLight320Large() = captureProfile("food-dialog", false, 320, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun fooddialogLight360Large() = captureProfile("food-dialog", false, 360, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun fooddialogLight320Normal() = captureProfile("food-dialog", false, 320, 1f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun fooddialogDark360Normal() = captureProfile("food-dialog", true, 360, 1f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun fooddialogDark320Large() = captureProfile("food-dialog", true, 320, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun fooddialogDark360Large() = captureProfile("food-dialog", true, 360, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun fooddialogDark320Normal() = captureProfile("food-dialog", true, 320, 1f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun vitaminformLight360Normal() = captureProfile("vitamin-form", false, 360, 1f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun vitaminformLight320Large() = captureProfile("vitamin-form", false, 320, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun vitaminformLight360Large() = captureProfile("vitamin-form", false, 360, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun vitaminformLight320Normal() = captureProfile("vitamin-form", false, 320, 1f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun vitaminformDark360Normal() = captureProfile("vitamin-form", true, 360, 1f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun vitaminformDark320Large() = captureProfile("vitamin-form", true, 320, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun vitaminformDark360Large() = captureProfile("vitamin-form", true, 360, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun vitaminformDark320Normal() = captureProfile("vitamin-form", true, 320, 1f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun sleepdialogLight360Normal() = captureProfile("sleep-dialog", false, 360, 1f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun sleepdialogLight320Large() = captureProfile("sleep-dialog", false, 320, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun sleepdialogLight360Large() = captureProfile("sleep-dialog", false, 360, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun sleepdialogLight320Normal() = captureProfile("sleep-dialog", false, 320, 1f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun sleepdialogDark360Normal() = captureProfile("sleep-dialog", true, 360, 1f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun sleepdialogDark320Large() = captureProfile("sleep-dialog", true, 320, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun sleepdialogDark360Large() = captureProfile("sleep-dialog", true, 360, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun sleepdialogDark320Normal() = captureProfile("sleep-dialog", true, 320, 1f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun controlsLight360Normal() = captureProfile("controls", false, 360, 1f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun controlsLight320Large() = captureProfile("controls", false, 320, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun controlsLight360Large() = captureProfile("controls", false, 360, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun controlsLight320Normal() = captureProfile("controls", false, 320, 1f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun controlsDark360Normal() = captureProfile("controls", true, 360, 1f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun controlsDark320Large() = captureProfile("controls", true, 320, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w360dp-h800dp-mdpi")
    fun controlsDark360Large() = captureProfile("controls", true, 360, 1.3f)
    @Test @Config(qualifiers = "en-rUS-w320dp-h800dp-mdpi")
    fun controlsDark320Normal() = captureProfile("controls", true, 320, 1f)

    private fun fixture(): AppData {
        val days = (0..20).associate { daysAgo ->
            val day = date.minusDays(daysAgo.toLong()).toString()
            day to DayRecord(date = day,
                food = listOf(
                    FoodEntry("milk-$day", "07:30", "Молоко", 110.0, "мл", "2026-09-12T00:00:00Z"),
                    FoodEntry("formula-$day", "10:30", "Смесь", 90.0, "мл", "2026-09-12T00:00:00Z"),
                ),
                sleeps = listOf(
                    SleepEntry("night-$day", "00:00", day, "06:30", "2026-09-12T00:00:00Z"),
                    SleepEntry("nap-$day", "08:30", day, "10:00", "2026-09-12T00:00:00Z"),
                ),
                measurement = if (daysAgo % 7 == 0) Measurement(61.0 - daysAgo * .08, 5.8 - daysAgo * .02, "08:00", "2026-09-12T00:00:00Z") else null,
                fussiness = daysAgo % 3, fussinessUpdatedAt = "2026-09-12T00:00:00Z",
                updatedAt = "2026-09-12T00:00:00Z",
            )
        }
        return AppData(ChildProfile(name = "Тестовый ребёнок", birthDate = "2026-06-12", updatedAt = "2026-09-12T00:00:00Z"), days)
    }

    private fun captureProfile(scene: String, dark: Boolean, width: Int, fontScale: Float) {
        val store = LocalStore(RuntimeEnvironment.getApplication())
        store.saveData(fixture())
        val model = RastiViewModel(store)
        model.selectAppTheme(if (dark) AppTheme.DARK else AppTheme.LIGHT)
        val models = ViewModelStore().apply { put("fixture", model) }
        lateinit var hostView: View
        compose.mainClock.autoAdvance = false
        val name = "$scene-${if (dark) "dark" else "light"}-$width-${if (fontScale > 1f) "large" else "normal"}"
        try {
            compose.setContent {
                val view = LocalView.current
                SideEffect { hostView = view }
                CompositionLocalProvider(LocalDensity provides Density(1f, fontScale)) {
                    RastiTheme(darkTheme = dark) {
                        Surface(color = MaterialTheme.colorScheme.background) {
                            Box(Modifier.requiredSize(width.dp, 640.dp).testTag("capture")) {
                                when (scene) {
                                    "today" -> TodayScreen(model, initialDate = date, clock = clock)
                                    "history" -> HistoryScreen(model, clock = clock)
                                    "charts" -> ChartsScreen(model, clock = clock)
                                    "settings" -> SettingsScreen(model, clock = clock)
                                    "app" -> RastiApp(model, clock = clock)
                                    "bottle" -> BottleAmountPicker(100f, {})
                                    "milk-summary" -> Column(Modifier.padding(16.dp)) {
                                        Text("Питание за сутки", style = MaterialTheme.typography.titleMedium)
                                        MilkIntakeChart(
                                            entries = listOf(
                                                FoodEntry(id = "milk", time = "08:00", name = "Молоко", amount = 100.0, unit = "мл"),
                                                FoodEntry(id = "formula", time = "12:00", name = "Смесь", amount = 100.0, unit = "мл"),
                                            ),
                                            date = date,
                                            minimumMl = 300,
                                            targetMl = 400,
                                            maximumMl = 500,
                                            onEntryClick = {},
                                            now = java.time.LocalDateTime.of(2026, 9, 12, 14, 0),
                                        )
                                    }
                                    "recovery" -> StorageRecoveryScreen(true, {})
                                    "storage-error" -> StorageRecoveryScreen(false, {})
                                    "food-dialog" -> FoodEditorDialog("Смесь", date,
                                        initial = model.data.days[date.toString()]!!.food.last(),
                                        fixedName = "Смесь", fixedUnit = "мл", days = model.data.days,
                                        onDismiss = {}, onSave = { _, _, _, _, _ -> })
                                    "vitamin-form" -> {
                                        var amount by remember { mutableStateOf("2") }
                                        var unit by remember { mutableStateOf("капля") }
                                        var time by remember { mutableStateOf("09:00") }
                                        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Text("Витамин D", style = MaterialTheme.typography.titleLarge)
                                                VitaminEditorFields(
                                                    name = "Витамин D", onNameChange = {},
                                                    amount = amount, onAmountChange = { amount = it },
                                                    unit = unit, onUnitChange = { unit = it },
                                                    time = time, onTimeChange = { time = it },
                                                    fixedName = "Витамин D",
                                                )
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                                    TextButton(onClick = {}) { Text("Отмена") }
                                                    Button(onClick = {}) { Text("Сохранить") }
                                                }
                                            }
                                        }
                                    }
                                    "sleep-dialog" -> SleepEditorDialog("Изменить сон", date,
                                        initial = model.data.days[date.toString()]!!.sleeps.last(), requireEnd = true,
                                        onDismiss = {}, onSave = { _, _, _, _ -> })
                                    "controls" -> ControlStates()
                                }
                            }
                        }
                    }
                }
            }
            compose.mainClock.advanceTimeBy(1_000)
            compose.waitForIdle()
            fun capture(suffix: String = "") {
                if (scene.endsWith("-dialog")) {
                    val dialog = checkNotNull(ShadowDialog.getLatestDialog())
                    val decor = checkNotNull(dialog.window).decorView
                    compose.runOnIdle {
                        val bitmap = Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888)
                        decor.draw(Canvas(bitmap))
                        errors.checkSucceeds { assertScreenshot(name + suffix, bitmap) }
                    }
                } else {
                    val bounds = compose.onNodeWithTag("capture").fetchSemanticsNode().boundsInRoot
                    compose.runOnIdle {
                        val bitmap = Bitmap.createBitmap(bounds.width.roundToInt(), bounds.height.roundToInt(), Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap).apply { translate(-bounds.left, -bounds.top) }
                        hostView.draw(canvas)
                        errors.checkSucceeds { assertScreenshot(name + suffix, bitmap) }
                    }
                }
            }
            capture()
            // Each viewport is captured independently so a changed baseline does not hide later scenes.
            val scrollTargets = when (scene) {
                "today" -> listOf("Питание за сутки", "Коротко за день", "Заметка")
                "history" -> listOf("12 сентября, суббота")
                "charts" -> listOf("Сон и бодрствование", "Рост", "Темп роста", "Календарь скачков развития")
                "settings" -> listOf("Профиль ребёнка", "Уведомления на телефоне", "Важно")
                else -> emptyList()
            }
            scrollTargets.forEachIndexed { index, target ->
                compose.mainClock.autoAdvance = true
                compose.onAllNodes(hasScrollToIndexAction()).onFirst().performScrollToNode(hasText(target))
                compose.mainClock.autoAdvance = false
                compose.mainClock.advanceTimeBy(300)
                compose.waitForIdle()
                capture("-scroll-${index + 1}")
            }
            if (scene == "app") {
                listOf("История", "Графики", "Настройки").forEachIndexed { index, label ->
                    compose.mainClock.autoAdvance = true
                    compose.onNodeWithText(label).performClick()
                    compose.waitForIdle()
                    compose.mainClock.autoAdvance = false
                    compose.mainClock.advanceTimeBy(300)
                    capture("-tab-${index + 1}")
                }
            }
            if (scene == "controls") {
                compose.mainClock.autoAdvance = true
                compose.onNodeWithText("Недоступное действие").performScrollTo()
                compose.mainClock.autoAdvance = false
                compose.mainClock.advanceTimeBy(300)
                capture("-bottom")
            }
            if (scene == "food-dialog") {
                compose.mainClock.autoAdvance = true
                compose.onNodeWithText("Часы").performScrollTo()
                compose.mainClock.autoAdvance = false
                compose.mainClock.advanceTimeBy(300)
                capture("-time")
            }
        } finally { models.clear() }
    }

    @Composable
    private fun ControlStates() {
        val focus = remember { FocusRequester() }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppFilterChip(true, {}, label = { Text("Выбрано") })
            AppFilterChip(false, {}, label = { Text("Не выбрано") })
            AppFilterChip(false, {}, label = { Text("Недоступно") }, enabled = false)
            OutlinedTextField("Ввод", {}, label = { Text("В фокусе") }, modifier = Modifier.fillMaxWidth().focusRequester(focus))
            OutlinedTextField("", {}, label = { Text("Ошибка") }, isError = true, supportingText = { Text("Проверьте значение") }, modifier = Modifier.fillMaxWidth())
            VitaminDReminder(false, {})
            Button({}, enabled = false) { Text("Недоступное действие") }
        }
        LaunchedEffect(Unit) { focus.requestFocus() }
    }
}

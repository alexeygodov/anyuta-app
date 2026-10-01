package ru.family.rasti.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.RemoteViews
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.rules.ErrorCollector
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ru.family.rasti.data.*
import ru.family.rasti.R
import ru.family.rasti.visual.assertScreenshot
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "en-rUS-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetScreenshotTest {
    @get:Rule val errors = ErrorCollector()
    private val now = LocalDateTime.parse("2026-09-12T12:00:00")

    @Test fun otherWidgetSizesAndStatuses() {
        val context = RuntimeEnvironment.getApplication()
        LocalStore(context).saveWakeReminderMinutes(120)
        val date = now.toLocalDate().toString()
        val data = AppData(days = mapOf(date to DayRecord(date,
            food = listOf(
                FoodEntry(id = "milk", time = "09:00", name = "Молоко", amount = 100.0, unit = "мл"),
                FoodEntry(id = "formula", time = "10:30", name = "Смесь", amount = 80.0, unit = "мл"),
            ),
            sleeps = listOf(SleepEntry(id = "sleep", startTime = "11:30")),
        )))
        for ((width, height) in listOf(110 to 72, 220 to 160)) {
            capture("quick-${width}x$height", AnyutaQuickWidget.views(context, data, now), width, height)
        }
        for ((width, height) in listOf(220 to 90, 320 to 180)) {
            capture("timeline-${width}x$height", AnyutaTimelineWidget.timelineViews(context, data, now), width, height)
        }
        val sleepStates = mapOf(
            "ok" to emptyList(),
            "attention" to listOf(SleepEntry(id = "sleep", startTime = "08:00", endDate = date, endTime = "09:45")),
            "alert" to listOf(SleepEntry(id = "sleep", startTime = "08:00", endDate = date, endTime = "09:00")),
            "sleep" to listOf(SleepEntry(id = "sleep", startTime = "11:30")),
        )
        for ((tone, sleeps) in sleepStates) {
            val statusData = AppData(days = mapOf(date to DayRecord(date, sleeps = sleeps)))
            assertEquals(tone, operationalStatus(widgetSnapshot(context, statusData, now)).tone.name.lowercase())
            for ((width, height) in listOf(150 to 72, 220 to 120)) {
                val options = Bundle().apply {
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, width)
                    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height)
                }
                capture("status-$tone-${width}x$height", AnyutaStatusWidget.views(context, statusData, options, now), width, height)
            }
        }
        capture("widget-storage-error", RemoteViews(context.packageName, R.layout.widget_storage_error), 180, 100)
    }

    private fun capture(name: String, remote: RemoteViews, width: Int, height: Int) {
        val context = RuntimeEnvironment.getApplication()
        val view = remote.apply(context, FrameLayout(context))
        repeat(3) {
            view.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
            )
            view.layout(0, 0, width, height)
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        errors.checkSucceeds { assertScreenshot(name, bitmap) }
    }

    @Test fun dashboardSizes() {
        val context = RuntimeEnvironment.getApplication()
        val now = LocalDateTime.parse("2026-09-12T12:00:00")
        val data = AppData(
            profile = ChildProfile(name = "Тест", birthDate = "2026-06-01"),
            days = mapOf("2026-09-12" to DayRecord(
                "2026-09-12",
                food = listOf(FoodEntry(id = "food", time = "10:30", name = "Молоко", amount = 100.0, unit = "мл")),
                sleeps = listOf(SleepEntry(id = "sleep", startTime = "11:30")),
                measurement = Measurement(weightKg = 6.0),
            )),
        )
        for ((width, height) in listOf(180 to 180, 280 to 100, 320 to 180)) {
            capture("dashboard-${width}x$height",
                AnyutaDashboardWidget.dashboardViews(context, data, width, height, now), width, height)
        }
    }
}

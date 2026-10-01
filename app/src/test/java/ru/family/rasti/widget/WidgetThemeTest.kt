package ru.family.rasti.widget

import android.graphics.drawable.GradientDrawable
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.family.rasti.R
import ru.family.rasti.data.*
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class WidgetThemeTest {
    private val context = RuntimeEnvironment.getApplication()
    private val now = LocalDateTime.parse("2026-09-12T12:00:00")

    @Test fun widgetTextAndChartPairsMeetContrastFloor() {
        val pairs = listOf(
            R.color.widget_on_surface to R.color.widget_background,
            R.color.widget_on_surface to R.color.widget_surface,
            R.color.widget_on_surface_variant to R.color.widget_surface,
            R.color.widget_on_milk to R.color.widget_milk,
            R.color.widget_on_formula to R.color.widget_formula,
            R.color.widget_on_sleep to R.color.widget_sleep,
            R.color.widget_on_alert to R.color.widget_alert,
            R.color.widget_chart_milk to R.color.widget_surface,
            R.color.widget_chart_formula to R.color.widget_surface,
            R.color.widget_chart_sleep to R.color.widget_surface,
        )
        pairs.forEach { (foreground, background) ->
            val contrast = ColorUtils.calculateContrast(context.getColor(foreground), context.getColor(background))
            assertTrue("Widget contrast $contrast below 4.5", contrast >= 4.5)
        }
    }

    @Test fun dashboardReapplyChangesVitaminAndSleepPairsTogether() {
        val date = now.toLocalDate().toString()
        LocalStore(context).saveWakeReminderMinutes(120)
        val attention = AppData(days = mapOf(date to DayRecord(date,
            sleeps = listOf(SleepEntry(id = "sleep", startTime = "08:00", endDate = date, endTime = "09:00")),
        )))
        val calm = attention.copy(days = mapOf(date to attention.days.getValue(date).copy(
            sleeps = emptyList(),
            vitamins = listOf(VitaminEntry(id = "vitamin", time = "10:00", name = "Витамин D", amount = 2.0, unit = "капля")),
        )))
        val remote = AnyutaDashboardWidget.dashboardViews(context, attention, 320, 180, now)
        val root = remote.apply(context, FrameLayout(context))
        fun checkPair(id: Int, foreground: Int, background: Int) {
            val view = root.findViewById<TextView>(id)
            assertEquals(context.getColor(foreground), view.currentTextColor)
            assertEquals(context.getColor(background), (view.background as GradientDrawable).color!!.defaultColor)
            assertTrue(view.hasOnClickListeners())
        }
        checkPair(R.id.widget_vitamin_panel, R.color.widget_on_alert, R.color.widget_alert)
        checkPair(R.id.widget_sleep_action, R.color.widget_on_alert, R.color.widget_alert)
        AnyutaDashboardWidget.dashboardViews(context, calm, 320, 180, now).reapply(context, root)
        checkPair(R.id.widget_vitamin_panel, R.color.widget_on_milk, R.color.widget_milk)
        checkPair(R.id.widget_sleep_action, R.color.widget_on_sleep, R.color.widget_sleep)
        remote.reapply(context, root)
        checkPair(R.id.widget_vitamin_panel, R.color.widget_on_alert, R.color.widget_alert)
    }
}

package ru.family.rasti.widget

import android.util.Xml
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.xmlpull.v1.XmlPullParser
import ru.family.rasti.R
import ru.family.rasti.data.AppData
import ru.family.rasti.data.ChildProfile
import ru.family.rasti.data.DayRecord
import ru.family.rasti.data.FoodEntry
import ru.family.rasti.data.Measurement
import ru.family.rasti.data.SleepEntry
import ru.family.rasti.data.VitaminEntry
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DashboardWidgetTest {
    private val now = LocalDateTime.parse("2026-09-06T22:33:00")
    private val data = AppData(
        profile = ChildProfile(birthDate = "2026-06-01"),
        days = mapOf(
            "2026-09-06" to DayRecord(
                "2026-09-06",
                food = listOf(FoodEntry(time = "18:15", name = "Смесь", amount = 25.0, unit = "мл")),
                sleeps = listOf(SleepEntry(startTime = "20:05")),
                measurement = Measurement(weightKg = 6.0),
                vitamins = listOf(VitaminEntry(time = "09:00", name = "Витамин D", amount = 2.0, unit = "капля")),
            ),
        ),
    )

    private fun render(width: Int, height: Int): View {
        val context = RuntimeEnvironment.getApplication()
        val view = AnyutaDashboardWidget.dashboardViews(context, data, width, height, now)
            .apply(context, FrameLayout(context))
        repeat(3) {
            view.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
            )
            view.layout(0, 0, width, height)
        }
        return view
    }

    @Test fun providerDefaultsToDashboardButAllowsBothDirectionsToResize() {
        val context = RuntimeEnvironment.getApplication()
        val parser = context.resources.getXml(R.xml.anyuta_widget_info)
        while (parser.next() != XmlPullParser.START_TAG) Unit
        val ns = "http://schemas.android.com/apk/res/android"
        assertEquals(4, parser.getAttributeIntValue(ns, "targetCellWidth", 0))
        assertEquals(2, parser.getAttributeIntValue(ns, "targetCellHeight", 0))
        assertEquals(3, parser.getAttributeIntValue(ns, "resizeMode", 0))
        val attributes = context.obtainStyledAttributes(
            Xml.asAttributeSet(parser),
            intArrayOf(android.R.attr.minResizeWidth, android.R.attr.minResizeHeight),
        )
        try {
            assertTrue(attributes.getDimensionPixelSize(0, 999) <= 110)
            assertTrue(attributes.getDimensionPixelSize(1, 999) <= 72)
        } finally {
            attributes.recycle()
            parser.close()
        }
    }

    @Test fun sizeSelectsThreeRealLayouts() {
        assertEquals(DashboardLayout.NARROW, dashboardLayout(180, 180))
        assertEquals(DashboardLayout.COMPACT, dashboardLayout(280, 100))
        assertEquals(DashboardLayout.FULL, dashboardLayout(320, 180))
    }

    @Test fun allDashboardSizesKeepTheFourOperationsReachable() {
        listOf(180 to 180, 280 to 100, 320 to 180).forEach { (width, height) ->
            val root = render(width, height)
            listOf(
                R.id.widget_milk_action,
                R.id.widget_formula_action,
                R.id.widget_sleep_action,
                R.id.widget_vitamin_panel,
            ).forEach { id ->
                val action = root.findViewById<TextView>(id)
                assertNotNull("Action $id missing at ${width}x$height", action)
                assertTrue(action.hasOnClickListeners())
                assertTrue(action.text.isNotBlank())
                assertTrue(action.contentDescription.isNotBlank())
                assertTrue("Action width at ${width}x$height", action.width > 0)
                assertTrue("Action height at ${width}x$height", action.height > 0)
            }
        }
    }

    @Test fun fullDashboardRestoresPreviousInformationHierarchy() {
        val root = render(320, 180)
        assertTrue(root.findViewById<TextView>(R.id.widget_last_feeding).text.contains("Смесь 25"))
        assertTrue(root.findViewById<TextView>(R.id.widget_recommendation).text.contains("Расчётная порция"))
        assertTrue(root.findViewById<TextView>(R.id.widget_last_sleep).text.contains("Спит"))
        assertTrue(root.findViewById<TextView>(R.id.widget_vitamin_panel).text.contains("2"))
        assertEquals("Проснулась", root.findViewById<TextView>(R.id.widget_sleep_action).text)
    }

    @Test fun operationalStatePrioritizesSleepThenOvertiredThenFeeding() {
        val base = widgetSnapshot(RuntimeEnvironment.getApplication(), data, now)
        assertEquals(OperationalTone.SLEEP, operationalStatus(base).tone)
        assertEquals(
            OperationalTone.ALERT,
            operationalStatus(base.copy(activeSleep = false, awakeMinutes = 180, wakeAttention = .9f)).tone,
        )
        assertEquals(
            OperationalTone.ATTENTION,
            operationalStatus(
                base.copy(
                    activeSleep = false,
                    awakeMinutes = 30,
                    wakeAttention = 0f,
                    recommendation = base.recommendation?.copy(moment = ru.family.rasti.feeding.FeedingMoment.USUAL_TIME),
                ),
            ).tone,
        )
    }
}

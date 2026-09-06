package ru.family.rasti.widget

import android.appwidget.AppWidgetProviderInfo
import android.graphics.Rect
import android.util.TypedValue
import android.util.Xml
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.xmlpull.v1.XmlPullParser
import ru.family.rasti.R
import ru.family.rasti.data.*
import java.time.LocalDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "mdpi")
class DashboardWidgetTest {
    private val now = LocalDateTime.parse("2026-09-06T22:33:00")
    private val data = AppData(profile = ChildProfile(birthDate = "2026-06-01"), days = mapOf(
        "2026-09-06" to DayRecord("2026-09-06", food = listOf(FoodEntry(time = "18:15", name = "Смесь", amount = 25.0, unit = "мл")),
            sleeps = listOf(SleepEntry(startTime = "20:05")), measurement = Measurement(weightKg = 6.0),
            vitamins = listOf(VitaminEntry(time = "09:00", name = "Витамин D", amount = 2.0, unit = "капля")))) )
    private val ids = listOf(R.id.widget_milk_action, R.id.widget_formula_action, R.id.widget_sleep_action, R.id.widget_vitamin_panel)

    private fun render(width: Int, height: Int): View {
        val context = RuntimeEnvironment.getApplication()
        val view = AnyutaDashboardWidget.dashboardViews(context, data, width, height, now).apply(context, FrameLayout(context))
        // TextView auto-size requests another layout pass after choosing its font size.
        repeat(3) {
            view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
            view.layout(0, 0, width, height)
        }
        return view
    }

    @Test fun providerReallyRequestsOneRowAndAllowsShrinkingBelowOld130dp() {
        val context = RuntimeEnvironment.getApplication()
        val parser = context.resources.getXml(R.xml.anyuta_widget_info)
        while (parser.next() != XmlPullParser.START_TAG) { }
        val ns = "http://schemas.android.com/apk/res/android"
        assertEquals(1, parser.getAttributeIntValue(ns, "targetCellHeight", 0))
        val attributes = context.obtainStyledAttributes(Xml.asAttributeSet(parser), intArrayOf(android.R.attr.minResizeHeight, android.R.attr.minHeight))
        try {
            assertTrue(attributes.getDimensionPixelSize(0, 999) <= 48)
            assertTrue(attributes.getDimensionPixelSize(1, 999) <= 48)
        } finally { attributes.recycle(); parser.close() }
    }

    @Test fun actionsAndInformationFitAcrossOneRowAndGridSizes() {
        listOf(250 to 48, 280 to 80, 280 to 100, 320 to 140, 320 to 220, 360 to 320, 600 to 60).forEach { (width, height) ->
            val root = render(width, height)
            ids.forEach { id ->
                val tile = root.findViewById<TextView>(id)
                assertTrue("Action $id missing at ${width}x$height", tile.hasOnClickListeners())
                assertTrue(tile.text.isNotBlank())
                assertTrue(tile.contentDescription.isNotBlank())
                val bounds = Rect()
                tile.getDrawingRect(bounds)
                (root as android.view.ViewGroup).offsetDescendantRectToMyCoords(tile, bounds)
                assertTrue("Tile outside ${width}x$height: $bounds", bounds.left >= 0 && bounds.top >= 0 && bounds.right <= width && bounds.bottom <= height)
                assertTrue("Tap target too short at ${width}x$height", tile.height >= 48)
                assertTrue("Tap target too narrow at ${width}x$height", tile.width >= 48)
                val textLayout = tile.layout
                assertNotNull(textLayout)
                assertTrue("Text clipped vertically at ${width}x$height: ${tile.text} (text ${textLayout.height}, tile ${tile.height})",
                    textLayout.height <= tile.height - tile.compoundPaddingTop - tile.compoundPaddingBottom)
                for (line in 0 until textLayout.lineCount) {
                    assertEquals("Ellipsized information at ${width}x$height", 0, textLayout.getEllipsisCount(line))
                }
            }
            assertTrue(root.findViewById<TextView>(R.id.widget_milk_action).text.contains("25"))
            assertTrue(root.findViewById<TextView>(R.id.widget_sleep_action).text.contains("20:05"))
            assertTrue(root.findViewById<TextView>(R.id.widget_sleep_action).text.contains("Проснулась"))
        }
    }

    @Test fun resizeChangesArrangementAndTextActuallyGrows() {
        assertEquals(DashboardLayout.STRIP, dashboardLayout(280, 80))
        assertEquals(DashboardLayout.GRID, dashboardLayout(320, 220))
        val small = render(280, 80).findViewById<TextView>(R.id.widget_sleep_action)
        val large = render(360, 320).findViewById<TextView>(R.id.widget_sleep_action)
        assertTrue("Larger widget should offer larger type", large.textSize > small.textSize)
    }
}

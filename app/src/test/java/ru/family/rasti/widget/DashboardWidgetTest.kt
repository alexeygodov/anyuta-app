package ru.family.rasti.widget

import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.family.rasti.R
import ru.family.rasti.data.AppData

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "mdpi")
class DashboardWidgetTest {
    private fun render(width: Int, height: Int): View {
        val context = RuntimeEnvironment.getApplication()
        val view = AnyutaDashboardWidget.dashboardViews(context, AppData(), width, height).apply(context, FrameLayout(context))
        view.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY))
        view.layout(0, 0, width, height)
        return view
    }

    @Test fun extraLauncherHeightDoesNotStretchPaintedPanel() {
        val view = render(320, 300)
        val panel = view.findViewById<View>(R.id.widget_root)
        val button = view.findViewById<View>(R.id.widget_sleep_action)
        assertTrue("The panel must wrap its content, not fill the launcher cell", panel.height < 210)
        val buttonRow = button.parent as View
        assertEquals("No empty tail under the action row", panel.height, buttonRow.bottom + panel.paddingBottom)
        assertNull("Outer launcher slot must not be painted", view.background)
        assertNotNull(panel.background)
        assertTrue(button.hasOnClickListeners())
    }

    @Test fun compactPanelFitsMinimumSizeAndSleepHasFeedingFontSize() {
        val view = render(250, 130)
        val panel = view.findViewById<View>(R.id.widget_root)
        val milk = view.findViewById<View>(R.id.widget_milk_action)
        val row = milk.parent as View
        assertTrue("Actions must fit the compact launcher slot", row.bottom + panel.paddingBottom <= 130)
        assertEquals(48, milk.height)
        assertEquals(view.findViewById<TextView>(R.id.widget_last_feeding).textSize,
            view.findViewById<TextView>(R.id.widget_last_sleep).textSize, .01f)
    }
}

package ru.family.rasti.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.widget.RemoteViews
import ru.family.rasti.R
import ru.family.rasti.data.AppData
import ru.family.rasti.data.LocalStore
import java.time.LocalDateTime

object WidgetAction {
    const val EXTRA = "ru.family.rasti.widget.ACTION"
    const val MILK = "milk"
    const val FORMULA = "formula"
    const val SLEEP = "sleep"
    const val VITAMIN_D = "vitamin_d"
}

class AnyutaDashboardWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val data = LocalStore(context).loadData()
        appWidgetIds.forEach { manager.updateAppWidget(it, sizedViews(context, data, manager.getAppWidgetOptions(it))) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        manager.updateAppWidget(appWidgetId, sizedViews(context, LocalStore(context).loadData(), newOptions))
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val data = LocalStore(context).loadData()
            val component = ComponentName(context, AnyutaDashboardWidget::class.java)
            manager.getAppWidgetIds(component).forEach {
                manager.updateAppWidget(it, sizedViews(context, data, manager.getAppWidgetOptions(it)))
            }
            AnyutaStatusWidget.updateAll(context, data)
            AnyutaQuickWidget.updateAll(context, data)
            AnyutaTimelineWidget.updateAll(context, data)
        }

        internal fun sizedViews(context: Context, data: AppData, options: Bundle): RemoteViews {
            if (Build.VERSION.SDK_INT >= 31) {
                val sizes = androidx.core.os.BundleCompat.getParcelableArrayList(
                    options, AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java,
                ).orEmpty().filter { it.width > 0 && it.height > 0 }.distinct().take(16)
                if (sizes.isNotEmpty()) {
                    return RemoteViews(sizes.associateWith {
                        dashboardViews(context, data, it.width.toInt(), it.height.toInt())
                    })
                }
            }
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 250).coerceAtLeast(1)
            val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidth).coerceAtLeast(minWidth)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 120).coerceAtLeast(1)
            val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeight).coerceAtLeast(minHeight)
            return RemoteViews(
                dashboardViews(context, data, maxWidth, minHeight),
                dashboardViews(context, data, minWidth, maxHeight),
            )
        }

        internal fun dashboardViews(
            context: Context,
            data: AppData,
            width: Int = 280,
            height: Int = 160,
            now: LocalDateTime = LocalDateTime.now(),
        ): RemoteViews {
            val snapshot = widgetSnapshot(context, data, now)
            return when (dashboardLayout(width, height)) {
                DashboardLayout.FULL -> fullViews(context, snapshot)
                DashboardLayout.COMPACT -> compactViews(context, snapshot)
                DashboardLayout.NARROW -> narrowViews(context, snapshot)
            }
        }

        private fun fullViews(context: Context, snapshot: WidgetSnapshot): RemoteViews =
            RemoteViews(context.packageName, R.layout.widget_dashboard).apply {
                setTextViewText(R.id.widget_last_feeding, snapshot.feedingFull)
                setTextViewText(R.id.widget_recommendation, snapshot.portionFull)
                setTextViewText(R.id.widget_last_sleep, snapshot.sleepFull)
                setTextViewText(R.id.widget_vitamin_panel, snapshot.vitaminFull)
                setTextViewText(R.id.widget_sleep_action, snapshot.sleepAction)
                bindDashboardActions(context, this, snapshot)
            }

        private fun compactViews(context: Context, snapshot: WidgetSnapshot): RemoteViews =
            RemoteViews(context.packageName, R.layout.widget_dashboard_compact).apply {
                setTextViewText(R.id.widget_last_feeding, snapshot.feedingShort)
                setTextViewText(R.id.widget_recommendation, snapshot.portionShort)
                setTextViewText(R.id.widget_last_sleep, snapshot.sleepShort)
                setTextViewText(R.id.widget_vitamin_panel, snapshot.vitaminShort)
                setTextViewText(R.id.widget_sleep_action, snapshot.sleepAction)
                bindDashboardActions(context, this, snapshot)
            }

        private fun narrowViews(context: Context, snapshot: WidgetSnapshot): RemoteViews =
            RemoteViews(context.packageName, R.layout.widget_dashboard_grid).apply {
                setTextViewText(R.id.widget_milk_action, "${snapshot.feedingShort}\n＋ Молоко")
                setTextViewText(R.id.widget_formula_action, "${snapshot.portionShort}\n＋ Смесь")
                setTextViewText(R.id.widget_sleep_action, "${snapshot.sleepShort}\n${snapshot.sleepAction}")
                setTextViewText(R.id.widget_vitamin_panel, "${snapshot.vitaminShort}\n${if (snapshot.vitaminTaken) "✓" else "＋ Отметить"}")
                bindDashboardActions(context, this, snapshot)
            }

        private fun bindDashboardActions(context: Context, views: RemoteViews, snapshot: WidgetSnapshot) {
            views.setTextColor(
                R.id.widget_vitamin_panel,
                if (snapshot.vitaminTaken) Color.rgb(177, 232, 183) else Color.rgb(255, 174, 167),
            )
            views.setTextColor(
                R.id.widget_sleep_action,
                androidx.core.graphics.ColorUtils.blendARGB(Color.WHITE, Color.rgb(255, 174, 167), snapshot.wakeAttention),
            )
            views.setOnClickPendingIntent(R.id.widget_root, widgetPendingIntent(context, null, 100))
            views.setOnClickPendingIntent(R.id.widget_milk_action, widgetPendingIntent(context, WidgetAction.MILK, 101))
            views.setOnClickPendingIntent(R.id.widget_formula_action, widgetPendingIntent(context, WidgetAction.FORMULA, 102))
            views.setOnClickPendingIntent(R.id.widget_sleep_action, widgetPendingIntent(context, WidgetAction.SLEEP, 103))
            views.setOnClickPendingIntent(R.id.widget_vitamin_panel, widgetPendingIntent(context, WidgetAction.VITAMIN_D, 104))
            views.setContentDescription(R.id.widget_milk_action, "${snapshot.feedingFull}. Добавить молоко")
            views.setContentDescription(R.id.widget_formula_action, "${snapshot.portionFull}. Добавить смесь")
            views.setContentDescription(R.id.widget_sleep_action, "${snapshot.sleepFull}. ${snapshot.sleepAction}")
            views.setContentDescription(R.id.widget_vitamin_panel, "${snapshot.vitaminFull}. Открыть витамин D")
        }
    }
}

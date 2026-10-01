package ru.family.rasti.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.widget.RemoteViews
import ru.family.rasti.R
import ru.family.rasti.data.AppData
import java.time.LocalDateTime

class AnyutaQuickWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val data = (loadWidgetData(context) ?: return)
        appWidgetIds.forEach { manager.updateAppWidget(it, views(context, data)) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        manager.updateAppWidget(appWidgetId, views(context, (loadWidgetData(context) ?: return)))
    }

    companion object {
        fun updateAll(context: Context, cachedData: AppData? = null) {
            val data = cachedData ?: loadWidgetData(context) ?: return
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, AnyutaQuickWidget::class.java)
            manager.getAppWidgetIds(component).forEach { manager.updateAppWidget(it, views(context, data)) }
        }

        internal fun views(context: Context, data: AppData, now: LocalDateTime = LocalDateTime.now()): RemoteViews {
            val snapshot = widgetSnapshot(context, data, now)
            return RemoteViews(context.packageName, R.layout.widget_quick_actions).apply {
                bindVitaminAndSleepColors(context, R.id.widget_quick_vitamin, R.id.widget_quick_sleep, snapshot)
                setTextViewText(R.id.widget_quick_milk, "＋ Молоко\n${snapshot.feedingShort}")
                setTextViewText(R.id.widget_quick_formula, "＋ Смесь\n${snapshot.portionShort}")
                setTextViewText(R.id.widget_quick_sleep, "${snapshot.sleepAction}\n${snapshot.sleepShort}")
                setTextViewText(
                    R.id.widget_quick_vitamin,
                    if (snapshot.vitaminTaken) "✓ Витамин D\n${snapshot.vitaminShort.removePrefix("D · ")}" else "＋ Витамин D\nне принят",
                )
                setOnClickPendingIntent(R.id.widget_quick_root, widgetPendingIntent(context, null, 300))
                setOnClickPendingIntent(R.id.widget_quick_milk, widgetPendingIntent(context, WidgetAction.MILK, 301))
                setOnClickPendingIntent(R.id.widget_quick_formula, widgetPendingIntent(context, WidgetAction.FORMULA, 302))
                setOnClickPendingIntent(R.id.widget_quick_sleep, widgetPendingIntent(context, WidgetAction.SLEEP, 303))
                setOnClickPendingIntent(R.id.widget_quick_vitamin, widgetPendingIntent(context, WidgetAction.VITAMIN_D, 304))
                setContentDescription(R.id.widget_quick_milk, "Добавить молоко. ${snapshot.feedingFull}")
                setContentDescription(R.id.widget_quick_formula, "Добавить смесь. ${snapshot.portionFull}")
                setContentDescription(R.id.widget_quick_sleep, "${snapshot.sleepAction}. ${snapshot.sleepFull}")
                setContentDescription(R.id.widget_quick_vitamin, snapshot.vitaminFull)
            }
        }
    }
}

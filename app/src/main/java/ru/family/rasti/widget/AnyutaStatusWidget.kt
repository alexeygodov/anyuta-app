package ru.family.rasti.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import ru.family.rasti.R
import ru.family.rasti.data.AppData
import ru.family.rasti.data.LocalStore

class AnyutaStatusWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val data = LocalStore(context).loadData()
        appWidgetIds.forEach { id ->
            manager.updateAppWidget(id, views(context, data, manager.getAppWidgetOptions(id)))
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        manager.updateAppWidget(appWidgetId, views(context, LocalStore(context).loadData(), newOptions))
    }

    companion object {
        fun updateAll(context: Context, data: AppData = LocalStore(context).loadData()) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, AnyutaStatusWidget::class.java)
            manager.getAppWidgetIds(component).forEach { id ->
                manager.updateAppWidget(id, views(context, data, manager.getAppWidgetOptions(id)))
            }
        }

        internal fun views(context: Context, data: AppData, options: Bundle = Bundle()): RemoteViews {
            val snapshot = widgetSnapshot(context, data)
            val status = operationalStatus(snapshot)
            val width = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 220)
            val height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 100)
            return RemoteViews(context.packageName, R.layout.widget_status).apply {
                setTextViewText(R.id.widget_status_title, status.title)
                setTextViewText(R.id.widget_status_detail, status.detail)
                setViewVisibility(R.id.widget_status_detail, if (height < 88) View.GONE else View.VISIBLE)
                setTextViewText(R.id.widget_status_milk, if (width < 190) "Молоко" else "＋ Молоко")
                setTextViewText(R.id.widget_status_formula, if (width < 190) "Смесь" else "＋ Смесь")
                setTextViewText(R.id.widget_status_sleep, if (width < 190) "Сон" else snapshot.sleepAction)
                setInt(R.id.widget_status_root, "setBackgroundResource", status.background())
                setOnClickPendingIntent(R.id.widget_status_root, widgetPendingIntent(context, null, 200))
                setOnClickPendingIntent(R.id.widget_status_milk, widgetPendingIntent(context, WidgetAction.MILK, 201))
                setOnClickPendingIntent(R.id.widget_status_formula, widgetPendingIntent(context, WidgetAction.FORMULA, 202))
                setOnClickPendingIntent(R.id.widget_status_sleep, widgetPendingIntent(context, WidgetAction.SLEEP, 203))
                setContentDescription(R.id.widget_status_root, "${status.title}. ${status.detail}")
                setContentDescription(R.id.widget_status_milk, "Добавить молоко")
                setContentDescription(R.id.widget_status_formula, "Добавить смесь")
                setContentDescription(R.id.widget_status_sleep, snapshot.sleepAction)
            }
        }

        private fun OperationalStatus.background(): Int = when (tone) {
            OperationalTone.OK -> R.drawable.widget_status_ok_background
            OperationalTone.ATTENTION -> R.drawable.widget_status_attention_background
            OperationalTone.ALERT -> R.drawable.widget_status_alert_background
            OperationalTone.SLEEP -> R.drawable.widget_status_sleep_background
        }
    }
}

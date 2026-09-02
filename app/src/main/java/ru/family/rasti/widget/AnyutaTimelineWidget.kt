package ru.family.rasti.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import ru.family.rasti.MainActivity
import ru.family.rasti.R
import ru.family.rasti.data.AppData
import ru.family.rasti.data.LocalStore
import java.time.LocalDate
import java.time.LocalDateTime

class AnyutaTimelineWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        val data = LocalStore(context).loadData()
        appWidgetIds.forEach { manager.updateAppWidget(it, timelineViews(context, data)) }
    }

    companion object {
        fun updateAll(context: Context, data: AppData = LocalStore(context).loadData()) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, AnyutaTimelineWidget::class.java)
            manager.getAppWidgetIds(component).forEach {
                manager.updateAppWidget(it, timelineViews(context, data))
            }
        }

        internal fun timelineViews(context: Context, data: AppData): RemoteViews {
            val today = LocalDate.now()
            val now = LocalDateTime.now()
            return RemoteViews(context.packageName, R.layout.widget_timeline).apply {
                setImageViewBitmap(R.id.widget_timeline_image, renderWidgetTimeline(data, today, now))
                setOnClickPendingIntent(R.id.widget_timeline_root, openAppIntent(context))
            }
        }

        private fun openAppIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            return PendingIntent.getActivity(
                context,
                10,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}

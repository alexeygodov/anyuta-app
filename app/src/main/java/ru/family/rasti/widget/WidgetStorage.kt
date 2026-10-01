package ru.family.rasti.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import ru.family.rasti.R
import ru.family.rasti.data.AppData
import ru.family.rasti.data.LocalStore

internal fun loadWidgetData(context: Context): AppData? {
    val store = LocalStore(context)
    val result = runCatching { store.loadData() }
    if (result.isSuccess && !store.wasDataRecovered()) return result.getOrThrow()
    val views = RemoteViews(context.packageName, R.layout.widget_storage_error).apply {
        setOnClickPendingIntent(R.id.widget_storage_error, widgetPendingIntent(context, null, 400))
    }
    val manager = AppWidgetManager.getInstance(context)
    listOf(
        AnyutaDashboardWidget::class.java, AnyutaStatusWidget::class.java,
        AnyutaQuickWidget::class.java, AnyutaTimelineWidget::class.java,
    ).forEach { provider ->
        manager.getAppWidgetIds(ComponentName(context, provider)).forEach {
            manager.updateAppWidget(it, views)
        }
    }
    return null
}

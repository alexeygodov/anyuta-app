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
import ru.family.rasti.data.FoodEntry
import ru.family.rasti.data.LocalStore
import ru.family.rasti.feeding.FeedingGuide
import ru.family.rasti.feeding.SmartFeedingGuide
import ru.family.rasti.sleep.activeSleep
import ru.family.rasti.sleep.formatSleepDuration
import ru.family.rasti.sleep.lastCompletedSleep
import ru.family.rasti.sleep.sleepDurationMinutes
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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
        appWidgetIds.forEach { manager.updateAppWidget(it, dashboardViews(context, data)) }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, AnyutaDashboardWidget::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return
            val data = LocalStore(context).loadData()
            ids.forEach { manager.updateAppWidget(it, dashboardViews(context, data)) }
        }

        internal fun dashboardViews(context: Context, data: AppData): RemoteViews {
            val today = LocalDate.now()
            val now = LocalDateTime.now()
            val guide = FeedingGuide.calculate(data, today).guide
            val recommendation = SmartFeedingGuide.calculate(data, today, guide, now)
            val last = lastFeeding(data, now)
            val active = activeSleep(data)
            val lastCompleted = lastCompletedSleep(data, now)

            val views = RemoteViews(context.packageName, R.layout.widget_dashboard)
            views.setTextViewText(
                R.id.widget_date,
                today.format(DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru"))),
            )
            views.setTextViewText(R.id.widget_updated, "обновлено ${now.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))}")
            views.setTextViewText(
                R.id.widget_recommendation,
                recommendation?.let { "Расчётная порция: ${it.amountMl} мл" } ?: "Добавьте вес для расчёта порции",
            )
            views.setTextViewText(
                R.id.widget_last_feeding,
                last?.let { (dateTime, entry) ->
                    val minutes = Duration.between(dateTime, now).toMinutes().coerceAtLeast(0)
                    "Последнее кормление: ${entry.name} ${entry.amount.toInt()} мл · ${agoText(minutes)}"
                } ?: "Последнее кормление: пока нет",
            )
            views.setTextViewText(
                R.id.widget_last_sleep,
                active?.let {
                    val duration = sleepDurationMinutes(it.startDate, it.entry, now) ?: 0
                    "Сон сейчас: ${formatSleepDuration(duration)} · с ${it.entry.startTime}"
                } ?: lastCompleted?.let {
                    val duration = sleepDurationMinutes(it.startDate, it.entry, now) ?: 0
                    "Последний сон: ${formatSleepDuration(duration)} · до ${it.entry.endTime}"
                } ?: "Последний сон: пока нет",
            )
            views.setImageViewBitmap(R.id.widget_timeline, renderWidgetTimeline(data, today, now))
            views.setTextViewText(R.id.widget_sleep_action, if (active == null) "Уснула" else "Проснулась")
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent(context, null, 0))
            views.setOnClickPendingIntent(R.id.widget_milk_action, pendingIntent(context, WidgetAction.MILK, 1))
            views.setOnClickPendingIntent(R.id.widget_formula_action, pendingIntent(context, WidgetAction.FORMULA, 2))
            views.setOnClickPendingIntent(R.id.widget_sleep_action, pendingIntent(context, WidgetAction.SLEEP, 3))
            return views
        }

        private fun pendingIntent(context: Context, action: String?, requestCode: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                action?.let { putExtra(WidgetAction.EXTRA, it) }
            }
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun lastFeeding(data: AppData, now: LocalDateTime): Pair<LocalDateTime, FoodEntry>? =
            data.days.values.asSequence()
                .mapNotNull { day -> runCatching { LocalDate.parse(day.date) }.getOrNull()?.let { it to day } }
                .flatMap { (date, day) ->
                    day.food.asSequence().filter(::isMeasuredMilk).mapNotNull { entry ->
                        runCatching { date.atTime(LocalTime.parse(entry.time)) }.getOrNull()?.let { it to entry }
                    }
                }
                .filter { it.first <= now }
                .maxByOrNull { it.first }

        private fun isMeasuredMilk(entry: FoodEntry): Boolean =
            entry.unit.trim().lowercase() in setOf("мл", "ml") &&
                entry.name.trim().lowercase() in setOf("молоко", "смесь")

        private fun agoText(minutes: Long): String = when {
            minutes < 1 -> "сейчас"
            minutes < 60 -> "$minutes мин назад"
            minutes % 60 == 0L -> "${minutes / 60} ч назад"
            else -> "${minutes / 60} ч ${minutes % 60} мин назад"
        }
    }
}

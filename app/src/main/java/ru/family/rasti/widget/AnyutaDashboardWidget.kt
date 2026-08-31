package ru.family.rasti.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import ru.family.rasti.MainActivity
import ru.family.rasti.R
import ru.family.rasti.data.AppData
import ru.family.rasti.data.FoodEntry
import ru.family.rasti.data.LocalStore
import ru.family.rasti.data.VitaminEntry
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
            val day = data.days[today.toString()]
            val guide = FeedingGuide.calculate(data, today).guide
            val recommendation = SmartFeedingGuide.calculate(data, today, guide, now)
            val consumed = day?.food.orEmpty().filter(::isMeasuredMilk).sumOf { it.amount }.toInt()
            val progress = guide?.targetMl?.takeIf { it > 0 }?.let { (consumed * 100 / it).coerceIn(0, 100) } ?: 0
            val last = lastFeeding(data, now)
            val active = activeSleep(data)
            val lastCompleted = lastCompletedSleep(data, now)
            val vitaminTaken = day?.vitamins.orEmpty().any(::isVitaminD)

            val views = RemoteViews(context.packageName, R.layout.widget_dashboard)
            views.setTextViewText(R.id.widget_progress_text, guide?.let { "$consumed / ${it.targetMl} мл" } ?: "$consumed мл")
            views.setProgressBar(R.id.widget_progress, 100, progress, guide == null)
            views.setTextViewText(
                R.id.widget_recommendation,
                recommendation?.let { "Расчётная порция: ${it.amountMl} мл" } ?: "Добавьте вес для расчёта порции",
            )
            views.setTextViewText(
                R.id.widget_last_feeding,
                last?.let { (dateTime, entry) ->
                    val minutes = Duration.between(dateTime, now).toMinutes().coerceAtLeast(0)
                    "Последнее: ${entry.name} ${entry.amount.toInt()} мл · ${agoText(minutes)}"
                } ?: "Кормлений пока нет",
            )
            views.setTextViewText(
                R.id.widget_sleep,
                active?.let {
                    val duration = sleepDurationMinutes(it.startDate, it.entry, now) ?: 0
                    "Сон: идёт ${formatSleepDuration(duration)}"
                } ?: lastCompleted?.let {
                    val duration = sleepDurationMinutes(it.startDate, it.entry, now) ?: 0
                    "Последний сон: ${formatSleepDuration(duration)}"
                } ?: "Сон: записей нет",
            )
            views.setTextViewText(R.id.widget_vitamin, if (vitaminTaken) "● Витамин D принят" else "● Витамин D не принят")
            views.setTextColor(R.id.widget_vitamin, if (vitaminTaken) Color.rgb(177, 232, 183) else Color.rgb(255, 174, 167))
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            return views
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

        private fun isVitaminD(entry: VitaminEntry): Boolean {
            val name = entry.name.lowercase().replace("ё", "е")
            return name.contains("витамин d") || name.contains("витамин д") || name.contains("d3")
        }

        private fun agoText(minutes: Long): String = when {
            minutes < 1 -> "сейчас"
            minutes < 60 -> "$minutes мин назад"
            minutes % 60 == 0L -> "${minutes / 60} ч назад"
            else -> "${minutes / 60} ч ${minutes % 60} мин назад"
        }
    }
}

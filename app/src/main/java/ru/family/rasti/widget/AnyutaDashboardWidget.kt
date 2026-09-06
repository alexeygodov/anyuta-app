package ru.family.rasti.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Build
import android.util.SizeF
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import ru.family.rasti.MainActivity
import ru.family.rasti.R
import ru.family.rasti.data.AppData
import ru.family.rasti.data.FoodEntry
import ru.family.rasti.data.LocalStore
import ru.family.rasti.data.VitaminEntry
import ru.family.rasti.data.displayDose
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
            val component = ComponentName(context, AnyutaDashboardWidget::class.java)
            val ids = manager.getAppWidgetIds(component)
            val data = LocalStore(context).loadData()
            ids.forEach { manager.updateAppWidget(it, sizedViews(context, data, manager.getAppWidgetOptions(it))) }
            AnyutaTimelineWidget.updateAll(context, data)
        }

        internal fun sizedViews(context: Context, data: AppData, options: Bundle): RemoteViews {
            if (Build.VERSION.SDK_INT >= 31) {
                val sizes = androidx.core.os.BundleCompat.getParcelableArrayList(
                    options, AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java,
                ).orEmpty().filter { it.width > 0 && it.height > 0 }.distinct().take(16)
                if (sizes.isNotEmpty()) {
                    return RemoteViews(sizes.associateWith { dashboardViews(context, data, it.width.toInt(), it.height.toInt()) })
                }
            }
            // Portrait and landscape are distinct sizes, not the impossible min-width/min-height pair.
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 280).coerceAtLeast(1)
            val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidth).coerceAtLeast(minWidth)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 80).coerceAtLeast(1)
            val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeight).coerceAtLeast(minHeight)
            return RemoteViews(
                dashboardViews(context, data, maxWidth, minHeight),
                dashboardViews(context, data, minWidth, maxHeight),
            )
        }

        internal fun dashboardViews(
            context: Context, data: AppData, width: Int = 280, height: Int = 80,
            now: LocalDateTime = LocalDateTime.now(),
        ): RemoteViews {
            val today = now.toLocalDate()
            val guide = FeedingGuide.calculate(data, today).guide
            val recommendation = SmartFeedingGuide.calculate(data, today, guide, now)
            val last = lastFeeding(data, now)
            val active = activeSleep(data, now)
            val completed = lastCompletedSleep(data, now)
            val awake = ru.family.rasti.sleep.awakeMinutes(data, now)
            val vitamin = data.days[today.toString()]?.vitamins.orEmpty()
                .filter(::isVitaminD).filter { runCatching { LocalTime.parse(it.time) <= now.toLocalTime() }.getOrDefault(false) }
                .maxByOrNull { it.time }
            val layout = dashboardLayout(width, height)
            val grid = layout == DashboardLayout.GRID
            val views = RemoteViews(context.packageName, if (grid) R.layout.widget_dashboard_grid else R.layout.widget_dashboard)
            val shortSleep = active ?: completed
            val duration = shortSleep?.let { sleepDurationMinutes(it.startDate, it.entry, now) }
            val sleepTime = shortSleep?.let { if (active != null) "с ${it.entry.startTime}" else "до ${it.entry.endTime}" }
            val sleepAction = if (active == null) "Уснула" else "Проснулась"
            fun compactDuration(minutes: Long): String =
                if (minutes < 60) "${minutes}м" else "${minutes / 60}ч" + if (minutes % 60 == 0L) "" else "${minutes % 60}м"

            val feedingFull = last?.let { (time, entry) ->
                "${entry.name} ${entry.amount.toInt()} мл · ${agoText(Duration.between(time, now).toMinutes())}"
            } ?: "Кормлений пока нет"
            val feedingCompact = last?.let { (time, entry) ->
                "${entry.name}\n${entry.amount.toInt()} мл\n${compactDuration(Duration.between(time, now).toMinutes())} назад"
            } ?: "Еда\nНет записей"
            val portionFull = recommendation?.let { "Расчётная порция: ${it.amountMl} мл" } ?: "Для порции нет расчёта"
            val sleepFull = if (shortSleep != null) "${if (active != null) "Спит" else "Сон"} ${formatSleepDuration(duration ?: 0)} · $sleepTime" else "Сон пока не записан"
            val sleepCompact = if (shortSleep != null) "${if (active != null) "Спит" else "Сон"}\n${compactDuration(duration ?: 0)}\n$sleepTime" else "Сон\nНет записей"
            val vitaminFull = vitamin?.let { "Витамин D: ${it.displayDose()}" } ?: "Витамин D не принят"
            val texts = mapOf(
                R.id.widget_milk_action to ((if (grid) feedingFull else feedingCompact) + "\n＋ Молоко"),
                R.id.widget_formula_action to ((if (grid) portionFull else "Порция\n${recommendation?.let { "${it.amountMl} мл" } ?: "—"}") + "\n＋ Смесь"),
                R.id.widget_sleep_action to ((if (grid) sleepFull + (awake?.let { "\nБодрствует ${formatSleepDuration(it)}" } ?: "") else sleepCompact) + "\n$sleepAction"),
                R.id.widget_vitamin_panel to (if (grid) "$vitaminFull\n${if (vitamin == null) "＋ Отметить" else "Открыть"}" else "D\n${vitamin?.displayDose() ?: "Не принят"}\n${if (vitamin == null) "＋" else "✓"}"),
            )
            texts.forEach { (id, value) -> views.setTextViewText(id, value) }
            views.setContentDescription(R.id.widget_milk_action, "$feedingFull. Добавить молоко")
            views.setContentDescription(R.id.widget_formula_action, "$portionFull. Добавить смесь")
            views.setContentDescription(R.id.widget_sleep_action, "$sleepFull. $sleepAction")
            views.setContentDescription(R.id.widget_vitamin_panel, "$vitaminFull. Открыть ввод витамина D")
            views.setTextColor(R.id.widget_vitamin_panel, if (vitamin != null) Color.rgb(177, 232, 183) else Color.rgb(255, 174, 167))
            val attention = ru.family.rasti.sleep.wakeAttention(awake, LocalStore(context).loadWakeReminderMinutes())
            views.setTextColor(R.id.widget_sleep_action,
                androidx.core.graphics.ColorUtils.blendARGB(Color.WHITE, Color.rgb(255, 174, 167), attention))
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent(context, null, 0))
            views.setOnClickPendingIntent(R.id.widget_milk_action, pendingIntent(context, WidgetAction.MILK, 1))
            views.setOnClickPendingIntent(R.id.widget_formula_action, pendingIntent(context, WidgetAction.FORMULA, 2))
            views.setOnClickPendingIntent(R.id.widget_sleep_action, pendingIntent(context, WidgetAction.SLEEP, 3))
            views.setOnClickPendingIntent(R.id.widget_vitamin_panel, pendingIntent(context, WidgetAction.VITAMIN_D, 4))
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

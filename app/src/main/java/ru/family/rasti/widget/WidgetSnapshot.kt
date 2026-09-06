package ru.family.rasti.widget

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import ru.family.rasti.MainActivity
import ru.family.rasti.data.AppData
import ru.family.rasti.data.FoodEntry
import ru.family.rasti.data.LocalStore
import ru.family.rasti.data.VitaminEntry
import ru.family.rasti.data.displayDose
import ru.family.rasti.feeding.FeedingGuide
import ru.family.rasti.feeding.FeedingMoment
import ru.family.rasti.feeding.SmartFeedingGuide
import ru.family.rasti.feeding.SmartFeedingRecommendation
import ru.family.rasti.sleep.activeSleep
import ru.family.rasti.sleep.awakeMinutes
import ru.family.rasti.sleep.formatSleepDuration
import ru.family.rasti.sleep.lastCompletedSleep
import ru.family.rasti.sleep.sleepDurationMinutes
import ru.family.rasti.sleep.wakeAttention
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

internal data class WidgetSnapshot(
    val feedingFull: String,
    val feedingShort: String,
    val portionFull: String,
    val portionShort: String,
    val sleepFull: String,
    val sleepShort: String,
    val sleepAction: String,
    val vitaminFull: String,
    val vitaminShort: String,
    val vitaminTaken: Boolean,
    val activeSleep: Boolean,
    val awakeMinutes: Long?,
    val wakeAttention: Float,
    val recommendation: SmartFeedingRecommendation?,
)

internal enum class OperationalTone { OK, ATTENTION, ALERT, SLEEP }

internal data class OperationalStatus(
    val title: String,
    val detail: String,
    val tone: OperationalTone,
)

internal fun widgetSnapshot(
    context: Context,
    data: AppData,
    now: LocalDateTime = LocalDateTime.now(),
): WidgetSnapshot {
    val today = now.toLocalDate()
    val guide = FeedingGuide.calculate(data, today).guide
    val recommendation = SmartFeedingGuide.calculate(data, today, guide, now)
    val last = lastFeeding(data, now)
    val active = activeSleep(data, now)
    val completed = lastCompletedSleep(data, now)
    val awake = awakeMinutes(data, now)
    val attention = wakeAttention(awake, LocalStore(context).loadWakeReminderMinutes())
    val vitamin = data.days[today.toString()]?.vitamins.orEmpty()
        .filter(::isVitaminD)
        .filter { runCatching { LocalTime.parse(it.time) <= now.toLocalTime() }.getOrDefault(false) }
        .maxByOrNull { it.time }
    val latestSleep = active ?: completed
    val sleepDuration = latestSleep?.let { sleepDurationMinutes(it.startDate, it.entry, now) }
    val sleepTime = latestSleep?.let { if (active != null) "с ${it.entry.startTime}" else "до ${it.entry.endTime}" }
    val feedingFull = last?.let { (time, entry) ->
        "${entry.name} ${entry.amount.toInt()} мл · ${agoText(Duration.between(time, now).toMinutes())}"
    } ?: "Кормлений пока нет"
    val feedingShort = last?.let { (time, entry) ->
        "${entry.name} ${entry.amount.toInt()} · ${compactDuration(Duration.between(time, now).toMinutes())}"
    } ?: "Еда: нет записей"
    val portionFull = recommendation?.let { "Расчётная порция: ${it.amountMl} мл" } ?: "Для порции нет расчёта"
    val sleepFull = when {
        active != null -> "Спит ${formatSleepDuration(sleepDuration ?: 0)} · $sleepTime"
        awake != null -> "Бодрствует ${formatSleepDuration(awake)} · последний сон $sleepTime"
        latestSleep != null -> "Сон ${formatSleepDuration(sleepDuration ?: 0)} · $sleepTime"
        else -> "Сон пока не записан"
    }
    val sleepShort = when {
        active != null -> "Спит ${compactDuration(sleepDuration ?: 0)}"
        awake != null -> "Бодр. ${compactDuration(awake)}"
        latestSleep != null -> "Сон ${compactDuration(sleepDuration ?: 0)}"
        else -> "Сон —"
    }
    return WidgetSnapshot(
        feedingFull = feedingFull,
        feedingShort = feedingShort,
        portionFull = portionFull,
        portionShort = recommendation?.let { "Порция ${it.amountMl}" } ?: "Порция —",
        sleepFull = sleepFull,
        sleepShort = sleepShort,
        sleepAction = if (active == null) "Уснула" else "Проснулась",
        vitaminFull = vitamin?.let { "Витамин D · ${it.displayDose()}" } ?: "Витамин D не принят",
        vitaminShort = vitamin?.let { "D · ${it.displayDose()}" } ?: "D не принят",
        vitaminTaken = vitamin != null,
        activeSleep = active != null,
        awakeMinutes = awake,
        wakeAttention = attention,
        recommendation = recommendation,
    )
}

internal fun operationalStatus(snapshot: WidgetSnapshot): OperationalStatus {
    if (snapshot.activeSleep) {
        return OperationalStatus("Спит", snapshot.sleepFull.removePrefix("Спит "), OperationalTone.SLEEP)
    }
    if (snapshot.wakeAttention > 0f) {
        return OperationalStatus(
            if (snapshot.wakeAttention >= .65f) "Пора отдыхать" else "Скоро отдыхать",
            snapshot.awakeMinutes?.let { "Бодрствует ${formatSleepDuration(it)}" } ?: snapshot.sleepFull,
            if (snapshot.wakeAttention >= .65f) OperationalTone.ALERT else OperationalTone.ATTENTION,
        )
    }
    val recommendation = snapshot.recommendation
    return when (recommendation?.moment) {
        FeedingMoment.LATER_THAN_USUAL -> OperationalStatus(
            "Пора предложить еду",
            "${snapshot.feedingShort} · порция ${recommendation.amountMl} мл",
            OperationalTone.ALERT,
        )
        FeedingMoment.USUAL_TIME -> OperationalStatus(
            "Можно кормить",
            "${snapshot.feedingShort} · порция ${recommendation.amountMl} мл",
            OperationalTone.ATTENTION,
        )
        FeedingMoment.EARLY -> OperationalStatus(
            "Сейчас всё спокойно",
            recommendation.minutesUntilUsual?.let { "До обычного кормления ≈ ${compactDuration(it.toLong())}" }
                ?: snapshot.feedingShort,
            OperationalTone.OK,
        )
        else -> OperationalStatus("Данных пока мало", "Добавьте кормление или сон", OperationalTone.OK)
    }
}

internal fun widgetPendingIntent(context: Context, action: String?, requestCode: Int): PendingIntent {
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

internal fun compactDuration(minutes: Long): String = when {
    minutes < 60 -> "${minutes}м"
    minutes % 60 == 0L -> "${minutes / 60}ч"
    else -> "${minutes / 60}ч ${minutes % 60}м"
}

private fun agoText(minutes: Long): String = when {
    minutes < 1 -> "сейчас"
    minutes < 60 -> "$minutes мин назад"
    minutes % 60 == 0L -> "${minutes / 60} ч назад"
    else -> "${minutes / 60} ч ${minutes % 60} мин назад"
}

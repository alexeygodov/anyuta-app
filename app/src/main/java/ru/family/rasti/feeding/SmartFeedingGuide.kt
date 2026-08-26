package ru.family.rasti.feeding

import ru.family.rasti.data.AppData
import ru.family.rasti.data.FoodEntry
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlin.math.ceil
import kotlin.math.roundToInt

enum class FeedingMoment {
    EARLY,
    USUAL_TIME,
    LATER_THAN_USUAL,
    NO_HISTORY,
}

data class SmartFeedingRecommendation(
    val amountMl: Int,
    val usualAmountMl: Int,
    val usualIntervalMinutes: Int,
    val minutesSinceLast: Long?,
    val minutesUntilUsual: Int?,
    val remainingToTargetMl: Int,
    val moment: FeedingMoment,
)

object SmartFeedingGuide {
    private const val defaultIntervalMinutes = 210

    fun calculate(
        data: AppData,
        date: LocalDate,
        guide: MilkGuide?,
        now: LocalDateTime = LocalDateTime.now(),
    ): SmartFeedingRecommendation? {
        if (guide == null || date != now.toLocalDate()) return null

        val events = feedingEvents(data, now)
        val recentEvents = events.filter { it.dateTime >= now.minusDays(7) }
        val usualInterval = median(
            recentEvents.zipWithNext { previous, next ->
                Duration.between(previous.dateTime, next.dateTime).toMinutes().toDouble()
            }.filter { it in 90.0..480.0 },
        )?.roundToInt()?.coerceIn(150, 300) ?: defaultIntervalMinutes

        val usualAmount = median(recentEvents.takeLast(20).map { it.entry.amount })
            ?.let(::roundToFive)
            ?: roundToFive(guide.targetMl.toDouble() / (1440.0 / usualInterval).roundToInt().coerceAtLeast(1))

        val todayStart = now.toLocalDate().atStartOfDay()
        val consumedToday = events
            .filter { it.dateTime >= todayStart }
            .sumOf { it.entry.amount }
        val minuteOfDay = now.hour * 60 + now.minute
        val expectedByNow = guide.targetMl * minuteOfDay / 1440.0
        val feedsRemaining = ceil((1440 - minuteOfDay).coerceAtLeast(1) / usualInterval.toDouble())
            .toInt()
            .coerceAtLeast(1)
        val paceCorrection = (expectedByNow - consumedToday) / feedsRemaining * .5
        val lowerBound = usualAmount * .85
        val upperBound = usualAmount * 1.15
        val recommendedAmount = roundToFive((usualAmount + paceCorrection).coerceIn(lowerBound, upperBound))
            .coerceIn(20, 200)

        val last = events.lastOrNull()
        val minutesSinceLast = last?.let { Duration.between(it.dateTime, now).toMinutes() }
        val minutesUntilUsual = minutesSinceLast?.let { (usualInterval - it).coerceAtLeast(0).toInt() }
        val moment = when {
            minutesSinceLast == null -> FeedingMoment.NO_HISTORY
            minutesSinceLast < usualInterval * .75 -> FeedingMoment.EARLY
            minutesSinceLast <= usualInterval * 1.25 -> FeedingMoment.USUAL_TIME
            else -> FeedingMoment.LATER_THAN_USUAL
        }

        return SmartFeedingRecommendation(
            amountMl = recommendedAmount,
            usualAmountMl = usualAmount.coerceIn(20, 200),
            usualIntervalMinutes = usualInterval,
            minutesSinceLast = minutesSinceLast,
            minutesUntilUsual = minutesUntilUsual,
            remainingToTargetMl = (guide.targetMl - consumedToday).roundToInt().coerceAtLeast(0),
            moment = moment,
        )
    }

    private fun feedingEvents(data: AppData, cutoff: LocalDateTime): List<FeedingEvent> =
        data.days.values.asSequence()
            .mapNotNull { day ->
                runCatching { LocalDate.parse(day.date) }.getOrNull()?.let { it to day }
            }
            .flatMap { (date, day) ->
                day.food.asSequence()
                    .filter(::isMeasuredMilk)
                    .filter { it.amount in 20.0..300.0 }
                    .mapNotNull { entry ->
                        runCatching { LocalTime.parse(entry.time) }.getOrNull()?.let { time ->
                            FeedingEvent(date.atTime(time), entry)
                        }
                    }
            }
            .filter { it.dateTime <= cutoff }
            .sortedBy { it.dateTime }
            .toList()

    private fun isMeasuredMilk(entry: FoodEntry): Boolean =
        entry.unit.trim().lowercase() in setOf("мл", "ml") &&
            entry.name.trim().lowercase() in setOf("молоко", "смесь")

    private fun median(values: List<Double>): Double? {
        if (values.isEmpty()) return null
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2.0
    }

    private fun roundToFive(value: Double): Int = (value / 5.0).roundToInt() * 5

    private data class FeedingEvent(val dateTime: LocalDateTime, val entry: FoodEntry)
}

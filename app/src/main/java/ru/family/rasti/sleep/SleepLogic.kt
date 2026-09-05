package ru.family.rasti.sleep

import ru.family.rasti.data.AppData
import ru.family.rasti.data.SleepEntry
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class DatedSleep(val startDate: LocalDate, val entry: SleepEntry)

data class SleepSegment(
    val entry: SleepEntry,
    val startMinute: Int,
    val endMinute: Int,
    val ongoing: Boolean,
)

fun sleepStart(date: LocalDate, entry: SleepEntry): LocalDateTime? =
    runCatching { date.atTime(LocalTime.parse(entry.startTime)) }.getOrNull()

fun sleepEnd(entry: SleepEntry): LocalDateTime? {
    val date = entry.endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
    val time = entry.endTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: return null
    return date.atTime(time)
}

fun sleepDurationMinutes(startDate: LocalDate, entry: SleepEntry, now: LocalDateTime = LocalDateTime.now()): Long? {
    val start = sleepStart(startDate, entry) ?: return null
    val end = sleepEnd(entry) ?: now
    return Duration.between(start, end).toMinutes().takeIf { it >= 0 }
}

fun activeSleep(data: AppData, now: LocalDateTime = LocalDateTime.now()): DatedSleep? = data.days.values.asSequence()
    .flatMap { day ->
        val date = runCatching { LocalDate.parse(day.date) }.getOrNull() ?: return@flatMap emptySequence()
        day.sleeps.asSequence().filter { it.endDate == null || it.endTime == null }.map { DatedSleep(date, it) }
    }
    .filter { sleepStart(it.startDate, it.entry)?.let { start -> start <= now } == true }
    .maxByOrNull { sleepStart(it.startDate, it.entry) ?: LocalDateTime.MIN }

fun lastCompletedSleep(data: AppData, cutoff: LocalDateTime = LocalDateTime.now()): DatedSleep? =
    data.days.values.asSequence()
        .flatMap { day ->
            val date = runCatching { LocalDate.parse(day.date) }.getOrNull() ?: return@flatMap emptySequence()
            day.sleeps.asSequence().map { DatedSleep(date, it) }
        }
        .filter { sleep -> sleepEnd(sleep.entry)?.let { it <= cutoff } == true }
        .maxByOrNull { sleepEnd(it.entry) ?: LocalDateTime.MIN }

fun sleepsForDate(data: AppData, date: LocalDate, now: LocalDateTime = LocalDateTime.now()): List<SleepSegment> {
    val dayStart = date.atStartOfDay()
    val dayEnd = date.plusDays(1).atStartOfDay()
    return data.days.values.asSequence()
        .flatMap { day ->
            val startDate = runCatching { LocalDate.parse(day.date) }.getOrNull() ?: return@flatMap emptySequence()
            day.sleeps.asSequence().map { startDate to it }
        }
        .mapNotNull { (startDate, entry) ->
            val start = sleepStart(startDate, entry) ?: return@mapNotNull null
            val end = sleepEnd(entry) ?: now
            if (end <= dayStart || start >= dayEnd || end < start) return@mapNotNull null
            val clippedStart = maxOf(start, dayStart)
            val clippedEnd = minOf(end, dayEnd)
            SleepSegment(
                entry = entry,
                startMinute = Duration.between(dayStart, clippedStart).toMinutes().toInt().coerceIn(0, 1440),
                endMinute = Duration.between(dayStart, clippedEnd).toMinutes().toInt().coerceIn(0, 1440),
                ongoing = sleepEnd(entry) == null,
            )
        }
        .filter { it.endMinute > it.startMinute }
        .sortedBy { it.startMinute }
        .toList()
}

fun sleepMinutesForDate(data: AppData, date: LocalDate, now: LocalDateTime = LocalDateTime.now()): Long =
    mergedSleepSegments(sleepsForDate(data, date, now)).sumOf { (it.endMinute - it.startMinute).toLong() }

/** Union overlapping records without changing the original entries used for editing. */
fun mergedSleepSegments(segments: List<SleepSegment>): List<SleepSegment> =
    segments.sortedBy { it.startMinute }.fold(mutableListOf<SleepSegment>()) { result, segment ->
        val previous = result.lastOrNull()
        if (previous != null && segment.startMinute <= previous.endMinute) {
            result[result.lastIndex] = previous.copy(
                endMinute = maxOf(previous.endMinute, segment.endMinute),
                ongoing = previous.ongoing || segment.ongoing,
            )
        } else result.add(segment)
        result
    }

fun awakeMinutes(data: AppData, now: LocalDateTime = LocalDateTime.now()): Long? {
    if (activeSleep(data, now) != null) return null
    val end = lastCompletedSleep(data, now)?.entry?.let(::sleepEnd) ?: return null
    // A stale diary is not evidence that the child has been awake for days.
    return Duration.between(end, now).toMinutes().takeIf { it in 0..720 }
}

fun wakeAttention(minutes: Long?, threshold: Int): Float =
    if (minutes == null || threshold <= 0 || minutes <= threshold) 0f
    else ((minutes - threshold) / 30f).coerceIn(0f, 1f)

/** Gaps bounded by recorded sleep; long gaps are unknown rather than presumed wakefulness. */
fun awakeSegmentsForDate(data: AppData, date: LocalDate, now: LocalDateTime): List<Pair<Int, Int>> {
    val intervals = data.days.values.flatMap { day ->
        val startDate = runCatching { LocalDate.parse(day.date) }.getOrNull() ?: return@flatMap emptyList()
        day.sleeps.mapNotNull { entry ->
            val start = sleepStart(startDate, entry) ?: return@mapNotNull null
            val end = minOf(sleepEnd(entry) ?: now, now)
            if (start < end) start to end else null
        }
    }.sortedBy { it.first }.fold(mutableListOf<Pair<LocalDateTime, LocalDateTime>>()) { merged, interval ->
        val previous = merged.lastOrNull()
        if (previous != null && interval.first <= previous.second) {
            merged[merged.lastIndex] = previous.first to maxOf(previous.second, interval.second)
        } else merged.add(interval)
        merged
    }
    val gaps = intervals.zipWithNext().map { (first, second) -> first.second to second.first }.toMutableList()
    intervals.lastOrNull()?.let { if (it.second < now) gaps.add(it.second to now) }
    val dayStart = date.atStartOfDay()
    val dayEnd = minOf(date.plusDays(1).atStartOfDay(), now)
    return gaps.filter { Duration.between(it.first, it.second).toMinutes() in 1..720 }
        .mapNotNull { (start, end) ->
            val clippedStart = maxOf(start, dayStart)
            val clippedEnd = minOf(end, dayEnd)
            if (clippedStart >= clippedEnd) null else
                Duration.between(dayStart, clippedStart).toMinutes().toInt() to
                    Duration.between(dayStart, clippedEnd).toMinutes().toInt()
        }
}

fun formatSleepDuration(minutes: Long): String = when {
    minutes < 60 -> "$minutes мин"
    minutes % 60 == 0L -> "${minutes / 60} ч"
    else -> "${minutes / 60} ч ${minutes % 60} мин"
}

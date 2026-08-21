package ru.family.rasti.notify

import ru.family.rasti.data.DayRecord
import java.time.LocalDate
import java.time.LocalTime

val SUMMARY_TIME: LocalTime = LocalTime.of(21, 0)

private val MONTHS_GENITIVE = arrayOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря",
)

internal fun buildDaySummary(day: DayRecord, date: LocalDate): String {
    val lines = mutableListOf("📋 Итог дня — ${date.dayOfMonth} ${MONTHS_GENITIVE[date.monthValue - 1]}")

    val milk = day.food
        .filter { isMilkEntry(it.name, it.unit) }
        .sortedBy { it.time }
    if (milk.isEmpty()) {
        lines += "🍼 Кормлений не было"
    } else {
        val total = milk.sumOf { it.amount }
        lines += "🍼 Кормлений: ${milk.size} · всего ${formatAmount(total)} мл"
        val last = milk.last()
        lines += "🕘 Последнее: ${last.name} ${formatAmount(last.amount)} мл в ${last.time}"
    }

    val vitamin = day.vitamins
        .filter { isVitaminDName(it.name) }
        .maxByOrNull { it.time }
    lines += if (vitamin != null) {
        "💊 Витамин D: принят в ${vitamin.time}"
    } else {
        "💊 Витамин D: не принят"
    }
    return lines.joinToString("\n")
}

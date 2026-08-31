package ru.family.rasti.notify

import ru.family.rasti.data.AppData
import ru.family.rasti.data.displayDose
import ru.family.rasti.sleep.formatSleepDuration
import ru.family.rasti.sleep.sleepDurationMinutes
import java.time.LocalDate

data class SyncUpdate(val title: String, val text: String)

fun collectSyncUpdates(before: AppData, after: AppData, today: LocalDate): List<SyncUpdate> {
    val updates = mutableListOf<SyncUpdate>()
    listOf(today, today.minusDays(1)).forEach { date ->
        val beforeDay = before.days[date.toString()]
        val afterDay = after.days[date.toString()] ?: return@forEach

        val knownFoodIds = beforeDay?.food?.mapTo(HashSet()) { it.id } ?: emptySet()
        afterDay.food
            .filter { it.id !in knownFoodIds && isMilkEntry(it.name, it.unit) }
            .sortedBy { it.time }
            .forEach { entry ->
                updates += SyncUpdate(
                    title = "Новое кормление",
                    text = "${entry.name} · ${formatAmount(entry.amount)} ${entry.unit} · ${entry.time}",
                )
            }

        val knownVitaminIds = beforeDay?.vitamins?.mapTo(HashSet()) { it.id } ?: emptySet()
        afterDay.vitamins
            .filter { it.id !in knownVitaminIds && isVitaminDName(it.name) }
            .sortedBy { it.time }
            .forEach { entry ->
                updates += SyncUpdate(
                    title = "Витамин D принят",
                    text = "${entry.displayDose()} · ${entry.time}",
                )
            }

        val knownSleeps = beforeDay?.sleeps?.associateBy { it.id }.orEmpty()
        afterDay.sleeps.sortedBy { it.startTime }.forEach { sleep ->
            val previous = knownSleeps[sleep.id]
            when {
                previous == null && sleep.endTime == null -> updates += SyncUpdate(
                    title = "Сон начался",
                    text = "Уснула в ${sleep.startTime}",
                )
                previous == null || (previous.endTime == null && sleep.endTime != null) -> {
                    val duration = sleepDurationMinutes(date, sleep)
                    updates += SyncUpdate(
                        title = "Сон завершён",
                        text = buildString {
                            append("${sleep.startTime}–${sleep.endTime}")
                            duration?.let { append(" · ${formatSleepDuration(it)}") }
                        },
                    )
                }
            }
        }
    }
    return updates
}

internal fun formatAmount(amount: Double): String =
    if (amount % 1.0 == 0.0) amount.toInt().toString() else amount.toString().replace('.', ',')

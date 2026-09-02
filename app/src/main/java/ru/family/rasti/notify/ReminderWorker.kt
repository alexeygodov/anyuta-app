package ru.family.rasti.notify

import android.content.Context
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ru.family.rasti.data.AppData
import ru.family.rasti.data.LocalStore
import ru.family.rasti.data.day
import ru.family.rasti.sync.GitHubSync
import ru.family.rasti.sync.decodeSyncState
import ru.family.rasti.sync.encodeSyncState
import ru.family.rasti.widget.AnyutaDashboardWidget
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

private const val PREFS_NAME = "reminder_state"
private const val KEY_FEEDING_NOTIFIED = "feeding_notified_at"
private const val KEY_VITAMIN_NOTIFIED = "vitamin_notified_on"
private const val KEY_SUMMARY_SENT = "summary_sent_on"
private const val WORK_NAME = "rasti-reminders"
private const val BACKGROUND_SYNC_GAP_MS = 3 * 60_000L

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val notifier = ReminderNotifier(applicationContext)
        val max = MaxMessenger(applicationContext)
        val store = LocalStore(applicationContext)
        val data = store.loadData()
        val notificationPreferences = store.loadNotificationPreferences()
        val now = LocalDateTime.now()
        checkFeeding(notifier, max, data, now, notificationPreferences.feedingReminders)
        checkVitamin(notifier, max, data, now, notificationPreferences.vitaminReminders)
        maybeSendDaySummary(max, data, now)
        maybeSyncInBackground(notifier, max, store, data)
        AnyutaDashboardWidget.updateAll(applicationContext)
        return Result.success()
    }

    private fun checkFeeding(
        notifier: ReminderNotifier,
        max: MaxMessenger,
        data: AppData,
        now: LocalDateTime,
        notifyOnPhone: Boolean,
    ) {
        val last = feedingReminderDue(data, now) ?: return
        val key = last.toString()
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_FEEDING_NOTIFIED, null) == key) return
        val elapsed = formatElapsed(Duration.between(last, now).toMinutes())
        if (notifyOnPhone) {
            notifier.notify(
                ID_FEEDING_REMINDER,
                "Пора покормить",
                "Прошло $elapsed с последнего кормления",
            )
        }
        runCatching { max.sendText("⏰ Прошло $elapsed с последнего кормления") }
        prefs.edit { putString(KEY_FEEDING_NOTIFIED, key) }
    }

    private fun checkVitamin(
        notifier: ReminderNotifier,
        max: MaxMessenger,
        data: AppData,
        now: LocalDateTime,
        notifyOnPhone: Boolean,
    ) {
        if (!vitaminReminderDue(data, now)) return
        val key = now.toLocalDate().toString()
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_VITAMIN_NOTIFIED, null) == key) return
        if (notifyOnPhone) {
            notifier.notify(
                ID_VITAMIN_REMINDER,
                "Витамин D",
                "Уже полдень, а витамин D сегодня ещё не принят",
            )
        }
        runCatching { max.sendText("💊 Уже полдень, а витамин D сегодня ещё не принят") }
        prefs.edit { putString(KEY_VITAMIN_NOTIFIED, key) }
    }

    private fun maybeSendDaySummary(max: MaxMessenger, data: AppData, now: LocalDateTime) {
        if (!max.isConfigured()) return
        if (now.toLocalTime() < SUMMARY_TIME) return
        val key = now.toLocalDate().toString()
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_SUMMARY_SENT, null) == key) return
        val sent = runCatching {
            max.sendText(buildDaySummary(data.day(now.toLocalDate()), now.toLocalDate()))
        }.isSuccess
        if (sent) prefs.edit { putString(KEY_SUMMARY_SENT, key) }
    }

    private fun maybeSyncInBackground(notifier: ReminderNotifier, max: MaxMessenger, store: LocalStore, before: AppData) {
        if (System.currentTimeMillis() - notifier.lastSyncAt() < BACKGROUND_SYNC_GAP_MS) return
        val config = store.loadGitHubConfig()
        if (config.owner.isBlank() || config.repo.isBlank() || config.token.isBlank()) return
        val syncer = GitHubSync()
        val result = runCatching { syncer.sync(config, before, decodeSyncState(store.loadSyncState())) }
            .getOrNull() ?: return
        val merged = syncer.merge(before, result.data)
        store.saveData(merged)
        store.saveSyncState(encodeSyncState(result.state))
        notifier.markSyncedNow()
        val updates = collectSyncUpdates(before, merged, LocalDate.now())
        if (updates.isNotEmpty()) {
            notifier.notifySyncUpdates(updates)
            runCatching { max.announceEvents(updates) }
        }
    }
}

object ReminderScheduler {
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}

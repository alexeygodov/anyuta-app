package ru.family.rasti.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ru.family.rasti.MainActivity
import ru.family.rasti.R
import ru.family.rasti.data.AppData
import ru.family.rasti.data.LocalStore
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID = "reminders"
private const val PREFS_NAME = "reminder_state"
private const val KEY_FEEDING_NOTIFIED = "feeding_notified_at"
private const val KEY_VITAMIN_NOTIFIED = "vitamin_notified_on"
private const val ID_FEEDING = 1
private const val ID_VITAMIN = 2
private const val WORK_NAME = "rasti-reminders"

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        ensureChannel()
        val data = LocalStore(applicationContext).loadData()
        val now = LocalDateTime.now()
        checkFeeding(data, now)
        checkVitamin(data, now)
        return Result.success()
    }

    private fun checkFeeding(data: AppData, now: LocalDateTime) {
        val last = feedingReminderDue(data, now) ?: return
        val key = last.toString()
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_FEEDING_NOTIFIED, null) == key) return
        val elapsed = formatElapsed(Duration.between(last, now).toMinutes())
        notify(
            ID_FEEDING,
            "Пора покормить",
            "Прошло $elapsed с последнего кормления",
        )
        prefs.edit { putString(KEY_FEEDING_NOTIFIED, key) }
    }

    private fun checkVitamin(data: AppData, now: LocalDateTime) {
        if (!vitaminReminderDue(data, now)) return
        val key = now.toLocalDate().toString()
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_VITAMIN_NOTIFIED, null) == key) return
        notify(
            ID_VITAMIN,
            "Витамин D",
            "Уже полдень, а витамин D сегодня ещё не принят",
        )
        prefs.edit { putString(KEY_VITAMIN_NOTIFIED, key) }
    }

    private fun notify(id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = NotificationManagerCompat.from(applicationContext)
        if (!manager.areNotificationsEnabled()) return
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            applicationContext,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching { manager.notify(id, notification) }
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Напоминания", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Напоминания о кормлении и витамине D"
        }
        applicationContext.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }
}

object ReminderScheduler {
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}

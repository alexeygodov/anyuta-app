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
import ru.family.rasti.MainActivity
import ru.family.rasti.R

private const val CHANNEL_ID = "reminders"
private const val PREFS_NAME = "reminder_state"
private const val KEY_BADGE = "badge_count"
private const val KEY_NEXT_SYNC_ID = "next_sync_notification_id"
private const val KEY_LAST_SYNC = "last_sync_at"

const val ID_FEEDING_REMINDER = 1
const val ID_VITAMIN_REMINDER = 2
private const val ID_SYNC_BASE = 100
private const val ID_SYNC_RANGE = 100

class ReminderNotifier(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun notify(id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        ensureChannel()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setNumber(nextBadge())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching { manager.notify(id, notification) }
    }

    fun notifySyncUpdates(updates: List<SyncUpdate>) {
        updates.forEach { update -> notify(nextSyncId(), update.title, update.text) }
    }

    fun clearAll() {
        NotificationManagerCompat.from(context).cancelAll()
        prefs.edit { putInt(KEY_BADGE, 0) }
    }

    fun markSyncedNow() {
        prefs.edit { putLong(KEY_LAST_SYNC, System.currentTimeMillis()) }
    }

    fun lastSyncAt(): Long = prefs.getLong(KEY_LAST_SYNC, 0L)

    private fun ensureChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Напоминания", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Напоминания о кормлении и витамине D"
            setShowBadge(true)
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    private fun nextBadge(): Int {
        val next = prefs.getInt(KEY_BADGE, 0) + 1
        prefs.edit { putInt(KEY_BADGE, next) }
        return next
    }

    private fun nextSyncId(): Int {
        val current = prefs.getInt(KEY_NEXT_SYNC_ID, ID_SYNC_BASE)
        val next = if (current >= ID_SYNC_BASE + ID_SYNC_RANGE - 1) ID_SYNC_BASE else current + 1
        prefs.edit { putInt(KEY_NEXT_SYNC_ID, next) }
        return current
    }
}

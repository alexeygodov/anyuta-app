package ru.family.rasti.data

import android.content.Context
import androidx.core.content.edit
import java.io.File
import ru.family.rasti.widget.AnyutaDashboardWidget

class LocalStore(private val context: Context) {
    private val journal = JournalFile(context.filesDir)
    private val syncStateFile = File(context.filesDir, "rasti-sync-state.json")
    private val settings = context.getSharedPreferences("github_settings", Context.MODE_PRIVATE)
    private val tokenStore = SecureTokenStore(context)
    private val maxTokenStore = SecureTokenStore(context, "max_secret", "rasti.max.token")

    fun loadData(): AppData = journal.read()

    fun wasDataRecovered(): Boolean = journal.wasRecovered()

    fun acknowledgeDataRecovery() {
        journal.acknowledgeRecovery()
        AnyutaDashboardWidget.updateAll(context)
    }

    fun saveData(data: AppData) {
        journal.write(data)
        AnyutaDashboardWidget.updateAll(context)
    }

    fun loadSyncState(): String? = runCatching {
        if (syncStateFile.exists()) syncStateFile.readText() else null
    }.getOrNull()

    fun saveSyncState(raw: String) {
        JournalFile.writeAtomic(syncStateFile, raw)
    }

    fun loadGitHubConfig(): GitHubConfig = GitHubConfig(
        owner = settings.getString("owner", "alexeygodov") ?: "alexeygodov",
        repo = settings.getString("repo", "anyuta-data") ?: "anyuta-data",
        branch = settings.getString("branch", "main") ?: "main",
        token = tokenStore.load(),
    )

    fun saveGitHubConfig(config: GitHubConfig) {
        settings.edit {
            putString("owner", config.owner.trim())
            putString("repo", config.repo.trim())
            putString("branch", config.branch.trim().ifBlank { "main" })
        }
        tokenStore.save(config.token.trim())
    }

    fun loadMaxConfig(): MaxConfig = MaxConfig(
        enabled = settings.getBoolean("max_enabled", false),
        token = maxTokenStore.load(),
        chatId = settings.getString("max_chat_id", "") ?: "",
    )

    fun saveMaxConfig(config: MaxConfig) {
        settings.edit {
            putBoolean("max_enabled", config.enabled)
            putString("max_chat_id", config.chatId.trim())
        }
        maxTokenStore.save(config.token.trim())
    }

    fun loadNotificationPreferences(): NotificationPreferences = NotificationPreferences(
        feedingReminders = settings.getBoolean("notification_feeding_reminders", true),
        vitaminReminders = settings.getBoolean("notification_vitamin_reminders", true),
        syncUpdates = settings.getBoolean("notification_sync_updates", true),
    )

    fun saveNotificationPreferences(preferences: NotificationPreferences) {
        settings.edit {
            putBoolean("notification_feeding_reminders", preferences.feedingReminders)
            putBoolean("notification_vitamin_reminders", preferences.vitaminReminders)
            putBoolean("notification_sync_updates", preferences.syncUpdates)
        }
    }

    fun loadAppTheme(): AppTheme = parseAppTheme(settings.getString("app_theme", null))

    fun loadWakeReminderMinutes(): Int = settings.getInt("wake_reminder_minutes", 120).coerceIn(0, 300)

    fun saveWakeReminderMinutes(minutes: Int) {
        settings.edit { putInt("wake_reminder_minutes", minutes.coerceIn(0, 300)) }
    }

    fun saveAppTheme(theme: AppTheme) {
        settings.edit { putString("app_theme", theme.name) }
    }
}

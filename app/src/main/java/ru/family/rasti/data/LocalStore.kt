package ru.family.rasti.data

import android.content.Context
import androidx.core.content.edit
import java.io.File

class LocalStore(private val context: Context) {
    private val dataFile = File(context.filesDir, "rasti-data.json")
    private val syncStateFile = File(context.filesDir, "rasti-sync-state.json")
    private val settings = context.getSharedPreferences("github_settings", Context.MODE_PRIVATE)
    private val tokenStore = SecureTokenStore(context)
    private val maxTokenStore = SecureTokenStore(context, "max_secret", "rasti.max.token")

    fun loadData(): AppData = runCatching {
        if (!dataFile.exists()) AppData() else JsonCodec.decodeAppData(dataFile.readText())
    }.getOrDefault(AppData())

    fun saveData(data: AppData) {
        val temporary = File(context.filesDir, "rasti-data.tmp")
        temporary.writeText(JsonCodec.encodeAppData(data))
        if (!temporary.renameTo(dataFile)) {
            dataFile.writeText(temporary.readText())
            temporary.delete()
        }
    }

    fun loadSyncState(): String? = runCatching {
        if (syncStateFile.exists()) syncStateFile.readText() else null
    }.getOrNull()

    fun saveSyncState(raw: String) {
        val temporary = File(context.filesDir, "rasti-sync-state.tmp")
        temporary.writeText(raw)
        if (!temporary.renameTo(syncStateFile)) {
            syncStateFile.writeText(temporary.readText())
            temporary.delete()
        }
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
}

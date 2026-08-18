package ru.family.rasti.data

import android.content.Context
import androidx.core.content.edit
import java.io.File

class LocalStore(private val context: Context) {
    private val dataFile = File(context.filesDir, "rasti-data.json")
    private val settings = context.getSharedPreferences("github_settings", Context.MODE_PRIVATE)
    private val tokenStore = SecureTokenStore(context)

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

    fun loadGitHubConfig(): GitHubConfig = GitHubConfig(
        owner = settings.getString("owner", "") ?: "",
        repo = settings.getString("repo", "") ?: "",
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
}

package ru.family.rasti.notify

import android.content.Context
import ru.family.rasti.data.LocalStore
import ru.family.rasti.max.MaxBotApi

class MaxMessenger(context: Context) {
    private val store = LocalStore(context.applicationContext)

    fun isConfigured(): Boolean = store.loadMaxConfig().isConfigured()

    fun sendText(text: String) {
        val config = store.loadMaxConfig()
        if (!config.isConfigured()) return
        val chatId = config.chatId.toLongOrNull() ?: return
        MaxBotApi(config.token).sendMessage(chatId, text)
    }

    fun announceEvents(updates: List<SyncUpdate>) {
        if (updates.isEmpty() || !isConfigured()) return
        updates.forEachIndexed { index, update ->
            if (index > 0) Thread.sleep(600)
            sendText("${emojiFor(update.title)} ${update.title}: ${update.text}")
        }
    }

    private fun emojiFor(title: String): String = when {
        title.contains("кормление", ignoreCase = true) -> "🍼"
        title.contains("Витамин", ignoreCase = true) -> "💊"
        else -> "🔔"
    }
}

package ru.family.rasti.max

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets

private const val BASE_URL = "https://platform-api2.max.ru"

data class MaxChat(val id: Long, val title: String)

class MaxBotApi(private val token: String) {

    fun getMe(): String {
        val json = JSONObject(request("GET", "$BASE_URL/me"))
        return json.optString("name").ifBlank { "бот" }
    }

    fun findChats(): List<MaxChat> {
        val json = JSONObject(request("GET", "$BASE_URL/updates?timeout=0&limit=100"))
        val updates = json.optJSONArray("updates") ?: return emptyList()
        val chats = LinkedHashMap<Long, String>()
        for (i in 0 until updates.length()) {
            val update = updates.optJSONObject(i) ?: continue
            val directId = update.optLong("chat_id", 0L)
            if (directId != 0L) {
                chats[directId] = update.optString("chat_title").ifBlank { "Чат $directId" }
                continue
            }
            val recipient = update.optJSONObject("message")?.optJSONObject("recipient") ?: continue
            val chatId = recipient.optLong("chat_id", 0L)
            if (chatId != 0L) {
                chats[chatId] = recipient.optString("chat_title").ifBlank { "Чат $chatId" }
            }
        }
        return chats.map { (id, title) -> MaxChat(id, title) }
    }

    fun sendMessage(chatId: Long, text: String) {
        val body = JSONObject()
            .put("text", text.take(4000))
            .put("disable_link_preview", true)
            .toString()
        request("POST", "$BASE_URL/messages?chat_id=$chatId", body)
    }

    private fun request(method: String, url: String, body: String? = null): String {
        val connection = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("Authorization", token)
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) {
                connection.outputStream.use { it.write(body.toByteArray(StandardCharsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.readBytes()?.toString(StandardCharsets.UTF_8).orEmpty()
            if (code !in 200..299) {
                val message = runCatching { JSONObject(text).optString("message") }.getOrDefault("")
                throw IOException("MAX API $code: ${message.ifBlank { text.take(200) }}")
            }
            return text
        } finally {
            connection.disconnect()
        }
    }
}

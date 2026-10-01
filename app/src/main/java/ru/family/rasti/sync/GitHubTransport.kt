package ru.family.rasti.sync

import ru.family.rasti.data.GitHubConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class GitHubResponse(val status: Int, val body: String, val etag: String? = null)

fun interface GitHubTransport {
    fun execute(
        config: GitHubConfig,
        method: String,
        endpoint: String,
        body: String?,
        headers: Map<String, String>,
    ): GitHubResponse
}

internal class GitHubHttpTransport : GitHubTransport {
    override fun execute(
        config: GitHubConfig,
        method: String,
        endpoint: String,
        body: String?,
        headers: Map<String, String>,
    ): GitHubResponse {
        val url = "https://api.github.com/repos/${encode(config.owner)}/${encode(config.repo)}$endpoint"
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("Authorization", "Bearer ${config.token}")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            connection.setRequestProperty("User-Agent", "Anyuta-Android")
            headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            GitHubResponse(
                status,
                stream?.bufferedReader()?.use { it.readText() }.orEmpty(),
                connection.getHeaderField("ETag"),
            )
        } catch (error: IOException) {
            throw IOException("Не удалось подключиться к GitHub", error)
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20")
}

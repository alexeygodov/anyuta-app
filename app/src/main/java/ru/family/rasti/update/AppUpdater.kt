package ru.family.rasti.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest

data class UpdateInfo(
    val versionName: String,
    val downloadUrl: String,
    val assetName: String,
    val sha256: String? = null,
)

object AppUpdater {
    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/alexeygodov/anyuta-app/releases/latest"

    fun check(currentVersion: String, token: String = ""): UpdateInfo? {
        val connection = URI(LATEST_RELEASE_URL).toURL().openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            connection.setRequestProperty("User-Agent", "Anyuta-Android")
            if (token.isNotBlank()) connection.setRequestProperty("Authorization", "Bearer $token")
            val status = connection.responseCode
            val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status == 404) throw IOException("Релиз не найден или токен не имеет доступа к anyuta-app")
            if (status !in 200..299) throw IOException("GitHub вернул ошибку $status")
            val json = JSONObject(body)
            val version = json.getString("tag_name").removePrefix("v")
            val assets = json.getJSONArray("assets")
            val apk = (0 until assets.length())
                .asSequence()
                .map { assets.getJSONObject(it) }
                .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
                ?: throw IOException("В новой версии не найден APK")
            if (!isVersionNewer(version, currentVersion)) return null
            UpdateInfo(
                versionName = version,
                downloadUrl = apk.getString("url"),
                assetName = apk.getString("name"),
                sha256 = apk.optString("digest").removePrefix("sha256:").takeIf { it.length == 64 },
            )
        } catch (error: IOException) {
            throw error
        } catch (error: Exception) {
            throw IOException("Не удалось проверить обновление: ${error.message}", error)
        } finally {
            connection.disconnect()
        }
    }

    fun download(context: Context, update: UpdateInfo, token: String = ""): File {
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(directory, "anyuta-${update.versionName}.apk")
        var connection = openDownloadConnection(update.downloadUrl, token)
        return try {
            var status = connection.responseCode
            if (status in setOf(301, 302, 303, 307, 308)) {
                val redirect = connection.getHeaderField("Location")
                    ?: throw IOException("GitHub не вернул адрес APK")
                connection.disconnect()
                connection = openDownloadConnection(redirect, "")
                status = connection.responseCode
            }
            if (status !in 200..299) throw IOException("Не удалось скачать APK: HTTP $status")
            connection.inputStream.use { input -> target.outputStream().use { output -> input.copyTo(output) } }
            if (target.length() < 100_000L) throw IOException("Скачанный APK повреждён или слишком мал")
            update.sha256?.let { expected ->
                val actual = target.inputStream().use { input ->
                    val digest = MessageDigest.getInstance("SHA-256")
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        digest.update(buffer, 0, count)
                    }
                    digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
                }
                if (!actual.equals(expected, ignoreCase = true)) throw IOException("Не совпала контрольная сумма APK")
            }
            target
        } catch (error: Exception) {
            target.delete()
            if (error is IOException) throw error
            throw IOException("Не удалось скачать обновление: ${error.message}", error)
        } finally {
            connection.disconnect()
        }
    }

    private fun openDownloadConnection(url: String, token: String): HttpURLConnection =
        (URI(url).toURL().openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            connectTimeout = 20_000
            readTimeout = 60_000
            setRequestProperty("Accept", "application/octet-stream")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "Anyuta-Android")
            if (token.isNotBlank()) setRequestProperty("Authorization", "Bearer $token")
        }

    fun requestInstall(context: Context, apk: File): Boolean {
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        return true
    }
}

internal fun isVersionNewer(candidate: String, current: String): Boolean {
    val candidateParts = candidate.split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
    val currentParts = current.split('.', '-', '+').map { it.toIntOrNull() ?: 0 }
    val size = maxOf(candidateParts.size, currentParts.size)
    for (index in 0 until size) {
        val left = candidateParts.getOrElse(index) { 0 }
        val right = currentParts.getOrElse(index) { 0 }
        if (left != right) return left > right
    }
    return false
}

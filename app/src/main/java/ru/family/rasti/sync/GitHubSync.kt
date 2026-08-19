package ru.family.rasti.sync

import android.util.Base64
import org.json.JSONObject
import ru.family.rasti.data.AppData
import ru.family.rasti.data.ChildProfile
import ru.family.rasti.data.DayRecord
import ru.family.rasti.data.FoodEntry
import ru.family.rasti.data.GitHubConfig
import ru.family.rasti.data.JsonCodec
import ru.family.rasti.data.Measurement
import ru.family.rasti.data.VitaminEntry
import ru.family.rasti.data.VaccinationEntry
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class SyncResult(val data: AppData, val uploadedFiles: Int, val downloadedFiles: Int)

class GitHubSync {
    private data class RemoteFile(val path: String, val sha: String)
    private data class Content(val raw: String, val sha: String)

    fun sync(config: GitHubConfig, local: AppData): SyncResult {
        require(config.owner.isNotBlank()) { "Укажите владельца репозитория" }
        require(config.repo.isNotBlank()) { "Укажите репозиторий данных" }
        require(config.token.isNotBlank()) { "Вставьте GitHub token" }

        val tree = listTree(config)
        var downloaded = 0
        var profile = local.profile
        val remoteProfile = getContent(config, "profile.json")?.let {
            downloaded += 1
            JsonCodec.decodeProfile(it.raw)
        }
        if (remoteProfile != null) profile = newerProfile(profile, remoteProfile)

        val remoteDays = mutableMapOf<String, DayRecord>()
        tree.filter { it.path.matches(Regex("data/\\d{4}/\\d{2}/\\d{4}-\\d{2}-\\d{2}\\.json")) }
            .forEach { file ->
                val content = getContent(config, file.path) ?: return@forEach
                remoteDays[file.path] = JsonCodec.decodeDay(content.raw)
                downloaded += 1
            }

        val mergedDays = local.days.toMutableMap()
        remoteDays.forEach { (_, remote) ->
            mergedDays[remote.date] = mergedDays[remote.date]?.let { mergeDay(it, remote) } ?: remote
        }
        val merged = AppData(profile = profile, days = mergedDays)

        var uploaded = 0
        if (remoteProfile != profile) {
            val sha = tree.firstOrNull { it.path == "profile.json" }?.sha
                ?: getContent(config, "profile.json")?.sha
            putContent(config, "profile.json", JsonCodec.encodeProfile(profile), sha)
            uploaded += 1
        }

        merged.days.values.sortedBy { it.date }.forEach { day ->
            val path = dayPath(day.date)
            val remote = remoteDays[path]
            if (remote != day) {
                val sha = tree.firstOrNull { it.path == path }?.sha
                ?: getContent(config, path)?.sha
                putWithConflictRetry(config, path, day, sha)
                uploaded += 1
            }
        }
        return SyncResult(merged, uploaded, downloaded)
    }

    fun merge(first: AppData, second: AppData): AppData {
        val days = first.days.toMutableMap()
        second.days.forEach { (date, day) ->
            days[date] = days[date]?.let { mergeDay(it, day) } ?: day
        }
        return AppData(
            profile = newerProfile(first.profile, second.profile),
            days = days,
        )
    }

    private fun putWithConflictRetry(config: GitHubConfig, path: String, day: DayRecord, sha: String?) {
        try {
            putContent(config, path, JsonCodec.encodeDay(day), sha)
        } catch (error: GitHubException) {
            if (error.status != 409) throw error
            val latest = getContent(config, path) ?: throw error
            val merged = mergeDay(JsonCodec.decodeDay(latest.raw), day)
            putContent(config, path, JsonCodec.encodeDay(merged), latest.sha)
        }
    }

    private fun dayPath(date: String): String {
        val year = date.substring(0, 4)
        val month = date.substring(5, 7)
        return "data/$year/$month/$date.json"
    }

    private fun listTree(config: GitHubConfig): List<RemoteFile> {
        val branch = encode(config.branch)
        val response = request(config, "GET", "/git/trees/$branch?recursive=1", null, setOf(404, 409))
        if (response.status == 404 || response.status == 409) return emptyList()
        val array = JSONObject(response.body).getJSONArray("tree")
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                if (item.optString("type") == "blob") {
                    add(RemoteFile(item.getString("path"), item.getString("sha")))
                }
            }
        }
    }

    private fun getContent(config: GitHubConfig, path: String): Content? {
        val response = request(config, "GET", "/contents/${path.split('/').joinToString("/") { encode(it) }}?ref=${encode(config.branch)}", null, setOf(404))
        if (response.status == 404) return null
        val json = JSONObject(response.body)
        val raw = String(Base64.decode(json.getString("content"), Base64.DEFAULT))
        return Content(raw = raw, sha = json.getString("sha"))
    }

    private fun putContent(config: GitHubConfig, path: String, raw: String, sha: String?) {
        val body = JSONObject()
            .put("message", "Анюта: обновление $path")
            .put("content", Base64.encodeToString(raw.toByteArray(), Base64.NO_WRAP))
            .put("branch", config.branch)
            .apply { sha?.let { put("sha", it) } }
            .toString()
        request(config, "PUT", "/contents/${path.split('/').joinToString("/") { encode(it) }}", body)
    }

    private data class Response(val status: Int, val body: String)

    private fun request(
        config: GitHubConfig,
        method: String,
        endpoint: String,
        body: String?,
        allowedErrors: Set<Int> = emptySet(),
    ): Response {
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
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toByteArray()) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299 && status !in allowedErrors) {
                val message = runCatching { JSONObject(responseBody).optString("message") }.getOrNull()
                    .orEmpty().ifBlank { "HTTP $status" }
                throw GitHubException(status, message)
            }
            Response(status, responseBody)
        } catch (error: GitHubException) {
            throw error
        } catch (error: IOException) {
            throw IOException("Не удалось подключиться к GitHub: ${error.message}", error)
        } finally {
            connection.disconnect()
        }
    }

    private fun newerProfile(first: ChildProfile, second: ChildProfile): ChildProfile =
        if (first.updatedAt >= second.updatedAt) first else second

    private fun mergeDay(first: DayRecord, second: DayRecord): DayRecord {
        val firstNewer = first.updatedAt >= second.updatedAt
        val deletedFoodIds = first.deletedFoodIds + second.deletedFoodIds
        val deletedVitaminIds = first.deletedVitaminIds + second.deletedVitaminIds
        val deletedVaccinationIds = first.deletedVaccinationIds + second.deletedVaccinationIds
        val measurementDeletedAt = listOfNotNull(first.measurementDeletedAt, second.measurementDeletedAt).maxOrNull()
        val newestMeasurement = newerMeasurement(first.measurement, second.measurement)
        val visibleMeasurement = newestMeasurement?.takeIf { measurement ->
            measurementDeletedAt == null || measurement.updatedAt > measurementDeletedAt
        }
        return DayRecord(
            date = first.date,
            food = mergeById(first.food, second.food, FoodEntry::id, FoodEntry::updatedAt)
                .filterNot { it.id in deletedFoodIds },
            vitamins = mergeById(first.vitamins, second.vitamins, VitaminEntry::id, VitaminEntry::updatedAt)
                .filterNot { it.id in deletedVitaminIds },
            vaccinations = mergeById(
                first.vaccinations,
                second.vaccinations,
                VaccinationEntry::id,
                VaccinationEntry::updatedAt,
            ).filterNot { it.id in deletedVaccinationIds },
            deletedFoodIds = deletedFoodIds,
            deletedVitaminIds = deletedVitaminIds,
            deletedVaccinationIds = deletedVaccinationIds,
            measurement = visibleMeasurement,
            measurementDeletedAt = measurementDeletedAt,
            note = if (firstNewer) first.note else second.note,
            updatedAt = maxOf(first.updatedAt, second.updatedAt),
        )
    }

    private fun newerMeasurement(first: Measurement?, second: Measurement?): Measurement? = when {
        first == null -> second
        second == null -> first
        first.updatedAt >= second.updatedAt -> first
        else -> second
    }

    private fun <T> mergeById(
        first: List<T>,
        second: List<T>,
        id: (T) -> String,
        updatedAt: (T) -> String,
    ): List<T> = (first + second)
        .groupBy(id)
        .values
        .map { duplicates -> duplicates.maxBy(updatedAt) }
        .sortedBy(updatedAt)

    private fun encode(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.toString()).replace("+", "%20")
}

class GitHubException(val status: Int, message: String) : IOException("GitHub: $message ($status)")

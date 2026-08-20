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
import ru.family.rasti.data.isPlaceholder
import ru.family.rasti.data.VitaminEntry
import ru.family.rasti.data.VaccinationEntry
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class SyncResult(
    val data: AppData,
    val uploadedFiles: Int,
    val downloadedFiles: Int,
    val state: SyncState,
)

data class SyncState(
    val etag: String? = null,
    val files: Map<String, RemoteState> = emptyMap(),
)

data class RemoteState(val sha: String, val updatedAt: String? = null)

fun encodeSyncState(state: SyncState): String {
    val files = JSONObject()
    state.files.forEach { (path, file) ->
        files.put(
            path,
            JSONObject()
                .put("sha", file.sha)
                .put("updatedAt", file.updatedAt ?: JSONObject.NULL),
        )
    }
    return JSONObject()
        .put("etag", state.etag ?: JSONObject.NULL)
        .put("files", files)
        .toString()
}

fun decodeSyncState(raw: String?): SyncState {
    if (raw.isNullOrBlank()) return SyncState()
    return runCatching {
        val json = JSONObject(raw)
        val filesJson = json.optJSONObject("files") ?: JSONObject()
        val files = mutableMapOf<String, RemoteState>()
        val keys = filesJson.keys()
        while (keys.hasNext()) {
            val path = keys.next()
            val file = filesJson.getJSONObject(path)
            files[path] = RemoteState(
                sha = file.getString("sha"),
                updatedAt = file.optString("updatedAt").ifBlank { null },
            )
        }
        SyncState(
            etag = json.optString("etag").ifBlank { null },
            files = files,
        )
    }.getOrDefault(SyncState())
}

private val DAY_PATH_PATTERN = Regex("data/\\d{4}/\\d{2}/\\d{4}-\\d{2}-\\d{2}\\.json")

class GitHubSync {
    private data class Content(val raw: String, val sha: String)
    private data class Tree(val files: Map<String, String>, val etag: String?)

    fun sync(config: GitHubConfig, local: AppData, state: SyncState = SyncState()): SyncResult {
        require(config.owner.isNotBlank()) { "Укажите владельца репозитория" }
        require(config.repo.isNotBlank()) { "Укажите репозиторий данных" }
        require(config.token.isNotBlank()) { "Вставьте GitHub token" }

        // При 304 дерево не изменилось: кеш sha из прошлой синхронизации актуален.
        val tree = listTree(config, state.etag)
        val remoteShas: Map<String, String> = tree?.files ?: state.files.mapValues { it.value.sha }
        val newFiles = mutableMapOf<String, RemoteState>()
        var downloaded = 0
        var uploaded = 0

        var profile = local.profile
        val profileSha = remoteShas["profile.json"]
        val cachedProfileUpdatedAt = state.files["profile.json"]
            ?.takeIf { it.sha == profileSha }
            ?.updatedAt
        when {
            profileSha == null -> {
                val sha = putContent(config, "profile.json", JsonCodec.encodeProfile(profile), null)
                newFiles["profile.json"] = RemoteState(sha, profile.updatedAt)
                uploaded += 1
            }
            cachedProfileUpdatedAt == profile.updatedAt -> {
                newFiles["profile.json"] = RemoteState(profileSha, cachedProfileUpdatedAt)
            }
            cachedProfileUpdatedAt != null && profile.updatedAt > cachedProfileUpdatedAt -> {
                // Удалённый профиль не менялся, локальный новее — только отправка.
                val sha = putContent(config, "profile.json", JsonCodec.encodeProfile(profile), profileSha)
                newFiles["profile.json"] = RemoteState(sha, profile.updatedAt)
                uploaded += 1
            }
            else -> {
                val remote = getContent(config, "profile.json")
                if (remote == null) {
                    val sha = putContent(config, "profile.json", JsonCodec.encodeProfile(profile), null)
                    newFiles["profile.json"] = RemoteState(sha, profile.updatedAt)
                    uploaded += 1
                } else {
                    downloaded += 1
                    val remoteProfile = JsonCodec.decodeProfile(remote.raw)
                    profile = newerProfile(profile, remoteProfile)
                    if (profile != remoteProfile) {
                        val sha = putContent(config, "profile.json", JsonCodec.encodeProfile(profile), remote.sha)
                        newFiles["profile.json"] = RemoteState(sha, profile.updatedAt)
                        uploaded += 1
                    } else {
                        newFiles["profile.json"] = RemoteState(remote.sha, remoteProfile.updatedAt)
                    }
                }
            }
        }

        val dayShas = remoteShas.filterKeys { DAY_PATH_PATTERN.matches(it) }
        val downloadPaths = mutableListOf<String>()
        val uploadLocalOnly = mutableMapOf<String, DayRecord>()
        for ((path, sha) in dayShas) {
            val date = path.substringAfterLast('/').removeSuffix(".json")
            val localDay = local.days[date]
            val cachedUpdatedAt = state.files[path]
                ?.takeIf { it.sha == sha }
                ?.updatedAt
            if (cachedUpdatedAt == null || localDay == null) {
                downloadPaths += path
            } else {
                when {
                    localDay.updatedAt == cachedUpdatedAt -> newFiles[path] = RemoteState(sha, cachedUpdatedAt)
                    localDay.updatedAt > cachedUpdatedAt -> uploadLocalOnly[path] = localDay
                    else -> downloadPaths += path
                }
            }
        }

        val remoteDays = mutableMapOf<String, DayRecord>()
        for (path in downloadPaths) {
            val content = getContent(config, path) ?: continue
            val day = JsonCodec.decodeDay(content.raw)
            remoteDays[path] = day
            newFiles[path] = RemoteState(content.sha, day.updatedAt)
            downloaded += 1
        }

        val mergedDays = local.days.toMutableMap()
        remoteDays.forEach { (_, remote) ->
            mergedDays[remote.date] = mergedDays[remote.date]?.let { mergeDay(it, remote) } ?: remote
        }

        for (day in mergedDays.values.toList()) {
            val path = dayPath(day.date)
            val remoteSha = remoteShas[path]
            when {
                remoteSha == null -> {
                    val (sha, finalDay) = putDayWithConflictRetry(config, path, day, null)
                    mergedDays[day.date] = finalDay
                    newFiles[path] = RemoteState(sha, finalDay.updatedAt)
                    uploaded += 1
                }
                uploadLocalOnly.containsKey(path) -> {
                    val (sha, finalDay) = putDayWithConflictRetry(config, path, day, remoteSha)
                    mergedDays[day.date] = finalDay
                    newFiles[path] = RemoteState(sha, finalDay.updatedAt)
                    uploaded += 1
                }
                remoteDays.containsKey(path) -> {
                    val remote = remoteDays.getValue(path)
                    if (remote != day) {
                        val (sha, finalDay) = putDayWithConflictRetry(config, path, day, remoteSha)
                        mergedDays[day.date] = finalDay
                        newFiles[path] = RemoteState(sha, finalDay.updatedAt)
                        uploaded += 1
                    } else {
                        newFiles[path] = RemoteState(remoteSha, remote.updatedAt)
                    }
                }
            }
        }

        val merged = AppData(profile = profile, days = mergedDays)
        return SyncResult(
            data = merged,
            uploadedFiles = uploaded,
            downloadedFiles = downloaded,
            state = SyncState(etag = tree?.etag ?: state.etag, files = newFiles),
        )
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

    private fun putDayWithConflictRetry(
        config: GitHubConfig,
        path: String,
        day: DayRecord,
        sha: String?,
    ): Pair<String, DayRecord> {
        return try {
            putContent(config, path, JsonCodec.encodeDay(day), sha) to day
        } catch (error: GitHubException) {
            if (error.status != 409) throw error
            val latest = getContent(config, path) ?: throw error
            val merged = mergeDay(JsonCodec.decodeDay(latest.raw), day)
            putContent(config, path, JsonCodec.encodeDay(merged), latest.sha) to merged
        }
    }

    private fun dayPath(date: String): String {
        val year = date.substring(0, 4)
        val month = date.substring(5, 7)
        return "data/$year/$month/$date.json"
    }

    private fun listTree(config: GitHubConfig, etag: String?): Tree? {
        val branch = encode(config.branch)
        val headers = if (etag.isNullOrBlank()) emptyMap() else mapOf("If-None-Match" to etag)
        val response = request(config, "GET", "/git/trees/$branch?recursive=1", null, setOf(304, 404, 409), headers)
        if (response.status == 304) return null
        if (response.status == 404 || response.status == 409) return Tree(emptyMap(), response.etag)
        val array = JSONObject(response.body).getJSONArray("tree")
        val files = mutableMapOf<String, String>()
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            if (item.optString("type") == "blob") {
                files[item.getString("path")] = item.getString("sha")
            }
        }
        return Tree(files, response.etag)
    }

    private fun getContent(config: GitHubConfig, path: String): Content? {
        val response = request(config, "GET", "/contents/${path.split('/').joinToString("/") { encode(it) }}?ref=${encode(config.branch)}", null, setOf(404))
        if (response.status == 404) return null
        val json = JSONObject(response.body)
        val raw = String(Base64.decode(json.getString("content"), Base64.DEFAULT))
        return Content(raw = raw, sha = json.getString("sha"))
    }

    private fun putContent(config: GitHubConfig, path: String, raw: String, sha: String?): String {
        val body = JSONObject()
            .put("message", "Анюта: обновление $path")
            .put("content", Base64.encodeToString(raw.toByteArray(), Base64.NO_WRAP))
            .put("branch", config.branch)
            .apply { sha?.let { put("sha", it) } }
            .toString()
        val response = request(config, "PUT", "/contents/${path.split('/').joinToString("/") { encode(it) }}", body)
        return JSONObject(response.body).getJSONObject("content").getString("sha")
    }

    private data class Response(val status: Int, val body: String, val etag: String?)

    private fun request(
        config: GitHubConfig,
        method: String,
        endpoint: String,
        body: String?,
        allowedErrors: Set<Int> = emptySet(),
        headers: Map<String, String> = emptyMap(),
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
            headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toByteArray()) }
            }
            val status = connection.responseCode
            val etag = connection.getHeaderField("ETag")
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299 && status !in allowedErrors) {
                val message = runCatching { JSONObject(responseBody).optString("message") }.getOrNull()
                    .orEmpty().ifBlank { "HTTP $status" }
                throw GitHubException(status, message)
            }
            Response(status, responseBody, etag)
        } catch (error: GitHubException) {
            throw error
        } catch (error: IOException) {
            throw IOException("Не удалось подключиться к GitHub: ${error.message}", error)
        } finally {
            connection.disconnect()
        }
    }

    private fun newerProfile(first: ChildProfile, second: ChildProfile): ChildProfile = when {
        first.isPlaceholder() && !second.isPlaceholder() -> second
        second.isPlaceholder() && !first.isPlaceholder() -> first
        first.updatedAt >= second.updatedAt -> first
        else -> second
    }

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

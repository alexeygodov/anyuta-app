package ru.family.rasti.sync

import android.util.Base64
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.family.rasti.data.*
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class GitHubProtocolTest {
    private val config = GitHubConfig(owner = "test", repo = "fixture", branch = "main", token = "fake-test-token")
    private val date = "2026-09-12"
    private val path = "data/2026/09/$date.json"
    private val local = AppData(
        profile = ChildProfile(name = "Тест", birthDate = "2026-01-01", updatedAt = "2026-09-01T00:00:00Z"),
        days = mapOf(date to DayRecord(date, food = listOf(food("local")), updatedAt = "2026-09-12T12:00:00Z")),
    )

    private fun food(id: String) = FoodEntry(
        id = id, time = "12:00", name = "Молоко", amount = 50.0, unit = "мл", updatedAt = "2026-09-12T12:00:00Z",
    )

    private fun cached(dayTime: String = local.days.getValue(date).updatedAt) = SyncState(
        etag = "fixture-etag",
        files = mapOf(
            "profile.json" to RemoteState("profile-sha", local.profile.updatedAt),
            path to RemoteState("old-sha", dayTime),
        ),
    )

    @Test fun unchangedTreeUsesEtagAndMakesNoContentRequests() {
        var calls = 0
        val syncer = GitHubSync { _, method, endpoint, _, headers ->
            calls++
            assertEquals("GET", method)
            assertEquals("/git/trees/main?recursive=1", endpoint)
            assertEquals("fixture-etag", headers["If-None-Match"])
            GitHubResponse(304, "")
        }
        val result = syncer.sync(config, local, cached())
        assertEquals(1, calls)
        assertEquals(local, result.data)
        assertEquals(0, result.uploadedFiles)
        assertEquals(0, result.downloadedFiles)
        assertEquals(cached(), result.state)
    }

    @Test fun conflictFetchesLatestAndRetriesWithBothDevicesEntries() {
        var calls = 0
        val remote = local.days.getValue(date).copy(food = listOf(food("remote")))
        val syncer = GitHubSync { _, method, endpoint, body, _ ->
            when (calls++) {
                0 -> GitHubResponse(304, "")
                1 -> {
                    assertEquals("PUT", method)
                    assertEquals("old-sha", JSONObject(body!!).getString("sha"))
                    GitHubResponse(409, "{\"message\":\"Conflict\"}")
                }
                2 -> {
                    assertEquals("GET", method)
                    assertTrue(endpoint.startsWith("/contents/$path"))
                    GitHubResponse(200, JSONObject().put("sha", "latest-sha")
                        .put("content", Base64.encodeToString(JsonCodec.encodeDay(remote).toByteArray(), Base64.NO_WRAP)).toString())
                }
                3 -> {
                    assertEquals("PUT", method)
                    val json = JSONObject(body!!)
                    assertEquals("latest-sha", json.getString("sha"))
                    val uploaded = JsonCodec.decodeDay(String(Base64.decode(json.getString("content"), Base64.DEFAULT)))
                    assertEquals(setOf("local", "remote"), uploaded.food.map { it.id }.toSet())
                    GitHubResponse(200, "{\"content\":{\"sha\":\"saved-sha\"}}")
                }
                else -> error("Unexpected extra request")
            }
        }
        val result = syncer.sync(config, local, cached("2026-09-12T11:00:00Z"))
        assertEquals(4, calls)
        assertEquals(setOf("local", "remote"), result.data.days.getValue(date).food.map { it.id }.toSet())
        assertEquals("saved-sha", result.state.files.getValue(path).sha)
    }

    @Test fun connectionLossDoesNotProduceSuccessOrMutateLocalSnapshot() {
        val state = cached("2026-09-12T11:00:00Z")
        var calls = 0
        val syncer = GitHubSync { _, _, _, _, _ ->
            if (calls++ == 0) GitHubResponse(304, "") else throw IOException("Simulated disconnect")
        }
        assertThrows(IOException::class.java) { syncer.sync(config, local, state) }
        assertEquals(2, calls)
        assertEquals(listOf("local"), local.days.getValue(date).food.map { it.id })
        assertEquals("old-sha", state.files.getValue(path).sha)
    }

    @Test fun secondConflictStopsWithoutUnboundedRetries() {
        var puts = 0
        val syncer = GitHubSync { _, method, endpoint, _, _ ->
            when {
                endpoint.startsWith("/git/trees/") -> GitHubResponse(304, "")
                method == "PUT" -> { puts++; GitHubResponse(409, "{}") }
                else -> GitHubResponse(200, JSONObject().put("sha", "new-sha")
                    .put("content", Base64.encodeToString(JsonCodec.encodeDay(local.days.getValue(date)).toByteArray(), Base64.NO_WRAP)).toString())
            }
        }
        assertThrows(GitHubException::class.java) { syncer.sync(config, local, cached("2026-09-12T11:00:00Z")) }
        assertEquals(2, puts)
    }

    @Test fun profileConflictKeepsNewerRemoteWithoutOverwritingIt() {
        val remote = local.profile.copy(name = "Другой телефон", updatedAt = "2026-09-02T00:00:00Z")
        val state = cached().copy(files = cached().files + ("profile.json" to RemoteState("old-profile-sha", "2026-08-01T00:00:00Z")))
        var calls = 0
        val syncer = GitHubSync { _, method, endpoint, body, _ ->
            when (calls++) {
                0 -> GitHubResponse(304, "")
                1 -> {
                    assertEquals("PUT", method)
                    assertEquals("old-profile-sha", JSONObject(body!!).getString("sha"))
                    GitHubResponse(409, "{}")
                }
                2 -> {
                    assertEquals("GET", method)
                    assertTrue(endpoint.startsWith("/contents/profile.json"))
                    GitHubResponse(200, JSONObject().put("sha", "new-profile-sha")
                        .put("content", Base64.encodeToString(JsonCodec.encodeProfile(remote).toByteArray(), Base64.NO_WRAP)).toString())
                }
                else -> error("Unexpected extra request")
            }
        }
        val result = syncer.sync(config, local, state)
        assertEquals(3, calls)
        assertEquals(remote, result.data.profile)
        assertEquals(RemoteState("new-profile-sha", remote.updatedAt), result.state.files.getValue("profile.json"))
        assertEquals(0, result.uploadedFiles)
        assertEquals(1, result.downloadedFiles)
        assertEquals("Тест", local.profile.name)
    }

    @Test fun profileConflictRetriesOnceWithLatestShaWhenLocalIsNewer() {
        val remote = local.profile.copy(name = "Старое имя", updatedAt = "2026-08-01T00:00:00Z")
        val state = cached().copy(files = cached().files + ("profile.json" to RemoteState("old-profile-sha", "2026-08-01T00:00:00Z")))
        var calls = 0
        val syncer = GitHubSync { _, method, endpoint, body, _ ->
            when (calls++) {
                0 -> GitHubResponse(304, "")
                1 -> GitHubResponse(409, "{}")
                2 -> {
                    assertEquals("GET", method)
                    assertTrue(endpoint.startsWith("/contents/profile.json"))
                    GitHubResponse(200, JSONObject().put("sha", "new-profile-sha")
                        .put("content", Base64.encodeToString(JsonCodec.encodeProfile(remote).toByteArray(), Base64.NO_WRAP)).toString())
                }
                3 -> {
                    assertEquals("PUT", method)
                    val request = JSONObject(body!!)
                    assertEquals("new-profile-sha", request.getString("sha"))
                    assertEquals(local.profile, JsonCodec.decodeProfile(String(Base64.decode(request.getString("content"), Base64.DEFAULT))))
                    GitHubResponse(200, "{\"content\":{\"sha\":\"saved-profile-sha\"}}")
                }
                else -> error("Unexpected extra request")
            }
        }
        val result = syncer.sync(config, local, state)
        assertEquals(4, calls)
        assertEquals(local.profile, result.data.profile)
        assertEquals("saved-profile-sha", result.state.files.getValue("profile.json").sha)
        assertEquals(1, result.uploadedFiles)
        assertEquals(1, result.downloadedFiles)
    }

    @Test fun secondProfileConflictStopsWithoutMutatingLocalSnapshot() {
        val remote = local.profile.copy(name = "Старое имя", updatedAt = "2026-08-01T00:00:00Z")
        val state = cached().copy(files = cached().files + ("profile.json" to RemoteState("old-profile-sha", "2026-08-01T00:00:00Z")))
        var puts = 0
        val syncer = GitHubSync { _, method, endpoint, _, _ ->
            when {
                endpoint.startsWith("/git/trees/") -> GitHubResponse(304, "")
                method == "PUT" -> { puts++; GitHubResponse(409, "{}") }
                else -> GitHubResponse(200, JSONObject().put("sha", "new-profile-sha")
                    .put("content", Base64.encodeToString(JsonCodec.encodeProfile(remote).toByteArray(), Base64.NO_WRAP)).toString())
            }
        }
        assertThrows(GitHubException::class.java) { syncer.sync(config, local, state) }
        assertEquals(2, puts)
        assertEquals("Тест", local.profile.name)
        assertEquals("old-profile-sha", state.files.getValue("profile.json").sha)
    }
    @Test fun mergeAfterRequestKeepsNewLocalChangesAndDeletions() {
        var current = local
        val syncer = GitHubSync { _, _, _, _, _ ->
            current = local.copy(days = mapOf(date to local.days.getValue(date).copy(
                food = listOf(food("added-during-request")), deletedFoodIds = setOf("local"),
                updatedAt = "2026-09-12T13:00:00Z",
            )))
            GitHubResponse(304, "")
        }
        val result = syncer.sync(config, local, cached())
        val applied = syncer.merge(current, result.data)
        assertEquals(listOf("added-during-request"), applied.days.getValue(date).food.map { it.id })
        assertTrue("local" in applied.days.getValue(date).deletedFoodIds)
    }
}

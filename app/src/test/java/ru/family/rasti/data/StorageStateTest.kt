package ru.family.rasti.data

import androidx.lifecycle.ViewModelStore
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ru.family.rasti.RastiViewModel
import ru.family.rasti.widget.AnyutaDashboardWidget
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class StorageStateTest {
    @Test fun corruptDiaryBlocksSyncAndWidgetRefreshDoesNotCrash() {
        val context = RuntimeEnvironment.getApplication()
        val primary = File(context.filesDir, "rasti-data.json").apply { writeText("{broken") }
        val store = LocalStore(context)
        val model = RastiViewModel(store)
        val models = ViewModelStore().apply { put("test", model) }
        try {
            assertNotNull(model.storageError)
            model.sync(GitHubConfig("test", "fixture", "main", "fake-test-token"))
            assertFalse(model.syncing)
            assertTrue(store.loadGitHubConfig().token.isBlank())
            AnyutaDashboardWidget.updateAll(context)
            assertEquals("{broken", primary.readText())
        } finally {
            models.clear()
        }
    }

    @Test fun recoveredDiaryBlocksSyncUntilAcknowledged() {
        val context = RuntimeEnvironment.getApplication()
        val expected = AppData(profile = ChildProfile(name = "Тест", birthDate = "2026-01-01"))
        JournalFile(context.filesDir).write(expected)
        File(context.filesDir, "rasti-data.json").writeText("{broken")
        val store = LocalStore(context)
        val model = RastiViewModel(store)
        val models = ViewModelStore().apply { put("test", model) }
        try {
            assertNull(model.storageError)
            assertTrue(model.dataRecovered)
            assertEquals(expected, model.data)
            model.sync(GitHubConfig("test", "fixture", "main", "fake-test-token"))
            assertTrue(store.loadGitHubConfig().token.isBlank())
            model.acknowledgeDataRecovery()
            assertFalse(model.dataRecovered)
            assertFalse(store.wasDataRecovered())
        } finally {
            models.clear()
        }
    }
}

package ru.family.rasti

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.family.rasti.data.AppData
import ru.family.rasti.data.ChildProfile
import ru.family.rasti.data.DayRecord
import ru.family.rasti.data.FoodEntry
import ru.family.rasti.data.GitHubConfig
import ru.family.rasti.data.LocalStore
import ru.family.rasti.data.Measurement
import ru.family.rasti.data.VitaminEntry
import ru.family.rasti.data.day
import ru.family.rasti.data.updateDay
import ru.family.rasti.sync.GitHubSync
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class RastiViewModel(private val store: LocalStore) : ViewModel() {
    var data by mutableStateOf(store.loadData())
        private set
    var githubConfig by mutableStateOf(store.loadGitHubConfig())
        private set
    var syncing by mutableStateOf(false)
        private set
    var statusMessage by mutableStateOf<String?>(null)
        private set

    fun day(date: LocalDate): DayRecord = data.day(date)

    fun saveProfile(profile: ChildProfile) {
        data = data.copy(profile = profile.copy(updatedAt = OffsetDateTime.now().toString()))
        persist()
        statusMessage = "Профиль сохранён"
    }

    fun addFood(date: LocalDate, name: String, amount: Double, unit: String, time: String?) {
        val day = day(date)
        val entry = FoodEntry(
            time = time?.takeIf { it.isNotBlank() } ?: currentTime(),
            name = name.trim(),
            amount = amount,
            unit = unit.trim().ifBlank { "г" },
        )
        data = data.updateDay(day.copy(food = day.food + entry))
        persist()
    }

    fun removeFood(date: LocalDate, id: String) {
        val day = day(date)
        data = data.updateDay(
            day.copy(
                food = day.food.filterNot { it.id == id },
                deletedFoodIds = day.deletedFoodIds + id,
            ),
        )
        persist()
    }

    fun addVitamin(date: LocalDate, name: String, dose: String, time: String?) {
        val day = day(date)
        val entry = VitaminEntry(
            time = time?.takeIf { it.isNotBlank() } ?: currentTime(),
            name = name.trim(),
            dose = dose.trim(),
        )
        data = data.updateDay(day.copy(vitamins = day.vitamins + entry))
        persist()
    }

    fun removeVitamin(date: LocalDate, id: String) {
        val day = day(date)
        data = data.updateDay(
            day.copy(
                vitamins = day.vitamins.filterNot { it.id == id },
                deletedVitaminIds = day.deletedVitaminIds + id,
            ),
        )
        persist()
    }

    fun saveMeasurement(date: LocalDate, heightCm: Double?, weightKg: Double?) {
        val day = day(date)
        val measurement = if (heightCm == null && weightKg == null) null else Measurement(heightCm, weightKg)
        data = data.updateDay(day.copy(measurement = measurement))
        persist()
    }

    fun saveNote(date: LocalDate, note: String) {
        data = data.updateDay(day(date).copy(note = note))
        persist()
    }

    fun saveGitHubConfig(config: GitHubConfig) {
        githubConfig = config.copy(branch = config.branch.ifBlank { "main" })
        store.saveGitHubConfig(githubConfig)
        statusMessage = "Настройки GitHub сохранены"
    }

    fun sync(config: GitHubConfig = githubConfig) {
        if (syncing) return
        saveGitHubConfig(config)
        syncing = true
        statusMessage = "Синхронизация…"
        val syncer = GitHubSync()
        val snapshot = data
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { syncer.sync(githubConfig, snapshot) }
            }.onSuccess { result ->
                data = syncer.merge(data, result.data)
                persist()
                statusMessage = "Готово: получено ${result.downloadedFiles}, отправлено ${result.uploadedFiles}"
            }.onFailure { error ->
                statusMessage = error.message ?: "Ошибка синхронизации"
            }
            syncing = false
        }
    }

    fun clearStatus() {
        statusMessage = null
    }

    private fun persist() = store.saveData(data)

    private fun currentTime(): String = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

    class Factory(private val store: LocalStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RastiViewModel(store) as T
    }
}

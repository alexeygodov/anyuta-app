package ru.family.rasti

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
import ru.family.rasti.data.VaccinationEntry
import ru.family.rasti.data.VaccinationStatus
import ru.family.rasti.data.day
import ru.family.rasti.data.updateDay
import ru.family.rasti.sync.GitHubSync
import ru.family.rasti.sync.decodeSyncState
import ru.family.rasti.sync.encodeSyncState
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
    private var syncRequested = false
    private var syncState: String? = store.loadSyncState()

    init {
        viewModelScope.launch {
            syncIfConfigured(showStatus = false)
            while (isActive) {
                delay(60_000)
                syncIfConfigured(showStatus = false)
            }
        }
    }

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

    fun updateFood(
        originalDate: LocalDate,
        targetDate: LocalDate,
        original: FoodEntry,
        name: String,
        amount: Double,
        unit: String,
        time: String,
    ) {
        val updated = original.copy(
            time = time,
            name = name.trim(),
            amount = amount,
            unit = unit.trim().ifBlank { "г" },
            updatedAt = OffsetDateTime.now().toString(),
        )
        if (originalDate == targetDate) {
            val source = day(originalDate)
            data = data.updateDay(
                source.copy(
                    food = source.food.map { if (it.id == original.id) updated else it },
                    deletedFoodIds = source.deletedFoodIds - original.id,
                ),
            )
        } else {
            val source = day(originalDate)
            data = data.updateDay(
                source.copy(
                    food = source.food.filterNot { it.id == original.id },
                    deletedFoodIds = source.deletedFoodIds + original.id,
                ),
            )
            val target = day(targetDate)
            data = data.updateDay(
                target.copy(
                    food = target.food.filterNot { it.id == original.id } + updated,
                    deletedFoodIds = target.deletedFoodIds - original.id,
                ),
            )
        }
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

    fun addVitamin(date: LocalDate, name: String, amount: Double, unit: String, time: String?) {
        val day = day(date)
        val entry = VitaminEntry(
            time = time?.takeIf { it.isNotBlank() } ?: currentTime(),
            name = name.trim(),
            amount = amount,
            unit = unit.trim(),
        )
        data = data.updateDay(day.copy(vitamins = day.vitamins + entry))
        persist()
    }

    fun updateVitamin(
        originalDate: LocalDate,
        targetDate: LocalDate,
        original: VitaminEntry,
        name: String,
        amount: Double,
        unit: String,
        time: String,
    ) {
        val updated = original.copy(
            time = time,
            name = name.trim(),
            amount = amount,
            unit = unit.trim(),
            updatedAt = OffsetDateTime.now().toString(),
        )
        if (originalDate == targetDate) {
            val source = day(originalDate)
            data = data.updateDay(
                source.copy(
                    vitamins = source.vitamins.map { if (it.id == original.id) updated else it },
                    deletedVitaminIds = source.deletedVitaminIds - original.id,
                ),
            )
        } else {
            val source = day(originalDate)
            data = data.updateDay(
                source.copy(
                    vitamins = source.vitamins.filterNot { it.id == original.id },
                    deletedVitaminIds = source.deletedVitaminIds + original.id,
                ),
            )
            val target = day(targetDate)
            data = data.updateDay(
                target.copy(
                    vitamins = target.vitamins.filterNot { it.id == original.id } + updated,
                    deletedVitaminIds = target.deletedVitaminIds - original.id,
                ),
            )
        }
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

    fun addVaccination(date: LocalDate, name: String, status: VaccinationStatus, note: String) {
        val day = day(date)
        val entry = VaccinationEntry(name = name.trim(), status = status, note = note.trim())
        data = data.updateDay(day.copy(vaccinations = day.vaccinations + entry))
        persist()
    }

    fun updateVaccination(
        originalDate: LocalDate,
        targetDate: LocalDate,
        original: VaccinationEntry,
        name: String,
        status: VaccinationStatus,
        note: String,
    ) {
        val updated = original.copy(
            name = name.trim(),
            status = status,
            note = note.trim(),
            updatedAt = OffsetDateTime.now().toString(),
        )
        if (originalDate == targetDate) {
            val source = day(originalDate)
            data = data.updateDay(
                source.copy(
                    vaccinations = source.vaccinations.map { if (it.id == original.id) updated else it },
                    deletedVaccinationIds = source.deletedVaccinationIds - original.id,
                ),
            )
        } else {
            val source = day(originalDate)
            data = data.updateDay(
                source.copy(
                    vaccinations = source.vaccinations.filterNot { it.id == original.id },
                    deletedVaccinationIds = source.deletedVaccinationIds + original.id,
                ),
            )
            val target = day(targetDate)
            data = data.updateDay(
                target.copy(
                    vaccinations = target.vaccinations.filterNot { it.id == original.id } + updated,
                    deletedVaccinationIds = target.deletedVaccinationIds - original.id,
                ),
            )
        }
        persist()
    }

    fun removeVaccination(date: LocalDate, id: String) {
        val day = day(date)
        data = data.updateDay(
            day.copy(
                vaccinations = day.vaccinations.filterNot { it.id == id },
                deletedVaccinationIds = day.deletedVaccinationIds + id,
            ),
        )
        persist()
    }

    fun saveMeasurement(date: LocalDate, heightCm: Double?, weightKg: Double?, time: String?) {
        val day = day(date)
        val now = OffsetDateTime.now().toString()
        val measurement = if (heightCm == null && weightKg == null) {
            null
        } else {
            Measurement(heightCm = heightCm, weightKg = weightKg, time = time?.ifBlank { currentTime() } ?: currentTime(), updatedAt = now)
        }
        data = data.updateDay(
            day.copy(
                measurement = measurement,
                measurementDeletedAt = if (measurement == null) now else day.measurementDeletedAt,
            ),
        )
        persist()
    }

    fun updateMeasurement(
        originalDate: LocalDate,
        targetDate: LocalDate,
        heightCm: Double?,
        weightKg: Double?,
        time: String,
    ) {
        val now = OffsetDateTime.now().toString()
        val updated = if (heightCm == null && weightKg == null) {
            null
        } else {
            Measurement(heightCm = heightCm, weightKg = weightKg, time = time, updatedAt = now)
        }
        if (originalDate == targetDate) {
            val source = day(originalDate)
            data = data.updateDay(
                source.copy(
                    measurement = updated,
                    measurementDeletedAt = if (updated == null) now else source.measurementDeletedAt,
                ),
            )
        } else {
            val source = day(originalDate)
            data = data.updateDay(source.copy(measurement = null, measurementDeletedAt = now))
            val target = day(targetDate)
            data = data.updateDay(target.copy(measurement = updated, measurementDeletedAt = target.measurementDeletedAt))
        }
        persist()
    }

    fun saveNote(date: LocalDate, note: String) {
        data = data.updateDay(day(date).copy(note = note))
        persist()
    }

    fun saveGitHubConfig(config: GitHubConfig, showStatus: Boolean = true) {
        val updated = config.copy(branch = config.branch.ifBlank { "main" })
        val previous = githubConfig
        githubConfig = updated
        store.saveGitHubConfig(updated)
        if (previous.owner != updated.owner || previous.repo != updated.repo || previous.branch != updated.branch) {
            syncState = null
            store.saveSyncState("")
        }
        if (showStatus) statusMessage = "Настройки GitHub сохранены"
    }

    fun sync(config: GitHubConfig = githubConfig, showStatus: Boolean = true) {
        saveGitHubConfig(config, showStatus = false)
        if (!hasSyncConfig(githubConfig)) return
        if (syncing) {
            syncRequested = true
            return
        }
        syncing = true
        if (showStatus) statusMessage = "Синхронизация…"
        val syncer = GitHubSync()
        val snapshot = data
        val activeConfig = githubConfig
        val activeState = decodeSyncState(syncState)
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { syncer.sync(activeConfig, snapshot, activeState) }
            }.onSuccess { result ->
                data = syncer.merge(data, result.data)
                persist(syncAfter = false)
                val encodedState = encodeSyncState(result.state)
                syncState = encodedState
                store.saveSyncState(encodedState)
                if (showStatus) {
                    statusMessage = "Готово: получено ${result.downloadedFiles}, отправлено ${result.uploadedFiles}"
                }
            }.onFailure { error ->
                if (showStatus) statusMessage = error.message ?: "Ошибка синхронизации"
            }
            syncing = false
            if (syncRequested) {
                syncRequested = false
                sync(githubConfig, showStatus = false)
            }
        }
    }

    fun syncIfConfigured(showStatus: Boolean = false) {
        if (hasSyncConfig(githubConfig)) sync(githubConfig, showStatus)
    }

    fun clearStatus() {
        statusMessage = null
    }

    private fun persist(syncAfter: Boolean = true) {
        store.saveData(data)
        if (syncAfter) syncIfConfigured(showStatus = false)
    }

    private fun currentTime(): String {
        val now = LocalTime.now()
        return now.withMinute(now.minute / 5 * 5).withSecond(0).withNano(0)
            .format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    private fun hasSyncConfig(config: GitHubConfig): Boolean =
        config.owner.isNotBlank() && config.repo.isNotBlank() && config.token.isNotBlank()

    class Factory(private val store: LocalStore) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            RastiViewModel(store) as T
    }
}

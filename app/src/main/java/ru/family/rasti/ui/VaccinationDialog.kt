package ru.family.rasti.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.family.rasti.data.VaccinationEntry
import ru.family.rasti.data.VaccinationStatus
import java.time.LocalDate

@Composable
internal fun VaccinationEditorDialog(
    initialDate: LocalDate,
    initial: VaccinationEntry? = null,
    onDismiss: () -> Unit,
    onSave: (LocalDate, String, VaccinationStatus, String) -> Unit,
) {
    val stateKey = initial?.id ?: initialDate.toString()
    var name by rememberSaveable(stateKey) { mutableStateOf(initial?.name.orEmpty()) }
    var dateRaw by rememberSaveable(stateKey) { mutableStateOf(initialDate.toString()) }
    var statusName by rememberSaveable(stateKey) { mutableStateOf((initial?.status ?: VaccinationStatus.PLANNED).name) }
    var note by rememberSaveable(stateKey) { mutableStateOf(initial?.note.orEmpty()) }
    val date = LocalDate.parse(dateRaw)
    val status = VaccinationStatus.valueOf(statusName)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Добавить прививку" else "Изменить прививку") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название вакцины") },
                    placeholder = { Text("Например, Пентаксим") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                DatePickerButton(
                    date = date,
                    onDateChange = { dateRaw = it.toString() },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = status == VaccinationStatus.PLANNED,
                        onClick = { statusName = VaccinationStatus.PLANNED.name },
                        label = { Text("Запланирована") },
                    )
                    FilterChip(
                        selected = status == VaccinationStatus.COMPLETED,
                        onClick = { statusName = VaccinationStatus.COMPLETED.name },
                        label = { Text("Сделана") },
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Заметка") },
                    placeholder = { Text("Серия, клиника, реакция — по желанию") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(date, name.trim(), status, note.trim()) },
                enabled = name.isNotBlank(),
            ) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}

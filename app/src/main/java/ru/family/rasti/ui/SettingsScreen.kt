package ru.family.rasti.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ru.family.rasti.BuildConfig
import ru.family.rasti.RastiViewModel
import ru.family.rasti.data.ChildProfile
import ru.family.rasti.data.ChildSex
import ru.family.rasti.data.GitHubConfig
import java.time.LocalDate

@Composable
fun SettingsScreen(viewModel: RastiViewModel, modifier: Modifier = Modifier) {
    val currentProfile = viewModel.data.profile
    var childName by remember(currentProfile) { mutableStateOf(currentProfile.name) }
    var birthDate by remember(currentProfile) { mutableStateOf(currentProfile.birthDate) }
    var sex by remember(currentProfile) { mutableStateOf(currentProfile.sex) }

    val currentConfig = viewModel.githubConfig
    var owner by remember(currentConfig) { mutableStateOf(currentConfig.owner) }
    var repo by remember(currentConfig) { mutableStateOf(currentConfig.repo) }
    var branch by remember(currentConfig) { mutableStateOf(currentConfig.branch) }
    var token by remember(currentConfig) { mutableStateOf(currentConfig.token) }

    val birthDateValid = runCatching { LocalDate.parse(birthDate) }.getOrNull()
        ?.let { !it.isAfter(LocalDate.now()) } == true
    val config = GitHubConfig(owner.trim(), repo.trim(), branch.trim(), token.trim())

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Настройки", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }
        item {
            SettingsCard("Профиль ребёнка") {
                OutlinedTextField(
                    childName,
                    { childName = it },
                    label = { Text("Имя или псевдоним") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    birthDate,
                    { birthDate = it },
                    label = { Text("Дата рождения: ГГГГ-ММ-ДД") },
                    supportingText = { if (!birthDateValid) Text("Например: 2025-04-23") },
                    isError = !birthDateValid,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilterChip(
                        selected = sex == ChildSex.GIRL,
                        onClick = { sex = ChildSex.GIRL },
                        label = { Text("Девочка") },
                    )
                    FilterChip(
                        selected = sex == ChildSex.BOY,
                        onClick = { sex = ChildSex.BOY },
                        label = { Text("Мальчик") },
                    )
                }
                Button(
                    onClick = { viewModel.saveProfile(ChildProfile(childName.trim(), birthDate, sex)) },
                    enabled = childName.isNotBlank() && birthDateValid,
                ) { Text("Сохранить профиль") }
            }
        }
        item {
            SettingsCard("Синхронизация через GitHub") {
                Text(
                    "Создайте отдельный приватный репозиторий данных с README. " +
                        "Приложение будет хранить в нём profile.json и по одному JSON-файлу на день.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(owner, { owner = it }, label = { Text("Владелец: username") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(repo, { repo = it }, label = { Text("Репозиторий: anyuta-data") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(branch, { branch = it }, label = { Text("Ветка") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(
                    token,
                    { token = it },
                    label = { Text("Fine-grained token") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "Токен шифруется Android Keystore и не записывается в JSON или Git.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Button(
                    onClick = { viewModel.sync(config) },
                    enabled = !viewModel.syncing && owner.isNotBlank() && repo.isNotBlank() && token.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (viewModel.syncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(Icons.Outlined.CloudSync, contentDescription = null)
                    }
                    Text(if (viewModel.syncing) "  Синхронизация…" else "  Сохранить и синхронизировать")
                }
                Text(
                    "Права token: только выбранный data-репозиторий, Contents — Read and write.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Важно", fontWeight = FontWeight.Bold)
                    Text(
                        "Приложение не ставит диагнозы. Границы на графиках — справочные стандарты WHO. " +
                            "Решения о витаминах и питании принимайте вместе с врачом.",
                    )
                }
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
            Text(
                "Анюта ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

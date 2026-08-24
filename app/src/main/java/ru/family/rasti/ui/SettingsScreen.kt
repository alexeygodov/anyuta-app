package ru.family.rasti.ui

import android.net.Uri
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
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AssistChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ru.family.rasti.BuildConfig
import ru.family.rasti.RastiViewModel
import ru.family.rasti.data.AppTheme
import ru.family.rasti.data.ChildProfile
import ru.family.rasti.data.ChildSex
import ru.family.rasti.data.GitHubConfig
import ru.family.rasti.data.MaxConfig
import ru.family.rasti.max.MaxChat
import ru.family.rasti.update.AppUpdater
import ru.family.rasti.update.UpdateInfo
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(viewModel: RastiViewModel, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentProfile = viewModel.data.profile
    var childName by remember(currentProfile) { mutableStateOf(currentProfile.name) }
    var birthDate by remember(currentProfile) {
        mutableStateOf(runCatching { LocalDate.parse(currentProfile.birthDate) }.getOrDefault(LocalDate.now().minusYears(1)))
    }
    var dueDate by remember(currentProfile) {
        mutableStateOf(runCatching { LocalDate.parse(currentProfile.dueDate) }.getOrDefault(birthDate))
    }
    var useBirthDateForLeaps by remember(currentProfile) { mutableStateOf(currentProfile.dueDate.isBlank()) }
    var sex by remember(currentProfile) { mutableStateOf(currentProfile.sex) }

    val currentConfig = viewModel.githubConfig
    var owner by remember(currentConfig) { mutableStateOf(currentConfig.owner) }
    var repo by remember(currentConfig) { mutableStateOf(currentConfig.repo) }
    var branch by remember(currentConfig) { mutableStateOf(currentConfig.branch) }
    var token by remember(currentConfig) { mutableStateOf(currentConfig.token) }
    var availableUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    var updateBusy by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf("Можно проверить новую версию в GitHub Releases.") }
    var downloadedApkPath by remember { mutableStateOf<String?>(null) }

    val currentMax = viewModel.maxConfig
    var maxEnabled by remember(currentMax) { mutableStateOf(currentMax.enabled) }
    var maxToken by remember(currentMax) { mutableStateOf(currentMax.token) }
    var maxChatId by remember(currentMax) { mutableStateOf(currentMax.chatId) }
    var maxBusy by remember { mutableStateOf(false) }
    var maxMessage by remember { mutableStateOf<String?>(null) }
    var maxFoundChats by remember { mutableStateOf<List<MaxChat>>(emptyList()) }

    val config = GitHubConfig(owner.trim(), repo.trim(), branch.trim(), token.trim())
    val tokenOwner = owner.trim().ifBlank { "alexeygodov" }
    val tokenUrl = "https://github.com/settings/personal-access-tokens/new" +
        "?name=Anyuta-phone&description=Anyuta-data-sync" +
        "&target_name=${Uri.encode(tokenOwner)}&expires_in=366&contents=write"

    androidx.compose.runtime.LaunchedEffect(token) {
        delay(600)
        val normalized = token.trim()
        val looksComplete = (normalized.startsWith("github_pat_") && normalized.length >= 80) ||
            (normalized.startsWith("ghp_") && normalized.length >= 40)
        if (looksComplete && normalized != viewModel.githubConfig.token) {
            viewModel.sync(config, showStatus = true)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text("Настройки", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        }
        item {
            SettingsCard("Оформление") {
                Text(
                    "Выберите тему — она применяется сразу и сохраняется только на этом телефоне.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    FilterChip(
                        selected = viewModel.appTheme == AppTheme.LIGHT,
                        onClick = { viewModel.selectAppTheme(AppTheme.LIGHT) },
                        label = { Text("Светлая") },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = viewModel.appTheme == AppTheme.DARK,
                        onClick = { viewModel.selectAppTheme(AppTheme.DARK) },
                        label = { Text("Тёмная") },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
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
                Text("Дата рождения", style = MaterialTheme.typography.labelLarge)
                DatePickerButton(
                    date = birthDate,
                    onDateChange = { birthDate = it },
                    modifier = Modifier.fillMaxWidth(),
                    maximumDate = LocalDate.now(),
                )
                FilterChip(
                    selected = useBirthDateForLeaps,
                    onClick = { useBirthDateForLeaps = !useBirthDateForLeaps },
                    label = { Text("ПДР неизвестна — считать скачки от рождения") },
                )
                if (!useBirthDateForLeaps) {
                    Text("Предполагаемая дата родов", style = MaterialTheme.typography.labelLarge)
                    DatePickerButton(
                        date = dueDate,
                        onDateChange = { dueDate = it },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
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
                    onClick = {
                        viewModel.saveProfile(
                            ChildProfile(
                                name = childName.trim(),
                                birthDate = birthDate.toString(),
                                dueDate = if (useBirthDateForLeaps) "" else dueDate.toString(),
                                sex = sex,
                            ),
                        )
                    },
                    enabled = childName.isNotBlank(),
                ) { Text("Сохранить профиль") }
            }
        }
        item {
            SettingsCard("Синхронизация через GitHub") {
                Text(
                    "Здесь нужен не SSH-ключ и не пароль, а отдельный fine-grained personal access token. " +
                        "Он используется только для приватных семейных данных.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = { uriHandler.openUri(tokenUrl) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                    Text("  Создать токен на GitHub")
                }
                Text(
                    "На открывшейся странице выберите Repository access → Only select repositories → anyuta-data. " +
                        "Для Contents оставьте Read and write, нажмите Generate token и сразу скопируйте результат.",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(owner, { owner = it }, label = { Text("Владелец: username") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(repo, { repo = it }, label = { Text("Репозиторий: anyuta-data") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(branch, { branch = it }, label = { Text("Ветка") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(
                    token,
                    { token = it },
                    label = { Text("Токен доступа GitHub") },
                    supportingText = { Text("Вставьте строку, которая обычно начинается с github_pat_.") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "Токен хранится только на этом телефоне, шифруется Android Keystore и не записывается в JSON или Git. Не отправляйте его в чат.",
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
                    "SSH-ключ, который вы добавляли в аккаунт GitHub, нужен компьютеру для git push. Приложение Android использует токен выше.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "После сохранения синхронизация идёт автоматически: при запуске, каждую минуту, после ввода данных и при переключении вкладок.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            SettingsCard("Уведомления в MAX") {
                Text(
                    "Приложение может слать в семейный чат MAX: новые кормления, приём витамина D, " +
                        "напоминания (3 часа без кормления, полдень без витамина) и итог дня в 21:00.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (maxEnabled) "Отправка включена" else "Отправка выключена",
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = maxEnabled, onCheckedChange = { maxEnabled = it })
                }
                Text(
                    "1. Создайте бота через Masterbot в MAX и скопируйте токен. " +
                        "2. Добавьте бота в семейный чат и сделайте администратором. " +
                        "3. Напишите в чат любое сообщение и нажмите «Найти чат».",
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    maxToken,
                    { maxToken = it },
                    label = { Text("Токен бота MAX") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    maxChatId,
                    { maxChatId = it.filter { ch -> ch.isDigit() } },
                    label = { Text("ID чата") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedButton(
                    onClick = {
                        maxBusy = true
                        maxMessage = null
                        viewModel.findMaxChats(maxToken) { chats, error ->
                            maxBusy = false
                            if (chats != null) {
                                maxFoundChats = chats
                                if (chats.size == 1) maxChatId = chats[0].id.toString()
                                maxMessage = if (chats.isEmpty()) {
                                    "Чаты не найдены. Напишите любое сообщение в чате с ботом и попробуйте снова."
                                } else {
                                    "Найдено чатов: ${chats.size} — выберите ниже."
                                }
                            } else {
                                maxMessage = error
                            }
                        }
                    },
                    enabled = !maxBusy && maxToken.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (maxBusy) "Поиск…" else "Найти чат") }
                maxFoundChats.forEach { chat ->
                    AssistChip(
                        onClick = { maxChatId = chat.id.toString() },
                        label = { Text("${chat.title} (${chat.id})") },
                    )
                }
                Button(
                    onClick = {
                        val config = MaxConfig(maxEnabled, maxToken.trim(), maxChatId.trim())
                        viewModel.saveMaxConfig(config)
                        maxBusy = true
                        maxMessage = null
                        viewModel.testMaxConnection(config) { ok, message ->
                            maxBusy = false
                            maxMessage = (if (ok) "✅ " else "⚠️ ") + message
                        }
                    },
                    enabled = !maxBusy && maxToken.isNotBlank() && maxChatId.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (maxBusy) "Проверка…" else "Сохранить и проверить") }
                maxMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Text(
                    "События с этого телефона отправит в чат второй телефон после синхронизации — " +
                        "поэтому настройте MAX на обоих устройствах.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            SettingsCard("Обновление приложения") {
                Text("Установлена версия ${BuildConfig.VERSION_NAME}")
                Text(updateMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(
                    onClick = {
                        val update = availableUpdate
                        scope.launch {
                            updateBusy = true
                            if (update == null) {
                                runCatching {
                                    withContext(Dispatchers.IO) { AppUpdater.check(BuildConfig.VERSION_NAME) }
                                }.onSuccess { result ->
                                    availableUpdate = result
                                    updateMessage = if (result == null) {
                                        "У вас последняя опубликованная версия."
                                    } else {
                                        "Доступна версия ${result.versionName}."
                                    }
                                }.onFailure { error ->
                                    updateMessage = error.message ?: "Не удалось проверить обновление"
                                }
                            } else {
                                runCatching {
                                    val cached = downloadedApkPath?.let(::File)?.takeIf(File::exists)
                                    cached ?: withContext(Dispatchers.IO) { AppUpdater.download(context, update) }
                                }.onSuccess { apk ->
                                    downloadedApkPath = apk.absolutePath
                                    val installerOpened = runCatching { AppUpdater.requestInstall(context, apk) }.getOrElse { error ->
                                        updateMessage = error.message ?: "Не удалось открыть установку"
                                        false
                                    }
                                    if (installerOpened) {
                                        updateMessage = "APK скачан. Подтвердите обновление в окне Android."
                                    } else if (!updateMessage.startsWith("Не удалось")) {
                                        updateMessage = "Разрешите установку из этого источника и нажмите кнопку ещё раз."
                                    }
                                }.onFailure { error ->
                                    updateMessage = error.message ?: "Не удалось скачать обновление"
                                }
                            }
                            updateBusy = false
                        }
                    },
                    enabled = !updateBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (updateBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Text(
                        when {
                            updateBusy -> "  Подождите…"
                            availableUpdate == null -> "Проверить обновление"
                            else -> "Скачать и установить ${availableUpdate?.versionName}"
                        },
                    )
                }
                Text(
                    "Обновления скачиваются из публичного anyuta-app без токена. Android один раз попросит " +
                        "разрешить установку из приложения «Анюта».",
                    style = MaterialTheme.typography.bodySmall,
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

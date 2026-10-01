package ru.family.rasti.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StorageRecoveryScreen(recovered: Boolean, onContinue: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                if (recovered) "Дневник восстановлен" else "Не удалось открыть дневник",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                if (recovered) {
                    "Открыта предыдущая локальная копия. Последние изменения могут отсутствовать. " +
                        "Повреждённый файл сохранён отдельно. После продолжения проверьте записи; " +
                        "синхронизация может вернуть данные с другого телефона."
                } else {
                    "Дневник или его резервная копия недоступны. Запись и синхронизация приостановлены. " +
                        "Проверьте свободное место и повторите чтение. Если ошибка остаётся, " +
                        "сохраните данные на другом телефоне и обратитесь за помощью. " +
                        "Не очищайте данные приложения и не удаляйте его."
                },
            )
            Button(onClick = onContinue) {
                Text(if (recovered) "Продолжить" else "Повторить чтение")
            }
        }
    }
}

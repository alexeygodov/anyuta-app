package ru.family.rasti.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.family.rasti.data.AppData
import ru.family.rasti.data.DayRecord
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HistoryScreen(data: AppData, modifier: Modifier = Modifier) {
    val days = data.days.values.sortedByDescending { it.date }
    val weekStart = LocalDate.now().minusDays(6).toString()
    val week = days.filter { it.date >= weekStart && it.date <= LocalDate.now().toString() }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("История", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Минимальный отчёт без таблиц и лишней бухгалтерии")
        }
        item { WeekReport(week) }
        if (days.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Text("Записи появятся здесь после заполнения первого дня.", Modifier.padding(20.dp))
                }
            }
        } else {
            items(days, key = { it.date }) { day -> HistoryDay(day) }
        }
    }
}

@Composable
private fun WeekReport(days: List<DayRecord>) {
    val foodCount = days.sumOf { it.food.size }
    val vitaminCount = days.sumOf { it.vitamins.size }
    val measured = days.count { it.measurement != null }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Последние 7 дней", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ReportValue("Заполнено", "${days.size} дн.")
                ReportValue("Приёмов еды", foodCount.toString())
                ReportValue("Витаминов", vitaminCount.toString())
                ReportValue("Измерений", measured.toString())
            }
        }
    }
}

@Composable
private fun ReportValue(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun HistoryDay(day: DayRecord) {
    val date = runCatching { LocalDate.parse(day.date) }.getOrNull()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                date?.format(DateTimeFormatter.ofPattern("d MMMM, EEEE", Locale.forLanguageTag("ru"))) ?: day.date,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            val totals = day.food.groupBy { it.unit }.mapValues { (_, items) -> items.sumOf { it.amount } }
            Text(
                if (totals.isEmpty()) "Еда: —"
                else "Еда: " + totals.entries.joinToString(" · ") { "${formatNumber(it.value)} ${it.key}" },
            )
            Text("Витамины: ${day.vitamins.joinToString { it.name }.ifBlank { "—" }}")
            day.measurement?.let { measurement ->
                Text(
                    listOfNotNull(
                        measurement.heightCm?.let { "рост ${formatNumber(it)} см" },
                        measurement.weightKg?.let { "вес ${formatNumber(it)} кг" },
                    ).joinToString(" · ").ifBlank { "Измерения: —" },
                )
            }
            if (day.note.isNotBlank()) {
                Text(day.note, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

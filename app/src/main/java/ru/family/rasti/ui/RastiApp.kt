package ru.family.rasti.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ru.family.rasti.RastiViewModel

private enum class AppScreen(val label: String, val icon: ImageVector) {
    TODAY("Сегодня", Icons.Outlined.Home),
    HISTORY("История", Icons.Outlined.CalendarMonth),
    CHARTS("Графики", Icons.Outlined.BarChart),
    SETTINGS("Настройки", Icons.Outlined.Settings),
}

@Composable
fun RastiApp(
    viewModel: RastiViewModel,
    widgetAction: String? = null,
    onWidgetActionConsumed: () -> Unit = {},
    clock: java.time.Clock? = null,
) {
    if (viewModel.storageError != null || viewModel.dataRecovered) {
        StorageRecoveryScreen(
            recovered = viewModel.storageError == null && viewModel.dataRecovered,
            onContinue = {
                if (viewModel.storageError != null) viewModel.retryStorage()
                else viewModel.acknowledgeDataRecovery()
            },
        )
        return
    }
    var screenName by rememberSaveable { mutableStateOf(AppScreen.TODAY.name) }
    val screen = AppScreen.valueOf(screenName)
    val snackbar = remember { SnackbarHostState() }
    val message = viewModel.statusMessage

    LaunchedEffect(widgetAction) {
        if (widgetAction != null) screenName = AppScreen.TODAY.name
    }

    LaunchedEffect(screen) {
        viewModel.syncIfConfigured(showStatus = false)
    }

    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.clearStatus()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                NavigationBar(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp,
                ) {
                    AppScreen.entries.forEach { item ->
                        NavigationBarItem(
                            selected = screen == item,
                            onClick = { screenName = item.name },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        when (screen) {
            AppScreen.TODAY -> TodayScreen(
                viewModel = viewModel,
                clock = clock,
                initialDate = java.time.LocalDate.now(clock ?: java.time.Clock.systemDefaultZone()),
                modifier = Modifier.padding(padding),
                widgetAction = widgetAction,
                onWidgetActionConsumed = onWidgetActionConsumed,
            )
            AppScreen.HISTORY -> HistoryScreen(viewModel, Modifier.padding(padding), clock ?: java.time.Clock.systemDefaultZone())
            AppScreen.CHARTS -> ChartsScreen(viewModel, Modifier.padding(padding), clock ?: java.time.Clock.systemDefaultZone())
            AppScreen.SETTINGS -> SettingsScreen(viewModel, Modifier.padding(padding), clock ?: java.time.Clock.systemDefaultZone())
        }
    }
}

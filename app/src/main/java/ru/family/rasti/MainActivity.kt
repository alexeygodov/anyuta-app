package ru.family.rasti

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.family.rasti.data.AppTheme
import ru.family.rasti.data.LocalStore
import ru.family.rasti.notify.AppVisibility
import ru.family.rasti.notify.MaxMessenger
import ru.family.rasti.notify.ReminderNotifier
import ru.family.rasti.notify.ReminderScheduler
import ru.family.rasti.ui.RastiApp
import ru.family.rasti.ui.theme.RastiTheme
import ru.family.rasti.widget.AnyutaDashboardWidget
import ru.family.rasti.widget.WidgetAction

class MainActivity : ComponentActivity() {
    private var pendingWidgetAction by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingWidgetAction = intent?.getStringExtra(WidgetAction.EXTRA)
        ReminderScheduler.schedule(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
        setContent {
            val viewModel: RastiViewModel = viewModel(
                factory = RastiViewModel.Factory(
                    LocalStore(this),
                    ReminderNotifier(applicationContext),
                    MaxMessenger(applicationContext),
                ),
            )
            val darkTheme = viewModel.appTheme == AppTheme.DARK
            RastiTheme(darkTheme = darkTheme) {
                val systemBarColor = MaterialTheme.colorScheme.background.toArgb()
                SideEffect {
                    @Suppress("DEPRECATION")
                    window.statusBarColor = systemBarColor
                    @Suppress("DEPRECATION")
                    window.navigationBarColor = systemBarColor
                    WindowCompat.getInsetsController(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !darkTheme
                        isAppearanceLightNavigationBars = !darkTheme
                    }
                }
                RastiApp(
                    viewModel = viewModel,
                    widgetAction = pendingWidgetAction,
                    onWidgetActionConsumed = {
                        pendingWidgetAction = null
                        intent?.removeExtra(WidgetAction.EXTRA)
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingWidgetAction = intent.getStringExtra(WidgetAction.EXTRA)
    }

    override fun onStart() {
        super.onStart()
        AnyutaDashboardWidget.updateAll(applicationContext)
        AppVisibility.inForeground = true
        ReminderNotifier(applicationContext).clearAll()
    }

    override fun onStop() {
        AppVisibility.inForeground = false
        super.onStop()
    }
}

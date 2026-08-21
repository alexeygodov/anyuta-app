package ru.family.rasti

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.family.rasti.data.LocalStore
import ru.family.rasti.notify.ReminderScheduler
import ru.family.rasti.ui.RastiApp
import ru.family.rasti.ui.theme.RastiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReminderScheduler.schedule(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }
        setContent {
            RastiTheme {
                val viewModel: RastiViewModel = viewModel(factory = RastiViewModel.Factory(LocalStore(this)))
                RastiApp(viewModel)
            }
        }
    }
}


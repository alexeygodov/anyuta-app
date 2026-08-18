package ru.family.rasti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.family.rasti.data.LocalStore
import ru.family.rasti.ui.RastiApp
import ru.family.rasti.ui.theme.RastiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RastiTheme {
                val viewModel: RastiViewModel = viewModel(factory = RastiViewModel.Factory(LocalStore(this)))
                RastiApp(viewModel)
            }
        }
    }
}


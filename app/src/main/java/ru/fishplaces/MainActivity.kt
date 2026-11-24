package ru.fishplaces

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.fishplaces.ui.FishPlacesApp
import ru.fishplaces.ui.SpotsViewModel

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<SpotsViewModel> {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SpotsViewModel((application as FishingApplication).repository) as T
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { ru.fishplaces.ui.AccountRoot(application as FishingApplication) }
    }
}

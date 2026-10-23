package dev.armaan.pd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.armaan.pd.ui.MainScreen
import dev.armaan.pd.ui.theme.Pd
import dev.armaan.pd.ui.theme.PdTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as PdApplication).container
        setContent {
            PdTheme {
                val vm: MainViewModel = viewModel(factory = viewModelFactory { initializer { MainViewModel(container) } })
                Box(Modifier.fillMaxSize().background(Pd.colors.stock)) {
                    MainScreen(vm)
                }
            }
        }
    }
}

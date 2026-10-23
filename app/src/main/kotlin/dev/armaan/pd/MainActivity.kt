package dev.armaan.pd

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.armaan.pd.app.Event
import dev.armaan.pd.app.Screen
import dev.armaan.pd.ui.MainScreen
import dev.armaan.pd.ui.theme.Pd
import dev.armaan.pd.ui.theme.PdTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels {
        viewModelFactory { initializer { MainViewModel((application as PdApplication).container) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PdTheme {
                Box(Modifier.fillMaxSize().background(Pd.colors.stock)) {
                    MainScreen(vm)
                }
            }
        }
    }

    /** The widget's tap reaches a running app here, and opens the Dashboard (SPEC §6.2). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_DASHBOARD, false)) vm.dispatch(Event.Open(Screen.Dashboard))
    }

    companion object {
        const val EXTRA_DASHBOARD = "dev.armaan.pd.extra.DASHBOARD"
    }
}

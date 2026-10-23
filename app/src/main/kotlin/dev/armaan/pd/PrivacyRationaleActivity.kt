package dev.armaan.pd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.armaan.pd.ui.settings.AboutScreen
import dev.armaan.pd.ui.theme.PdTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * The About screen on its own, for Health Connect's "privacy policy" link (SPEC §5.3, PRD A9).
 * Started by `ACTION_SHOW_PERMISSIONS_RATIONALE` on 28–33 and by the `VIEW_PERMISSION_USAGE`
 * alias on 34+. Back closes it. It follows Settings → Theme like the app (D27).
 */
class PrivacyRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val theme = (application as PdApplication).container.settings.settings.map { it.theme }.distinctUntilChanged()
        setContent {
            val choice by theme.collectAsStateWithLifecycle(initialValue = null)
            PdTheme(choice) { AboutScreen(onBack = ::finish) }
        }
    }
}

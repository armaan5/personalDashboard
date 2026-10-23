package dev.armaan.pd

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.armaan.pd.ui.settings.AboutScreen
import dev.armaan.pd.ui.theme.PdTheme

/**
 * The About screen on its own, for Health Connect's "privacy policy" link (SPEC §5.3, PRD A9).
 * Started by `ACTION_SHOW_PERMISSIONS_RATIONALE` on 28–33 and by the `VIEW_PERMISSION_USAGE`
 * alias on 34+. Back closes it.
 */
class PrivacyRationaleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PdTheme { AboutScreen(onBack = ::finish) }
        }
    }
}

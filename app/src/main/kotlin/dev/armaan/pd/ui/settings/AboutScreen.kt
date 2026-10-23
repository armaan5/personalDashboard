package dev.armaan.pd.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.armaan.pd.R
import dev.armaan.pd.ui.Section
import dev.armaan.pd.ui.TopBar
import dev.armaan.pd.ui.theme.Pd

/**
 * PRD A9: the version and a one-screen privacy statement. Also what Health Connect shows as this
 * app's permission rationale (SPEC §5.3), via [dev.armaan.pd.PrivacyRationaleActivity].
 */
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val version = remember(context) { versionName(context) }
    Scaffold(
        topBar = { TopBar(stringResource(R.string.about_title), onBack) },
        containerColor = Pd.colors.stock,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 32.dp),
        ) {
            Section {
                Text(
                    stringResource(R.string.app_name),
                    style = Pd.type.habitName,
                    color = Pd.colors.ink,
                    modifier = Modifier.padding(top = 14.dp),
                )
                Text(
                    stringResource(R.string.about_version, version),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Pd.colors.ink2,
                    modifier = Modifier.padding(top = 2.dp, bottom = 20.dp),
                )
            }
            Section {
                Text(
                    stringResource(R.string.about_privacy_head),
                    style = MaterialTheme.typography.titleLarge,
                    color = Pd.colors.ink,
                    modifier = Modifier.padding(top = 14.dp, bottom = 8.dp).semantics { heading() },
                )
                listOf(R.string.about_privacy_network, R.string.about_privacy_reads, R.string.about_privacy_revoke).forEach {
                    Text(
                        stringResource(it),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Pd.colors.ink,
                        modifier = Modifier.padding(vertical = 6.dp).widthIn(max = 560.dp),
                    )
                }
            }
        }
    }
}

private fun versionName(context: Context): String {
    val pm = context.packageManager
    val info = if (Build.VERSION.SDK_INT >= 33) {
        pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
    } else {
        @Suppress("DEPRECATION")
        pm.getPackageInfo(context.packageName, 0)
    }
    return info.versionName.orEmpty()
}

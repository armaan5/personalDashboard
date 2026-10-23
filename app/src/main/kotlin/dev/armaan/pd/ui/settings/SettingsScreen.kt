package dev.armaan.pd.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.armaan.pd.R
import dev.armaan.pd.ui.Section
import dev.armaan.pd.ui.TopBar
import dev.armaan.pd.ui.theme.Pd
import dev.armaan.pd.ui.theme.PdIcons

/** SPEC §6.1 Settings: Calendars here; Step goal and About arrive with T7. */
@Composable
fun SettingsScreen(onBack: () -> Unit, onOpenCalendars: () -> Unit) {
    Scaffold(
        topBar = { TopBar(stringResource(R.string.settings), onBack) },
        containerColor = Pd.colors.stock,
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 32.dp),
        ) {
            item {
                Section {
                    SettingsRow(
                        stringResource(R.string.settings_calendars),
                        stringResource(R.string.settings_calendars_hint),
                        onOpenCalendars,
                    )
                    HorizontalDivider(thickness = 1.dp, color = Pd.colors.rule)
                }
            }
        }
    }
}

/** A row that opens a sub-screen: title over a one-line hint, then a chevron. */
@Composable
private fun SettingsRow(title: String, hint: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Pd.type.habitName, color = Pd.colors.ink)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = Pd.colors.ink2, modifier = Modifier.padding(top = 3.dp))
        }
        Icon(PdIcons.Chevron, contentDescription = null, tint = Pd.colors.ink2)
    }
}

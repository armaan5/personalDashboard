package dev.armaan.pd.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.armaan.pd.R
import dev.armaan.pd.app.Access
import dev.armaan.pd.calendar.DeviceCalendar
import dev.armaan.pd.ui.Blocked
import dev.armaan.pd.ui.EmptyLine
import dev.armaan.pd.ui.Section
import dev.armaan.pd.ui.TopBar
import dev.armaan.pd.ui.theme.Pd

/**
 * SPEC §5.2 Settings → Calendars: every calendar on the device with its swatch and a switch. The
 * switches start from the default (every visible calendar); the first toggle writes the set.
 */
@Composable
fun CalendarsScreen(
    access: Access?,
    calendars: List<DeviceCalendar>,
    shown: Set<String>,
    onBack: () -> Unit,
    onGrant: () -> Unit,
    onToggle: (calendarId: String, on: Boolean) -> Unit,
) {
    Scaffold(
        topBar = { TopBar(stringResource(R.string.calendars_title), onBack) },
        containerColor = Pd.colors.stock,
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 32.dp),
        ) {
            // One ruled section, as on Settings (D23).
            item {
                Section {
                    when {
                        access == null -> Unit
                        access != Access.Granted ->
                            Blocked(stringResource(R.string.calendars_off), stringResource(R.string.grant_access), onGrant)
                        calendars.isEmpty() -> EmptyLine(stringResource(R.string.calendars_none))
                        else -> calendars.forEachIndexed { i, calendar ->
                            CalendarRow(calendar, on = calendar.id in shown, onToggle = { onToggle(calendar.id, it) })
                            if (i < calendars.lastIndex) HorizontalDivider(thickness = 1.dp, color = Pd.colors.rule)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarRow(calendar: DeviceCalendar, on: Boolean, onToggle: (Boolean) -> Unit) {
    val c = Pd.colors
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(value = on, role = Role.Switch, onValueChange = onToggle)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The provider's colour is the calendar's own; a hairline keeps pale ones visible.
        Box(
            Modifier
                .size(12.dp)
                .background(Color(calendar.color).copy(alpha = 1f), CircleShape)
                .border(1.dp, c.rule, CircleShape),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(calendar.name, style = Pd.type.habitName, color = c.ink)
            if (calendar.account.isNotBlank() && calendar.account != calendar.name) {
                Text(calendar.account, style = Pd.type.schedule, color = c.ink2, modifier = Modifier.padding(top = 3.dp))
            }
        }
        Switch(
            checked = on,
            // The row is the toggle target, so the switch itself is not separately focusable.
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.onInk,
                checkedTrackColor = c.ink,
                checkedBorderColor = c.ink,
                uncheckedThumbColor = c.ink2,
                uncheckedTrackColor = c.stock,
                uncheckedBorderColor = c.ink2,
            ),
        )
    }
}

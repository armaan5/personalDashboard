package dev.armaan.pd.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.armaan.pd.R
import dev.armaan.pd.habit.HABIT_NAME_MAX
import dev.armaan.pd.habit.Habit
import dev.armaan.pd.habit.cleanHabitName
import dev.armaan.pd.ui.SheetHandle
import dev.armaan.pd.ui.UiLocale
import dev.armaan.pd.ui.theme.Pd
import dev.armaan.pd.ui.theme.PdIcons
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.format.TextStyle

/**
 * SPEC §6.1 add/edit sheet: name and weekday chips, all seven chosen by default (D14). Delete
 * only when editing; it has no confirmation, the snackbar's Undo restores it (PRD A7).
 * Validation is `:core`'s [cleanHabitName]; the reducer refuses anything it rejects anyway.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitSheet(
    habit: Habit?,
    onSave: (name: String, weekdays: Set<DayOfWeek>) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun close(then: () -> Unit) {
        then()
        scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss() }
    }

    var name by rememberSaveable { mutableStateOf(habit?.name.orEmpty()) }
    var days by rememberSaveable {
        mutableStateOf((habit?.weekdays ?: DayOfWeek.entries.toSet()).map { it.value })
    }
    val chosen = days.map(DayOfWeek::of).toSet()
    val valid = cleanHabitName(name) != null && chosen.isNotEmpty()
    val c = Pd.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = c.stock,
        contentColor = c.ink,
        scrimColor = c.scrim,
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
        dragHandle = { SheetHandle() },
    ) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
                .imePadding()
                .padding(bottom = 16.dp),
        ) {
            Text(
                stringResource(if (habit == null) R.string.add_habit else R.string.edit_habit),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 18.dp).semantics { heading() },
            )
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= HABIT_NAME_MAX) name = it },
                label = { Text(stringResource(R.string.habit_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                supportingText = {
                    Row(Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.habit_name_helper, HABIT_NAME_MAX), Modifier.weight(1f))
                        Text(stringResource(R.string.habit_name_count, name.length, HABIT_NAME_MAX))
                    }
                },
                shape = RoundedCornerShape(4.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = c.ink,
                    unfocusedBorderColor = c.ink,
                    focusedLabelColor = c.ink2,
                    unfocusedLabelColor = c.ink2,
                    cursorColor = c.calendar,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.habit_days).uppercase(UiLocale),
                style = Pd.type.caps,
                color = c.ink2,
                modifier = Modifier.padding(top = 18.dp, bottom = 10.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DayOfWeek.entries.forEach { day ->
                    val on = day in chosen
                    DayChip(day, on, Modifier.weight(1f)) {
                        days = if (on) days - day.value else days + day.value
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 26.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (habit != null) {
                    TextButton(onClick = { close(onDelete) }) {
                        Icon(PdIcons.Delete, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text(stringResource(R.string.delete), textDecoration = TextDecoration.Underline)
                    }
                } else {
                    Box(Modifier)
                }
                Button(
                    onClick = { close { onSave(name, chosen) } },
                    enabled = valid,
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.ink, contentColor = c.onInk),
                    modifier = Modifier.height(48.dp),
                ) { Text(stringResource(R.string.save)) }
            }
        }
    }
}

/** A 48 dp square weekday chip; announced by its full name and selected state. */
@Composable
private fun DayChip(day: DayOfWeek, on: Boolean, modifier: Modifier, onToggle: () -> Unit) {
    val c = Pd.colors
    val shape = RoundedCornerShape(4.dp)
    val full = day.getDisplayName(TextStyle.FULL, UiLocale)
    Box(
        modifier
            .height(48.dp)
            .then(if (on) Modifier.background(c.ink, shape) else Modifier.border(1.5.dp, c.ink2, shape))
            .toggleable(value = on, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = full },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            day.getDisplayName(TextStyle.NARROW, UiLocale),
            style = Pd.type.week.copy(fontSize = 17.sp),
            color = if (on) c.onInk else c.ink,
            modifier = Modifier.clearAndSetSemantics {},
        )
    }
}

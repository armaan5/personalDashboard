package dev.armaan.pd.ui.todos

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.remember
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
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import dev.armaan.pd.R
import dev.armaan.pd.todo.TODO_TITLE_MAX
import dev.armaan.pd.todo.Todo
import dev.armaan.pd.todo.cleanTodoTitle
import dev.armaan.pd.ui.SheetHandle
import dev.armaan.pd.ui.UiLocale
import dev.armaan.pd.ui.theme.Pd
import dev.armaan.pd.ui.theme.PdIcons
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val SheetDate = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", UiLocale)

/**
 * SPEC §6.1 add/edit sheet: a title and an optional due date from the Material date picker.
 * Validation is `:core`'s [cleanTodoTitle]; the reducer refuses anything it rejects anyway.
 * To-dos have no delete in v1: they are ticked, then cleared from history (SPEC §4.3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoSheet(
    todo: Todo?,
    onSave: (title: String, dueDate: LocalDate?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun close(then: () -> Unit) {
        then()
        scope.launch { sheet.hide() }.invokeOnCompletion { onDismiss() }
    }

    var title by rememberSaveable { mutableStateOf(todo?.title.orEmpty()) }
    // Epoch day, so it survives recreation as a plain Long.
    var dueDay by rememberSaveable { mutableStateOf(todo?.dueDate?.toEpochDay()) }
    var picking by rememberSaveable { mutableStateOf(false) }
    val due = dueDay?.let(LocalDate::ofEpochDay)
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
                stringResource(if (todo == null) R.string.add_todo else R.string.edit_todo),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 18.dp).semantics { heading() },
            )
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= TODO_TITLE_MAX) title = it },
                label = { Text(stringResource(R.string.todo_title)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                supportingText = {
                    Row(Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.todo_title_helper, TODO_TITLE_MAX), Modifier.weight(1f))
                        Text(stringResource(R.string.habit_name_count, title.length, TODO_TITLE_MAX))
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
                stringResource(R.string.todo_due_date).uppercase(UiLocale),
                style = Pd.type.caps,
                color = c.ink2,
                modifier = Modifier.padding(top = 18.dp, bottom = 10.dp),
            )
            DateField(due, onPick = { picking = true }, onClear = { dueDay = null })
            Row(
                Modifier.fillMaxWidth().padding(top = 26.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = { close { onSave(title, due) } },
                    enabled = cleanTodoTitle(title) != null,
                    shape = RoundedCornerShape(4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = c.ink, contentColor = c.onInk),
                    modifier = Modifier.height(48.dp),
                ) { Text(stringResource(R.string.save)) }
            }
        }
    }

    if (picking) {
        DueDatePicker(due, onPicked = { dueDay = it.toEpochDay() }, onDismiss = { picking = false })
    }
}

/** A 48 dp field showing the due date or “No date”; tapping it opens the picker. */
@Composable
private fun DateField(due: LocalDate?, onPick: () -> Unit, onClear: () -> Unit) {
    val c = Pd.colors
    val shown = due?.format(SheetDate) ?: stringResource(R.string.todo_no_date)
    val spoken = stringResource(R.string.todo_date_cd, shown)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .border(1.dp, c.ink, RoundedCornerShape(4.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .heightIn(min = 52.dp)
                .clickable(onClick = onPick)
                .clearAndSetSemantics {
                    contentDescription = spoken
                    role = Role.Button
                }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(PdIcons.Event, contentDescription = null, tint = c.ink2)
            Text(shown, style = MaterialTheme.typography.bodyLarge, color = if (due == null) c.ink2 else c.ink)
        }
        if (due != null) {
            IconButton(onClick = onClear) {
                Icon(PdIcons.Close, contentDescription = stringResource(R.string.todo_remove_date), tint = c.ink)
            }
        }
    }
}

/** The Material date picker. It speaks UTC midnight millis; a due date is a plain day (SPEC §4.1). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DueDatePicker(due: LocalDate?, onPicked: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    // en-GB (D21): weeks start on Monday and dates read the UK way whatever the device locale.
    val state = remember {
        DatePickerState(
            locale = UiLocale,
            initialSelectedDateMillis = due?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        )
    }
    val c = Pd.colors
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let {
                        onPicked(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    onDismiss()
                },
                enabled = state.selectedDateMillis != null,
            ) { Text(stringResource(R.string.ok), color = c.ink) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = c.ink) }
        },
        colors = DatePickerDefaults.colors(containerColor = c.stock),
    ) {
        DatePicker(
            state = state,
            colors = DatePickerDefaults.colors(
                containerColor = c.stock,
                selectedDayContainerColor = c.ink,
                selectedDayContentColor = c.onInk,
                todayDateBorderColor = c.ink,
                todayContentColor = c.ink,
            ),
        )
    }
}

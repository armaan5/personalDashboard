package dev.armaan.pd.ui.todos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.armaan.pd.R
import dev.armaan.pd.todo.Buckets
import dev.armaan.pd.ui.EmptyLine
import dev.armaan.pd.ui.Section
import dev.armaan.pd.ui.SectionHead
import dev.armaan.pd.ui.TopBar
import dev.armaan.pd.ui.UiLocale
import dev.armaan.pd.ui.dashboard.TodoKind
import dev.armaan.pd.ui.dashboard.TodoLine
import dev.armaan.pd.ui.dashboard.cardRows
import dev.armaan.pd.ui.theme.Pd
import dev.armaan.pd.ui.theme.PdIcons
import java.time.LocalDate

private const val NONE = -1L

/**
 * SPEC §4.3 To-dos screen: the card's sections, then upcoming (soonest first), then a collapsible
 * Done with history (most recent first) and Clear history.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodosScreen(
    buckets: Buckets,
    anyTodos: Boolean,
    snackbar: SnackbarHostState,
    startAdding: Boolean,
    onStartedAdding: () -> Unit,
    onBack: () -> Unit,
    onTick: (todoId: Long) -> Unit,
    onSave: (id: Long, title: String, dueDate: LocalDate?) -> Unit,
    onClearHistory: () -> Unit,
) {
    // The sheet's target: NONE closed, 0 a new to-do, otherwise the to-do being edited.
    var editing by rememberSaveable { mutableLongStateOf(NONE) }
    var showDone by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(startAdding) {
        if (startAdding) {
            editing = 0L
            onStartedAdding()
        }
    }
    val rows = cardRows(buckets)
    val left = buckets.overdue.size + buckets.today.size + buckets.undated.size

    Scaffold(
        topBar = { TopBar(stringResource(R.string.todos_title), onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            val label = stringResource(R.string.add_todo)
            ExtendedFloatingActionButton(
                // Its text isn't in the merged tree on its own, so TalkBack gets the label here.
                modifier = Modifier.semantics { contentDescription = label },
                onClick = { editing = 0L },
                icon = { Icon(PdIcons.Add, contentDescription = null) },
                text = { Text(label) },
                containerColor = Pd.colors.ink,
                contentColor = Pd.colors.onInk,
            )
        },
        containerColor = Pd.colors.stock,
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item {
                Section {
                    val title = stringResource(R.string.todos_today)
                    val description = if (rows.isEmpty()) title else pluralStringResource(R.plurals.todos_card_cd, left, left)
                    SectionHead(title, description, Pd.colors.todos)
                    when {
                        !anyTodos -> EmptyLine(stringResource(R.string.todos_none_yet))
                        rows.isEmpty() -> EmptyLine(stringResource(R.string.todos_nothing_due_screen))
                        else -> rows.forEachIndexed { i, (todo, kind) ->
                            TodoLine(
                                todo,
                                kind,
                                onTick = { onTick(todo.id) },
                                onEdit = { editing = todo.id },
                                divider = i < rows.lastIndex,
                            )
                        }
                    }
                }
            }
            if (buckets.upcoming.isNotEmpty()) {
                item {
                    Section {
                        val title = stringResource(R.string.todos_upcoming)
                        SectionHead(title, title, dot = null)
                        buckets.upcoming.forEachIndexed { i, todo ->
                            TodoLine(
                                todo,
                                TodoKind.Upcoming,
                                onTick = { onTick(todo.id) },
                                onEdit = { editing = todo.id },
                                divider = i < buckets.upcoming.lastIndex,
                            )
                        }
                    }
                }
            }
            if (buckets.history.isNotEmpty()) {
                item {
                    Section {
                        DoneHead(buckets.history.size, showDone, onToggle = { showDone = !showDone })
                        if (showDone) {
                            buckets.history.forEach { todo -> TodoLine(todo, TodoKind.History, onTick = null) }
                            TextButton(onClick = onClearHistory, modifier = Modifier.padding(top = 4.dp)) {
                                Icon(PdIcons.Delete, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                                Text(stringResource(R.string.clear_history), textDecoration = TextDecoration.Underline)
                            }
                        }
                    }
                }
            }
        }
    }

    if (editing != NONE) {
        val all = rows.map { it.first } + buckets.upcoming
        val todo = all.find { it.id == editing }
        // A to-do that left the editable buckets (cleared, or gone) closes its sheet.
        if (editing != 0L && todo == null) {
            editing = NONE
        } else {
            TodoSheet(
                todo = todo,
                onSave = { title, due -> onSave(editing, title, due) },
                onDismiss = { editing = NONE },
            )
        }
    }
}

/** The collapsible Done head: a 48 dp row announced with its count and expanded state. */
@Composable
private fun DoneHead(count: Int, expanded: Boolean, onToggle: () -> Unit) {
    val description = pluralStringResource(R.plurals.todos_done_cd, count, count)
    val state = stringResource(if (expanded) R.string.todos_expanded else R.string.todos_collapsed)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onToggle)
            .clearAndSetSemantics {
                contentDescription = description
                stateDescription = state
                heading()
                role = Role.Button
                onClick { onToggle(); true }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            stringResource(R.string.todos_done).uppercase(UiLocale),
            style = Pd.type.section,
            color = Pd.colors.ink,
        )
        Text(count.toString(), style = Pd.type.meta, color = Pd.colors.ink2, modifier = Modifier.weight(1f))
        Icon(
            PdIcons.Expand,
            contentDescription = null,
            tint = Pd.colors.ink2,
            modifier = Modifier.rotate(if (expanded) 180f else 0f),
        )
    }
}

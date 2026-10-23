package dev.armaan.pd.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.armaan.pd.R
import dev.armaan.pd.todo.Buckets
import dev.armaan.pd.todo.Todo
import dev.armaan.pd.ui.EmptyLine
import dev.armaan.pd.ui.Section
import dev.armaan.pd.ui.SectionHead
import dev.armaan.pd.ui.UiLocale
import dev.armaan.pd.ui.theme.Pd
import dev.armaan.pd.ui.theme.PdIcons
import java.time.format.DateTimeFormatter

/** Which §4.3 bucket a row came from; it decides the label at the row's end. */
enum class TodoKind { Overdue, Today, Undated, Upcoming, DoneToday, History }

/** SPEC §4.3 card: overdue, today, undated, then done today struck through. */
@Composable
fun TodosCard(
    buckets: Buckets,
    anyTodos: Boolean,
    onOpen: () -> Unit,
    onAdd: () -> Unit,
    onTick: (todoId: Long) -> Unit,
) {
    val rows = cardRows(buckets)
    val left = buckets.overdue.size + buckets.today.size + buckets.undated.size
    val description = when {
        !anyTodos -> stringResource(R.string.todos_card_none_cd)
        rows.isEmpty() -> stringResource(R.string.todos_card_nothing_due_cd)
        else -> pluralStringResource(R.plurals.todos_card_cd, left, left)
    }
    Section {
        SectionHead(stringResource(R.string.section_todos), description, Pd.colors.todos, onClick = onOpen) {
            if (rows.isNotEmpty()) LeftMeta(left)
        }
        when {
            !anyTodos -> {
                EmptyLine(stringResource(R.string.todos_none_yet))
                AddLink(stringResource(R.string.add_a_todo), onAdd)
            }
            rows.isEmpty() -> EmptyLine(stringResource(R.string.todos_nothing_due), Modifier.padding(bottom = 8.dp))
            else -> rows.forEachIndexed { i, (todo, kind) ->
                TodoLine(todo, kind, onTick = { onTick(todo.id) }, divider = i < rows.lastIndex)
            }
        }
    }
}

/** The card's rows in SPEC §4.3 order, each tagged with its bucket. */
fun cardRows(b: Buckets): List<Pair<Todo, TodoKind>> =
    b.overdue.map { it to TodoKind.Overdue } +
        b.today.map { it to TodoKind.Today } +
        b.undated.map { it to TodoKind.Undated } +
        b.doneToday.map { it to TodoKind.DoneToday }

/** “3 left” beside the section title, as the comp sets it. */
@Composable
private fun LeftMeta(left: Int) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(left.toString(), style = Pd.type.count, color = Pd.colors.ink)
        Spacer(Modifier.width(4.dp))
        Text(
            stringResource(R.string.todos_left),
            style = Pd.type.meta,
            color = Pd.colors.ink2,
            modifier = Modifier.padding(bottom = 3.dp),
        )
    }
}

private val ShortDate = DateTimeFormatter.ofPattern("EEE d MMM", UiLocale)
private val LongDate = DateTimeFormatter.ofPattern("EEEE d MMMM", UiLocale)

/**
 * One to-do on a ruled line: a 48 dp box, the title, the due label (D23). [onTick] null leaves
 * the box static (history can't be unticked, PRD A8). [onEdit] makes the title open the sheet.
 */
@Composable
fun TodoLine(
    todo: Todo,
    kind: TodoKind,
    onTick: (() -> Unit)?,
    onEdit: (() -> Unit)? = null,
    divider: Boolean = true,
) {
    val done = kind == TodoKind.DoneToday || kind == TodoKind.History
    val editLabel = stringResource(R.string.todo_edit_action)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tick = stringResource(R.string.todo_tick_cd, todo.title)
        val toggle = if (onTick != null) {
            Modifier
                .toggleable(value = done, role = Role.Checkbox, onValueChange = { onTick() })
                .semantics { contentDescription = tick }
        } else {
            Modifier.clearAndSetSemantics {}
        }
        Box(Modifier.size(48.dp).then(toggle), contentAlignment = Alignment.Center) { CheckBox(done) }
        Spacer(Modifier.width(8.dp))
        val edit = if (onEdit != null) Modifier.clickable(onClickLabel = editLabel, onClick = onEdit) else Modifier
        Row(
            Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .then(edit)
                .semantics(mergeDescendants = true) {
                    if (onEdit != null) onClick(editLabel) { onEdit(); true }
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                todo.title,
                style = Pd.type.todoTitle.copy(
                    textDecoration = if (done) TextDecoration.LineThrough else null,
                ),
                color = if (done) Pd.colors.ink2 else Pd.colors.ink,
                modifier = Modifier.weight(1f).padding(vertical = 6.dp),
            )
            DueLabel(todo, kind)
        }
    }
    if (divider) HorizontalDivider(thickness = 1.dp, color = Pd.colors.rule)
}

@Composable
private fun DueLabel(todo: Todo, kind: TodoKind) {
    val c = Pd.colors
    when (kind) {
        TodoKind.Overdue -> Row(
            Modifier.padding(end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Pennant()
            Text(stringResource(R.string.todo_overdue).uppercase(UiLocale), style = Pd.type.overdue, color = c.ink)
        }
        TodoKind.Today -> DateText(stringResource(R.string.todo_due_today), null)
        TodoKind.Upcoming -> todo.dueDate?.let {
            DateText(it.format(ShortDate), stringResource(R.string.todo_due_cd, it.format(LongDate)))
        }
        TodoKind.History -> todo.completedOn?.let {
            DateText(it.format(ShortDate), stringResource(R.string.todo_done_on_cd, it.format(LongDate)))
        }
        TodoKind.Undated, TodoKind.DoneToday -> Unit
    }
}

@Composable
private fun DateText(text: String, spoken: String?) {
    val a11y = if (spoken != null) Modifier.semantics { contentDescription = spoken } else Modifier
    Text(text, style = Pd.type.due, color = Pd.colors.ink2, modifier = Modifier.padding(end = 2.dp).then(a11y))
}

/** The small yellow pennant that marks overdue; not red (D23, PRODUCT.md “calm”). */
@Composable
private fun Pennant() {
    val fill = Pd.colors.todos
    Canvas(Modifier.size(width = 10.dp, height = 9.dp)) {
        val p = Path().apply {
            moveTo(size.width / 2, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(p, fill)
    }
}

/** 24 dp square box; done is a yellow sticker with an ink check, set at a 3° tilt (D23). */
@Composable
private fun CheckBox(done: Boolean) {
    val c = Pd.colors
    val shape = RoundedCornerShape(4.dp)
    Box(
        Modifier
            .size(24.dp)
            .then(
                if (done) Modifier.rotate(-3f).background(c.todos, shape)
                else Modifier.border(2.dp, c.ink2, shape),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (done) Icon(PdIcons.Check, contentDescription = null, tint = c.onTodos, modifier = Modifier.size(18.dp))
    }
}

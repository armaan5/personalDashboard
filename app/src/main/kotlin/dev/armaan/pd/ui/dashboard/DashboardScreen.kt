package dev.armaan.pd.ui.dashboard

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import dev.armaan.pd.R
import dev.armaan.pd.app.Access
import dev.armaan.pd.calendar.Agenda
import dev.armaan.pd.habit.HabitsToday
import dev.armaan.pd.todo.Buckets
import dev.armaan.pd.ui.Section
import dev.armaan.pd.ui.SectionHead
import dev.armaan.pd.ui.UiLocale
import dev.armaan.pd.ui.theme.Pd
import dev.armaan.pd.ui.theme.PdIcons
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields

/**
 * SPEC §6.1: four sections in the D18 order on one ruled sheet (D23). Steps is a titled
 * placeholder until T7 fills it.
 */
@Composable
fun DashboardScreen(
    today: LocalDate,
    onOpenSettings: () -> Unit,
    calendarAccess: Access?,
    agenda: Agenda,
    now: Instant,
    zone: ZoneId,
    onCalendarShown: () -> Unit,
    onGrantCalendar: () -> Unit,
    habits: HabitsToday,
    anyHabits: Boolean,
    onOpenHabits: () -> Unit,
    onAddHabit: () -> Unit,
    onTick: (habitId: Long) -> Unit,
    todos: Buckets,
    anyTodos: Boolean,
    onOpenTodos: () -> Unit,
    onAddTodo: () -> Unit,
    onTickTodo: (todoId: Long) -> Unit,
) {
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val dir = LocalLayoutDirection.current
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = insets.calculateStartPadding(dir),
            end = insets.calculateEndPadding(dir),
            top = insets.calculateTopPadding(),
            bottom = insets.calculateBottomPadding() + 32.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { Masthead(today, onOpenSettings) }
        item { CalendarCard(calendarAccess, agenda, now, zone, onCalendarShown, onGrantCalendar, onOpenSettings) }
        item { HabitsCard(habits, anyHabits, today, onOpenHabits, onAddHabit, onTick) }
        item { TodosCard(todos, anyTodos, onOpenTodos, onAddTodo, onTickTodo) }
        item { Placeholder(stringResource(R.string.section_steps), Pd.colors.steps) }
    }
}

/** Weekday over the date, set like a planner column head, the ISO week number, and Settings. */
@Composable
private fun Masthead(today: LocalDate, onOpenSettings: () -> Unit) {
    val day = today.format(DateTimeFormatter.ofPattern("EEEE", UiLocale))
    val date = today.format(DateTimeFormatter.ofPattern("d MMMM", UiLocale))
    val week = today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
    val description = stringResource(R.string.masthead_cd, day, date, week)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(Modifier.weight(1f).clearAndSetSemantics { contentDescription = description }) {
            Text(day, style = Pd.type.day, color = Pd.colors.ink2)
            Spacer(Modifier.height(4.dp))
            Text(date, style = Pd.type.date, color = Pd.colors.ink)
        }
        Text(
            stringResource(R.string.week_label, week).uppercase(UiLocale),
            style = Pd.type.week,
            color = Pd.colors.ink,
            modifier = Modifier
                .padding(bottom = 11.dp)
                .clearAndSetSemantics {}
                .border(1.dp, Pd.colors.ink, RoundedCornerShape(3.dp))
                .padding(horizontal = 7.dp, vertical = 4.dp),
        )
        IconButton(onClick = onOpenSettings, modifier = Modifier.padding(start = 4.dp)) {
            Icon(PdIcons.Settings, contentDescription = stringResource(R.string.settings), tint = Pd.colors.ink)
        }
    }
}

@Composable
private fun Placeholder(title: String, dot: androidx.compose.ui.graphics.Color) {
    Section { SectionHead(title, title, dot) }
}

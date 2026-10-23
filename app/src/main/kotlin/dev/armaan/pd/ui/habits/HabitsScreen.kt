package dev.armaan.pd.ui.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.armaan.pd.R
import dev.armaan.pd.habit.Habit
import dev.armaan.pd.habit.HabitsToday
import dev.armaan.pd.habit.Weekdays
import dev.armaan.pd.ui.CountMeta
import dev.armaan.pd.ui.EmptyLine
import dev.armaan.pd.ui.HabitLine
import dev.armaan.pd.ui.Section
import dev.armaan.pd.ui.SectionHead
import dev.armaan.pd.ui.TopBar
import dev.armaan.pd.ui.UiLocale
import dev.armaan.pd.ui.WeekLetters
import dev.armaan.pd.ui.theme.Pd
import dev.armaan.pd.ui.theme.PdIcons
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.LocalDate
import java.time.format.TextStyle

private const val NONE = -1L

/** SPEC §6.1 Habits: today's scheduled habits with tick and streak, then “Not today”. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    today: LocalDate,
    habits: HabitsToday,
    snackbar: SnackbarHostState,
    startAdding: Boolean,
    onStartedAdding: () -> Unit,
    onBack: () -> Unit,
    onTick: (habitId: Long) -> Unit,
    onSave: (id: Long, name: String, weekdays: Set<DayOfWeek>) -> Unit,
    onDelete: (habitId: Long) -> Unit,
) {
    // The sheet's target: NONE closed, 0 a new habit, otherwise the habit being edited.
    var editing by rememberSaveable { mutableLongStateOf(NONE) }
    LaunchedEffect(startAdding) {
        if (startAdding) {
            editing = 0L
            onStartedAdding()
        }
    }
    val all = habits.scheduled + habits.notToday

    Scaffold(
        topBar = { TopBar(stringResource(R.string.habits_title), onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            val label = stringResource(R.string.add_habit)
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
                    val description = if (habits.total > 0) {
                        stringResource(R.string.habits_card_cd, habits.done, habits.total)
                    } else {
                        stringResource(R.string.habits_card_none_cd)
                    }
                    SectionHead(stringResource(R.string.habits_today), description, Pd.colors.habits) {
                        if (habits.total > 0) CountMeta(habits.done, habits.total)
                    }
                    when {
                        all.isEmpty() -> EmptyLine(stringResource(R.string.habits_none_yet))
                        habits.total == 0 -> EmptyLine(stringResource(R.string.habits_none_today))
                        else -> {
                            WeekLetters(today)
                            habits.scheduled.forEachIndexed { i, row ->
                                HabitLine(
                                    row,
                                    onTick = { onTick(row.habit.id) },
                                    schedule = schedule(row.habit.weekdays),
                                    onEdit = { editing = row.habit.id },
                                    divider = i < habits.scheduled.lastIndex,
                                )
                            }
                        }
                    }
                }
            }
            if (habits.notToday.isNotEmpty()) {
                item {
                    Section {
                        val title = stringResource(R.string.habits_not_today)
                        SectionHead(title, title, dot = null)
                        habits.notToday.forEachIndexed { i, row ->
                            HabitLine(
                                row,
                                onTick = null,
                                schedule = schedule(row.habit.weekdays),
                                onEdit = { editing = row.habit.id },
                                divider = i < habits.notToday.lastIndex,
                            )
                        }
                    }
                }
            }
        }
    }

    if (editing != NONE) {
        val habit: Habit? = all.find { it.habit.id == editing }?.habit
        // A deleted or vanished habit closes its sheet.
        if (editing != 0L && habit == null) {
            editing = NONE
        } else {
            HabitSheet(
                habit = habit,
                onSave = { name, days -> onSave(editing, name, days) },
                onDelete = { onDelete(editing) },
                onDismiss = { editing = NONE },
            )
        }
    }
}

/** “Every day”, “Mon–Fri”, or the short day names in order (en-GB). */
@Composable
fun schedule(weekdays: Set<DayOfWeek>): String = when {
    Weekdays.toMask(weekdays) == Weekdays.EVERY_DAY -> stringResource(R.string.schedule_every_day)
    weekdays == (MONDAY..FRIDAY).toSet() -> stringResource(R.string.schedule_weekdays)
    else -> weekdays.sorted().joinToString(stringResource(R.string.schedule_separator)) {
        it.getDisplayName(TextStyle.SHORT, UiLocale)
    }
}

private operator fun DayOfWeek.rangeTo(end: DayOfWeek): List<DayOfWeek> =
    DayOfWeek.entries.filter { it.value in value..end.value }

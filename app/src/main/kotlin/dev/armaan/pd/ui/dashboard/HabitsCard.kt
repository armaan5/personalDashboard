package dev.armaan.pd.ui.dashboard

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.armaan.pd.R
import dev.armaan.pd.habit.HabitsToday
import dev.armaan.pd.ui.CountMeta
import dev.armaan.pd.ui.EmptyLine
import dev.armaan.pd.ui.HabitLine
import dev.armaan.pd.ui.Section
import dev.armaan.pd.ui.SectionHead
import dev.armaan.pd.ui.WeekLetters
import dev.armaan.pd.ui.theme.Pd
import dev.armaan.pd.ui.theme.PdIcons
import java.time.LocalDate

/** SPEC §4.2 card: today's scheduled habits only, with done / scheduled in the head. */
@Composable
fun HabitsCard(
    habits: HabitsToday,
    anyHabits: Boolean,
    today: LocalDate,
    onOpen: () -> Unit,
    onAdd: () -> Unit,
    onTick: (habitId: Long) -> Unit,
) {
    val description = if (habits.total > 0) {
        stringResource(R.string.habits_card_cd, habits.done, habits.total)
    } else {
        stringResource(R.string.habits_card_none_cd)
    }
    Section {
        SectionHead(stringResource(R.string.section_habits), description, Pd.colors.habits, onClick = onOpen) {
            if (habits.total > 0) CountMeta(habits.done, habits.total)
        }
        when {
            !anyHabits -> {
                EmptyLine(stringResource(R.string.habits_none_yet))
                AddLink(stringResource(R.string.add_a_habit), onAdd)
            }
            habits.total == 0 -> EmptyLine(stringResource(R.string.habits_none_today), Modifier.padding(bottom = 8.dp))
            else -> {
                WeekLetters(today)
                habits.scheduled.forEachIndexed { i, row ->
                    HabitLine(row, onTick = { onTick(row.habit.id) }, divider = i < habits.scheduled.lastIndex)
                }
            }
        }
    }
}

/** An inline text action in ink, underlined like a planner note (D23). */
@Composable
fun AddLink(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.padding(bottom = 8.dp)) {
        Icon(PdIcons.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
        Text(label, textDecoration = TextDecoration.Underline)
    }
}

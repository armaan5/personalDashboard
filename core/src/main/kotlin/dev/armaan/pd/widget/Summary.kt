package dev.armaan.pd.widget

import dev.armaan.pd.calendar.Instance
import dev.armaan.pd.calendar.agenda
import dev.armaan.pd.habit.Habit
import dev.armaan.pd.habit.habitsToday
import dev.armaan.pd.steps.StepsCard
import dev.armaan.pd.time.today
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** SPEC §6.2: the widget's calendar line. */
sealed interface NextLine {
    /** "HH:mm Title", prefixed "Tomorrow" when [tomorrow]. */
    data class Event(val event: Instance, val tomorrow: Boolean) : NextLine

    /** "Nothing scheduled". */
    data object Nothing : NextLine

    /** Calendar access isn't granted: a dash and "Open to set up". */
    data object SetUp : NextLine
}

/** SPEC §6.2: the widget's steps line. */
sealed interface StepsLine {
    /** "today / goal". */
    data class Count(val today: Long, val goal: Int) : StepsLine

    /** Not granted, or Health Connect needs installing: a dash and "Open to set up". */
    data object SetUp : StepsLine

    /** Nothing opening the app would fix (unavailable, no data, not read): a dash alone. */
    data object Dash : StepsLine
}

/** SPEC §6.2: the widget's habits line. */
sealed interface HabitsLine {
    /** "done / scheduled". */
    data class Count(val done: Int, val scheduled: Int) : HabitsLine

    /** "No habits today". */
    data object NoneToday : HabitsLine
}

data class Summary(val today: LocalDate, val next: NextLine, val steps: StepsLine, val habits: HabitsLine)

/**
 * SPEC §6.2, over the same snapshots and rules the app's cards use: [agenda] for the calendar,
 * the Steps card state, and [habitsToday].
 */
fun summary(
    now: Instant,
    zone: ZoneId,
    calendarGranted: Boolean,
    instances: List<Instance>,
    steps: StepsCard?,
    stepGoal: Int,
    habits: List<Habit>,
    completions: Map<Long, Set<LocalDate>>,
): Summary {
    val today = today(now, zone)
    return Summary(
        today = today,
        next = if (calendarGranted) nextEvent(instances, now, zone) else NextLine.SetUp,
        steps = when (steps) {
            is StepsCard.Steps -> StepsLine.Count(steps.today, stepGoal)
            is StepsCard.Blocked, StepsCard.NeedsInstall -> StepsLine.SetUp
            StepsCard.Unavailable, StepsCard.NoData, null -> StepsLine.Dash
        },
        habits = habitsToday(habits, completions, today).let {
            if (it.total == 0) HabitsLine.NoneToday else HabitsLine.Count(it.done, it.total)
        },
    )
}

/** The first timed event still to start or in progress, today, else tomorrow. */
private fun nextEvent(instances: List<Instance>, now: Instant, zone: ZoneId): NextLine {
    val days = agenda(instances, now, zone)
    days.today.timed.firstOrNull()?.let { return NextLine.Event(it, tomorrow = false) }
    days.tomorrow.timed.firstOrNull()?.let { return NextLine.Event(it, tomorrow = true) }
    return NextLine.Nothing
}

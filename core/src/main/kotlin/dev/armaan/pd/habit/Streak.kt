package dev.armaan.pd.habit

import java.time.LocalDate

/** SPEC §4.2 streak, against the habit's current weekdays. [done] is the habit's completion dates. */
fun streak(habit: Habit, done: Set<LocalDate>, today: LocalDate): Int {
    // Today still open neither counts nor breaks the run.
    var day = if (habit.isScheduledOn(today) && today !in done) today.minusDays(1) else today
    var count = 0
    while (day >= habit.createdOn) {
        if (habit.isScheduledOn(day)) {
            if (day !in done) break
            count++
        }
        day = day.minusDays(1)
    }
    return count
}

data class HabitRow(val habit: Habit, val doneToday: Boolean, val streak: Int)

/** The Habits card and screen for [today]: scheduled rows (counted) and "Not today" rows. */
data class HabitsToday(val scheduled: List<HabitRow>, val notToday: List<HabitRow>) {
    val done: Int get() = scheduled.count { it.doneToday }
    val total: Int get() = scheduled.size
}

fun habitsToday(
    habits: List<Habit>,
    completions: Map<Long, Set<LocalDate>>,
    today: LocalDate,
): HabitsToday {
    val rows = habits.sortedBy { it.sortOrder }.map { habit ->
        val done = completions[habit.id].orEmpty()
        HabitRow(habit, doneToday = today in done, streak = streak(habit, done, today))
    }
    val (scheduled, notToday) = rows.partition { it.habit.isScheduledOn(today) }
    return HabitsToday(scheduled, notToday)
}

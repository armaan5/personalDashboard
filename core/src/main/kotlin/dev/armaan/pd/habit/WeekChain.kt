package dev.armaan.pd.habit

import java.time.DayOfWeek
import java.time.LocalDate

/** How one day of the week chain reads (D23). */
enum class ChainDay { Done, Missed, Off, Open, Ahead }

/**
 * This week, Monday to Sunday, as the habit row's chain (D23). Scheduling follows SPEC §4.2, so a
 * completion on an unscheduled day reads [ChainDay.Off], like it counts nowhere else.
 */
fun weekChain(habit: Habit, done: Set<LocalDate>, today: LocalDate): List<ChainDay> {
    val monday = today.minusDays((today.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
    return (0L until 7L).map { offset ->
        val day = monday.plusDays(offset)
        when {
            !habit.isScheduledOn(day) -> ChainDay.Off
            day in done -> ChainDay.Done
            day < today -> ChainDay.Missed
            day == today -> ChainDay.Open
            else -> ChainDay.Ahead
        }
    }
}

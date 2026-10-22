package dev.armaan.pd.habit

import java.time.DayOfWeek
import java.time.LocalDate

/** SPEC §3.1 / §4.2. `id == 0` means not yet stored. */
data class Habit(
    val id: Long,
    val name: String,
    val weekdays: Set<DayOfWeek>,
    val createdOn: LocalDate,
    val sortOrder: Int,
)

const val HABIT_NAME_MAX = 60

/** SPEC §4.2: scheduled on d ⇔ d's weekday is chosen and d ≥ created_on. */
fun Habit.isScheduledOn(date: LocalDate): Boolean =
    date.dayOfWeek in weekdays && date >= createdOn

/** Trimmed name, or null when blank or longer than [HABIT_NAME_MAX] (SPEC §4.2). */
fun cleanHabitName(raw: String): String? =
    raw.trim().takeIf { it.isNotEmpty() && it.length <= HABIT_NAME_MAX }

/** SPEC §3.1 `weekdays` column: Mon = bit 0 … Sun = bit 6. */
object Weekdays {
    const val EVERY_DAY = 127

    fun toMask(days: Set<DayOfWeek>): Int = days.fold(0) { mask, day -> mask or bit(day) }

    fun fromMask(mask: Int): Set<DayOfWeek> =
        DayOfWeek.entries.filterTo(mutableSetOf()) { mask and bit(it) != 0 }

    private fun bit(day: DayOfWeek) = 1 shl (day.value - 1)
}

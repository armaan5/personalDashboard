package dev.armaan.pd.habit

import dev.armaan.pd.habit.ChainDay.Ahead
import dev.armaan.pd.habit.ChainDay.Done
import dev.armaan.pd.habit.ChainDay.Missed
import dev.armaan.pd.habit.ChainDay.Off
import dev.armaan.pd.habit.ChainDay.Open
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/** D23: each habit row shows this week, Mon–Sun, under a shared letter row. */
class WeekChainTest {
    // Thursday 2026-10-22; its week runs Mon 19 – Sun 25.
    private val today = LocalDate.of(2026, 10, 22)

    private fun oct(day: Int) = LocalDate.of(2026, 10, day)

    private fun base() = Habit(
        id = 1,
        name = "Stretch",
        weekdays = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY),
        createdOn = oct(1),
        sortOrder = 0,
    )

    @Test
    fun `week chain - runs Monday to Sunday of today's week`() {
        val chain = weekChain(base(), setOf(oct(19), oct(20), oct(21), oct(22)), today)
        assertEquals(listOf(Done, Done, Done, Done, Ahead, Off, Off), chain)
    }

    @Test
    fun `week chain - a past scheduled day not done is missed`() {
        val chain = weekChain(base(), setOf(oct(19), oct(21)), today)
        assertEquals(listOf(Done, Missed, Done, Open, Ahead, Off, Off), chain)
    }

    @Test
    fun `week chain - today scheduled and not done is open`() {
        assertEquals(Open, weekChain(base(), emptySet(), today)[3])
    }

    @Test
    fun `week chain - days before created_on are unscheduled`() {
        val chain = weekChain(base().copy(createdOn = oct(21)), setOf(oct(21)), today)
        assertEquals(listOf(Off, Off, Done, Open, Ahead, Off, Off), chain)
    }

    @Test
    fun `week chain - completions on unscheduled days are ignored`() {
        val run = base().copy(weekdays = setOf(TUESDAY, THURSDAY, SATURDAY))
        val chain = weekChain(run, setOf(oct(19), oct(20), oct(22)), today)
        assertEquals(listOf(Off, Done, Off, Done, Off, Ahead, Off), chain)
    }

    @Test
    fun `week chain - on Sunday every day is past or today`() {
        val chain = weekChain(base(), setOf(oct(19), oct(20), oct(21), oct(22), oct(23)), oct(25))
        assertEquals(listOf(Done, Done, Done, Done, Done, Off, Off), chain)
    }

    @Test
    fun `habits today - rows carry the week chain`() {
        val rows = habitsToday(listOf(base()), mapOf(1L to setOf(oct(22))), today)
        assertEquals(listOf(Missed, Missed, Missed, Done, Ahead, Off, Off), rows.scheduled.single().week)
    }
}

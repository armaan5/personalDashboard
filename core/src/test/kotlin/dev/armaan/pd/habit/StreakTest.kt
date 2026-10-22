package dev.armaan.pd.habit

import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class StreakTest {
    // Thursday 2026-10-22.
    private val today = LocalDate.of(2026, 10, 22)
    private val weekdays = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)

    private fun oct(day: Int) = LocalDate.of(2026, 10, day)

    private fun base() = Habit(
        id = 1,
        name = "Stretch",
        weekdays = weekdays,
        createdOn = oct(1),
        sortOrder = 0,
    )

    @Test
    fun `streak - today not done doesn't break it`() {
        assertEquals(3, streak(base(), setOf(oct(19), oct(20), oct(21)), today))
    }

    @Test
    fun `streak - today done counts`() {
        assertEquals(4, streak(base(), setOf(oct(19), oct(20), oct(21), oct(22)), today))
    }

    @Test
    fun `streak - unscheduled days are skipped`() {
        // Sat 17 and Sun 18 aren't scheduled, so Fri 16 joins the run.
        assertEquals(4, streak(base(), setOf(oct(16), oct(19), oct(20), oct(21)), today))
    }

    @Test
    fun `streak - a missed scheduled day stops the count`() {
        assertEquals(1, streak(base(), setOf(oct(19), oct(21)), today))
    }

    @Test
    fun `streak - the count stops at created_on`() {
        val habit = base().copy(createdOn = oct(20))
        // Mon 19 is before created_on: neither counted nor a miss.
        assertEquals(2, streak(habit, setOf(oct(19), oct(20), oct(21)), today))
        assertEquals(2, streak(habit, setOf(oct(20), oct(21)), today))
    }

    @Test
    fun `streak - completions on unscheduled days are ignored`() {
        val thursdays = base().copy(weekdays = setOf(THURSDAY))
        // Sat 17 isn't scheduled; Thu 8 was missed.
        assertEquals(2, streak(thursdays, setOf(oct(15), oct(17), oct(22)), today))
    }

    @Test
    fun `streak - a weekday change recomputes`() {
        val done = setOf(oct(16), oct(19), oct(20), oct(21))
        assertEquals(4, streak(base(), done, today))
        // Every day: Sun 18 is now scheduled and missed.
        assertEquals(3, streak(base().copy(weekdays = DayOfWeek.entries.toSet()), done, today))
    }

    @Test
    fun `card count - done over scheduled today, unscheduled go to Not today`() {
        val a = base().copy(id = 1, name = "Stretch", sortOrder = 0)
        val b = base().copy(id = 2, name = "Long walk", weekdays = setOf(SATURDAY, SUNDAY), sortOrder = 1)
        val c = base().copy(id = 3, name = "Read", weekdays = DayOfWeek.entries.toSet(), sortOrder = 2)

        val out = habitsToday(listOf(c, b, a), mapOf(1L to setOf(oct(21), oct(22))), today)

        assertEquals(1, out.done)
        assertEquals(2, out.total)
        assertEquals(listOf("Stretch", "Read"), out.scheduled.map { it.habit.name }) // sort_order
        assertEquals(listOf(true, false), out.scheduled.map { it.doneToday })
        assertEquals(listOf(2, 0), out.scheduled.map { it.streak })
        assertEquals(listOf("Long walk"), out.notToday.map { it.habit.name })
    }
}

package dev.armaan.pd.habit

import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HabitTest {
    private fun base() = Habit(
        id = 1,
        name = "Stretch",
        weekdays = setOf(MONDAY, WEDNESDAY, FRIDAY),
        createdOn = LocalDate.of(2026, 10, 19), // Monday
        sortOrder = 0,
    )

    @Test
    fun `schedule - chosen weekday on or after created_on is scheduled`() {
        assertTrue(base().isScheduledOn(LocalDate.of(2026, 10, 19))) // Mon, created that day
        assertTrue(base().isScheduledOn(LocalDate.of(2026, 10, 21))) // Wed
        assertFalse(base().isScheduledOn(LocalDate.of(2026, 10, 22))) // Thu, not chosen
        assertFalse(base().isScheduledOn(LocalDate.of(2026, 10, 16))) // Fri, before created_on
    }

    @Test
    fun `bitmask - Monday is bit 0 and Sunday is bit 6`() {
        assertEquals(1, Weekdays.toMask(setOf(MONDAY)))
        assertEquals(64, Weekdays.toMask(setOf(SUNDAY)))
        assertEquals(0b10101, Weekdays.toMask(setOf(MONDAY, WEDNESDAY, FRIDAY)))
    }

    @Test
    fun `bitmask - 127 is every day and round-trips`() {
        assertEquals(DayOfWeek.entries.toSet(), Weekdays.fromMask(Weekdays.EVERY_DAY))
        assertEquals(127, Weekdays.toMask(DayOfWeek.entries.toSet()))
        assertEquals(setOf(MONDAY, WEDNESDAY, FRIDAY), Weekdays.fromMask(0b10101))
    }

    @Test
    fun `validation - name is trimmed`() {
        assertEquals("Stretch", cleanHabitName("  Stretch \n"))
    }

    @Test
    fun `validation - blank name is refused`() {
        assertNull(cleanHabitName("   "))
    }

    @Test
    fun `validation - name over 60 characters is refused, 60 is allowed`() {
        assertEquals("a".repeat(60), cleanHabitName("a".repeat(60)))
        assertNull(cleanHabitName("a".repeat(61)))
    }
}

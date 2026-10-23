package dev.armaan.pd.widget

import dev.armaan.pd.app.Access
import dev.armaan.pd.calendar.Instance
import dev.armaan.pd.habit.Habit
import dev.armaan.pd.steps.DaySteps
import dev.armaan.pd.steps.StepsCard
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

/** SPEC §6.2. */
class SummaryTest {
    // Thursday 2026-10-22, 10:40 in London (BST, UTC+1).
    private val london = ZoneId.of("Europe/London")
    private val now = Instant.parse("2026-10-22T09:40:00Z")
    private val today = LocalDate.of(2026, 10, 22)

    /** [from] and [until] are London wall-clock times on 2026-10-xx, e.g. "22T10:30". */
    private fun timed(id: Long, from: String, until: String) = Instance(
        id,
        "Event $id",
        Instant.parse("2026-10-${from}:00+01:00"),
        Instant.parse("2026-10-${until}:00+01:00"),
        allDay = false,
    )

    private fun allDay(id: Long, from: String, until: String) =
        Instance(id, "All day $id", Instant.parse("${from}T00:00:00Z"), Instant.parse("${until}T00:00:00Z"), allDay = true)

    private val walked = (6L downTo 0L).map { DaySteps(today.minusDays(it), 5000) }

    private fun habit(id: Long, vararg days: DayOfWeek) = Habit(id, "Habit $id", days.toSet(), today.minusDays(30), id.toInt())

    private fun run(
        calendarGranted: Boolean = true,
        instances: List<Instance> = emptyList(),
        steps: StepsCard? = StepsCard.Steps(6240, walked),
        stepGoal: Int = 8000,
        habits: List<Habit> = emptyList(),
        completions: Map<Long, Set<LocalDate>> = emptyMap(),
    ) = summary(now, london, calendarGranted, instances, steps, stepGoal, habits, completions)

    private fun eventId(line: NextLine) = (line as NextLine.Event).event.eventId

    @Test
    fun `next event - an event in progress is picked over a later one`() {
        val out = run(instances = listOf(timed(2, "22T11:00", "22T12:00"), timed(1, "22T10:00", "22T11:00")))
        assertEquals(1L, eventId(out.next))
        assertEquals(false, (out.next as NextLine.Event).tomorrow)
    }

    @Test
    fun `next event - tomorrow's first event is marked Tomorrow when today has none left`() {
        val out = run(instances = listOf(timed(1, "22T08:00", "22T09:00"), timed(2, "23T09:00", "23T10:00")))
        assertEquals(NextLine.Event(timed(2, "23T09:00", "23T10:00"), tomorrow = true), out.next)
    }

    @Test
    fun `next event - nothing left today or tomorrow gives Nothing scheduled`() {
        val out = run(instances = listOf(timed(1, "22T08:00", "22T09:00"), timed(2, "24T09:00", "24T10:00")))
        assertEquals(NextLine.Nothing, out.next)
    }

    @Test
    fun `missing source - calendar not granted gives a dash with the set-up hint`() {
        val out = run(calendarGranted = false, instances = listOf(timed(1, "22T11:00", "22T12:00")))
        assertEquals(NextLine.SetUp, out.next)
    }

    @Test
    fun `missing source - steps blocked or needing Health Connect give a dash with the set-up hint`() {
        assertEquals(StepsLine.SetUp, run(steps = StepsCard.Blocked(Access.NotAsked)).steps)
        assertEquals(StepsLine.SetUp, run(steps = StepsCard.Blocked(Access.PermanentlyDenied)).steps)
        assertEquals(StepsLine.SetUp, run(steps = StepsCard.NeedsInstall).steps)
    }

    @Test
    fun `steps - today over the goal when the card has steps`() {
        assertEquals(StepsLine.Count(6240, 8000), run().steps)
    }

    @Test
    fun `next event - all-day events are skipped`() {
        val out = run(instances = listOf(allDay(1, "2026-10-22", "2026-10-23"), allDay(2, "2026-10-23", "2026-10-24")))
        assertEquals(NextLine.Nothing, out.next)
    }

    @Test
    fun `next event - an ended event today is skipped for a later one`() {
        val out = run(instances = listOf(timed(1, "22T09:00", "22T10:00"), timed(2, "22T15:00", "22T16:00")))
        assertEquals(NextLine.Event(timed(2, "22T15:00", "22T16:00"), tomorrow = false), out.next)
    }

    @Test
    fun `steps - granted but not read, unavailable or no data give a dash without the hint`() {
        assertEquals(StepsLine.Dash, run(steps = null).steps)
        assertEquals(StepsLine.Dash, run(steps = StepsCard.Unavailable).steps)
        assertEquals(StepsLine.Dash, run(steps = StepsCard.NoData).steps)
    }

    @Test
    fun `habits - done over scheduled today, unscheduled ones not counted`() {
        val habits = listOf(habit(1, DayOfWeek.THURSDAY), habit(2, DayOfWeek.THURSDAY), habit(3, DayOfWeek.MONDAY))
        val out = run(habits = habits, completions = mapOf(1L to setOf(today), 3L to setOf(today.minusDays(3))))
        assertEquals(HabitsLine.Count(1, 2), out.habits)
    }

    @Test
    fun `habits - none scheduled today gives No habits today`() {
        assertEquals(HabitsLine.NoneToday, run(habits = listOf(habit(1, DayOfWeek.MONDAY))).habits)
        assertEquals(HabitsLine.NoneToday, run().habits)
    }

    @Test
    fun `date - the header shows the local date`() {
        // 23:30 UTC on the 22nd is 00:30 on Friday the 23rd in London.
        val late = summary(Instant.parse("2026-10-22T23:30:00Z"), london, true, emptyList(), null, 8000, emptyList(), emptyMap())
        assertEquals(LocalDate.of(2026, 10, 23), late.today)
    }
}

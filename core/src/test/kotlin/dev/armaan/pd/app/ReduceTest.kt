package dev.armaan.pd.app

import dev.armaan.pd.calendar.DeviceCalendar
import dev.armaan.pd.habit.Habit
import dev.armaan.pd.todo.Todo
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReduceTest {
    // Thursday 2026-10-22, 11:00 in London.
    private val now = Instant.parse("2026-10-22T10:00:00Z")
    private val zone = ZoneId.of("Europe/London")
    private val today = LocalDate.of(2026, 10, 22)
    private val weekdays = setOf(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)

    private fun oct(day: Int) = LocalDate.of(2026, 10, day)

    private val stretch = Habit(1, "Stretch", weekdays, createdOn = oct(1), sortOrder = 0)
    private val walk = Habit(2, "Long walk", setOf(SATURDAY), createdOn = oct(1), sortOrder = 1)

    private fun todo(id: Long, due: LocalDate? = null, done: LocalDate? = null) =
        Todo(id, "Item $id", due, Instant.parse("2026-10-01T09:00:00Z").plusSeconds(id), done)

    private fun base() = State(
        habits = listOf(stretch, walk),
        completions = mapOf(1L to setOf(oct(20), oct(21))),
        todos = listOf(todo(1), todo(2, done = oct(22)), todo(3, done = oct(20)), todo(4, done = oct(19))),
    )

    private fun run(event: Event, state: State = base()) = reduce(state, event, now, zone)

    // Navigation (PRD A4)

    @Test
    fun `back - pops a pushed screen`() {
        val opened = run(Event.Open(Screen.Habits)).state
        assertEquals(Screen.Habits, opened.screen)

        val out = run(Event.Back, opened)
        assertEquals(listOf(Screen.Dashboard), out.state.backStack)
        assertEquals(emptyList(), out.effects)
    }

    @Test
    fun `back - at the root emits Finish`() {
        val out = run(Event.Back)
        assertEquals(listOf(Screen.Dashboard), out.state.backStack)
        assertEquals(listOf<Effect>(Effect.Finish), out.effects)
    }

    @Test
    fun `open - Dashboard from a pushed screen returns to the root`() {
        val opened = run(Event.Open(Screen.Habits)).state
        assertEquals(listOf(Screen.Dashboard), run(Event.Open(Screen.Dashboard), opened).state.backStack)
    }

    // Habits (SPEC §4.2, PRD A7)

    @Test
    fun `tick - scheduled today emits a done completion for today`() {
        val out = run(Event.ToggleHabit(1, today), base().copy(completions = emptyMap()))
        assertEquals(listOf<Effect>(Effect.ToggleCompletion(1, today, done = true)), out.effects)
    }

    @Test
    fun `tick - again today undoes it`() {
        val ticked = base().copy(completions = mapOf(1L to setOf(today)))
        assertEquals(listOf<Effect>(Effect.ToggleCompletion(1, today, done = false)), run(Event.ToggleHabit(1, today), ticked).effects)
    }

    @Test
    fun `tick - refused on an unscheduled day`() {
        assertEquals(emptyList(), run(Event.ToggleHabit(2, today)).effects) // walk is Saturdays only
    }

    @Test
    fun `tick - refused on a past date`() {
        assertEquals(emptyList(), run(Event.ToggleHabit(1, oct(21))).effects)
    }

    @Test
    fun `tick - refused on a future date`() {
        assertEquals(emptyList(), run(Event.ToggleHabit(1, oct(23))).effects)
    }

    @Test
    fun `save habit - a new habit is created today and goes last`() {
        val out = run(Event.SaveHabit(0, "  Read ", DayOfWeek.entries.toSet()))
        assertEquals(
            listOf<Effect>(Effect.WriteHabit(Habit(0, "Read", DayOfWeek.entries.toSet(), createdOn = today, sortOrder = 2))),
            out.effects,
        )
    }

    @Test
    fun `save habit - an edit keeps created_on and sort_order`() {
        val out = run(Event.SaveHabit(1, "Stretch more", setOf(MONDAY)))
        assertEquals(listOf<Effect>(Effect.WriteHabit(stretch.copy(name = "Stretch more", weekdays = setOf(MONDAY)))), out.effects)
    }

    @Test
    fun `save habit - blank name or no weekdays is refused`() {
        assertEquals(emptyList(), run(Event.SaveHabit(0, "   ", weekdays)).effects)
        assertEquals(emptyList(), run(Event.SaveHabit(0, "Read", emptySet())).effects)
    }

    @Test
    fun `delete habit - emits DeleteHabit then a snackbar with undo`() {
        val out = run(Event.DeleteHabit(1))
        assertEquals(
            listOf(Effect.DeleteHabit(1), Effect.ShowSnackbar(Message.HabitDeleted("Stretch"), undo = true)),
            out.effects,
        )
    }

    @Test
    fun `delete habit - undo restores it with its completions`() {
        val deleted = run(Event.DeleteHabit(1)).state
        val out = run(Event.Undo, deleted)
        assertEquals(listOf<Effect>(Effect.RestoreHabit(stretch, setOf(oct(20), oct(21)))), out.effects)
        assertNull(out.state.pendingUndo)
    }

    @Test
    fun `undo - after the snackbar is dismissed does nothing`() {
        val dismissed = run(Event.SnackbarDismissed, run(Event.DeleteHabit(1)).state).state
        assertEquals(emptyList(), run(Event.Undo, dismissed).effects)
    }

    // To-dos (SPEC §4.3, PRD A8)

    @Test
    fun `save todo - a new to-do is stamped with now`() {
        val out = run(Event.SaveTodo(0, " Buy milk ", oct(23)))
        assertEquals(listOf<Effect>(Effect.WriteTodo(Todo(0, "Buy milk", oct(23), createdAt = now, completedOn = null))), out.effects)
    }

    @Test
    fun `save todo - blank title is refused`() {
        assertEquals(emptyList(), run(Event.SaveTodo(0, "  ", null)).effects)
    }

    @Test
    fun `tick todo - completes it today with an undo snackbar`() {
        val out = run(Event.ToggleTodo(1))
        assertEquals(
            listOf(Effect.WriteTodo(todo(1, done = today)), Effect.ShowSnackbar(Message.TodoDone("Item 1"), undo = true)),
            out.effects,
        )
    }

    @Test
    fun `tick todo - undo reopens it`() {
        val out = run(Event.Undo, run(Event.ToggleTodo(1)).state)
        assertEquals(listOf<Effect>(Effect.WriteTodo(todo(1))), out.effects)
    }

    @Test
    fun `tick todo - undo keeps an edit made while the snackbar was up`() {
        val ticked = run(Event.ToggleTodo(1)).state
        val edited = todo(1, done = today).copy(title = "Item 1, renamed")
        val reloaded = run(Event.TodosLoaded(listOf(edited)), ticked).state
        assertEquals(listOf<Effect>(Effect.WriteTodo(edited.copy(completedOn = null))), run(Event.Undo, reloaded).effects)
    }

    @Test
    fun `untick todo - allowed while done today`() {
        assertEquals(listOf<Effect>(Effect.WriteTodo(todo(2))), run(Event.ToggleTodo(2)).effects)
    }

    @Test
    fun `untick todo - refused once in history`() {
        assertEquals(emptyList(), run(Event.ToggleTodo(3)).effects)
    }

    @Test
    fun `clear history - emits DeleteTodos then a snackbar with undo`() {
        val out = run(Event.ClearHistory)
        assertEquals(
            listOf(Effect.DeleteTodos(listOf(3, 4)), Effect.ShowSnackbar(Message.HistoryCleared(2), undo = true)),
            out.effects,
        )
    }

    @Test
    fun `clear history - undo restores the cleared items`() {
        val out = run(Event.Undo, run(Event.ClearHistory).state)
        assertEquals(listOf<Effect>(Effect.WriteTodo(todo(3, done = oct(20))), Effect.WriteTodo(todo(4, done = oct(19)))), out.effects)
    }

    @Test
    fun `clear history - with nothing in history does nothing`() {
        assertEquals(emptyList(), run(Event.ClearHistory, base().copy(todos = listOf(todo(1)))).effects)
    }

    // Snapshots

    @Test
    fun `loaded - snapshots replace state without effects`() {
        val out = run(Event.TodosLoaded(listOf(todo(9))), State())
        assertEquals(listOf(todo(9)), out.state.todos)
        assertEquals(emptyList(), out.effects)
    }

    // Calendar (SPEC §5.1, §5.2)

    private val refused = PermissionCheck(granted = false, rationale = false)

    private fun calendarState(check: PermissionCheck, asked: Boolean) =
        State(settings = Settings(askedCalendar = asked), calendarCheck = check)

    @Test
    fun `calendar shown - in NotAsked requests the permission`() {
        val out = run(Event.CalendarShown, calendarState(refused, asked = false))
        assertEquals(listOf<Effect>(Effect.RequestPermission(PermissionKind.Calendar)), out.effects)
    }

    @Test
    fun `calendar shown - requests nothing once asked, granted or still unknown`() {
        assertEquals(emptyList(), run(Event.CalendarShown, calendarState(PermissionCheck(false, rationale = true), asked = true)).effects)
        assertEquals(emptyList(), run(Event.CalendarShown, calendarState(refused, asked = true)).effects)
        assertEquals(emptyList(), run(Event.CalendarShown, calendarState(PermissionCheck(true, false), asked = false)).effects)
        assertEquals(emptyList(), run(Event.CalendarShown, State(calendarCheck = refused)).effects)
    }

    @Test
    fun `calendar request in flight - reads Denied, not PermanentlyDenied, until the next check`() {
        // Plumbing sets asked_calendar as the dialog opens, before the user answers.
        val asking = run(Event.CalendarShown, calendarState(refused, asked = false)).state
        val flagged = run(Event.SettingsLoaded(Settings(askedCalendar = true)), asking).state
        assertEquals(Access.Denied, flagged.calendarAccess)
        assertEquals(Access.PermanentlyDenied, run(Event.CalendarChecked(refused), flagged).state.calendarAccess)
    }

    @Test
    fun `grant calendar - when Denied requests again`() {
        val out = run(Event.GrantCalendar, calendarState(PermissionCheck(false, rationale = true), asked = true))
        assertEquals(listOf<Effect>(Effect.RequestPermission(PermissionKind.Calendar)), out.effects)
    }

    @Test
    fun `grant calendar - when PermanentlyDenied opens the app settings page`() {
        val out = run(Event.GrantCalendar, calendarState(refused, asked = true))
        assertEquals(listOf<Effect>(Effect.OpenAppSettings), out.effects)
    }

    private val personal = DeviceCalendar("1", "Personal", "me@example.com", 0, visible = true)
    private val work = DeviceCalendar("2", "Work", "me@example.com", 0, visible = true)
    private val holidays = DeviceCalendar("3", "Holidays", "me@example.com", 0, visible = false)

    private fun calendarsState(stored: Set<String>?) =
        State(settings = Settings(calendarIds = stored), calendars = listOf(personal, work, holidays))

    @Test
    fun `toggle calendar - the first toggle writes the visible default explicitly`() {
        assertEquals(
            listOf<Effect>(Effect.WriteCalendarIds(setOf("1", "2", "3"))),
            run(Event.ToggleCalendar("3", on = true), calendarsState(stored = null)).effects,
        )
        assertEquals(
            listOf<Effect>(Effect.WriteCalendarIds(setOf("2"))),
            run(Event.ToggleCalendar("1", on = false), calendarsState(stored = null)).effects,
        )
    }

    @Test
    fun `toggle calendar - later toggles edit the stored set`() {
        assertEquals(
            listOf<Effect>(Effect.WriteCalendarIds(setOf("3"))),
            run(Event.ToggleCalendar("3", on = true), calendarsState(stored = emptySet())).effects,
        )
    }

    @Test
    fun `open - Calendars is pushed over Settings and Back returns to Settings`() {
        val settings = run(Event.Open(Screen.Settings), State()).state
        val calendars = run(Event.Open(Screen.Calendars), settings).state
        assertEquals(listOf(Screen.Dashboard, Screen.Settings, Screen.Calendars), calendars.backStack)
        assertEquals(listOf(Screen.Dashboard, Screen.Settings), run(Event.Back, calendars).state.backStack)
    }
}

package dev.armaan.pd.app

import dev.armaan.pd.calendar.DeviceCalendar
import dev.armaan.pd.calendar.Instance
import dev.armaan.pd.habit.Habit
import dev.armaan.pd.todo.Todo
import java.time.DayOfWeek
import java.time.LocalDate

sealed interface Event {
    data class Open(val screen: Screen) : Event

    data object Back : Event

    data class HabitsLoaded(val habits: List<Habit>, val completions: Map<Long, Set<LocalDate>>) : Event

    data class TodosLoaded(val todos: List<Todo>) : Event

    /** `id == 0` adds a habit; otherwise edits that one. */
    data class SaveHabit(val id: Long, val name: String, val weekdays: Set<DayOfWeek>) : Event

    data class ToggleHabit(val habitId: Long, val date: LocalDate) : Event

    data class DeleteHabit(val habitId: Long) : Event

    /** `id == 0` adds a to-do; otherwise edits that one. */
    data class SaveTodo(val id: Long, val title: String, val dueDate: LocalDate?) : Event

    data class ToggleTodo(val todoId: Long) : Event

    data object ClearHistory : Event

    data object Undo : Event

    data object SnackbarDismissed : Event

    data class SettingsLoaded(val settings: Settings) : Event

    data class CalendarChecked(val check: PermissionCheck) : Event

    data class CalendarLoaded(val instances: List<Instance>) : Event

    data class CalendarsListed(val calendars: List<DeviceCalendar>) : Event

    /** The Calendar card was composed on screen (SPEC §5.1: the first view requests). */
    data object CalendarShown : Event

    data object GrantCalendar : Event

    data class ToggleCalendar(val calendarId: String, val on: Boolean) : Event

    data class HealthChecked(val check: HealthCheck) : Event

    /** The Health Connect request came back; [granted] is whether `READ_STEPS` was among the grants. */
    data class HealthAnswered(val granted: Boolean) : Event

    /**
     * [day]'s total so far, and daily totals by local date for the 7 days ending on [day] (days with
     * records only). [day] is the date the read was made for, so a read straddling midnight stays
     * consistent.
     */
    data class StepsLoaded(val day: LocalDate, val total: Long, val days: Map<LocalDate, Long>) : Event

    /** The Steps card was composed on screen (SPEC §5.1: the first view requests). */
    data object StepsShown : Event

    /** The Steps card's action: install, request, or open Health Connect's settings. */
    data object GrantSteps : Event

    /** Settings → Step goal stepper; the reducer clamps it. */
    data class SetStepGoal(val goal: Int) : Event

    /** Settings → Theme chips (D27 c). */
    data class SetTheme(val choice: ThemeChoice) : Event
}

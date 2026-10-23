package dev.armaan.pd.app

import dev.armaan.pd.calendar.DeviceCalendar
import dev.armaan.pd.calendar.Instance
import dev.armaan.pd.habit.Habit
import dev.armaan.pd.todo.Todo
import java.time.LocalDate

/** SPEC §6.1 screens. Dashboard is the root of the back stack (PRD A4). Calendars sits under Settings. */
enum class Screen { Dashboard, Habits, Todos, Settings, Calendars }

/**
 * SPEC §4.4. [habits], [completions], [todos], [settings] and the calendar fields are the last
 * snapshot plumbing delivered; the reducer never edits them, it asks for writes through effects.
 */
data class State(
    val backStack: List<Screen> = listOf(Screen.Dashboard),
    val habits: List<Habit> = emptyList(),
    val completions: Map<Long, Set<LocalDate>> = emptyMap(),
    val todos: List<Todo> = emptyList(),
    val pendingUndo: Undo? = null,
    /** Null until DataStore has delivered. */
    val settings: Settings? = null,
    /** Null until the first check (on foreground, SPEC §5.1). */
    val calendarCheck: PermissionCheck? = null,
    /** A request is showing; cleared by the next check. */
    val calendarRequesting: Boolean = false,
    val calendarInstances: List<Instance> = emptyList(),
    val calendars: List<DeviceCalendar> = emptyList(),
) {
    val screen: Screen get() = backStack.last()

    /**
     * While the dialog is up, `asked_calendar` is already set but the user hasn't answered, so it
     * reads Denied rather than PermanentlyDenied.
     */
    val calendarAccess: Access?
        get() = access(calendarCheck, settings?.askedCalendar)
            .let { if (calendarRequesting && it == Access.PermanentlyDenied) Access.Denied else it }
}

/** What the snackbar's Undo puts back. Only the latest action can be undone. */
sealed interface Undo {
    data class HabitDeleted(val habit: Habit, val completions: Set<LocalDate>) : Undo

    data class TodoTicked(val todoId: Long) : Undo

    data class HistoryCleared(val todos: List<Todo>) : Undo
}

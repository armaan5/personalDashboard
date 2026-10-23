package dev.armaan.pd.app

import dev.armaan.pd.calendar.DeviceCalendar
import dev.armaan.pd.calendar.Instance
import dev.armaan.pd.habit.Habit
import dev.armaan.pd.steps.DaySteps
import dev.armaan.pd.steps.HealthSdk
import dev.armaan.pd.steps.StepsCard
import dev.armaan.pd.steps.stepsCard
import dev.armaan.pd.todo.Todo
import java.time.LocalDate

/**
 * SPEC §6.1 screens. Dashboard is the root of the back stack (PRD A4). Calendars and About sit
 * under Settings.
 */
enum class Screen { Dashboard, Habits, Todos, Settings, Calendars, About }

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
    /** Null until the first Health Connect check (on foreground, SPEC §5.3). */
    val healthCheck: HealthCheck? = null,
    /** A Health Connect request is showing; cleared by the next check. */
    val healthRequesting: Boolean = false,
    /** The request showing was a repeat: `asked_health` was already set when it fired. */
    val healthRepeatAsk: Boolean = false,
    /**
     * A repeat request came back refused, so Health Connect won't show its dialog again (D25).
     * For this process only; cleared once access is granted.
     */
    val healthLockedOut: Boolean = false,
    /** Null until read while granted. */
    val stepsToday: Long? = null,
    val stepsWeek: List<DaySteps>? = null,
) {
    val screen: Screen get() = backStack.last()

    /**
     * While the dialog is up, `asked_calendar` is already set but the user hasn't answered, so it
     * reads Denied rather than PermanentlyDenied.
     */
    val calendarAccess: Access?
        get() = access(calendarCheck, settings?.askedCalendar)
            .let { if (calendarRequesting && it == Access.PermanentlyDenied) Access.Denied else it }

    /**
     * SPEC §5.1 for Health Connect, which has no rationale check of its own: it asks again until a
     * repeat request has come back refused (D25).
     */
    val healthAccess: Access?
        get() = access(healthCheck?.let { PermissionCheck(it.granted, rationale = !healthLockedOut) }, settings?.askedHealth)
            .let { if (healthRequesting && it == Access.PermanentlyDenied) Access.Denied else it }

    /** SPEC §5.3. */
    val steps: StepsCard? get() = stepsCard(healthCheck?.sdk, healthAccess, stepsToday, stepsWeek)
}

/** What plumbing reads from Health Connect: its SDK status, and whether `READ_STEPS` is granted. */
data class HealthCheck(val sdk: HealthSdk, val granted: Boolean)

/** What the snackbar's Undo puts back. Only the latest action can be undone. */
sealed interface Undo {
    data class HabitDeleted(val habit: Habit, val completions: Set<LocalDate>) : Undo

    data class TodoTicked(val todoId: Long) : Undo

    data class HistoryCleared(val todos: List<Todo>) : Undo
}

package dev.armaan.pd.app

import dev.armaan.pd.calendar.shownCalendars
import dev.armaan.pd.habit.Habit
import dev.armaan.pd.habit.cleanHabitName
import dev.armaan.pd.habit.isScheduledOn
import dev.armaan.pd.steps.StepsCard
import dev.armaan.pd.steps.clampGoal
import dev.armaan.pd.steps.lastSevenDays
import dev.armaan.pd.time.today
import dev.armaan.pd.todo.Todo
import dev.armaan.pd.todo.buckets
import dev.armaan.pd.todo.cleanTodoTitle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class Next(val state: State, val effects: List<Effect> = emptyList())

/** SPEC §4.4. Pure: no I/O, time and zone are arguments. */
fun reduce(state: State, event: Event, now: Instant, zone: ZoneId): Next = step(state, event, now, zone).refreshingWidget()

/** SPEC §6.2, D26: a write the widget shows is followed by one refresh, after the writes. */
private fun Next.refreshingWidget(): Next =
    if (effects.any { it.changesWidget }) copy(effects = effects + Effect.RefreshWidget) else this

private val Effect.changesWidget: Boolean
    get() = when (this) {
        is Effect.WriteHabit, is Effect.DeleteHabit, is Effect.RestoreHabit, is Effect.ToggleCompletion,
        is Effect.WriteTodo, is Effect.DeleteTodos, is Effect.WriteStepGoal, is Effect.WriteCalendarIds,
        -> true
        else -> false
    }

private fun step(state: State, event: Event, now: Instant, zone: ZoneId): Next {
    val today = today(now, zone)
    return when (event) {
        is Event.Open -> when {
            event.screen == Screen.Dashboard -> Next(state.copy(backStack = listOf(Screen.Dashboard)))
            state.screen == event.screen -> Next(state)
            else -> Next(state.copy(backStack = state.backStack + event.screen))
        }

        Event.Back ->
            if (state.backStack.size > 1) Next(state.copy(backStack = state.backStack.dropLast(1)))
            else Next(state, listOf(Effect.Finish))

        is Event.HabitsLoaded -> Next(state.copy(habits = event.habits, completions = event.completions))
        is Event.TodosLoaded -> Next(state.copy(todos = event.todos))

        is Event.SaveHabit -> saveHabit(state, event, today)
        is Event.ToggleHabit -> toggleHabit(state, event, today)
        is Event.DeleteHabit -> deleteHabit(state, event)

        is Event.SaveTodo -> saveTodo(state, event, now)
        is Event.ToggleTodo -> toggleTodo(state, event, today)
        Event.ClearHistory -> clearHistory(state, today)

        Event.Undo -> undo(state)
        Event.SnackbarDismissed -> Next(state.copy(pendingUndo = null))

        is Event.SettingsLoaded -> Next(state.copy(settings = event.settings))
        is Event.CalendarChecked -> Next(state.copy(calendarCheck = event.check, calendarRequesting = false))
        is Event.CalendarLoaded -> Next(state.copy(calendarInstances = event.instances))
        is Event.CalendarsListed -> Next(state.copy(calendars = event.calendars))
        Event.CalendarShown ->
            if (state.calendarAccess == Access.NotAsked) requestCalendar(state) else Next(state)
        Event.GrantCalendar -> grantCalendar(state)
        is Event.ToggleCalendar -> toggleCalendar(state, event)

        is Event.HealthChecked -> Next(
            state.copy(
                healthCheck = event.check,
                healthRequesting = false,
                healthLockedOut = state.healthLockedOut && !event.check.granted,
            ),
        )
        is Event.HealthAnswered -> Next(
            state.copy(healthLockedOut = state.healthLockedOut || (!event.granted && state.healthRepeatAsk)),
        )
        is Event.StepsLoaded -> Next(state.copy(stepsToday = event.total, stepsWeek = lastSevenDays(event.days, event.day)))
        Event.StepsShown ->
            if (!state.healthRequesting && state.steps == StepsCard.Blocked(Access.NotAsked)) requestHealth(state)
            else Next(state)
        Event.GrantSteps -> grantSteps(state)
        is Event.SetStepGoal -> Next(state, listOf(Effect.WriteStepGoal(clampGoal(event.goal))))
    }
}

private fun saveHabit(state: State, event: Event.SaveHabit, today: LocalDate): Next {
    val name = cleanHabitName(event.name) ?: return Next(state)
    if (event.weekdays.isEmpty()) return Next(state)
    val habit = if (event.id == 0L) {
        val last = state.habits.maxOfOrNull { it.sortOrder } ?: -1
        Habit(0, name, event.weekdays, createdOn = today, sortOrder = last + 1)
    } else {
        state.habit(event.id)?.copy(name = name, weekdays = event.weekdays) ?: return Next(state)
    }
    return Next(state, listOf(Effect.WriteHabit(habit)))
}

/** PRD A7: only today, and only when scheduled today. */
private fun toggleHabit(state: State, event: Event.ToggleHabit, today: LocalDate): Next {
    val habit = state.habit(event.habitId) ?: return Next(state)
    if (event.date != today || !habit.isScheduledOn(today)) return Next(state)
    val done = today !in state.completions[habit.id].orEmpty()
    return Next(state, listOf(Effect.ToggleCompletion(habit.id, today, done)))
}

private fun deleteHabit(state: State, event: Event.DeleteHabit): Next {
    val habit = state.habit(event.habitId) ?: return Next(state)
    val completions = state.completions[habit.id].orEmpty()
    return Next(
        state.copy(pendingUndo = Undo.HabitDeleted(habit, completions)),
        listOf(Effect.DeleteHabit(habit.id), Effect.ShowSnackbar(Message.HabitDeleted(habit.name), undo = true)),
    )
}

private fun saveTodo(state: State, event: Event.SaveTodo, now: Instant): Next {
    val title = cleanTodoTitle(event.title) ?: return Next(state)
    val todo = if (event.id == 0L) {
        Todo(0, title, event.dueDate, createdAt = now, completedOn = null)
    } else {
        state.todo(event.id)?.copy(title = title, dueDate = event.dueDate) ?: return Next(state)
    }
    return Next(state, listOf(Effect.WriteTodo(todo)))
}

/** PRD A8: open → done today with Undo; done today → open; history is read-only. */
private fun toggleTodo(state: State, event: Event.ToggleTodo, today: LocalDate): Next {
    val todo = state.todo(event.todoId) ?: return Next(state)
    val completedOn = todo.completedOn
    return when {
        completedOn == null -> Next(
            state.copy(pendingUndo = Undo.TodoTicked(todo.id)),
            listOf(
                Effect.WriteTodo(todo.copy(completedOn = today)),
                Effect.ShowSnackbar(Message.TodoDone(todo.title), undo = true),
            ),
        )
        completedOn >= today -> Next(state, listOf(Effect.WriteTodo(todo.copy(completedOn = null))))
        else -> Next(state)
    }
}

private fun clearHistory(state: State, today: LocalDate): Next {
    val history = buckets(state.todos, today).history
    if (history.isEmpty()) return Next(state)
    return Next(
        state.copy(pendingUndo = Undo.HistoryCleared(history)),
        listOf(
            Effect.DeleteTodos(history.map { it.id }),
            Effect.ShowSnackbar(Message.HistoryCleared(history.size), undo = true),
        ),
    )
}

private fun undo(state: State): Next {
    val effects = when (val pending = state.pendingUndo) {
        null -> return Next(state)
        is Undo.HabitDeleted -> listOf(Effect.RestoreHabit(pending.habit, pending.completions))
        // Reopen the current row, so an edit made while the snackbar was up survives.
        is Undo.TodoTicked -> listOfNotNull(state.todo(pending.todoId)?.let { Effect.WriteTodo(it.copy(completedOn = null)) })
        is Undo.HistoryCleared -> pending.todos.map { Effect.WriteTodo(it) }
    }
    return Next(state.copy(pendingUndo = null), effects)
}

/** SPEC §5.1: Grant asks again while the system will still ask, then opens the app's settings. */
private fun grantCalendar(state: State): Next = when (state.calendarAccess) {
    Access.NotAsked, Access.Denied -> requestCalendar(state)
    Access.PermanentlyDenied -> Next(state, listOf(Effect.OpenAppSettings))
    Access.Granted, null -> Next(state)
}

private fun requestCalendar(state: State) =
    Next(state.copy(calendarRequesting = true), listOf(Effect.RequestPermission(PermissionKind.Calendar)))

/** SPEC §5.3: install when missing, ask while Health Connect will still ask, then its settings. */
private fun grantSteps(state: State): Next = when (val card = state.steps) {
    StepsCard.NeedsInstall -> Next(state, listOf(Effect.OpenHealthConnectInstall))
    is StepsCard.Blocked -> when {
        card.access == Access.PermanentlyDenied -> Next(state, listOf(Effect.OpenHealthConnectSettings))
        // One request at a time, so the repeat flag belongs to the request it answers.
        state.healthRequesting -> Next(state)
        else -> requestHealth(state)
    }
    else -> Next(state)
}

private fun requestHealth(state: State) = Next(
    state.copy(healthRequesting = true, healthRepeatAsk = state.settings?.askedHealth == true),
    listOf(Effect.RequestPermission(PermissionKind.Health)),
)

/** SPEC §5.2: the first toggle writes `calendar_ids` explicitly, starting from the default. */
private fun toggleCalendar(state: State, event: Event.ToggleCalendar): Next {
    val settings = state.settings ?: return Next(state)
    val shown = shownCalendars(state.calendars, settings.calendarIds)
    val ids = if (event.on) shown + event.calendarId else shown - event.calendarId
    return Next(state, listOf(Effect.WriteCalendarIds(ids)))
}

private fun State.habit(id: Long): Habit? = habits.find { it.id == id }

private fun State.todo(id: Long): Todo? = todos.find { it.id == id }

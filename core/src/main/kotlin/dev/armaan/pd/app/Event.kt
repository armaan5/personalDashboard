package dev.armaan.pd.app

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
}

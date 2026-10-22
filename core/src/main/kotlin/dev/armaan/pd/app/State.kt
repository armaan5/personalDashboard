package dev.armaan.pd.app

import dev.armaan.pd.habit.Habit
import dev.armaan.pd.todo.Todo
import java.time.LocalDate

/** SPEC §6.1 screens. Dashboard is the root of the back stack (PRD A4). */
enum class Screen { Dashboard, Habits, Todos, Settings }

/**
 * SPEC §4.4. [habits], [completions] and [todos] are the last snapshot the repositories
 * delivered; the reducer never edits them, it asks for writes through effects.
 */
data class State(
    val backStack: List<Screen> = listOf(Screen.Dashboard),
    val habits: List<Habit> = emptyList(),
    val completions: Map<Long, Set<LocalDate>> = emptyMap(),
    val todos: List<Todo> = emptyList(),
    val pendingUndo: Undo? = null,
) {
    val screen: Screen get() = backStack.last()
}

/** What the snackbar's Undo puts back. Only the latest action can be undone. */
sealed interface Undo {
    data class HabitDeleted(val habit: Habit, val completions: Set<LocalDate>) : Undo

    data class TodoTicked(val todoId: Long) : Undo

    data class HistoryCleared(val todos: List<Todo>) : Undo
}

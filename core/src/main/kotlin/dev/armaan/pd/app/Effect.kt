package dev.armaan.pd.app

import dev.armaan.pd.habit.Habit
import dev.armaan.pd.todo.Todo
import java.time.LocalDate

/** SPEC §4.4. Data only: plumbing performs these, in list order. */
sealed interface Effect {
    /** Insert when `habit.id == 0`, otherwise update. */
    data class WriteHabit(val habit: Habit) : Effect

    /** Deletes the habit and, by cascade, its completions. */
    data class DeleteHabit(val habitId: Long) : Effect

    /** Undo of a delete: re-inserts the habit with its old id and its completions. */
    data class RestoreHabit(val habit: Habit, val completions: Set<LocalDate>) : Effect

    /** Sets whether the habit is done on [date]: adds or removes that completion. */
    data class ToggleCompletion(val habitId: Long, val date: LocalDate, val done: Boolean) : Effect

    /** Insert when `todo.id == 0`, otherwise upsert by id (Undo re-inserts cleared ones). */
    data class WriteTodo(val todo: Todo) : Effect

    data class DeleteTodos(val ids: List<Long>) : Effect

    /** Plumbing sets the matching `asked_*` flag, then shows the system request (SPEC §5.1). */
    data class RequestPermission(val kind: PermissionKind) : Effect

    /** The app's system settings page, `ACTION_APPLICATION_DETAILS_SETTINGS`. */
    data object OpenAppSettings : Effect

    /** Writes `calendar_ids` explicitly (SPEC §3.2). */
    data class WriteCalendarIds(val ids: Set<String>) : Effect

    /** Health Connect's Play listing, or the browser when Play is absent (SPEC §5.3). */
    data object OpenHealthConnectInstall : Effect

    /** Health Connect's permission settings for this app (SPEC §5.3, PermanentlyDenied). */
    data object OpenHealthConnectSettings : Effect

    data class WriteStepGoal(val goal: Int) : Effect

    /** Redraws the widget from the sources (SPEC §6.2). Follows every write that changes what it shows. */
    data object RefreshWidget : Effect

    data class ShowSnackbar(val message: Message, val undo: Boolean) : Effect

    data object Finish : Effect
}

enum class PermissionKind { Calendar, Health }

/** Snackbar copy keys. The text lives in `strings.xml`. */
sealed interface Message {
    data class HabitDeleted(val name: String) : Message

    data class TodoDone(val title: String) : Message

    data class HistoryCleared(val count: Int) : Message
}

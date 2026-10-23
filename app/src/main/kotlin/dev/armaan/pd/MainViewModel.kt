package dev.armaan.pd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.armaan.pd.app.Effect
import dev.armaan.pd.app.Event
import dev.armaan.pd.app.Message
import dev.armaan.pd.app.State
import dev.armaan.pd.app.reduce
import dev.armaan.pd.time.today
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/** What only the UI can perform: show a snackbar, finish the activity. */
sealed interface UiEffect {
    data class Snackbar(val message: Message, val undo: Boolean) : UiEffect

    data object Finish : UiEffect
}

/**
 * Plumbing only (D10): feeds events to [reduce], publishes the state, and performs the effects in
 * list order. No rule lives here.
 */
class MainViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _today = MutableStateFlow(currentDay())

    /** Re-read on every event and on foreground, so midnight rolls over (SPEC §4.1). */
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    private val ui = Channel<UiEffect>(Channel.BUFFERED)
    val uiEffects: Flow<UiEffect> = ui.receiveAsFlow()

    init {
        viewModelScope.launch {
            merge(container.habits.loaded, container.todos.loaded).collect(::dispatch)
        }
    }

    fun dispatch(event: Event) {
        val next = reduce(_state.value, event, container.clock.instant(), container.zone())
        _state.value = next.state
        refreshDay()
        if (next.effects.isNotEmpty()) viewModelScope.launch { next.effects.forEach { perform(it) } }
    }

    fun refreshDay() {
        _today.value = currentDay()
    }

    private fun currentDay() = today(container.clock.instant(), container.zone())

    private suspend fun perform(effect: Effect) {
        when (effect) {
            is Effect.WriteHabit -> container.habits.write(effect.habit)
            is Effect.DeleteHabit -> container.habits.delete(effect.habitId)
            is Effect.RestoreHabit -> container.habits.restore(effect.habit, effect.completions)
            is Effect.ToggleCompletion -> container.habits.setCompletion(effect.habitId, effect.date, effect.done)
            is Effect.WriteTodo -> container.todos.write(effect.todo)
            is Effect.DeleteTodos -> container.todos.delete(effect.ids)
            is Effect.ShowSnackbar -> ui.send(UiEffect.Snackbar(effect.message, effect.undo))
            Effect.Finish -> ui.send(UiEffect.Finish)
        }
    }
}

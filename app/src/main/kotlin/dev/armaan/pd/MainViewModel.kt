package dev.armaan.pd

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.armaan.pd.app.Effect
import dev.armaan.pd.app.Event
import dev.armaan.pd.app.Message
import dev.armaan.pd.app.PermissionCheck
import dev.armaan.pd.app.PermissionKind
import dev.armaan.pd.app.State
import dev.armaan.pd.app.reduce
import dev.armaan.pd.calendar.queryWindow
import dev.armaan.pd.time.today
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** What only the UI can perform: show a snackbar, finish the activity, start a system screen. */
sealed interface UiEffect {
    data class Snackbar(val message: Message, val undo: Boolean) : UiEffect

    data class RequestPermission(val kind: PermissionKind) : UiEffect

    data object OpenAppSettings : UiEffect

    data object OpenHealthConnectInstall : UiEffect

    data object OpenHealthConnectSettings : UiEffect

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

    private val _now = MutableStateFlow(container.clock.instant())

    /** Ticks each minute while resumed, so ended events drop off and "Now" moves (SPEC §5.2). */
    val now: StateFlow<Instant> = _now.asStateFlow()

    private val ui = Channel<UiEffect>(Channel.BUFFERED)
    val uiEffects: Flow<UiEffect> = ui.receiveAsFlow()

    /** Bumped on foreground and each minute: steps have no change feed worth watching (SPEC §5.3). */
    private val stepsTick = MutableStateFlow(0L)

    private var ticker: Job? = null

    init {
        viewModelScope.launch {
            merge(container.habits.loaded, container.todos.loaded, container.settings.loaded).collect(::dispatch)
        }
        viewModelScope.launch { readCalendar() }
        viewModelScope.launch { readSteps() }
    }

    fun zone(): ZoneId = container.zone()

    fun dispatch(event: Event) {
        val next = reduce(_state.value, event, container.clock.instant(), container.zone())
        _state.value = next.state
        refreshDay()
        if (next.effects.isNotEmpty()) viewModelScope.launch { next.effects.forEach { perform(it) } }
    }

    fun refreshDay() {
        _now.value = container.clock.instant()
        _today.value = currentDay()
    }

    /** SPEC §5.1: the permission is re-read on every foreground and after each request. */
    fun checkCalendar(rationale: Boolean) {
        dispatch(Event.CalendarChecked(PermissionCheck(container.calendarGranted(), rationale)))
    }

    /** SPEC §5.1, §5.3: the SDK status and the grant are re-read on foreground and after a request. */
    fun checkHealth() {
        viewModelScope.launch { dispatch(Event.HealthChecked(container.steps.check())) }
    }

    /** What Health Connect's request asks for (D26). */
    fun healthPermissions(): Set<String> = container.steps.permissions()

    fun healthAnswered(granted: Boolean) {
        dispatch(Event.HealthAnswered(granted))
        checkHealth()
    }

    fun resume(calendarRationale: Boolean) {
        refreshDay()
        checkCalendar(calendarRationale)
        checkHealth()
        stepsTick.value++
        // SPEC §6.2: the widget catches up on every foreground.
        viewModelScope.launch { container.refreshWidget() }
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                delay(MINUTE - container.clock.millis() % MINUTE)
                refreshDay()
                stepsTick.value++
            }
        }
    }

    fun pause() {
        ticker?.cancel()
    }

    private fun currentDay() = today(container.clock.instant(), container.zone())

    /**
     * Re-reads the calendar when access is granted or lost, when `calendar_ids` changes, at
     * midnight, and whenever the provider reports a change.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun readCalendar() {
        val granted = state.map { it.calendarCheck?.granted == true }.distinctUntilChanged()
        val ids = container.settings.settings.map { it.calendarIds }.distinctUntilChanged()
        combine(granted, ids, today) { g, i, _ -> g to i }
            .collectLatest { (isGranted, calendarIds) ->
                if (isGranted) {
                    // Only observed while granted: registering an observer without the permission throws.
                    container.calendar.changes.collect {
                        dispatch(Event.CalendarsListed(container.calendar.calendars()))
                        val (from, until) = queryWindow(container.clock.instant(), container.zone())
                        dispatch(Event.CalendarLoaded(container.calendar.instances(from, until, calendarIds)))
                    }
                } else {
                    dispatch(Event.CalendarsListed(emptyList()))
                    dispatch(Event.CalendarLoaded(emptyList()))
                }
            }
    }

    /** Re-reads today and the last 7 days when access is granted, on foreground and each minute. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun readSteps() {
        val granted = state.map { it.healthCheck?.granted == true }.distinctUntilChanged()
        combine(granted, stepsTick) { g, _ -> g }
            .collectLatest { isGranted ->
                if (!isGranted) return@collectLatest
                container.steps.read(container.clock.instant(), container.zone())?.let(::dispatch)
            }
    }

    private suspend fun perform(effect: Effect) {
        when (effect) {
            is Effect.WriteHabit -> container.habits.write(effect.habit)
            is Effect.DeleteHabit -> container.habits.delete(effect.habitId)
            is Effect.RestoreHabit -> container.habits.restore(effect.habit, effect.completions)
            is Effect.ToggleCompletion -> container.habits.setCompletion(effect.habitId, effect.date, effect.done)
            is Effect.WriteTodo -> container.todos.write(effect.todo)
            is Effect.DeleteTodos -> container.todos.delete(effect.ids)
            is Effect.RequestPermission -> {
                when (effect.kind) {
                    PermissionKind.Calendar -> container.settings.setAskedCalendar()
                    PermissionKind.Health -> container.settings.setAskedHealth()
                }
                ui.send(UiEffect.RequestPermission(effect.kind))
            }
            Effect.OpenAppSettings -> ui.send(UiEffect.OpenAppSettings)
            is Effect.WriteCalendarIds -> container.settings.setCalendarIds(effect.ids)
            Effect.OpenHealthConnectInstall -> ui.send(UiEffect.OpenHealthConnectInstall)
            Effect.OpenHealthConnectSettings -> ui.send(UiEffect.OpenHealthConnectSettings)
            is Effect.WriteStepGoal -> container.settings.setStepGoal(effect.goal)
            is Effect.WriteTheme -> container.settings.setTheme(effect.choice)
            Effect.RefreshWidget -> container.refreshWidget()
            is Effect.ShowSnackbar -> ui.send(UiEffect.Snackbar(effect.message, effect.undo))
            Effect.Finish -> ui.send(UiEffect.Finish)
        }
    }

    private companion object {
        const val MINUTE = 60_000L
    }
}

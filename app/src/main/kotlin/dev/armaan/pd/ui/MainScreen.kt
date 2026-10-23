package dev.armaan.pd.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.armaan.pd.MainViewModel
import dev.armaan.pd.R
import dev.armaan.pd.UiEffect
import dev.armaan.pd.app.Event
import dev.armaan.pd.app.Message
import dev.armaan.pd.app.Screen
import dev.armaan.pd.habit.habitsToday
import dev.armaan.pd.ui.dashboard.DashboardScreen
import dev.armaan.pd.ui.habits.HabitsScreen
import kotlinx.coroutines.launch

/** Maps reducer state to screens and runs the UI effects. No rules here (D10). */
@Composable
fun MainScreen(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val habits = remember(state.habits, state.completions, today) {
        habitsToday(state.habits, state.completions, today)
    }
    val snackbar = remember { SnackbarHostState() }
    val activity = LocalActivity.current
    val resources = LocalResources.current
    // The dashboard's “Add a habit” opens Habits with the sheet up.
    var addHabit by rememberSaveable { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        vm.refreshDay()
        onPauseOrDispose {}
    }
    BackHandler { vm.dispatch(Event.Back) }

    LaunchedEffect(vm) {
        // Only the newest snackbar's dismissal clears the pending undo (SPEC §4.4).
        var shown = 0
        vm.uiEffects.collect { effect ->
            when (effect) {
                UiEffect.Finish -> activity?.finish()
                is UiEffect.Snackbar -> {
                    val id = ++shown
                    snackbar.currentSnackbarData?.dismiss()
                    launch {
                        val result = snackbar.showSnackbar(
                            message = text(resources, effect.message),
                            actionLabel = if (effect.undo) resources.getString(R.string.undo) else null,
                            duration = SnackbarDuration.Long,
                        )
                        when {
                            result == SnackbarResult.ActionPerformed -> vm.dispatch(Event.Undo)
                            id == shown -> vm.dispatch(Event.SnackbarDismissed)
                        }
                    }
                }
            }
        }
    }

    when (state.screen) {
        Screen.Habits -> HabitsScreen(
            today = today,
            habits = habits,
            snackbar = snackbar,
            startAdding = addHabit,
            onStartedAdding = { addHabit = false },
            onBack = { vm.dispatch(Event.Back) },
            onTick = { vm.dispatch(Event.ToggleHabit(it, today)) },
            onSave = { id, name, days -> vm.dispatch(Event.SaveHabit(id, name, days)) },
            onDelete = { vm.dispatch(Event.DeleteHabit(it)) },
        )
        // To-dos and Settings arrive with T5 and T6; nothing opens them yet.
        else -> Box(Modifier.fillMaxSize()) {
            DashboardScreen(
                today = today,
                habits = habits,
                anyHabits = state.habits.isNotEmpty(),
                onOpenHabits = { vm.dispatch(Event.Open(Screen.Habits)) },
                onAddHabit = {
                    addHabit = true
                    vm.dispatch(Event.Open(Screen.Habits))
                },
                onTick = { vm.dispatch(Event.ToggleHabit(it, today)) },
            )
            SnackbarHost(
                snackbar,
                Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.safeDrawing),
            )
        }
    }
}

private fun text(resources: android.content.res.Resources, message: Message): String = when (message) {
    is Message.HabitDeleted -> resources.getString(R.string.snack_habit_deleted, message.name)
    is Message.TodoDone -> resources.getString(R.string.snack_todo_done, message.title)
    is Message.HistoryCleared ->
        resources.getQuantityString(R.plurals.snack_history_cleared, message.count, message.count)
}

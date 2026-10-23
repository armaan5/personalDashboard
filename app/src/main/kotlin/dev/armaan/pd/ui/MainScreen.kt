package dev.armaan.pd.ui

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.health.connect.HealthConnectManager
import android.os.Build
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.net.toUri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.armaan.pd.MainViewModel
import dev.armaan.pd.R
import dev.armaan.pd.UiEffect
import dev.armaan.pd.app.Event
import dev.armaan.pd.app.Message
import dev.armaan.pd.app.PermissionKind
import dev.armaan.pd.app.Screen
import dev.armaan.pd.app.ThemeChoice
import dev.armaan.pd.calendar.agenda
import dev.armaan.pd.calendar.shownCalendars
import dev.armaan.pd.habit.habitsToday
import dev.armaan.pd.source.StepsReader
import dev.armaan.pd.steps.GoalDefault
import dev.armaan.pd.todo.buckets
import dev.armaan.pd.ui.dashboard.DashboardScreen
import dev.armaan.pd.ui.habits.HabitsScreen
import dev.armaan.pd.ui.settings.AboutScreen
import dev.armaan.pd.ui.settings.CalendarsScreen
import dev.armaan.pd.ui.settings.SettingsScreen
import dev.armaan.pd.ui.todos.TodosScreen
import kotlinx.coroutines.launch

/** Maps reducer state to screens and runs the UI effects. No rules here (D10). */
@Composable
fun MainScreen(vm: MainViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val now by vm.now.collectAsStateWithLifecycle()
    val zone = vm.zone()
    val agenda = remember(state.calendarInstances, now, zone) { agenda(state.calendarInstances, now, zone) }
    val habits = remember(state.habits, state.completions, today) {
        habitsToday(state.habits, state.completions, today)
    }
    val todos = remember(state.todos, today) { buckets(state.todos, today) }
    val stepGoal = state.settings?.stepGoal ?: GoalDefault
    val snackbar = remember { SnackbarHostState() }
    val activity = LocalActivity.current
    val resources = LocalResources.current
    // The dashboard's “Add a habit” / “Add a to-do” open their screen with the sheet up.
    var addHabit by rememberSaveable { mutableStateOf(false) }
    var addTodo by rememberSaveable { mutableStateOf(false) }

    // SPEC §5.1: once denied, the system only shows its dialog again while this is true.
    val calendarRationale = { activity?.shouldShowRequestPermissionRationale(Manifest.permission.READ_CALENDAR) ?: false }
    val requestCalendar = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.checkCalendar(calendarRationale())
    }
    // Health Connect's own contract replaces the runtime dialog (SPEC §5.3).
    val requestHealth = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) {
        vm.healthAnswered(StepsReader.ReadSteps in it)
    }

    LifecycleResumeEffect(Unit) {
        vm.resume(calendarRationale())
        onPauseOrDispose { vm.pause() }
    }
    BackHandler { vm.dispatch(Event.Back) }

    LaunchedEffect(vm) {
        // Only the newest snackbar's dismissal clears the pending undo (SPEC §4.4).
        var shown = 0
        vm.uiEffects.collect { effect ->
            when (effect) {
                UiEffect.Finish -> activity?.finish()
                is UiEffect.RequestPermission -> when (effect.kind) {
                    PermissionKind.Calendar -> requestCalendar.launch(Manifest.permission.READ_CALENDAR)
                    PermissionKind.Health -> try {
                        requestHealth.launch(vm.healthPermissions())
                    } catch (_: ActivityNotFoundException) {
                        // Health Connect went away since the last check: re-check, which ends the request.
                        vm.checkHealth()
                    }
                }
                UiEffect.OpenHealthConnectInstall -> activity?.let(::openHealthConnectInstall)
                UiEffect.OpenHealthConnectSettings -> activity?.let(::openHealthConnectSettings)
                UiEffect.OpenAppSettings -> activity?.let(::openAppSettings)
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
        Screen.Todos -> TodosScreen(
            buckets = todos,
            anyTodos = state.todos.isNotEmpty(),
            snackbar = snackbar,
            startAdding = addTodo,
            onStartedAdding = { addTodo = false },
            onBack = { vm.dispatch(Event.Back) },
            onTick = { vm.dispatch(Event.ToggleTodo(it)) },
            onSave = { id, title, due -> vm.dispatch(Event.SaveTodo(id, title, due)) },
            onClearHistory = { vm.dispatch(Event.ClearHistory) },
        )
        Screen.Settings -> SettingsScreen(
            onBack = { vm.dispatch(Event.Back) },
            onOpenCalendars = { vm.dispatch(Event.Open(Screen.Calendars)) },
            stepGoal = stepGoal,
            onStepGoal = { vm.dispatch(Event.SetStepGoal(it)) },
            theme = state.settings?.theme ?: ThemeChoice.System,
            onTheme = { vm.dispatch(Event.SetTheme(it)) },
            onOpenAbout = { vm.dispatch(Event.Open(Screen.About)) },
        )
        Screen.About -> AboutScreen(onBack = { vm.dispatch(Event.Back) })
        Screen.Calendars -> CalendarsScreen(
            access = state.calendarAccess,
            calendars = state.calendars,
            shown = shownCalendars(state.calendars, state.settings?.calendarIds),
            onBack = { vm.dispatch(Event.Back) },
            onGrant = { vm.dispatch(Event.GrantCalendar) },
            onToggle = { id, on -> vm.dispatch(Event.ToggleCalendar(id, on)) },
        )
        Screen.Dashboard -> Box(Modifier.fillMaxSize()) {
            DashboardScreen(
                today = today,
                onOpenSettings = { vm.dispatch(Event.Open(Screen.Settings)) },
                calendarAccess = state.calendarAccess,
                agenda = agenda,
                now = now,
                zone = zone,
                onCalendarShown = { vm.dispatch(Event.CalendarShown) },
                onGrantCalendar = { vm.dispatch(Event.GrantCalendar) },
                habits = habits,
                anyHabits = state.habits.isNotEmpty(),
                onOpenHabits = { vm.dispatch(Event.Open(Screen.Habits)) },
                onAddHabit = {
                    addHabit = true
                    vm.dispatch(Event.Open(Screen.Habits))
                },
                onTick = { vm.dispatch(Event.ToggleHabit(it, today)) },
                todos = todos,
                anyTodos = state.todos.isNotEmpty(),
                onOpenTodos = { vm.dispatch(Event.Open(Screen.Todos)) },
                onAddTodo = {
                    addTodo = true
                    vm.dispatch(Event.Open(Screen.Todos))
                },
                onTickTodo = { vm.dispatch(Event.ToggleTodo(it)) },
                steps = state.steps,
                stepGoal = stepGoal,
                onStepsShown = { vm.dispatch(Event.StepsShown) },
                onGrantSteps = { vm.dispatch(Event.GrantSteps) },
            )
            SnackbarHost(
                snackbar,
                Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.safeDrawing),
            )
        }
    }
}

private fun openAppSettings(activity: Activity) {
    activity.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", activity.packageName, null)),
    )
}

/** Health Connect's Play listing; the browser when Play is absent (SPEC §5.3). User-initiated, not app network use (D3). */
private fun openHealthConnectInstall(activity: Activity) {
    val play = Intent(Intent.ACTION_VIEW, "market://details?id=$HealthConnectPackage&url=healthconnect%3A%2F%2Fonboarding".toUri())
        .setPackage("com.android.vending")
    val web = Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$HealthConnectPackage".toUri())
    startFirst(activity, play, web)
}

/** Health Connect's permissions for this app on 34+, its own settings on 28–33, else the app's settings. */
private fun openHealthConnectSettings(activity: Activity) {
    val manage = if (Build.VERSION.SDK_INT >= 34) {
        Intent(HealthConnectManager.ACTION_MANAGE_HEALTH_PERMISSIONS).putExtra(Intent.EXTRA_PACKAGE_NAME, activity.packageName)
    } else {
        null
    }
    val settings = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
    if (!startFirst(activity, *listOfNotNull(manage, settings).toTypedArray())) openAppSettings(activity)
}

/** Starts the first intent something can handle; false when none can. */
private fun startFirst(activity: Activity, vararg intents: Intent): Boolean = intents.any {
    try {
        activity.startActivity(it)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

private const val HealthConnectPackage = "com.google.android.apps.healthdata"

private fun text(resources: android.content.res.Resources, message: Message): String = when (message) {
    is Message.HabitDeleted -> resources.getString(R.string.snack_habit_deleted, message.name)
    is Message.TodoDone -> resources.getString(R.string.snack_todo_done, message.title)
    is Message.HistoryCleared ->
        resources.getQuantityString(R.plurals.snack_history_cleared, message.count, message.count)
}

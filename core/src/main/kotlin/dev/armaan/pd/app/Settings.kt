package dev.armaan.pd.app

import dev.armaan.pd.steps.GoalDefault

/** SPEC §3.2, as DataStore last delivered it. [calendarIds] null = the key is absent. */
data class Settings(
    val calendarIds: Set<String>? = null,
    val askedCalendar: Boolean = false,
    val stepGoal: Int = GoalDefault,
    val askedHealth: Boolean = false,
    val theme: ThemeChoice = ThemeChoice.System,
)

package dev.armaan.pd.app

/** SPEC §3.2, as DataStore last delivered it. [calendarIds] null = the key is absent. */
data class Settings(
    val calendarIds: Set<String>? = null,
    val askedCalendar: Boolean = false,
)

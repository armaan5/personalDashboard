package dev.armaan.pd.calendar

/** One row of `CalendarContract.Calendars`, for Settings → Calendars (SPEC §5.2). */
data class DeviceCalendar(
    val id: String,
    val name: String,
    val account: String,
    /** ARGB, as the provider stores it. */
    val color: Int,
    val visible: Boolean,
)

/** SPEC §3.2: `calendar_ids` when present, otherwise every calendar with `VISIBLE = 1`. */
fun shownCalendars(calendars: List<DeviceCalendar>, stored: Set<String>?): Set<String> =
    stored ?: calendars.filter { it.visible }.mapTo(mutableSetOf()) { it.id }

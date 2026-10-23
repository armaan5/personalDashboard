package dev.armaan.pd.calendar

import dev.armaan.pd.time.today
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * One row of `CalendarContract.Instances` (SPEC §5.2). For an all-day row, [begin] and [end] are
 * UTC midnights, [end] exclusive.
 */
data class Instance(
    val eventId: Long,
    val title: String,
    val begin: Instant,
    val end: Instant,
    val allDay: Boolean,
)

/** One section of the card: the all-day strip, then timed events ordered by begin. */
data class AgendaDay(val allDay: List<Instance>, val timed: List<Instance>) {
    val isEmpty: Boolean get() = allDay.isEmpty() && timed.isEmpty()
}

data class Agenda(val today: AgendaDay, val tomorrow: AgendaDay)

/**
 * The window the reader queries: today and tomorrow in the local zone, padded by a day on each
 * side so no all-day row is lost to the provider's own zone handling. [agenda] drops the extras.
 */
fun queryWindow(now: Instant, zone: ZoneId): Pair<Instant, Instant> {
    val today = today(now, zone)
    return today.minusDays(1).atStartOfDay(zone).toInstant() to today.plusDays(3).atStartOfDay(zone).toInstant()
}

/** SPEC §5.2: today's remaining events and tomorrow's. */
fun agenda(instances: List<Instance>, now: Instant, zone: ZoneId): Agenda {
    val today = today(now, zone)
    val tomorrowStart = today.plusDays(1).atStartOfDay(zone).toInstant()
    val dayAfterStart = today.plusDays(2).atStartOfDay(zone).toInstant()
    val order = compareBy<Instance>({ it.begin }, { it.title })
    val (allDay, timed) = instances.partition { it.allDay }
    return Agenda(
        today = AgendaDay(
            allDay = allDay.filter { it.covers(today) }.sortedWith(order),
            // Includes events that began earlier and are still running, and ones spanning midnight.
            timed = timed.filter { it.begin < tomorrowStart && it.end > now }.sortedWith(order),
        ),
        tomorrow = AgendaDay(
            allDay = allDay.filter { it.covers(today.plusDays(1)) }.sortedWith(order),
            timed = timed.filter { it.begin >= tomorrowStart && it.begin < dayAfterStart }.sortedWith(order),
        ),
    )
}

/** All-day dates are read in UTC, never the local zone; [Instance.end] is exclusive. */
private fun Instance.covers(date: LocalDate): Boolean {
    val first = begin.atOffset(ZoneOffset.UTC).toLocalDate()
    val until = end.atOffset(ZoneOffset.UTC).toLocalDate()
    // A malformed zero-length row still shows on its start date.
    return if (until <= first) date == first else date >= first && date < until
}

/** In progress: the card stamps it "Now". */
fun Instance.isNow(now: Instant): Boolean = begin <= now && now < end

private val FullRule: Duration = Duration.ofHours(2)

/** The duration rule's length as a fraction of the row: two hours is full width (D23). */
fun Instance.ruleFraction(): Float =
    (Duration.between(begin, end).toMillis().toFloat() / FullRule.toMillis()).coerceIn(0f, 1f)

/** Where now falls along an event in progress, 0–1; null when it isn't in progress. */
fun Instance.progress(now: Instant): Float? =
    if (isNow(now)) Duration.between(begin, now).toMillis().toFloat() / Duration.between(begin, end).toMillis() else null

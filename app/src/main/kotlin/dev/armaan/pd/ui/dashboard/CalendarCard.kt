package dev.armaan.pd.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import dev.armaan.pd.R
import dev.armaan.pd.app.Access
import dev.armaan.pd.calendar.Agenda
import dev.armaan.pd.calendar.AgendaDay
import dev.armaan.pd.calendar.Instance
import dev.armaan.pd.calendar.isNow
import dev.armaan.pd.calendar.progress
import dev.armaan.pd.calendar.ruleFraction
import dev.armaan.pd.ui.Blocked
import dev.armaan.pd.ui.EmptyLine
import dev.armaan.pd.ui.Section
import dev.armaan.pd.ui.SectionHead
import dev.armaan.pd.ui.UiLocale
import dev.armaan.pd.ui.theme.Pd
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TimeColumn = 52.dp

/**
 * SPEC §5.2 card, D23 layout: today's all-day strip, the next event set large with its end time
 * and a Now stamp, the rest of today, then Tomorrow. The head opens Settings (SPEC §6.1).
 */
@Composable
fun CalendarCard(
    access: Access?,
    agenda: Agenda,
    now: Instant,
    zone: ZoneId,
    onShown: () -> Unit,
    onGrant: () -> Unit,
    onOpen: () -> Unit,
) {
    // SPEC §5.1: the first view in NotAsked requests; the reducer ignores every other state.
    LaunchedEffect(access) { onShown() }
    val time = DateTimeFormatter.ofPattern("HH:mm", UiLocale).withZone(zone)
    val description = when (access) {
        Access.Granted -> stringResource(R.string.calendar_card_cd, todaySummary(agenda.today), tomorrowSummary(agenda.tomorrow))
        null -> stringResource(R.string.section_calendar)
        else -> stringResource(R.string.calendar_card_off_cd)
    }
    Section {
        SectionHead(stringResource(R.string.section_calendar), description, Pd.colors.calendar, onClick = onOpen)
        when (access) {
            null -> Spacer(Modifier.height(8.dp))
            Access.NotAsked, Access.Denied ->
                Blocked(stringResource(R.string.calendar_off), stringResource(R.string.grant_access), onGrant)
            Access.PermanentlyDenied ->
                Blocked(stringResource(R.string.calendar_off_settings), stringResource(R.string.grant_access), onGrant)
            Access.Granted -> Column(Modifier.padding(bottom = 8.dp)) {
                Today(agenda.today, now, time)
                Text(
                    stringResource(R.string.calendar_tomorrow).uppercase(UiLocale),
                    style = Pd.type.sub,
                    color = Pd.colors.ink2,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
                Tomorrow(agenda.tomorrow, now, time)
            }
        }
    }
}

@Composable
private fun todaySummary(day: AgendaDay): String {
    val count = day.allDay.size + day.timed.size
    return if (count == 0) stringResource(R.string.calendar_cd_none_today)
    else pluralStringResource(R.plurals.calendar_cd_today, count, count)
}

@Composable
private fun tomorrowSummary(day: AgendaDay): String {
    val count = day.allDay.size + day.timed.size
    return if (count == 0) stringResource(R.string.calendar_cd_none_tomorrow)
    else stringResource(R.string.calendar_cd_tomorrow, count)
}

@Composable
private fun Today(day: AgendaDay, now: Instant, time: DateTimeFormatter) {
    AllDayStrip(day.allDay)
    val first = day.timed.firstOrNull()
    when {
        first != null -> {
            // Every row of today is ruled, so the last one closes today before Tomorrow (D23).
            NextEvent(first, now, time)
            day.timed.drop(1).forEach { event -> EventLine(event, now, time, divider = true) }
        }
        day.allDay.isEmpty() -> EmptyLine(stringResource(R.string.calendar_nothing_today))
    }
}

@Composable
private fun Tomorrow(day: AgendaDay, now: Instant, time: DateTimeFormatter) {
    if (day.isEmpty) {
        EmptyLine(stringResource(R.string.calendar_nothing_tomorrow))
        return
    }
    AllDayStrip(day.allDay)
    day.timed.forEachIndexed { i, event -> EventLine(event, now, time, divider = i < day.timed.lastIndex) }
}

@Composable
private fun title(event: Instance): String = event.title.ifBlank { stringResource(R.string.calendar_no_title) }

/** All-day events as washed bars (D23). */
@Composable
private fun AllDayStrip(events: List<Instance>) {
    if (events.isEmpty()) return
    Column(Modifier.padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        events.forEach { event ->
            val name = title(event)
            val description = stringResource(R.string.calendar_all_day_cd, name)
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Pd.colors.calendarWash, RoundedCornerShape(4.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                    .clearAndSetSemantics { contentDescription = description },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(name, style = Pd.type.allDay, color = Pd.colors.ink, modifier = Modifier.weight(1f))
                Text(
                    stringResource(R.string.calendar_all_day).uppercase(UiLocale),
                    style = Pd.type.allDayTag,
                    color = Pd.colors.ink2,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun eventDescription(event: Instance, now: Instant, time: DateTimeFormatter): String {
    val from = time.format(event.begin)
    val until = time.format(event.end)
    return if (event.isNow(now)) stringResource(R.string.calendar_event_now_cd, from, until, title(event))
    else stringResource(R.string.calendar_event_cd, from, until, title(event))
}

/** The next event, or the one in progress: start set large, end time, Now stamp, title. */
@Composable
private fun NextEvent(event: Instance, now: Instant, time: DateTimeFormatter) {
    val description = eventDescription(event, now, time)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 14.dp)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(time.format(event.begin), style = Pd.type.nextTime, color = Pd.colors.ink, modifier = Modifier.alignByBaseline())
            Text(
                stringResource(R.string.calendar_until, time.format(event.end)),
                style = Pd.type.nextEnd,
                color = Pd.colors.ink2,
                modifier = Modifier.alignByBaseline(),
            )
            if (event.isNow(now)) {
                Text(
                    stringResource(R.string.calendar_now).uppercase(UiLocale),
                    style = Pd.type.stamp,
                    color = Pd.colors.onInk,
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .background(Pd.colors.calendar, RoundedCornerShape(3.dp))
                        .padding(start = 6.dp, end = 6.dp, top = 4.dp, bottom = 3.dp),
                )
            }
        }
        Text(title(event), style = Pd.type.nextTitle, color = Pd.colors.ink, modifier = Modifier.padding(top = 6.dp))
        DurationRule(event, now, thickness = 3, modifier = Modifier.padding(top = 10.dp))
    }
    HorizontalDivider(thickness = 1.dp, color = Pd.colors.rule)
}

@Composable
private fun EventLine(event: Instance, now: Instant, time: DateTimeFormatter, divider: Boolean) {
    val description = eventDescription(event, now, time)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 11.dp)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Text(
            time.format(event.begin),
            style = Pd.type.eventTime,
            color = Pd.colors.ink,
            modifier = Modifier.width(TimeColumn).alignByBaseline(),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f).alignBy(FirstBaseline)) {
            Text(title(event), style = Pd.type.eventTitle, color = Pd.colors.ink)
            DurationRule(event, now, thickness = 2, modifier = Modifier.padding(top = 7.dp))
        }
    }
    if (divider) HorizontalDivider(thickness = 1.dp, color = Pd.colors.rule)
}

/**
 * A rule whose length is proportional to the event's (two hours fills the row, D23). On the
 * event in progress, an ink tick marks now. Decorative: the row's description carries the times.
 */
@Composable
private fun DurationRule(event: Instance, now: Instant, thickness: Int, modifier: Modifier = Modifier) {
    // The rule sits flush under its padding, so it underlines the title; the tick overhangs it.
    BoxWithConstraints(modifier.fillMaxWidth().height(thickness.dp)) {
        val length = maxWidth * event.ruleFraction()
        Box(
            Modifier
                .width(length)
                .height(thickness.dp)
                .background(Pd.colors.calendar, RoundedCornerShape(2.dp)),
        )
        event.progress(now)?.let { at ->
            Box(
                Modifier
                    .offset(x = (length * at - 1.dp).coerceAtLeast(0.dp))
                    .width(2.dp)
                    // Taller than the rule's box: required size overhangs it, centred on the rule.
                    .requiredHeight(9.dp)
                    .background(Pd.colors.ink),
            )
        }
    }
}

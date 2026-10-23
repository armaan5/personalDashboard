package dev.armaan.pd.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.armaan.pd.AppContainer
import dev.armaan.pd.MainActivity
import dev.armaan.pd.PdApplication
import dev.armaan.pd.R
import dev.armaan.pd.app.PermissionCheck
import dev.armaan.pd.app.access
import dev.armaan.pd.calendar.queryWindow
import dev.armaan.pd.steps.lastSevenDays
import dev.armaan.pd.steps.stepsCard
import dev.armaan.pd.steps.stepsLabel
import dev.armaan.pd.ui.theme.DarkColors
import dev.armaan.pd.ui.theme.LightColors
import dev.armaan.pd.ui.theme.PdColors
import dev.armaan.pd.ui.UiLocale
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * SPEC §6.2, D19: one read-only summary widget, the T3 widget comp. It reads the same sources and
 * `:core` rules as the app each time it is refreshed; there is no widget cache. Tap opens the
 * Dashboard.
 */
class DashboardWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as PdApplication).container
        val seen = refreshes.value
        val first = container.drawn()
        provideContent {
            // A live session only recomposes on update, so later refreshes re-read the sources here.
            val drawn by remember { refreshes.dropWhile { it == seen }.map { container.drawn() } }.collectAsState(first)
            Content(drawn.summary, drawn.zone)
        }
    }

    companion object {
        /** Bumped by each refresh. It carries no data, only the signal to read again. */
        private val refreshes = MutableStateFlow(0L)

        /** Redraws every placed widget from the sources (SPEC §6.2). */
        suspend fun refresh(context: Context) {
            refreshes.update { it + 1 }
            DashboardWidget().updateAll(context)
        }
    }
}

private class Drawn(val summary: Summary, val zone: ZoneId)

private suspend fun AppContainer.drawn() = Drawn(summary(), zone())

/** Plumbing: reads each source once and hands the snapshots to [summary]. */
private suspend fun AppContainer.summary(): Summary {
    val now = clock.instant()
    val zone = zone()
    val settings = settings.settings.first()
    val habits = habits.loaded.first()
    val calendarOn = calendarGranted()
    val instances = if (calendarOn) {
        val (from, until) = queryWindow(now, zone)
        calendar.instances(from, until, settings.calendarIds)
    } else {
        emptyList()
    }
    val health = steps.check()
    // Null in the background without the background permission: the line shows a dash (D26).
    val read = if (health.granted) steps.read(now, zone) else null
    // Every blocked state reads "Open to set up" here, so Denied and PermanentlyDenied needn't differ.
    val access = access(PermissionCheck(health.granted, rationale = true), settings.askedHealth)
    val card = stepsCard(health.sdk, access, read?.total, read?.let { lastSevenDays(it.days, it.day) })
    return summary(now, zone, calendarOn, instances, card, settings.stepGoal, habits.habits, habits.completions)
}

private fun provider(pick: PdColors.() -> Color) = ColorProvider(day = LightColors.pick(), night = DarkColors.pick())

private val Ink = provider { ink }
private val Ink2 = provider { ink2 }
private val Rule = provider { rule }
private val CalendarDot = provider { calendar }
private val StepsDot = provider { steps }
private val HabitsDot = provider { habits }

/** RemoteViews can't load the app's Barlow; the system condensed face stands in. */
private val Condensed = FontFamily("sans-serif-condensed")

private val Day = DateTimeFormatter.ofPattern("EEE d", UiLocale)
private val Month = DateTimeFormatter.ofPattern("MMM", UiLocale)

@Composable
private fun Content(summary: Summary, zone: ZoneId) {
    val context = LocalContext.current
    val open = Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .putExtra(MainActivity.EXTRA_DASHBOARD, true)
    Column(
        GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(ImageProvider(R.drawable.widget_stock))
            .padding(start = 14.dp, top = 12.dp, end = 12.dp, bottom = 10.dp)
            .clickable(actionStartActivity(open)),
    ) {
        Row(GlanceModifier.fillMaxWidth().padding(bottom = 7.dp), verticalAlignment = Alignment.Bottom) {
            Text(summary.today.format(Day), GlanceModifier.defaultWeight(), style = text(19.sp, FontWeight.Bold, Ink), maxLines = 1)
            Text(summary.today.format(Month), style = text(15.sp, FontWeight.Medium, Ink2), maxLines = 1)
        }
        Box(GlanceModifier.fillMaxWidth().height(2.dp).background(Ink)) {}
        NextLine(context, summary.next, zone)
        Hairline()
        StepsLine(context, summary.steps)
        Hairline()
        HabitsLine(context, summary.habits)
    }
}

@Composable
private fun ColumnScope.NextLine(context: Context, line: NextLine, zone: ZoneId) = when (line) {
    is NextLine.Event -> {
        val time = DateTimeFormatter.ofPattern("HH:mm", UiLocale).withZone(zone).format(line.event.begin)
        val title = line.event.title.ifBlank { context.getString(R.string.calendar_no_title) }
        val value = context.getString(if (line.tomorrow) R.string.widget_next_tomorrow else R.string.widget_next, time, title)
        Line(CalendarDot, context.getString(R.string.widget_next_cd, value), value)
    }
    NextLine.Nothing -> context.getString(R.string.widget_nothing_scheduled).let { Line(CalendarDot, it, it) }
    NextLine.SetUp -> SetUpLine(context, CalendarDot, context.getString(R.string.widget_calendar_set_up_cd))
}

@Composable
private fun ColumnScope.StepsLine(context: Context, line: StepsLine) = when (line) {
    is StepsLine.Count -> Line(
        StepsDot,
        context.getString(R.string.steps_card_cd, stepsLabel(line.today), stepsLabel(line.goal.toLong())),
        stepsLabel(line.today),
        context.getString(R.string.widget_steps_of, stepsLabel(line.goal.toLong())),
    )
    StepsLine.SetUp -> SetUpLine(context, StepsDot, context.getString(R.string.widget_steps_set_up_cd))
    StepsLine.Dash -> Line(StepsDot, context.getString(R.string.widget_steps_none_cd), context.getString(R.string.widget_dash))
}

@Composable
private fun ColumnScope.HabitsLine(context: Context, line: HabitsLine) = when (line) {
    is HabitsLine.Count -> Line(
        HabitsDot,
        context.getString(R.string.habits_card_cd, line.done, line.scheduled),
        context.getString(R.string.widget_habits_count, line.done, line.scheduled),
        context.getString(R.string.widget_habits_unit),
    )
    HabitsLine.NoneToday -> context.getString(R.string.widget_no_habits).let { Line(HabitsDot, it, it) }
}

@Composable
private fun ColumnScope.SetUpLine(context: Context, dot: ColorProvider, description: String) =
    Line(dot, description, context.getString(R.string.widget_dash), hint = context.getString(R.string.widget_set_up))

/**
 * One summary line: a source dot, the value in bold with an optional small suffix, then a hint.
 * The hint sits beside the dash rather than under it, so three lines fit a 2×2 without clipping.
 */
@Composable
private fun ColumnScope.Line(dot: ColorProvider, description: String, value: String, suffix: String? = null, hint: String? = null) {
    Row(
        GlanceModifier.fillMaxWidth().defaultWeight().semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(ImageProvider(R.drawable.widget_dot), null, GlanceModifier.size(8.dp), colorFilter = ColorFilter.tint(dot))
        Spacer(GlanceModifier.width(8.dp))
        Text(value, style = text(18.sp, FontWeight.Bold, Ink), maxLines = 1)
        if (suffix != null) Text(" $suffix", style = text(15.sp, FontWeight.Medium, Ink2), maxLines = 1)
        if (hint != null) {
            Text("  $hint", style = TextStyle(fontSize = 12.sp, color = Ink2, fontFamily = FontFamily.SansSerif), maxLines = 1)
        }
    }
}

@Composable
private fun Hairline() = Box(GlanceModifier.fillMaxWidth().height(1.dp).background(Rule)) {}

private fun text(size: TextUnit, weight: FontWeight, color: ColorProvider) =
    TextStyle(fontSize = size, fontWeight = weight, color = color, fontFamily = Condensed)

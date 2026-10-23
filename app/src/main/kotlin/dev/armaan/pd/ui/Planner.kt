package dev.armaan.pd.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import dev.armaan.pd.R
import dev.armaan.pd.habit.ChainDay
import dev.armaan.pd.habit.HabitRow
import dev.armaan.pd.ui.theme.Pd
import dev.armaan.pd.ui.theme.PdIcons
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** The app is en-GB only (D21: no translations), so dates format in the UK locale. */
val UiLocale: Locale = Locale.UK

private val Gutter = 16.dp
private val TickSize = 48.dp
private val TickGap = 12.dp
private val Dot = 14.dp
private val DotGap = 6.dp

/** A ruled planner section: a 2 dp ink rule opens it; no card, no elevation (D23). */
@Composable
fun Section(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val ink = Pd.colors.ink
    Column(
        modifier
            .padding(horizontal = Gutter)
            .fillMaxWidth()
            .drawBehind { drawRect(ink, size = Size(size.width, 2.dp.toPx())) }
            .padding(top = 2.dp),
        content = content,
    )
}

/**
 * Section head, a 48 dp row. It is announced as one node, [description], which carries the
 * section's summary (SPEC §6.1, e.g. “Habits, 2 of 3 done”). With [onClick] it opens its screen.
 */
@Composable
fun SectionHead(
    title: String,
    description: String,
    dot: Color?,
    onClick: (() -> Unit)? = null,
    meta: @Composable RowScope.() -> Unit = {},
) {
    val click = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(click)
            .clearAndSetSemantics {
                contentDescription = description
                heading()
                if (onClick != null) {
                    role = Role.Button
                    onClick { onClick(); true }
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (dot != null) Box(Modifier.size(10.dp).background(dot, CircleShape))
        Text(
            title.uppercase(UiLocale),
            style = Pd.type.section,
            color = Pd.colors.ink,
            modifier = Modifier.weight(1f),
        )
        meta()
        if (onClick != null) Icon(PdIcons.Chevron, contentDescription = null, tint = Pd.colors.ink2)
    }
}

/** "2/3 done" beside a section title. */
@Composable
fun CountMeta(done: Int, total: Int) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(stringResource(R.string.habits_count, done, total), style = Pd.type.count, color = Pd.colors.ink)
        Spacer(Modifier.width(4.dp))
        Text(
            stringResource(R.string.habits_done),
            style = Pd.type.meta,
            color = Pd.colors.ink2,
            modifier = Modifier.padding(bottom = 3.dp),
        )
    }
}

/** M T W T F S S over the week chains, today underlined (D23). Decorative for TalkBack. */
@Composable
fun WeekLetters(today: LocalDate) {
    Row(
        Modifier.padding(start = TickSize + TickGap, bottom = 2.dp).clearAndSetSemantics {},
        horizontalArrangement = Arrangement.spacedBy(DotGap),
    ) {
        DayOfWeek.entries.forEach { day ->
            val isToday = day == today.dayOfWeek
            Text(
                day.getDisplayName(TextStyle.NARROW, UiLocale),
                style = Pd.type.caps.copy(textDecoration = if (isToday) TextDecoration.Underline else null),
                color = if (isToday) Pd.colors.ink else Pd.colors.ink2,
                modifier = Modifier.width(Dot),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * One habit: today's tick (48 dp), the name over this week's chain, the streak (D23).
 * [onTick] is null for a habit not scheduled today. [onEdit] makes the row open the edit sheet.
 */
@Composable
fun HabitLine(
    row: HabitRow,
    onTick: (() -> Unit)?,
    schedule: String? = null,
    onEdit: (() -> Unit)? = null,
    divider: Boolean = true,
) {
    val name = row.habit.name
    val description = if (row.streak > 0) {
        stringResource(R.string.habit_row_cd, name, row.streak)
    } else {
        stringResource(R.string.habit_row_no_streak_cd, name)
    }
    val editLabel = stringResource(R.string.habit_edit_action)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 68.dp).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onTick != null) {
            Tick(done = row.doneToday, description = stringResource(R.string.habit_tick_cd, name), onToggle = onTick)
        } else {
            Spacer(Modifier.size(TickSize))
        }
        Spacer(Modifier.width(TickGap))
        val edit = if (onEdit != null) Modifier.clickable(onClickLabel = editLabel, onClick = onEdit) else Modifier
        Row(
            Modifier
                .weight(1f)
                .then(edit)
                .clearAndSetSemantics {
                    contentDescription = description
                    if (onEdit != null) onClick(editLabel) { onEdit(); true }
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    style = Pd.type.habitName,
                    color = if (onTick != null) Pd.colors.ink else Pd.colors.ink2,
                )
                Spacer(Modifier.size(7.dp))
                Chain(row.week)
                if (schedule != null) {
                    Text(schedule, style = Pd.type.schedule, color = Pd.colors.ink2, modifier = Modifier.padding(top = 3.dp))
                }
            }
            Column(Modifier.widthIn(min = 56.dp), horizontalAlignment = Alignment.End) {
                Text(row.streak.toString(), style = Pd.type.streak, color = Pd.colors.ink)
                Text(stringResource(R.string.streak_label).uppercase(UiLocale), style = Pd.type.caps, color = Pd.colors.ink2)
            }
        }
    }
    if (divider) HorizontalDivider(thickness = 1.dp, color = Pd.colors.rule)
}

/**
 * Today's tick. The signature moment (D23): ticking presses a sticker on, 1.12 → 1 with a 3°
 * settle over 200 ms. Compose scales it with the system animator setting, so “Remove
 * animations” makes it instant.
 */
@Composable
private fun Tick(done: Boolean, description: String, onToggle: () -> Unit) {
    val scale = remember { Animatable(1f) }
    val tilt = remember { Animatable(if (done) -3f else 0f) }
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(done) {
        if (first[0]) {
            first[0] = false
            return@LaunchedEffect
        }
        if (done) {
            scale.snapTo(1.12f)
            tilt.snapTo(0f)
            val spec = tween<Float>(200, easing = LinearOutSlowInEasing)
            coroutineScope {
                launch { scale.animateTo(1f, spec) }
                tilt.animateTo(-3f, spec)
            }
        } else {
            scale.snapTo(1f)
            tilt.snapTo(0f)
        }
    }
    val c = Pd.colors
    Box(
        Modifier
            .size(TickSize)
            .toggleable(value = done, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(34.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    rotationZ = tilt.value
                }
                .then(
                    if (done) Modifier.background(c.habits, CircleShape)
                    else Modifier.border(2.dp, c.ink2, CircleShape),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Icon(PdIcons.Check, contentDescription = null, tint = c.onInk, modifier = Modifier.size(22.dp))
        }
    }
}

/** This week's chain, Monday first. Decorative: the row's description carries the streak. */
@Composable
private fun Chain(week: List<ChainDay>) {
    val c = Pd.colors
    Row(horizontalArrangement = Arrangement.spacedBy(DotGap)) {
        week.forEach { day ->
            Canvas(Modifier.size(Dot)) {
                val r = size.minDimension / 2
                val hair = 1.5.dp.toPx()
                when (day) {
                    ChainDay.Done -> drawCircle(c.habits, r)
                    ChainDay.Missed -> drawCircle(c.ink2, r - hair / 2, style = Stroke(hair))
                    ChainDay.Off -> drawRoundRect(c.tint, cornerRadius = CornerRadius(2.dp.toPx()))
                    ChainDay.Open -> {
                        drawCircle(c.ink, r - hair / 2, style = Stroke(hair))
                        drawCircle(c.ink, r * 0.45f)
                    }
                    ChainDay.Ahead -> drawCircle(
                        c.ink2.copy(alpha = 0.7f),
                        r - hair / 2,
                        style = Stroke(hair, pathEffect = PathEffect.dashPathEffect(floatArrayOf(hair, hair * 1.4f))),
                    )
                }
            }
        }
    }
}

/** A screen's top bar: Back, then the screen title as a heading. */
@Composable
fun TopBar(title: String, onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .heightIn(min = 64.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(PdIcons.Back, contentDescription = stringResource(R.string.back), tint = Pd.colors.ink)
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = Pd.colors.ink,
            modifier = Modifier.padding(start = 4.dp).semantics { heading() },
        )
    }
}

/**
 * A sheet's top edge: the 2 dp ink rule every planner section opens with (D23), then the drag
 * handle. The sheet's shape clips the rule into its rounded corners.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetHandle() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().height(2.dp).background(Pd.colors.ink))
        BottomSheetDefaults.DragHandle(color = Pd.colors.ink2)
    }
}

/** One line saying what is missing, then the action that fixes it (SPEC §7, D7). */
@Composable
fun EmptyLine(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = Pd.colors.ink,
        modifier = modifier.padding(top = 4.dp, bottom = 8.dp).widthIn(max = 340.dp),
    )
}

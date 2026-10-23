package dev.armaan.pd.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.armaan.pd.R
import dev.armaan.pd.app.Access
import dev.armaan.pd.steps.DaySteps
import dev.armaan.pd.steps.StepsCard
import dev.armaan.pd.steps.barScale
import dev.armaan.pd.steps.ringFraction
import dev.armaan.pd.steps.stepsLabel
import dev.armaan.pd.steps.toGo
import dev.armaan.pd.ui.Blocked
import dev.armaan.pd.ui.EmptyLine
import dev.armaan.pd.ui.Section
import dev.armaan.pd.ui.SectionHead
import dev.armaan.pd.ui.UiLocale
import dev.armaan.pd.ui.theme.Pd
import java.time.format.TextStyle

private val RingSize = 112.dp
private val RingStroke = 12.dp
private val BarsHeight = 92.dp
private val BarWidth = 24.dp

/**
 * SPEC §5.3 card, D23 layout: a flat ring for today / goal beside the uncapped count, then 7 bars,
 * today last and outlined. Bars that met the goal turn tomato. The head opens Settings (SPEC §6.1).
 */
@Composable
fun StepsCard(card: StepsCard?, goal: Int, onShown: () -> Unit, onGrant: () -> Unit, onOpen: () -> Unit) {
    // SPEC §5.1: the first view in NotAsked requests; the reducer ignores every other state.
    LaunchedEffect(card) { onShown() }
    val goalLabel = stepsLabel(goal.toLong())
    val description = when (card) {
        is StepsCard.Steps -> stringResource(R.string.steps_card_cd, stepsLabel(card.today), goalLabel)
        is StepsCard.Blocked -> stringResource(R.string.steps_card_off_cd)
        StepsCard.Unavailable -> stringResource(R.string.steps_card_unavailable_cd)
        StepsCard.NeedsInstall -> stringResource(R.string.steps_card_install_cd)
        StepsCard.NoData -> stringResource(R.string.steps_card_no_data_cd)
        null -> stringResource(R.string.section_steps)
    }
    Section {
        SectionHead(stringResource(R.string.section_steps), description, Pd.colors.steps, onClick = onOpen) {
            // Only beside a count: the comp's blocked head has none (D23).
            if (card is StepsCard.Steps) {
                Text(stringResource(R.string.steps_goal_meta, goalLabel), style = Pd.type.meta, color = Pd.colors.ink2)
            }
        }
        when (card) {
            null -> Spacer(Modifier.height(8.dp))
            StepsCard.Unavailable -> EmptyLine(stringResource(R.string.steps_unavailable), Modifier.padding(bottom = 8.dp))
            StepsCard.NeedsInstall ->
                Blocked(stringResource(R.string.steps_needs_install), stringResource(R.string.get_health_connect), onGrant)
            is StepsCard.Blocked -> Blocked(
                stringResource(if (card.access == Access.PermanentlyDenied) R.string.steps_off_settings else R.string.steps_off),
                stringResource(R.string.grant_access),
                onGrant,
            )
            StepsCard.NoData -> EmptyLine(stringResource(R.string.steps_no_data), Modifier.padding(bottom = 8.dp))
            is StepsCard.Steps -> {
                Today(card.today, goal, goalLabel)
                Bars(card.last7, goal, goalLabel)
            }
        }
    }
}

/** The ring and the count. The head's description carries both for TalkBack. */
@Composable
private fun Today(today: Long, goal: Int, goalLabel: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 16.dp)
            .clearAndSetSemantics {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        val track = Pd.colors.track
        val steps = Pd.colors.steps
        val fraction = ringFraction(today, goal)
        Canvas(Modifier.size(RingSize)) {
            val stroke = RingStroke.toPx()
            val inset = stroke / 2
            val arc = Size(size.width - stroke, size.height - stroke)
            drawCircle(track, radius = (size.minDimension - stroke) / 2, style = Stroke(stroke))
            if (fraction > 0f) {
                drawArc(steps, -90f, 360f * fraction, useCenter = false, topLeft = Offset(inset, inset), size = arc, style = Stroke(stroke))
            }
        }
        Column(Modifier.weight(1f)) {
            // Uncapped and never wrapped: at large font scales the count shrinks to fit instead.
            BasicText(
                stepsLabel(today),
                style = Pd.type.stepsCount.copy(color = Pd.colors.ink),
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 24.sp, maxFontSize = Pd.type.stepsCount.fontSize),
            )
            Text(
                stringResource(R.string.steps_of_goal, goalLabel),
                style = Pd.type.stepsOf,
                color = Pd.colors.ink2,
                modifier = Modifier.padding(top = 4.dp),
            )
            val left = toGo(today, goal)
            Text(
                if (left > 0) stringResource(R.string.steps_to_go, stepsLabel(left)) else stringResource(R.string.steps_goal_met),
                style = MaterialTheme.typography.bodyMedium,
                color = Pd.colors.ink,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/**
 * The last 7 days, today last, with a dashed goal line. Decorative: TalkBack reads the head.
 * Bars below the goal keep the comp's ink tint, capped with an ink2 hairline so each still reads
 * against the stock (WCAG 1.4.11). Today is always outlined, as a stub on the baseline at 0.
 */
@Composable
private fun Bars(week: List<DaySteps>, goal: Int, goalLabel: String) {
    val colors = Pd.colors
    val measurer = rememberTextMeasurer()
    val labelStyle = Pd.type.barLabel.copy(color = colors.ink2)
    val scale = barScale(week, goal).toFloat()
    Column(Modifier.padding(bottom = 12.dp).clearAndSetSemantics {}) {
        Canvas(Modifier.fillMaxWidth().height(BarsHeight)) {
            val column = size.width / week.size
            val bar = BarWidth.toPx()
            val radius = CornerRadius(2.dp.toPx())
            val stub = 3.dp.toPx()
            val cap = 1.5.dp.toPx()
            week.forEachIndexed { i, day ->
                val isToday = i == week.lastIndex
                val h = (size.height * (day.count / scale)).let { if (isToday) it.coerceAtLeast(stub) else it }
                if (h <= 0f) return@forEachIndexed
                val topLeft = Offset(column * i + (column - bar) / 2, size.height - h)
                val met = day.count >= goal
                drawRoundRect(if (met) colors.steps else colors.track, topLeft, Size(bar, h), radius)
                if (!met && !isToday) drawRect(colors.ink2, topLeft, Size(bar, cap.coerceAtMost(h)))
                if (isToday) {
                    val line = 1.5.dp.toPx()
                    drawRoundRect(
                        colors.ink,
                        topLeft + Offset(line / 2, line / 2),
                        Size(bar - line, h - line / 2),
                        radius,
                        style = Stroke(line),
                    )
                }
            }
            // Goal line, labelled at its right end, then the baseline in ink.
            val y = size.height * (1f - goal / scale)
            val hair = 1.dp.toPx()
            drawLine(
                colors.ink2,
                Offset(0f, y),
                Offset(size.width, y),
                hair,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
            )
            // Knocked out of whatever bar it crosses, so it keeps its contrast once today meets the goal.
            val label = measurer.measure(goalLabel, labelStyle)
            val at = Offset(size.width - label.size.width, (y - label.size.height - 2.dp.toPx()).coerceAtLeast(0f))
            val pad = 2.dp.toPx()
            drawRect(colors.stock, at - Offset(pad, 0f), Size(label.size.width + pad, label.size.height.toFloat()))
            drawText(label, topLeft = at)
            drawLine(colors.ink, Offset(0f, size.height - hair / 2), Offset(size.width, size.height - hair / 2), hair)
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            week.forEachIndexed { i, day ->
                Text(
                    day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, UiLocale),
                    style = Pd.type.barLabel,
                    color = if (i == week.lastIndex) colors.ink else colors.ink2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

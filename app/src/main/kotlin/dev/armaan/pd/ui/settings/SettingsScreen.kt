package dev.armaan.pd.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.armaan.pd.R
import dev.armaan.pd.app.ThemeChoice
import dev.armaan.pd.steps.GoalMax
import dev.armaan.pd.steps.GoalMin
import dev.armaan.pd.steps.GoalStep
import dev.armaan.pd.steps.stepsLabel
import dev.armaan.pd.ui.Section
import dev.armaan.pd.ui.TopBar
import dev.armaan.pd.ui.theme.Pd
import dev.armaan.pd.ui.theme.PdIcons

/** SPEC §6.1 Settings: Calendars, Step goal, Theme, About. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenCalendars: () -> Unit,
    stepGoal: Int,
    onStepGoal: (Int) -> Unit,
    theme: ThemeChoice,
    onTheme: (ThemeChoice) -> Unit,
    onOpenAbout: () -> Unit,
) {
    Scaffold(
        topBar = { TopBar(stringResource(R.string.settings), onBack) },
        containerColor = Pd.colors.stock,
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 32.dp),
        ) {
            item {
                Section {
                    SettingsRow(
                        stringResource(R.string.settings_calendars),
                        stringResource(R.string.settings_calendars_hint),
                        onOpenCalendars,
                    )
                    HorizontalDivider(thickness = 1.dp, color = Pd.colors.rule)
                    StepGoalRow(stepGoal, onStepGoal)
                    HorizontalDivider(thickness = 1.dp, color = Pd.colors.rule)
                    ThemeRow(theme, onTheme)
                    HorizontalDivider(thickness = 1.dp, color = Pd.colors.rule)
                    SettingsRow(stringResource(R.string.settings_about), stringResource(R.string.settings_about_hint), onOpenAbout)
                }
            }
        }
    }
}

/**
 * SPEC §5.3 Step goal: a stepper in steps of 500. The reducer clamps it; the buttons stop at the
 * ends so the range is visible. TalkBack reads the value as one node between the two buttons.
 */
@Composable
private fun StepGoalRow(goal: Int, onChange: (Int) -> Unit) {
    val label = stepsLabel(goal.toLong())
    val description = stringResource(R.string.step_goal_cd, label)
    val buttons = IconButtonDefaults.iconButtonColors(
        contentColor = Pd.colors.ink,
        disabledContentColor = Pd.colors.ink.copy(alpha = 0.38f),
    )
    // Sized to the widest goal, so the − button doesn't jump when the goal crosses 10,000.
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val valueWidth = with(density) { measurer.measure(stepsLabel(GoalMax.toLong()), Pd.type.count).size.width.toDp() }
    val stepper = @Composable {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onChange(goal - GoalStep) }, enabled = goal > GoalMin, colors = buttons) {
                Icon(PdIcons.Remove, contentDescription = stringResource(R.string.step_goal_less))
            }
            Text(
                label,
                style = Pd.type.count,
                color = Pd.colors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(valueWidth)
                    .clearAndSetSemantics {
                        contentDescription = description
                        liveRegion = LiveRegionMode.Polite
                    },
            )
            IconButton(onClick = { onChange(goal + GoalStep) }, enabled = goal < GoalMax, colors = buttons) {
                Icon(PdIcons.Add, contentDescription = stringResource(R.string.step_goal_more))
            }
        }
    }
    val text = @Composable { modifier: Modifier ->
        Column(modifier) {
            Text(stringResource(R.string.settings_step_goal), style = Pd.type.habitName, color = Pd.colors.ink)
            Text(
                stringResource(R.string.settings_step_goal_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Pd.colors.ink2,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
    // At large font scales the stepper takes its own line, so the label doesn't wrap to a sliver.
    if (density.fontScale >= 1.5f) {
        Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
            text(Modifier)
            Box(Modifier.padding(top = 4.dp).offset(x = (-12).dp)) { stepper() }
        }
    } else {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            text(Modifier.weight(1f))
            stepper()
        }
    }
}

/**
 * D27 c, e: three chips in the weekday-chip style, one always chosen. TalkBack reads each as a
 * radio button with its selected state, in a group.
 */
@Composable
private fun ThemeRow(choice: ThemeChoice, onChange: (ThemeChoice) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(stringResource(R.string.settings_theme), style = Pd.type.habitName, color = Pd.colors.ink)
        Text(
            stringResource(R.string.settings_theme_hint),
            style = MaterialTheme.typography.bodySmall,
            color = Pd.colors.ink2,
            modifier = Modifier.padding(top = 3.dp),
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ThemeChoice.entries.forEach { option ->
                ThemeChip(stringResource(option.label), option == choice, Modifier.weight(1f)) { onChange(option) }
            }
        }
    }
}

private val ThemeChoice.label: Int
    get() = when (this) {
        ThemeChoice.System -> R.string.theme_system
        ThemeChoice.Light -> R.string.theme_light
        ThemeChoice.Dark -> R.string.theme_dark
    }

/** `DESIGN.md` Chips: off is a 1.5 dp ink-2 outline with ink text; on is an ink fill with stock text. */
@Composable
private fun ThemeChip(label: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = Pd.colors
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier
            .heightIn(min = 48.dp)
            .clip(shape)
            .then(if (on) Modifier.background(c.ink) else Modifier.border(1.5.dp, c.ink2, shape))
            .selectable(selected = on, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = Pd.type.week.copy(fontSize = 17.sp), color = if (on) c.onInk else c.ink, textAlign = TextAlign.Center)
    }
}

/** A row that opens a sub-screen: title over a one-line hint, then a chevron. */
@Composable
private fun SettingsRow(title: String, hint: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = Pd.type.habitName, color = Pd.colors.ink)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = Pd.colors.ink2, modifier = Modifier.padding(top = 3.dp))
        }
        Icon(PdIcons.Chevron, contentDescription = null, tint = Pd.colors.ink2)
    }
}

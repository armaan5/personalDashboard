package dev.armaan.pd.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * D23 “The Wall Planner”: two values per scheme (stock + one ink), every neutral a fixed tint of
 * the ink, and four sticker colours that only mean a source or an achievement. Actions are ink.
 * Source of truth: `design/t3/comps.html`.
 */
@Immutable
data class PdColors(
    val stock: Color,
    val ink: Color,
    /** Secondary text: ink at 72 % over stock (5.3:1 in light). */
    val ink2: Color,
    /** Hairline grid: ink 16 %. */
    val rule: Color,
    /** Unscheduled day, the planner's weekend shade: ink 7 %. */
    val tint: Color,
    /** Ring track and bars below goal: ink 10–12 %. */
    val track: Color,
    val onInk: Color,
    val calendar: Color,
    val calendarWash: Color,
    val habits: Color,
    val todos: Color,
    val steps: Color,
    /** Ink on a yellow sticker, in both schemes. */
    val onTodos: Color,
    val scrim: Color,
)

private val Ink = Color(0xFF14324F)
private val Stock = Color(0xFFF7F9F8)
private val Night = Color(0xFF0E1C2B)
private val Chalk = Color(0xFFE6EEF2)

val LightColors = PdColors(
    stock = Stock,
    ink = Ink,
    ink2 = Color(0xFF546A7E),
    rule = Ink.copy(alpha = 0.16f),
    tint = Ink.copy(alpha = 0.07f),
    track = Ink.copy(alpha = 0.10f),
    onInk = Stock,
    calendar = Color(0xFF2A6DB8),
    calendarWash = Color(0xFF2A6DB8).copy(alpha = 0.14f),
    habits = Color(0xFF3E9C55),
    todos = Color(0xFFE8B92F),
    steps = Color(0xFFE0573E),
    onTodos = Ink,
    scrim = Ink.copy(alpha = 0.32f),
)

val DarkColors = PdColors(
    stock = Night,
    ink = Chalk,
    ink2 = Color(0xFFA9B3BA),
    rule = Chalk.copy(alpha = 0.16f),
    tint = Chalk.copy(alpha = 0.07f),
    track = Chalk.copy(alpha = 0.12f),
    onInk = Night,
    calendar = Color(0xFF6AA5EC),
    calendarWash = Color(0xFF6AA5EC).copy(alpha = 0.18f),
    habits = Color(0xFF5DBB72),
    todos = Color(0xFFF0C94A),
    steps = Color(0xFFF07A5F),
    onTodos = Ink,
    scrim = Color.Black.copy(alpha = 0.5f),
)

package dev.armaan.pd.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.armaan.pd.R

/** D23: Barlow Condensed for heads, times and numerals; Barlow for names and body. Bundled, OFL. */
val Barlow = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
)

val BarlowCondensed = FontFamily(
    Font(R.font.barlow_condensed_medium, FontWeight.Medium),
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
)

private const val TABULAR = "tnum"

private fun condensed(weight: FontWeight, size: Int, line: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = BarlowCondensed,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.em,
    fontFeatureSettings = TABULAR,
)

private fun barlow(weight: FontWeight, size: Int, line: Int) = TextStyle(
    fontFamily = Barlow,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontFeatureSettings = TABULAR,
)

/** Planner roles the Material scale has no name for. */
@Immutable
data class PdType(
    /** Masthead weekday. */
    val day: TextStyle = condensed(FontWeight.Medium, 20, 22, 0.02),
    /** Masthead date. */
    val date: TextStyle = condensed(FontWeight.Bold, 46, 46),
    /** Section head, set in capitals. */
    val section: TextStyle = condensed(FontWeight.SemiBold, 19, 22, 0.06),
    /** Small capitals: weekday letters, “streak”, “Tomorrow”. */
    val caps: TextStyle = condensed(FontWeight.SemiBold, 12, 14, 0.06),
    /** Section meta, beside a [count]. */
    val meta: TextStyle = condensed(FontWeight.SemiBold, 15, 18, 0.02),
    val count: TextStyle = condensed(FontWeight.Bold, 28, 30),
    val streak: TextStyle = condensed(FontWeight.Bold, 30, 32),
    val week: TextStyle = condensed(FontWeight.SemiBold, 15, 18, 0.04),
    val habitName: TextStyle = barlow(FontWeight.Medium, 17, 22),
    val schedule: TextStyle = condensed(FontWeight.Medium, 13, 16, 0.03),
    val todoTitle: TextStyle = barlow(FontWeight.Normal, 16, 21),
    /** A to-do's due date at the row's end. */
    val due: TextStyle = condensed(FontWeight.SemiBold, 14, 16, 0.03),
    /** “OVERDUE”, heavy ink capitals beside the pennant (D23). */
    val overdue: TextStyle = condensed(FontWeight.Bold, 14, 16, 0.07),
    /** Calendar: the next event's start, set large (D23). */
    val nextTime: TextStyle = condensed(FontWeight.Bold, 38, 40),
    val nextEnd: TextStyle = condensed(FontWeight.Medium, 17, 18),
    val nextTitle: TextStyle = barlow(FontWeight.Medium, 20, 25),
    /** The "Now" stamp on the event in progress. */
    val stamp: TextStyle = condensed(FontWeight.Bold, 13, 14, 0.08),
    val eventTime: TextStyle = condensed(FontWeight.SemiBold, 19, 22),
    val eventTitle: TextStyle = barlow(FontWeight.Normal, 16, 21),
    val allDay: TextStyle = barlow(FontWeight.Medium, 15, 19),
    /** "ALL DAY" at the end of a washed bar. */
    val allDayTag: TextStyle = condensed(FontWeight.SemiBold, 13, 16, 0.05),
    /** A sub-head inside a section, e.g. “Tomorrow”. */
    val sub: TextStyle = condensed(FontWeight.SemiBold, 14, 16, 0.07),
)

val PdTypography = Typography(
    displaySmall = condensed(FontWeight.Bold, 46, 46),
    headlineMedium = condensed(FontWeight.Bold, 30, 34),
    titleLarge = condensed(FontWeight.Bold, 26, 30),
    titleMedium = condensed(FontWeight.SemiBold, 19, 22, 0.06),
    bodyLarge = barlow(FontWeight.Normal, 16, 22),
    bodyMedium = barlow(FontWeight.Normal, 15, 21),
    bodySmall = barlow(FontWeight.Normal, 13, 18),
    labelLarge = barlow(FontWeight.SemiBold, 16, 20),
    labelMedium = condensed(FontWeight.SemiBold, 12, 14, 0.07),
    labelSmall = condensed(FontWeight.SemiBold, 11, 13, 0.05),
)

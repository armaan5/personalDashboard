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

package dev.armaan.pd.ui.theme

import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import dev.armaan.pd.app.ThemeChoice
import dev.armaan.pd.app.isDark
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

private val LocalPdColors = staticCompositionLocalOf { LightColors }
private val LocalPdType = staticCompositionLocalOf { PdType() }

/** Planner tokens beside [MaterialTheme]. */
object Pd {
    val colors: PdColors
        @Composable @ReadOnlyComposable get() = LocalPdColors.current

    val type: PdType
        @Composable @ReadOnlyComposable get() = LocalPdType.current
}

/** 4 dp corners throughout; 8 dp on the FAB, 12 dp on a sheet's top (D23). */
private val PdShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(4.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp),
)

/** Material roles resolved from the two planner values, so stock components stay in the world. */
private fun PdColors.scheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = ink, onPrimary = onInk,
        primaryContainer = ink, onPrimaryContainer = onInk,
        secondary = ink, onSecondary = onInk,
        secondaryContainer = tint, onSecondaryContainer = ink,
        tertiary = ink, onTertiary = onInk,
        background = stock, onBackground = ink,
        surface = stock, onSurface = ink,
        surfaceVariant = tint, onSurfaceVariant = ink2,
        surfaceContainerLowest = stock, surfaceContainerLow = stock, surfaceContainer = stock,
        surfaceContainerHigh = stock, surfaceContainerHighest = stock,
        surfaceBright = stock, surfaceDim = stock,
        inverseSurface = ink, inverseOnSurface = onInk, inversePrimary = onInk,
        outline = ink2, outlineVariant = rule,
        scrim = scrim,
    )
}

/**
 * An activity's theme for Settings → Theme [choice] (D27): light or dark, and the status- and
 * navigation-bar icons to match. The one place the system's dark mode is read.
 *
 * [choice] is null until settings have loaded; nothing is drawn until then (the window background
 * shows), so a forced theme never flashes the other scheme's screen first.
 */
@Composable
fun PdTheme(choice: ThemeChoice?, content: @Composable () -> Unit) {
    if (choice == null) return
    val dark = isDark(choice, systemDark = androidx.compose.foundation.isSystemInDarkTheme())
    val activity = LocalActivity.current as? ComponentActivity
    LaunchedEffect(activity, dark) {
        activity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(NavLightScrim, NavDarkScrim) { dark },
        )
    }
    PdTheme(dark, content)
}

/** `enableEdgeToEdge`'s own navigation-bar scrims (three-button navigation on 28). */
private val NavLightScrim = AndroidColor.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val NavDarkScrim = AndroidColor.argb(0x80, 0x1b, 0x1b, 0x1b)

/** Light or dark planner tokens; no Dynamic Color (D23). */
@Composable
fun PdTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) DarkColors else LightColors
    CompositionLocalProvider(LocalPdColors provides colors, LocalPdType provides PdType()) {
        MaterialTheme(
            colorScheme = colors.scheme(dark),
            typography = PdTypography,
            shapes = PdShapes,
            content = content,
        )
    }
}

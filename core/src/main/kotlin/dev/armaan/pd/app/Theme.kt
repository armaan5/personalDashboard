package dev.armaan.pd.app

/** Settings → Theme (D27 c). Stored by name; absent = [System]. */
enum class ThemeChoice { System, Light, Dark }

/** Whether the app draws dark: the choice, or the system's mode when the choice is [ThemeChoice.System]. */
fun isDark(choice: ThemeChoice, systemDark: Boolean): Boolean = when (choice) {
    ThemeChoice.System -> systemDark
    ThemeChoice.Light -> false
    ThemeChoice.Dark -> true
}

/** The widget's palette (D27 d): forced by a Light or Dark choice, day/night on System. */
enum class WidgetScheme { Light, Dark, DayNight }

fun widgetScheme(choice: ThemeChoice): WidgetScheme = when (choice) {
    ThemeChoice.System -> WidgetScheme.DayNight
    ThemeChoice.Light -> WidgetScheme.Light
    ThemeChoice.Dark -> WidgetScheme.Dark
}

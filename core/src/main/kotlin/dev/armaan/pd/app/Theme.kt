package dev.armaan.pd.app

/** Settings → Theme (D27 c). Stored by name; absent = [System]. */
enum class ThemeChoice { System, Light, Dark }

/** Whether the app draws dark: the choice, or the system's mode when the choice is [ThemeChoice.System]. */
fun isDark(choice: ThemeChoice, systemDark: Boolean): Boolean = when (choice) {
    ThemeChoice.System -> systemDark
    ThemeChoice.Light -> false
    ThemeChoice.Dark -> true
}

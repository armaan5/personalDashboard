package dev.armaan.pd.app

import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

/** D27 c, d, SPEC §3.2, §4.4, §6.2. */
class ThemeTest {
    private val now = Instant.parse("2026-10-23T10:00:00Z")
    private val zone = ZoneId.of("Europe/London")

    @Test
    fun isDarkFollowsSystemOnlyForSystem() {
        assertEquals(false, isDark(ThemeChoice.System, systemDark = false))
        assertEquals(true, isDark(ThemeChoice.System, systemDark = true))
        assertEquals(false, isDark(ThemeChoice.Light, systemDark = false))
        assertEquals(false, isDark(ThemeChoice.Light, systemDark = true))
        assertEquals(true, isDark(ThemeChoice.Dark, systemDark = false))
        assertEquals(true, isDark(ThemeChoice.Dark, systemDark = true))
    }

    @Test
    fun setThemeWritesAndRefreshesWidget() {
        val out = reduce(State(settings = Settings()), Event.SetTheme(ThemeChoice.Dark), now, zone)
        assertEquals(listOf(Effect.WriteTheme(ThemeChoice.Dark), Effect.RefreshWidget), out.effects)
    }

    @Test
    fun defaultSettingsThemeIsSystem() {
        assertEquals(ThemeChoice.System, Settings().theme)
    }

    @Test
    fun widgetSchemeMatchesChoice() {
        assertEquals(WidgetScheme.DayNight, widgetScheme(ThemeChoice.System))
        assertEquals(WidgetScheme.Light, widgetScheme(ThemeChoice.Light))
        assertEquals(WidgetScheme.Dark, widgetScheme(ThemeChoice.Dark))
    }
}

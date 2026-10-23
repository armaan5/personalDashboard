package dev.armaan.pd.ui

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.armaan.pd.AppContainer
import dev.armaan.pd.MainActivity
import dev.armaan.pd.PdApplication
import dev.armaan.pd.ui.theme.DarkColors
import dev.armaan.pd.ui.theme.LightColors
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * BACKLOG U2 Accept (D27 c, e). The system is put in light mode first, so Dark can only come from
 * the setting. Fresh in-memory container with isolated settings; calendar refused and both
 * permissions already asked, so no system dialog covers the dashboard.
 */
@RunWith(AndroidJUnit4::class)
class SettingsThemeTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = instrumentation.targetContext.applicationContext as PdApplication

    @get:Rule
    val rule = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null
    private var nightBefore: String? = null

    @Before
    fun setUp() {
        nightBefore = shell("cmd uimode night").substringAfter(":").trim().takeIf { it == "yes" || it == "no" }
        shell("cmd uimode night no")
        val utc = ZoneOffset.UTC
        app.container = AppContainer(
            app,
            clock = Clock.fixed(Instant.parse("2026-10-23T10:00:00Z"), utc),
            zoneOf = { utc },
            inMemory = true,
            calendarGranted = { false },
        )
        runBlocking {
            app.container.settings.setAskedCalendar()
            app.container.settings.setAskedHealth()
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario?.close()
        nightBefore?.let { shell("cmd uimode night $it") }
    }

    private fun shell(command: String): String =
        instrumentation.uiAutomation.executeShellCommand(command).use { fd ->
            java.io.FileInputStream(fd.fileDescriptor).bufferedReader().readText()
        }

    /** A pixel of the Settings background, below its rows. */
    private fun background(): Int {
        val pixels = rule.onRoot().captureToImage().toPixelMap()
        return pixels[pixels.width / 2, pixels.height * 7 / 8].toArgb()
    }

    private fun openSettingsAndPickDark() {
        rule.waitUntil(TIMEOUT) { runCatching { rule.onNodeWithContentDescription("Settings").assertExists() }.isSuccess }
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("System").assertIsSelected()
        check(background() == LightColors.stock.toArgb()) { "Settings didn't start light" }

        rule.onNodeWithText("Dark").performClick()
        rule.waitUntil(TIMEOUT) { background() == DarkColors.stock.toArgb() }
    }

    private fun assertDark() {
        rule.onNodeWithText("Dark").assertIsSelected()
        rule.onNodeWithText("System").assertIsNotSelected()
        rule.onNodeWithText("Light").assertIsNotSelected()
        check(background() == DarkColors.stock.toArgb()) { "Settings background isn't DarkColors.stock" }
    }

    @Test
    fun pickingDarkRecoloursTheApp() {
        openSettingsAndPickDark()
        assertDark()
    }

    @Test
    fun choiceSurvivesRecreate() {
        openSettingsAndPickDark()
        scenario!!.recreate()
        rule.waitUntil(TIMEOUT) { runCatching { rule.onNodeWithText("Dark").assertExists() }.isSuccess }
        assertDark()
    }

    private companion object {
        const val TIMEOUT = 10_000L
    }
}

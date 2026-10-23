package dev.armaan.pd.ui

import android.content.res.Configuration
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.armaan.pd.AppContainer
import dev.armaan.pd.MainActivity
import dev.armaan.pd.PdApplication
import dev.armaan.pd.todo.Todo
import dev.armaan.pd.ui.theme.DarkColors
import dev.armaan.pd.ui.theme.LightColors
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import kotlin.test.assertTrue

/**
 * BACKLOG U1 Accept (D27 a). Wednesday 2026-10-21, 12:00 UTC, on an in-memory database with
 * enough to-dos that the Dashboard is longer than the screen.
 */
@RunWith(AndroidJUnit4::class)
class DashboardInsetsTest {
    private val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as PdApplication
    private val now = Instant.parse("2026-10-21T12:00:00Z")

    private val inject = object : ExternalResource() {
        override fun before() {
            val utc = ZoneOffset.UTC
            // Calendar refused and already asked: no system dialog, and no reads of the device's own calendars.
            app.container = AppContainer(app, clock = Clock.fixed(now, utc), zoneOf = { utc }, inMemory = true, calendarGranted = { false })
            runBlocking {
                app.container.settings.setAskedCalendar()
                // Steps already asked too, so Health Connect's request doesn't cover the dashboard.
                app.container.settings.setAskedHealth()
                repeat(16) { i -> app.container.todos.write(Todo(0, "To-do ${i + 1}", null, createdAt = now.minusSeconds(3600L * (i + 1)), completedOn = null)) }
            }
        }
    }

    private val rule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(inject).around(rule)

    /** The status-bar inset's height in px, read from the window. */
    private fun statusBarInset(): Int {
        val view = rule.activity.window.decorView
        val insets = ViewCompat.getRootWindowInsets(view) ?: error("no window insets")
        return insets.getInsets(WindowInsetsCompat.Type.statusBars()).top.also { check(it > 0) { "no status bar" } }
    }

    /** Steps is the fourth section: scrolling to it puts the sections above under the bar. */
    private fun scrollToSteps() {
        rule.onNode(hasScrollAction()).performScrollToNode(hasContentDescription("Steps, ", substring = true))
        rule.waitForIdle()
    }

    private fun SemanticsNode.descendants(): List<SemanticsNode> = children.flatMap { listOf(it) + it.descendants() }

    @Test
    fun scrolledRowsStayBelowTheStatusBar() {
        scrollToSteps()
        val inset = statusBarInset()

        val list = rule.onNode(hasScrollAction(), useUnmergedTree = true).fetchSemanticsNode()
        // boundsInRoot is clipped by every ancestor, so a row cut off by the list's edge reports
        // only its visible part, and one scrolled fully out reports no height.
        val visible = list.descendants().map { it.boundsInRoot }.filter { it.height > 0f }
        assertTrue(visible.isNotEmpty())
        val above = visible.filter { it.top < inset }
        assertTrue(above.isEmpty(), "rows above the ${inset}px inset: $above")
    }

    @Test
    fun bandFillsTheInsetInStock() {
        scrollToSteps()
        val inset = statusBarInset()
        val night = rule.activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val stock = (if (night == Configuration.UI_MODE_NIGHT_YES) DarkColors else LightColors).stock.toArgb()

        // The capture is the app's window alone: the clock and status icons aren't in it.
        val image = rule.onRoot().captureToImage()
        val pixels = IntArray(image.width * inset)
        image.readPixels(pixels, startY = 0, width = image.width, height = inset)
        val off = pixels.count { it != stock }
        assertTrue(off == 0, "$off of ${pixels.size} px in the inset are not stock")
    }
}

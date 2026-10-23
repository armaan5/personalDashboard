package dev.armaan.pd.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.TextDecoration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.armaan.pd.AppContainer
import dev.armaan.pd.MainActivity
import dev.armaan.pd.PdApplication
import dev.armaan.pd.todo.Todo
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** BACKLOG T5 Accept. Wednesday 2026-10-21, 12:00 UTC, on an in-memory database. */
@RunWith(AndroidJUnit4::class)
class TodosFlowTest {
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
            }
        }
    }

    private val rule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(inject).around(rule)

    private fun oct(day: Int) = LocalDate.of(2026, 10, day)

    private fun seed(vararg todos: Todo) = runBlocking {
        todos.forEach { app.container.todos.write(it) }
        rule.waitForIdle()
    }

    private fun todo(title: String, due: LocalDate? = null, createdHoursAgo: Long = 1, completedOn: LocalDate? = null) =
        Todo(0, title, due, createdAt = now.minusSeconds(createdHoursAgo * 3600), completedOn = completedOn)

    /** The dashboard composes lazily; bring the To-dos section on screen first. */
    private fun scrollToTodos() {
        rule.onNode(hasScrollAction()).performScrollToNode(hasContentDescription("To-dos", substring = true))
    }

    private fun top(text: String) = rule.onNodeWithText(text).fetchSemanticsNode().boundsInRoot.top

    @Test
    fun undatedAndDueYesterday_bothShowOverdueFirst() {
        // The undated one is older, so only the overdue rule can put the other first (SPEC §4.3).
        seed(todo("Order a bike chain", createdHoursAgo = 48), todo("Renew passport photo", due = oct(20)))
        scrollToTodos()

        rule.onNodeWithText("Order a bike chain").assertExists()
        rule.onNodeWithText("Renew passport photo").assertExists()
        rule.onNodeWithText("Overdue", ignoreCase = true).assertExists()
        assertTrue(top("Renew passport photo") < top("Order a bike chain"))
    }

    @Test
    fun tick_strikesThroughAndStaysOnTheCard() {
        seed(todo("Call the garage", due = oct(21)))
        scrollToTodos()

        rule.onNodeWithContentDescription("Tick Call the garage").performClick()
        rule.waitForIdle()

        val node = rule.onNodeWithText("Call the garage").fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
        assertEquals(TextDecoration.LineThrough, layouts.single().layoutInput.style.textDecoration)
        rule.onNodeWithContentDescription("To-dos, 0 left").assertExists()
    }

    @Test
    fun clearHistoryThenUndo_restoresIt() {
        seed(todo("Pay council tax", completedOn = oct(20)))
        scrollToTodos()
        rule.onNodeWithContentDescription("To-dos, nothing due").performClick()

        rule.onNodeWithContentDescription("Done, 1 to-do").performClick()
        rule.onNodeWithText("Pay council tax").assertExists()
        rule.onNodeWithText("Clear history").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Pay council tax").assertDoesNotExist()

        rule.onNodeWithText("Undo").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Pay council tax").assertExists()
    }

    @Test
    fun futureDatedItem_appearsOnlyOnTheTodosScreen() {
        seed(todo("Book the dentist", due = oct(22)))
        scrollToTodos()
        rule.onNodeWithText("Book the dentist").assertDoesNotExist()

        rule.onNodeWithContentDescription("To-dos, nothing due").performClick()

        rule.onNodeWithText("Book the dentist").assertExists()
    }
}

package dev.armaan.pd.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.armaan.pd.AppContainer
import dev.armaan.pd.MainActivity
import dev.armaan.pd.PdApplication
import dev.armaan.pd.habit.Habit
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import kotlin.test.assertTrue

/** BACKLOG T4 Accept. Wednesday 2026-10-21, 12:00 UTC, on an in-memory database. */
@RunWith(AndroidJUnit4::class)
class HabitsFlowTest {
    private val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as PdApplication

    private val inject = object : ExternalResource() {
        override fun before() {
            val utc = ZoneOffset.UTC
            app.container = AppContainer(
                app,
                clock = Clock.fixed(Instant.parse("2026-10-21T12:00:00Z"), utc),
                zoneOf = { utc },
                inMemory = true,
            )
        }
    }

    private val rule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(inject).around(rule)

    private fun oct(day: Int) = LocalDate.of(2026, 10, day)

    private fun back() {
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    /** Stretch, Mon–Fri since Monday 19th, done Mon and Tue: a 2-day streak, open today. */
    private fun seedStretch() = runBlocking {
        val weekdays = DayOfWeek.entries.take(5).toSet()
        app.container.habits.write(Habit(0, "Stretch", weekdays, createdOn = oct(19), sortOrder = 0))
        rule.waitForIdle()
        val id = 1L
        app.container.habits.setCompletion(id, oct(19), done = true)
        app.container.habits.setCompletion(id, oct(20), done = true)
        rule.waitForIdle()
    }

    @Test
    fun addTickedHabit_cardShowsOneOfOneWithStreakOne() {
        rule.onNodeWithContentDescription("Habits, nothing scheduled today").performClick()
        rule.onNodeWithContentDescription("Add habit").performClick()
        rule.onNode(hasSetTextAction()).performTextInput("Stretch")
        rule.onNodeWithContentDescription("Saturday").performClick()
        rule.onNodeWithContentDescription("Sunday").performClick()
        rule.onNodeWithText("Save").performClick()
        rule.waitForIdle()
        back()

        rule.onNodeWithContentDescription("Tick Stretch").performClick()
        rule.waitForIdle()

        rule.onNodeWithContentDescription("Habits, 1 of 1 done").assertExists()
        rule.onNodeWithContentDescription("Stretch, 1-day streak").assertExists()
    }

    @Test
    fun deleteThenUndo_restoresHabitWithItsStreak() {
        seedStretch()
        rule.onNodeWithContentDescription("Habits, 0 of 1 done").performClick()

        // On the Habits screen the row itself opens the edit sheet.
        rule.onNodeWithContentDescription("Stretch, 2-day streak").performClick()
        rule.onNodeWithText("Delete").performClick()
        rule.waitForIdle()
        rule.onAllNodesWithText("Stretch").assertCountEquals(0)

        rule.onNodeWithText("Undo").performClick()
        rule.waitForIdle()
        rule.onNodeWithContentDescription("Stretch, 2-day streak").assertExists()
    }

    @Test
    fun systemBackFromHabits_returnsToDashboard() {
        rule.onNodeWithContentDescription("Habits, nothing scheduled today").performClick()
        rule.onNodeWithContentDescription("Add habit").assertExists()

        back()

        rule.onNodeWithContentDescription("Add habit").assertDoesNotExist()
        rule.onNodeWithContentDescription("Calendar").assertExists()
    }

    @Test
    fun everyCardAndControl_hasAContentDescription() {
        seedStretch()
        listOf("Calendar", "Habits, 0 of 1 done", "To-dos, nothing to do", "Steps").forEach {
            rule.onNodeWithContentDescription(it).assertExists()
        }
        assertControlsLabelled()

        rule.onNodeWithContentDescription("Habits, 0 of 1 done").performClick()
        assertControlsLabelled()
    }

    /** Every clickable node is announced by a content description or its own text. */
    private fun assertControlsLabelled() {
        val labelled = SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription) or
            SemanticsMatcher.keyIsDefined(SemanticsProperties.Text)
        val clickables = rule.onAllNodes(hasClickAction()).fetchSemanticsNodes()
        assertTrue(clickables.isNotEmpty())
        val unlabelled = rule.onAllNodes(hasClickAction() and !labelled).fetchSemanticsNodes()
        assertTrue(unlabelled.isEmpty(), "unlabelled controls: ${unlabelled.map { it.config }}")
    }
}

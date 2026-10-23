package dev.armaan.pd.ui

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import android.provider.CalendarContract
import android.provider.CalendarContract.Calendars
import android.provider.CalendarContract.Events
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.armaan.pd.AppContainer
import dev.armaan.pd.MainActivity
import dev.armaan.pd.PdApplication
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * BACKLOG T6 Accept. Thursday 2026-10-22, 10:40 UTC, on an in-memory database with isolated
 * settings.
 *
 * Revoking a runtime permission kills the process under test, so the blocked case fakes the check
 * through [AppContainer]'s seam. The granted case grants `READ_CALENDAR` for real and inserts the
 * event through `CalendarContract` under the shell's identity: the app itself never holds
 * `WRITE_CALENDAR` (D21).
 */
@RunWith(AndroidJUnit4::class)
class CalendarCardTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = instrumentation.targetContext.applicationContext as PdApplication
    private val now = Instant.parse("2026-10-22T10:40:00Z")
    private val utc = ZoneOffset.UTC

    @get:Rule
    val rule = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null
    private var calendarId: Long? = null

    @After
    fun tearDown() {
        scenario?.close()
        calendarId?.let { id -> asShell { app.contentResolver.delete(asSyncAdapter(ContentUris.withAppendedId(Calendars.CONTENT_URI, id)), null, null) } }
    }

    private fun launch(calendarGranted: (() -> Boolean)? = null, askedBefore: Boolean = false) {
        app.container = AppContainer(app, clock = Clock.fixed(now, utc), zoneOf = { utc }, inMemory = true, calendarGranted = calendarGranted)
        if (askedBefore) runBlocking { app.container.settings.setAskedCalendar() }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    private fun <T> asShell(block: () -> T): T {
        instrumentation.uiAutomation.adoptShellPermissionIdentity(Manifest.permission.WRITE_CALENDAR, Manifest.permission.READ_CALENDAR)
        return try {
            block()
        } finally {
            instrumentation.uiAutomation.dropShellPermissionIdentity()
        }
    }

    private fun asSyncAdapter(uri: Uri): Uri = uri.buildUpon()
        .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
        .appendQueryParameter(Calendars.ACCOUNT_NAME, ACCOUNT)
        .appendQueryParameter(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
        .build()

    /** A local calendar holding one event, [from]–[until] UTC. */
    private fun insertEvent(title: String, from: Instant, until: Instant) = asShell {
        val calendar = ContentValues().apply {
            put(Calendars.ACCOUNT_NAME, ACCOUNT)
            put(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(Calendars.NAME, ACCOUNT)
            put(Calendars.CALENDAR_DISPLAY_NAME, "Test")
            put(Calendars.CALENDAR_COLOR, 0xFF2A6DB8.toInt())
            put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER)
            put(Calendars.OWNER_ACCOUNT, ACCOUNT)
            put(Calendars.VISIBLE, 1)
            put(Calendars.SYNC_EVENTS, 1)
        }
        val id = ContentUris.parseId(app.contentResolver.insert(asSyncAdapter(Calendars.CONTENT_URI), calendar)!!)
        calendarId = id
        val event = ContentValues().apply {
            put(Events.CALENDAR_ID, id)
            put(Events.TITLE, title)
            put(Events.DTSTART, from.toEpochMilli())
            put(Events.DTEND, until.toEpochMilli())
            put(Events.EVENT_TIMEZONE, "UTC")
        }
        app.contentResolver.insert(Events.CONTENT_URI, event)
    }

    @Test
    fun permissionRevoked_showsTheReasonAndGrant() {
        // Asked before and refused, so the card doesn't fire the system dialog over the test.
        launch(calendarGranted = { false }, askedBefore = true)

        rule.waitUntil(TIMEOUT) { rule.onAllNodes(hasText("Grant access")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Calendar access is off", substring = true).assertExists()
        rule.onNodeWithText("Grant access").assertExists()
    }

    @Test
    fun permissionGranted_insertedEventTitleAppears() {
        instrumentation.uiAutomation.grantRuntimePermission(app.packageName, Manifest.permission.READ_CALENDAR)
        insertEvent("Lunch with Sam", Instant.parse("2026-10-22T13:00:00Z"), Instant.parse("2026-10-22T14:00:00Z"))
        launch()

        val lunch = hasContentDescription("Lunch with Sam", substring = true)
        rule.waitUntil(TIMEOUT) { rule.onAllNodes(lunch).fetchSemanticsNodes().isNotEmpty() }
        rule.onNode(hasContentDescription("13:00 to 14:00, Lunch with Sam")).assertExists()
    }

    private companion object {
        const val ACCOUNT = "pd-calendar-card-test"
        const val TIMEOUT = 5_000L
    }
}

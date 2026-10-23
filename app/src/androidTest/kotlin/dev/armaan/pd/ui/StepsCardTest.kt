package dev.armaan.pd.ui

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.health.connect.HealthConnectManager
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.performScrollToNode
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.armaan.pd.AppContainer
import dev.armaan.pd.MainActivity
import dev.armaan.pd.PdApplication
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * BACKLOG T7 Accept, on `pd_api37` where Health Connect is built in. The clock is fixed at the
 * real time the test starts: Health Connect refuses records that end in the future.
 *
 * The record is inserted through `HealthConnectClient` under the app's own identity, using the
 * `WRITE_STEPS` grant that only debug builds declare (D25). The grant goes through Health
 * Connect's own per-app page, not `pm grant`: only that path puts the app on Health Connect's
 * data-priority list, and `aggregate` counts nothing from an app that isn't on it (seen on
 * `pd_api37`: `readRecords` returned the record, `aggregate` returned null).
 */
@RunWith(AndroidJUnit4::class)
class StepsCardTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val app = instrumentation.targetContext.applicationContext as PdApplication
    private val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
    private val utc = ZoneOffset.UTC
    private val client by lazy { HealthConnectClient.getOrCreate(app) }

    @get:Rule
    val rule = createEmptyComposeRule()

    private var scenario: ActivityScenario<MainActivity>? = null
    private val inserted = mutableListOf<String>()

    @After
    fun tearDown() {
        scenario?.close()
        if (inserted.isNotEmpty()) runBlocking { client.deleteRecords(StepsRecord::class, inserted, emptyList()) }
    }

    private val readSteps = HealthPermission.getReadPermission(StepsRecord::class)
    private val writeSteps = HealthPermission.getWritePermission(StepsRecord::class)

    private fun granted(permission: String) = app.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    /** Turns on "Allow all" on Health Connect's page for this app, then leaves it. */
    private fun grantThroughHealthConnect() {
        if (granted(readSteps) && granted(writeSteps)) return
        val ui = instrumentation.uiAutomation
        ui.executeShellCommand(
            // A fresh task, so a page left open by an earlier run can't be toggled back off.
            "am start -f ${Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK} " +
                "-a ${HealthConnectManager.ACTION_MANAGE_HEALTH_PERMISSIONS} " +
                "--es ${Intent.EXTRA_PACKAGE_NAME} ${app.packageName}",
        ).close()
        val deadline = System.currentTimeMillis() + TIMEOUT
        while (!(granted(readSteps) && granted(writeSteps))) {
            check(System.currentTimeMillis() < deadline) { "Health Connect's \"Allow all\" was not found or did not grant" }
            ui.rootInActiveWindow?.findAccessibilityNodeInfosByText("Allow all")?.firstOrNull()?.let { label ->
                generateSequence(label) { it.parent }.firstOrNull { it.isClickable }
                    ?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            Thread.sleep(500)
        }
        ui.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    @Test
    fun permissionGranted_insertedStepsShowWithAThousandsSeparator() {
        grantThroughHealthConnect()
        // Otherwise the Calendar card's first view puts the system dialog over the dashboard.
        instrumentation.uiAutomation.grantRuntimePermission(app.packageName, Manifest.permission.READ_CALENDAR)
        // Within today (UTC) and ending before now.
        val until = now.minusSeconds(1)
        val from = maxOf(now.truncatedTo(ChronoUnit.DAYS), until.minus(20, ChronoUnit.MINUTES))
        runBlocking {
            // Health Connect keeps an app's records across reinstalls: start today from this one only.
            // Deleting by range touches only records this app wrote.
            client.deleteRecords(StepsRecord::class, TimeRangeFilter.between(now.truncatedTo(ChronoUnit.DAYS), now))
            inserted += client.insertRecords(
                listOf(StepsRecord(from, utc, until, utc, 12_345, Metadata.manualEntry())),
            ).recordIdsList
        }
        app.container = AppContainer(app, clock = Clock.fixed(now, utc), zoneOf = { utc }, inMemory = true)
        scenario = ActivityScenario.launch(MainActivity::class.java)

        // Steps is the fourth section: bring it on screen, then wait for the read.
        rule.waitUntil(TIMEOUT) { rule.onAllNodes(hasScrollAction()).fetchSemanticsNodes().isNotEmpty() }
        // The count is drawn under the head, which announces it (SPEC §6.1): read it from there.
        val count = hasContentDescription("Steps, 12,345 of 8,000 today")
        rule.waitUntil(TIMEOUT) {
            runCatching { rule.onNode(hasScrollAction()).performScrollToNode(count) }.isSuccess
        }
        rule.onNode(count).assertExists()
    }

    private companion object {
        const val TIMEOUT = 10_000L
    }
}

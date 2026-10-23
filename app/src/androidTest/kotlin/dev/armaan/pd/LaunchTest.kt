package dev.armaan.pd

import android.Manifest
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LaunchTest {
    // Granted up front, so the Calendar and Steps cards' first-view requests don't cover the dashboard.
    private val grant = object : ExternalResource() {
        override fun before() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val pkg = instrumentation.targetContext.packageName
            instrumentation.uiAutomation.grantRuntimePermission(pkg, Manifest.permission.READ_CALENDAR)
            instrumentation.uiAutomation.grantRuntimePermission(pkg, HealthPermission.getReadPermission(StepsRecord::class))
        }
    }

    private val rule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain.outerRule(grant).around(rule)

    @Test
    fun launch_showsDashboard() {
        rule.onNodeWithContentDescription("Calendar", substring = true).assertIsDisplayed()
    }
}

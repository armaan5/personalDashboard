package dev.armaan.pd.steps

import dev.armaan.pd.app.Access
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** SPEC §5.3. */
class StepsTest {
    // Thursday 2026-10-22.
    private val today = LocalDate.of(2026, 10, 22)

    private fun week(vararg counts: Long) = counts.mapIndexed { i, n -> DaySteps(today.minusDays(6L - i), n) }

    private val walked = week(4000, 9000, 0, 7000, 5000, 6000, 6240)
    private val still = week(0, 0, 0, 0, 0, 0, 0)

    @Test
    fun `state - Unavailable wins over every other condition`() {
        assertEquals(StepsCard.Unavailable, stepsCard(HealthSdk.Unavailable, Access.Granted, 6240, walked))
        assertEquals(StepsCard.Unavailable, stepsCard(HealthSdk.Unavailable, Access.Denied, null, null))
    }

    @Test
    fun `state - NeedsInstall wins over permission and data`() {
        assertEquals(StepsCard.NeedsInstall, stepsCard(HealthSdk.NeedsInstall, Access.Granted, 6240, walked))
        assertEquals(StepsCard.NeedsInstall, stepsCard(HealthSdk.NeedsInstall, Access.NotAsked, null, null))
    }

    @Test
    fun `state - a permission state wins over data`() {
        assertEquals(StepsCard.Blocked(Access.Denied), stepsCard(HealthSdk.Available, Access.Denied, 6240, walked))
        assertEquals(StepsCard.Blocked(Access.NotAsked), stepsCard(HealthSdk.Available, Access.NotAsked, null, null))
        assertEquals(
            StepsCard.Blocked(Access.PermanentlyDenied),
            stepsCard(HealthSdk.Available, Access.PermanentlyDenied, 0, still),
        )
    }

    @Test
    fun `state - granted with a 7-day total of 0 is NoData`() {
        assertEquals(StepsCard.NoData, stepsCard(HealthSdk.Available, Access.Granted, 0, still))
    }

    @Test
    fun `state - granted with steps in the week is Steps`() {
        assertEquals(StepsCard.Steps(6240, walked), stepsCard(HealthSdk.Available, Access.Granted, 6240, walked))
    }

    @Test
    fun `state - unknown until the sdk, the access and, once granted, the counts are read`() {
        assertNull(stepsCard(null, Access.Granted, 6240, walked))
        assertNull(stepsCard(HealthSdk.Available, null, 6240, walked))
        assertNull(stepsCard(HealthSdk.Available, Access.Granted, null, null))
    }

    @Test
    fun `ring - the fraction is capped at 1 and the label is not`() {
        assertEquals(1f, ringFraction(12_345, goal = 8000))
        assertEquals("12,345", stepsLabel(12_345))
    }

    @Test
    fun `ring - below the goal the fraction is today over goal`() {
        assertEquals(0.78f, ringFraction(6240, goal = 8000))
        assertEquals("6,240", stepsLabel(6240))
        assertEquals(0f, ringFraction(0, goal = 8000))
    }

    @Test
    fun `week - seven days ordered today last, zero-filling days with no records`() {
        // Buckets as aggregateGroupByPeriod returns them: only days that have records, any order.
        val buckets = mapOf(
            LocalDate.of(2026, 10, 22) to 6240L,
            LocalDate.of(2026, 10, 16) to 4000L,
            LocalDate.of(2026, 10, 19) to 7000L,
        )
        val out = lastSevenDays(buckets, today)
        assertEquals(
            listOf(
                DaySteps(LocalDate.of(2026, 10, 16), 4000),
                DaySteps(LocalDate.of(2026, 10, 17), 0),
                DaySteps(LocalDate.of(2026, 10, 18), 0),
                DaySteps(LocalDate.of(2026, 10, 19), 7000),
                DaySteps(LocalDate.of(2026, 10, 20), 0),
                DaySteps(LocalDate.of(2026, 10, 21), 0),
                DaySteps(LocalDate.of(2026, 10, 22), 6240),
            ),
            out,
        )
    }

    @Test
    fun `week - buckets outside the seven days are dropped`() {
        val buckets = mapOf(LocalDate.of(2026, 10, 15) to 9000L, LocalDate.of(2026, 10, 23) to 100L)
        assertEquals(List(7) { 0L }, lastSevenDays(buckets, today).map { it.count })
    }

    @Test
    fun `goal - clamped to 1,000-50,000`() {
        assertEquals(1000, clampGoal(0))
        assertEquals(1000, clampGoal(-500))
        assertEquals(50_000, clampGoal(50_500))
        assertEquals(50_000, clampGoal(Int.MAX_VALUE))
        assertEquals(8000, clampGoal(8000))
    }

    @Test
    fun `goal - snapped to the 500 step`() {
        assertEquals(7000, clampGoal(7240))
        assertEquals(7500, clampGoal(7260))
    }
}

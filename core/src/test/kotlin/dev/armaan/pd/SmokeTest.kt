package dev.armaan.pd

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

// Proves :core compiles and runs JVM tests. T1 replaces it with real rule tests.
class SmokeTest {
    @Test
    fun `core - runs on the JVM with java time`() {
        assertEquals(LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 22).plusDays(1))
    }
}

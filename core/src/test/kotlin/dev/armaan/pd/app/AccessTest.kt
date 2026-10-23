package dev.armaan.pd.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** SPEC §5.1. */
class AccessTest {
    private val granted = PermissionCheck(granted = true, rationale = false)
    private val refused = PermissionCheck(granted = false, rationale = false)
    private val refusedAskAgain = PermissionCheck(granted = false, rationale = true)

    @Test
    fun `granted wins whatever the flag says`() {
        assertEquals(Access.Granted, access(granted, asked = false))
        assertEquals(Access.Granted, access(granted, asked = true))
    }

    @Test
    fun `not granted and never asked is NotAsked`() {
        assertEquals(Access.NotAsked, access(refused, asked = false))
    }

    @Test
    fun `asked and the system would ask again is Denied`() {
        assertEquals(Access.Denied, access(refusedAskAgain, asked = true))
    }

    @Test
    fun `asked and the system will no longer show its dialog is PermanentlyDenied`() {
        assertEquals(Access.PermanentlyDenied, access(refused, asked = true))
    }

    @Test
    fun `unknown until both the check and the flag are read`() {
        assertNull(access(null, asked = false))
        assertNull(access(refused, asked = null))
    }
}

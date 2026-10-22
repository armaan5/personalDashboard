package dev.armaan.pd.todo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TodoTest {
    @Test
    fun `validation - title is trimmed`() {
        assertEquals("Buy milk", cleanTodoTitle("  Buy milk "))
    }

    @Test
    fun `validation - blank title is refused`() {
        assertNull(cleanTodoTitle(" \t "))
    }

    @Test
    fun `validation - title over 120 characters is refused, 120 is allowed`() {
        assertEquals("a".repeat(120), cleanTodoTitle("a".repeat(120)))
        assertNull(cleanTodoTitle("a".repeat(121)))
    }
}

package dev.armaan.pd.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

class DayTest {
    private val london = ZoneId.of("Europe/London")

    @Test
    fun `day - tick at 23 59 and 00 01 land on different dates`() {
        // 2026-10-22 is BST (UTC+1): 23:59 local = 22:59Z, 00:01 local next day = 23:01Z.
        assertEquals(LocalDate.of(2026, 10, 22), today(Instant.parse("2026-10-22T22:59:00Z"), london))
        assertEquals(LocalDate.of(2026, 10, 23), today(Instant.parse("2026-10-22T23:01:00Z"), london))
    }

    @Test
    fun `day - a zone change moves today`() {
        val now = Instant.parse("2026-10-22T20:00:00Z")
        assertEquals(LocalDate.of(2026, 10, 22), today(now, london))
        assertEquals(LocalDate.of(2026, 10, 23), today(now, ZoneId.of("Pacific/Auckland")))
    }
}

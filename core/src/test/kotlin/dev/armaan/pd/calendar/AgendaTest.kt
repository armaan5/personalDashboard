package dev.armaan.pd.calendar

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AgendaTest {
    // Thursday 2026-10-22, 10:40 in London (BST, UTC+1).
    private val london = ZoneId.of("Europe/London")
    private val now = Instant.parse("2026-10-22T09:40:00Z")

    private fun allDay(id: Long, from: String, until: String) =
        Instance(id, "All day $id", Instant.parse("${from}T00:00:00Z"), Instant.parse("${until}T00:00:00Z"), allDay = true)

    /** [from] and [until] are London wall-clock times on 2026-10-xx, e.g. "22T10:30". */
    private fun timed(id: Long, from: String, until: String) = Instance(
        id,
        "Event $id",
        Instant.parse("2026-10-${from}:00+01:00"),
        Instant.parse("2026-10-${until}:00+01:00"),
        allDay = false,
    )

    private fun ids(list: List<Instance>) = list.map { it.eventId }

    @Test
    fun `all-day - UTC midnight row lands on its own date west of UTC`() {
        val pacific = ZoneOffset.ofHours(-8)
        val tenAm = Instant.parse("2026-10-22T10:00:00-08:00")
        // Friday 23 October. Read in UTC-8 it would start on the 22nd at 16:00.
        val out = agenda(listOf(allDay(1, "2026-10-23", "2026-10-24")), tenAm, pacific)
        assertEquals(emptyList(), ids(out.today.allDay))
        assertEquals(listOf(1L), ids(out.tomorrow.allDay))
    }

    @Test
    fun `all-day - UTC midnight row lands on its own date east of UTC`() {
        val tonga = ZoneOffset.ofHours(13)
        val tenAm = Instant.parse("2026-10-22T10:00:00+13:00")
        // Thursday 22 October. Read in UTC+13 it would run into the 23rd until 13:00.
        val out = agenda(listOf(allDay(1, "2026-10-22", "2026-10-23")), tenAm, tonga)
        assertEquals(listOf(1L), ids(out.today.allDay))
        assertEquals(emptyList(), ids(out.tomorrow.allDay))
    }

    @Test
    fun `all-day - multi-day event covers each date, end exclusive`() {
        val out = agenda(
            listOf(
                allDay(1, "2026-10-22", "2026-10-25"), // 22, 23, 24
                allDay(2, "2026-10-21", "2026-10-23"), // 21, 22
                allDay(3, "2026-10-23", "2026-10-24"), // 23
            ),
            now,
            london,
        )
        assertEquals(listOf(1L, 2L), ids(out.today.allDay).sorted())
        assertEquals(listOf(1L, 3L), ids(out.tomorrow.allDay).sorted())
    }

    @Test
    fun `today - an event that has ended is hidden`() {
        val out = agenda(listOf(timed(1, "22T08:00", "22T09:00"), timed(2, "22T09:40", "22T10:40")), now, london)
        // Event 2 ends exactly now: `end > now` is false.
        assertEquals(emptyList(), ids(out.today.timed))
    }

    @Test
    fun `today - an event in progress is shown and is Now`() {
        val inProgress = timed(1, "22T10:30", "22T11:15")
        val later = timed(2, "22T13:00", "22T14:00")
        val out = agenda(listOf(later, inProgress), now, london)
        assertEquals(listOf(1L, 2L), ids(out.today.timed))
        assertTrue(inProgress.isNow(now))
        assertFalse(later.isNow(now))
    }

    @Test
    fun `today - an event spanning midnight shows under today only`() {
        val out = agenda(listOf(timed(1, "22T23:00", "23T01:00")), now, london)
        assertEquals(listOf(1L), ids(out.today.timed))
        assertEquals(emptyList(), ids(out.tomorrow.timed))
    }

    @Test
    fun `tomorrow - timed events that start tomorrow, ordered by begin`() {
        val out = agenda(
            listOf(timed(1, "23T18:30", "23T19:30"), timed(2, "23T08:15", "23T08:51"), timed(3, "23T00:00", "23T00:30")),
            now,
            london,
        )
        assertEquals(listOf(3L, 2L, 1L), ids(out.tomorrow.timed))
    }

    @Test
    fun `rows outside today and tomorrow are dropped`() {
        // The reader pads its query window, so rows a day either side arrive too.
        val out = agenda(
            listOf(
                timed(1, "21T09:00", "21T10:00"),
                timed(2, "24T00:00", "24T01:00"),
                allDay(3, "2026-10-21", "2026-10-22"),
                allDay(4, "2026-10-24", "2026-10-25"),
            ),
            now,
            london,
        )
        assertTrue(out.today.isEmpty)
        assertTrue(out.tomorrow.isEmpty)
    }

    @Test
    fun `duration rule - full width is two hours, capped`() {
        // design/t3/comps.html: 36 min reads 30 %, 3 h reads full.
        assertEquals(0.3f, timed(1, "23T08:15", "23T08:51").ruleFraction(), 0.001f)
        assertEquals(1f, timed(2, "23T09:00", "23T12:00").ruleFraction())
    }

    @Test
    fun `duration rule - progress marks now within an event in progress only`() {
        // 10:30–11:15 at 10:40: 10 of 45 minutes.
        assertEquals(10f / 45f, timed(1, "22T10:30", "22T11:15").progress(now)!!, 0.001f)
        assertEquals(null, timed(2, "22T13:00", "22T14:00").progress(now))
    }
}
